package com.boutique.sale;

import org.springframework.stereotype.Component;

import com.boutique.customer.Customer;
import com.boutique.sale.dto.SaleItemResponse;
import com.boutique.sale.dto.SaleResponse;

@Component
public class SaleMapper {

    public SaleResponse toResponse(Sale sale) {
        Customer customer = sale.getCustomer();
        return new SaleResponse(
                sale.getId(),
                customer.getId(),
                customer.getFirstName() + " " + customer.getLastName(),
                sale.getSaleDate(),
                sale.getTotal(),
                sale.getItems().stream().map(this::toItemResponse).toList());
    }

    public SaleItemResponse toItemResponse(SaleItem item) {
        return new SaleItemResponse(
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal());
    }
}
