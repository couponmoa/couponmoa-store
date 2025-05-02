package com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service;

import com.couponmoa.backend.couponmoacoupon.common.dto.CouponIssueDto;
import com.couponmoa.backend.couponmoacoupon.common.dto.CouponUseDto;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.repository.CouponRepository;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.entity.UserCoupon;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.repository.UserCouponRepository;
import com.couponmoa.common.sqssender.enums.QueueType;
import com.couponmoa.common.sqssender.service.SqsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCouponAsyncServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private UserCouponRedisService userCouponRedisService;

    @Mock
    private SqsService sqsService;

    @InjectMocks
    private UserCouponAsyncService userCouponAsyncService;

    private Coupon testCoupon;
    private final Long USER_ID = 1L;
    private final Long COUPON_ID = 1L;
    private final Long USER_COUPON_ID = 1L;

    @BeforeEach
    void setUp() {
        testCoupon = Coupon.builder()
                .storeId(1L).name("테스트 쿠폰").totalQuantity(100)
                .discountAmount(BigDecimal.valueOf(1000)).expiryDate(LocalDateTime.now().plusMonths(1))
                .build();
        ReflectionTestUtils.setField(testCoupon, "id", COUPON_ID);
    }

    @Test
    void 사용자_쿠폰_저장_성공() {
        // Given
        when(couponRepository.getReferenceById(COUPON_ID)).thenReturn(testCoupon);
        when(userCouponRepository.save(any(UserCoupon.class))).thenAnswer(invocation -> {
            UserCoupon uc = invocation.getArgument(0);
            ReflectionTestUtils.setField(uc, "id", USER_COUPON_ID);
            return uc;
        });

        // When
        userCouponAsyncService.saveUserCoupon(USER_ID, COUPON_ID);

        // Then
        verify(couponRepository).getReferenceById(COUPON_ID);
        verify(userCouponRepository).save(argThat(uc ->
                uc.getUserId().equals(USER_ID) && uc.getCoupon().equals(testCoupon)
        ));
    }

    @Test
    void 쿠폰_발급_처리_성공() {
        // Given
        when(userCouponRedisService.couponIssue(USER_ID, COUPON_ID)).thenReturn(0);
        when(userCouponRepository.save(any(UserCoupon.class))).thenAnswer(invocation -> {
            UserCoupon uc = invocation.getArgument(0);
            ReflectionTestUtils.setField(uc, "id", USER_COUPON_ID);
            return uc;
        });
        doNothing().when(sqsService).sendMessage(eq(QueueType.COUPON_ISSUE), any(CouponIssueDto.class));

        // When
        userCouponAsyncService.couponIssue(USER_ID, testCoupon);

        // Then
        verify(userCouponRedisService).couponIssue(USER_ID, COUPON_ID);
        verify(userCouponRepository).save(any(UserCoupon.class));
        verify(sqsService).sendMessage(eq(QueueType.COUPON_ISSUE), any(CouponIssueDto.class));
    }

    @Test
    void 쿠폰_발급_처리_실패_redis() {
        // Given
        when(userCouponRedisService.couponIssue(USER_ID, COUPON_ID)).thenReturn(2);

        // When
        userCouponAsyncService.couponIssue(USER_ID, testCoupon);

        // Then
        verify(userCouponRedisService).couponIssue(USER_ID, COUPON_ID);
        verify(userCouponRepository, never()).save(any(UserCoupon.class));
        verify(sqsService, never()).sendMessage(any(), any());
    }

    @Test
    void 쿠폰_사용_메시지_전송_성공() {
        // Given
        doNothing().when(sqsService).sendMessage(eq(QueueType.COUPON_USE), any(CouponUseDto.class));

        // When
        userCouponAsyncService.sendCouponUseMessage(USER_COUPON_ID);

        // Then
        verify(sqsService).sendMessage(eq(QueueType.COUPON_USE), argThat(
                dto -> dto instanceof CouponUseDto && ((CouponUseDto) dto).
                        getUserCouponId().equals(USER_COUPON_ID)
        ));
    }
}