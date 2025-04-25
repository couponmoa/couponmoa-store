package com.couponmoa.backend.couponmoacoupon.common.grpc;

import com.couponmoa.grpc.store.StoreIdRequest;
import com.couponmoa.grpc.store.StoreResponse;
import com.couponmoa.grpc.store.StoreServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Component
public class StoreGrpcClient {

    private final StoreServiceGrpc.StoreServiceBlockingStub storeStub;

    public StoreGrpcClient(@GrpcClient("store-service") StoreServiceGrpc.StoreServiceBlockingStub storeStub) {
        this.storeStub = storeStub;
    }

    public StoreResponse getStoreById(Long storeId) {
        StoreIdRequest request = StoreIdRequest.newBuilder().setStoreId(storeId).build();
        return storeStub.findById(request);
    }
}