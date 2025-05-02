package com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.controller;

import com.couponmoa.backend.couponmoacoupon.config.TestSecurityConfig;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.dto.response.FindCouponSubscribeListResponse;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.service.UserCouponSubscribeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 필요한 static import 추가
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@AutoConfigureRestDocs
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
    @WithMockUser(username = "1")
    void 쿠폰_구독_성공() throws Exception {
        // Given
        doNothing().when(userCouponSubscribeService).subscribeCoupon(anyLong(), anyLong());
        mockMvc.perform(post("/api/v1/coupons/{couponId}/subscriptions", COUPON_ID)
                        .header("X-User-Id", String.valueOf(USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value(COUPON_ID + "번 쿠폰 구독 완료"))
                .andDo(document("coupon-subscribe",
                        pathParameters(
                                parameterWithName("couponId").description("구독할 쿠폰 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data").description("응답 데이터 (null)").optional()
                        )
                ));
    }

    @Test
    @WithMockUser(username = "1")
    void 쿠폰_구독_취소_성공() throws Exception {
        // Given
        doNothing().when(userCouponSubscribeService).unSubscribeCoupon(anyLong(), anyLong());
        mockMvc.perform(post("/api/v1/coupons/{couponId}/unsubscriptions", COUPON_ID)
                        .header("X-User-Id", String.valueOf(USER_ID)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value(COUPON_ID + "번 쿠폰 구독 취소"))
                .andDo(document("coupon-unsubscribe",
                        pathParameters(
                                parameterWithName("couponId").description("구독 취소할 쿠폰 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data").description("응답 데이터 (null)").optional()
                        )
                ));
    }

    @Test
    @WithMockUser(username = "1")
    void 내_쿠폰_구독_목록_조회_성공() throws Exception {
        // Given
        FindCouponSubscribeListResponse response = new FindCouponSubscribeListResponse();
        // 테스트를 위해 ReflectionTestUtils 또는 setter를 사용하여 필드 값 설정 (예시)
        // ReflectionTestUtils.setField(response, "id", 1L);
        // ReflectionTestUtils.setField(response, "name", "테스트 구독 쿠폰");
        // ... 나머지 필드 설정 ...
        List<FindCouponSubscribeListResponse> responseList = Collections.singletonList(response);
        when(userCouponSubscribeService.findSubscribeList(eq(USER_ID), anyInt(), anyInt())).thenReturn(responseList);

        // When & Then
        mockMvc.perform(get("/api/v1/coupons/subscriptions")
                        .header("X-User-Id", String.valueOf(USER_ID))
                        .param("page", "1")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("쿠폰 구독 목록 조회 성공"))
                .andExpect(jsonPath("$.data").isArray())
                .andDo(document("coupon-subscription-list",
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (기본값: 1)").optional(),
                                parameterWithName("size").description("페이지당 항목 수 (기본값: 10)").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data").description("쿠폰 구독 목록"),
                                fieldWithPath("data[].id").description("쿠폰 ID"),
                                fieldWithPath("data[].name").description("쿠폰 이름").optional(), // FindCouponSubscribeListResponse에 name 필드가 없으므로 optional
                                fieldWithPath("data[].availableQuantity").description("사용 가능한 쿠폰 수량"),
                                fieldWithPath("data[].discountAmount").description("할인 금액 (정액)").optional(),
                                fieldWithPath("data[].discountRate").description("할인율 (정률)").optional(),
                                fieldWithPath("data[].description").description("쿠폰 설명").optional(),
                                fieldWithPath("data[].startDate").description("쿠폰 발급 시작일").optional(),
                                fieldWithPath("data[].endDate").description("쿠폰 발급 종료일").optional(),
                                fieldWithPath("data[].expiryDate").description("쿠폰 만료일").optional()
                        )
                ));
    }

    @Test
    @WithMockUser
    void 알림_전송_성공() throws Exception {
        // Given
        List<String> emailList = List.of("test1@example.com", "test2@example.com");
        when(userCouponSubscribeService.sendAlert(anyLong())).thenReturn(emailList);

        // When & Then
        mockMvc.perform(post("/api/v1/coupons/{couponId}/alert", COUPON_ID))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0]").value("test1@example.com"))
                .andDo(document("coupon-alert",
                        pathParameters(
                                parameterWithName("couponId").description("알림을 보낼 쿠폰 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data").description("알림이 발송된 이메일 주소 목록")
                        )
                ));
    }
}