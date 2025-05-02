package com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc;

import com.couponmoa.grpc.user.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserGrpcClient {
    private final UserServiceGrpc.UserServiceBlockingStub userStub;

    public UserGrpcClient(@GrpcClient("user-service") UserServiceGrpc.UserServiceBlockingStub userStub) {
        this.userStub = userStub;
    }

    public UserResponse getUserById(Long userId) {
        UserIdRequest request = UserIdRequest.newBuilder().setUserId(userId).build();
        return userStub.findById(request);
    }

    public List<String> getUserEmails(List<Long> userIds) {
        UserIdsRequest request = UserIdsRequest.newBuilder().addAllUserIds(userIds).build();

        UserEmailsResponse response = userStub.getUserEmailList(request);

        return response.getEmailsList();
    }
}