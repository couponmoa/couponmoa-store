package com.couponmoa.backend.couponmoastore.domain.store.grpc;

import com.couponmoa.backend.couponmoastore.domain.store.entity.Store;
import com.couponmoa.backend.couponmoastore.domain.store.service.v2.StoreServiceV2;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.service.UserStoreSubscribeService;
import com.couponmoa.grpc.store.EmailListResponse;
import com.couponmoa.grpc.store.StoreIdRequest;
import com.couponmoa.grpc.store.StoreResponse;
import com.couponmoa.grpc.store.StoreServiceGrpc;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
public class StoreGrpcService extends StoreServiceGrpc.StoreServiceImplBase {
    private final StoreServiceV2 storeServiceV2;
    private final UserStoreSubscribeService storeSubServ;

    public StoreGrpcService(StoreServiceV2 storeServiceV2, UserStoreSubscribeService storeSubServ) {
        this.storeServiceV2 = storeServiceV2;
        this.storeSubServ = storeSubServ;
    }

    @Override
    public void findById(StoreIdRequest request, StreamObserver<StoreResponse> responseObserver) {
        Store store = storeServiceV2.getStoreById(request.getStoreId());

        StoreResponse.Builder responseBuilder = StoreResponse.newBuilder()
                .setId(store.getId())
                .setAddress(store.getAddress())
                .setDescription(store.getDescription())
                .setName(store.getName())
                .setUserId(store.getUserId());

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void findSubscribedUdserEmails(StoreIdRequest request, StreamObserver<EmailListResponse> responseObserver) {
        List<String> emailList = storeSubServ.sendToSQS(request.getStoreId());

        EmailListResponse.Builder responseBuilder = EmailListResponse.newBuilder()
                .addAllEmails(emailList);

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}