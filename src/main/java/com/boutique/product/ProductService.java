package com.boutique.product;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.boutique.category.Category;
import com.boutique.category.CategoryService;
import com.boutique.common.exception.BusinessException;
import com.boutique.common.exception.ResourceNotFoundException;
import com.boutique.product.dto.ProductRequest;
import com.boutique.product.dto.ProductResponse;

@Service
@Transactional
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository repository;
    private final ProductMapper mapper;
    private final CategoryService categoryService;

    public ProductService(ProductRepository repository, ProductMapper mapper, CategoryService categoryService) {
        log.info("Entrée {}", "ProductService");
        this.repository = repository;
        this.mapper = mapper;
        this.categoryService = categoryService;
        log.info("Sortie {}", "ProductService");
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(Long categoryId, String name) {
        log.info("Entrée {}", "findAll");
        List<Product> products;
        if (categoryId != null) {
            log.debug("Recherche des produits par catégorie : categoryId={}", categoryId);
            products = repository.findByCategoryId(categoryId);
        } else if (name != null && !name.isBlank()) {
            log.debug("Recherche des produits par nom : name={}", name);
            products = repository.findByNameContainingIgnoreCase(name);
        } else {
            log.debug("Recherche de {}", "tous les produits");
            products = repository.findAll();
        }
        List<ProductResponse> responses = products.stream().map(mapper::toResponse).toList();
        log.info("Sortie {} : {} produit(s)", "findAll", responses.size());
        return responses;
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        log.info("Entrée {} : id={}", "findById", id);
        ProductResponse response = mapper.toResponse(getEntity(id));
        log.info("Sortie {} : id={}", "findById", id);
        return response;
    }

    public ProductResponse create(ProductRequest request) {
        log.info("Entrée {} : categoryId={}", "create", request.categoryId());
        Category category = categoryService.getEntity(request.categoryId());
        Product product = repository.save(mapper.toEntity(request, category));
        log.debug("Produit créé : id={}, categoryId={}", product.getId(), request.categoryId());
        ProductResponse response = mapper.toResponse(product);
        log.info("Sortie {} : id={}", "create", product.getId());
        return response;
    }

    public ProductResponse update(Long id, ProductRequest request) {
        log.info("Entrée {} : id={}, categoryId={}", "update", id, request.categoryId());
        Product product = getEntity(id);
        Category category = categoryService.getEntity(request.categoryId());
        mapper.update(product, request, category);
        ProductResponse response = mapper.toResponse(product);
        log.debug("Produit mis à jour : id={}", id);
        log.info("Sortie {} : id={}", "update", id);
        return response;
    }

    public void delete(Long id) {
        log.info("Entrée {} : id={}", "delete", id);
        repository.delete(getEntity(id));
        repository.flush();
        log.debug("Produit supprimé : id={}", id);
        log.info("Sortie {} : id={}", "delete", id);
    }

    /** Réserve du stock pour une commande ; lève BusinessException si insuffisant. */
    public Product reserveStock(Long productId, int quantity) {
        log.info("Entrée {} : productId={}, quantity={}", "reserveStock", productId, quantity);
        Product product = getEntity(productId);
        if (quantity <= 0) {
            log.warn("Quantité de réservation non positive : productId={}, quantity={}", productId, quantity);
        }
        try {
            product.decreaseStock(quantity);
        } catch (BusinessException exception) {
            log.warn("Stock insuffisant : productId={}, stock={}, quantity={}",
                    productId, product.getStock(), quantity);
            throw exception;
        }
        log.debug("Stock réservé : productId={}, stock restant={}", productId, product.getStock());
        log.info("Sortie {}", "reserveStock");
        return product;
    }

    public void releaseStock(Long productId, int quantity) {
        log.info("Entrée {} : productId={}, quantity={}", "releaseStock", productId, quantity);
        Product product = getEntity(productId);
        if (quantity <= 0) {
            log.warn("Quantité de remise en stock non positive : productId={}, quantity={}", productId, quantity);
        }
        product.increaseStock(quantity);
        log.debug("Stock remis à jour : productId={}, stock actuel={}", productId, product.getStock());
        log.info("Sortie {}", "releaseStock");
    }

    @Transactional(readOnly = true)
    public Product getEntity(Long id) {
        log.info("Entrée {} : id={}", "getEntity", id);
        Product product = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Produit introuvable : id={}", id);
                    return new ResourceNotFoundException("Produit", id);
                });
        log.debug("Produit récupéré : id={}", id);
        log.info("Sortie {} : id={}", "getEntity", id);
        return product;
    }
}
