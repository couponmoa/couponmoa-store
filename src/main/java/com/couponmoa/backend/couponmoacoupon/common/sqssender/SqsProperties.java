package com.couponmoa.backend.couponmoacoupon.common.sqssender;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "spring.cloud.aws.sqs.queue")
@Getter
@Setter
@Validated
public class SqsProperties {
    private String emailAlert;
    private String couponAlert;
    @NotNull
    private String couponIssueEndpoint;
}