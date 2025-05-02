package com.couponmoa.common.config;

import com.couponmoa.common.sqssender.SqsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableConfigurationProperties(SqsProperties.class)
@EnableJpaAuditing
public class CommonConfig {
}