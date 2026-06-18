package com.dz.tavern.service.dto;

import com.dz.tavern.dao.entity.OrderEntity;
import com.dz.tavern.dao.entity.OrderItemEntity;

import java.util.List;

public record OrderDetail(OrderEntity order, List<OrderItemEntity> items) {
}
