package com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.service;

import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc.UserGrpcClient;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.repository.CouponRepository;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.dto.response.FindCouponSubscribeListResponse;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.entity.UserCouponSubscribe;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.repository.UserCouponSubscribeRepository;
import com.couponmoa.common.exception.ApplicationException;
import com.couponmoa.common.exception.ErrorCode;
import com.couponmoa.common.sqssender.dto.CouponCreateDto;
import com.couponmoa.common.sqssender.service.SqsService;
import com.couponmoa.common.testcontainers.TestContainerBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCouponSubscribeServiceTest extends TestContainerBase {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private UserCouponSubscribeRepository userCouponSubscribeRepository;

    @Mock
    private UserGrpcClient userGrpcClient;

    @Mock
    private SqsService sqsService;

    @InjectMocks
    private UserCouponSubscribeService userCouponSubServ;

    private Coupon testCoupon;
    private UserCouponSubscribe testSubscription;
    private final Long USER_ID = 1L;
    private final Long COUPON_ID = 1L;

    @BeforeEach
    void setUp() {
        testCoupon = Coupon.builder()
                .storeId(1L)
                .name("테스트 쿠폰")
                .totalQuantity(100)
                .discountAmount(BigDecimal.valueOf(1000))
                .build();
        ReflectionTestUtils.setField(testCoupon, "id", COUPON_ID);

        testSubscription = new UserCouponSubscribe(USER_ID, testCoupon);
        ReflectionTestUtils.setField(testSubscription, "id", 1L);
    }

    @Test
    void 쿠폰_구독_성공() {
        // Given
        when(couponRepository.findByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND)).thenReturn(testCoupon);
        when(userCouponSubscribeRepository.existsByUserIdAndCoupon(USER_ID, testCoupon)).thenReturn(false);
        when(userCouponSubscribeRepository.save(any(UserCouponSubscribe.class))).thenReturn(testSubscription);

        // When
        userCouponSubServ.subscribeCoupon(USER_ID, COUPON_ID);

        // Then
        verify(couponRepository).findByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
        verify(userCouponSubscribeRepository).existsByUserIdAndCoupon(USER_ID, testCoupon);
        verify(userCouponSubscribeRepository).save(any(UserCouponSubscribe.class));
    }

    @Test
    void 쿠폰_구독_실패_이미_구독함() {
        // Given
        when(couponRepository.findByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND)).thenReturn(testCoupon);
        when(userCouponSubscribeRepository.existsByUserIdAndCoupon(USER_ID, testCoupon)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userCouponSubServ.subscribeCoupon(USER_ID, COUPON_ID))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(ErrorCode.DUPLICATED_USER_COUPON.getMessage());

        verify(couponRepository).findByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
        verify(userCouponSubscribeRepository).existsByUserIdAndCoupon(USER_ID, testCoupon);
        verify(userCouponSubscribeRepository, never()).save(any(UserCouponSubscribe.class));
    }

    @Test
    void 쿠폰_구독_취소_성공() {
        // Given
        when(couponRepository.findByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND)).thenReturn(testCoupon);
        when(userCouponSubscribeRepository.findByUserIdAndCoupon(USER_ID, testCoupon)).thenReturn(Optional.of(testSubscription));
        doNothing().when(userCouponSubscribeRepository).delete(any(UserCouponSubscribe.class));

        // When
        userCouponSubServ.unSubscribeCoupon(USER_ID, COUPON_ID);

        // Then
        verify(couponRepository).findByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
        verify(userCouponSubscribeRepository).findByUserIdAndCoupon(USER_ID, testCoupon);
        verify(userCouponSubscribeRepository).delete(testSubscription);
    }

    @Test
    void 쿠폰_구독_취소_실패_구독정보_없음() {
        // Given
        when(couponRepository.findByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND)).thenReturn(testCoupon);
        when(userCouponSubscribeRepository.findByUserIdAndCoupon(USER_ID, testCoupon)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> userCouponSubServ.unSubscribeCoupon(USER_ID, COUPON_ID))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(ErrorCode.USER_COUPON_NOT_FOUND.getMessage());

        verify(couponRepository).findByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
        verify(userCouponSubscribeRepository).findByUserIdAndCoupon(USER_ID, testCoupon);
        verify(userCouponSubscribeRepository, never()).delete(any(UserCouponSubscribe.class));
    }

    @Test
    void 구독_목록_조회_성공() {
        // Given
        Page<UserCouponSubscribe> page = new PageImpl<>(List.of(testSubscription));
        when(userCouponSubscribeRepository.findByUserId(eq(USER_ID), any(Pageable.class))).thenReturn(page);

        // When
        List<FindCouponSubscribeListResponse> result = userCouponSubServ.findSubscribeList(USER_ID, 0, 10);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(testCoupon.getId());
        assertThat(result.get(0).getName()).isEqualTo(testCoupon.getName());
        verify(userCouponSubscribeRepository).findByUserId(eq(USER_ID), any(Pageable.class));
    }

    @Test
    void 알림_전송_성공() {
        // Given
        List<Long> userIds = List.of(USER_ID);
        List<String> emails = List.of("test@example.com");
        when(couponRepository.findByIdOrElseThrow(COUPON_ID, ErrorCode.USER_COUPON_NOT_FOUND)).thenReturn(testCoupon);
        when(userCouponSubscribeRepository.findByCouponId(COUPON_ID)).thenReturn(List.of(testSubscription));
        when(userGrpcClient.getUserEmails(userIds)).thenReturn(emails);
        doNothing().when(sqsService).sendMessage(any(CouponCreateDto.class));

        // When
        List<String> resultEmails = userCouponSubServ.sendAlert(COUPON_ID);

        // Then
        assertThat(resultEmails).isEqualTo(emails);
        verify(couponRepository).findByIdOrElseThrow(COUPON_ID, ErrorCode.USER_COUPON_NOT_FOUND);
        verify(userCouponSubscribeRepository).findByCouponId(COUPON_ID);
        verify(userGrpcClient).getUserEmails(userIds);
        verify(sqsService).sendMessage(argThat(dto ->
                dto.getStoreName().equals(testCoupon.getName()) &&
                        dto.getEmailList().equals(emails)
        ));
    }

    @Test
    void 알림_전송_구독자_없음() {
        // Given
        when(couponRepository.findByIdOrElseThrow(COUPON_ID, ErrorCode.USER_COUPON_NOT_FOUND)).thenReturn(testCoupon);
        when(userCouponSubscribeRepository.findByCouponId(COUPON_ID)).thenReturn(Collections.emptyList());
        when(userGrpcClient.getUserEmails(Collections.emptyList())).thenReturn(Collections.emptyList()); // userGrpcClient.getUserEmails로 빈 리스트를 받으면 빈 리스트를 반환,

        // When
        List<String> resultEmails = userCouponSubServ.sendAlert(COUPON_ID);

        // Then
        assertThat(resultEmails).isEmpty();
        verify(couponRepository).findByIdOrElseThrow(COUPON_ID, ErrorCode.USER_COUPON_NOT_FOUND);
        verify(userCouponSubscribeRepository).findByCouponId(COUPON_ID);
        verify(userGrpcClient).getUserEmails(eq(Collections.emptyList()));
        verify(sqsService, never()).sendMessage(any(CouponCreateDto.class));
    }
}