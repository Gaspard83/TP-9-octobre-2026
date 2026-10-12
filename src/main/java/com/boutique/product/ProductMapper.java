package com.boutique.product;

import org.springframework.stereotype.Component;

import com.boutique.category.Category;
import com.boutique.product.dto.ProductRequest;
import com.boutique.product.dto.ProductResponse;

@Component
public class ProductMapper {

    public Product toEntity(ProductRequest request, Category category) {
        return new Product(request.name(), request.description(), request.price(), request.stock(), category);
    }

    public void update(Product product, ProductRequest request, Category category) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());
        product.setCategory(category);
    }

    public ProductResponse toResponse(Product product) {
        Category category = product.getCategory();
        return new ProductResponse(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getStock(), category.getId(), category.getName());
    }
}
