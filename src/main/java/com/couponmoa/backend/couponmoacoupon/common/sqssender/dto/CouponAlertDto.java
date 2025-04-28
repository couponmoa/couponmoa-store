package com.couponmoa.backend.couponmoacoupon.common.sqssender.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CouponAlertDto {
    private Long couponId;
    private String couponName;
    private Long storeId;
    private String storeName;
    private String message;
    private List<String> emails;
//    private Long userId;
//    private Long notificationId;
}