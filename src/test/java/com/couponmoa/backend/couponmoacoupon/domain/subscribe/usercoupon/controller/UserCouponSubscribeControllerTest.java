package com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.controller;

import com.couponmoa.backend.couponmoacoupon.config.TestSecurityConfig;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.dto.response.FindCouponSubscribeListResponse;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.service.UserCouponSubscribeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserCouponSubscribeController.class)
@Import(TestSecurityConfig.class)
class UserCouponSubscribeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserCouponSubscribeService userCouponSubscribeService;

    private final Long USER_ID = 1L;
    private final Long COUPON_ID = 1L;

    @Test
    @WithMockUser(username = "1") // USER_ID를 username으로 설정 ( princapal의 이름 )
    void 쿠폰_구독_성공() throws Exception {
        // Given
        doNothing().when(userCouponSubscribeService).subscribeCoupon(eq(USER_ID), eq(COUPON_ID));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/coupons/{couponId}/subscriptions", COUPON_ID)
                        .header("X-User-Id", String.valueOf(USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value(COUPON_ID + "번 쿠폰 구독 완료"));
    }

    @Test
    @WithMockUser(username = "1")
    void 쿠폰_구독_취소_성공() throws Exception {
        // Given
        doNothing().when(userCouponSubscribeService).unSubscribeCoupon(eq(USER_ID), eq(COUPON_ID));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/coupons/{couponId}/unsubscriptions", COUPON_ID)
                        .header("X-User-Id", String.valueOf(USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value(COUPON_ID + "번 쿠폰 구독 취소"));
    }

    @Test
    @WithMockUser(username = "1")
    void 내_쿠폰_구독_목록_조회_성공() throws Exception {
        // Given
        List<FindCouponSubscribeListResponse> responseList = Collections.singletonList(
                new FindCouponSubscribeListResponse());
        when(userCouponSubscribeService.findSubscribeList(eq(USER_ID), anyInt(), anyInt())).thenReturn(responseList);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/coupons/subscriptions")
                        .header("X-User-Id", String.valueOf(USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("쿠폰 구독 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @WithMockUser
    void 알림_전송_성공() throws Exception {
        // Given
        List<String> emailList = List.of("test1@example.com", "test2@example.com");
        when(userCouponSubscribeService.sendAlert(anyLong())).thenReturn(emailList);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/coupons/{couponId}/alert", COUPON_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("test1@example.com"));
    }
}