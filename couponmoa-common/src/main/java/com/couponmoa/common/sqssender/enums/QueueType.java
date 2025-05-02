package com.couponmoa.common.sqssender.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum QueueType {
    COUPON_ISSUE("coupon-issue-v1-queue"),
    COUPON_EXPIRE("coupon-expire-queue"),
    COUPON_USE("coupon-use-queue");

    private final String queueEndpoint;
}
