package com.dz.tavern.api.controller;

import com.dz.tavern.common.context.LoginContext;
import com.dz.tavern.common.model.ApiResponse;
import com.dz.tavern.dao.entity.CartEntity;
import com.dz.tavern.service.CartService;
import com.dz.tavern.service.dto.CartCommand;
import com.dz.tavern.service.dto.CartItemView;
import jakarta.validation.Valid;
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
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("/list")
    public ApiResponse<List<CartEntity>> list(
            @RequestParam(required = false) Long storeId) {
        return ApiResponse.ok(cartService.list(LoginContext.currentId(), storeId));
    }

    @GetMapping("/detail")
    public ApiResponse<List<CartItemView>> listDetails(
            @RequestParam(required = false) Long storeId) {
        return ApiResponse.ok(cartService.listDetails(LoginContext.currentId(), storeId));
    }

    @PostMapping("/add")
    public ApiResponse<Void> add(@Valid @RequestBody CartCommand command) {
        cartService.add(LoginContext.currentId(), command.storeId(),
                command.skuId(), command.quantity());
        return ApiResponse.ok();
    }

    @PutMapping("/update")
    public ApiResponse<Void> update(@Valid @RequestBody CartCommand command) {
        cartService.update(LoginContext.currentId(), command.id(), command.quantity());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        cartService.delete(LoginContext.currentId(), id);
        return ApiResponse.ok();
    }
}
