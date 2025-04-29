package com.couponmoa.backend.couponmoastore.common.emailSender.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class SendToMQDto {
//    private List<String> emailList;coupon-create-queue
//    private String subject;
//    private String text;
//    private String name;
//
//    public SendToMQDto(List<String> emailList, String subject, String name, String text) {
//        this.emailList = emailList;
//        this.subject = subject;
//        this.name = name;
//        this.text = text;
//    }

    private String storeName;
    private List<String> emailList;

    public SendToMQDto(List<String> emailList, String storeName) {
        this.emailList = emailList;
        this.storeName = storeName;
    }
}
