package com.couponmoa.backend.couponmoastore.common.emailSender.service;

import com.couponmoa.backend.couponmoastore.common.emailSender.SqsProperties;
import com.couponmoa.backend.couponmoastore.common.emailSender.dto.CouponAlertDto;
import com.couponmoa.backend.couponmoastore.common.emailSender.dto.SendToMQDto;
import com.couponmoa.backend.couponmoastore.common.exception.ApplicationException;
import com.couponmoa.backend.couponmoastore.common.exception.ErrorCode;
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

    public void sendMessage(SendToMQDto message) {
        queueUrl = sqsProperties.getEmailAlert();
        try {
            log.info(">>> Sending message to SQS: {}", message);
            if (queueUrl == null) {
                queueUrl = "coupon-create-queue";
            }
            String messageQ = objectMapper.writeValueAsString(message);
            sqsTemplate.send(queueUrl, messageQ);
            log.info(">>> Message sent successfully.");
        } catch (Exception e) {
            log.error(">>> Failed to send message", e);
            throw new ApplicationException(ErrorCode.UNABLE_SEND_MESSAGE);
        }
    }

//    public void sendMessage(CouponAlertDto message) {
//        queueUrl = sqsProperties.getCouponAlert();
//        try {
//            log.info(">>> Sending coupon message to SQS: {}", message);
//            if (queueUrl == null) {
//                queueUrl = "coupon-alert";
//            }
//            String messageQ = objectMapper.writeValueAsString(message);
//            sqsTemplate.send(queueUrl, messageQ);
//            log.info(">>> Message sent successfully");
//        } catch (Exception e) {
//            log.error(">>> Failed to send message", e);
//            throw new ApplicationException(ErrorCode.UNABLE_SEND_MESSAGE);
//        }
//    }
}
