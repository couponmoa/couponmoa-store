package com.couponmoa.backend.couponmoastore.domain.store.service.v2;

import com.couponmoa.backend.couponmoastore.common.exception.ApplicationException;
import com.couponmoa.backend.couponmoastore.common.exception.ErrorCode;
import com.couponmoa.backend.couponmoastore.domain.store.dto.request.StoreCursor;
import com.couponmoa.backend.couponmoastore.domain.store.dto.request.StoreRequestDto;
import com.couponmoa.backend.couponmoastore.domain.store.dto.response.StoreResponseDto;
import com.couponmoa.backend.couponmoastore.domain.store.dto.response.StoreSimpleResponse;
import com.couponmoa.backend.couponmoastore.domain.store.entity.Store;
import com.couponmoa.backend.couponmoastore.domain.store.grpc.UserGrpcClient;
import com.couponmoa.backend.couponmoastore.domain.store.repository.StoreQueryDslRepository;
import com.couponmoa.backend.couponmoastore.domain.store.repository.StoreRepository;
import com.couponmoa.grpc.user.UserResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class StoreServiceV2Test {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreQueryDslRepository storeQueryDslRepository;

    @Mock
    private UserGrpcClient userGrpcClient;

    @InjectMocks
    private StoreServiceV2 storeServiceV2;

    @Nested
    class createStore {
        @Test
        void 사용자_조회_실패() {
            Long userId = null;
            StoreRequestDto dto = new StoreRequestDto("name", "desc", "address");

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.createStore(dto, userId));
            assertEquals(ErrorCode.USER_NOT_FOUND.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 사용자_권한_없음() {
            Long userId = 1L;
            UserResponse response = UserResponse.newBuilder().setUserRole("ROLE_USER").build();

            given(userGrpcClient.getUserById(anyLong())).willReturn(response);

            StoreRequestDto dto = new StoreRequestDto("name", "desc", "address");

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.createStore(dto, userId));
            assertEquals(ErrorCode.FORBIDDEN_ADMIN_ONLY.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 동일한_이름의_스토어_존재() {
            Long userId = 1L;
            UserResponse response = UserResponse.newBuilder().setUserRole("ROLE_ADMIN").build();

            given(userGrpcClient.getUserById(anyLong())).willReturn(response);

            StoreRequestDto dto = new StoreRequestDto("name", "desc", "address");

            given(storeRepository.existsByNameAndDeletedAtIsNull(dto.getName())).willReturn(true);

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.createStore(dto, userId));
            assertEquals(ErrorCode.DUPLICATE_RESOURCE.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 정상_생성() {
            Long userId = 1L;
            StoreRequestDto dto = new StoreRequestDto("name", "desc", "address");

            UserResponse response = UserResponse.newBuilder().setUserRole("ROLE_ADMIN").build();

            given(userGrpcClient.getUserById(anyLong())).willReturn(response);
            given(storeRepository.existsByNameAndDeletedAtIsNull(dto.getName())).willReturn(false);

            Store savedStore = new Store(userId, dto.getName(), dto.getDescription(), dto.getAddress());
            given(storeRepository.save(any(Store.class))).willReturn(savedStore);

            StoreResponseDto result = storeServiceV2.createStore(dto, userId);

            assertThat(result.getName()).isEqualTo("name");
        }
    }

    @Test
    void 키워드로_가게목록_조회() {
        Long storeId = 1L;
        StoreCursor cursor = new StoreCursor("keyword", storeId);
        StoreResponseDto dto = new StoreResponseDto(storeId, "name", "desc", "address");
        List<StoreResponseDto> list = List.of(dto);
        given(storeQueryDslRepository.searchStoresByKeyword(cursor, 1)).willReturn(list);

        storeServiceV2.findStoresByKeyword(cursor, 1);

        assertThat(list.get(0).getName()).isEqualTo("name");
    }

    @Nested
    class findStore {
        @Test
        void 가게_조회_실패() {
            Long storeId = 1L;
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class)))
                    .willThrow(new ApplicationException(ErrorCode.STORE_NOT_FOUND));

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.findStore(storeId));
            assertEquals(ErrorCode.STORE_NOT_FOUND.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 가게_단건_조회() {
            Long storeId = 1L;
            Long userId = 1L;
            Store store = new Store(userId, "name", "des", "address");
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);

            StoreResponseDto result = storeServiceV2.findStore(storeId);

            assertThat(result.getName()).isEqualTo("name");
        }
    }


    @Nested
    class getStoreById {
        @Test
        void 가게_조회_실패() {
            Long storeId = 1L;
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class)))
                    .willThrow(new ApplicationException(ErrorCode.STORE_NOT_FOUND));

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.getStoreById(storeId));
            assertEquals(ErrorCode.STORE_NOT_FOUND.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 가게_조회_성공() {
            Long storeId = 1L;
            Long userId = 1L;
            Store store = new Store(userId, "name", "des", "address");
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);

            Store result = storeServiceV2.getStoreById(storeId);

            assertThat(result.getName()).isEqualTo("name");
        }
    }

    @Nested
    class findMyStores {
        @Test
        void 사용자_조회_실패() {
            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.findMyStores(null));
            assertEquals(ErrorCode.UNAUTHORIZED_ACCESS.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 내_가게_조회_성공() {
            Long storeId = 1L;
            Long userId = 1L;
            Store store = new Store(userId, "name", "des", "address");
            List<Store> stores = List.of(store);
            given(storeRepository.findByUserIdAndDeletedAtIsNull(anyLong())).willReturn(stores);

            List<StoreResponseDto> result = storeServiceV2.findMyStores(userId);

            assertThat(result.get(0).getName()).isEqualTo("name");
        }
    }

    @Nested
    class findMySimpleStores {
        @Test
        void 사용자_조회_실패() {
            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.findMySimpleStores(null));
            assertEquals(ErrorCode.UNAUTHORIZED_ACCESS.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 내_가게_간단_조회_성공() {
            Long userId = 1L;
            Store store = new Store(userId, "name", "des", "address");
            List<Store> stores = List.of(store);
            given(storeRepository.findByUserIdAndDeletedAtIsNull(anyLong())).willReturn(stores);

            List<StoreSimpleResponse> result = storeServiceV2.findMySimpleStores(userId);

            assertThat(result.get(0).getName()).isEqualTo("name");
        }
    }

    @Nested
    class updateStore {
        @Test
        void 사용자_권한_없음() {
            Long userId = 1L;
            Long storeId = 1L;
            UserResponse response = UserResponse.newBuilder().setUserRole("ROLE_USER").build();

            given(userGrpcClient.getUserById(anyLong())).willReturn(response);

            StoreRequestDto dto = new StoreRequestDto("name", "desc", "address");

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.updateStore(storeId, dto, userId));
            assertEquals(ErrorCode.FORBIDDEN_ADMIN_ONLY.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 가게_조회_실패() {
            Long userId = 1L;
            Long storeId = 1L;
            UserResponse response = UserResponse.newBuilder().setUserRole("ROLE_ADMIN").build();

            given(userGrpcClient.getUserById(anyLong())).willReturn(response);
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class)))
                    .willThrow(new ApplicationException(ErrorCode.STORE_NOT_FOUND));

            StoreRequestDto dto = new StoreRequestDto("name", "desc", "address");

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.updateStore(storeId, dto, userId));
            assertEquals(ErrorCode.STORE_NOT_FOUND.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 가게_주인_검증_실패() {
            Long userId = 1L;
            Long storeId = 1L;
            UserResponse response = UserResponse.newBuilder().setUserRole("ROLE_ADMIN").build();
            Store store = new Store(2L, "name", "des", "address");
            given(userGrpcClient.getUserById(anyLong())).willReturn(response);
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);

            StoreRequestDto dto = new StoreRequestDto("name", "desc", "address");

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.updateStore(storeId, dto, userId));
            assertEquals(ErrorCode.FORBIDDEN_ADMIN_ONLY.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 가게_업데이트() {
            Long userId = 1L;
            Long storeId = 1L;
            UserResponse response = UserResponse.newBuilder().setUserRole("ROLE_ADMIN").build();
            Store store = new Store(userId, "storeName", "des", "address");
            given(userGrpcClient.getUserById(anyLong())).willReturn(response);
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);

            StoreRequestDto dto = new StoreRequestDto("name", "desc", "address");

            StoreResponseDto result = storeServiceV2.updateStore(storeId, dto, userId);

            assertThat(result.getName()).isEqualTo("name");
        }
    }

    @Nested
    class deleteStore {
        @Test
        void 사용자_권한_없음() {
            Long userId = 1L;
            Long storeId = 1L;
            UserResponse response = UserResponse.newBuilder().setUserRole("ROLE_USER").build();

            given(userGrpcClient.getUserById(anyLong())).willReturn(response);

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.deleteStore(storeId, userId));
            assertEquals(ErrorCode.FORBIDDEN_ADMIN_ONLY.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 가게_조회_실패() {
            Long userId = 1L;
            Long storeId = 1L;
            UserResponse response = UserResponse.newBuilder().setUserRole("ROLE_ADMIN").build();

            given(userGrpcClient.getUserById(anyLong())).willReturn(response);
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class)))
                    .willThrow(new ApplicationException(ErrorCode.STORE_NOT_FOUND));

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.deleteStore(storeId, userId));
            assertEquals(ErrorCode.STORE_NOT_FOUND.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 가게_주인_검증_실패() {
            Long userId = 1L;
            Long storeId = 1L;
            UserResponse response = UserResponse.newBuilder().setUserRole("ROLE_ADMIN").build();
            Store store = new Store(2L, "name", "des", "address");
            given(userGrpcClient.getUserById(anyLong())).willReturn(response);
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.deleteStore(storeId, userId));
            assertEquals(ErrorCode.FORBIDDEN_ADMIN_ONLY.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 이미_삭제된_가게() {
            Long userId = 1L;
            Long storeId = 1L;
            UserResponse response = UserResponse.newBuilder().setUserRole("ROLE_ADMIN").build();
            Store store = new Store(userId, "name", "des", "address");
            ReflectionTestUtils.setField(store, "deletedAt", LocalDateTime.now().minusDays(1));
            given(userGrpcClient.getUserById(anyLong())).willReturn(response);
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);

            ApplicationException exception = assertThrows(ApplicationException.class,
                    () -> storeServiceV2.deleteStore(storeId, userId));
            assertEquals(ErrorCode.ALREADY_DELETED.getHttpStatus(), exception.getStatus());
        }

        @Test
        void 가게_삭제() {
            Long userId = 1L;
            Long storeId = 1L;
            UserResponse response = UserResponse.newBuilder().setUserRole("ROLE_ADMIN").build();
            Store store = new Store(userId, "name", "des", "address");
            given(userGrpcClient.getUserById(anyLong())).willReturn(response);
            given(storeRepository.findByIdOrElseThrow(anyLong(), any(ErrorCode.class))).willReturn(store);

            storeServiceV2.deleteStore(storeId, userId);

            assertNotNull(store.getDeletedAt());
        }
    }

}
