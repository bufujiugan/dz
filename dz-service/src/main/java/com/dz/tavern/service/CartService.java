package com.dz.tavern.service;

import com.dz.tavern.dao.entity.CartEntity;
import com.dz.tavern.service.dto.CartItemView;

import java.util.List;

public interface CartService {
    List<CartEntity> list(Long userId, Long storeId);

    default List<CartEntity> list(Long userId) {
        return list(userId, null);
    }

    List<CartItemView> listDetails(Long userId, Long storeId);

    default List<CartItemView> listDetails(Long userId) {
        return listDetails(userId, null);
    }

    void add(Long userId, Long storeId, Long skuId, Integer quantity);

    default void add(Long userId, Long skuId, Integer quantity) {
        add(userId, null, skuId, quantity);
    }

    void update(Long userId, Long cartId, Integer quantity);

    void delete(Long userId, Long cartId);
}
