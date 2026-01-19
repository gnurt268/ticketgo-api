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

    List<Seat> findByTicketZoneIdOrderByRowLabelAscSeatNumberAsc(Long ticketZoneId);

    Optional<Seat> findByTicketZoneIdAndSeatCode(Long ticketZoneId, String seatCode);

    boolean existsByTicketZoneIdAndSeatCode(Long ticketZoneId, String seatCode);

    List<Seat> findByTicketZoneIdAndStatus(Long ticketZoneId, SeatStatus status);

    List<Seat> findByTicketZoneIdAndStatusOrderByRowLabelAscSeatNumberAsc(Long ticketZoneId, SeatStatus status);

    Long countByTicketZoneIdAndStatus(Long ticketZoneId, SeatStatus status);

    List<Seat> findByReservedById(Long userId);

    List<Seat> findByTicketZoneIdAndReservedById(Long ticketZoneId, Long userId);

    @Query("SELECT s FROM Seat s WHERE s.status = 'RESERVED' AND s.reservedUntil < :now")
    List<Seat> findExpiredReservations(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.reservedBy = null, s.reservedUntil = null " +
            "WHERE s.status = 'RESERVED' AND s.reservedUntil < :now")
    int releaseExpiredReservations(@Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE Seat s SET s.status = 'AVAILABLE', s.reservedBy = null, s.reservedUntil = null " +
            "WHERE s.ticketZone.id = :zoneId AND s.reservedBy.id = :userId AND s.status = 'RESERVED'")
    int releaseUserReservations(@Param("zoneId") Long zoneId, @Param("userId") Long userId);

    Long countByTicketZoneId(Long ticketZoneId);

    @Query("SELECT DISTINCT s.rowLabel FROM Seat s WHERE s.ticketZone.id = :zoneId ORDER BY s.rowLabel")
    List<String> findDistinctRowsByZoneId(@Param("zoneId") Long ticketZoneId);

    @Query("SELECT MAX(s.seatNumber) FROM Seat s WHERE s.ticketZone.id = :zoneId")
    Integer findMaxSeatNumberByZoneId(@Param("zoneId") Long ticketZoneId);

    List<Seat> findByIdIn(List<Long> ids);

    @Query("SELECT CASE WHEN COUNT(s) = :count THEN true ELSE false END FROM Seat s " +
            "WHERE s.id IN :seatIds AND s.status = 'AVAILABLE'")
    boolean areAllSeatsAvailable(@Param("seatIds") List<Long> seatIds, @Param("count") long count);

    @Query("SELECT s FROM Seat s WHERE s.ticketZone.event.id = :eventId ORDER BY s.ticketZone.displayOrder, s.rowLabel, s.seatNumber")
    List<Seat> findByEventId(@Param("eventId") Long eventId);

    @Modifying
    @Query("DELETE FROM Seat s WHERE s.ticketZone.id = :zoneId")
    void deleteByTicketZoneId(@Param("zoneId") Long ticketZoneId);
}