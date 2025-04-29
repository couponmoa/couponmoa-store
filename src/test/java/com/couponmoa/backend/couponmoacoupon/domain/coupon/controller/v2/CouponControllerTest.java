package com.couponmoa.backend.couponmoacoupon.domain.coupon.controller.v2;

import com.couponmoa.backend.couponmoacoupon.config.TestSecurityConfig;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponCreateRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponSearchByStoreRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponUpdateRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponDetailResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponIdResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponSimpleResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.enums.CouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.service.v2.CouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
@Import(TestSecurityConfig.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

    @BeforeEach
    void setUp() {
        Long adminUserId = 1L;
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                String.valueOf(adminUserId),
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 쿠폰_생성_성공() throws Exception {
        // Given
        CouponCreateRequest request = CouponCreateRequest.builder()
                .name("관리자 테스트 쿠폰")
                .totalQuantity(1000)
                .discountAmount(BigDecimal.valueOf(1000))
                .discountRate(BigDecimal.ZERO)
                .storeId(1L)
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .expiryDate(LocalDateTime.now().plusMonths(1))
                .build();

        CouponIdResponse response = new CouponIdResponse(1L);
        when(couponService.createCoupon(any(CouponCreateRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "USER")
    void 키워드로_쿠폰_조회_성공() throws Exception {
        // Given
        List<CouponSimpleResponse> coupons = Collections.singletonList(
                CouponSimpleResponse.builder()
                        .id(2L)
                        .name("키워드 테스트 쿠폰")
                        .discountAmount(BigDecimal.valueOf(3000))
                        .discountRate(BigDecimal.ZERO)
                        .startDate(LocalDateTime.now().minusDays(1))
                        .endDate(LocalDateTime.now().plusDays(5))
                        .status(CouponStatus.IN_PROGRESS)
                        .build()
        );
        when(couponService.findCouponsByKeyword(any(), any(), anyInt())).thenReturn(coupons);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/coupons")
                        .param("keyword", "키워드")
                        .param("size", "5")
                        .param("id", "10")
                        .param("issuedQuantity", "50"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(2L))
                .andExpect(jsonPath("$.data[0].name").value("키워드 테스트 쿠폰"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void 스토어별_쿠폰_조회_성공() throws Exception {
        // Given
        List<CouponSimpleResponse> content = Collections.singletonList(
                CouponSimpleResponse.builder()
                        .id(3L)
                        .name("스토어 테스트 쿠폰")
                        .discountRate(BigDecimal.TEN)
                        .discountAmount(BigDecimal.ZERO)
                        .startDate(LocalDateTime.now().minusDays(2))
                        .endDate(LocalDateTime.now().plusDays(7))
                        .status(CouponStatus.IN_PROGRESS)
                        .build()
        );
        Page<CouponSimpleResponse> couponsPage = new PageImpl<>(content, PageRequest.of(0, 10), 1);
        when(couponService.findCouponsByStore(anyLong(), any(CouponSearchByStoreRequest.class), anyInt(), anyInt())).thenReturn(couponsPage);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/coupons/store/5")
                        .param("page", "1")
                        .param("size", "10")
                        .param("keyword", "스토어")
                        .param("status", "IN_PROGRESS"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.content[0].id").value(3L))
                .andExpect(jsonPath("$.data.content[0].name").value("스토어 테스트 쿠폰"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void 쿠폰_상세_조회_성공() throws Exception {
        // Given
        CouponDetailResponse detailResponse = CouponDetailResponse.builder()
                .id(1L)
                .name("상세 테스트 쿠폰")
                .totalQuantity(100)
                .availableQuantity(80)
                .discountAmount(BigDecimal.valueOf(5000))
                .status(CouponStatus.IN_PROGRESS)
                .startDate(LocalDateTime.now().minusDays(1))
                .endDate(LocalDateTime.now().plusDays(10))
                .expiryDate(LocalDateTime.now().plusMonths(2))
                .build();
        when(couponService.findCoupon(anyLong(), anyLong())).thenReturn(detailResponse);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v2/coupons/1")
                        .header("X-User-Id", "1")) // @WithMockUser가 자동으로 헤더를 설정해주지 않기 때무네, .header("X-User-Id", "...") 부분을 유지
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andExpect(jsonPath("$.data.name").value("상세 테스트 쿠폰"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 쿠폰_수정_성공() throws Exception {
        // Given
        CouponUpdateRequest request = CouponUpdateRequest.builder()
                .name("수정된 관리자 쿠폰")
                .newTotalQuantity(500)
                .endDate(LocalDateTime.now().plusDays(20))
                .storeId(1L)
                .build();

        CouponIdResponse response = new CouponIdResponse(1L);
        when(couponService.updateCoupon(anyLong(), any(CouponUpdateRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v2/coupons/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.id").value(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 쿠폰_삭제_성공() throws Exception {
        // Given
        doNothing().when(couponService).deleteCoupon(anyLong());

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v2/coupons/1"))
                .andDo(print())
                .andExpect(status().isNoContent());
    }
}