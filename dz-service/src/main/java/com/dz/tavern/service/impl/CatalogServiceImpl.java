package com.dz.tavern.service.impl;

import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.CategoryEntity;
import com.dz.tavern.dao.entity.CouponTemplateEntity;
import com.dz.tavern.dao.entity.HomeContentEntity;
import com.dz.tavern.dao.entity.ProductEntity;
import com.dz.tavern.dao.entity.ProductSkuEntity;
import com.dz.tavern.dao.entity.StoreEntity;
import com.dz.tavern.dao.entity.StoreOperationConfigEntity;
import com.dz.tavern.dao.mapper.CategoryMapper;
import com.dz.tavern.dao.mapper.CouponMapper;
import com.dz.tavern.dao.mapper.HomeContentMapper;
import com.dz.tavern.dao.mapper.ProductMapper;
import com.dz.tavern.dao.mapper.ProductSkuMapper;
import com.dz.tavern.dao.mapper.StoreMapper;
import com.dz.tavern.dao.mapper.StoreOperationConfigMapper;
import com.dz.tavern.service.CatalogService;
import com.dz.tavern.service.dto.HomeView;
import com.dz.tavern.service.dto.MenuProductView;
import com.dz.tavern.service.dto.MenuView;
import com.dz.tavern.service.dto.ProductDetail;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CatalogServiceImpl implements CatalogService {
    private final StoreMapper storeMapper;
    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final CouponMapper couponMapper;
    private final HomeContentMapper homeContentMapper;
    private final StoreOperationConfigMapper storeOperationConfigMapper;

    @Override
    public List<StoreEntity> listStores() {
        return storeMapper.selectActiveList();
    }

    @Override
    public PageResult<StoreEntity> pageStores(String keyword, Integer status,
                                              long current, long size) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return new PageResult<>(normalizedCurrent, normalizedSize,
                storeMapper.countPage(normalizedKeyword, status),
                storeMapper.selectPage(normalizedKeyword, status, offset, normalizedSize));
    }

    @Override
    public Long saveStore(StoreEntity store) {
        if (store == null || store.getName() == null || store.getName().isBlank()
                || store.getAddress() == null || store.getAddress().isBlank()) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        store.setName(store.getName().trim());
        store.setAddress(store.getAddress().trim());
        store.setPhone(store.getPhone() == null ? "" : store.getPhone().trim());
        if (store.getStatus() == null) {
            store.setStatus(1);
        }
        if (store.getStatus() != 0 && store.getStatus() != 1) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        boolean creating = store.getId() == null;
        if (creating) {
            storeMapper.insertStore(store);
        } else if (storeMapper.updateStore(store) == 0) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        log.info("门店已保存 storeId={} operation={} status={}",
                store.getId(), creating ? "CREATE" : "UPDATE", store.getStatus());
        return store.getId();
    }

    @Override
    public void changeStoreStatus(Long storeId, Integer status) {
        if (storeId == null || status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        if (storeMapper.updateStatus(storeId, status) == 0) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        log.info("门店状态已变更 storeId={} status={}", storeId, status);
    }

    @Override
    public List<CategoryEntity> listCategories(Long storeId) {
        return categoryMapper.selectByStoreId(storeId);
    }

    @Override
    public PageResult<ProductEntity> pageProducts(Long storeId, Long categoryId, String keyword,
                                                  long current, long size, boolean onlyActive) {
        long normalizedCurrent = Math.max(current, 1);
        long normalizedSize = Math.min(Math.max(size, 1), 50);
        long offset = (normalizedCurrent - 1) * normalizedSize;
        return new PageResult<>(normalizedCurrent, normalizedSize,
                productMapper.countPage(storeId, categoryId, keyword, onlyActive),
                productMapper.selectPage(storeId, categoryId, keyword, onlyActive,
                        offset, normalizedSize));
    }

    @Override
    public ProductDetail getProduct(Long productId, Long storeId, boolean onlyActive) {
        ProductEntity product = productMapper.selectById(productId, onlyActive);
        if (product == null || (storeId != null && !product.getStoreId().equals(storeId))) {
            throw new BizException(ErrorCode.NOT_FOUND);
        }
        return new ProductDetail(product, productSkuMapper.selectByProductId(productId));
    }

    @Override
    public HomeView getHome(Long storeId) {
        HomeContentEntity content = homeContentMapper.selectByStoreId(storeId);
        String announcement = content == null ? "" : content.getAnnouncement();
        StoreOperationConfigEntity config = storeOperationConfigMapper.selectByStoreId(storeId);
        return new HomeView(
                announcement,
                config == null ? "02:00" : config.getBusinessEndTime(),
                config == null ? "今晚，慢一点。" : config.getHomeSlogan(),
                config == null ? "" : config.getHeroImage(),
                config == null ? "" : config.getGameplayDescription(),
                config == null ? "今晚酒单" : config.getMenuTitle(),
                filterCouponSaleProducts(storeId, productMapper.selectRecommended(storeId)));
    }

    @Override
    public MenuView getMenu(Long storeId) {
        List<ProductEntity> products = filterCouponSaleProducts(storeId,
                productMapper.selectPage(storeId, null, null, true, 0, 200));
        Set<Long> visibleCategoryIds = products.stream()
                .map(ProductEntity::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<CategoryEntity> categories = categoryMapper.selectByStoreId(storeId).stream()
                .filter(category -> visibleCategoryIds.contains(category.getId()))
                .toList();
        if (products.isEmpty()) {
            return new MenuView(categories, List.of());
        }
        List<Long> productIds = products.stream().map(ProductEntity::getId).toList();
        Map<Long, List<ProductSkuEntity>> skuByProductId =
                productSkuMapper.selectByProductIds(productIds).stream()
                        .collect(Collectors.groupingBy(ProductSkuEntity::getProductId));
        List<MenuProductView> menuProducts = products.stream().map(product -> {
            List<ProductSkuEntity> skus = skuByProductId.getOrDefault(
                    product.getId(), List.of());
            long minPriceFen = skus.stream()
                    .mapToLong(ProductSkuEntity::getPriceFen)
                    .min()
                    .orElse(0L);
            int sales = skus.stream()
                    .mapToInt(sku -> sku.getSales() == null ? 0 : sku.getSales())
                    .sum();
            return new MenuProductView(
                    product.getId(),
                    product.getStoreId(),
                    product.getCategoryId(),
                    product.getName(),
                    product.getMainImage(),
                    product.getDescription(),
                    product.getRecommended(),
                    minPriceFen,
                    sales,
                    skus);
        }).toList();
        return new MenuView(categories, menuProducts);
    }

    private List<ProductEntity> filterCouponSaleProducts(Long storeId,
                                                         List<ProductEntity> products) {
        if (products == null || products.isEmpty()) {
            return List.of();
        }
        Set<Long> couponSaleProductIds = couponMapper.selectTemplates(storeId, true).stream()
                .map(CouponTemplateEntity::getSaleProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (couponSaleProductIds.isEmpty()) {
            return products;
        }
        // 卡券售卖商品只在卡券中心呈现，避免在自助点酒菜单中被误认为酒水或小食。
        return products.stream()
                .filter(product -> !couponSaleProductIds.contains(product.getId()))
                .toList();
    }

    @Override
    public Long saveCategory(CategoryEntity category) {
        boolean creating = category.getId() == null;
        if (category.getId() == null) {
            categoryMapper.insertCategory(category);
        } else {
            categoryMapper.updateCategory(category);
        }
        log.info("商品分类已保存 categoryId={} storeId={} operation={}",
                category.getId(), category.getStoreId(), creating ? "CREATE" : "UPDATE");
        return category.getId();
    }

    @Override
    public void deleteCategory(Long categoryId) {
        categoryMapper.deleteCategory(categoryId);
        log.info("商品分类已删除 categoryId={}", categoryId);
    }

    @Override
    @Transactional
    public Long saveProduct(ProductEntity product, List<ProductSkuEntity> skus) {
        boolean creating = product.getId() == null;
        if (product.getId() == null) {
            productMapper.insertProduct(product);
        } else {
            productMapper.updateProduct(product);
        }
        if (skus != null) {
            for (ProductSkuEntity sku : skus) {
                sku.setProductId(product.getId());
                if (sku.getId() == null) {
                    productSkuMapper.insertSku(sku);
                } else {
                    productSkuMapper.updateSku(sku);
                }
            }
        }
        log.info("商品已保存 productId={} storeId={} operation={} skuCount={}",
                product.getId(), product.getStoreId(), creating ? "CREATE" : "UPDATE",
                skus == null ? 0 : skus.size());
        return product.getId();
    }

    @Override
    public void changeProductStatus(Long productId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        productMapper.updateStatus(productId, status);
        log.info("商品状态已变更 productId={} status={}", productId, status);
    }

    @Override
    public void updateHomeAnnouncement(Long storeId, String announcement) {
        homeContentMapper.upsert(storeId, announcement);
        log.info("门店首页公告已更新 storeId={}", storeId);
    }
}
