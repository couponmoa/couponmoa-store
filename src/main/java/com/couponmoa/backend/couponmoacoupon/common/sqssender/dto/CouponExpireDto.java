package com.couponmoa.backend.couponmoacoupon.common.sqssender.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CouponExpireDto {
    private String couponName;
    private LocalDateTime expiryDate;
    private List<String> emailList;
    private List<Long> userCouponIdList;
}