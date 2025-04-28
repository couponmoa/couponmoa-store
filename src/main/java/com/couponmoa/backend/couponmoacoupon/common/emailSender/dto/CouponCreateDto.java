package com.couponmoa.backend.couponmoacoupon.common.emailSender.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class CouponCreateDto {
    private String storeName;
    private List<String> emailList;
}
