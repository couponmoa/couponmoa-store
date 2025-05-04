package com.couponmoa.backend.couponmoacoupon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.couponmoa.backend.couponmoacoupon", "com.couponmoa.common"})
public class CouponmoaCouponApplication implements CommandLineRunner {

    private static final Logger jacksonLogger = LoggerFactory.getLogger(com.fasterxml.jackson.databind.ObjectMapper.class);
    private static final Logger autoConfigLogger = LoggerFactory.getLogger(org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration.class);
    private static final Logger applicationLogger = LoggerFactory.getLogger(CouponmoaCouponApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(CouponmoaCouponApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        jacksonLogger.debug("Jackson logger debug level test.");
        autoConfigLogger.debug("AutoConfig logger debug level test.");
        applicationLogger.debug("Debug level logging is enabled.");
    }
}
