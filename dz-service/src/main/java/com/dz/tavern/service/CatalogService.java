package com.dz.tavern.service;

import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.CategoryEntity;
import com.dz.tavern.dao.entity.ProductEntity;
import com.dz.tavern.dao.entity.ProductSkuEntity;
import com.dz.tavern.dao.entity.StoreEntity;
import com.dz.tavern.service.dto.HomeView;
import com.dz.tavern.service.dto.MenuView;
import com.dz.tavern.service.dto.ProductDetail;

import java.util.List;

public interface CatalogService {
    List<StoreEntity> listStores();

    PageResult<StoreEntity> pageStores(String keyword, Integer status, long current, long size);

    Long saveStore(StoreEntity store);

    void changeStoreStatus(Long storeId, Integer status);

    List<CategoryEntity> listCategories(Long storeId);

    PageResult<ProductEntity> pageProducts(Long storeId, Long categoryId, String keyword,
                                           long current, long size, boolean onlyActive);

    ProductDetail getProduct(Long productId, Long storeId, boolean onlyActive);

    default ProductDetail getProduct(Long productId, boolean onlyActive) {
        return getProduct(productId, null, onlyActive);
    }

    HomeView getHome(Long storeId);

    MenuView getMenu(Long storeId);

    Long saveCategory(CategoryEntity category);

    void deleteCategory(Long categoryId);

    Long saveProduct(ProductEntity product, List<ProductSkuEntity> skus);

    void changeProductStatus(Long productId, Integer status);

    void updateHomeAnnouncement(Long storeId, String announcement);
}
