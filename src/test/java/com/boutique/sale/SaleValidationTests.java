package com.boutique.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.boutique.category.Category;
import com.boutique.customer.Customer;
import com.boutique.customer.Role;
import com.boutique.product.Product;
import com.boutique.sale.dto.SaleItemRequest;
import com.boutique.sale.dto.SaleRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

@DisplayName("Validation des DTO et entités Sale")
class SaleValidationTests {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        factory.close();
    }

    @Test
    @DisplayName("SaleRequest valide ne produit aucune violation")
    void validSaleRequestHasNoViolations() {
        SaleRequest request = new SaleRequest(1L, List.of(new SaleItemRequest(2L, 3)));
        Set<ConstraintViolation<SaleRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("SaleRequest avec customerId nul produit une violation")
    void saleRequestWithNullCustomerIdFails() {
        SaleRequest request = new SaleRequest(null, List.of(new SaleItemRequest(2L, 3)));
        Set<ConstraintViolation<SaleRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
    }

    @Test
    @DisplayName("SaleRequest avec items vide ou nul produit une violation")
    void saleRequestWithEmptyItemsFails() {
        SaleRequest emptyItems = new SaleRequest(1L, List.of());
        SaleRequest nullItems = new SaleRequest(1L, null);

        assertFalse(validator.validate(emptyItems).isEmpty());
        assertFalse(validator.validate(nullItems).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5})
    @DisplayName("SaleItemRequest avec quantite non positive produit une violation")
    void saleItemRequestWithInvalidQuantityFails(int quantity) {
        SaleItemRequest request = new SaleItemRequest(1L, quantity);
        Set<ConstraintViolation<SaleItemRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
    }

    @Test
    @DisplayName("SaleItemRequest avec productId nul produit une violation")
    void saleItemRequestWithNullProductIdFails() {
        SaleItemRequest request = new SaleItemRequest(null, 2);
        Set<ConstraintViolation<SaleItemRequest>> violations = validator.validate(request);
        assertEquals(1, violations.size());
    }

    @Test
    @DisplayName("SaleRequest avec un item invalide propage la violation via @Valid")
    void saleRequestPropagatesItemViolations() {
        SaleRequest request = new SaleRequest(1L, List.of(new SaleItemRequest(null, 0)));
        Set<ConstraintViolation<SaleRequest>> violations = validator.validate(request);
        assertEquals(2, violations.size());
    }

    @Test
    @DisplayName("L'entité Sale calcule le total à l'ajout des articles et protège sa liste")
    void saleCalculatesTotalAndProtectsItemsList() {
        Customer customer = new Customer("John", "Doe", "john@example.com", null, null, "pass", Role.CUSTOMER);
        Category category = new Category("Informatique", "Accessoires");
        Product p1 = new Product("Clavier", "Clavier méca", BigDecimal.valueOf(50.0), 10, category);
        Product p2 = new Product("Souris", "Souris optique", BigDecimal.valueOf(25.0), 10, category);

        Sale sale = new Sale(customer);
        assertNotNull(sale.getSaleDate());
        assertEquals(BigDecimal.ZERO, sale.getTotal());

        SaleItem item1 = new SaleItem(p1, 2);
        assertEquals(BigDecimal.valueOf(100.0), item1.getSubtotal());
        sale.addItem(item1);

        assertEquals(BigDecimal.valueOf(100.0), sale.getTotal());
        assertEquals(1, sale.getItems().size());
        assertEquals(sale, item1.getSale());
        assertEquals(p1, item1.getProduct());
        assertEquals(2, item1.getQuantity());
        assertEquals(BigDecimal.valueOf(50.0), item1.getUnitPrice());
        assertEquals(null, item1.getId());

        assertEquals(customer, sale.getCustomer());
        assertEquals(null, sale.getId());

        SaleItem item2 = new SaleItem(p2, 1);
        assertEquals(BigDecimal.valueOf(25.0), item2.getSubtotal());
        sale.addItem(item2);

        assertEquals(BigDecimal.valueOf(125.0), sale.getTotal());
        assertEquals(2, sale.getItems().size());

        assertThrows(UnsupportedOperationException.class, () -> sale.getItems().add(new SaleItem(p1, 1)));
    }
}
