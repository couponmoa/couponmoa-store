package com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.repository;

import com.couponmoa.backend.couponmoastore.common.repository.BaseRepository;
import com.couponmoa.backend.couponmoastore.domain.store.entity.Store;
import com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.entity.UserStoreSubscribe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserStoreSubscribeRepository extends BaseRepository<UserStoreSubscribe, Long> {
    Optional<UserStoreSubscribe> findByUserIdAndStore(Long userId, Store store);

    List<UserStoreSubscribe> findByStore_Id(Long storeId);

    List<UserStoreSubscribe> findByUserId(Long userId);

//    @EntityGraph(attributePaths = {"user"})
    Page<UserStoreSubscribe> findByUserId(Long userId, Pageable pageable);

    boolean existsByUserIdAndStore(Long userId, Store store);
}
