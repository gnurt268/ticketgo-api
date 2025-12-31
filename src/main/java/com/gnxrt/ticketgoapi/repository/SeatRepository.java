package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.enums.SeatStatus;
import com.gnxrt.ticketgoapi.model.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Tìm tất cả ghế của một zone, sắp xếp theo hàng và số ghế
     */
    List<Seat> findByTicketZoneIdOrderByRowLabelAscSeatNumberAsc(Long ticketZoneId);

    /**
     * Tìm ghế theo zone và seat code
     */
    Optional<Seat> findByTicketZoneIdAndSeatCode(Long ticketZoneId, String seatCode);

    /**
     * Kiểm tra seat code đã tồn tại trong zone chưa
     */
    boolean existsByTicketZoneIdAndSeatCode(Long ticketZoneId, String seatCode);

    /**
     * Tìm ghế theo status
     */
    List<Seat> findByTicketZoneIdAndStatus(Long ticketZoneId, SeatStatus status);

    /**
     * Tìm ghế available của zone
     */
    List<Seat> findByTicketZoneIdAndStatusOrderByRowLabelAscSeatNumberAsc(Long ticketZoneId, SeatStatus status);

    /**
     * Đếm ghế theo status trong zone
     */
    Long countByTicketZoneIdAndStatus(Long ticketZoneId, SeatStatus status);

    /**
     * Tìm ghế đã reserved bởi user
     */
    List<Seat> findByReservedById(Long userId);

    /**
     * Tìm ghế reserved bởi user trong zone cụ thể
     */
    List<Seat> findByTicketZoneIdAndReservedById(Long ticketZoneId, Long userId);

    /**
     * Tìm ghế có reservation đã hết hạn
     */
    @Query("SELECT s FROM Seat s WHERE s.status = 'RESERVED' AND s.reservedUntil < :now")
    List<Seat> findExpiredReservations(@Param("now") LocalDateTime now);

    /**
     * Giải phóng các reservation đã hết hạn
     */
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.reservedBy = null, s.reservedUntil = null " +
            "WHERE s.status = 'RESERVED' AND s.reservedUntil < :now")
    int releaseExpiredReservations(@Param("now") LocalDateTime now);

    /**
     * Giải phóng reservation của user cụ thể trong zone
     */
    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.reservedBy = null, s.reservedUntil = null " +
            "WHERE s.ticketZone.id = :zoneId AND s.reservedBy.id = :userId AND s.status = 'RESERVED'")
    int releaseUserReservations(@Param("zoneId") Long zoneId, @Param("userId") Long userId);

    /**
     * Đếm tổng số ghế trong zone
     */
    Long countByTicketZoneId(Long ticketZoneId);

    /**
     * Lấy số hàng distinct trong zone
     */
    @Query("SELECT DISTINCT s.rowLabel FROM Seat s WHERE s.ticketZone.id = :zoneId ORDER BY s.rowLabel")
    List<String> findDistinctRowsByZoneId(@Param("zoneId") Long ticketZoneId);

    /**
     * Lấy số ghế tối đa mỗi hàng
     */
    @Query("SELECT MAX(s.seatNumber) FROM Seat s WHERE s.ticketZone.id = :zoneId")
    Integer findMaxSeatNumberByZoneId(@Param("zoneId") Long ticketZoneId);

    /**
     * Tìm nhiều ghế theo danh sách ID
     */
    List<Seat> findByIdIn(List<Long> ids);

    /**
     * Kiểm tra tất cả ghế có available không
     */
    @Query("SELECT CASE WHEN COUNT(s) = :count THEN true ELSE false END FROM Seat s " +
            "WHERE s.id IN :seatIds AND s.status = 'AVAILABLE'")
    boolean areAllSeatsAvailable(@Param("seatIds") List<Long> seatIds, @Param("count") long count);

    /**
     * Tìm ghế theo event ID (qua ticket zone)
     */
    @Query("SELECT s FROM Seat s WHERE s.ticketZone.event.id = :eventId ORDER BY s.ticketZone.displayOrder, s.rowLabel, s.seatNumber")
    List<Seat> findByEventId(@Param("eventId") Long eventId);

    /**
     * Xóa tất cả ghế của zone
     */
    @Modifying
    @Query("DELETE FROM Seat s WHERE s.ticketZone.id = :zoneId")
    void deleteByTicketZoneId(@Param("zoneId") Long ticketZoneId);
}