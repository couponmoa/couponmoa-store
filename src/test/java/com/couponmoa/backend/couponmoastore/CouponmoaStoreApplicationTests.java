package com.couponmoa.backend.couponmoastore;

import com.couponmoa.backend.couponmoastore.common.emailSender.service.SqsService;
import com.couponmoa.backend.couponmoastore.domain.store.grpc.UserGrpcClient;
import com.couponmoa.common.testcontainers.TestContainerBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class CouponmoaStoreApplicationTests extends TestContainerBase {

    @MockitoBean
    private UserGrpcClient userGrpcClient;
    @MockitoBean
    private SqsService sqsService;

    @Test
    void contextLoads() {
    }

}
