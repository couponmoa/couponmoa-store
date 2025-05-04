package com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.controller;

import com.couponmoa.backend.couponmoastore.config.TestSecurityConfig;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.dto.response.FindStoreSubscribeListResponse;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.service.UserStoreSubscribeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs
@WebMvcTest(UserStoreSubscribeController.class)
@Import(TestSecurityConfig.class)
public class UserStoreSubscribeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserStoreSubscribeService userStoreSubscribeService;

    @Test
    @WithMockUser
    void 가게_구독() throws Exception {
        Long userId = 1L;
        Long storeId = 1L;
        willDoNothing().given(userStoreSubscribeService).subscribeStore(anyLong(), anyLong());
        mockMvc.perform(post("/api/v1/stores/{storeId}/subscriptions", storeId)
                        .header("X-User-Id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andDo(document("subscribe-store",
                        pathParameters(
                                parameterWithName("storeId").description("구독할 스토어 ID")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 가게_구독_취소() throws Exception {
        Long userId = 1L;
        Long storeId = 1L;
        willDoNothing().given(userStoreSubscribeService).unSubscribeCoupon(anyLong(), anyLong());
        mockMvc.perform(post("/api/v1/stores/{storeId}/unsubscriptions", storeId)
                        .header("X-User-Id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andDo(document("unsubscribe-store",
                        pathParameters(
                                parameterWithName("storeId").description("구독 취소할 스토어 ID")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 가게_구독_목록_확인() throws Exception {
        Long userId = 1L;
        Long storeId = 1L;
        FindStoreSubscribeListResponse response = new FindStoreSubscribeListResponse(
                storeId, "name", "desc", "address");
        List<FindStoreSubscribeListResponse> subList = List.of(response);

        given(userStoreSubscribeService.findSubscribeList(anyLong(), anyInt(), anyInt())).willReturn(subList);
        mockMvc.perform(get("/api/v1/stores/subscriptions")
                        .header("X-User-Id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andDo(document("store-subscription-list",
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data[].id").description("스토어 ID"),
                                fieldWithPath("data[].name").description("스토어 이름"),
                                fieldWithPath("data[].description").description("스토어 설명"),
                                fieldWithPath("data[].address").description("스토어 주소")
                        )
                ));
    }

    @Test
    @WithMockUser
    void 알림_서비스() throws Exception {
        Long storeId = 1L;
        List<String> emailList = List.of("email1@test.com", "email2@test.com");
        given(userStoreSubscribeService.sendToSQS(anyLong())).willReturn(emailList);
        mockMvc.perform(post("/api/v1/stores/{storeId}/alert", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andDo(document("store-subscription-alert",
                        pathParameters(
                                parameterWithName("storeId").description("알림 전송할 스토어 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data").description("알림 발송 대상 사용자 이메일 리스트")
                        )
                ));
    }
}
