package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.enums.OrganizerRequestStatus;
import com.gnxrt.ticketgoapi.model.OrganizerRequest;
import com.gnxrt.ticketgoapi.model.User;
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
public interface OrganizerRequestRepository extends JpaRepository<OrganizerRequest, Long> {

    Optional<OrganizerRequest> findByUser(User user);

    Optional<OrganizerRequest> findByUserId(Long userId);

    Optional<OrganizerRequest> findByUserAndStatus(User user, OrganizerRequestStatus status);

    Optional<OrganizerRequest> findByUserIdAndStatus(Long userId, OrganizerRequestStatus status);

    boolean existsByUserAndStatus(User user, OrganizerRequestStatus status);

    boolean existsByUserIdAndStatus(Long userId, OrganizerRequestStatus status);

    Long countByStatus(OrganizerRequestStatus status);

    Page<OrganizerRequest> findByStatus(OrganizerRequestStatus status, Pageable pageable);

    @Query("SELECT r FROM OrganizerRequest r WHERE " +
            "(:status IS NULL OR r.status = :status) AND " +
            "(:keyword IS NULL OR LOWER(r.organizationName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(r.user.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(r.user.email) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<OrganizerRequest> findAllWithFilters(
            @Param("status") OrganizerRequestStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<OrganizerRequest> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT r FROM OrganizerRequest r ORDER BY r.createdAt DESC")
    List<OrganizerRequest> findRecentRequests(Pageable pageable);

    @Query("SELECT r.status, COUNT(r) FROM OrganizerRequest r GROUP BY r.status")
    List<Object[]> countByStatusGrouped();
}