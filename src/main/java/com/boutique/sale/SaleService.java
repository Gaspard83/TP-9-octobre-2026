package com.boutique.sale;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.common.exception.ResourceNotFoundException;
import com.boutique.customer.Customer;
import com.boutique.customer.CustomerService;
import com.boutique.product.Product;
import com.boutique.product.ProductService;
import com.boutique.sale.dto.SaleItemRequest;
import com.boutique.sale.dto.SaleRequest;
import com.boutique.sale.dto.SaleResponse;
import com.boutique.security.CurrentUserService;

/**
 * Orchestration des ventes. Ne manipule pas directement les repositories des autres domaines :
 * passe par CustomerService et ProductService.
 */
@Service
@Transactional
public class SaleService {

    private final SaleRepository repository;
    private final SaleMapper mapper;
    private final CustomerService customerService;
    private final ProductService productService;
    private final CurrentUserService currentUser;

    public SaleService(SaleRepository repository, SaleMapper mapper,
                       CustomerService customerService, ProductService productService,
                       CurrentUserService currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.customerService = customerService;
        this.productService = productService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<SaleResponse> findAll(Long customerId) {
        Long effectiveCustomerId = currentUser.isAdmin() ? customerId : currentUser.currentCustomerId();
        List<Sale> sales = effectiveCustomerId != null
                ? repository.findByCustomerIdOrderBySaleDateDesc(effectiveCustomerId)
                : repository.findAll();
        return sales.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        return mapper.toResponse(getOwnedEntity(id));
    }

    public SaleResponse create(SaleRequest request) {
        currentUser.checkAccessTo(request.customerId());
        Customer customer = customerService.getEntity(request.customerId());
        Sale sale = new Sale(customer);
        for (SaleItemRequest itemRequest : request.items()) {
            Product product = productService.reserveStock(itemRequest.productId(), itemRequest.quantity());
            sale.addItem(new SaleItem(product, itemRequest.quantity()));
        }
        return mapper.toResponse(repository.save(sale));
    }

    private Sale getOwnedEntity(Long id) {
        Sale sale = getEntity(id);
        currentUser.checkAccessTo(sale.getCustomer().getId());
        return sale;
    }

    private Sale getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vente", id));
    }
}
