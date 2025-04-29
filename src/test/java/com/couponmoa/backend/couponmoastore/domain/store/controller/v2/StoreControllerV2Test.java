package com.couponmoa.backend.couponmoastore.domain.store.controller.v2;

import com.couponmoa.backend.couponmoastore.domain.store.dto.request.StoreCursor;
import com.couponmoa.backend.couponmoastore.domain.store.dto.request.StoreRequestDto;
import com.couponmoa.backend.couponmoastore.domain.store.dto.response.StoreResponseDto;
import com.couponmoa.backend.couponmoastore.domain.store.dto.response.StoreSimpleResponse;
import com.couponmoa.backend.couponmoastore.domain.store.service.v2.StoreServiceV2;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureRestDocs
@WebMvcTest(StoreControllerV2.class)
public class StoreControllerV2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StoreServiceV2 storeServiceV2;

    @Test
    void 스토어_생성() throws Exception {
        Long userId = 1L;
        StoreRequestDto request = new StoreRequestDto("name", "description", "address");
        StoreResponseDto response = new StoreResponseDto(userId, "name", "description", "address");

        given(storeServiceV2.createStore(any(StoreRequestDto.class), anyLong())).willReturn(response);

        mockMvc.perform(post("/api/v2/stores")
                        .header("X-User-Id", String.valueOf(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.name").value("name"))
                .andExpect(jsonPath("$.data.description").value("description"))
                .andDo(document("store-create",
                        requestFields(
                                fieldWithPath("name").description("스토어 이름"),
                                fieldWithPath("description").description("스토어 설명"),
                                fieldWithPath("address").description("스토어 주소")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("스토어 ID"),
                                fieldWithPath("data.name").description("스토어 이름"),
                                fieldWithPath("data.description").description("스토어 설명"),
                                fieldWithPath("data.address").description("스토어 주소")
                        )
                ));
    }

    @Test
    void 키워드로_스토어_조회_커서() throws Exception {
        Long userId = 1L;
        Long storeId = 1L;
        String keyword = "keyword";
        StoreResponseDto response = new StoreResponseDto(userId, "name", "description", "address");
        List<StoreResponseDto> stores = List.of(response);

        given(storeServiceV2.findStoresByKeyword(any(StoreCursor.class), anyInt())).willReturn(stores);

        mockMvc.perform(get("/api/v2/stores")
                        .param("keyword", keyword)
                        .param("storeId", String.valueOf(storeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andDo(document("store-find-by-keyword",
                        queryParameters(
                                parameterWithName("keyword").description("검색 키워드").optional(),
                                parameterWithName("storeId").description("스토어 ID (커서)").optional()
                        ),
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
    void 키워드로_스토어_조회_커서_keyword_null() throws Exception {
        Long userId = 1L;
        Long storeId = 1L;
        StoreResponseDto response = new StoreResponseDto(userId, "name", "description", "address");
        List<StoreResponseDto> stores = List.of(response);

        given(storeServiceV2.findStoresByKeyword(any(StoreCursor.class), anyInt())).willReturn(stores);

        mockMvc.perform(get("/api/v2/stores")
                        .param("storeId", String.valueOf(storeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void 키워드로_스토어_조회_커서_null() throws Exception {
        given(storeServiceV2.findStoresByKeyword(null, 10)).willReturn(List.of());

        mockMvc.perform(get("/api/v2/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void 내_스토어_목록_조회() throws Exception {
        Long userId = 1L;
        StoreResponseDto response = new StoreResponseDto(userId, "name", "description", "address");
        List<StoreResponseDto> stores = List.of(response);

        given(storeServiceV2.findMyStores(anyLong())).willReturn(stores);

        mockMvc.perform(get("/api/v2/stores/my")
                        .header("X-User-Id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andDo(document("store-find-my",
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
    void 내_간단_스토어_목록_조회() throws Exception {
        Long userId = 1L;
        StoreSimpleResponse response = new StoreSimpleResponse(userId, "name");
        List<StoreSimpleResponse> stores = List.of(response);

        given(storeServiceV2.findMySimpleStores(anyLong())).willReturn(stores);

        mockMvc.perform(get("/api/v2/stores/my/simple")
                        .header("X-User-Id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andDo(document("store-find-my-simple",
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data[].id").description("스토어 ID"),
                                fieldWithPath("data[].name").description("스토어 이름")
                        )
                ));
    }

    @Test
    void 스토어_단건_조회() throws Exception {
        Long storeId = 1L;
        StoreResponseDto response = new StoreResponseDto(storeId, "name", "description", "address");

        given(storeServiceV2.findStore(anyLong())).willReturn(response);

        mockMvc.perform(get("/api/v2/stores/{storeId}", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(storeId))
                .andExpect(jsonPath("$.data.name").value("name"))
                .andDo(document("store-find-one",
                        pathParameters(
                                parameterWithName("storeId").description("스토어 ID")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("스토어 ID"),
                                fieldWithPath("data.name").description("스토어 이름"),
                                fieldWithPath("data.description").description("스토어 설명"),
                                fieldWithPath("data.address").description("스토어 주소")
                        )
                ));
    }

    @Test
    void 스토어_수정() throws Exception {
        Long storeId = 1L;
        Long userId = 1L;
        StoreRequestDto request = new StoreRequestDto("name", "description", "address");
        StoreResponseDto response = new StoreResponseDto(storeId, "name", "description", "address");

        given(storeServiceV2.updateStore(anyLong(), any(StoreRequestDto.class), anyLong())).willReturn(response);

        mockMvc.perform(put("/api/v2/stores/{storeId}", storeId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .header("X-User-Id", String.valueOf(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(userId))
                .andExpect(jsonPath("$.data.name").value("name"))
                .andExpect(jsonPath("$.data.description").value("description"))
                .andDo(document("store-update",
                        pathParameters(
                                parameterWithName("storeId").description("스토어 ID")
                        ),
                        requestFields(
                                fieldWithPath("name").description("수정할 스토어 이름"),
                                fieldWithPath("description").description("수정할 스토어 설명"),
                                fieldWithPath("address").description("수정할 스토어 주소")
                        ),
                        responseFields(
                                fieldWithPath("code").description("응답 코드"),
                                fieldWithPath("status").description("응답 상태"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.id").description("스토어 ID"),
                                fieldWithPath("data.name").description("스토어 이름"),
                                fieldWithPath("data.description").description("스토어 설명"),
                                fieldWithPath("data.address").description("스토어 주소")
                        )
                ));
    }

    @Test
    void 스토어_삭제() throws Exception {
        Long storeId = 1L;
        Long userId = 1L;

        willDoNothing().given(storeServiceV2).deleteStore(anyLong(),anyLong());

        mockMvc.perform(delete("/api/v2/stores/{storeId}", storeId)
                        .header("X-User-Id", String.valueOf(userId)))
                .andExpect(status().isNoContent())
                .andDo(document("store-delete",
                        pathParameters(
                                parameterWithName("storeId").description("삭제할 스토어 ID")
                        )
                ));
    }
}
