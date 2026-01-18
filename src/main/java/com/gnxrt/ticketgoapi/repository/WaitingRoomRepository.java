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

    /**
     * Tìm waiting room theo event
     */
    Optional<WaitingRoom> findByEventId(Long eventId);

    /**
     * Kiểm tra event đã có waiting room chưa
     */
    boolean existsByEventId(Long eventId);

    /**
     * Tìm các waiting room cần chuyển sang PRE_QUEUE
     */
    @Query("SELECT wr FROM WaitingRoom wr WHERE wr.status = 'SCHEDULED' " +
            "AND wr.isEnabled = true " +
            "AND wr.preQueueStart <= :now")
    List<WaitingRoom> findWaitingRoomsToStartPreQueue(@Param("now") LocalDateTime now);

    /**
     * Tìm các waiting room cần chuyển sang SELLING
     */
    @Query("SELECT wr FROM WaitingRoom wr WHERE wr.status = 'PRE_QUEUE' " +
            "AND wr.isEnabled = true " +
            "AND wr.saleStart <= :now")
    List<WaitingRoom> findWaitingRoomsToStartSelling(@Param("now") LocalDateTime now);

    /**
     * Tìm các waiting room đang SELLING (để drain queue)
     */
    List<WaitingRoom> findByStatusAndIsEnabledTrue(WaitingRoomStatus status);

    /**
     * Tìm các waiting room cần kết thúc
     */
    @Query("SELECT wr FROM WaitingRoom wr WHERE wr.status = 'SELLING' " +
            "AND wr.isEnabled = true " +
            "AND wr.saleEnd IS NOT NULL " +
            "AND wr.saleEnd <= :now")
    List<WaitingRoom> findWaitingRoomsToEnd(@Param("now") LocalDateTime now);

    /**
     * Tìm waiting room theo event slug (for public API)
     */
    @Query("SELECT wr FROM WaitingRoom wr JOIN wr.event e WHERE e.slug = :slug")
    Optional<WaitingRoom> findByEventSlug(@Param("slug") String slug);

    /**
     * Lấy danh sách waiting rooms sắp diễn ra (for admin dashboard)
     */
    @Query("SELECT wr FROM WaitingRoom wr WHERE wr.status IN ('SCHEDULED', 'PRE_QUEUE') " +
            "AND wr.isEnabled = true " +
            "ORDER BY wr.saleStart ASC")
    List<WaitingRoom> findUpcomingWaitingRooms();

    /**
     * Lấy danh sách waiting rooms đang active
     */
    @Query("SELECT wr FROM WaitingRoom wr WHERE wr.status IN ('PRE_QUEUE', 'SELLING') " +
            "AND wr.isEnabled = true")
    List<WaitingRoom> findActiveWaitingRooms();
}