package com.couponmoa.backend.couponmoacoupon.domain.coupon.controller.v2;

import com.couponmoa.backend.couponmoacoupon.domain.coupon.service.v2.CouponElasticsearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class CouponSearchController {

    private final CouponElasticsearchService couponElasticsearchService;

    @GetMapping
    public List<com.couponmoa.backend.domain.coupon.entity.Search> search(@RequestParam(name = "keyword") String keyword,
                                                                          @RequestParam(name = "userId") String userId) {
        return couponElasticsearchService.recommendCoupons(keyword, userId);
    }
}

