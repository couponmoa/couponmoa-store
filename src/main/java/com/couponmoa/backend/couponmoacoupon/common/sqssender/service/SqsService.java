package com.couponmoa.backend.couponmoacoupon.common.sqssender.service;

import com.couponmoa.backend.couponmoacoupon.common.sqssender.SqsProperties;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.dto.CouponAlertDto;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.dto.CouponCreateDto;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.dto.CouponExpireDto;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.dto.CouponIssueDto;
import com.couponmoa.backend.couponmoacoupon.common.exception.ApplicationException;
import com.couponmoa.backend.couponmoacoupon.common.exception.ErrorCode;
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
    private String queueUrl;
    private final ObjectMapper objectMapper;

    public void sendMessage(CouponExpireDto message) {
        String queueUrl = sqsProperties.getCouponExpireEndpoint();

        try {
            log.info(">>> Sending message to SQS: {}", message);
            sqsTemplate.send(queueUrl, message);
            log.info(">>> Message sent successfully.");
        } catch (Exception e) {
            log.error(">>> Failed to send message", e);
            throw new ApplicationException(ErrorCode.UNABLE_SEND_MESSAGE);
        }
    }

    public void sendMessage(CouponCreateDto message) {
        queueUrl = sqsProperties.getEmailAlert();
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

    public void sendMessage(CouponAlertDto message) {
        queueUrl = sqsProperties.getCouponAlert();
        try {
            log.info(">>> Sending coupon message to SQS: {}", message);
            if (queueUrl == null) {
                queueUrl = "coupon-alert";
            }
            String messageQ = objectMapper.writeValueAsString(message);
            sqsTemplate.send(queueUrl, messageQ);
            log.info(">>> Message sent successfully");
        } catch (Exception e) {
            log.error(">>> Failed to send message", e);
            throw new ApplicationException(ErrorCode.UNABLE_SEND_MESSAGE);
        }
    }

    public void sendMessage(CouponIssueDto message) {
        String queueUrl = sqsProperties.getCouponIssueEndpoint();

        try {
            log.info(">>> Sending coupon message to SQS: {}", message);
            sqsTemplate.send(queueUrl, message);
            log.info(">>> Message sent successfully");
        } catch (Exception e) {
            log.error(">>> Failed to send issue message: {}", e.getMessage(), e);
            throw new ApplicationException(ErrorCode.UNABLE_SEND_MESSAGE);
        }
    }
}