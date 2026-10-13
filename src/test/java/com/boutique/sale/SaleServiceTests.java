package com.boutique.sale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.access.AccessDeniedException;

import com.boutique.category.Category;
import com.boutique.common.exception.BusinessException;
import com.boutique.common.exception.ResourceNotFoundException;
import com.boutique.customer.Customer;
import com.boutique.customer.CustomerService;
import com.boutique.customer.Role;
import com.boutique.product.Product;
import com.boutique.product.ProductService;
import com.boutique.sale.dto.SaleItemRequest;
import com.boutique.sale.dto.SaleItemResponse;
import com.boutique.sale.dto.SaleRequest;
import com.boutique.sale.dto.SaleResponse;
import com.boutique.security.CurrentUserService;

@DisplayName("SaleService")
class SaleServiceTests {

    private SaleRepository repository;
    private SaleMapper mapper;
    private CustomerService customerService;
    private ProductService productService;
    private CurrentUserService currentUser;
    private SaleService service;

    @BeforeEach
    void setUp() {
        repository = mock(SaleRepository.class);
        mapper = mock(SaleMapper.class);
        customerService = mock(CustomerService.class);
        productService = mock(ProductService.class);
        currentUser = mock(CurrentUserService.class);
        service = new SaleService(repository, mapper, customerService, productService, currentUser);
    }

    @Test
    @DisplayName("findAll pour un ADMIN sans filtre retourne toutes les ventes")
    void findAllAsAdminWithoutFilterReturnsAllSales() {
        when(currentUser.isAdmin()).thenReturn(true);
        Sale sale1 = new Sale(customer(1L));
        Sale sale2 = new Sale(customer(2L));
        SaleResponse response1 = response(1L, 1L);
        SaleResponse response2 = response(2L, 2L);

        when(repository.findAll()).thenReturn(List.of(sale1, sale2));
        when(mapper.toResponse(sale1)).thenReturn(response1);
        when(mapper.toResponse(sale2)).thenReturn(response2);

        assertEquals(List.of(response1, response2), service.findAll(null));

        verify(repository).findAll();
        verify(repository, never()).findByCustomerIdOrderBySaleDateDesc(any());
    }

    @Test
    @DisplayName("findAll pour un ADMIN avec filtre retourne les ventes du client demandé")
    void findAllAsAdminWithFilterReturnsFilteredSales() {
        when(currentUser.isAdmin()).thenReturn(true);
        Sale sale = new Sale(customer(5L));
        SaleResponse response = response(1L, 5L);

        when(repository.findByCustomerIdOrderBySaleDateDesc(5L)).thenReturn(List.of(sale));
        when(mapper.toResponse(sale)).thenReturn(response);

        assertEquals(List.of(response), service.findAll(5L));

        verify(repository).findByCustomerIdOrderBySaleDateDesc(5L);
        verify(repository, never()).findAll();
    }

    @Test
    @DisplayName("findAll pour un client non-admin force le filtre sur son propre identifiant")
    void findAllAsCustomerForcesCurrentCustomerId() {
        when(currentUser.isAdmin()).thenReturn(false);
        when(currentUser.currentCustomerId()).thenReturn(3L);
        Sale sale = new Sale(customer(3L));
        SaleResponse response = response(1L, 3L);

        when(repository.findByCustomerIdOrderBySaleDateDesc(3L)).thenReturn(List.of(sale));
        when(mapper.toResponse(sale)).thenReturn(response);

        // Même si on passe customerId = 99L, c'est son propre id 3L qui est utilisé
        assertEquals(List.of(response), service.findAll(99L));

        verify(repository).findByCustomerIdOrderBySaleDateDesc(3L);
        verify(repository, never()).findByCustomerIdOrderBySaleDateDesc(99L);
    }

    @Test
    @DisplayName("findAll accepte une liste vide")
    void findAllReturnsEmptyList() {
        when(currentUser.isAdmin()).thenReturn(true);
        when(repository.findAll()).thenReturn(List.of());

        assertEquals(List.of(), service.findAll(null));
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("findById vérifie l'accès du client propriétaire avant de renvoyer la vente")
    void findByIdChecksAccessThenReturnsMappedSale() {
        Customer customer = customer(7L);
        Sale sale = new Sale(customer);
        SaleResponse expected = response(10L, 7L);

        when(repository.findById(10L)).thenReturn(Optional.of(sale));
        when(mapper.toResponse(sale)).thenReturn(expected);

        assertSame(expected, service.findById(10L));

        InOrder order = inOrder(repository, currentUser, mapper);
        order.verify(repository).findById(10L);
        order.verify(currentUser).checkAccessTo(7L);
        order.verify(mapper).toResponse(sale);
    }

    @Test
    @DisplayName("findById lève ResourceNotFoundException si la vente n'existe pas")
    void findByIdThrowsWhenNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
        verify(currentUser, never()).checkAccessTo(any());
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("findById propage l'accès refusé sans mapper")
    void findByIdPropagatesAccessDenied() {
        Customer customer = customer(7L);
        Sale sale = new Sale(customer);
        when(repository.findById(10L)).thenReturn(Optional.of(sale));
        org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                .when(currentUser).checkAccessTo(7L);

        assertThrows(AccessDeniedException.class, () -> service.findById(10L));
        verifyNoInteractions(mapper);
    }

    @Test
    @DisplayName("create vérifie l'accès, réserve le stock, persiste la vente et retourne la réponse")
    void createSuccess() {
        SaleRequest request = new SaleRequest(1L, List.of(new SaleItemRequest(10L, 2)));
        Customer customer = customer(1L);
        Product product = product(10L, 20);
        SaleResponse expected = response(100L, 1L);

        when(customerService.getEntity(1L)).thenReturn(customer);
        when(productService.reserveStock(10L, 2)).thenReturn(product);
        when(repository.save(any(Sale.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(Sale.class))).thenReturn(expected);

        SaleResponse result = service.create(request);

        assertSame(expected, result);

        InOrder order = inOrder(currentUser, customerService, productService, repository, mapper);
        order.verify(currentUser).checkAccessTo(1L);
        order.verify(customerService).getEntity(1L);
        order.verify(productService).reserveStock(10L, 2);
        order.verify(repository).save(any(Sale.class));
        order.verify(mapper).toResponse(any(Sale.class));
    }

    @Test
    @DisplayName("create propage BusinessException quand le stock est insuffisant sans sauvegarder")
    void createFailsWhenStockInsufficient() {
        SaleRequest request = new SaleRequest(1L, List.of(new SaleItemRequest(10L, 999)));
        Customer customer = customer(1L);

        when(customerService.getEntity(1L)).thenReturn(customer);
        when(productService.reserveStock(10L, 999)).thenThrow(new BusinessException("Stock insuffisant"));

        assertThrows(BusinessException.class, () -> service.create(request));

        verify(repository, never()).save(any());
        verifyNoInteractions(mapper);
    }

    private Customer customer(Long id) {
        Customer c = new Customer("Ada", "Lovelace", "ada" + id + "@example.com", null, null, "pass", Role.CUSTOMER);
        try {
            var field = Customer.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(c, id);
        } catch (Exception ignored) {}
        return c;
    }

    private Product product(Long id, int stock) {
        Category cat = new Category("Cat", "Desc");
        Product p = new Product("Prod " + id, "Desc", BigDecimal.valueOf(15.0), stock, cat);
        try {
            var field = Product.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(p, id);
        } catch (Exception ignored) {}
        return p;
    }

    private SaleResponse response(Long id, Long customerId) {
        return new SaleResponse(id, customerId, "Ada Lovelace", LocalDateTime.now(), BigDecimal.valueOf(30.0), List.of());
    }
}
