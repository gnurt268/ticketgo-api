package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.enums.Role;
import com.gnxrt.ticketgoapi.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     *
     */
    Optional<User> findByEmail(String email);

    /**
     *
     */
    boolean existsByEmail(String email);

    /**
     *
     */
    Optional<User> findByPhone(String phone);

    /**
     *
     */
    boolean existsByPhone(String phone);

    /**
     *
     */
    Page<User> findByRole(Role role, Pageable pageable);

    /**
     *
     */
    Page<User> findByIsActive(boolean isActive, Pageable pageable);

    /**
     *
     */
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    /**
     *
     */
    Page<User> findByRoleAndIsActive(Role role, boolean isActive, Pageable pageable);

    // ==================== ADMIN STATISTICS METHODS ====================

    /**
     *
     */
    Long countByRole(Role role);

    /**
     *
     */
    Long countByIsActive(boolean isActive);

    /**
     *
     */
    @Query("SELECT u FROM User u WHERE u.createdAt BETWEEN :start AND :end ORDER BY u.createdAt DESC")
    List<User> findUsersCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     *
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt BETWEEN :start AND :end")
    Long countUsersCreatedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     *
     */
    @Query("SELECT u FROM User u LEFT JOIN Event e ON e.organizer.id = u.id WHERE u.role = 'ORGANIZER' GROUP BY u ORDER BY COUNT(e) DESC")
    List<User> findTopOrganizers(Pageable pageable);

    // ==================== USER MANAGEMENT SERVICE METHODS ====================

    /**
     *
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Integer countOrdersByUserId(@Param("userId") Long userId);

    /**
     *
     */
    @Query("SELECT COUNT(e) FROM Event e WHERE e.organizer.id = :userId")
    Integer countEventsOrganizedByUserId(@Param("userId") Long userId);

    /**
     *
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.order.user.id = :userId AND t.status = 'ACTIVE'")
    Integer countTicketsPurchasedByUserId(@Param("userId") Long userId);

    /**
     *
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.user.id = :userId AND o.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalSpentByUserId(@Param("userId") Long userId);

    /**
     *
     */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.user.id = :userId")
    Integer countReviewsByUserId(@Param("userId") Long userId);

    /**
     *
     */
    @Query("SELECT COUNT(i) FROM UserEventInteraction i WHERE i.user.id = :userId")
    Integer countInteractionsByUserId(@Param("userId") Long userId);
}