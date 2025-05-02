package com.couponmoa.backend.couponmoacoupon.domain.usercoupon.controller;

import com.couponmoa.backend.couponmoacoupon.config.TestSecurityConfig;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.request.UserCouponRequest;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponCodeResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponUseResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.enums.UserCouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service.UserCouponAsyncService;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service.UserCouponService;
import com.couponmoa.common.config.CommonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
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
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs
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
                .andExpect(jsonPath("$.status").value("OK"))
                .andDo(document("user-coupon-issue-sync",
                        pathParameters(
                                parameterWithName("couponId").description("발급받을 쿠폰 ID")
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
    @WithMockUser(username = "1", roles = "USER")
    void 쿠폰_발급_성공_async() throws Exception {
        // Given
        doNothing().when(userCouponService).createUserCouponAsync(eq(USER_ID), eq(COUPON_ID));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v2/coupons/{couponId}/issue", COUPON_ID)
                        .header("X-User_Id", String.valueOf(USER_ID)))
                .andDo(print())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.code").value(202))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andDo(document("user-coupon-issue-async",
                        pathParameters(
                                parameterWithName("couponId").description("발급받을 쿠폰 ID")
                        ),
                        requestHeaders(
                                headerWithName("X-User_Id").description("요청하는 사용자의 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드 (202 Accepted)"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지 (요청 접수)"),
                                fieldWithPath("data").description("응답 데이터 (null)").optional()
                        )
                ));
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
                .andExpect(jsonPath("$.data.content[0].name").value("내 쿠폰 1"))
                .andDo(document("user-coupon-find-list",
                        queryParameters(
                                parameterWithName("status").description("조회할 쿠폰 상태 (UNUSED, USED, EXPIRED)").optional(),
                                parameterWithName("page").description("페이지 번호 (1부터 시작, 기본값 1)").optional(),
                                parameterWithName("size").description("페이지당 항목 수 (기본값 10)").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.content[]").description("사용자 쿠폰 목록"),
                                fieldWithPath("data.content[].id").description("사용자 쿠폰 ID"),
                                fieldWithPath("data.content[].status").description("쿠폰 상태 (UNUSED, USED, EXPIRED)"),
                                fieldWithPath("data.content[].discountAmount").description("할인 금액 (정액)").optional(),
                                fieldWithPath("data.content[].discountRate").description("할인율 (정률)").optional(),
                                fieldWithPath("data.content[].name").description("쿠폰 이름"),
                                fieldWithPath("data.content[].description").description("쿠폰 설명").optional(),
                                fieldWithPath("data.content[].expiryDate").description("쿠폰 만료일"),
                                fieldWithPath("data.content[].minOrderAmount").description("최소 주문 금액").optional(),
                                fieldWithPath("data.content[].maxDiscountAmount").description("최대 할인 금액").optional(),
                                // Page 정보 필드 추가
                                fieldWithPath("data.pageable.pageNumber").description("현재 페이지 번호 (0부터 시작)"),
                                fieldWithPath("data.pageable.pageSize").description("페이지당 항목 수"),
                                fieldWithPath("data.pageable.sort.empty").description("정렬 정보 없음 여부"),
                                fieldWithPath("data.pageable.sort.sorted").description("정렬됨 여부"),
                                fieldWithPath("data.pageable.sort.unsorted").description("정렬 안됨 여부"),
                                fieldWithPath("data.pageable.offset").description("현재 페이지 시작 오프셋"),
                                fieldWithPath("data.pageable.unpaged").description("페이징 안됨 여부"),
                                fieldWithPath("data.pageable.paged").description("페이징 됨 여부"),
                                fieldWithPath("data.last").description("마지막 페이지 여부"),
                                fieldWithPath("data.totalPages").description("총 페이지 수"),
                                fieldWithPath("data.totalElements").description("총 항목 수"),
                                fieldWithPath("data.size").description("현재 페이지 크기"),
                                fieldWithPath("data.number").description("현재 페이지 번호 (0부터 시작)"),
                                fieldWithPath("data.sort.empty").description("정렬 정보 없음 여부"),
                                fieldWithPath("data.sort.sorted").description("정렬됨 여부"),
                                fieldWithPath("data.sort.unsorted").description("정렬 안됨 여부"),
                                fieldWithPath("data.first").description("첫 페이지 여부"),
                                fieldWithPath("data.numberOfElements").description("현재 페이지 항목 수"),
                                fieldWithPath("data.empty").description("현재 페이지 비어있음 여부")
                        )
                ));
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
                .andExpect(jsonPath("$.data.code").value(couponCode))
                .andDo(document("user-coupon-find-code",
                        pathParameters(
                                parameterWithName("userCouponId").description("조회할 사용자 쿠폰 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.code").description("쿠폰 사용 코드 (UUID)")
                        )
                ));
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
                .andExpect(jsonPath("$.data.name").value("사용될 쿠폰"))
                .andDo(document("user-coupon-use",
                        requestFields(
                                fieldWithPath("code").description("사용할 쿠폰 코드 (UUID)")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("사용한 사용자 쿠폰 ID"),
                                fieldWithPath("data.discountAmount").description("할인 금액 (정액)").optional(),
                                fieldWithPath("data.discountRate").description("할인율 (정률)").optional(),
                                fieldWithPath("data.name").description("사용한 쿠폰 이름"),
                                fieldWithPath("data.description").description("사용한 쿠폰 설명").optional(),
                                fieldWithPath("data.minOrderAmount").description("최소 주문 금액").optional(),
                                fieldWithPath("data.maxDiscountAmount").description("최대 할인 금액").optional()
                        )
                ));
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
                .andExpect(jsonPath("$.status").value("OK"))
                .andDo(document("user-coupon-expire-notify"));
    }
}