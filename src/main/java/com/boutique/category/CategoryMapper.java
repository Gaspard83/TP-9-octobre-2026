package com.boutique.category;

import org.springframework.stereotype.Component;

import com.boutique.category.dto.CategoryRequest;
import com.boutique.category.dto.CategoryResponse;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request) {
        return new Category(request.name(), request.description());
    }

    public void update(Category category, CategoryRequest request) {
        category.setName(request.name());
        category.setDescription(request.description());
    }

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.getDescription());
    }
}
