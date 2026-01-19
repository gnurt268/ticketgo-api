package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.enums.WaitingRoomStatus;
import com.gnxrt.ticketgoapi.model.WaitingRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WaitingRoomRepository extends JpaRepository<WaitingRoom, Long> {

    Optional<WaitingRoom> findByEventId(Long eventId);

    boolean existsByEventId(Long eventId);

    @Query("SELECT wr FROM WaitingRoom wr WHERE wr.status = 'SCHEDULED' " +
            "AND wr.isEnabled = true " +
            "AND wr.preQueueStart <= :now")
    List<WaitingRoom> findWaitingRoomsToStartPreQueue(@Param("now") LocalDateTime now);

    @Query("SELECT wr FROM WaitingRoom wr WHERE wr.status = 'PRE_QUEUE' " +
            "AND wr.isEnabled = true " +
            "AND wr.saleStart <= :now")
    List<WaitingRoom> findWaitingRoomsToStartSelling(@Param("now") LocalDateTime now);

    List<WaitingRoom> findByStatusAndIsEnabledTrue(WaitingRoomStatus status);

    @Query("SELECT wr FROM WaitingRoom wr WHERE wr.status = 'SELLING' " +
            "AND wr.isEnabled = true " +
            "AND wr.saleEnd IS NOT NULL " +
            "AND wr.saleEnd <= :now")
    List<WaitingRoom> findWaitingRoomsToEnd(@Param("now") LocalDateTime now);

    @Query("SELECT wr FROM WaitingRoom wr JOIN wr.event e WHERE e.slug = :slug")
    Optional<WaitingRoom> findByEventSlug(@Param("slug") String slug);

    @Query("SELECT wr FROM WaitingRoom wr WHERE wr.status IN ('SCHEDULED', 'PRE_QUEUE') " +
            "AND wr.isEnabled = true " +
            "ORDER BY wr.saleStart ASC")
    List<WaitingRoom> findUpcomingWaitingRooms();

    @Query("SELECT wr FROM WaitingRoom wr WHERE wr.status IN ('PRE_QUEUE', 'SELLING') " +
            "AND wr.isEnabled = true")
    List<WaitingRoom> findActiveWaitingRooms();
}