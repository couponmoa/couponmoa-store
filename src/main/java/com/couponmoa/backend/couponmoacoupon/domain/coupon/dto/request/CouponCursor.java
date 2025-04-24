package com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request;

import java.math.BigDecimal;

public record CouponCursor(
        BigDecimal issuedQuantity,
        String keyword,
        Long couponId
) {}