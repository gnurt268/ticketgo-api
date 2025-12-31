package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.enums.CheckInMethod;
import com.gnxrt.ticketgoapi.enums.TicketStatus;
import com.gnxrt.ticketgoapi.model.Ticket;
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
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     *
     */
    Optional<Ticket> findByTicketCode(String ticketCode);

    /**
     *
     */
    Optional<Ticket> findByQrCode(String qrCode);

    /**
     *
     */
    boolean existsByTicketCode(String ticketCode);

    /**
     *
     */
    boolean existsByQrCode(String qrCode);

    /**
     *
     */
    List<Ticket> findByOrderId(Long orderId);

    /**
     *
     */
    Page<Ticket> findByEventIdOrderByCreatedAtDesc(Long eventId, Pageable pageable);

    /**
     *
     */
    Page<Ticket> findByEventIdAndStatusOrderByCreatedAtDesc(Long eventId, TicketStatus status, Pageable pageable);

    /**
     *
     */
    @Query("SELECT t FROM Ticket t WHERE t.order.user.id = :userId ORDER BY t.createdAt DESC")
    Page<Ticket> findByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     *
     */
    @Query("SELECT t FROM Ticket t WHERE t.order.user.id = :userId AND t.event.id = :eventId AND t.status = 'ACTIVE'")
    List<Ticket> findActiveTicketsByUserAndEvent(@Param("userId") Long userId, @Param("eventId") Long eventId);

    /**
     *
     */
    Page<Ticket> findByHolderEmailOrderByCreatedAtDesc(String holderEmail, Pageable pageable);

    /**
     *
     */
    Long countByStatus(TicketStatus status);

    /**
     *
     */
    Long countByEventIdAndStatus(Long eventId, TicketStatus status);

    /**
     *
     */
    Long countByEventIdAndIsCheckedInTrue(Long eventId);

    /**
     *
     */
    List<Ticket> findByEventIdAndStatusAndIsCheckedInFalse(Long eventId, TicketStatus status);

    /**
     *
     */
    Optional<Ticket> findBySeatId(Long seatId);

    /**
     *
     */
    Page<Ticket> findByTicketZoneIdOrderByCreatedAtDesc(Long ticketZoneId, Pageable pageable);

    /**
     *
     */
    boolean existsBySeatIdAndStatusIn(Long seatId, List<TicketStatus> statuses);

    /**
     *
     */
    @Query("SELECT t FROM Ticket t WHERE t.event.id = :eventId AND t.faceEmbeddingId IS NOT NULL AND t.status = 'ACTIVE'")
    List<Ticket> findTicketsWithFaceByEventId(@Param("eventId") Long eventId);

    /**
     *
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.event.id = :eventId AND t.faceEmbeddingId IS NOT NULL AND t.status = 'ACTIVE'")
    Long countTicketsWithFaceByEventId(@Param("eventId") Long eventId);

    // ==================== ADMIN STATISTICS METHODS ====================

    /**
     *
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.isCheckedIn = true")
    Long countCheckedInTickets();

    /**
     *
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = 'ACTIVE' AND t.createdAt BETWEEN :start AND :end")
    Long countTicketsSoldBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     *
     */
    @Query("SELECT (COUNT(CASE WHEN t.isCheckedIn = true THEN 1 END) * 100.0 / COUNT(t)) FROM Ticket t WHERE t.status = 'ACTIVE'")
    Double getCheckInRate();

    /**
     *
     */
    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.checkedInBy = :method")
    Long countByCheckInMethod(@Param("method") CheckInMethod method);

    /**
     *
     */
    @Query("SELECT CAST(t.createdAt AS DATE), COUNT(t) " +
            "FROM Ticket t WHERE t.status = 'ACTIVE' AND t.createdAt BETWEEN :start AND :end " +
            "GROUP BY CAST(t.createdAt AS DATE) ORDER BY CAST(t.createdAt AS DATE)")
    List<Object[]> getTicketSalesTrendByDay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}