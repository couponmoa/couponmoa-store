package com.couponmoa.backend.couponmoacoupon.domain.usercoupon.entity;

import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.enums.UserCouponStatus;
import com.couponmoa.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Getter
@Table(name = "user_coupons")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserCoupon extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserCouponStatus status = UserCouponStatus.UNUSED;

    @Column(nullable = false, unique = true)
    private String code = UUID.randomUUID().toString();

    public UserCoupon(Long userId, Coupon coupon) {
        this.userId = userId;
        this.coupon = coupon;
    }

    public void setUsed() {
        if (status != UserCouponStatus.UNUSED) {
            throw new IllegalStateException("사용되지 않은 쿠폰만 사용 처리할 수 있습니다.");
        }
        status = UserCouponStatus.USED;
    }
}
