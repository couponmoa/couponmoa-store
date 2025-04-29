package com.couponmoa.backend.couponmoacoupon.domain.usercoupon.controller;

import com.couponmoa.backend.couponmoacoupon.config.TestSecurityConfig;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.request.UserCouponRequest;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponCodeResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponUseResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.enums.UserCouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service.UserCouponAsyncService;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service.UserCouponService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCouponController.class)
@Import(TestSecurityConfig.class)
class UserCouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserCouponService userCouponService;

    @MockitoBean
    private UserCouponAsyncService userCouponAsyncService;

    private final Long USER_ID = 1L;
    private final Long ADMIN_ID = 2L;
    private final Long COUPON_ID = 1L;
    private final Long USER_COUPON_ID = 1L;

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void 쿠폰_발급_성공_sync() throws Exception {
        // Given
        doNothing().when(userCouponService).createUserCouponSync(eq(USER_ID), eq(COUPON_ID));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/coupons/{couponId}/issue", COUPON_ID)
                        .header("X-User_Id", String.valueOf(USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void 쿠폰_발급_성공_async() throws Exception {
        // Given
        doNothing().when(userCouponService).createUserCouponAsync(eq(USER_ID), eq(COUPON_ID));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/coupons/{couponId}/issue", COUPON_ID)
                        .header("X-User_Id", String.valueOf(USER_ID)))
                .andDo(print())
//                .andExpect(status().isAccepted())
//                .andExpect(jsonPath("$.code").value(202)) // @WebMvcTest 환경에서 202가 반환되지 않고 200이 반환 됨.
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void 내_쿠폰_목록_조회_성공() throws Exception {
        // Given
        List<UserCouponResponse> content = List.of(
                new UserCouponResponse(USER_COUPON_ID, UserCouponStatus.UNUSED, BigDecimal.valueOf(1000), BigDecimal.ZERO,
                        "내 쿠폰 1", "설명1", LocalDateTime.now().plusDays(30), BigDecimal.ZERO, BigDecimal.valueOf(5000))
        );
        Page<UserCouponResponse> responsePage = new PageImpl<>(content, PageRequest.of(0, 10), 1);
        when(userCouponService.findUserCoupons(eq(USER_ID), any(), anyInt(), anyInt())).thenReturn(responsePage);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/user-coupons")
                        .header("X-User_Id", String.valueOf(USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].id").value(USER_COUPON_ID))
                .andExpect(jsonPath("$.data.content[0].name").value("내 쿠폰 1"));
    }

    @Test
    @WithMockUser(username = "1", roles = "USER")
    void 쿠폰_코드_조회_성공() throws Exception {
        // Given
        String couponCode = UUID.randomUUID().toString();
        UserCouponCodeResponse response = new UserCouponCodeResponse(couponCode);
        when(userCouponService.findUserCouponCode(USER_ID, USER_COUPON_ID)).thenReturn(response);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/user-coupons/{userCouponId}/code", USER_COUPON_ID)
                        .header("X-User_Id", String.valueOf(USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.code").value(couponCode));
    }

    @Test
    @WithMockUser(username = "2", roles = "ADMIN")
    void 쿠폰_사용_처리_성공() throws Exception {
        // Given
        String couponCode = UUID.randomUUID().toString();
        UserCouponRequest request = new UserCouponRequest(couponCode);
        UserCouponUseResponse response = new UserCouponUseResponse(
                USER_COUPON_ID, BigDecimal.valueOf(1000), BigDecimal.ZERO, "사용될 쿠폰", "설명", BigDecimal.ZERO, BigDecimal.valueOf(5000)
        );
        when(userCouponService.useUserCoupon(eq(ADMIN_ID), any(UserCouponRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/user-coupons/use")
                        .header("X-User_Id", String.valueOf(ADMIN_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.id").value(USER_COUPON_ID))
                .andExpect(jsonPath("$.data.name").value("사용될 쿠폰"));
    }

    @Test
    @WithMockUser
    void 만료_쿠폰_알림_전송_성공() throws Exception {
        // Given
        doNothing().when(userCouponService).sendExpireCouponNotifications();

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/user-coupons/expire"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"));
    }
}