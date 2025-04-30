package com.couponmoa.backend.couponmoacoupon.domain.coupon.controller.v2;

import com.couponmoa.backend.couponmoacoupon.domain.coupon.service.v2.CouponRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/recommend")
@RequiredArgsConstructor
public class CouponRecommendationController {

    private final CouponRecommendationService recommendationService;

    @GetMapping
    public List<com.couponmoa.backend.domain.coupon.entity.Search> getRecommendations(@RequestParam(name = "userId") String userId) throws IOException {
        return recommendationService.getAIRecommendations(userId);
    }
}

