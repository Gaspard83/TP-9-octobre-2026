package com.boutique.sale;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.boutique.sale.dto.SaleItemRequest;
import com.boutique.sale.dto.SaleRequest;
import com.boutique.sale.dto.SaleResponse;

@DisplayName("SaleController")
class SaleControllerTests {

    private SaleService service;
    private SaleController controller;

    @BeforeEach
    void setUp() {
        service = mock(SaleService.class);
        controller = new SaleController(service);
    }

    @Test
    @DisplayName("findAll sans customerId delegue au service avec null")
    void findAllWithoutCustomerIdDelegatesToService() {
        List<SaleResponse> expected = List.of(response(1L, 10L));
        when(service.findAll(null)).thenReturn(expected);

        assertSame(expected, controller.findAll(null));

        verify(service).findAll(null);
    }

    @Test
    @DisplayName("findAll avec customerId transmet le filtre au service")
    void findAllWithCustomerIdDelegatesToService() {
        List<SaleResponse> expected = List.of(response(1L, 10L));
        when(service.findAll(10L)).thenReturn(expected);

        assertSame(expected, controller.findAll(10L));

        verify(service).findAll(10L);
    }

    @Test
    @DisplayName("findAll retourne sans modification une liste vide")
    void findAllReturnsAnEmptyList() {
        List<SaleResponse> expected = List.of();
        when(service.findAll(null)).thenReturn(expected);

        assertSame(expected, controller.findAll(null));

        verify(service).findAll(null);
    }

    @Test
    @DisplayName("findById transmet l'identifiant au service")
    void findByIdDelegatesToService() {
        SaleResponse expected = response(5L, 2L);
        when(service.findById(5L)).thenReturn(expected);

        assertSame(expected, controller.findById(5L));

        verify(service).findById(5L);
    }

    @Test
    @DisplayName("create transmet la requete au service")
    void createDelegatesToService() {
        SaleRequest request = new SaleRequest(2L, List.of(new SaleItemRequest(1L, 3)));
        SaleResponse expected = response(12L, 2L);
        when(service.create(request)).thenReturn(expected);

        assertSame(expected, controller.create(request));

        verify(service).create(request);
    }

    private SaleResponse response(Long id, Long customerId) {
        return new SaleResponse(id, customerId, "Ada Lovelace", LocalDateTime.now(), BigDecimal.valueOf(100.0), List.of());
    }
}
