package com.couponmoa.backend.couponmoacoupon;

import com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc.StoreGrpcClient;
import com.couponmoa.backend.couponmoacoupon.domain.coupon.grpc.UserGrpcClient;
import com.couponmoa.common.config.CommonConfig;
import com.couponmoa.common.service.RedisService;
import com.couponmoa.common.sqssender.service.SqsService;
import com.couponmoa.common.testcontainers.TestContainerBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(CommonConfig.class)
class CouponmoaCouponApplicationTests extends TestContainerBase {

    @MockitoBean
    private StoreGrpcClient storeGrpcClient;
    @MockitoBean
    private UserGrpcClient userGrpcClient;
    @MockitoBean
    private SqsService sqsService;
    @MockitoBean
    private RedisService redisService;

    @Test
    void contextLoads() {
        // 컨텍스트 로딩 성공 여부 확인
    }
}
