package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     *
     */
    Optional<Review> findByUserIdAndEventId(Long userId, Long eventId);

    /**
     *
     */
    boolean existsByUserIdAndEventId(Long userId, Long eventId);

    /**
     *
     */
    Page<Review> findByEventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    /**
     *
     */
    Page<Review> findByEventIdAndIsApprovedTrueOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    /**
     *
     */
    @Query("SELECT r FROM Review r WHERE r.event.id = :eventId AND r.isApproved = true ORDER BY r.createdAt DESC")
    List<Review> findRecentApprovedReviews(@Param("eventId") Long eventId, Pageable pageable);

    /**
     *
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.event.id = :eventId AND r.isApproved = true")
    Double getAverageRatingByEventId(@Param("eventId") Long eventId);

    /**
     *
     */
    Long countByEventIdAndIsApprovedTrue(Long eventId);

    /**
     *
     */
    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.event.id = :eventId AND r.isApproved = true GROUP BY r.rating")
    List<Object[]> countByRatingForEvent(@Param("eventId") Long eventId);

    /**
     *
     */
    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     *
     */
    Page<Review> findByIsApprovedFalseOrderByCreatedAtAsc(Pageable pageable);

    /**
     *
     */
    Page<Review> findByIsReportedTrueOrderByCreatedAtDesc(Pageable pageable);
}