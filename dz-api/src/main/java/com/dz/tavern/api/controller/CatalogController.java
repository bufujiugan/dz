package com.dz.tavern.api.controller;

import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.common.model.PageResult;
import com.dz.tavern.dao.entity.CategoryEntity;
import com.dz.tavern.dao.entity.ProductEntity;
import com.dz.tavern.dao.entity.StoreEntity;
import com.dz.tavern.service.CatalogService;
import com.dz.tavern.service.dto.HomeView;
import com.dz.tavern.service.dto.MenuView;
import com.dz.tavern.service.dto.ProductDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CatalogController {
    private final CatalogService catalogService;

    @GetMapping("/store/list")
    public ApiResponse<List<StoreEntity>> stores() {
        return ApiResponse.ok(catalogService.listStores());
    }

    @GetMapping("/category/list")
    public ApiResponse<List<CategoryEntity>> categories(@RequestParam Long storeId) {
        return ApiResponse.ok(catalogService.listCategories(storeId));
    }

    @GetMapping("/product/page")
    public ApiResponse<PageResult<ProductEntity>> products(
            @RequestParam Long storeId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return ApiResponse.ok(catalogService.pageProducts(
                storeId, categoryId, keyword, current, size, true));
    }

    @GetMapping("/product/{id}")
    public ApiResponse<ProductDetail> product(
            @PathVariable Long id,
            @RequestParam(required = false) Long storeId) {
        return ApiResponse.ok(catalogService.getProduct(id, storeId, true));
    }

    @GetMapping("/home")
    public ApiResponse<HomeView> home(@RequestParam Long storeId) {
        return ApiResponse.ok(catalogService.getHome(storeId));
    }

    @GetMapping("/menu")
    public ApiResponse<MenuView> menu(@RequestParam Long storeId) {
        return ApiResponse.ok(catalogService.getMenu(storeId));
    }
}
