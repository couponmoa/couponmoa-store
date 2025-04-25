package com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.controller;

import com.couponmoa.backend.couponmoacoupon.common.dto.ApiResponse;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.dto.response.FindCouponSubscribeListResponse;
import com.couponmoa.backend.couponmoacoupon.domain.subscribe.usercoupon.service.UserCouponSubscribeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "쿠폰 구독 API", description = "유저 쿠폰 구독 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/coupons")
public class UserCouponSubscribeController {

    private final UserCouponSubscribeService userCouponSubServ;

    @Operation(summary = "쿠폰 구독", description = "유저가 특정 쿠폰을 구독함")
    @PostMapping("/{couponId}/subscriptions")
    public ResponseEntity<ApiResponse<Void>> subscribeCoupon(@RequestHeader("X-User-Id") Long userId,
                                                             @Parameter(description = "구독할 쿠폰 id", required = true)
                                                             @PathVariable Long couponId) {
        userCouponSubServ.subscribeCoupon(userId, couponId);

        return ResponseEntity.ok(ApiResponse.success(couponId + "번 쿠폰 구독 완료"));
    }

    @Operation(summary = "쿠폰 구독 취소", description = "유저가 특정 쿠폰 구독을 취소함")
    @PostMapping("/{couponId}/unsubscriptions")
    public ResponseEntity<ApiResponse<Void>> unsubscribeCoupon(@RequestHeader("X-User-Id") Long userId,
                                                               @Parameter(description = "구독할 쿠폰 id", required = true)
                                                               @PathVariable Long couponId) {
        userCouponSubServ.unSubscribeCoupon(userId, couponId);

        return ResponseEntity.ok(ApiResponse.success(couponId + "번 쿠폰 구독 취소"));
    }

    @Operation(summary = "내 쿠폰 구독 목록 확인", description = "쿠폰 구독 목록 확인")
    @GetMapping("/subscriptions")
    public ResponseEntity<ApiResponse<List<FindCouponSubscribeListResponse>>> findCouponSubscribeList(@RequestHeader("X-User-Id") Long userId,
                                                                                                      @RequestParam(defaultValue = "0") int page,
                                                                                                      @RequestParam(defaultValue = "10") int size) {
        List<FindCouponSubscribeListResponse> subscribeList = userCouponSubServ.findSubscribeList(userId, page, size);

        return ResponseEntity.ok(ApiResponse.success(subscribeList, "쿠폰 구독 목록 조회 성공"));
    }

    @Operation(summary = "알림서비스", description = "구독한 쿠폰이 새로 생기면 이메일로 알림을 보냄")
    @PostMapping("/{couponId}/alert")
    public ResponseEntity<ApiResponse<List<String>>> sendAlert(@PathVariable Long couponId) {
        List<String> emailList = userCouponSubServ.sendAlert(couponId);

        return ResponseEntity.ok(ApiResponse.success(emailList));
    }
}
