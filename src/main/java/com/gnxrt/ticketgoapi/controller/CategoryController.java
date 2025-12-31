package com.gnxrt.ticketgoapi.controller;

import com.gnxrt.ticketgoapi.dto.request.category.CategoryRequest;
import com.gnxrt.ticketgoapi.dto.response.category.CategoryDTO;
import com.gnxrt.ticketgoapi.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * GET /api/categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryDTO>> getActiveCategories() {
        List<CategoryDTO> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * GET /api/categories/with-events
     */
    @GetMapping("/with-events")
    public ResponseEntity<List<CategoryDTO>> getCategoriesWithEvents() {
        List<CategoryDTO> categories = categoryService.getCategoriesWithEvents();
        return ResponseEntity.ok(categories);
    }

    /**
     * GET /api/categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryDTO> getCategoryById(@PathVariable Long id) {
        CategoryDTO category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(category);
    }

    /**
     * GET /api/categories/slug/{slug}
     */
    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryDTO> getCategoryBySlug(@PathVariable String slug) {
        CategoryDTO category = categoryService.getCategoryBySlug(slug);
        return ResponseEntity.ok(category);
    }

    /**
     * GET /api/categories/all
     * ADMIN
     */
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CategoryDTO>> getAllCategories() {
        List<CategoryDTO> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * POST /api/categories
     * ADMIN
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDTO> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryDTO category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(category);
    }

    /**
     * PUT /api/categories/{id}
     * ADMIN
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDTO> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        CategoryDTO category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(category);
    }

    /**
     * DELETE /api/categories/{id}
     * ADMIN
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/categories/{id}/toggle-active
     * ADMIN
     */
    @PatchMapping("/{id}/toggle-active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryDTO> toggleActive(@PathVariable Long id) {
        CategoryDTO category = categoryService.toggleActive(id);
        return ResponseEntity.ok(category);
    }
}