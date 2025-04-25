package com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.repository;

import com.couponmoa.backend.couponmoacoupon.common.repository.BaseRepository;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.entity.UserCouponSubscribe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface UserCouponSubscribeRepository extends BaseRepository<UserCouponSubscribe, Long> {
    Optional<UserCouponSubscribe> findByUserIdAndCoupon(Long userId, Coupon coupon);

    Page<UserCouponSubscribe> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndCoupon(Long userId, Coupon coupon);

    @EntityGraph(attributePaths = {"user"})
    List<UserCouponSubscribe> findByCouponId(Long couponId);
}
