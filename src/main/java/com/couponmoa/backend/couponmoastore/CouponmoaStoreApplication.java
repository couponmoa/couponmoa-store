package com.couponmoa.backend.couponmoastore;

import com.couponmoa.backend.couponmoastore.common.emailSender.SqsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SqsProperties.class)
public class CouponmoaStoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CouponmoaStoreApplication.class, args);
    }

}
