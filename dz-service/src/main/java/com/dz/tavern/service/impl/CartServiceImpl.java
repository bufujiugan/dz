package com.dz.tavern.service.impl;

import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.dao.entity.CartEntity;
import com.dz.tavern.dao.entity.ProductEntity;
import com.dz.tavern.dao.entity.ProductSkuEntity;
import com.dz.tavern.dao.mapper.CartMapper;
import com.dz.tavern.dao.mapper.ProductMapper;
import com.dz.tavern.dao.mapper.ProductSkuMapper;
import com.dz.tavern.service.CartService;
import com.dz.tavern.service.dto.CartItemView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartMapper cartMapper;
    private final ProductSkuMapper productSkuMapper;
    private final ProductMapper productMapper;

    @Override
    public List<CartEntity> list(Long userId, Long storeId) {
        List<CartEntity> cartItems = cartMapper.selectByUserId(userId);
        if (storeId == null || cartItems.isEmpty()) {
            return cartItems;
        }
        List<Long> currentStoreCartIds = listDetails(userId, storeId).stream()
                .map(CartItemView::cartId)
                .toList();
        return cartItems.stream()
                .filter(cartItem -> currentStoreCartIds.contains(cartItem.getId()))
                .toList();
    }

    @Override
    public List<CartItemView> listDetails(Long userId, Long storeId) {
        List<CartEntity> cartItems = cartMapper.selectByUserId(userId);
        if (cartItems.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> skuIds = cartItems.stream().map(CartEntity::getSkuId).distinct().toList();
        Map<Long, ProductSkuEntity> skuById = productSkuMapper.selectByIds(skuIds).stream()
                .collect(Collectors.toMap(ProductSkuEntity::getId, Function.identity()));
        List<Long> productIds = skuById.values().stream()
                .map(ProductSkuEntity::getProductId)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ProductEntity> productById = productMapper.selectByIds(productIds, true).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));

        // 商品或规格下架后仍可能残留购物车记录，展示查询只返回当前可购买的完整条目。
        return cartItems.stream().map(cartItem -> {
            ProductSkuEntity sku = skuById.get(cartItem.getSkuId());
            ProductEntity product = sku == null ? null : productById.get(sku.getProductId());
            if (sku == null || product == null) {
                return null;
            }
            if (storeId != null && !product.getStoreId().equals(storeId)) {
                return null;
            }
            return new CartItemView(
                    cartItem.getId(),
                    product.getStoreId(),
                    product.getId(),
                    sku.getId(),
                    product.getName(),
                    product.getMainImage(),
                    sku.getSpecName(),
                    sku.getPriceFen(),
                    sku.getStock(),
                    cartItem.getQuantity(),
                    sku.getPriceFen() * cartItem.getQuantity());
        }).filter(item -> item != null).toList();
    }

    @Override
    public void add(Long userId, Long storeId, Long skuId, Integer quantity) {
        validateQuantity(quantity);
        ProductSkuEntity sku = productSkuMapper.selectById(skuId);
        if (sku == null) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        ProductEntity product = productMapper.selectById(sku.getProductId(), true);
        if (product == null || (storeId != null && !product.getStoreId().equals(storeId))) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        cartMapper.upsert(userId, skuId, quantity);
        log.info("购物车商品已增加 userId={} storeId={} skuId={} quantity={}",
                userId, storeId, skuId, quantity);
    }

    @Override
    public void update(Long userId, Long cartId, Integer quantity) {
        validateQuantity(quantity);
        if (cartMapper.updateQuantity(cartId, userId, quantity) == 0) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        log.info("购物车数量已更新 userId={} cartId={} quantity={}", userId, cartId, quantity);
    }

    @Override
    public void delete(Long userId, Long cartId) {
        if (cartMapper.deleteByIdAndUserId(cartId, userId) == 0) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        log.info("购物车商品已删除 userId={} cartId={}", userId, cartId);
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity <= 0 || quantity > 99) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
    }
}
