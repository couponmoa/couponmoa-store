package com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response;

import lombok.Getter;

@Getter
public class CouponIdResponse {

    private final Long id;

    public CouponIdResponse(Long id) {
        this.id = id;
    }
}
