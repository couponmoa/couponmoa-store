package com.couponmoa.backend.couponmoacoupon.common.emailSender;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.cloud.aws.sqs.queue")
@Getter
@Setter
public class SqsProperties {
    private String emailAlert;
    private String couponAlert;
}