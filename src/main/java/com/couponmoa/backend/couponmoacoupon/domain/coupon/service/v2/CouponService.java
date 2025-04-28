package com.couponmoa.backend.couponmoacoupon.domain.coupon.service.v2;

import com.couponmoa.backend.couponmoacoupon.common.sqssender.dto.CouponAlertDto;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.service.SqsService;
import com.couponmoa.backend.couponmoacoupon.common.exception.ApplicationException;
import com.couponmoa.backend.couponmoacoupon.common.exception.ErrorCode;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc.StoreGrpcClient;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponCreateRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponCursor;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponSearchByStoreRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponUpdateRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponDetailResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponIdResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponSimpleResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.enums.CouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.repository.CouponQueryDslRepository;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.repository.CouponRepository;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.service.UserCouponSubscribeService;
import com.couponmoa.grpc.store.StoreResponse;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.couponmoa.backend.couponmoacoupon.domain.coupon.enums.CouponStatus.editStatus;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponQueryDslRepository couponQueryDslRepository;
    private final StoreGrpcClient storeGrpcClient;
    private final UserCouponSubscribeService userCouponSubServ;
    private final SqsService sqsService;

    @Timed(value = "coupon.create.time", description = "쿠폰 생성에 걸린 시간", histogram = true)
    @Counted(value = "coupon.create.count", description = "생성된 쿠폰 수")
    @CacheEvict(value = "coupons", allEntries = true)
    @Transactional
    public CouponIdResponse createCoupon(CouponCreateRequest requestDto) {

        // Store의 소유자가 맞는지 검증 & Store가 존재하는지도 검증
        StoreResponse storeResponse = validateStoreOwnerAndGetStore(requestDto.getStoreId());

        // 이름 중복 검증 추가
        if (couponRepository.existsByNameAndDeletedAtIsNull(requestDto.getName())) {
            throw new ApplicationException(ErrorCode.DUPLICATE_RESOURCE, "이미 존재하는 쿠폰 이름입니다.");
        }

        // 할인 로직 검증
        // discountAmount와 discountRate 중 하나는 반드시 0이어야 함 (변액 할인과 정액 할인을 동시에 제공하는 쿠폰은 없다.)
        boolean isDiscountAmountDefault = requestDto.getDiscountAmount().compareTo(BigDecimal.ZERO) == 0;
        boolean isDiscountRateDefault = requestDto.getDiscountRate().compareTo(BigDecimal.ZERO) == 0;

        // 정액 할인이나 변액 할인 둘중 하나는 설정 해야함.
        if (isDiscountAmountDefault && isDiscountRateDefault) {
            throw new ApplicationException(ErrorCode.DISCOUNT_REQUIRED);
        }

        // 정액 할인과 변액 할인이 동시에 적용될 수 없음.
        if (!isDiscountAmountDefault && !isDiscountRateDefault) {
            throw new ApplicationException(ErrorCode.INVALID_DISCOUNT_SETTING);
        }

        // 최대 할인 금액은 정액 할인 금액보다 커야함.
        if (!isDiscountAmountDefault && requestDto.getMaxDiscountAmount() != null) {
            if (requestDto.getDiscountAmount().compareTo(requestDto.getMaxDiscountAmount()) > 0) {
                throw new ApplicationException(ErrorCode.DISCOUNT_EXCEEDS_MAX);
            }
        }



        // 날짜 검증
        validateDates(requestDto.getStartDate(), requestDto.getEndDate(), requestDto.getExpiryDate(), true);

        Coupon newCoupon = Coupon.builder()
                .name(requestDto.getName())
                .storeId(requestDto.getStoreId())
                .totalQuantity(requestDto.getTotalQuantity())
                .discountAmount(requestDto.getDiscountAmount())
                .discountRate(requestDto.getDiscountRate())
                .minOrderAmount(requestDto.getMinOrderAmount())
                .maxDiscountAmount(requestDto.getMaxDiscountAmount())
                .description(requestDto.getDescription())
                .startDate(requestDto.getStartDate())
                .endDate(requestDto.getEndDate())
                .expiryDate(requestDto.getExpiryDate())
                .status(CouponStatus.UPCOMING)
                .build();

        Coupon savedCoupon = couponRepository.save(newCoupon);

        sendEmail(savedCoupon);

        return new CouponIdResponse(savedCoupon.getId());
    }

    @Timed(value = "coupon.find_by_keyword.time", description = "키워드로 쿠폰 조회에 걸린 시간", histogram = true)
    @Counted(value = "coupon.find_by_keyword.count", description = "키워드로 쿠폰 조회 횟수")
    @Cacheable(value = "coupons", key = "T(com.couponmoa.backend.common.util.CacheKeyGenerator).generateCacheKey(#status, #cursor, #size)")
    @Retry(name = "couponService", fallbackMethod = "fallbackFindCouponsByKeyword")
    @Transactional(readOnly = true)
    public List<CouponSimpleResponse> findCouponsByKeyword(CouponStatus status, CouponCursor cursor, int size) {
        log.info("findCouponsByKeyword 호출");
        return searchWithSafeCursor(status, cursor == null ? new CouponCursor(null, null, null) : cursor, size);
    }

    @Transactional(readOnly = true)
    @Timed(value = "coupon.find_by_store.time", description = "스토어별 쿠폰 조회에 걸린 시간", histogram = true)
    @Counted(value = "coupon.find_by_store.count", description = "스토어별 쿠폰 조회 횟수")
    @Cacheable(value = "coupons", key = "T(com.couponmoa.backend.common.util.CacheKeyGenerator).generateCacheKey(#storeId, #requestDto, #size, #page)")
    @Retry(name = "couponService", fallbackMethod = "fallbackFindCouponsByStore")
    public Page<CouponSimpleResponse> findCouponsByStore(
            Long storeId,
            CouponSearchByStoreRequest requestDto,
            int size, int page
    ) {
        log.info("findCouponsByStore 호출");
        Pageable pageable = PageRequest.of(page - 1, size);

        return couponQueryDslRepository.searchCouponsByStore(
                storeId,
                requestDto.getKeyword(),
                requestDto.getStatus(),
                requestDto.getDiscountAmount(),
                requestDto.getDiscountRate(),
                requestDto.getStartDate(),
                pageable
        );
    }

    @Timed(value = "coupon.find.time", description = "쿠폰 상세 조회에 걸린 시간", histogram = true)
    @Counted(value = "coupon.find.count", description = "쿠폰 상세 조회 횟수")
    @Cacheable(value = "couponDetails", key = "T(com.couponmoa.backend.common.util.CacheKeyGenerator).generateCouponCacheKey(#couponId)")
    @Retry(name = "couponService", fallbackMethod = "fallbackFindCoupon")
    @Transactional(readOnly = true)
    public CouponDetailResponse findCoupon(Long couponId, Long userId) {
        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.COUPON_NOT_FOUND));
        return CouponDetailResponse.toDto(coupon);
    }

    @Transactional(readOnly = true)
    public Coupon getCouponById(Long couponId) {
        return couponRepository.findByIdOrElseThrow(couponId, ErrorCode.COUPON_NOT_FOUND);
    }

    // 쿠폰 수정 시 캐시 무효화
    @Timed(value = "coupon.update.time", description = "쿠폰 수정에 걸린 시간", histogram = true)
    @CacheEvict(value = "coupons", allEntries = true)
    public CouponIdResponse updateCoupon(Long couponId, CouponUpdateRequest requestDto) {

        // 아직 존재하는 쿠폰인지 검증
        Coupon coupon = couponRepository.findByIdOrElseThrow(couponId, ErrorCode.COUPON_NOT_FOUND);

        // Store의 소유자가 맞는지 검증 & Store가 존재하는지도 검증
        validateStoreOwnerAndGetStore(requestDto.getStoreId());

        // 새 이름이 기존 이름과 다를 경우에만 중복 검사
        if (!coupon.getName().equals(requestDto.getName()) &&
                couponRepository.existsByNameAndDeletedAtIsNull(requestDto.getName())) {
            throw new ApplicationException(ErrorCode.DUPLICATE_RESOURCE, "이미 존재하는 쿠폰 이름입니다.");
        }

        // update 요청데이터의 dates null 검증, null 일 경우 이전 데이터로.
        ResolvedDates resolvedDates = resolveDates(requestDto, coupon);

        // 날짜 검증
        validateDates(resolvedDates.startDate(), resolvedDates.endDate(), resolvedDates.expiryDate(), false);

        if (requestDto.getNewTotalQuantity() > 0) {
            coupon.updateQuantity(requestDto.getNewTotalQuantity());
        }

        // 할인율과 할인금액 검증
        boolean isDiscountAmountSet = requestDto.getDiscountAmount() != null && requestDto.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0;
        boolean isDiscountRateSet = requestDto.getDiscountRate() != null && requestDto.getDiscountRate().compareTo(BigDecimal.ZERO) > 0;

        if (isDiscountAmountSet && isDiscountRateSet) {
            throw new ApplicationException(ErrorCode.INVALID_DISCOUNT_SETTING);
        }

        // 쿠폰 상태 업데이트 (날짜, 발급수량에 따라)
        CouponStatus oldStatus = coupon.getStatus();
        CouponStatus newStatus = editStatus(resolvedDates.startDate, resolvedDates.endDate);

        // 상태가 실제로 변경되었을때만 updateStatus
        if (oldStatus != newStatus) {
            coupon.updateStatus(newStatus);
            // 상태가 IN_PROGRESS로 변경된 경우에만 알림 전송
            if (newStatus == CouponStatus.IN_PROGRESS) {

                userCouponSubServ.sendAlert(couponId);
            }
        }

        // 쿠폰 업데이트, 사실상 put 방식처럼 작동하도록.. 이게맞나 ?
        updateIfPresent(coupon, requestDto, coupon.getStoreId());

        couponRepository.save(coupon);

        return new CouponIdResponse(coupon.getId());
    }

    // 쿠폰 삭제 시 캐시 무효화
    @Timed(value = "coupon.delete.time", description = "쿠폰 삭제에 걸린 시간", histogram = true)
    @CacheEvict(value = "coupons", allEntries = true)
    public void deleteCoupon(Long couponId) {
        // 존재하는 쿠폰인지 검증
        Coupon coupon = couponRepository.findByIdOrElseThrow(couponId, ErrorCode.COUPON_NOT_FOUND);

        // Store의 소유자가 맞는지 검증 & Store가 존재하는지도 검증
        validateStoreOwnerAndGetStore(coupon.getStoreId());

        coupon.delete();
    }

    private StoreResponse validateStoreOwnerAndGetStore(Long storeId) {
        StoreResponse storeResponse;
        try {
            storeResponse = storeGrpcClient.getStoreById(storeId);
        } catch (RuntimeException e) {
            throw new ApplicationException(ErrorCode.STORE_NOT_FOUND);
        }

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof String userIdString)) {
            throw new ApplicationException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        Long userId = Long.valueOf(userIdString);

        if (storeResponse.getUserId() != userId) {
            throw new ApplicationException(ErrorCode.NOT_VALIDATE_STORE_OWNER);
        }

        return storeResponse;
    }

    private void updateIfPresent(Coupon coupon, CouponUpdateRequest requestDto, Long storeId) {
        String name = requestDto.getName() != null ? requestDto.getName() : coupon.getName();
        BigDecimal discountAmount = requestDto.getDiscountAmount() != null ? requestDto.getDiscountAmount() : coupon.getDiscountAmount();
        BigDecimal discountRate = requestDto.getDiscountRate() != null ? requestDto.getDiscountRate() : coupon.getDiscountRate();
        BigDecimal minOrderAmount = requestDto.getMinOrderAmount() != null ? requestDto.getMinOrderAmount() : coupon.getMinOrderAmount();
        BigDecimal maxDiscountAmount = requestDto.getMaxDiscountAmount() != null ? requestDto.getMaxDiscountAmount() : coupon.getMaxDiscountAmount();
        String description = requestDto.getDescription() != null ? requestDto.getDescription() : coupon.getDescription();
        LocalDateTime startDate = requestDto.getStartDate() != null ? requestDto.getStartDate() : coupon.getStartDate();
        LocalDateTime endDate = requestDto.getEndDate() != null ? requestDto.getEndDate() : coupon.getEndDate();
        LocalDateTime expiryDate = requestDto.getExpiryDate() != null ? requestDto.getExpiryDate() : coupon.getExpiryDate();

        coupon.update(
                storeId,
                name,
                discountAmount,
                discountRate,
                minOrderAmount,
                maxDiscountAmount,
                description,
                startDate,
                endDate,
                expiryDate
        );
    }

    private static void validateDates(LocalDateTime startDate, LocalDateTime endDate, LocalDateTime expiryDate, boolean isCreate) {
        LocalDateTime now = LocalDateTime.now();

        if (startDate.isAfter(endDate)) {
            throw new ApplicationException(ErrorCode.INVALID_END_DATE);
        }

        if (expiryDate.isBefore(endDate)) {
            throw new ApplicationException(ErrorCode.INVALID_EXPIRY_DATE);
        }

        if (isCreate) {
            // 생성일 경우에만 현재 시간 기준 검증
            if (startDate.isBefore(now)) {
                throw new ApplicationException(ErrorCode.INVALID_START_DATE);
            }
        }
    }

    // 쿠폰 수정시에 요청 dto의 값과 기존 날짜 값을 고려해 실제 수정될 날짜 값을 담는 내부 record 클래스.
    private static record ResolvedDates(
            LocalDateTime startDate,
            LocalDateTime endDate,
            LocalDateTime expiryDate
    ) {}

    private static ResolvedDates resolveDates(CouponUpdateRequest requestDto, Coupon coupon) {
        LocalDateTime startDate = requestDto.getStartDate() != null ? requestDto.getStartDate() : coupon.getStartDate();
        LocalDateTime endDate = requestDto.getEndDate() != null ? requestDto.getEndDate() : coupon.getEndDate();
        LocalDateTime expiryDate = requestDto.getExpiryDate() != null ? requestDto.getExpiryDate() : coupon.getExpiryDate();

        return new ResolvedDates(startDate, endDate, expiryDate);
    }

    /**
     * 새로 쿠폰이 발행 될 경우의 로직
     * > 해당 가게를 구독한 사람에게 이메일로 알림이 전송된다
     */
    private void sendEmail(Coupon savedCoupon) {
        Long storeId = savedCoupon.getStoreId();
        String storeName = storeGrpcClient.getStoreById(storeId).getName();
        List<String> emails = storeGrpcClient.getSubscribedUserEmails(storeId);// 가게 구독 메일 전송
        CouponAlertDto couponAlertDto = new CouponAlertDto(
                savedCoupon.getId(),
                savedCoupon.getName(),
                storeId,
                storeName,
                "구독한 가게의 새 쿠폰 발급 알림",
                emails);

        sqsService.sendMessage(couponAlertDto);
    }


    // findCouponsByKeyword 실패 시 fallback 메서드
    public List<CouponSimpleResponse> fallbackFindCouponsByKeyword(CouponStatus status, CouponCursor cursor, int size, Exception e) {

        log.info("Redis 장애 발생, DB에서 조회: " + e.getMessage());

        return couponRepository.findAll()
                .stream()
                .map(CouponSimpleResponse::toDto)
                .collect(Collectors.toList());
    }

    // findCouponsByStore 실패 시 fallback 메서드
    public Page<CouponSimpleResponse> fallbackFindCouponsByStore(
            Long storeId,
            CouponSearchByStoreRequest requestDto,
            int size, int page, Exception e) {
        log.info("Redis 장애 발생, DB에서 조회: " + e.getMessage());

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Coupon> couponPage = couponRepository.findByStoreId(storeId, pageable);

        return couponPage.map(CouponSimpleResponse::toDto);
    }

    // findCoupon 실패 시 fallback 메서드
    public CouponDetailResponse fallbackFindCoupon(
            Long couponId,
            Long userId,
            Exception e) {
        log.info("Redis 장애 발생, DB에서 조회: " + e.getMessage());

        Coupon coupon = couponRepository.findById(couponId)
                .orElseThrow(() -> new ApplicationException(ErrorCode.COUPON_NOT_FOUND));

        return CouponDetailResponse.toDto(coupon);
    }

    public List<CouponSimpleResponse> searchWithSafeCursor(
            CouponStatus status,
            CouponCursor cursor,
            int size) {
        return couponQueryDslRepository.searchCouponsByKeyword(status, cursor, size);
    }
}