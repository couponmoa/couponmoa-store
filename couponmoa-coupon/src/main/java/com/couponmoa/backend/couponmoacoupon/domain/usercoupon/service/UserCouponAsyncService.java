package com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service;

import com.couponmoa.backend.couponmoacoupon.common.dto.CouponIssueDto;
import com.couponmoa.backend.couponmoacoupon.common.dto.CouponUseDto;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.repository.CouponRepository;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.entity.UserCoupon;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.repository.UserCouponRepository;
import com.couponmoa.common.sqssender.enums.QueueType;
import com.couponmoa.common.sqssender.service.SqsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserCouponAsyncService {

    private final CouponRepository couponRepository;
    private final UserCouponRepository userCouponRepository;
    private final UserCouponRedisService userCouponRedisService;
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
        sendCouponIssueMessage(userCoupon);
    }

    @Async
    public void sendCouponUseMessage(Long userCouponId) {
        CouponUseDto message = CouponUseDto.of(userCouponId);
        sqsService.sendMessage(QueueType.COUPON_USE, message);
    }

    private UserCoupon saveUserCoupon(Long userId, Coupon coupon) {
        UserCoupon userCoupon = new UserCoupon(userId, coupon);
        return userCouponRepository.save(userCoupon);
    }

    private void sendCouponIssueMessage(UserCoupon userCoupon) {
        CouponIssueDto message = CouponIssueDto.of(userCoupon);
        sqsService.sendMessage(QueueType.COUPON_ISSUE, message);
    }
}
