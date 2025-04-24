package com.couponmoa.backend.couponmoacoupon;

import com.couponmoa.backend.couponmoacoupon.common.emailSender.SqsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SqsProperties.class)
public class CouponmoaCouponApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponmoaCouponApplication.class, args);
    }

}
