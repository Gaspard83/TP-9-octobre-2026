package com.boutique.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.boutique.category.Category;
import com.boutique.customer.Customer;
import com.boutique.customer.Role;
import com.boutique.product.Product;
import com.boutique.sale.dto.SaleItemResponse;
import com.boutique.sale.dto.SaleResponse;

@DisplayName("SaleMapper")
class SaleMapperTests {

    private final SaleMapper mapper = new SaleMapper();

    @Test
    @DisplayName("toResponse mappe correctement les champs de la vente et ses articles")
    void toResponseMapsAllFields() {
        Customer customer = new Customer("Ada", "Lovelace", "ada@example.com", null, null, "pass", Role.CUSTOMER);
        Category category = new Category("Livres", "Romans");
        Product product = new Product("Clean Code", "Livre", BigDecimal.valueOf(35.0), 10, category);

        Sale sale = new Sale(customer);
        SaleItem item = new SaleItem(product, 2);
        sale.addItem(item);

        SaleResponse response = mapper.toResponse(sale);

        assertEquals(customer.getId(), response.customerId());
        assertEquals("Ada Lovelace", response.customerName());
        assertNotNull(response.saleDate());
        assertEquals(BigDecimal.valueOf(70.0), response.total());
        assertEquals(1, response.items().size());

        SaleItemResponse itemResponse = response.items().get(0);
        assertEquals(product.getId(), itemResponse.productId());
        assertEquals("Clean Code", itemResponse.productName());
        assertEquals(2, itemResponse.quantity());
        assertEquals(BigDecimal.valueOf(35.0), itemResponse.unitPrice());
        assertEquals(BigDecimal.valueOf(70.0), itemResponse.subtotal());
    }

    @Test
    @DisplayName("toResponse gère une vente sans article")
    void toResponseHandlesEmptyItems() {
        Customer customer = new Customer("Bob", "Martin", "bob@example.com", null, null, "pass", Role.CUSTOMER);
        Sale sale = new Sale(customer);

        SaleResponse response = mapper.toResponse(sale);

        assertEquals("Bob Martin", response.customerName());
        assertEquals(BigDecimal.ZERO, response.total());
        assertTrue(response.items().isEmpty());
    }

    @Test
    @DisplayName("toItemResponse mappe correctement un article de vente")
    void toItemResponseMapsItemDirectly() {
        Category category = new Category("Tech", "Gadgets");
        Product product = new Product("Souris", "Optique", BigDecimal.valueOf(25.50), 5, category);
        SaleItem item = new SaleItem(product, 3);

        SaleItemResponse response = mapper.toItemResponse(item);

        assertEquals(product.getId(), response.productId());
        assertEquals("Souris", response.productName());
        assertEquals(3, response.quantity());
        assertEquals(BigDecimal.valueOf(25.50), response.unitPrice());
        assertEquals(BigDecimal.valueOf(76.50), response.subtotal());
    }
}
