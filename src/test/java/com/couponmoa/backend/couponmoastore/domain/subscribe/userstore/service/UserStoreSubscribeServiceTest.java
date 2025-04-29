package com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.service;

import com.couponmoa.backend.couponmoastore.common.emailSender.dto.SendToMQDto;
import com.couponmoa.backend.couponmoastore.common.emailSender.service.SqsService;
import com.couponmoa.backend.couponmoastore.common.exception.ApplicationException;
import com.couponmoa.backend.couponmoastore.common.exception.ErrorCode;
import com.couponmoa.backend.couponmoastore.domain.store.entity.Store;
import com.couponmoa.backend.couponmoastore.domain.store.grpc.UserGrpcClient;
import com.couponmoa.backend.couponmoastore.domain.store.repository.StoreRepository;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.dto.response.FindStoreSubscribeListResponse;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.entity.UserStoreSubscribe;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.repository.UserStoreSubscribeRepository;
import com.couponmoa.grpc.user.UserResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserStoreSubscribeServiceTest {

    @Mock
    private UserGrpcClient userGrpcClient;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserStoreSubscribeRepository userStoreSubRepository;

    @Mock
    private SqsService sqsService;

    @InjectMocks
    private UserStoreSubscribeService userStoreSubscribeService;

    @Nested
    class subscribeStore {
        @Test
        void 가게_조회_실패() {
            Long userId = 1L;
            Long storeId = 1L;
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class)))
                    .willThrow(new ApplicationException(ErrorCode.STORE_NOT_FOUND));

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> userStoreSubscribeService.subscribeStore(userId, storeId));
            assertEquals(ErrorCode.STORE_NOT_FOUND.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 이미_존재하는_구독() {
            Long storeId = 1L;
            Long userId = 1L;
            Store store = new Store(userId, "name", "des", "address");
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);
            given(userStoreSubRepository.existsByUserIdAndStore(anyLong(), any(Store.class)))
                    .willThrow(new ApplicationException(ErrorCode.DUPLICATED_USER_COUPON));

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> userStoreSubscribeService.subscribeStore(userId, storeId));
            assertEquals(ErrorCode.DUPLICATED_USER_COUPON.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 구독_성공() {
            Long storeId = 1L;
            Long userId = 1L;
            Store store = new Store(userId, "name", "des", "address");
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);
            given(userStoreSubRepository.existsByUserIdAndStore(anyLong(), any(Store.class))).willReturn(false);

            userStoreSubscribeService.subscribeStore(userId, storeId);
            verify(userStoreSubRepository, times(1)).save(any(UserStoreSubscribe.class));
        }
    }

    @Nested
    class unSubscribeCoupon {
        @Test
        void 가게_조회_실패() {
            Long userId = 1L;
            Long storeId = 1L;
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class)))
                    .willThrow(new ApplicationException(ErrorCode.STORE_NOT_FOUND));

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> userStoreSubscribeService.unSubscribeCoupon(userId, storeId));
            assertEquals(ErrorCode.STORE_NOT_FOUND.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 해당_구독_없음() {
            Long storeId = 1L;
            Long userId = 1L;
            Store store = new Store(userId, "name", "des", "address");
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);

            UserStoreSubscribe subscribe = new UserStoreSubscribe(userId, store);
            given(userStoreSubRepository.findByUserIdAndStore(anyLong(), any(Store.class))).willReturn(Optional.empty());

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> userStoreSubscribeService.unSubscribeCoupon(userId, storeId));
            assertEquals(ErrorCode.STORE_NOT_FOUND.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 구독_취소_성공() {
            Long storeId = 1L;
            Long userId = 1L;
            Store store = new Store(userId, "name", "des", "address");
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);

            UserStoreSubscribe subscribe = new UserStoreSubscribe(userId, store);
            given(userStoreSubRepository.findByUserIdAndStore(anyLong(), any(Store.class))).willReturn(Optional.of(subscribe));

            userStoreSubscribeService.unSubscribeCoupon(userId, storeId);
            verify(userStoreSubRepository, times(1)).delete(any(UserStoreSubscribe.class));
        }
    }

    @Test
    void 구독_목록_조회() {
        Long userId = 1L;
        Store store = new Store(userId, "name", "des", "address");
        UserStoreSubscribe subscribe = new UserStoreSubscribe(userId, store);
        Page<UserStoreSubscribe> pageResult = new PageImpl<>(List.of(subscribe));

        given(userStoreSubRepository.findByUserId(eq(userId), any(PageRequest.class))).willReturn(pageResult);

        List<FindStoreSubscribeListResponse> result =
                userStoreSubscribeService.findSubscribeList(userId, 0, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("name");
    }

    @Nested
    class sendToSQS {
        @Test
        void 가게_조회_실패() {
            Long storeId = 1L;
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class)))
                    .willThrow(new ApplicationException(ErrorCode.STORE_NOT_FOUND));

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> userStoreSubscribeService.sendToSQS(storeId));
            assertEquals(ErrorCode.STORE_NOT_FOUND.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 이메일_리스트_없음() {
            Long storeId = 1L;
            Long userId = 1L;
            Store store = new Store(userId, "name", "des", "address");
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);

            given(userStoreSubRepository.findByStore_Id(anyLong())).willReturn(List.of());

            userStoreSubscribeService.sendToSQS(storeId);
            verify(sqsService, never()).sendMessage(any(SendToMQDto.class));
        }

        @Test
        void SQS_전송_성공() {
            Long storeId = 1L;
            Long userId = 1L;
            Store store = new Store(userId, "name", "des", "address");

            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);

            UserStoreSubscribe subscribe = new UserStoreSubscribe(userId, store);
            List<UserStoreSubscribe> storeSubscribes = List.of(subscribe);

            given(userStoreSubRepository.findByStore_Id(anyLong())).willReturn(storeSubscribes);
            given(userGrpcClient.getUserById(anyLong())).willReturn(UserResponse.newBuilder().setEmail("test@email.com").build());

            List<String> result = userStoreSubscribeService.sendToSQS(storeId);

            verify(sqsService, times(1)).sendMessage(any(SendToMQDto.class));
            assertEquals(result.size(),1);
        }

    }
}
