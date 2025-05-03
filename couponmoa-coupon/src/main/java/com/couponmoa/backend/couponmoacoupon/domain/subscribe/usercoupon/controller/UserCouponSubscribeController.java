package com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.controller;

import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.dto.response.FindCouponSubscribeListResponse;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.service.UserCouponSubscribeService;
import com.couponmoa.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/coupons")
public class UserCouponSubscribeController {

    private final UserCouponSubscribeService userCouponSubscribeService;

    @PostMapping("/{couponId}/subscriptions")
    public ResponseEntity<ApiResponse<Void>> subscribeCoupon(@RequestHeader("X-User-Id") Long userId,
                                                             @Parameter(description = "구독할 쿠폰 id", required = true)
                                                             @PathVariable Long couponId) {
        userCouponSubscribeService.subscribeCoupon(userId, couponId);

        return ResponseEntity.ok(ApiResponse.success(couponId + "번 쿠폰 구독 완료"));
    }

    @PostMapping("/{couponId}/unsubscriptions")
    public ResponseEntity<ApiResponse<Void>> unsubscribeCoupon(@RequestHeader("X-User-Id") Long userId,
                                                               @Parameter(description = "구독할 쿠폰 id", required = true)
                                                               @PathVariable Long couponId) {
        userCouponSubscribeService.unSubscribeCoupon(userId, couponId);

        return ResponseEntity.ok(ApiResponse.success(couponId + "번 쿠폰 구독 취소"));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<ApiResponse<List<FindCouponSubscribeListResponse>>> findCouponSubscribeList(@RequestHeader("X-User-Id") Long userId,
                                                                                                      @RequestParam(defaultValue = "0") int page,
                                                                                                      @RequestParam(defaultValue = "10") int size) {
        List<FindCouponSubscribeListResponse> subscribeList = userCouponSubscribeService.findSubscribeList(userId, page, size);

        return ResponseEntity.ok(ApiResponse.success(subscribeList, "쿠폰 구독 목록 조회 성공"));
    }

    @PostMapping("/{couponId}/alert")
    public ResponseEntity<ApiResponse<List<String>>> sendAlert(@PathVariable Long couponId) {
        List<String> emailList = userCouponSubscribeService.sendAlert(couponId);

        return ResponseEntity.ok(ApiResponse.success(emailList));
    }
}
