package com.couponmoa.backend.couponmoacoupon.domain.usercoupon.repository;

import com.couponmoa.backend.couponmoacoupon.common.repository.BaseRepository;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.entity.UserCoupon;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.enums.UserCouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.repository.projection.UserCouponProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserCouponRepository extends BaseRepository<UserCoupon, Long> {
    @Query("""
                SELECT uc.id AS id, uc.status AS status, c.discountAmount AS discountAmount, c.discountRate AS discountRate,
                       c.name AS name, c.description AS description, c.expiryDate AS expiryDate, c.minOrderAmount AS minOrderAmount,
                       c.maxDiscountAmount AS maxDiscountAmount
                FROM UserCoupon uc
                JOIN uc.coupon c
                WHERE uc.userId = :userId AND (:status IS NULL OR uc.status = :status)
            """)
    Page<UserCouponProjection> findByUserIdAndStatus(Long userId, UserCouponStatus status, Pageable pageable);

    @Query("SELECT uc FROM UserCoupon uc JOIN FETCH uc.coupon c WHERE uc.code = :code")
    Optional<UserCoupon> findByCodeWithCoupon(String code);

    @Query("""
            SELECT uc
            FROM UserCoupon uc
            WHERE uc.status = 'UNUSED'
              AND uc.coupon.expiryDate BETWEEN :start AND :end
            """)
    List<UserCoupon> findUserCouponsExpireTomorrow(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
