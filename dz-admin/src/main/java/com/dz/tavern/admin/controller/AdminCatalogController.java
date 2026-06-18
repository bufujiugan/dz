package com.dz.tavern.admin.controller;

import com.dz.tavern.common.annotation.OpLog;
import com.dz.tavern.common.annotation.RequirePermission;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.CategoryEntity;
import com.dz.tavern.dao.entity.ProductEntity;
import com.dz.tavern.dao.entity.ProductSkuEntity;
import com.dz.tavern.dao.entity.StoreEntity;
import com.dz.tavern.service.CatalogService;
import com.dz.tavern.service.dto.ProductDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin-api/catalog")
@RequirePermission("product:manage")
@RequiredArgsConstructor
public class AdminCatalogController {
    private final CatalogService catalogService;

    @GetMapping("/store/list")
    public ApiResponse<List<StoreEntity>> stores() {
        return ApiResponse.ok(catalogService.listStores());
    }

    @GetMapping("/store/page")
    public ApiResponse<PageResult<StoreEntity>> storePage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(catalogService.pageStores(keyword, status, current, size));
    }

    @PostMapping("/store")
    @OpLog(module = "STORE", action = "CREATE")
    public ApiResponse<Long> createStore(@RequestBody StoreEntity store) {
        store.setId(null);
        return ApiResponse.ok(catalogService.saveStore(store));
    }

    @PutMapping("/store")
    @OpLog(module = "STORE", action = "UPDATE")
    public ApiResponse<Long> updateStore(@RequestBody StoreEntity store) {
        return ApiResponse.ok(catalogService.saveStore(store));
    }

    @PostMapping("/store/{id}/status")
    @OpLog(module = "STORE", action = "CHANGE_STATUS")
    public ApiResponse<Void> changeStoreStatus(@PathVariable Long id,
                                               @RequestParam Integer status) {
        catalogService.changeStoreStatus(id, status);
        return ApiResponse.ok();
    }

    @GetMapping("/category/list")
    public ApiResponse<List<CategoryEntity>> categories(@RequestParam Long storeId) {
        return ApiResponse.ok(catalogService.listCategories(storeId));
    }

    @GetMapping("/product/page")
    public ApiResponse<PageResult<ProductEntity>> products(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(catalogService.pageProducts(
                storeId, categoryId, keyword, current, size, false));
    }

    @GetMapping("/product/{id}")
    public ApiResponse<ProductDetail> product(@PathVariable Long id) {
        return ApiResponse.ok(catalogService.getProduct(id, false));
    }

    @PostMapping("/category")
    @OpLog(module = "CATEGORY", action = "CREATE")
    public ApiResponse<Long> createCategory(@RequestBody CategoryEntity category) {
        category.setId(null);
        return ApiResponse.ok(catalogService.saveCategory(category));
    }

    @PutMapping("/category")
    @OpLog(module = "CATEGORY", action = "UPDATE")
    public ApiResponse<Long> updateCategory(@RequestBody CategoryEntity category) {
        return ApiResponse.ok(catalogService.saveCategory(category));
    }

    @DeleteMapping("/category/{id}")
    @OpLog(module = "CATEGORY", action = "DELETE")
    public ApiResponse<Void> deleteCategory(@PathVariable Long id) {
        catalogService.deleteCategory(id);
        return ApiResponse.ok();
    }

    @PostMapping("/product")
    @OpLog(module = "PRODUCT", action = "CREATE")
    public ApiResponse<Long> createProduct(@RequestBody ProductSaveRequest request) {
        request.product().setId(null);
        return ApiResponse.ok(catalogService.saveProduct(request.product(), request.skus()));
    }

    @PutMapping("/product")
    @OpLog(module = "PRODUCT", action = "UPDATE_WITH_PRICE_CHANGE")
    public ApiResponse<Long> updateProduct(@RequestBody ProductSaveRequest request) {
        return ApiResponse.ok(catalogService.saveProduct(request.product(), request.skus()));
    }

    @PostMapping("/product/{id}/status")
    @OpLog(module = "PRODUCT", action = "CHANGE_STATUS")
    public ApiResponse<Void> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        catalogService.changeProductStatus(id, status);
        return ApiResponse.ok();
    }

    @PutMapping("/home/announcement")
    @OpLog(module = "HOME", action = "UPDATE_ANNOUNCEMENT")
    public ApiResponse<Void> announcement(@RequestParam Long storeId,
                                          @RequestBody String announcement) {
        catalogService.updateHomeAnnouncement(storeId, announcement);
        return ApiResponse.ok();
    }

    public record ProductSaveRequest(ProductEntity product, List<ProductSkuEntity> skus) {
    }
}
