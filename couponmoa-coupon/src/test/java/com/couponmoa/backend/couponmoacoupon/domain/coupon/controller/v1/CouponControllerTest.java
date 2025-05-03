package com.couponmoa.backend.couponmoacoupon.domain.coupon.controller.v1;

import com.couponmoa.backend.couponmoacoupon.config.TestSecurityConfig;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponCreateRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponSearchByStoreRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponUpdateRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponDetailResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponIdResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponSimpleResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.enums.CouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.service.v1.CouponService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs
@WebMvcTest(CouponController.class)
@Import(TestSecurityConfig.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CouponService couponService;

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
        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/coupons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.id").value(1L))
                .andDo(document("coupon-create",
                        requestFields(
                                fieldWithPath("name").description("쿠폰 이름"),
                                fieldWithPath("totalQuantity").description("총 발급 수량"),
                                fieldWithPath("discountAmount").description("할인 금액 (정액)"),
                                fieldWithPath("discountRate").description("할인율 (정률, 0~100)"),
                                fieldWithPath("minOrderAmount").description("최소 주문 금액").optional(),
                                fieldWithPath("maxDiscountAmount").description("최대 할인 금액").optional(),
                                fieldWithPath("description").description("쿠폰 설명").optional(),
                                fieldWithPath("startDate").description("쿠폰 발급 시작일 (yyyy-MM-ddTHH:mm:ss)").optional(),
                                fieldWithPath("endDate").description("쿠폰 발급 종료일 (yyyy-MM-ddTHH:mm:ss)").optional(),
                                fieldWithPath("expiryDate").description("쿠폰 만료일 (yyyy-MM-ddTHH:mm:ss)").optional(),
                                fieldWithPath("storeId").description("쿠폰을 발급하는 스토어 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("생성된 쿠폰 ID")
                        )
                ));
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
                        .startDate(LocalDateTime.parse("2025-04-28T22:26:00"))
                        .endDate(LocalDateTime.parse("2025-05-04T22:26:00"))
                        .status(CouponStatus.IN_PROGRESS)
                        .build()
        );
        when(couponService.findCouponsByKeyword(any(), any(), anyInt())).thenReturn(coupons);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/coupons")
                        .param("keyword", "키워드")
                        .param("size", "5")
                        .param("id", "10")
                        .param("issuedQuantity", "50")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data[0].id").value(2L))
                .andExpect(jsonPath("$.data[0].name").value("키워드 테스트 쿠폰"))
                .andDo(document("coupon-find-by-keyword",
                        queryParameters(
                                parameterWithName("keyword").description("검색 키워드").optional(),
                                parameterWithName("size").description("가져올 쿠폰 개수 (기본값 10)").optional(),
                                parameterWithName("id").description("마지막으로 조회된 쿠폰 ID (커서)").optional(),
                                parameterWithName("issuedQuantity").description("마지막으로 조회된 쿠폰 발급수량 (커서)").optional(),
                                parameterWithName("status").description("쿠폰 상태 (UPCOMING, IN_PROGRESS, ENDED)").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data[]").description("쿠폰 목록"),
                                fieldWithPath("data[].id").description("쿠폰 ID"),
                                fieldWithPath("data[].name").description("쿠폰 이름"),
                                fieldWithPath("data[].discountAmount").description("할인 금액 (정액)"),
                                fieldWithPath("data[].discountRate").description("할인율 (정률)"),
                                fieldWithPath("data[].startDate").description("쿠폰 발급 시작일"),
                                fieldWithPath("data[].endDate").description("쿠폰 발급 종료일"),
                                fieldWithPath("data[].status").description("쿠폰 상태")
                        )
                ));
    }

    @Test
    @WithMockUser(roles = "USER")
    void 스토어별_쿠폰_조회_성공() throws Exception {
        // Given
        Long storeId = 5L;
        List<CouponSimpleResponse> content = Collections.singletonList(
                CouponSimpleResponse.builder()
                        .id(3L)
                        .name("스토어 테스트 쿠폰")
                        .discountRate(BigDecimal.TEN)
                        .discountAmount(BigDecimal.ZERO)
                        .startDate(LocalDateTime.parse("2025-04-27T22:26:00"))
                        .endDate(LocalDateTime.parse("2025-05-06T22:26:00"))
                        .status(CouponStatus.IN_PROGRESS)
                        .build()
        );
        Page<CouponSimpleResponse> couponsPage = new PageImpl<>(content, PageRequest.of(0, 10), 1);
        when(couponService.findCouponsByStore(anyLong(), any(CouponSearchByStoreRequest.class), anyInt(), anyInt())).thenReturn(couponsPage);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/coupons/stores/{storeId}", storeId)
                        .param("page", "1")
                        .param("size", "10")
                        .param("keyword", "스토어")
                        .param("status", "IN_PROGRESS")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.content[0].id").value(3L))
                .andExpect(jsonPath("$.data.content[0].name").value("스토어 테스트 쿠폰"))
                .andDo(document("coupon-find-by-store",
                        pathParameters(
                                parameterWithName("storeId").description("스토어 ID")
                        ),
                        queryParameters(
                                parameterWithName("page").description("페이지 번호 (1부터 시작, 기본값 1)").optional(),
                                parameterWithName("size").description("페이지당 항목 수 (기본값 10)").optional(),
                                parameterWithName("keyword").description("검색 키워드 (쿠폰 이름)").optional(),
                                parameterWithName("status").description("쿠폰 상태 (UPCOMING, IN_PROGRESS, ENDED)").optional(),
                                parameterWithName("discountAmount").description("할인 금액 필터").optional(),
                                parameterWithName("discountRate").description("할인율 필터").optional(),
                                parameterWithName("startDate").description("발급 시작일 필터 (yyyy-MM-ddTHH:mm:ss)").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.content[]").description("쿠폰 목록"),
                                fieldWithPath("data.content[].id").description("쿠폰 ID"),
                                fieldWithPath("data.content[].name").description("쿠폰 이름"),
                                fieldWithPath("data.content[].discountAmount").description("할인 금액 (정액)").optional(),
                                fieldWithPath("data.content[].discountRate").description("할인율 (정률)").optional(),
                                fieldWithPath("data.content[].startDate").description("쿠폰 발급 시작일"),
                                fieldWithPath("data.content[].endDate").description("쿠폰 발급 종료일"),
                                fieldWithPath("data.content[].status").description("쿠폰 상태"),
                                fieldWithPath("data.pageable.pageNumber").description("현재 페이지 번호 (0부터 시작)"),
                                fieldWithPath("data.pageable.pageSize").description("페이지당 항목 수"),
                                fieldWithPath("data.pageable.sort.empty").description("정렬 정보 없음 여부"),
                                fieldWithPath("data.pageable.sort.sorted").description("정렬됨 여부"),
                                fieldWithPath("data.pageable.sort.unsorted").description("정렬 안됨 여부"),
                                fieldWithPath("data.pageable.offset").description("현재 페이지 시작 오프셋"),
                                fieldWithPath("data.pageable.paged").description("페이징 됨 여부"),
                                fieldWithPath("data.pageable.unpaged").description("페이징 안됨 여부"),
                                fieldWithPath("data.totalElements").description("총 항목 수"),
                                fieldWithPath("data.totalPages").description("총 페이지 수"),
                                fieldWithPath("data.last").description("마지막 페이지 여부"),
                                fieldWithPath("data.size").description("현재 페이지 크기"),
                                fieldWithPath("data.number").description("현재 페이지 번호 (0부터 시작)"),
                                fieldWithPath("data.sort.empty").description("정렬 정보 없음 여부"),
                                fieldWithPath("data.sort.sorted").description("정렬됨 여부"),
                                fieldWithPath("data.sort.unsorted").description("정렬 안됨 여부"),
                                fieldWithPath("data.numberOfElements").description("현재 페이지 항목 수"),
                                fieldWithPath("data.first").description("첫 페이지 여부"),
                                fieldWithPath("data.empty").description("현재 페이지 비어있음 여부")
                        )
                ));
    }

    @Test
    @WithMockUser(roles = "USER")
    void 쿠폰_상세_조회_성공() throws Exception {
        // Given
        Long couponId = 1L;
        LocalDateTime now = LocalDateTime.now();
        CouponDetailResponse detailResponse = CouponDetailResponse.builder()
                .id(couponId)
                .name("상세 테스트 쿠폰")
                .totalQuantity(100)
                .availableQuantity(80)
                .discountAmount(BigDecimal.valueOf(5000))
                .status(CouponStatus.IN_PROGRESS)
                .startDate(now.minusDays(1))
                .endDate(now.plusDays(10))
                .expiryDate(now.plusMonths(2))
                .build();
        when(couponService.findCoupon(eq(couponId), anyLong())).thenReturn(detailResponse);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.get("/api/v1/coupons/{couponId}", couponId)
                        .header("X-User-Id", "2")
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.id").value(couponId))
                .andExpect(jsonPath("$.data.name").value("상세 테스트 쿠폰"))
                .andDo(document("coupon-find-one",
                        pathParameters(
                                parameterWithName("couponId").description("조회할 쿠폰 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("쿠폰 ID"),
                                fieldWithPath("data.name").description("쿠폰 이름"),
                                fieldWithPath("data.totalQuantity").description("총 발급 수량"),
                                fieldWithPath("data.availableQuantity").description("남은 수량"),
                                fieldWithPath("data.discountAmount").description("할인 금액 (정액)").optional(),
                                fieldWithPath("data.discountRate").description("할인율 (정률)").optional(),
                                fieldWithPath("data.minOrderAmount").description("최소 주문 금액").optional(),
                                fieldWithPath("data.maxDiscountAmount").description("최대 할인 금액").optional(),
                                fieldWithPath("data.description").description("쿠폰 설명").optional(),
                                fieldWithPath("data.startDate").description("쿠폰 발급 시작일"),
                                fieldWithPath("data.endDate").description("쿠폰 발급 종료일"),
                                fieldWithPath("data.expiryDate").description("쿠폰 만료일"),
                                fieldWithPath("data.createdAt").description("쿠폰 생성일").optional(),
                                fieldWithPath("data.modifiedAt").description("쿠폰 수정일").optional(),
                                fieldWithPath("data.status").description("쿠폰 상태")
                        )
                ));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 쿠폰_수정_성공() throws Exception {
        // Given
        Long couponId = 1L;
        CouponUpdateRequest request = CouponUpdateRequest.builder()
                .name("수정된 관리자 쿠폰")
                .newTotalQuantity(500)
                .endDate(LocalDateTime.now().plusDays(20))
                .storeId(1L)
                .build();

        CouponIdResponse response = new CouponIdResponse(couponId);
        when(couponService.updateCoupon(eq(couponId), any(CouponUpdateRequest.class))).thenReturn(response);

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.put("/api/v1/coupons/{couponId}", couponId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.id").value(couponId))
                .andDo(document("coupon-update",
                        pathParameters(
                                parameterWithName("couponId").description("수정할 쿠폰 ID")
                        ),
                        requestFields(
                                fieldWithPath("name").description("수정할 쿠폰 이름").optional(),
                                fieldWithPath("newTotalQuantity").description("수정할 총 발급 수량").optional(),
                                fieldWithPath("discountAmount").description("수정할 할인 금액 (정액)").optional(),
                                fieldWithPath("discountRate").description("수정할 할인율 (정률)").optional(),
                                fieldWithPath("minOrderAmount").description("수정할 최소 주문 금액").optional(),
                                fieldWithPath("maxDiscountAmount").description("수정할 최대 할인 금액").optional(),
                                fieldWithPath("description").description("수정할 쿠폰 설명").optional(),
                                fieldWithPath("startDate").description("수정할 쿠폰 발급 시작일 (yyyy-MM-ddTHH:mm:ss)").optional(),
                                fieldWithPath("endDate").description("수정할 쿠폰 발급 종료일 (yyyy-MM-ddTHH:mm:ss)").optional(),
                                fieldWithPath("expiryDate").description("수정할 쿠폰 만료일 (yyyy-MM-ddTHH:mm:ss)").optional(),
                                fieldWithPath("storeId").description("쿠폰의 스토어 ID (변경 불가, 검증용)").optional()
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("수정된 쿠폰 ID")
                        )
                ));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void 쿠폰_삭제_성공() throws Exception {
        // Given
        Long couponId = 1L;
        doNothing().when(couponService).deleteCoupon(eq(couponId));

        // When & Then
        mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/coupons/{couponId}", couponId)
                )
                .andDo(print())
                .andExpect(status().isNoContent())
                .andDo(document("coupon-delete",
                        pathParameters(
                                parameterWithName("couponId").description("삭제할 쿠폰 ID"))));
    }
}