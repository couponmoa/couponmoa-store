package com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc;

import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.service.v2.CouponService;
import com.couponmoa.grpc.coupon.CouponDetailResponse;
import com.couponmoa.grpc.coupon.CouponIdRequest;
import com.couponmoa.grpc.coupon.CouponServiceV2Grpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class CouponGrpcService extends CouponServiceV2Grpc.CouponServiceV2ImplBase {

    private final CouponService couponService;


    @Override
    public void findById(CouponIdRequest request, StreamObserver<CouponDetailResponse> responseObserver) {

        Coupon coupon = couponService.getCouponById(request.getCouponId());

        if (coupon == null) {
            responseObserver.onError(new Throwable("Coupon not found"));
            return;
        }

        CouponDetailResponse.Builder responseBuilder = CouponDetailResponse.newBuilder()
                .setId(coupon.getId())
                .setName(coupon.getName())
                .setTotalQuantity(coupon.getTotalQuantity())
                .setAvailableQuantity(coupon.getAvailableQuantity())
                .setDiscountAmount(coupon.getDiscountAmount().toString())
                .setDiscountRate(coupon.getDiscountRate().toString())
                .setMinOrderAmount(coupon.getMinOrderAmount().toString())
                .setMaxDiscountAmount(coupon.getMaxDiscountAmount().toString())
                .setDescription(coupon.getDescription())
                .setStartDate(coupon.getStartDate().toString())
                .setEndDate(coupon.getEndDate().toString())
                .setExpiryDate(coupon.getExpiryDate().toString())
                .setCreatedAt(coupon.getCreatedAt().toString())
                .setModifiedAt(coupon.getModifiedAt().toString())
                .setStatus(coupon.getStatus().toString());

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }
}
