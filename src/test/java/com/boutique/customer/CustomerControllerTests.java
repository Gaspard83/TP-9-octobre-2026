package com.boutique.customer;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.boutique.customer.dto.CustomerCreateRequest;
import com.boutique.customer.dto.CustomerRequest;
import com.boutique.customer.dto.CustomerResponse;

@DisplayName("CustomerController")
class CustomerControllerTests {

    private CustomerService service;
    private CustomerController controller;

    @BeforeEach
    void setUp() {
        service = mock(CustomerService.class);
        controller = new CustomerController(service);
    }

    @Test
    @DisplayName("create delegue la creation au service")
    void createDelegatesToService() {
        CustomerCreateRequest request = new CustomerCreateRequest("Ada", "Lovelace", "ada@example.com",
                null, null, "secret-pass", null);
        CustomerResponse expected = response(1L);
        when(service.create(request)).thenReturn(expected);

        assertSame(expected, controller.create(request));

        verify(service).create(request);
    }

    @Test
    @DisplayName("findAll retourne les clients du service")
    void findAllDelegatesToService() {
        List<CustomerResponse> expected = List.of(response(1L));
        when(service.findAll()).thenReturn(expected);

        assertSame(expected, controller.findAll());

        verify(service).findAll();
    }

    @Test
    @DisplayName("findAll retourne sans modification une liste vide")
    void findAllReturnsAnEmptyList() {
        List<CustomerResponse> expected = List.of();
        when(service.findAll()).thenReturn(expected);

        assertSame(expected, controller.findAll());

        verify(service).findAll();
    }

    @Test
    @DisplayName("me retourne le profil courant")
    void meDelegatesToService() {
        CustomerResponse expected = response(1L);
        when(service.findCurrent()).thenReturn(expected);

        assertSame(expected, controller.me());

        verify(service).findCurrent();
    }

    @Test
    @DisplayName("findById transmet l'identifiant")
    void findByIdDelegatesToService() {
        CustomerResponse expected = response(8L);
        when(service.findById(8L)).thenReturn(expected);

        assertSame(expected, controller.findById(8L));

        verify(service).findById(8L);
    }

    @Test
    @DisplayName("update transmet l'identifiant et les donnees")
    void updateDelegatesToService() {
        CustomerRequest request = new CustomerRequest("Ada", "Lovelace", "ada@example.com",
                null, null);
        CustomerResponse expected = response(8L);
        when(service.update(8L, request)).thenReturn(expected);

        assertSame(expected, controller.update(8L, request));

        verify(service).update(8L, request);
    }

    @Test
    @DisplayName("delete delegue la suppression")
    void deleteDelegatesToService() {
        controller.delete(8L);

        verify(service).delete(8L);
    }

    private CustomerResponse response(Long id) {
        return new CustomerResponse(id, "Ada", "Lovelace", "ada@example.com",
                null, null, Role.CUSTOMER);
    }
}
