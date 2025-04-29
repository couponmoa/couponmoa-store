package com.couponmoa.backend.couponmoacoupon.domain.coupon.service.v2;

import com.couponmoa.backend.couponmoacoupon.common.exception.ApplicationException;
import com.couponmoa.backend.couponmoacoupon.common.exception.ErrorCode;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponCreateRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponCursor;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponSearchByStoreRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponUpdateRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponDetailResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponIdResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponSimpleResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.entity.Coupon;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.enums.CouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc.StoreGrpcClient;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.repository.CouponQueryDslRepository;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.repository.CouponRepository;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.service.UserCouponSubscribeService;
import com.couponmoa.grpc.store.StoreResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @Mock
    private CouponQueryDslRepository couponQueryDslRepository;

    @Mock
    private StoreGrpcClient storeGrpcClient;

    @Mock
    private UserCouponSubscribeService userCouponSubServ;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private CouponService couponService;

    private Coupon testCoupon;
    private StoreResponse storeResponse;
    private final Long ADMIN_USER_ID = 1L;
    private final Long STORE_ID = 1L;

    @BeforeEach
    void setUp() {
        MockingSecurityContext();

        storeResponse = StoreResponse.newBuilder()
                .setId(STORE_ID)
                .setUserId(ADMIN_USER_ID)
                .setName("테스트 스토어")
                .build();

        testCoupon = Coupon.builder()
                .storeId(STORE_ID)
                .name("테스트 쿠폰")
                .totalQuantity(100)
                .discountAmount(BigDecimal.valueOf(1000))
                .discountRate(BigDecimal.ZERO)
                .minOrderAmount(BigDecimal.valueOf(10000))
                .maxDiscountAmount(BigDecimal.valueOf(5000))
                .description("테스트 설명")
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .expiryDate(LocalDateTime.now().plusMonths(1))
                .status(CouponStatus.UPCOMING)
                .build();
    }

    @Test
    void 쿠폰_생성_성공() {
        // Given
        CouponCreateRequest request = CouponCreateRequest.builder()
                .name("새 쿠폰")
                .totalQuantity(500)
                .discountAmount(BigDecimal.valueOf(2000))
                .storeId(STORE_ID)
                .build();

        when(storeGrpcClient.getStoreById(STORE_ID)).thenReturn(storeResponse);
        when(couponRepository.existsByNameAndDeletedAtIsNull(request.getName())).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> {
            Coupon coupon = invocation.getArgument(0);
            ReflectionTestUtils.setField(coupon, "id", 1L);
            return coupon;
        });
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(),
                any(ParameterizedTypeReference.class), eq(STORE_ID)))
                .thenReturn(ResponseEntity.ok().build()); // 예시


        // When
        CouponIdResponse response = couponService.createCoupon(request);

        // Then
        assertNotNull(response);
        assertEquals(1L, response.getId());
        verify(storeGrpcClient).getStoreById(STORE_ID);
        verify(couponRepository).existsByNameAndDeletedAtIsNull(request.getName());
        verify(couponRepository).save(any(Coupon.class));
        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), any(),
                any(ParameterizedTypeReference.class), eq(STORE_ID));
    }


    @Test
    void 쿠폰_생성_실패_중복된_이름() {
        // Given
        CouponCreateRequest request = CouponCreateRequest.builder()
                .name("이미 있는 쿠폰")
                .totalQuantity(100)
                .discountAmount(BigDecimal.valueOf(1000))
                .storeId(STORE_ID)
                .build();

        when(storeGrpcClient.getStoreById(STORE_ID)).thenReturn(storeResponse);
        when(couponRepository.existsByNameAndDeletedAtIsNull(request.getName())).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> couponService.createCoupon(request))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(ErrorCode.DUPLICATE_RESOURCE.getMessage());

        verify(storeGrpcClient).getStoreById(STORE_ID);
        verify(couponRepository).existsByNameAndDeletedAtIsNull(request.getName());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void 쿠폰_생성_실패_스토어_소유자_불일치() {
        // Given
        CouponCreateRequest request = CouponCreateRequest.builder()
                .name("새 쿠폰")
                .storeId(STORE_ID)
                .build();
        StoreResponse differentOwnerStore = StoreResponse.newBuilder(storeResponse).setUserId(ADMIN_USER_ID + 1).build();
        when(storeGrpcClient.getStoreById(STORE_ID)).thenReturn(differentOwnerStore);

        // When & Then
        assertThatThrownBy(() -> couponService.createCoupon(request))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(ErrorCode.NOT_VALIDATE_STORE_OWNER.getMessage());

        verify(storeGrpcClient).getStoreById(STORE_ID);
        verify(couponRepository, never()).existsByNameAndDeletedAtIsNull(anyString());
        verify(couponRepository, never()).save(any(Coupon.class));
    }

    @Test
    void 쿠폰_생성_실패_할인_중복_설정() {
        CouponCreateRequest request = CouponCreateRequest.builder()
                .name("할인 중복 쿠폰").totalQuantity(100)
                .discountAmount(BigDecimal.valueOf(1000)).discountRate(BigDecimal.TEN)
                .storeId(STORE_ID).startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10)).expiryDate(LocalDateTime.now().plusMonths(1))
                .build();

        when(storeGrpcClient.getStoreById(STORE_ID)).thenReturn(storeResponse);
        when(couponRepository.existsByNameAndDeletedAtIsNull(request.getName())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> couponService.createCoupon(request))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(ErrorCode.INVALID_DISCOUNT_SETTING.getMessage());
    }

    @Test
    void 쿠폰_생성_실패_할인_미설정() {
        CouponCreateRequest request = CouponCreateRequest.builder()
                .name("할인 없는 쿠폰").totalQuantity(100)
                .discountAmount(BigDecimal.ZERO).discountRate(BigDecimal.ZERO)
                .storeId(STORE_ID).startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10)).expiryDate(LocalDateTime.now().plusMonths(1))
                .build();

        when(storeGrpcClient.getStoreById(STORE_ID)).thenReturn(storeResponse);
        when(couponRepository.existsByNameAndDeletedAtIsNull(request.getName())).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> couponService.createCoupon(request))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(ErrorCode.DISCOUNT_REQUIRED.getMessage());
    }

    @Test
    void 쿠폰_상세_조회_성공() {
        // Given
        Long couponId = 1L;
        ReflectionTestUtils.setField(testCoupon, "id", couponId);
        when(couponRepository.findById(couponId)).thenReturn(Optional.of(testCoupon));

        // When
        CouponDetailResponse response = couponService.findCoupon(couponId, ADMIN_USER_ID);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getName()).isEqualTo(testCoupon.getName());
        verify(couponRepository).findById(couponId);
    }

    @Test
    void 쿠폰_상세_조회_실패_쿠폰_없음() {
        // Given
        Long couponId = 99L;
        when(couponRepository.findById(couponId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> couponService.findCoupon(couponId, ADMIN_USER_ID))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(ErrorCode.COUPON_NOT_FOUND.getMessage());

        verify(couponRepository).findById(couponId);
    }

    @Test
    void 쿠폰_ID로_조회_성공() {
        // Given
        Long couponId = 1L;
        ReflectionTestUtils.setField(testCoupon, "id", couponId);
        when(couponRepository.findByIdOrElseThrow(couponId, ErrorCode.COUPON_NOT_FOUND)).thenReturn(testCoupon);

        // When
        Coupon foundCoupon = couponService.getCouponById(couponId);

        // Then
        assertThat(foundCoupon).isEqualTo(testCoupon);
        verify(couponRepository).findByIdOrElseThrow(couponId, ErrorCode.COUPON_NOT_FOUND);
    }

    @Test
    void 쿠폰_ID로_조회_실패_쿠폰_없음() {
        // Given
        Long nonExistingCouponId = 999L;
        when(couponRepository.findByIdOrElseThrow(nonExistingCouponId, ErrorCode.COUPON_NOT_FOUND))
                .thenThrow(new ApplicationException(ErrorCode.COUPON_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> couponService.getCouponById(nonExistingCouponId))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(ErrorCode.COUPON_NOT_FOUND.getMessage());
        verify(couponRepository).findByIdOrElseThrow(nonExistingCouponId, ErrorCode.COUPON_NOT_FOUND);
    }

    @Test
    void 쿠폰_수정_성공() {
        // Given
        Long couponId = 1L;
        CouponUpdateRequest request = CouponUpdateRequest.builder()
                .name("수정된 쿠폰 이름")
                .newTotalQuantity(150)
                .discountRate(BigDecimal.valueOf(15))
                .discountAmount(BigDecimal.ZERO)
                .storeId(STORE_ID)
                .build();

        Coupon existingCoupon = Coupon.builder()
                .storeId(STORE_ID).name("기존 쿠폰 이름").totalQuantity(100)
                .discountAmount(BigDecimal.valueOf(1000)).discountRate(BigDecimal.ZERO)
                .startDate(LocalDateTime.now().minusDays(1)).endDate(LocalDateTime.now().plusDays(10))
                .expiryDate(LocalDateTime.now().plusMonths(1)).status(CouponStatus.IN_PROGRESS)
                .build();
        ReflectionTestUtils.setField(existingCoupon, "id", couponId);

        when(couponRepository.findByIdOrElseThrow(couponId, ErrorCode.COUPON_NOT_FOUND)).thenReturn(existingCoupon);
        when(storeGrpcClient.getStoreById(STORE_ID)).thenReturn(storeResponse);
        when(couponRepository.existsByNameAndDeletedAtIsNull(request.getName())).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        CouponIdResponse response = couponService.updateCoupon(couponId, request);

        // Then
        assertNotNull(response);
        assertEquals(couponId, response.getId());

        verify(couponRepository).findByIdOrElseThrow(couponId, ErrorCode.COUPON_NOT_FOUND);
        verify(storeGrpcClient).getStoreById(STORE_ID);
        verify(couponRepository).existsByNameAndDeletedAtIsNull(request.getName());
        verify(couponRepository).save(argThat(coupon ->
                coupon.getName().equals("수정된 쿠폰 이름") &&
                        coupon.getTotalQuantity() == 150 &&
                        coupon.getDiscountRate().compareTo(BigDecimal.valueOf(15)) == 0 &&
                        coupon.getDiscountAmount().compareTo(BigDecimal.ZERO) == 0
        ));
        verify(userCouponSubServ, never()).sendAlert(anyLong()); // 상태 변경이 없어 sendAlert 호출 없음
    }

    @Test
    void 쿠폰_삭제_성공() {
        // Given
        Long couponId = 1L;
        ReflectionTestUtils.setField(testCoupon, "id", couponId);
        when(couponRepository.findByIdOrElseThrow(couponId, ErrorCode.COUPON_NOT_FOUND)).thenReturn(testCoupon);
        when(storeGrpcClient.getStoreById(STORE_ID)).thenReturn(storeResponse);
        when(couponRepository.save(any(Coupon.class))).thenReturn(testCoupon);

        // When
        couponService.deleteCoupon(couponId);

        // Then
        verify(couponRepository).findByIdOrElseThrow(couponId, ErrorCode.COUPON_NOT_FOUND);
        verify(storeGrpcClient).getStoreById(STORE_ID);
        assertThat(testCoupon.getDeletedAt()).isNotNull();
        verify(couponRepository).save(testCoupon);
    }

    @Test
    void 쿠폰_수정_실패_쿠폰_없음() {
        // Given
        Long nonExistingCouponId = 999L;
        CouponUpdateRequest request = CouponUpdateRequest.builder().name("수정").storeId(STORE_ID).build();
        when(couponRepository.findByIdOrElseThrow(nonExistingCouponId, ErrorCode.COUPON_NOT_FOUND))
                .thenThrow(new ApplicationException(ErrorCode.COUPON_NOT_FOUND));

        // When & Then
        assertThatThrownBy(() -> couponService.updateCoupon(nonExistingCouponId, request))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining(ErrorCode.COUPON_NOT_FOUND.getMessage());
        verify(couponRepository).findByIdOrElseThrow(nonExistingCouponId, ErrorCode.COUPON_NOT_FOUND);
    }

    @Test
    void 쿠폰_수정_시_상태변경_및_알림호출_성공() {
        // Given: 시작/종료일을 수정하여 상태가 UPCOMING -> IN_PROGRESS 로 변경
        Long couponId = 1L;
        LocalDateTime now = LocalDateTime.now();
        CouponUpdateRequest request = CouponUpdateRequest.builder()
                .name("상태 변경 쿠폰")
                .startDate(now.minusDays(1)) // 시작일을 과거로
                .endDate(now.plusDays(5))   // 종료일을 미래로 == > IN_PROGRESS
                .storeId(STORE_ID)
                .build();

        Coupon existingCoupon = Coupon.builder()
                .storeId(STORE_ID).name("기존 쿠폰").totalQuantity(100)
                .startDate(now.plusDays(1)).endDate(now.plusDays(10)) // 시작일과 종료일 모두 미래==> UPCOMING
                .expiryDate(now.plusMonths(1)).status(CouponStatus.UPCOMING)
                .build();
        ReflectionTestUtils.setField(existingCoupon, "id", couponId);

        when(couponRepository.findByIdOrElseThrow(couponId, ErrorCode.COUPON_NOT_FOUND)).thenReturn(existingCoupon);
        when(storeGrpcClient.getStoreById(STORE_ID)).thenReturn(storeResponse);
        when(couponRepository.existsByNameAndDeletedAtIsNull(request.getName())).thenReturn(false);
        when(couponRepository.save(any(Coupon.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userCouponSubServ.sendAlert(couponId)).thenReturn(Collections.emptyList());

        // When
        couponService.updateCoupon(couponId, request);

        // Then
        verify(couponRepository).save(argThat(coupon ->
                coupon.getStatus() == CouponStatus.IN_PROGRESS
        ));
        verify(userCouponSubServ).sendAlert(couponId);
    }

    @Test
    void 키워드로_쿠폰_조회_성공() {
        // Given
        CouponCursor cursor = new CouponCursor(BigDecimal.valueOf(10), "테스트", 5L);
        int size = 10;
        List<CouponSimpleResponse> expectedResponse = Collections.singletonList(
                CouponSimpleResponse.builder().id(1L).name("테스트 쿠폰 1").build()
        );
        when(couponQueryDslRepository.searchCouponsByKeyword(eq(CouponStatus.IN_PROGRESS), eq(cursor), eq(size)))
                .thenReturn(expectedResponse);

        // When
        List<CouponSimpleResponse> actualResponse = couponService.findCouponsByKeyword(CouponStatus.IN_PROGRESS, cursor, size);

        // Then
        assertThat(actualResponse).isEqualTo(expectedResponse);
        verify(couponQueryDslRepository).searchCouponsByKeyword(eq(CouponStatus.IN_PROGRESS), eq(cursor), eq(size));
    }

    @Test
    void 스토어별_쿠폰_조회_성공() {
        // Given
        Long storeId = 1L;
        CouponSearchByStoreRequest searchRequest = CouponSearchByStoreRequest.builder().keyword("할인").build();
        int size = 5;
        int page = 1;
        Pageable pageable = PageRequest.of(page - 1, size);
        Page<CouponSimpleResponse> expectedPage = new PageImpl<>(
                Collections.singletonList(CouponSimpleResponse.builder().id(1L).name("할인 쿠폰").build()),
                pageable,
                1
        );

        when(couponQueryDslRepository.searchCouponsByStore(
                eq(storeId), eq(searchRequest.getKeyword()), eq(searchRequest.getStatus()),
                eq(searchRequest.getDiscountAmount()), eq(searchRequest.getDiscountRate()),
                eq(searchRequest.getStartDate()), any(Pageable.class)
        )).thenReturn(expectedPage);

        // When
        Page<CouponSimpleResponse> actualPage = couponService.findCouponsByStore(storeId, searchRequest, size, page);

        // Then
        assertThat(actualPage).isEqualTo(expectedPage);
        verify(couponQueryDslRepository).searchCouponsByStore(
                eq(storeId), eq(searchRequest.getKeyword()), eq(searchRequest.getStatus()),
                eq(searchRequest.getDiscountAmount()), eq(searchRequest.getDiscountRate()),
                eq(searchRequest.getStartDate()), any(Pageable.class)
        );
    }

    private void MockingSecurityContext() {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                String.valueOf(ADMIN_USER_ID),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}