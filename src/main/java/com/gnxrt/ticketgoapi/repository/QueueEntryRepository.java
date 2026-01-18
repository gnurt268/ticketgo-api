package com.gnxrt.ticketgoapi.repository;

import com.gnxrt.ticketgoapi.enums.QueueEntryStatus;
import com.gnxrt.ticketgoapi.model.QueueEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface QueueEntryRepository extends JpaRepository<QueueEntry, Long> {

    /**
     * Tìm entry theo visitor token
     */
    Optional<QueueEntry> findByVisitorToken(String visitorToken);

    /**
     * Tìm entry theo visitor token và waiting room ID
     */
    Optional<QueueEntry> findByVisitorTokenAndWaitingRoomId(String visitorToken, Long waitingRoomId);

    /**
     * Tìm entry theo access token
     */
    Optional<QueueEntry> findByAccessToken(String accessToken);

    /**
     * Tìm entry của user trong waiting room
     */
    Optional<QueueEntry> findByWaitingRoomIdAndUserId(Long waitingRoomId, Long userId);

    /**
     * Kiểm tra user đã join waiting room chưa
     */
    boolean existsByWaitingRoomIdAndUserId(Long waitingRoomId, Long userId);

    /**
     * Đếm số entries trong waiting room
     */
    long countByWaitingRoomId(Long waitingRoomId);

    /**
     * Đếm số entries theo status
     */
    long countByWaitingRoomIdAndStatus(Long waitingRoomId, QueueEntryStatus status);

    /**
     * Lấy danh sách entries đang WAITING
     */
    List<QueueEntry> findByWaitingRoomIdAndStatusOrderByQueuePositionAsc(
            Long waitingRoomId, QueueEntryStatus status);

    /**
     * Lấy N entries tiếp theo để cho vào
     */
    @Query("SELECT qe FROM QueueEntry qe WHERE qe.waitingRoom.id = :waitingRoomId " +
            "AND qe.status = 'WAITING' " +
            "AND qe.queuePosition IS NOT NULL " +
            "ORDER BY qe.queuePosition ASC " +
            "LIMIT :limit")
    List<QueueEntry> findNextEntriesToAdmit(@Param("waitingRoomId") Long waitingRoomId,
                                            @Param("limit") int limit);

    /**
     * Lấy entries đã expired
     */
    @Query("SELECT qe FROM QueueEntry qe WHERE qe.waitingRoom.id = :waitingRoomId " +
            "AND qe.status = 'SHOPPING' " +
            "AND qe.expiresAt < :now")
    List<QueueEntry> findExpiredShoppingSessions(@Param("waitingRoomId") Long waitingRoomId,
                                                 @Param("now") LocalDateTime now);

    /**
     * Lấy entries READY nhưng chưa vào shopping
     */
    @Query("SELECT qe FROM QueueEntry qe WHERE qe.waitingRoom.id = :waitingRoomId " +
            "AND qe.status = 'READY' " +
            "AND qe.notifiedAt < :timeout")
    List<QueueEntry> findExpiredReadyEntries(@Param("waitingRoomId") Long waitingRoomId,
                                             @Param("timeout") LocalDateTime timeout);

    /**
     * Đếm số người đang shopping
     */
    @Query("SELECT COUNT(qe) FROM QueueEntry qe WHERE qe.waitingRoom.id = :waitingRoomId " +
            "AND qe.status = 'SHOPPING'")
    long countActiveShoppingSessions(@Param("waitingRoomId") Long waitingRoomId);

    /**
     * Update status theo batch
     */
    @Modifying
    @Query("UPDATE QueueEntry qe SET qe.status = :newStatus, qe.completedAt = :now " +
            "WHERE qe.id IN :ids")
    int updateStatusBatch(@Param("ids") List<Long> ids,
                          @Param("newStatus") QueueEntryStatus newStatus,
                          @Param("now") LocalDateTime now);

    /**
     * Lấy max queue position hiện tại
     */
    @Query("SELECT COALESCE(MAX(qe.queuePosition), 0) FROM QueueEntry qe " +
            "WHERE qe.waitingRoom.id = :waitingRoomId")
    int getMaxQueuePosition(@Param("waitingRoomId") Long waitingRoomId);

    /**
     * Lấy danh sách entries cho analytics
     */
    @Query("SELECT qe FROM QueueEntry qe WHERE qe.waitingRoom.id = :waitingRoomId " +
            "ORDER BY qe.joinedAt ASC")
    List<QueueEntry> findAllByWaitingRoomIdOrderByJoinedAt(@Param("waitingRoomId") Long waitingRoomId);

    /**
     * Xóa entries cũ (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM QueueEntry qe WHERE qe.waitingRoom.id = :waitingRoomId " +
            "AND qe.createdAt < :before")
    int deleteOldEntries(@Param("waitingRoomId") Long waitingRoomId,
                         @Param("before") LocalDateTime before);

    /**
     * Tìm entries theo fingerprint (cho bot detection analytics)
     */
    @Query("SELECT qe FROM QueueEntry qe WHERE qe.fingerprint = :fingerprint " +
            "ORDER BY qe.createdAt DESC")
    List<QueueEntry> findByFingerprint(@Param("fingerprint") String fingerprint);

    /**
     * Đếm số lần fingerprint xuất hiện trong thời gian gần đây
     * Dùng để detect suspicious activity
     */
    @Query("SELECT COUNT(qe) FROM QueueEntry qe WHERE qe.fingerprint = :fingerprint " +
            "AND qe.createdAt > :since")
    long countByFingerprintSince(@Param("fingerprint") String fingerprint,
                                 @Param("since") LocalDateTime since);
}