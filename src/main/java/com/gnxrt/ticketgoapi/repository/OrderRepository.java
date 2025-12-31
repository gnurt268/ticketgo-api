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

    /**
     *
     */
    Optional<Order> findByOrderCode(String orderCode);

    /**
     *
     */
    boolean existsByOrderCode(String orderCode);

    /**
     *
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     *
     */
    Page<Order> findByUserIdAndPaymentStatusOrderByCreatedAtDesc(Long userId, PaymentStatus status, Pageable pageable);

    /**
     *
     */
    Page<Order> findByEventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    /**
     *
     */
    Page<Order> findByEventIdAndPaymentStatusOrderByCreatedAtDesc(Long eventId, PaymentStatus status, Pageable pageable);

    /**
     *
     */
    List<Order> findByPaymentStatusAndCreatedAtBefore(PaymentStatus status, LocalDateTime createdAt);

    /**
     *
     */
    Long countByPaymentStatus(PaymentStatus status);

    /**
     *
     */
    Long countByEventIdAndPaymentStatus(Long eventId, PaymentStatus status);

    /**
     *
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.event.id = :eventId AND o.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalRevenueByEventId(@Param("eventId") Long eventId);

    /**
     *
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'COMPLETED' AND o.paidAt BETWEEN :startDate AND :endDate")
    BigDecimal getTotalRevenueBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     *
     */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.paymentStatus = 'COMPLETED' AND o.paidAt BETWEEN :startDate AND :endDate")
    Long countCompletedOrdersBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     *
     */
    List<Order> findTop10ByPaymentStatusOrderByCreatedAtDesc(PaymentStatus status);

    /**
     *
     */
    Optional<Order> findByPaymentTransactionId(String transactionId);

    /**
     *
     */
    @Query("SELECT DATE(o.paidAt) as date, SUM(o.totalAmount) as revenue, COUNT(o) as orderCount " +
            "FROM Order o WHERE o.paymentStatus = 'COMPLETED' AND o.paidAt BETWEEN :startDate AND :endDate " +
            "GROUP BY DATE(o.paidAt) ORDER BY DATE(o.paidAt)")
    List<Object[]> getDailyRevenueStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     *
     */
    boolean existsByUserIdAndEventIdAndPaymentStatus(Long userId, Long eventId, PaymentStatus status);

    // ==================== ADMIN STATISTICS METHODS ====================

    /**
     *
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'COMPLETED'")
    BigDecimal getTotalRevenue();

    /**
     *
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'COMPLETED' AND o.paidAt BETWEEN :start AND :end")
    BigDecimal getRevenueBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     *
     */
    @Query("SELECT COALESCE(AVG(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'COMPLETED'")
    BigDecimal getAverageOrderValue();

    /**
     *
     */
    @Query("SELECT o.event.id, o.event.title, SUM(o.totalAmount) as revenue " +
            "FROM Order o WHERE o.paymentStatus = 'COMPLETED' " +
            "GROUP BY o.event.id, o.event.title ORDER BY revenue DESC")
    List<Object[]> getTopEventsByRevenue(Pageable pageable);

    /**
     *
     */
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    List<Order> findRecentOrders(Pageable pageable);

    /**
     *
     */
    @Query("SELECT CAST(o.paidAt AS DATE), SUM(o.totalAmount) " +
            "FROM Order o WHERE o.paymentStatus = 'COMPLETED' AND o.paidAt BETWEEN :start AND :end " +
            "GROUP BY CAST(o.paidAt AS DATE) ORDER BY CAST(o.paidAt AS DATE)")
    List<Object[]> getRevenueTrendByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     *
     */
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