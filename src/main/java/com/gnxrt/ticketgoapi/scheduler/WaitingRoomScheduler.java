package com.gnxrt.ticketgoapi.scheduler;

import com.gnxrt.ticketgoapi.enums.QueueEntryStatus;
import com.gnxrt.ticketgoapi.enums.WaitingRoomStatus;
import com.gnxrt.ticketgoapi.model.QueueEntry;
import com.gnxrt.ticketgoapi.model.WaitingRoom;
import com.gnxrt.ticketgoapi.repository.QueueEntryRepository;
import com.gnxrt.ticketgoapi.repository.WaitingRoomRepository;
import com.gnxrt.ticketgoapi.service.WaitingRoomRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled Jobs cho Virtual Waiting Room
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WaitingRoomScheduler {

    private final WaitingRoomRepository waitingRoomRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final WaitingRoomRedisService redisService;

    /**
     * Job 1: Transition SCHEDULED -> PRE_QUEUE
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void transitionToPreQueue() {
        LocalDateTime now = LocalDateTime.now();
        List<WaitingRoom> rooms = waitingRoomRepository.findWaitingRoomsToStartPreQueue(now);

        for (WaitingRoom room : rooms) {
            try {
                room.setStatus(WaitingRoomStatus.PRE_QUEUE);
                waitingRoomRepository.save(room);

                log.info("Waiting room {} transitioned to PRE_QUEUE for event {}",
                        room.getId(), room.getEvent().getId());
            } catch (Exception e) {
                log.error("Error transitioning waiting room {} to PRE_QUEUE", room.getId(), e);
            }
        }
    }

    /**
     * Job 2: Shuffle & Transition PRE_QUEUE -> SELLING
     */
    @Scheduled(fixedRate = 5000)
    @Transactional
    public void shuffleAndStartSelling() {
        LocalDateTime now = LocalDateTime.now();
        List<WaitingRoom> rooms = waitingRoomRepository.findWaitingRoomsToStartSelling(now);

        for (WaitingRoom room : rooms) {
            try {
                Long eventId = room.getEvent().getId();

                List<Long> shuffledUsers = redisService.shuffleAndCreateQueue(eventId);

                if (shuffledUsers.isEmpty()) {
                    log.warn("No users to shuffle for event {}", eventId);
                }

                for (int i = 0; i < shuffledUsers.size(); i++) {
                    Long userId = shuffledUsers.get(i);
                    int position = i + 1;

                    queueEntryRepository.findByWaitingRoomIdAndUserId(room.getId(), userId)
                            .ifPresent(entry -> {
                                entry.setQueuePosition(position);
                                queueEntryRepository.save(entry);
                            });
                }

                room.setStatus(WaitingRoomStatus.SELLING);
                room.setShuffledAt(now);
                waitingRoomRepository.save(room);

                log.info("Waiting room {} shuffled {} users and started SELLING for event {}",
                        room.getId(), shuffledUsers.size(), eventId);

            } catch (Exception e) {
                log.error("Error shuffling/starting waiting room {}", room.getId(), e);
            }
        }
    }

    /**
     * Job 3: Drain Queue - Cho users vào protected zone
     */
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void drainQueue() {
        List<WaitingRoom> sellingRooms = waitingRoomRepository
                .findByStatusAndIsEnabledTrue(WaitingRoomStatus.SELLING);

        for (WaitingRoom room : sellingRooms) {
            try {
                drainQueueForRoom(room);
            } catch (Exception e) {
                log.error("Error draining queue for waiting room {}", room.getId(), e);
            }
        }
    }

    /**
     * Drain queue cho một waiting room cụ thể
     */
    private void drainQueueForRoom(WaitingRoom room) {
        Long eventId = room.getEvent().getId();

        int activeShoppers = redisService.getActiveShoppersCount(eventId);
        int maxConcurrent = room.getMaxConcurrentUsers();

        if (activeShoppers >= maxConcurrent) {
            log.debug("Event {} at max capacity ({}/{}), not admitting more users",
                    eventId, activeShoppers, maxConcurrent);
            return;
        }
        int usersPerBatch = Math.max(1, room.getThroughputPerMinute() / 6);
        int availableSlots = maxConcurrent - activeShoppers;
        int toAdmit = Math.min(usersPerBatch, availableSlots);

        List<Long> nextUsers = redisService.getNextUsersToAdmit(eventId, toAdmit);

        if (nextUsers.isEmpty()) {
            log.debug("No more users waiting in queue for event {}", eventId);
            return;
        }

        int admitted = 0;
        for (Long userId : nextUsers) {
            try {
                admitUser(room, userId);
                admitted++;
            } catch (Exception e) {
                log.error("Error admitting user {} to event {}", userId, eventId, e);
            }
        }

        redisService.incrementServingPosition(eventId, admitted);

        log.info("Admitted {} users to event {}. Active shoppers: {}",
                admitted, eventId, redisService.getActiveShoppersCount(eventId));
    }

    /**
     * Cho phép user vào
     */
    private void admitUser(WaitingRoom room, Long userId) {
        Long eventId = room.getEvent().getId();

        String accessToken = "AT-" + UUID.randomUUID().toString().replace("-", "");

        redisService.setUserReady(eventId, userId, accessToken, room.getSessionTimeoutMinutes());

        queueEntryRepository.findByWaitingRoomIdAndUserId(room.getId(), userId)
                .ifPresent(entry -> {
                    entry.setStatus(QueueEntryStatus.READY);
                    entry.setAccessToken(accessToken);
                    entry.setNotifiedAt(LocalDateTime.now());
                    entry.setExpiresAt(LocalDateTime.now().plusMinutes(room.getSessionTimeoutMinutes()));
                    queueEntryRepository.save(entry);
                });

        log.debug("User {} is now READY for event {}", userId, eventId);
    }

    /**
     * Job 4: Expire Sessions
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void expireSessions() {
        List<WaitingRoom> sellingRooms = waitingRoomRepository
                .findByStatusAndIsEnabledTrue(WaitingRoomStatus.SELLING);

        for (WaitingRoom room : sellingRooms) {
            try {
                expireSessionsForRoom(room);
            } catch (Exception e) {
                log.error("Error expiring sessions for waiting room {}", room.getId(), e);
            }
        }
    }

    /**
     * Expire sessions cho một waiting room
     */
    private void expireSessionsForRoom(WaitingRoom room) {
        LocalDateTime now = LocalDateTime.now();
        Long eventId = room.getEvent().getId();

        List<QueueEntry> expiredShopping = queueEntryRepository
                .findExpiredShoppingSessions(room.getId(), now);

        for (QueueEntry entry : expiredShopping) {
            redisService.setUserExpired(eventId, entry.getUser().getId());
            entry.setStatus(QueueEntryStatus.EXPIRED);
            entry.setCompletedAt(now);
            queueEntryRepository.save(entry);

            log.info("Expired SHOPPING session for user {} in event {}",
                    entry.getUser().getId(), eventId);
        }

        LocalDateTime readyTimeout = now.minusMinutes(2);
        List<QueueEntry> expiredReady = queueEntryRepository
                .findExpiredReadyEntries(room.getId(), readyTimeout);

        for (QueueEntry entry : expiredReady) {
            redisService.setUserExpired(eventId, entry.getUser().getId());
            entry.setStatus(QueueEntryStatus.EXPIRED);
            entry.setCompletedAt(now);
            queueEntryRepository.save(entry);

            log.info("Expired READY entry for user {} in event {} (did not enter in time)",
                    entry.getUser().getId(), eventId);
        }
    }

    /**
     * Job 5: End Sales
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void endSales() {
        LocalDateTime now = LocalDateTime.now();
        List<WaitingRoom> toEnd = waitingRoomRepository.findWaitingRoomsToEnd(now);

        for (WaitingRoom room : toEnd) {
            try {
                room.setStatus(WaitingRoomStatus.ENDED);
                waitingRoomRepository.save(room);

                redisService.clearWaitingRoomData(room.getEvent().getId());

                log.info("Waiting room {} ended for event {}",
                        room.getId(), room.getEvent().getId());
            } catch (Exception e) {
                log.error("Error ending waiting room {}", room.getId(), e);
            }
        }
    }

    /**
     * Job 6: Sync Stats
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void syncStats() {
        List<WaitingRoom> activeRooms = waitingRoomRepository.findActiveWaitingRooms();

        for (WaitingRoom room : activeRooms) {
            try {
                Long eventId = room.getEvent().getId();

                int totalInQueue;
                if (room.getStatus() == WaitingRoomStatus.PRE_QUEUE) {
                    totalInQueue = (int) redisService.getPreQueueSize(eventId);
                } else {
                    totalInQueue = (int) redisService.getQueueSize(eventId);
                }

                if (totalInQueue != room.getTotalQueueEntries()) {
                    room.setTotalQueueEntries(totalInQueue);
                    waitingRoomRepository.save(room);
                }
            } catch (Exception e) {
                log.error("Error syncing stats for waiting room {}", room.getId(), e);
            }
        }
    }
}