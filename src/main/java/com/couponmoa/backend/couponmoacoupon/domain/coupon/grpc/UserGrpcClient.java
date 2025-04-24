package com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc;

import com.couponmoa.grpc.user.UserIdRequest;
import com.couponmoa.grpc.user.UserResponse;
import com.couponmoa.grpc.user.UserServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

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
}