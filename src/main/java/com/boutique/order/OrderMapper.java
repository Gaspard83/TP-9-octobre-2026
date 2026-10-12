package com.boutique.order;

import org.springframework.stereotype.Component;

import com.boutique.customer.Customer;
import com.boutique.order.dto.OrderItemResponse;
import com.boutique.order.dto.OrderResponse;

@Component
public class OrderMapper {

    public OrderResponse toResponse(Order order) {
        Customer customer = order.getCustomer();
        return new OrderResponse(
                order.getId(),
                customer.getId(),
                customer.getFirstName() + " " + customer.getLastName(),
                order.getCreatedAt(),
                order.getStatus(),
                order.getTotal(),
                order.getItems().stream().map(this::toItemResponse).toList());
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        return new OrderItemResponse(item.getProduct().getId(), item.getProduct().getName(),
                item.getQuantity(), item.getUnitPrice(), item.getSubtotal());
    }
}
