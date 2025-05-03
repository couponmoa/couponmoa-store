package com.couponmoa.backend.couponmoacoupon.domain.coupon.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public enum CouponStatus {
    UPCOMING, IN_PROGRESS, ENDED;

    public static CouponStatus editStatus(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();

        if (now.isBefore(startDate)) {
            return CouponStatus.UPCOMING;
        }
        if (now.isAfter(endDate)) {
            return CouponStatus.ENDED;
        }
        return CouponStatus.IN_PROGRESS;
    }
}

