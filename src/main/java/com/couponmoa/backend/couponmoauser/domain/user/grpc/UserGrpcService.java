package com.couponmoa.backend.couponmoauser.domain.user.grpc;
import com.couponmoa.backend.couponmoauser.domain.user.entity.User;
import com.couponmoa.backend.couponmoauser.domain.user.service.v1.UserServiceV1;
import com.couponmoa.grpc.user.UserIdRequest;
import com.couponmoa.grpc.user.UserResponse;
import com.couponmoa.grpc.user.UserServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserServiceV1 userService;

    @Override
    public void findById(UserIdRequest request, StreamObserver<UserResponse> responseObserver) {
        User user = userService.getUserById(request.getUserId());

        String deletedAt = user.getDeletedAt() != null ? user.getDeletedAt().toString() : ""; // String format

        UserResponse.Builder responseBuilder = UserResponse.newBuilder();
        responseBuilder.setId(user.getId());
        responseBuilder.setEmail(user.getEmail());
        responseBuilder.setNickname(user.getNickname());
        responseBuilder.setUserRole(user.getUserRole().toString());
        responseBuilder.setDeletedAt(deletedAt);

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
