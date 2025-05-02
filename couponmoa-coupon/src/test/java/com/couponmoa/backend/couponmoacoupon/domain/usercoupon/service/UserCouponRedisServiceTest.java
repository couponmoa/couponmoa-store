package com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCouponRedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisScript<Long> couponIssueScript;

    @InjectMocks
    private UserCouponRedisService userCouponRedisService;

    private final Long USER_ID = 1L;
    private final Long COUPON_ID = 1L;

    @Test
    void 쿠폰_발급_스크립트_실행_성공() {
        // Given
        String stockKey = "coupon:" + COUPON_ID + ":stock";
        String userSetKey = "coupon:" + COUPON_ID;
        List<String> keys = List.of(stockKey, userSetKey);
        String userIdArg = String.valueOf(USER_ID);
        Long expectedResultCode = 0L;

        when(redisTemplate.execute(eq(couponIssueScript), eq(keys), eq(userIdArg))).thenReturn(expectedResultCode);

        // When
        Integer actualResultCode = userCouponRedisService.couponIssue(USER_ID, COUPON_ID);

        // Then
        assertThat(actualResultCode).isEqualTo(expectedResultCode.intValue());
        verify(redisTemplate).execute(eq(couponIssueScript), eq(keys), eq(userIdArg));
    }
}