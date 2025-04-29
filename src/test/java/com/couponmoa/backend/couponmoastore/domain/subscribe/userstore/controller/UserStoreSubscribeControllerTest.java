package com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.controller;

import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.dto.response.FindStoreSubscribeListResponse;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.service.UserStoreSubscribeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserStoreSubscribeController.class)
public class UserStoreSubscribeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserStoreSubscribeService userStoreSubServ;

    @Test
    void 가게_구독() throws Exception {
        Long userId = 1L;
        Long storeId = 1L;
        willDoNothing().given(userStoreSubServ).subscribeStore(anyLong(),anyLong());
        mockMvc.perform(post("/api/v1/stores/{storeId}/subscriptions",storeId)
                        .header("X-User-Id", String.valueOf(userId)))
                .andExpect(status().isOk());
    }

    @Test
    void 가게_구독_취소() throws Exception {
        Long userId = 1L;
        Long storeId = 1L;
        willDoNothing().given(userStoreSubServ).unSubscribeCoupon(anyLong(),anyLong());
        mockMvc.perform(post("/api/v1/stores/{storeId}/unsubscriptions",storeId)
                        .header("X-User-Id", String.valueOf(userId)))
                .andExpect(status().isOk());
    }

    @Test
    void 가게_구독_목록_확인() throws Exception {
        Long userId = 1L;
        Long storeId = 1L;
        FindStoreSubscribeListResponse response = new FindStoreSubscribeListResponse(
                storeId,"name","desc","address");
        List<FindStoreSubscribeListResponse> subList = List.of(response);

        given(userStoreSubServ.findSubscribeList(anyLong(),anyInt(),anyInt())).willReturn(subList);
        mockMvc.perform(get("/api/v1/stores/subscriptions")
                        .header("X-User-Id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void 알림_서비스() throws Exception {
        Long userId = 1L;
        Long storeId = 1L;
        List<String> emailList = List.of("email1@test.com","email2@test.com");
        given(userStoreSubServ.sendToSQS(anyLong())).willReturn(emailList);
        mockMvc.perform(post("/api/v1/stores/{storeId}/alert",storeId))
                .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
