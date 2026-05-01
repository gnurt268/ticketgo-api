package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import com.gnxrt.ticketgoapi.model.Order;
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
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderCode(String orderCode);

    boolean existsByOrderCode(String orderCode);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndPaymentStatusOrderByCreatedAtDesc(Long userId, PaymentStatus status, Pageable pageable);

    Page<Order> findByEventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    Page<Order> findByEventIdAndPaymentStatusOrderByCreatedAtDesc(Long eventId, PaymentStatus status, Pageable pageable);

    List<Order> findByEventIdAndPaymentStatus(Long eventId, PaymentStatus status);

    List<Order> findByPaymentStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime createdAt);

    Long countByPaymentStatus(PaymentStatus status);

    Long countByEventIdAndPaymentStatus(Long eventId, PaymentStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.event.id = :eventId AND o.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalRevenueByEventId(@Param("eventId") Long eventId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o JOIN o.payments p WHERE p.status = 'COMPLETED' AND p.paidAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenueBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(o) FROM Order o JOIN o.payments p WHERE p.status = 'COMPLETED' AND p.paidAt BETWEEN :startDate AND :endDate")
    Long countCompletedOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    List<Order> findTop10ByPaymentStatusOrderByCreatedAtDesc(PaymentStatus status);

    @Query("SELECT DATE(p.paidAt) as date, SUM(o.totalAmount) as revenue, COUNT(o) as orderCount " +
            "FROM Order o JOIN o.payments p WHERE p.status = 'COMPLETED' AND p.paidAt BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(p.paidAt) ORDER BY DATE(p.paidAt)")
    List<Object[]> getDailyRevenueStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    boolean existsByUserIdAndEventIdAndPaymentStatus(Long userId, Long eventId, PaymentStatus status);

    // ==================== ORGANIZER STATISTICS METHODS ====================

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.event.organizer.id = :organizerId AND o.paymentStatus = 'COMPLETED'")
    BigDecimal sumRevenueByOrganizerId(@Param("organizerId") Long organizerId);

    @Query("SELECT CAST(p.paidAt AS DATE), SUM(o.totalAmount), COUNT(DISTINCT o.id) " +
            "FROM Order o JOIN o.payments p " +
            "WHERE o.event.organizer.id = :organizerId " +
            "AND p.status = 'COMPLETED' AND p.paidAt >= :from " +
            "GROUP BY CAST(p.paidAt AS DATE) ORDER BY CAST(p.paidAt AS DATE)")
    List<Object[]> findRevenueByDayForOrganizer(@Param("organizerId") Long organizerId, @Param("from") LocalDateTime from);

    @Query("SELECT CAST(p.paidAt AS DATE), COALESCE(SUM(o.totalAmount), 0), COALESCE(SUM(o.quantity), 0) " +
            "FROM Order o JOIN o.payments p " +
            "WHERE p.status = 'COMPLETED' " +
            "AND p.paidAt >= :from AND p.paidAt < :to " +
            "AND (:organizerId IS NULL OR o.event.organizer.id = :organizerId) " +
            "GROUP BY CAST(p.paidAt AS DATE) " +
            "ORDER BY CAST(p.paidAt AS DATE)")
    List<Object[]> findDailyRevenueStats(
            @Param("organizerId") Long organizerId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @Query("SELECT o.event.id, o.event.title, SUM(o.totalAmount) as revenue " +
            "FROM Order o WHERE o.event.organizer.id = :organizerId AND o.paymentStatus = 'COMPLETED' " +
            "GROUP BY o.event.id, o.event.title ORDER BY SUM(o.totalAmount) DESC")
    List<Object[]> findTopEventsByRevenueForOrganizer(@Param("organizerId") Long organizerId, Pageable pageable);

    // ==================== ADMIN STATISTICS METHODS ====================

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o JOIN o.payments p WHERE p.status = 'COMPLETED' AND p.paidAt BETWEEN :start AND :end")
    BigDecimal getRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'COMPLETED'")
    BigDecimal getAverageOrderValue();

    @Query("SELECT o.event.id, o.event.title, SUM(o.totalAmount) as revenue " +
            "FROM Order o WHERE o.paymentStatus = 'COMPLETED' " +
            "GROUP BY o.event.id, o.event.title ORDER BY revenue DESC")
    List<Object[]> getTopEventsByRevenue(Pageable pageable);

    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(Pageable pageable);

    @Query("SELECT CAST(p.paidAt AS DATE), SUM(o.totalAmount) " +
            "FROM Order o JOIN o.payments p WHERE p.status = 'COMPLETED' AND p.paidAt BETWEEN :start AND :end " +
            "GROUP BY CAST(p.paidAt AS DATE) ORDER BY CAST(p.paidAt AS DATE)")
    List<Object[]> getRevenueTrendByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT o.event.category.name, SUM(o.totalAmount) " +
            "FROM Order o WHERE o.paymentStatus = 'COMPLETED' " +
            "GROUP BY o.event.category.name")
    List<Object[]> getRevenueByCategory();

    @Query("SELECT o FROM Order o WHERE " +
            "(:keyword IS NULL OR LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(o.buyerName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(o.buyerEmail) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:paymentStatus IS NULL OR o.paymentStatus = :paymentStatus)")
    Page<Order> findAllForAdmin(
            @Param("keyword") String keyword,
            @Param("paymentStatus") PaymentStatus paymentStatus,
            Pageable pageable
    );
}