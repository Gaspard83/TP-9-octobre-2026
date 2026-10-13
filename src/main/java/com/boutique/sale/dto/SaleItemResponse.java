package com.boutique.sale.dto;

import java.math.BigDecimal;

public record SaleItemResponse(
        Long productId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {
}
