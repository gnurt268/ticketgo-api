package com.gnxrt.ticketgoapi.dto.request.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must not exceed 100 characters")
    private String name;

    // Slug optional: nếu để trống sẽ tự sinh từ name (xem CategoryService.toSlug)
    @Size(max = 100, message = "Category slug must not exceed 100 characters")
    private String slug;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @Size(max = 500, message = "Icon URL must not exceed 500 characters")
    private String iconUrl;

    private Integer displayOrder;

    @Builder.Default
    private Boolean isActive = true;
}