package com.boutique.order.dto;

import com.boutique.order.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record OrderStatusRequest(@NotNull OrderStatus status) {
}
