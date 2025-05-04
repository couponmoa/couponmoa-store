package com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.controller;

import com.couponmoa.backend.couponmoastore.common.dto.ApiResponse;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.dto.response.FindStoreSubscribeListResponse;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.service.UserStoreSubscribeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/stores")
public class UserStoreSubscribeController {

    private final UserStoreSubscribeService userStoreSubServ;

    @PostMapping("/{storeId}/subscriptions")
    public ResponseEntity<ApiResponse<Void>> subscribeStore(@RequestHeader("X-User-Id") Long userId,
                                                             @PathVariable Long storeId) {
        userStoreSubServ.subscribeStore(userId, storeId);

        return ResponseEntity.ok(ApiResponse.success(storeId + "번 가게 구독 완료"));
    }

    @PostMapping("/{storeId}/unsubscriptions")
    public ResponseEntity<ApiResponse<Void>> unsubscribeStore(@RequestHeader("X-User-Id") Long userId,
                                                               @PathVariable Long storeId) {
        userStoreSubServ.unSubscribeCoupon(userId, storeId);

        return ResponseEntity.ok(ApiResponse.success(storeId + "번 가게 구독 취소"));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<ApiResponse<List<FindStoreSubscribeListResponse>>> findStoreSubscribeList(@RequestHeader("X-User-Id") Long userId,
                                                                                                    @RequestParam(defaultValue = "0") int page,
                                                                                                    @RequestParam(defaultValue = "10") int size) {
        List<FindStoreSubscribeListResponse> subscribeList = userStoreSubServ.findSubscribeList(userId, page, size);

        return ResponseEntity.ok(ApiResponse.success(subscribeList, "가게 구독 목록 조회 성공"));
    }

    @PostMapping("/{storeId}/alert")
    public ResponseEntity<ApiResponse<List<String>>> sendAlert(@PathVariable Long storeId) {
        List<String> emailList = userStoreSubServ.sendToSQS(storeId);

        return ResponseEntity.ok(ApiResponse.success(emailList));
    }
}
