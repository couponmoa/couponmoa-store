package com.couponmoa.backend.couponmoastore.domain.subscribe.userstore.entity;


import com.couponmoa.backend.couponmoastore.common.entity.BaseEntity;
import com.couponmoa.backend.couponmoastore.domain.store.entity.Store;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
public class UserStoreSubscribe extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    private Long userId;

    public UserStoreSubscribe(Long userId, Store store) {
        this.userId = userId;
        this.store = store;
    }
}
