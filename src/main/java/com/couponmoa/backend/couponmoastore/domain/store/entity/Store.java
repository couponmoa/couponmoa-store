package com.couponmoa.backend.couponmoastore.domain.store.entity;

import com.couponmoa.backend.couponmoastore.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Entity
@Getter
@Table(name = "stores")
@NoArgsConstructor
public class Store extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private String name;

    private String description;

    private String address;


    @Builder
    public Store(Long userId, String name, String description, String address) {
        this.userId = userId;
        this.name = name;
        this.description = description;
        this.address = address;
    }

    // update 메서드
    public void update(String name, String description, String address) {
        this.name = name;
        this.description = description;
        this.address = address;
    }

    public void delete() {
        this.setDeletedAt(LocalDateTime.now());
    }
}
