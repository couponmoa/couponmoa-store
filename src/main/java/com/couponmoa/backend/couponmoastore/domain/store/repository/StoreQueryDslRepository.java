package com.couponmoa.backend.couponmoastore.domain.store.repository;

import com.couponmoa.backend.couponmoastore.domain.store.dto.request.StoreCursor;
import com.couponmoa.backend.couponmoastore.domain.store.dto.response.StoreResponse;
import com.couponmoa.backend.couponmoastore.domain.store.entity.Store;

import java.util.List;

public interface StoreQueryDslRepository {

    List<Store> findAllStoreByName(String storeName);

    List<StoreResponse> searchStoresByKeyword(StoreCursor cursor, int size);
}
