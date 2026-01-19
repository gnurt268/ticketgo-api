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

    Optional<QueueEntry> findByVisitorToken(String visitorToken);

    Optional<QueueEntry> findByVisitorTokenAndWaitingRoomId(String visitorToken, Long waitingRoomId);

    Optional<QueueEntry> findByAccessToken(String accessToken);

    Optional<QueueEntry> findByWaitingRoomIdAndUserId(Long waitingRoomId, Long userId);

    boolean existsByWaitingRoomIdAndUserId(Long waitingRoomId, Long userId);

    long countByWaitingRoomId(Long waitingRoomId);

    long countByWaitingRoomIdAndStatus(Long waitingRoomId, QueueEntryStatus status);

    List<QueueEntry> findByWaitingRoomIdAndStatusOrderByQueuePositionAsc(
            Long waitingRoomId, QueueEntryStatus status);

    @Query("SELECT qe FROM QueueEntry qe WHERE qe.waitingRoom.id = :waitingRoomId " +
            "AND qe.status = 'WAITING' " +
            "AND qe.queuePosition IS NOT NULL " +
            "ORDER BY qe.queuePosition ASC " +
            "LIMIT :limit")
    List<QueueEntry> findNextEntriesToAdmit(@Param("waitingRoomId") Long waitingRoomId,
                                            @Param("limit") int limit);

    @Query("SELECT qe FROM QueueEntry qe WHERE qe.waitingRoom.id = :waitingRoomId " +
            "AND qe.status = 'SHOPPING' " +
            "AND qe.expiresAt < :now")
    List<QueueEntry> findExpiredShoppingSessions(@Param("waitingRoomId") Long waitingRoomId,
                                                 @Param("now") LocalDateTime now);

    @Query("SELECT qe FROM QueueEntry qe WHERE qe.waitingRoom.id = :waitingRoomId " +
            "AND qe.status = 'READY' " +
            "AND qe.notifiedAt < :timeout")
    List<QueueEntry> findExpiredReadyEntries(@Param("waitingRoomId") Long waitingRoomId,
                                             @Param("timeout") LocalDateTime timeout);

    @Query("SELECT COUNT(qe) FROM QueueEntry qe WHERE qe.waitingRoom.id = :waitingRoomId " +
            "AND qe.status = 'SHOPPING'")
    long countActiveShoppingSessions(@Param("waitingRoomId") Long waitingRoomId);

    @Modifying
    @Query("UPDATE QueueEntry qe SET qe.status = :newStatus, qe.completedAt = :now " +
            "WHERE qe.id IN :ids")
    int updateStatusBatch(@Param("ids") List<Long> ids,
                          @Param("newStatus") QueueEntryStatus newStatus,
                          @Param("now") LocalDateTime now);

    @Query("SELECT COALESCE(MAX(qe.queuePosition), 0) FROM QueueEntry qe " +
            "WHERE qe.waitingRoom.id = :waitingRoomId")
    int getMaxQueuePosition(@Param("waitingRoomId") Long waitingRoomId);

    @Query("SELECT qe FROM QueueEntry qe WHERE qe.waitingRoom.id = :waitingRoomId " +
            "ORDER BY qe.joinedAt ASC")
    List<QueueEntry> findAllByWaitingRoomIdOrderByJoinedAt(@Param("waitingRoomId") Long waitingRoomId);

    @Modifying
    @Query("DELETE FROM QueueEntry qe WHERE qe.waitingRoom.id = :waitingRoomId " +
            "AND qe.createdAt < :before")
    int deleteOldEntries(@Param("waitingRoomId") Long waitingRoomId,
                         @Param("before") LocalDateTime before);

    @Query("SELECT qe FROM QueueEntry qe WHERE qe.fingerprint = :fingerprint " +
            "ORDER BY qe.createdAt DESC")
    List<QueueEntry> findByFingerprint(@Param("fingerprint") String fingerprint);

    @Query("SELECT COUNT(qe) FROM QueueEntry qe WHERE qe.fingerprint = :fingerprint " +
            "AND qe.createdAt > :since")
    long countByFingerprintSince(@Param("fingerprint") String fingerprint,
                                 @Param("since") LocalDateTime since);
}