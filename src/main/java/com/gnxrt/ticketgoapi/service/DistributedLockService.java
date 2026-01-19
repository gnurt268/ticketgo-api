package com.gnxrt.ticketgoapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedLockService {

    private final RedissonClient redissonClient;

    private static final String SEAT_LOCK_PREFIX = "lock:seat:";
    private static final String ORDER_LOCK_PREFIX = "lock:order:";
    private static final String ZONE_LOCK_PREFIX = "lock:zone:";

    private static final long DEFAULT_WAIT_TIME = 5;
    private static final long DEFAULT_LEASE_TIME = 30;

    public boolean tryLockSeat(Long seatId, long waitTime, long leaseTime, TimeUnit unit) {
        String lockKey = SEAT_LOCK_PREFIX + seatId;
        return tryLock(lockKey, waitTime, leaseTime, unit);
    }

    public boolean tryLockSeats(List<Long> seatIds, long waitTime, long leaseTime, TimeUnit unit) {
        if (seatIds == null || seatIds.isEmpty()) {
            return true;
        }

        List<Long> sortedIds = new ArrayList<>(seatIds);
        sortedIds.sort(Long::compareTo);

        List<RLock> locks = new ArrayList<>();
        for (Long seatId : sortedIds) {
            String lockKey = SEAT_LOCK_PREFIX + seatId;
            locks.add(redissonClient.getLock(lockKey));
        }

        RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));

        try {
            boolean acquired = multiLock.tryLock(waitTime, leaseTime, unit);
            if (acquired) {
                log.debug("Acquired locks for {} seats", seatIds.size());
            } else {
                log.warn("Failed to acquire locks for seats: {}", seatIds);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring seat locks", e);
            return false;
        }
    }

    public void unlockSeats(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return;
        }

        List<Long> sortedIds = new ArrayList<>(seatIds);
        sortedIds.sort(Long::compareTo);

        List<RLock> locks = new ArrayList<>();
        for (Long seatId : sortedIds) {
            String lockKey = SEAT_LOCK_PREFIX + seatId;
            locks.add(redissonClient.getLock(lockKey));
        }

        RLock multiLock = redissonClient.getMultiLock(locks.toArray(new RLock[0]));

        try {
            if (multiLock.isHeldByCurrentThread()) {
                multiLock.unlock();
                log.debug("Released locks for {} seats", seatIds.size());
            }
        } catch (Exception e) {
            log.error("Error releasing seat locks", e);
        }
    }

    public boolean tryLockZone(Long zoneId, long waitTime, long leaseTime, TimeUnit unit) {
        String lockKey = ZONE_LOCK_PREFIX + zoneId;
        return tryLock(lockKey, waitTime, leaseTime, unit);
    }

    public void unlockZone(Long zoneId) {
        String lockKey = ZONE_LOCK_PREFIX + zoneId;
        unlock(lockKey);
    }

    public boolean tryLockOrder(String orderCode, long waitTime, long leaseTime, TimeUnit unit) {
        String lockKey = ORDER_LOCK_PREFIX + orderCode;
        return tryLock(lockKey, waitTime, leaseTime, unit);
    }

    public void unlockOrder(String orderCode) {
        String lockKey = ORDER_LOCK_PREFIX + orderCode;
        unlock(lockKey);
    }

    public <T> T executeWithSeatLock(List<Long> seatIds, Supplier<T> operation) {
        boolean locked = false;
        try {
            locked = tryLockSeats(seatIds, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new SeatLockException("Không thể khóa ghế. Vui lòng thử lại.");
            }
            return operation.get();
        } finally {
            if (locked) {
                unlockSeats(seatIds);
            }
        }
    }

    public <T> T executeWithZoneLock(Long zoneId, Supplier<T> operation) {
        boolean locked = false;
        try {
            locked = tryLockZone(zoneId, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
            if (!locked) {
                throw new SeatLockException("Không thể khóa khu vực. Vui lòng thử lại.");
            }
            return operation.get();
        } finally {
            if (locked) {
                unlockZone(zoneId);
            }
        }
    }

    public <T> T executeWithSeatAndZoneLock(List<Long> seatIds, Long zoneId, Supplier<T> operation) {
        boolean seatLocked = false;
        boolean zoneLocked = false;
        try {
            zoneLocked = tryLockZone(zoneId, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
            if (!zoneLocked) {
                throw new SeatLockException("Khu vực đang bận. Vui lòng thử lại.");
            }

            seatLocked = tryLockSeats(seatIds, DEFAULT_WAIT_TIME, DEFAULT_LEASE_TIME, TimeUnit.SECONDS);
            if (!seatLocked) {
                throw new SeatLockException("Ghế đang được người khác đặt. Vui lòng thử lại.");
            }

            return operation.get();
        } finally {
            if (seatLocked) {
                unlockSeats(seatIds);
            }
            if (zoneLocked) {
                unlockZone(zoneId);
            }
        }
    }

    private boolean tryLock(String lockKey, long waitTime, long leaseTime, TimeUnit unit) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            boolean acquired = lock.tryLock(waitTime, leaseTime, unit);
            if (acquired) {
                log.debug("Acquired lock: {}", lockKey);
            } else {
                log.warn("Failed to acquire lock: {}", lockKey);
            }
            return acquired;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while acquiring lock: {}", lockKey, e);
            return false;
        }
    }

    private void unlock(String lockKey) {
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
                log.debug("Released lock: {}", lockKey);
            }
        } catch (Exception e) {
            log.error("Error releasing lock: {}", lockKey, e);
        }
    }

    public static class SeatLockException extends RuntimeException {
        public SeatLockException(String message) {
            super(message);
        }
    }
}