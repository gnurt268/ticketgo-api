package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.enums.EventType;
import com.gnxrt.ticketgoapi.model.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Optional<Event> findBySlug(String slug);

    boolean existsBySlug(String slug);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    List<Event> findByStatusOrderByStartDateAsc(EventStatus status);

    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);

    Page<Event> findByOrganizerIdAndStatus(Long organizerId, EventStatus status, Pageable pageable);

    Page<Event> findByCategoryId(Long categoryId, Pageable pageable);

    Page<Event> findByStatusAndOrganizerId(EventStatus status, Long organizerId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.startDate > :now ORDER BY e.startDate ASC")
    Page<Event> findUpcomingPublishedEvents(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.category.id = :categoryId AND e.status = 'PUBLISHED' AND e.startDate > :now ORDER BY e.startDate ASC")
    Page<Event> findUpcomingPublishedEventsByCategory(@Param("categoryId") Long categoryId, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.startDate > :now AND " +
            "(LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.venue) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Event> searchPublishedEvents(@Param("keyword") String keyword, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.isFeatured = true AND e.startDate > :now ORDER BY e.startDate ASC")
    Page<Event> findFeaturedEvents(@Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' AND e.city = :city AND e.startDate > :now ORDER BY e.startDate ASC")
    Page<Event> findEventsByCity(@Param("city") String city, @Param("now") LocalDateTime now, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' ORDER BY e.totalTicketsSold DESC")
    Page<Event> findTopSellingEvents(Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.status = 'PUBLISHED' ORDER BY e.viewCount DESC")
    Page<Event> findMostViewedEvents(Pageable pageable);

    @Query("SELECT e FROM Event e WHERE " +
            "(:status IS NULL OR e.status = :status) AND " +
            "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
            "(:organizerId IS NULL OR e.organizer.id = :organizerId) AND " +
            "(:eventType IS NULL OR e.eventType = :eventType) AND " +
            "(:city IS NULL OR e.city = :city) AND " +
            "(:isFeatured IS NULL OR e.isFeatured = :isFeatured) AND " +
            "(:startAfter IS NULL OR e.startDate > :startAfter)")
    Page<Event> findByFilters(
            @Param("status") EventStatus status,
            @Param("categoryId") Long categoryId,
            @Param("organizerId") Long organizerId,
            @Param("eventType") EventType eventType,
            @Param("city") String city,
            @Param("isFeatured") Boolean isFeatured,
            @Param("startAfter") LocalDateTime startAfter,
            Pageable pageable
    );

    @Query("SELECT DISTINCT e.city FROM Event e WHERE e.status = 'PUBLISHED' AND e.city IS NOT NULL AND e.startDate > :now ORDER BY e.city")
    List<String> findDistinctCitiesOfPublishedEvents(@Param("now") LocalDateTime now);

    // ==================== ADMIN STATISTICS METHODS ====================

    Long countByStatus(EventStatus status);

    @Query("SELECT e FROM Event e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    Page<Event> findPendingEvents(Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.createdAt BETWEEN :start AND :end ORDER BY e.createdAt DESC")
    List<Event> findEventsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.createdAt BETWEEN :start AND :end")
    Long countEventsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    List<Event> findByOrganizerIdAndStatusIn(Long organizerId, List<EventStatus> statuses);

    boolean existsByIdAndOrganizerId(Long eventId, Long organizerId);

    @Query("SELECT e FROM Event e WHERE " +
            "LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(e.venue) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Event> searchAllEvents(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT COUNT(e) FROM Event e WHERE e.status = 'PENDING'")
    Long countPendingEvents();

    Long countByOrganizerId(Long organizerId);

    @Query("SELECT e.status, COUNT(e) FROM Event e WHERE e.organizer.id = :organizerId GROUP BY e.status")
    List<Object[]> countByOrganizerIdGroupByStatus(@Param("organizerId") Long organizerId);

    @Query("SELECT e FROM Event e WHERE e.organizer.id = :organizerId ORDER BY e.createdAt DESC")
    Page<Event> findByOrganizerIdOrderByCreatedAtDesc(@Param("organizerId") Long organizerId, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.organizer.id = :organizerId AND e.status = :status ORDER BY e.createdAt DESC")
    Page<Event> findByOrganizerIdAndStatusOrderByCreatedAtDesc(
            @Param("organizerId") Long organizerId,
            @Param("status") EventStatus status,
            Pageable pageable
    );
}