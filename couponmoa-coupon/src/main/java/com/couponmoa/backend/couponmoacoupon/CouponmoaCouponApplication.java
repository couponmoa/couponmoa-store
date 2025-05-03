package com.couponmoa.backend.couponmoacoupon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.couponmoa.backend.couponmoacoupon", "com.couponmoa.common"})
public class CouponmoaCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponmoaCouponApplication.class, args);
    }

}
