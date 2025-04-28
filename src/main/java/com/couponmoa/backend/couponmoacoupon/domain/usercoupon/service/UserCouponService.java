package com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service;

import com.couponmoa.backend.couponmoacoupon.common.sqssender.dto.CouponExpireDto;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.enums.QueueType;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.service.SqsService;
import com.couponmoa.backend.couponmoacoupon.common.exception.ApplicationException;
import com.couponmoa.backend.couponmoacoupon.common.exception.ErrorCode;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.enums.CouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc.StoreGrpcClient;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc.UserGrpcClient;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.repository.CouponRepository;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.request.UserCouponRequest;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponCodeResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponUseResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.entity.UserCoupon;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.enums.UserCouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.repository.UserCouponRepository;
import com.couponmoa.grpc.store.StoreResponse;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import org.jobrunr.jobs.annotations.Job;
import org.jobrunr.scheduling.JobScheduler;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserCouponService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserCouponRedisService userCouponRedisService;
    private final UserCouponAsyncService userCouponAsyncService;
    private final StoreGrpcClient storeGrpcClient;
    private final JobScheduler jobScheduler;
    private final SqsService sqsService;
    private final UserGrpcClient userGrpcClient;

    @Timed(value = "user_coupon.create_sync.time", description = "동기 쿠폰 발급에 걸린 시간",  histogram = true)
    @Counted(value = "user_coupon.create_sync.count", description = "동기 쿠폰 발급 횟수")
    public void createUserCouponSync(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findActiveByIdOrElseThrow(couponId, ErrorCode.COUPON_NOT_FOUND);

        validateCouponIssuablePeriod(coupon.getStatus());
        validateCouponNotSoldOut(coupon.getAvailableQuantity());

        Integer resultCode = userCouponRedisService.couponIssue(userId, couponId);
        validateIssueResultCode(resultCode);

        userCouponAsyncService.saveUserCoupon(userId, couponId);
    }

    @Timed(value = "user_coupon.create_async.time", description = "비동기 쿠폰 발급 요청에 걸린 시간",  histogram = true)
    @Counted(value = "user_coupon.create_async.count", description = "비동기 쿠폰 발급 요청 횟수")
    public void createUserCouponAsync(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findActiveByIdOrElseThrow(couponId, ErrorCode.COUPON_NOT_FOUND);

        validateCouponIssuablePeriod(coupon.getStatus());
        validateCouponNotSoldOut(coupon.getAvailableQuantity());

        userCouponAsyncService.couponIssue(userId, coupon);
    }

    @Timed(value = "user_coupon.find_coupons.time", description = "사용자 쿠폰 목록 조회에 걸린 시간",  histogram = true)
    @Counted(value = "user_coupon.find_coupons.count", description = "사용자 쿠폰 목록 조회 횟수")
    @Transactional(readOnly = true)
    public Page<UserCouponResponse> findUserCoupons(Long userId, UserCouponStatus status, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        return userCouponRepository.findByUserIdAndStatus(userId, status, pageable)
                .map(UserCouponResponse::from);
    }

    @Timed(value = "user_coupon.find_code.time", description = "쿠폰 코드 조회에 걸린 시간",  histogram = true)
    @Counted(value = "user_coupon.find_code.count", description = "쿠폰 코드 조회 횟수")
    @Transactional(readOnly = true)
    public UserCouponCodeResponse findUserCouponCode(Long userId, Long userCouponId) {
        UserCoupon userCoupon = userCouponRepository.findByIdOrElseThrow(userCouponId, ErrorCode.USER_COUPON_NOT_FOUND);

        validateCouponOwner(userCoupon.getUserId(), userId);
        validateCouponStatus(userCoupon.getStatus());

        return new UserCouponCodeResponse(userCoupon.getCode());
    }

    @Timed(value = "user_coupon.use.time", description = "쿠폰 사용 처리에 걸린 시간",  histogram = true)
    @Counted(value = "user_coupon.use.count", description = "쿠폰 사용 처리 횟수")
    @Transactional
    public UserCouponUseResponse useUserCoupon(Long userId, UserCouponRequest request) {
        UserCoupon userCoupon = userCouponRepository.findByCodeWithCoupon(request.getCode())
                .orElseThrow(() -> new ApplicationException(ErrorCode.USER_COUPON_NOT_FOUND));

        StoreResponse storeResponse = storeGrpcClient.getStoreById(userCoupon.getCoupon().getStoreId());

        validateCouponStoreOwner(storeResponse.getId(), userId);
        validateCouponStatus(userCoupon.getStatus());

        userCoupon.setUsed();
        userCouponAsyncService.sendCouponUseMessage(userCoupon.getId());
        return UserCouponUseResponse.from(userCoupon);
    }

    @Transactional
    public void sendExpireCouponNotifications() {
        List<UserCoupon> userCouponList = findUserCouponsExpireTomorrow();

        Map<String, List<UserCoupon>> grouped = userCouponList.stream()
                .collect(Collectors.groupingBy(userCoupon -> userCoupon.getCoupon().getName()));

        for (Map.Entry<String, List<UserCoupon>> entry : grouped.entrySet()) {
            String couponName = entry.getKey();

            List<Long> userCouponIds = new ArrayList<>();
            List<Long> userIds = new ArrayList<>();

            entry.getValue().forEach(userCoupon -> {
                userCouponIds.add(userCoupon.getId());
                userIds.add(userCoupon.getUserId());
            });

            // 큐에 Job 등록
            jobScheduler.enqueue(() -> sendGroupedExpireNotification(userIds, userCouponIds, couponName));
        }
    }

    // jobrunr 실행 대상, sqs 요청 메서드
    @Job(name = "Send grouped notification", retries = 3)
    @Transactional
    public void sendGroupedExpireNotification(List<Long> userIds, List<Long> userCouponIds, String couponName) {
        try { // sqs 메시지 요청 실패시 db 롤백
            sqsService.sendMessage(QueueType.COUPON_EXPIRE, createMessageQueueDto(userIds, userCouponIds, couponName));
        } catch (Exception e) {
            throw new ApplicationException(ErrorCode.SQS_SEND_FAILED);
        }
    }

    private void validateCouponIssuablePeriod(CouponStatus status) {
        if (status != CouponStatus.IN_PROGRESS) {
            throw new ApplicationException(ErrorCode.COUPON_NOT_ACTIVE);
        }
    }

    private void validateCouponNotSoldOut(int availableQuantity) {
        if (availableQuantity <= 0) {
            throw new ApplicationException(ErrorCode.COUPON_SOLD_OUT);
        }
    }

    private void validateIssueResultCode(Integer resultCode) {
        switch (resultCode) {
            case 0:
                return;
            case 1:
                throw new IllegalStateException("쿠폰 재고가 redis에 등록되지 않았습니다.");
            case 2:
                throw new ApplicationException(ErrorCode.DUPLICATED_USER_COUPON);
            case 3:
                throw new ApplicationException(ErrorCode.COUPON_SOLD_OUT);
            default:
                throw new IllegalStateException("예상하지 못한 값이 반환되었습니다.");
        }
    }

    private void validateCouponOwner(Long couponUserId, Long userId) {
        if (!couponUserId.equals(userId)) {
            throw new ApplicationException(ErrorCode.USER_COUPON_ACCESS_DENIED);
        }
    }

    private void validateCouponStatus(UserCouponStatus status) {
        if (status != UserCouponStatus.UNUSED) {
            throw new ApplicationException(ErrorCode.USER_COUPON_CODE_UNAVAILABLE);
        }
    }

    private void validateCouponStoreOwner(Long storeId, Long userId) {
        Long storeOwnerId = storeGrpcClient.getStoreById(storeId).getUserId();
        if (storeOwnerId.equals(userId)) {
            throw new ApplicationException(ErrorCode.USER_COUPON_ACCESS_DENIED);
        }
    }

    private CouponExpireDto createMessageQueueDto(List<Long> userIds, List<Long> userCouponIds, String couponName) {
        List<String> emailList = userGrpcClient.getUserEmails(userIds);

        return CouponExpireDto.builder()
                .couponName(couponName)
                .expiryDate(LocalDateTime.now().plusDays(1))
                .emailList(emailList)
                .userCouponIdList(userCouponIds).build();
    }

    // 다음날에 만료되는 쿠폰 조회
    private List<UserCoupon> findUserCouponsExpireTomorrow() {
        LocalDateTime start = LocalDate.now().plusDays(1).atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        return userCouponRepository.findUserCouponsExpireTomorrow(start, end);
    }
}
