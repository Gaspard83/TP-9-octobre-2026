package com.boutique.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.boutique.order.OrderStatus;

public record OrderResponse(
        Long id,
        Long customerId,
        String customerName,
        LocalDateTime createdAt,
        OrderStatus status,
        BigDecimal total,
        List<OrderItemResponse> items) {
}
