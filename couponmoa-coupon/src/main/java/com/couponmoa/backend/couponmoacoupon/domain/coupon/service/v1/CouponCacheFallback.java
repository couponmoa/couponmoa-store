package com.couponmoa.backend.couponmoacoupon.domain.coupon.service.v1;

import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponCursor;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponSearchByStoreRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponDetailResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponSimpleResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.enums.CouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.repository.CouponRepository;
import com.couponmoa.common.exception.ApplicationException;
import com.couponmoa.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponCacheFallback {

    private final CouponRepository couponRepository;

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
}
