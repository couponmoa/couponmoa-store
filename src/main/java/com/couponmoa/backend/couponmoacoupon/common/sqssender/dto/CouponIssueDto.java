package com.couponmoa.backend.couponmoacoupon.common.sqssender.dto;

import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.entity.UserCoupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class CouponIssueDto {
    private final Long userId;
    private final Long userCouponId;
    private final String couponName;
    private final LocalDateTime expiryDate;

    public static CouponIssueDto of(UserCoupon userCoupon) {
        return CouponIssueDto.builder()
                .userId(userCoupon.getUserId())
                .userCouponId(userCoupon.getId())
                .couponName(userCoupon.getCoupon().getName())
                .expiryDate(userCoupon.getCoupon().getExpiryDate())
                .build();
    }
}
