package com.couponmoa.backend.couponmoacoupon.domain.usercoupon.controller;

import com.couponmoa.common.*;
import com.couponmoa.backend.couponmoacoupon.domain.user.enums.UserRole;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.request.UserCouponRequest;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponCodeResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponUseResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.enums.UserCouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service.UserCouponAsyncService;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service.UserCouponService;
import com.couponmoa.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@Tag(name = "사용자 쿠폰 API", description = "쿠폰을 발급받고, 발급받은 쿠폰을 관리할 수 있는 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserCouponController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;

    private final UserCouponService userCouponService;
    private final UserCouponAsyncService userCouponAsyncService;

    @Operation(summary = "쿠폰 발급 (동기)")
    @Secured(UserRole.Authority.USER)
    @PostMapping("/v1/coupons/{couponId}/issue")
    public ApiResponse<Void> createUserCouponSync(
            @RequestHeader("X-User_Id") Long userId,
            @PathVariable Long couponId
    ) {
        userCouponService.createUserCouponSync(userId, couponId);
        return ApiResponse.success();
    }

    @Operation(summary = "쿠폰 발급 (비동기)")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Secured(UserRole.Authority.USER)
    @PostMapping("/v2/coupons/{couponId}/issue")
    public ApiResponse<Void> createUserCouponAsync(
            @RequestHeader("X-User_Id") Long userId,
            @PathVariable Long couponId
    ) {
        userCouponService.createUserCouponAsync(userId, couponId);
        return ApiResponse.of(HttpStatus.ACCEPTED, "쿠폰 발급 요청이 접수되었습니다.", null);
    }

    @Operation(summary = "발급받은 쿠폰 목록 조회")
    @Secured(UserRole.Authority.USER)
    @GetMapping("/v1/user-coupons")
    public ApiResponse<Page<UserCouponResponse>> findUserCoupons(
            @RequestHeader("X-User_Id") Long userId,
            @RequestParam(required = false) UserCouponStatus status,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) @Min(1) Integer page,
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) @Min(1) Integer size
    ) {
        Page<UserCouponResponse> response = userCouponService.findUserCoupons(userId, status, page, size);
        return ApiResponse.success(response);
    }

    @Operation(summary = "쿠폰 코드 조회")
    @Secured(UserRole.Authority.USER)
    @GetMapping("/v1/user-coupons/{userCouponId}/code")
    public ApiResponse<UserCouponCodeResponse> findUserCouponCode(
            @RequestHeader("X-User_Id") Long userId,
            @PathVariable Long userCouponId
    ) {
        UserCouponCodeResponse response = userCouponService.findUserCouponCode(userId, userCouponId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "쿠폰 사용 처리")
    @Secured(UserRole.Authority.ADMIN)
    @PostMapping("/v1/user-coupons/use")
    public ApiResponse<UserCouponUseResponse> useUserCoupon(
            @RequestHeader("X-User_Id") Long userId,
            @Valid @RequestBody UserCouponRequest request
    ) {
        UserCouponUseResponse response = userCouponService.useUserCoupon(userId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "발급 쿠폰 만료 알림 전송", description = "만료 알림 조회 및 알림 서버로 전송(스케줄링 서버에서 요청하는 api)")
    @PostMapping("/v1/user-coupons/expire")
    public ResponseEntity<ApiResponse<Void>> NotifyCouponExpire() {
        userCouponService.sendExpireCouponNotifications();
        return ResponseEntity.ok(ApiResponse.success());
    }
}
