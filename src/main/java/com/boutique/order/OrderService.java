package com.boutique.order;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.common.exception.ResourceNotFoundException;
import com.boutique.customer.CustomerService;
import com.boutique.order.dto.OrderItemRequest;
import com.boutique.order.dto.OrderRequest;
import com.boutique.order.dto.OrderResponse;
import com.boutique.product.Product;
import com.boutique.product.ProductService;
import com.boutique.security.CurrentUserService;

/**
 * Orchestration des commandes. Ne touche jamais directement aux repositories des autres
 * domaines : il passe par CustomerService et ProductService.
 */
@Service
@Transactional
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final CustomerService customerService;
    private final ProductService productService;
    private final CurrentUserService currentUser;

    public OrderService(OrderRepository repository, OrderMapper mapper,
                        CustomerService customerService, ProductService productService,
                        CurrentUserService currentUser) {
        this.repository = repository;
        this.mapper = mapper;
        this.customerService = customerService;
        this.productService = productService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll(Long customerId) {
        // Un client ne voit que ses propres commandes, quel que soit le filtre demandé.
        Long effectiveCustomerId = currentUser.isAdmin() ? customerId : currentUser.currentCustomerId();
        List<Order> orders = effectiveCustomerId != null
                ? repository.findByCustomerIdOrderByCreatedAtDesc(effectiveCustomerId)
                : repository.findAll();
        return orders.stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(Long id) {
        return mapper.toResponse(getOwnedEntity(id));
    }

    public OrderResponse create(OrderRequest request) {
        currentUser.checkAccessTo(request.customerId());
        Order order = new Order(customerService.getEntity(request.customerId()));
        for (OrderItemRequest itemRequest : request.items()) {
            Product product = productService.reserveStock(itemRequest.productId(), itemRequest.quantity());
            order.addItem(new OrderItem(product, itemRequest.quantity()));
        }
        return mapper.toResponse(repository.save(order));
    }

    public OrderResponse changeStatus(Long id, OrderStatus newStatus) {
        return applyStatus(getEntity(id), newStatus);
    }

    /** Annulation : autorisée au propriétaire de la commande ou à un administrateur. */
    public OrderResponse cancel(Long id) {
        return applyStatus(getOwnedEntity(id), OrderStatus.CANCELLED);
    }

    private OrderResponse applyStatus(Order order, OrderStatus newStatus) {
        order.changeStatus(newStatus);
        if (newStatus == OrderStatus.CANCELLED) {
            order.getItems().forEach(item ->
                    productService.releaseStock(item.getProduct().getId(), item.getQuantity()));
        }
        return mapper.toResponse(order);
    }

    private Order getOwnedEntity(Long id) {
        Order order = getEntity(id);
        currentUser.checkAccessTo(order.getCustomer().getId());
        return order;
    }

    private Order getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", id));
    }
}
