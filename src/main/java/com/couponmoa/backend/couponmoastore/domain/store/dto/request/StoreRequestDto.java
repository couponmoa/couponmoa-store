package com.couponmoa.backend.couponmoastore.domain.store.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StoreRequestDto {

    private String name;
    private String description;
    private String address;

    public StoreRequestDto(String name, String description, String address) {
        this.name = name;
        this.description = description;
        this.address = address;
    }
}
