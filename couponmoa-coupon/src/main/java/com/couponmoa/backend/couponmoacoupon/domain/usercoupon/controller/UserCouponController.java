package com.couponmoa.backend.couponmoacoupon.domain.usercoupon.controller;

import com.couponmoa.backend.couponmoacoupon.domain.user.enums.UserRole;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.request.UserCouponRequest;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponCodeResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.dto.response.UserCouponUseResponse;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.enums.UserCouponStatus;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service.UserCouponAsyncService;
import com.couponmoa.backend.couponmoacoupon.domain.usercoupon.service.UserCouponService;
import com.couponmoa.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserCouponController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;

    private final UserCouponService userCouponService;
    private final UserCouponAsyncService userCouponAsyncService;

    @Secured(UserRole.Authority.USER)
    @PostMapping("/v1/coupons/{couponId}/issue")
    public ApiResponse<Void> createUserCouponSync(
            @RequestHeader("X-User_Id") Long userId,
            @PathVariable Long couponId
    ) {
        userCouponService.createUserCouponSync(userId, couponId);
        return ApiResponse.success();
    }

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

    @Secured(UserRole.Authority.USER)
    @GetMapping("/v1/user-coupons/{userCouponId}/code")
    public ApiResponse<UserCouponCodeResponse> findUserCouponCode(
            @RequestHeader("X-User_Id") Long userId,
            @PathVariable Long userCouponId
    ) {
        UserCouponCodeResponse response = userCouponService.findUserCouponCode(userId, userCouponId);
        return ApiResponse.success(response);
    }

    @Secured(UserRole.Authority.ADMIN)
    @PostMapping("/v1/user-coupons/use")
    public ApiResponse<UserCouponUseResponse> useUserCoupon(
            @RequestHeader("X-User_Id") Long userId,
            @Valid @RequestBody UserCouponRequest request
    ) {
        UserCouponUseResponse response = userCouponService.useUserCoupon(userId, request);
        return ApiResponse.success(response);
    }

    @PostMapping("/v1/user-coupons/expire")
    public ResponseEntity<ApiResponse<Void>> NotifyCouponExpire() {
        userCouponService.sendExpireCouponNotifications();
        return ResponseEntity.ok(ApiResponse.success());
    }
}
