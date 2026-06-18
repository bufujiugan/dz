package com.dz.tavern.service.dto;

import com.dz.tavern.dao.entity.CategoryEntity;

import java.util.List;

public record MenuView(List<CategoryEntity> categories, List<MenuProductView> products) {
}
