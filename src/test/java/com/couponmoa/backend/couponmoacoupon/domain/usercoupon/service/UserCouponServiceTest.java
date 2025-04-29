package com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service;

import com.couponmoa.backend.couponmoacoupon.common.exception.ApplicationException;
import com.couponmoa.backend.couponmoacoupon.common.exception.ErrorCode;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.dto.CouponExpireDto;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.enums.QueueType;
import com.couponmoa.backend.couponmoacoupon.common.sqssender.service.SqsService;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.enums.CouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc.StoreGrpcClient;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc.UserGrpcClient;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.repository.CouponRepository;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.request.UserCouponRequest;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponCodeResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponUseResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.entity.UserCoupon;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.enums.UserCouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.repository.UserCouponRepository;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.repository.projection.UserCouponProjection;
import com.couponmoa.grpc.store.StoreResponse;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.JobScheduler;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserCouponServiceTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private UserCouponRepository userCouponRepository;
    @Mock
    private UserCouponRedisService userCouponRedisService;
    @Mock
    private UserCouponAsyncService userCouponAsyncService;
    @Mock
    private StoreGrpcClient storeGrpcClient;
    @Mock
    private JobScheduler jobScheduler;
    @Mock
    private SqsService sqsService;
    @Mock
    private UserGrpcClient userGrpcClient;

    @InjectMocks
    private UserCouponService userCouponService;

    private final Long USER_ID = 1L;
    private final Long ADMIN_ID = 2L;
    private final Long COUPON_ID = 1L;
    private final Long USER_COUPON_ID = 1L;
    private final String COUPON_CODE = UUID.randomUUID().toString();
    private final Long STORE_ID = 5L;


    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CreateUserCouponSyncTests {

        @Test
        @Order(1)
        void 쿠폰_발급_쿠폰_없음_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.COUPON_NOT_FOUND;
            given(couponRepository.findActiveByIdOrElseThrow(anyLong(), eq(errorCode)))
                    .willThrow(new ApplicationException(errorCode));

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.createUserCouponSync(USER_ID, COUPON_ID));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(couponRepository).findActiveByIdOrElseThrow(COUPON_ID, errorCode);
            verifyNoInteractions(userCouponRedisService, userCouponAsyncService);
        }

        @Test
        @Order(2)
        void 쿠폰_발급_기간_아님_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.COUPON_NOT_ACTIVE;
            Coupon coupon = mock(Coupon.class);
            given(coupon.getStatus()).willReturn(CouponStatus.UPCOMING);
            given(couponRepository.findActiveByIdOrElseThrow(anyLong(), eq(ErrorCode.COUPON_NOT_FOUND)))
                    .willReturn(coupon);

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.createUserCouponSync(USER_ID, COUPON_ID));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(couponRepository).findActiveByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
            verifyNoInteractions(userCouponRedisService, userCouponAsyncService);
        }

        @Test
        @Order(3)
        void 쿠폰_발급_매진_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.COUPON_SOLD_OUT;
            Coupon coupon = mock(Coupon.class);
            given(coupon.getStatus()).willReturn(CouponStatus.IN_PROGRESS);
            given(coupon.getAvailableQuantity()).willReturn(0);
            given(couponRepository.findActiveByIdOrElseThrow(anyLong(), eq(ErrorCode.COUPON_NOT_FOUND)))
                    .willReturn(coupon);

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.createUserCouponSync(USER_ID, COUPON_ID));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(couponRepository).findActiveByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
            verifyNoInteractions(userCouponRedisService, userCouponAsyncService);
        }

        @Test
        @Order(4)
        void 쿠폰_발급_redis_매진_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.COUPON_SOLD_OUT;
            Coupon coupon = mock(Coupon.class);
            given(coupon.getStatus()).willReturn(CouponStatus.IN_PROGRESS);
            given(coupon.getAvailableQuantity()).willReturn(10);
            given(couponRepository.findActiveByIdOrElseThrow(anyLong(), eq(ErrorCode.COUPON_NOT_FOUND)))
                    .willReturn(coupon);
            given(userCouponRedisService.couponIssue(USER_ID, COUPON_ID)).willReturn(3);

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.createUserCouponSync(USER_ID, COUPON_ID));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(couponRepository).findActiveByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
            verify(userCouponRedisService).couponIssue(USER_ID, COUPON_ID);
            verifyNoInteractions(userCouponAsyncService);
        }

        @Test
        @Order(5)
        void 쿠폰_발급_redis_중복_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.DUPLICATED_USER_COUPON;
            Coupon coupon = mock(Coupon.class);
            given(coupon.getStatus()).willReturn(CouponStatus.IN_PROGRESS);
            given(coupon.getAvailableQuantity()).willReturn(10);
            given(couponRepository.findActiveByIdOrElseThrow(anyLong(), eq(ErrorCode.COUPON_NOT_FOUND)))
                    .willReturn(coupon);
            given(userCouponRedisService.couponIssue(USER_ID, COUPON_ID)).willReturn(2);

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.createUserCouponSync(USER_ID, COUPON_ID));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(couponRepository).findActiveByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
            verify(userCouponRedisService).couponIssue(USER_ID, COUPON_ID);
            verifyNoInteractions(userCouponAsyncService);
        }

        @Test
        @Order(6)
        void 쿠폰_발급_redis_재고_미등록_실패() {
            // Given
            Coupon coupon = mock(Coupon.class);
            given(coupon.getStatus()).willReturn(CouponStatus.IN_PROGRESS);
            given(coupon.getAvailableQuantity()).willReturn(10);
            given(couponRepository.findActiveByIdOrElseThrow(anyLong(), eq(ErrorCode.COUPON_NOT_FOUND)))
                    .willReturn(coupon);
            given(userCouponRedisService.couponIssue(USER_ID, COUPON_ID)).willReturn(1);

            // When & Then
            assertThrows(IllegalStateException.class,
                    () -> userCouponService.createUserCouponSync(USER_ID, COUPON_ID));
            verify(couponRepository).findActiveByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
            verify(userCouponRedisService).couponIssue(USER_ID, COUPON_ID);
            verifyNoInteractions(userCouponAsyncService);
        }

        @Test
        @Order(7)
        void 쿠폰_발급_redis_스크립트_오류_실패() {
            // Given
            Coupon coupon = mock(Coupon.class);
            given(coupon.getStatus()).willReturn(CouponStatus.IN_PROGRESS);
            given(coupon.getAvailableQuantity()).willReturn(10);
            given(couponRepository.findActiveByIdOrElseThrow(anyLong(), eq(ErrorCode.COUPON_NOT_FOUND)))
                    .willReturn(coupon);
            given(userCouponRedisService.couponIssue(USER_ID, COUPON_ID)).willReturn(-1);

            // When & Then
            assertThrows(IllegalStateException.class,
                    () -> userCouponService.createUserCouponSync(USER_ID, COUPON_ID));
            verify(couponRepository).findActiveByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
            verify(userCouponRedisService).couponIssue(USER_ID, COUPON_ID);
            verifyNoInteractions(userCouponAsyncService);
        }

        @Test
        @Order(8)
        void 쿠폰_발급_성공() {
            // Given
            Coupon coupon = mock(Coupon.class);
            given(coupon.getStatus()).willReturn(CouponStatus.IN_PROGRESS);
            given(coupon.getAvailableQuantity()).willReturn(10);
            given(couponRepository.findActiveByIdOrElseThrow(anyLong(), eq(ErrorCode.COUPON_NOT_FOUND)))
                    .willReturn(coupon);
            given(userCouponRedisService.couponIssue(USER_ID, COUPON_ID)).willReturn(0);
            doNothing().when(userCouponAsyncService).saveUserCoupon(USER_ID, COUPON_ID);

            // When
            userCouponService.createUserCouponSync(USER_ID, COUPON_ID);

            // Then
            verify(couponRepository).findActiveByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
            verify(userCouponRedisService).couponIssue(USER_ID, COUPON_ID);
            verify(userCouponAsyncService).saveUserCoupon(USER_ID, COUPON_ID);
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class CreateUserCouponAsyncTests {

        @Test
        @Order(1)
        void 쿠폰_발급_요청_성공() {
            // Given
            Coupon coupon = mock(Coupon.class);
            given(coupon.getStatus()).willReturn(CouponStatus.IN_PROGRESS);
            given(coupon.getAvailableQuantity()).willReturn(10);
            given(couponRepository.findActiveByIdOrElseThrow(anyLong(), eq(ErrorCode.COUPON_NOT_FOUND)))
                    .willReturn(coupon);
            doNothing().when(userCouponAsyncService).couponIssue(USER_ID, coupon);

            // When
            userCouponService.createUserCouponAsync(USER_ID, COUPON_ID);

            // Then
            verify(couponRepository).findActiveByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
            verify(userCouponAsyncService).couponIssue(USER_ID, coupon);
        }

        @Test
        @Order(2)
        void 쿠폰_발급_요청_실패_쿠폰_없음() {
            // Given
            ErrorCode errorCode = ErrorCode.COUPON_NOT_FOUND;
            given(couponRepository.findActiveByIdOrElseThrow(anyLong(), eq(errorCode)))
                    .willThrow(new ApplicationException(errorCode));

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.createUserCouponAsync(USER_ID, COUPON_ID));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(couponRepository).findActiveByIdOrElseThrow(COUPON_ID, errorCode);
            verifyNoInteractions(userCouponAsyncService);
        }

        @Test
        @Order(3)
        void 쿠폰_발급_요청_실패_기간_아님() {
            // Given
            ErrorCode errorCode = ErrorCode.COUPON_NOT_ACTIVE;
            Coupon coupon = mock(Coupon.class);
            given(coupon.getStatus()).willReturn(CouponStatus.ENDED);
            given(couponRepository.findActiveByIdOrElseThrow(anyLong(), eq(ErrorCode.COUPON_NOT_FOUND)))
                    .willReturn(coupon);

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.createUserCouponAsync(USER_ID, COUPON_ID));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(couponRepository).findActiveByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
            verifyNoInteractions(userCouponAsyncService);
        }

        @Test
        @Order(4)
        void 쿠폰_발급_요청_실패_매진() {
            // Given
            ErrorCode errorCode = ErrorCode.COUPON_SOLD_OUT;
            Coupon coupon = mock(Coupon.class);
            given(coupon.getStatus()).willReturn(CouponStatus.IN_PROGRESS);
            given(coupon.getAvailableQuantity()).willReturn(0);
            given(couponRepository.findActiveByIdOrElseThrow(anyLong(), eq(ErrorCode.COUPON_NOT_FOUND)))
                    .willReturn(coupon);

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.createUserCouponAsync(USER_ID, COUPON_ID));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(couponRepository).findActiveByIdOrElseThrow(COUPON_ID, ErrorCode.COUPON_NOT_FOUND);
            verifyNoInteractions(userCouponAsyncService);
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FindUserCouponsTests {
        @Test
        @Order(1)
        void 목록_조회_성공() {
            // Given
            int page = 1;
            int size = 10;
            Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
            UserCouponProjection projection = mock(UserCouponProjection.class);
            Page<UserCouponProjection> projectionPage = new PageImpl<>(List.of(projection), pageable, 1);
            given(userCouponRepository.findByUserIdAndStatus(eq(USER_ID), eq(UserCouponStatus.UNUSED), any(Pageable.class)))
                    .willReturn(projectionPage);

            given(projection.getId()).willReturn(USER_COUPON_ID);
            given(projection.getStatus()).willReturn(UserCouponStatus.UNUSED);
            given(projection.getName()).willReturn("테스트 쿠폰");

            // When
            Page<UserCouponResponse> result = userCouponService.findUserCoupons(USER_ID, UserCouponStatus.UNUSED, page, size);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getId()).isEqualTo(USER_COUPON_ID);
            assertThat(result.getContent().get(0).getName()).isEqualTo("테스트 쿠폰");
            verify(userCouponRepository).findByUserIdAndStatus(eq(USER_ID), eq(UserCouponStatus.UNUSED), any(Pageable.class));
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class FindUserCouponCodeTests {

        @Test
        @Order(1)
        void 코드_조회_쿠폰_없음_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.USER_COUPON_NOT_FOUND;
            given(userCouponRepository.findByIdOrElseThrow(anyLong(), eq(errorCode)))
                    .willThrow(new ApplicationException(errorCode));

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.findUserCouponCode(USER_ID, USER_COUPON_ID));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(userCouponRepository).findByIdOrElseThrow(USER_COUPON_ID, errorCode);
        }

        @Test
        @Order(2)
        void 코드_조회_쿠폰_주인_아님_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.USER_COUPON_ACCESS_DENIED;
            UserCoupon userCoupon = mock(UserCoupon.class);
            given(userCoupon.getUserId()).willReturn(USER_ID + 1);
            given(userCouponRepository.findByIdOrElseThrow(anyLong(), eq(ErrorCode.USER_COUPON_NOT_FOUND)))
                    .willReturn(userCoupon);

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.findUserCouponCode(USER_ID, USER_COUPON_ID));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(userCouponRepository).findByIdOrElseThrow(USER_COUPON_ID, ErrorCode.USER_COUPON_NOT_FOUND);
        }

        @Test
        @Order(3)
        void 코드_조회_사용된_쿠폰_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.USER_COUPON_CODE_UNAVAILABLE;
            UserCoupon userCoupon = mock(UserCoupon.class);
            given(userCoupon.getUserId()).willReturn(USER_ID);
            given(userCoupon.getStatus()).willReturn(UserCouponStatus.USED);
            given(userCouponRepository.findByIdOrElseThrow(anyLong(), eq(ErrorCode.USER_COUPON_NOT_FOUND)))
                    .willReturn(userCoupon);

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.findUserCouponCode(USER_ID, USER_COUPON_ID));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(userCouponRepository).findByIdOrElseThrow(USER_COUPON_ID, ErrorCode.USER_COUPON_NOT_FOUND);
        }

        @Test
        @Order(4)
        void 코드_조회_만료된_쿠폰_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.USER_COUPON_CODE_UNAVAILABLE;
            UserCoupon userCoupon = mock(UserCoupon.class);
            given(userCoupon.getUserId()).willReturn(USER_ID);
            given(userCoupon.getStatus()).willReturn(UserCouponStatus.EXPIRED);
            given(userCouponRepository.findByIdOrElseThrow(anyLong(), eq(ErrorCode.USER_COUPON_NOT_FOUND)))
                    .willReturn(userCoupon);

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.findUserCouponCode(USER_ID, USER_COUPON_ID));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(userCouponRepository).findByIdOrElseThrow(USER_COUPON_ID, ErrorCode.USER_COUPON_NOT_FOUND);
        }

        @Test
        @Order(5)
        void 코드_조회_성공() {
            // Given
            String code = "test-code-123";
            UserCoupon userCoupon = mock(UserCoupon.class);
            given(userCoupon.getUserId()).willReturn(USER_ID);
            given(userCoupon.getStatus()).willReturn(UserCouponStatus.UNUSED); // 사용 안 됨 상태
            given(userCoupon.getCode()).willReturn(code);
            given(userCouponRepository.findByIdOrElseThrow(anyLong(), eq(ErrorCode.USER_COUPON_NOT_FOUND)))
                    .willReturn(userCoupon);

            // When
            UserCouponCodeResponse result = userCouponService.findUserCouponCode(USER_ID, USER_COUPON_ID);

            // Then
            assertNotNull(result);
            assertEquals(code, result.getCode());
            verify(userCouponRepository).findByIdOrElseThrow(USER_COUPON_ID, ErrorCode.USER_COUPON_NOT_FOUND);
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UseUserCouponTests {

        private final UserCouponRequest userCouponRequest = new UserCouponRequest(COUPON_CODE);
        private final Long STORE_ID = 5L;

        @Test
        @Order(1)
        void 쿠폰_사용_처리_쿠폰_없음_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.USER_COUPON_NOT_FOUND;
            given(userCouponRepository.findByCodeWithCoupon(COUPON_CODE)).willReturn(Optional.empty());

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.useUserCoupon(ADMIN_ID, userCouponRequest));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());
            verify(userCouponRepository).findByCodeWithCoupon(COUPON_CODE);
            verifyNoInteractions(storeGrpcClient, userCouponAsyncService);
        }

        @Test
        @Order(2)
        void 쿠폰_사용_처리_쿠폰_매장_주인_아님_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.USER_COUPON_ACCESS_DENIED;
            UserCoupon userCoupon = mock(UserCoupon.class);
            Coupon coupon = mock(Coupon.class);
            StoreResponse storeOwnerResponse = StoreResponse.newBuilder().setId(STORE_ID).setUserId(ADMIN_ID).build();

            given(coupon.getStoreId()).willReturn(STORE_ID);
            given(userCoupon.getCoupon()).willReturn(coupon);
            given(userCouponRepository.findByCodeWithCoupon(COUPON_CODE)).willReturn(Optional.of(userCoupon));
            given(storeGrpcClient.getStoreById(eq(STORE_ID))).willReturn(storeOwnerResponse);

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.useUserCoupon(ADMIN_ID, userCouponRequest));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());

            verify(userCouponRepository).findByCodeWithCoupon(COUPON_CODE);
            verify(storeGrpcClient, atLeastOnce()).getStoreById(STORE_ID); // 최소 1번 호출
            verifyNoInteractions(userCouponAsyncService);
        }

        @Test
        @Order(3)
        void 쿠폰_사용_처리_사용된_쿠폰_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.USER_COUPON_CODE_UNAVAILABLE;
            UserCoupon userCoupon = mock(UserCoupon.class);
            Coupon coupon = mock(Coupon.class);
            StoreResponse storeOwnerResponse = StoreResponse.newBuilder().setId(STORE_ID).setUserId(ADMIN_ID + 1).build();

            given(coupon.getStoreId()).willReturn(STORE_ID);
            given(userCoupon.getCoupon()).willReturn(coupon);
            given(userCoupon.getStatus()).willReturn(UserCouponStatus.USED);
            given(userCouponRepository.findByCodeWithCoupon(COUPON_CODE)).willReturn(Optional.of(userCoupon));
            given(storeGrpcClient.getStoreById(STORE_ID)).willReturn(storeOwnerResponse);

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.useUserCoupon(ADMIN_ID, userCouponRequest));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());

            verify(userCouponRepository).findByCodeWithCoupon(COUPON_CODE);
            verify(storeGrpcClient, atLeastOnce()).getStoreById(STORE_ID);
            verify(userCoupon, never()).setUsed();
            verifyNoInteractions(userCouponAsyncService);
        }

        @Test
        @Order(4)
        void 쿠폰_사용_처리_만료된_쿠폰_실패() {
            // Given
            ErrorCode errorCode = ErrorCode.USER_COUPON_CODE_UNAVAILABLE;
            UserCoupon userCoupon = mock(UserCoupon.class);
            Coupon coupon = mock(Coupon.class);
            StoreResponse storeOwnerResponse = StoreResponse.newBuilder().setId(STORE_ID).setUserId(ADMIN_ID + 1).build();

            given(coupon.getStoreId()).willReturn(STORE_ID);
            given(userCoupon.getCoupon()).willReturn(coupon);
            given(userCoupon.getStatus()).willReturn(UserCouponStatus.EXPIRED);
            given(userCouponRepository.findByCodeWithCoupon(COUPON_CODE)).willReturn(Optional.of(userCoupon));
            given(storeGrpcClient.getStoreById(STORE_ID)).willReturn(storeOwnerResponse);

            // When & Then
            ApplicationException thrown = assertThrows(ApplicationException.class,
                    () -> userCouponService.useUserCoupon(ADMIN_ID, userCouponRequest));
            assertEquals(errorCode.getMessage(), thrown.getMessage());
            assertEquals(errorCode.getHttpStatus(), thrown.getStatus());

            verify(userCouponRepository).findByCodeWithCoupon(COUPON_CODE);
            verify(storeGrpcClient, atLeastOnce()).getStoreById(STORE_ID);
            verify(userCoupon, never()).setUsed();
            verifyNoInteractions(userCouponAsyncService);
        }

        @Test
        @Order(5)
        void 쿠폰_사용_처리_성공() {
            // Given
            UserCoupon userCoupon = mock(UserCoupon.class);
            Coupon coupon = mock(Coupon.class);
            StoreResponse storeOwnerResponse = StoreResponse.newBuilder().setId(STORE_ID).setUserId(ADMIN_ID + 1).build(); // 요청자와 다른 가게 주인

            given(coupon.getStoreId()).willReturn(STORE_ID);
            given(coupon.getName()).willReturn("사용 테스트 쿠폰");
            given(coupon.getDiscountAmount()).willReturn(BigDecimal.valueOf(1000));
            given(coupon.getDescription()).willReturn("테스트 쿠폰 설명");

            given(userCoupon.getId()).willReturn(USER_COUPON_ID);
            given(userCoupon.getCoupon()).willReturn(coupon);
            given(userCoupon.getStatus()).willReturn(UserCouponStatus.UNUSED);
            given(userCouponRepository.findByCodeWithCoupon(COUPON_CODE)).willReturn(Optional.of(userCoupon));
            given(storeGrpcClient.getStoreById(STORE_ID)).willReturn(storeOwnerResponse);

            doNothing().when(userCoupon).setUsed();
            doNothing().when(userCouponAsyncService).sendCouponUseMessage(USER_COUPON_ID);

            // When
            UserCouponUseResponse result = userCouponService.useUserCoupon(ADMIN_ID, userCouponRequest);

            // Then
            assertNotNull(result);
            assertEquals("사용 테스트 쿠폰", result.getName());
            assertEquals(USER_COUPON_ID, result.getId());
            verify(userCouponRepository).findByCodeWithCoupon(COUPON_CODE);
            verify(storeGrpcClient,atLeastOnce()).getStoreById(STORE_ID); // 왜인지는 모르겠지만, 2번 호출됨.
            verify(userCoupon).setUsed();
            verify(userCouponAsyncService).sendCouponUseMessage(USER_COUPON_ID);
        }
    }

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class NotifyExpireUserCouponTests {

        @Test
        @Order(1)
        void 만료_알림_전송_성공() {
            // Given
            LocalDateTime tomorrowStart = LocalDate.now().plusDays(1).atStartOfDay();
            LocalDateTime tomorrowEnd = tomorrowStart.plusDays(1);
            UserCoupon expiringUserCoupon = mock(UserCoupon.class);
            Coupon expiringCoupon = mock(Coupon.class);

            given(expiringUserCoupon.getCoupon()).willReturn(expiringCoupon);
            given(expiringCoupon.getName()).willReturn("만료 예정 쿠폰");
            given(expiringUserCoupon.getUserId()).willReturn(USER_ID);
            given(expiringUserCoupon.getId()).willReturn(USER_COUPON_ID);
            List<UserCoupon> expiringCoupons = List.of(expiringUserCoupon);

            when(userCouponRepository.findUserCouponsExpireTomorrow(eq(tomorrowStart), eq(tomorrowEnd)))
                    .thenReturn(expiringCoupons);

            // When
            userCouponService.sendExpireCouponNotifications();

            // Then
            verify(userCouponRepository).findUserCouponsExpireTomorrow(eq(tomorrowStart), eq(tomorrowEnd));
            verify(jobScheduler).enqueue(any(JobLambda.class));
        }

        @Test
        @Order(2)
        void 그룹_만료_알림_전송_성공() {
            // Given
            List<Long> userIds = List.of(USER_ID);
            List<Long> userCouponIds = List.of(USER_COUPON_ID);
            String couponName = "만료 쿠폰";
            List<String> emails = List.of("user@example.com");

            when(userGrpcClient.getUserEmails(userIds)).thenReturn(emails);
            doNothing().when(sqsService).sendMessage(eq(QueueType.COUPON_EXPIRE), any(CouponExpireDto.class));

            // When
            userCouponService.sendGroupedExpireNotification(userIds, userCouponIds, couponName);

            // Then
            verify(userGrpcClient).getUserEmails(userIds);
            verify(sqsService).sendMessage(eq(QueueType.COUPON_EXPIRE), argThat(dto ->
                    dto instanceof CouponExpireDto &&
                            ((CouponExpireDto)dto).getCouponName().equals(couponName) &&
                            ((CouponExpireDto)dto).getEmailList().equals(emails) &&
                            ((CouponExpireDto)dto).getUserCouponIdList().equals(userCouponIds)
            ));
        }

        @Test
        @Order(3)
        void 그룹_만료_알림_전송_실패_SQS_오류() {
            // Given
            List<Long> userIds = List.of(USER_ID);
            List<Long> userCouponIds = List.of(USER_COUPON_ID);
            String couponName = "만료 쿠폰";
            List<String> emails = List.of("user@example.com");

            when(userGrpcClient.getUserEmails(userIds)).thenReturn(emails);
            doThrow(new RuntimeException("SQS Send Error")).when(sqsService).sendMessage(eq(QueueType.COUPON_EXPIRE), any(CouponExpireDto.class));

            // When & Then
            assertThatThrownBy(() -> userCouponService.sendGroupedExpireNotification(userIds, userCouponIds, couponName))
                    .isInstanceOf(ApplicationException.class)
                    .hasMessageContaining(ErrorCode.SQS_SEND_FAILED.getMessage());

            verify(userGrpcClient).getUserEmails(userIds);
            verify(sqsService).sendMessage(eq(QueueType.COUPON_EXPIRE), any(CouponExpireDto.class));
        }
    }
}