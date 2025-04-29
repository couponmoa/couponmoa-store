package com.couponmoa.backend.couponmoastore.domain.store.controller.v2;

import com.couponmoa.backend.couponmoastore.common.dto.ApiResponse;
import com.couponmoa.backend.couponmoastore.domain.store.dto.request.StoreCursor;
import com.couponmoa.backend.couponmoastore.domain.store.dto.request.StoreRequestDto;
import com.couponmoa.backend.couponmoastore.domain.store.dto.response.StoreResponseDto;
import com.couponmoa.backend.couponmoastore.domain.store.dto.response.StoreSimpleResponse;
import com.couponmoa.backend.couponmoastore.domain.store.service.test.UserFindByIdTestService;
import com.couponmoa.backend.couponmoastore.domain.store.service.v2.StoreServiceV2;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "스토어 API V2", description = "스토어 관련 기능을 제공하는 API (커서 기반 조회) + 캐싱")
@RestController
@RequestMapping("/api/v2/stores")
@RequiredArgsConstructor
public class StoreControllerV2 {

    private final StoreServiceV2 storeServiceV2;
    private final UserFindByIdTestService testService;

    @Operation(summary = "스토어 생성", description = "사용자가 새로운 스토어를 생성함")
//    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<StoreResponseDto>> createStore(
            @Valid @RequestBody StoreRequestDto request,
            @RequestHeader("X-User-Id") Long userId) {
        StoreResponseDto response = storeServiceV2.createStore(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "키워드로 스토어 조회 (커서 방식)", description = "스토어 이름 기준 키워드로 검색하며, 커서 방식으로 페이징 처리")
    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreResponseDto>>> findStoresByKeyword(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "10") int size) {

        StoreCursor cursor = (keyword != null || storeId != null)
                ? new StoreCursor(keyword, storeId)
                : null;

        List<StoreResponseDto> stores = storeServiceV2.findStoresByKeyword(cursor, size);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @Operation(summary = "내 스토어 목록 조회", description = "현재 로그인한 유저의 스토어들을 반환함")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<StoreResponseDto>>> findMyStores(
            @RequestHeader("X-User-Id") Long userId) {

        List<StoreResponseDto> stores = storeServiceV2.findMyStores(userId);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @Operation(summary = "내 간단한 스토어 목록 조회", description = "스토어 id와 이름만 반환")
    @GetMapping("/my/simple")
    public ResponseEntity<ApiResponse<List<StoreSimpleResponse>>> findMySimpleStores(
            @RequestHeader("X-User-Id") Long userId) { // TODO: 여쭤보기

        List<StoreSimpleResponse> stores = storeServiceV2.findMySimpleStores(userId);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @Operation(summary = "스토어 단건 조회", description = "스토어 ID로 상세 정보를 조회")
    @GetMapping("/{storeId}")
    public ResponseEntity<ApiResponse<StoreResponseDto>> findStore(
            @PathVariable Long storeId) {

        StoreResponseDto store = storeServiceV2.findStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(store));
    }

    @Operation(summary = "스토어 수정", description = "사용자가 자신의 스토어 정보를 수정함")
//    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{storeId}")
    public ResponseEntity<ApiResponse<StoreResponseDto>> updateStore(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreRequestDto request,
            @RequestHeader("X-User-Id") Long userId) {

        StoreResponseDto updatedStore = storeServiceV2.updateStore(storeId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(updatedStore));
    }

    @Operation(summary = "스토어 삭제", description = "사용자가 자신의 스토어를 삭제함")
//    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(
            @PathVariable Long storeId,
            @RequestHeader("X-User-Id") Long userId) {

        storeServiceV2.deleteStore(storeId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<String> measureFindById() {
        long result = testService.measureFindById();
        return ResponseEntity.ok().body(result + "초 소요");
    }
}