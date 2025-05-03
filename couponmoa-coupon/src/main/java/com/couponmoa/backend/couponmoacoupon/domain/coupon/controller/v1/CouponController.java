package com.couponmoa.backend.couponmoacoupon.domain.coupon.controller.v1;

import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponCreateRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponCursor;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponSearchByStoreRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.request.CouponUpdateRequest;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponDetailResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponIdResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.dto.response.CouponSimpleResponse;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.enums.CouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.service.v1.CouponService;
import com.couponmoa.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/coupons")
@RequiredArgsConstructor
@Slf4j
public class CouponController {

    private final CouponService couponService;
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ApiResponse<CouponIdResponse>> createCoupon(
            @Valid @RequestBody CouponCreateRequest requestDto) {

        CouponIdResponse responseDto = couponService.createCoupon(requestDto);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    @GetMapping
    public ApiResponse<List<CouponSimpleResponse>> findCouponsByKeyword(
            @RequestParam(required = false) CouponStatus status,
            @RequestParam(required = false) BigDecimal issuedQuantity,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long id,
            @RequestParam(defaultValue = "10") int size) {

        CouponCursor cursor = (issuedQuantity != null || keyword != null || id != null)
                ? new CouponCursor(issuedQuantity, keyword, id)
                : null;

        List<CouponSimpleResponse> coupons = couponService.findCouponsByKeyword(status, cursor, size);
        return ApiResponse.success(coupons);
    }

    @GetMapping("/store/{storeId}")
    public ApiResponse<Page<CouponSimpleResponse>> findCouponsByStore(
            @PathVariable Long storeId,
            @ModelAttribute CouponSearchByStoreRequest requestDto,
            @Parameter(description = "페이지당 쿠폰 수", example = "10")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "조회할 페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page) {

        Page<CouponSimpleResponse> coupons = couponService.findCouponsByStore(storeId, requestDto, size, page);
        return ApiResponse.success(coupons);
    }

    @GetMapping("/{couponId}")
    public ApiResponse<CouponDetailResponse> findCoupon(
            @PathVariable Long couponId,
            @RequestHeader("X-User-Id") Long userId) {

        CouponDetailResponse couponDetail = couponService.findCoupon(couponId, userId);
        return ApiResponse.success(couponDetail);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{couponId}")
    public ResponseEntity<ApiResponse<CouponIdResponse>> updateCoupon(
            @PathVariable Long couponId,
            @Valid @RequestBody CouponUpdateRequest requestDto) {

        CouponIdResponse responseDto = couponService.updateCoupon(couponId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{couponId}")
    public ResponseEntity<Void> deleteCoupon(
            @PathVariable Long couponId) {

        couponService.deleteCoupon(couponId);
        return ResponseEntity.noContent().build();
    }
}
