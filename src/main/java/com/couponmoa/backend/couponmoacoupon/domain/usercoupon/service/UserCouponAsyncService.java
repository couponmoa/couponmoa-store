package com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service;

import com.couponmoa.backend.couponmoacoupon.common.emailSender.dto.CouponAlertDto;
import com.couponmoa.backend.couponmoacoupon.common.emailSender.service.SqsService;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc.StoreGrpcClient;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.repository.CouponRepository;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.entity.UserCoupon;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.repository.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserCouponAsyncService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserCouponRedisService userCouponRedisService;
    private final StoreGrpcClient storeGrpcClient;
    private final SqsService sqsService;

    @Async
    public void saveUserCoupon(Long userId, Long couponId) {
        Coupon coupon = couponRepository.getReferenceById(couponId);
        UserCoupon userCoupon = new UserCoupon(userId, coupon);
        userCouponRepository.save(userCoupon);
    }

    @Async
    public void couponIssue(Long userId, Coupon coupon) {
        Integer resultCode = userCouponRedisService.couponIssue(userId, coupon.getId());
        if (resultCode != 0) return; // 쿠폰 발급 실패

        UserCoupon userCoupon = saveUserCoupon(userId, coupon);
        saveNotification(userId, userCoupon);
    }

    private UserCoupon saveUserCoupon(Long userId, Coupon coupon) {
        UserCoupon userCoupon = new UserCoupon(userId, coupon);
        return userCouponRepository.save(userCoupon);
    }

    private void saveNotification(Long userId, UserCoupon userCoupon) {
        Coupon coupon = userCoupon.getCoupon();
        String storeName = storeGrpcClient.getStoreById(coupon.getStoreId()).getName();
        List<String> emails = storeGrpcClient.getSubscribedUserEmails(coupon.getStoreId());

        sqsService.sendMessage(new CouponAlertDto(
                coupon.getId(),
                coupon.getName(),
                coupon.getStoreId(),
                storeName,
                "쿠폰이 발급되었습니다.",
                emails));
    }
}
