package com.boutique.sale.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SaleResponse(
        Long id,
        Long customerId,
        String customerName,
        LocalDateTime saleDate,
        BigDecimal total,
        List<SaleItemResponse> items) {
}
