package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.category.CategoryRequest;
import com.gnxrt.ticketgoapi.dto.response.category.CategoryDTO;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ConflictException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.Category;
import com.gnxrt.ticketgoapi.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public List<CategoryDTO> getAllCategories() {
        log.info("Getting all categories");
        return categoryRepository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<CategoryDTO> getActiveCategories() {
        log.info("Getting active categories");
        return categoryRepository.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public List<CategoryDTO> getCategoriesWithEvents() {
        log.info("Getting categories with events");
        return categoryRepository.findCategoriesWithEvents().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public CategoryDTO getCategoryById(Long id) {
        log.info("Getting category by id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
        return mapToDTO(category);
    }

    public CategoryDTO getCategoryBySlug(String slug) {
        log.info("Getting category by slug: {}", slug);
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));
        return mapToDTO(category);
    }

    @Transactional
    public CategoryDTO createCategory(CategoryRequest request) {
        log.info("Creating new category: {}", request.getName());

        String slug = StringUtils.hasText(request.getSlug())
                ? request.getSlug().trim()
                : generateUniqueSlug(request.getName());

        if (categoryRepository.existsBySlug(slug)) {
            throw new ConflictException("Slug danh mục đã tồn tại: " + slug);
        }

        if (categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Tên danh mục đã tồn tại: " + request.getName());
        }

        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null) {
            displayOrder = (int) categoryRepository.count();
        }

        // isActive có thể null khi client bỏ qua field (bẫy @Builder.Default) -> mặc định true
        boolean isActive = request.getIsActive() == null || request.getIsActive();

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .displayOrder(displayOrder)
                .isActive(isActive)
                .build();

        category = categoryRepository.save(category);
        log.info("Category created successfully with id: {}", category.getId());

        return mapToDTO(category);
    }

    @Transactional
    public CategoryDTO updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        String slug = StringUtils.hasText(request.getSlug())
                ? request.getSlug().trim()
                : (StringUtils.hasText(category.getSlug())
                        ? category.getSlug()
                        : generateUniqueSlug(request.getName()));

        if (!category.getSlug().equals(slug)
                && categoryRepository.existsBySlug(slug)) {
            throw new ConflictException("Slug danh mục đã tồn tại: " + slug);
        }

        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new ConflictException("Tên danh mục đã tồn tại: " + request.getName());
        }

        category.setName(request.getName());
        category.setSlug(slug);
        category.setDescription(request.getDescription());
        category.setIconUrl(request.getIconUrl());

        // Chỉ đổi trạng thái khi client gửi rõ ràng (tránh vô tình bật lại category đã ẩn)
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        category = categoryRepository.save(category);
        log.info("Category updated successfully with id: {}", category.getId());

        return mapToDTO(category);
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting category id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        Long eventCount = categoryRepository.countEventsByCategoryId(id);
        if (eventCount > 0) {
            throw new BadRequestException("Không thể xóa danh mục đang có " + eventCount + " sự kiện");
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully with id: {}", id);
    }

    @Transactional
    public CategoryDTO toggleActive(Long id) {
        log.info("Toggling active status for category id: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        category.setIsActive(!category.getIsActive());
        category = categoryRepository.save(category);

        log.info("Category active status toggled to: {} for id: {}", category.getIsActive(), id);
        return mapToDTO(category);
    }

    /**
     * Sinh slug duy nhất từ name, tự thêm hậu tố -2, -3... nếu trùng.
     */
    private String generateUniqueSlug(String name) {
        String base = toSlug(name);
        if (base.isEmpty()) {
            base = "danh-muc";
        }
        String slug = base;
        int suffix = 2;
        while (categoryRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix++;
        }
        return slug;
    }

    /**
     * Chuẩn hóa chuỗi tiếng Việt thành slug: bỏ dấu, đ->d, thường hóa,
     * thay ký tự không phải [a-z0-9] bằng dấu gạch nối.
     */
    private String toSlug(String input) {
        if (input == null) {
            return "";
        }
        String s = input.trim().toLowerCase(Locale.forLanguageTag("vi"));
        s = s.replace('đ', 'd');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return s.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    }

    private CategoryDTO mapToDTO(Category category) {
        Long eventCount = categoryRepository.countEventsByCategoryId(category.getId());

        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .iconUrl(category.getIconUrl())
                .displayOrder(category.getDisplayOrder())
                .isActive(category.getIsActive())
                .eventCount(eventCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}