package com.couponmoa.backend.couponmoastore.domain.store.controller.v1;

import com.couponmoa.backend.couponmoastore.common.dto.ApiResponse;
import com.couponmoa.backend.couponmoastore.domain.store.dto.request.StoreCursor;
import com.couponmoa.backend.couponmoastore.domain.store.dto.request.StoreRequestDto;
import com.couponmoa.backend.couponmoastore.domain.store.dto.response.StoreResponseDto;
import com.couponmoa.backend.couponmoastore.domain.store.dto.response.StoreSimpleResponse;
import com.couponmoa.backend.couponmoastore.domain.store.service.v1.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
public class StoreController {

    private final StoreService storeServiceV1;
//    private final UserFindByIdTestService testService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<StoreResponseDto>> createStore(
            @Valid @RequestBody StoreRequestDto request,
            @RequestHeader("X-User-Id") Long userId) {
        StoreResponseDto response = storeServiceV1.createStore(request, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StoreResponseDto>>> findStoresByKeyword(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long storeId,
            @RequestParam(defaultValue = "10") int size) {

        StoreCursor cursor = (keyword != null || storeId != null)
                ? new StoreCursor(keyword, storeId)
                : null;

        List<StoreResponseDto> stores = storeServiceV1.findStoresByKeyword(cursor, size);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<StoreResponseDto>>> findMyStores(
            @RequestHeader("X-User-Id") Long userId) {

        List<StoreResponseDto> stores = storeServiceV1.findMyStores(userId);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @GetMapping("/my/simple")
    public ResponseEntity<ApiResponse<List<StoreSimpleResponse>>> findMySimpleStores(
            @RequestHeader("X-User-Id") Long userId) { // TODO: 여쭤보기

        List<StoreSimpleResponse> stores = storeServiceV1.findMySimpleStores(userId);
        return ResponseEntity.ok(ApiResponse.success(stores));
    }

    @GetMapping("/{storeId}")
    public ResponseEntity<ApiResponse<StoreResponseDto>> findStore(
            @PathVariable Long storeId) {

        StoreResponseDto store = storeServiceV1.findStore(storeId);
        return ResponseEntity.ok(ApiResponse.success(store));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{storeId}")
    public ResponseEntity<ApiResponse<StoreResponseDto>> updateStore(
            @PathVariable Long storeId,
            @Valid @RequestBody StoreRequestDto request,
            @RequestHeader("X-User-Id") Long userId) {

        StoreResponseDto updatedStore = storeServiceV1.updateStore(storeId, request, userId);
        return ResponseEntity.ok(ApiResponse.success(updatedStore));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{storeId}")
    public ResponseEntity<Void> deleteStore(
            @PathVariable Long storeId,
            @RequestHeader("X-User-Id") Long userId) {

        storeServiceV1.deleteStore(storeId, userId);
        return ResponseEntity.noContent().build();
    }

//    @GetMapping("/test/user")
//    public ResponseEntity<String> measureFindById() {
//        long result = testService.measureFindById();
//        return ResponseEntity.ok().body(result + "초 소요");
//    }
}