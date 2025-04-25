package com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.service;

import com.couponmoa.backend.couponmoacoupon.common.emailSender.dto.SendToMQDto;
import com.couponmoa.backend.couponmoacoupon.common.emailSender.service.SqsService;
import com.couponmoa.backend.couponmoacoupon.common.exception.ApplicationException;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc.UserGrpcClient;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.repository.CouponRepository;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.dto.response.FindCouponSubscribeListResponse;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.entity.UserCouponSubscribe;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.repository.UserCouponSubscribeRepository;
import com.couponmoa.grpc.user.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.couponmoa.backend.couponmoacoupon.common.exception.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class UserCouponSubscribeService {

    private final CouponRepository couponRepository;
    private final UserCouponSubscribeRepository userCouponSubscribeRepository;
    private final UserGrpcClient userGrpcClient;
    private final SqsService sqsService;

    @Transactional
    public void subscribeCoupon(Long userId, Long couponId) {
        Coupon coupon = getCoupon(couponId);

        if (userCouponSubscribeRepository.existsByUserIdAndCoupon(userId, coupon)) {
            throw new ApplicationException(DUPLICATED_USER_COUPON);
        }

        UserCouponSubscribe userCouponSubscribe = new UserCouponSubscribe(userId, coupon);
        userCouponSubscribeRepository.save(userCouponSubscribe);
    }

    @Transactional
    public void unSubscribeCoupon(Long userId, Long couponId) {
        Coupon coupon = getCoupon(couponId);

        UserCouponSubscribe userCouponSubscribe = userCouponSubscribeRepository.findByUserIdAndCoupon(
                userId, coupon).orElseThrow(() -> new ApplicationException(USER_COUPON_NOT_FOUND));

        userCouponSubscribeRepository.delete(userCouponSubscribe);
    }

    @Transactional(readOnly = true)
    public List<FindCouponSubscribeListResponse> findSubscribeList(Long userId, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        return userCouponSubscribeRepository.findByUserId(userId, pageable)
                .stream()
                .map(FindCouponSubscribeListResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> sendAlert(Long couponId) {
        Coupon coupon = couponRepository.findByIdOrElseThrow(couponId, USER_COUPON_NOT_FOUND);

        List<Long> userIdList = userCouponSubscribeRepository.findByCouponId(couponId)
                .stream()
                .map(UserCouponSubscribe::getUserId)
                .toList();

        List<UserResponse> users = userGrpcClient.getUsersByIds(userIdList);

        List<String> emailList = users.stream()
                .map(UserResponse::getEmail)
                .toList();

        if (emailList.isEmpty()) {
            return emailList;
        }

        SendToMQDto message = new SendToMQDto(
                emailList,
                "쿠폰 갱신 안내",
                coupon.getName(),
                "쿠폰이 새로 발행되었습니다!");

        sqsService.sendMessage(message);

        return emailList;
    }

    private UserResponse getUser(Long userId) {
        return userGrpcClient.getUserById(userId);
    }

    private Coupon getCoupon(Long couponId) {
        return couponRepository.findByIdOrElseThrow(couponId, COUPON_NOT_FOUND);
    }
}
