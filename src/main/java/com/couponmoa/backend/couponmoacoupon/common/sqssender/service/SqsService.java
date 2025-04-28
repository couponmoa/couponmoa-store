package com.couponmoa.backend.couponmoacoupon.common.sqssender.service;

import com.couponmoa.backend.couponmoacoupon.common.exception.ApplicationException;
import com.couponmoa.backend.couponmoacoupon.common.exception.ErrorCode;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.SqsProperties;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.dto.CouponCreateDto;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.enums.QueueType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
@Getter
public class SqsService {
    private final SqsTemplate sqsTemplate;
    private final SqsProperties sqsProperties;
    private final ObjectMapper objectMapper;

    public <T> void sendMessage(QueueType type, T message) {
        try {
            log.info(">>> Sending message to SQS: {}", message);
            sqsTemplate.send(type.getQueueEndpoint(), message);
            log.info(">>> Message sent successfully.");
        } catch (Exception e) {
            log.error(">>> Failed to send message : {}", e.getMessage(), e);
            throw new ApplicationException(ErrorCode.UNABLE_SEND_MESSAGE);
        }
    }

    public void sendMessage(CouponCreateDto message) {
        String queueUrl = sqsProperties.getEmailAlert();
        try {
            log.info(">>> Sending message to SQS: {}", message);
            if (queueUrl == null) {
                queueUrl = "couponmoa-queue";
            }
            String messageQ = objectMapper.writeValueAsString(message);
            sqsTemplate.send(queueUrl, messageQ);
            log.info(">>> Message sent successfully.");
        } catch (Exception e) {
            log.error(">>> Failed to send message", e);
            throw new ApplicationException(ErrorCode.UNABLE_SEND_MESSAGE);
        }
    }
}