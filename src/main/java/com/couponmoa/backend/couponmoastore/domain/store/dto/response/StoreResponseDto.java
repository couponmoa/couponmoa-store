package com.couponmoa.backend.couponmoastore.domain.store.dto.response;

import com.couponmoa.backend.couponmoastore.domain.store.entity.Store;
import lombok.Getter;

import java.io.Serializable;

@Getter
public class StoreResponseDto implements Serializable {

    private final Long id;
    private final String name;
    private final String description;
    private final String address;

    public StoreResponseDto(Long id, String name, String description, String address) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.address = address;
    }

    public static StoreResponseDto toDto(Store store) {
        return new StoreResponseDto(
                store.getId(),
                store.getName(),
                store.getDescription(),
                store.getAddress());
    }
}
