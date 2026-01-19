package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    List<Category> findByIsActiveTrueOrderByDisplayOrderAsc();

    List<Category> findAllByOrderByDisplayOrderAsc();

    @Query("SELECT COUNT(e) FROM Event e WHERE e.category.id = :categoryId")
    Long countEventsByCategoryId(Long categoryId);

    @Query("SELECT DISTINCT c FROM Category c JOIN c.events e WHERE c.isActive = true ORDER BY c.displayOrder")
    List<Category> findCategoriesWithEvents();
}