package com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.entity;

import com.couponmoa.backend.couponmoacoupon.common.entity.BaseEntity;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class UserCouponSubscribe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    public UserCouponSubscribe(Long userId, Coupon coupon) {
        this.userId = userId;
        this.coupon = coupon;
    }
}
