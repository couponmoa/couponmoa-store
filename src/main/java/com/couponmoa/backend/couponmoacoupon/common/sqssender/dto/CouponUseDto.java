package com.couponmoa.backend.couponmoacoupon.common.sqssender.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class CouponUseDto {
    private final Long userCouponId;

    public static CouponUseDto of(Long userCouponId) {
        return new CouponUseDto(userCouponId);
    }
}
