package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.model.TicketZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketZoneRepository extends JpaRepository<TicketZone, Long> {

    /**
     *
     */
    List<TicketZone> findByEventIdOrderByDisplayOrderAsc(Long eventId);

    /**
     *
     */
    List<TicketZone> findByEventIdAndIsActiveTrueOrderByDisplayOrderAsc(Long eventId);

    /**
     *
     */
    Optional<TicketZone> findByEventIdAndZoneCode(Long eventId, String zoneCode);

    /**
     *
     */
    boolean existsByEventIdAndZoneCode(Long eventId, String zoneCode);

    /**
     *
     */
    @Query("SELECT MIN(tz.price) FROM TicketZone tz WHERE tz.event.id = :eventId AND tz.isActive = true")
    BigDecimal findMinPriceByEventId(@Param("eventId") Long eventId);

    /**
     *
     */
    @Query("SELECT MAX(tz.price) FROM TicketZone tz WHERE tz.event.id = :eventId AND tz.isActive = true")
    BigDecimal findMaxPriceByEventId(@Param("eventId") Long eventId);

    /**
     *
     */
    @Query("SELECT COALESCE(SUM(tz.totalCapacity), 0) FROM TicketZone tz WHERE tz.event.id = :eventId AND tz.isActive = true")
    Integer getTotalCapacityByEventId(@Param("eventId") Long eventId);

    /**
     *
     */
    @Query("SELECT COALESCE(SUM(tz.availableCapacity), 0) FROM TicketZone tz WHERE tz.event.id = :eventId AND tz.isActive = true")
    Integer getAvailableCapacityByEventId(@Param("eventId") Long eventId);

    /**
     *
     */
    Long countByEventId(Long eventId);

    /**
     *
     */
    @Query("SELECT CASE WHEN SUM(tz.availableCapacity) > 0 THEN false ELSE true END FROM TicketZone tz WHERE tz.event.id = :eventId AND tz.isActive = true")
    Boolean isEventSoldOut(@Param("eventId") Long eventId);
}