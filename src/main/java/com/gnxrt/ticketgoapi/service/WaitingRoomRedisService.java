package com.gnxrt.ticketgoapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingRoomRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String PRE_QUEUE_KEY = "waiting_room:pre_queue:";
    private static final String QUEUE_KEY = "waiting_room:queue:";
    private static final String SESSION_KEY = "waiting_room:session:";
    // Active shoppers (READY/SHOPPING): ZSet member=userId, score=epoch millis của expiresAt.
    // Tự dọn entry hết hạn khi đếm → không drift, không kẹt capacity (thay cho counter số cũ).
    private static final String ACTIVE_ZSET_KEY = "waiting_room:active_z:";
    private static final String USER_TOKEN_KEY = "waiting_room:user_token:";
    private static final String TOKEN_USER_KEY = "waiting_room:token_user:";

    private static final long SESSION_TTL_HOURS = 24;
    private static final long QUEUE_TTL_HOURS = 48;


    /**
     * Thêm user vào pre-queue
     */
    public boolean addToPreQueue(Long eventId, Long userId, String visitorToken) {
        String preQueueKey = PRE_QUEUE_KEY + eventId;
        String userTokenKey = USER_TOKEN_KEY + eventId + ":" + userId;
        String tokenUserKey = TOKEN_USER_KEY + eventId + ":" + visitorToken;

        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(preQueueKey, userId.toString()))) {
            log.debug("User {} already in pre-queue for event {}", userId, eventId);
            return false;
        }

        redisTemplate.opsForSet().add(preQueueKey, userId.toString());
        redisTemplate.expire(preQueueKey, QUEUE_TTL_HOURS, TimeUnit.HOURS);

        redisTemplate.opsForValue().set(userTokenKey, visitorToken, SESSION_TTL_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(tokenUserKey, userId.toString(), SESSION_TTL_HOURS, TimeUnit.HOURS);

        saveSession(eventId, userId, Map.of(
                "visitorToken", visitorToken,
                "status", "WAITING",
                "joinedAt", LocalDateTime.now().toString()
        ));

        log.info("User {} joined pre-queue for event {}", userId, eventId);
        return true;
    }

    /**
     * Đếm số người trong pre-queue
     */
    public long getPreQueueSize(Long eventId) {
        String preQueueKey = PRE_QUEUE_KEY + eventId;
        Long size = redisTemplate.opsForSet().size(preQueueKey);
        return size != null ? size : 0;
    }

    /**
     * Lấy tất cả userIds trong pre-queue
     */
    public Set<String> getPreQueueMembers(Long eventId) {
        String preQueueKey = PRE_QUEUE_KEY + eventId;
        Set<Object> members = redisTemplate.opsForSet().members(preQueueKey);
        if (members == null) return Collections.emptySet();

        Set<String> result = new HashSet<>();
        for (Object member : members) {
            result.add(member.toString());
        }
        return result;
    }

    /**
     * Xóa user khỏi pre-queue
     */
    public boolean removeFromPreQueue(Long eventId, Long userId) {
        String preQueueKey = PRE_QUEUE_KEY + eventId;
        Long removed = redisTemplate.opsForSet().remove(preQueueKey, userId.toString());
        return removed != null && removed > 0;
    }

    /**
     * Shuffle pre-queue và chuyển sang main queue.
     * Score = vị trí ngẫu nhiên (1..N). User vào sau (late arrival) dùng score = timestamp
     * nên luôn xếp sau nhóm shuffle. Thứ tự phục vụ luôn theo ZRANK (score thấp = trước).
     */
    public List<Long> shuffleAndCreateQueue(Long eventId) {
        String preQueueKey = PRE_QUEUE_KEY + eventId;
        String queueKey = QUEUE_KEY + eventId;

        Set<String> members = getPreQueueMembers(eventId);
        if (members.isEmpty()) {
            log.warn("No users in pre-queue for event {} to shuffle", eventId);
            return Collections.emptyList();
        }

        List<Long> userIds = new ArrayList<>();
        for (String member : members) {
            userIds.add(Long.parseLong(member));
        }
        Collections.shuffle(userIds, new Random());

        ZSetOperations<String, Object> zSetOps = redisTemplate.opsForZSet();
        for (int i = 0; i < userIds.size(); i++) {
            Long userId = userIds.get(i);
            int position = i + 1;
            zSetOps.add(queueKey, userId.toString(), position);

            updateSession(eventId, userId, Map.of(
                    "queuePosition", String.valueOf(position),
                    "shuffledAt", LocalDateTime.now().toString()
            ));
        }
        redisTemplate.expire(queueKey, QUEUE_TTL_HOURS, TimeUnit.HOURS);

        // Reset active shoppers cho phiên bán mới.
        redisTemplate.delete(ACTIVE_ZSET_KEY + eventId);
        redisTemplate.delete(preQueueKey);

        log.info("Shuffled {} users for event {}", userIds.size(), eventId);
        return userIds;
    }

    /**
     * Vị trí hiện tại của user trong queue (1-based) = ZRANK + 1.
     * Giảm dần khi người phía trước được phục vụ → UX "bạn đang tiến lên".
     */
    public Integer getQueuePosition(Long eventId, Long userId) {
        String queueKey = QUEUE_KEY + eventId;
        Long rank = redisTemplate.opsForZSet().rank(queueKey, userId.toString());
        return rank != null ? rank.intValue() + 1 : null;
    }

    /**
     * Đếm số người trong queue
     */
    public long getQueueSize(Long eventId) {
        String queueKey = QUEUE_KEY + eventId;
        Long size = redisTemplate.opsForZSet().size(queueKey);
        return size != null ? size : 0;
    }

    /**
     * Lấy số người đang shopping (READY/SHOPPING). Tự dọn session hết hạn trước khi đếm
     * → không bao giờ drift hay kẹt capacity.
     */
    public int getActiveShoppersCount(Long eventId) {
        String activeKey = ACTIVE_ZSET_KEY + eventId;
        long now = System.currentTimeMillis();
        redisTemplate.opsForZSet().removeRangeByScore(activeKey, 0, now);
        Long count = redisTemplate.opsForZSet().zCard(activeKey);
        return count != null ? count.intValue() : 0;
    }

    private void addActiveShopper(Long eventId, Long userId, LocalDateTime expiresAt) {
        long expiryMillis = expiresAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        redisTemplate.opsForZSet().add(ACTIVE_ZSET_KEY + eventId, userId.toString(), expiryMillis);
    }

    private void removeActiveShopper(Long eventId, Long userId) {
        redisTemplate.opsForZSet().remove(ACTIVE_ZSET_KEY + eventId, userId.toString());
    }

    /**
     * Atomic pop N người đầu hàng (score thấp nhất) khỏi queue ZSet để admit.
     * ZPOPMIN là atomic → an toàn khi nhiều pod chạy drain (không cấp vượt quota,
     * không cần serving counter / self-heal).
     */
    public List<Long> popNextUsersToAdmit(Long eventId, int count) {
        if (count <= 0) return Collections.emptyList();
        String queueKey = QUEUE_KEY + eventId;
        Set<ZSetOperations.TypedTuple<Object>> popped =
                redisTemplate.opsForZSet().popMin(queueKey, count);
        if (popped == null || popped.isEmpty()) return Collections.emptyList();

        List<Long> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<Object> tuple : popped) {
            if (tuple.getValue() != null) {
                result.add(Long.parseLong(tuple.getValue().toString()));
            }
        }
        return result;
    }

    /**
     * Trả user về đầu hàng chờ (khi admit thất bại) để được retry trước ở vòng drain kế.
     */
    public void requeueAtFront(Long eventId, Long userId) {
        String queueKey = QUEUE_KEY + eventId;
        Set<ZSetOperations.TypedTuple<Object>> head =
                redisTemplate.opsForZSet().rangeWithScores(queueKey, 0, 0);
        double score = 0;
        if (head != null && !head.isEmpty()) {
            Double min = head.iterator().next().getScore();
            score = (min != null ? min : 0) - 1;
        }
        redisTemplate.opsForZSet().add(queueKey, userId.toString(), score);
        log.warn("Requeued user {} at front of event {} after admit failure", userId, eventId);
    }

    /**
     * Số người đang ở phía trước trong queue = ZRANK (0-based).
     */
    public int getPeopleAhead(Long eventId, Long userId) {
        String queueKey = QUEUE_KEY + eventId;
        Long rank = redisTemplate.opsForZSet().rank(queueKey, userId.toString());
        return rank != null ? rank.intValue() : 0;
    }

    /**
     * Thêm user vào cuối queue (late arrival). Score = timestamp → atomic (1 ZADD),
     * không race giữa các pod, luôn xếp sau nhóm shuffle (score 1..N). Trả về vị trí hiện tại.
     */
    public int addToQueueEnd(Long eventId, Long userId, String visitorToken) {
        String queueKey = QUEUE_KEY + eventId;

        redisTemplate.opsForZSet().add(queueKey, userId.toString(), System.currentTimeMillis());
        redisTemplate.expire(queueKey, QUEUE_TTL_HOURS, TimeUnit.HOURS);

        String userTokenKey = USER_TOKEN_KEY + eventId + ":" + userId;
        String tokenUserKey = TOKEN_USER_KEY + eventId + ":" + visitorToken;
        redisTemplate.opsForValue().set(userTokenKey, visitorToken, SESSION_TTL_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(tokenUserKey, userId.toString(), SESSION_TTL_HOURS, TimeUnit.HOURS);

        Long rank = redisTemplate.opsForZSet().rank(queueKey, userId.toString());
        int position = rank != null ? rank.intValue() + 1 : 1;

        saveSession(eventId, userId, Map.of(
                "visitorToken", visitorToken,
                "status", "WAITING",
                "joinedAt", LocalDateTime.now().toString(),
                "queuePosition", String.valueOf(position)
        ));

        log.info("User {} added to queue end at position {} for event {}", userId, position, eventId);
        return position;
    }

    /**
     * Thêm user vào queue tại vị trí cụ thể (dùng khi reconnect — khôi phục vị trí cũ).
     */
    public void addToQueueAtPosition(Long eventId, Long userId, String visitorToken, int position) {
        String queueKey = QUEUE_KEY + eventId;
        String userTokenKey = USER_TOKEN_KEY + eventId + ":" + userId;
        String tokenUserKey = TOKEN_USER_KEY + eventId + ":" + visitorToken;

        redisTemplate.opsForZSet().add(queueKey, userId.toString(), position);

        redisTemplate.opsForValue().set(userTokenKey, visitorToken, SESSION_TTL_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(tokenUserKey, userId.toString(), SESSION_TTL_HOURS, TimeUnit.HOURS);

        saveSession(eventId, userId, Map.of(
                "visitorToken", visitorToken,
                "status", "WAITING",
                "joinedAt", LocalDateTime.now().toString(),
                "queuePosition", String.valueOf(position)
        ));

        log.info("User {} reconnected to queue at position {} for event {}", userId, position, eventId);
    }

    /**
     * Lưu session info
     */
    public void saveSession(Long eventId, Long userId, Map<String, String> data) {
        String sessionKey = SESSION_KEY + eventId + ":" + userId;
        redisTemplate.opsForHash().putAll(sessionKey, data);
        redisTemplate.expire(sessionKey, SESSION_TTL_HOURS, TimeUnit.HOURS);
    }

    /**
     * Update session info
     */
    public void updateSession(Long eventId, Long userId, Map<String, String> updates) {
        String sessionKey = SESSION_KEY + eventId + ":" + userId;
        redisTemplate.opsForHash().putAll(sessionKey, updates);
    }

    /**
     * Lấy session info
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getSession(Long eventId, Long userId) {
        String sessionKey = SESSION_KEY + eventId + ":" + userId;
        Map<Object, Object> data = redisTemplate.opsForHash().entries(sessionKey);

        Map<String, String> result = new HashMap<>();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            result.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return result;
    }

    /**
     * Lấy session status
     */
    public String getSessionStatus(Long eventId, Long userId) {
        String sessionKey = SESSION_KEY + eventId + ":" + userId;
        Object status = redisTemplate.opsForHash().get(sessionKey, "status");
        return status != null ? status.toString() : null;
    }

    /**
     * Set user status to READY (đến lượt). Remove khỏi queue ZSET để drain không re-admit.
     * Thêm vào active ZSet (score = hạn phiên) — READY user đã "giữ" 1 slot capacity.
     */
    public void setUserReady(Long eventId, Long userId, String accessToken, int sessionTimeoutMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(sessionTimeoutMinutes);

        updateSession(eventId, userId, Map.of(
                "status", "READY",
                "accessToken", accessToken,
                "notifiedAt", now.toString(),
                "expiresAt", expiresAt.toString()
        ));

        String queueKey = QUEUE_KEY + eventId;
        redisTemplate.opsForZSet().remove(queueKey, userId.toString());

        addActiveShopper(eventId, userId, expiresAt);

        log.debug("User {} is now READY for event {}", userId, eventId);
    }

    /**
     * Set user status to SHOPPING (đã vào protected zone). Gia hạn slot active.
     */
    public void setUserShopping(Long eventId, Long userId, int sessionTimeoutMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(sessionTimeoutMinutes);

        updateSession(eventId, userId, Map.of(
                "status", "SHOPPING",
                "enteredAt", now.toString(),
                "expiresAt", expiresAt.toString()
        ));

        addActiveShopper(eventId, userId, expiresAt);

        log.debug("User {} is now SHOPPING for event {}", userId, eventId);
    }

    /**
     * Set user status to COMPLETED
     */
    public void setUserCompleted(Long eventId, Long userId) {
        updateSession(eventId, userId, Map.of(
                "status", "COMPLETED",
                "completedAt", LocalDateTime.now().toString()
        ));

        removeActiveShopper(eventId, userId);

        String queueKey = QUEUE_KEY + eventId;
        redisTemplate.opsForZSet().remove(queueKey, userId.toString());

        log.debug("User {} COMPLETED shopping for event {}", userId, eventId);
    }

    /**
     * Set user status to EXPIRED
     */
    public void setUserExpired(Long eventId, Long userId) {
        updateSession(eventId, userId, Map.of(
                "status", "EXPIRED",
                "completedAt", LocalDateTime.now().toString()
        ));

        removeActiveShopper(eventId, userId);

        String queueKey = QUEUE_KEY + eventId;
        redisTemplate.opsForZSet().remove(queueKey, userId.toString());

        log.debug("User {} session EXPIRED for event {}", userId, eventId);
    }

    /**
     * Lấy userId từ visitor token
     */
    public Long getUserIdByVisitorToken(Long eventId, String visitorToken) {
        String tokenUserKey = TOKEN_USER_KEY + eventId + ":" + visitorToken;
        Object userId = redisTemplate.opsForValue().get(tokenUserKey);
        return userId != null ? Long.parseLong(userId.toString()) : null;
    }

    /**
     * Lấy visitor token của user
     */
    public String getVisitorToken(Long eventId, Long userId) {
        String userTokenKey = USER_TOKEN_KEY + eventId + ":" + userId;
        Object token = redisTemplate.opsForValue().get(userTokenKey);
        return token != null ? token.toString() : null;
    }

    /**
     * Kiểm tra user có đang ở trong waiting room flow không.
     * True nếu: đang chờ trong pre-queue/main queue, HOẶC đã được admit nhưng
     * chưa hoàn tất (READY/SHOPPING) — sau khi admit user bị remove khỏi ZSET
     * nhưng session vẫn active cho tới khi COMPLETED/EXPIRED.
     */
    public boolean isUserInQueue(Long eventId, Long userId) {
        String preQueueKey = PRE_QUEUE_KEY + eventId;
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(preQueueKey, userId.toString()))) {
            return true;
        }

        String queueKey = QUEUE_KEY + eventId;
        Double score = redisTemplate.opsForZSet().score(queueKey, userId.toString());
        if (score != null) {
            return true;
        }

        String status = getSessionStatus(eventId, userId);
        return "WAITING".equals(status) || "READY".equals(status) || "SHOPPING".equals(status);
    }

    /**
     * Xóa tất cả data của waiting room
     */
    public void clearWaitingRoomData(Long eventId) {
        String preQueueKey = PRE_QUEUE_KEY + eventId;
        String queueKey = QUEUE_KEY + eventId;
        String activeKey = ACTIVE_ZSET_KEY + eventId;

        redisTemplate.delete(List.of(preQueueKey, queueKey, activeKey));

        log.info("Cleared all Redis data for waiting room event {}", eventId);
    }

    /**
     * Lấy danh sách users cần check expired session
     */
    public List<Map.Entry<Long, LocalDateTime>> getSessionsToCheck(Long eventId) {
        return Collections.emptyList();
    }

    /**
     * Debug: dump full ZSET contents (userId → score) cho 1 event
     */
    public String inspectQueue(Long eventId) {
        String queueKey = QUEUE_KEY + eventId;
        String preQueueKey = PRE_QUEUE_KEY + eventId;
        Set<ZSetOperations.TypedTuple<Object>> ranged =
                redisTemplate.opsForZSet().rangeWithScores(queueKey, 0, -1);
        Set<Object> preMembers = redisTemplate.opsForSet().members(preQueueKey);

        StringBuilder sb = new StringBuilder();
        sb.append("preQueue=").append(preMembers == null ? "null" : preMembers);
        sb.append(", queue=[");
        if (ranged != null) {
            boolean first = true;
            for (ZSetOperations.TypedTuple<Object> t : ranged) {
                if (!first) sb.append(",");
                sb.append(t.getValue()).append("@").append(t.getScore());
                first = false;
            }
        }
        sb.append("]");
        sb.append(", active=").append(getActiveShoppersCount(eventId));
        return sb.toString();
    }
}
