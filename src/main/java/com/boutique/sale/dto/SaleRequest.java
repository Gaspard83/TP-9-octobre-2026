package com.boutique.sale.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record SaleRequest(
        @NotNull Long customerId,
        @NotEmpty List<@Valid SaleItemRequest> items) {
}
