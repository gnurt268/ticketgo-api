package com.gnxrt.ticketgoapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    private static final String SERVING_COUNTER_KEY = "waiting_room:serving:";
    private static final String ACTIVE_COUNTER_KEY = "waiting_room:active:";
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
     * Shuffle pre-queue và chuyển sang main queue
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

        String servingKey = SERVING_COUNTER_KEY + eventId;
        redisTemplate.opsForValue().set(servingKey, "0", QUEUE_TTL_HOURS, TimeUnit.HOURS);

        String activeKey = ACTIVE_COUNTER_KEY + eventId;
        redisTemplate.opsForValue().set(activeKey, "0", QUEUE_TTL_HOURS, TimeUnit.HOURS);

        redisTemplate.delete(preQueueKey);

        log.info("Shuffled {} users for event {}", userIds.size(), eventId);
        return userIds;
    }

    /**
     * Lấy vị trí của user trong queue
     */
    public Integer getQueuePosition(Long eventId, Long userId) {
        String queueKey = QUEUE_KEY + eventId;
        Double score = redisTemplate.opsForZSet().score(queueKey, userId.toString());
        return score != null ? score.intValue() : null;
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
     * Lấy serving counter hiện tại (position đang được serve)
     */
    public int getServingPosition(Long eventId) {
        String servingKey = SERVING_COUNTER_KEY + eventId;
        Object value = redisTemplate.opsForValue().get(servingKey);
        return value != null ? Integer.parseInt(value.toString()) : 0;
    }

    /**
     * Tăng serving counter và trả về new value
     */
    public int incrementServingPosition(Long eventId, int increment) {
        String servingKey = SERVING_COUNTER_KEY + eventId;
        Long newValue = redisTemplate.opsForValue().increment(servingKey, increment);
        return newValue != null ? newValue.intValue() : 0;
    }

    /**
     * Lấy số người đang shopping
     */
    public int getActiveShoppersCount(Long eventId) {
        String activeKey = ACTIVE_COUNTER_KEY + eventId;
        Object value = redisTemplate.opsForValue().get(activeKey);
        return value != null ? Integer.parseInt(value.toString()) : 0;
    }

    /**
     * Tăng/giảm active shoppers counter
     */
    public void updateActiveShoppersCount(Long eventId, int delta) {
        String activeKey = ACTIVE_COUNTER_KEY + eventId;
        redisTemplate.opsForValue().increment(activeKey, delta);
    }

    /**
     * Lấy danh sách users tiếp theo cần được cho vào (để admit)
     * @param eventId ID của event
     * @param count Số lượng users cần lấy
     * @return Danh sách userIds
     */
    public List<Long> getNextUsersToAdmit(Long eventId, int count) {
        String queueKey = QUEUE_KEY + eventId;
        int currentServing = getServingPosition(eventId);

        Set<Object> users = redisTemplate.opsForZSet().rangeByScore(
                queueKey,
                currentServing + 1,
                currentServing + count
        );

        if (users == null) return Collections.emptyList();

        List<Long> result = new ArrayList<>();
        for (Object user : users) {
            result.add(Long.parseLong(user.toString()));
        }
        return result;
    }

    /**
     * Lấy số người đang ở phía trước trong queue
     */
    public int getPeopleAhead(Long eventId, Long userId) {
        Integer position = getQueuePosition(eventId, userId);
        if (position == null) return 0;

        int servingPosition = getServingPosition(eventId);
        return Math.max(0, position - servingPosition - 1);
    }

    /**
     * Thêm user vào cuối queue (late arrival)
     */
    public int addToQueueEnd(Long eventId, Long userId, String visitorToken) {
        String queueKey = QUEUE_KEY + eventId;

        Set<Object> lastUser = redisTemplate.opsForZSet().reverseRange(queueKey, 0, 0);
        int maxPosition = 0;
        if (lastUser != null && !lastUser.isEmpty()) {
            String lastUserId = lastUser.iterator().next().toString();
            Double score = redisTemplate.opsForZSet().score(queueKey, lastUserId);
            maxPosition = score != null ? score.intValue() : 0;
        }

        int newPosition = maxPosition + 1;
        redisTemplate.opsForZSet().add(queueKey, userId.toString(), newPosition);

        String userTokenKey = USER_TOKEN_KEY + eventId + ":" + userId;
        String tokenUserKey = TOKEN_USER_KEY + eventId + ":" + visitorToken;
        redisTemplate.opsForValue().set(userTokenKey, visitorToken, SESSION_TTL_HOURS, TimeUnit.HOURS);
        redisTemplate.opsForValue().set(tokenUserKey, userId.toString(), SESSION_TTL_HOURS, TimeUnit.HOURS);

        saveSession(eventId, userId, Map.of(
                "visitorToken", visitorToken,
                "status", "WAITING",
                "joinedAt", LocalDateTime.now().toString(),
                "queuePosition", String.valueOf(newPosition)
        ));

        log.info("User {} added to queue end at position {} for event {}", userId, newPosition, eventId);
        return newPosition;
    }

    /**
     * Thêm user vào queue tại vị trí cụ thể
     * @param eventId ID của event
     * @param userId ID của user
     * @param visitorToken Visitor token
     * @param position Vị trí muốn chèn vào
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
     * Set user status to READY (đến lượt)
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

        log.debug("User {} is now READY for event {}", userId, eventId);
    }

    /**
     * Set user status to SHOPPING (đã vào protected zone)
     */
    public void setUserShopping(Long eventId, Long userId, int sessionTimeoutMinutes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMinutes(sessionTimeoutMinutes);

        updateSession(eventId, userId, Map.of(
                "status", "SHOPPING",
                "enteredAt", now.toString(),
                "expiresAt", expiresAt.toString()
        ));

        updateActiveShoppersCount(eventId, 1);

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

        updateActiveShoppersCount(eventId, -1);

        String queueKey = QUEUE_KEY + eventId;
        redisTemplate.opsForZSet().remove(queueKey, userId.toString());

        log.debug("User {} COMPLETED shopping for event {}", userId, eventId);
    }

    /**
     * Set user status to EXPIRED
     */
    public void setUserExpired(Long eventId, Long userId) {
        String currentStatus = getSessionStatus(eventId, userId);

        updateSession(eventId, userId, Map.of(
                "status", "EXPIRED",
                "completedAt", LocalDateTime.now().toString()
        ));

        if ("SHOPPING".equals(currentStatus)) {
            updateActiveShoppersCount(eventId, -1);
        }

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
     * Kiểm tra user có trong queue không (pre-queue hoặc main queue)
     */
    public boolean isUserInQueue(Long eventId, Long userId) {
        String preQueueKey = PRE_QUEUE_KEY + eventId;
        if (Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(preQueueKey, userId.toString()))) {
            return true;
        }

        String queueKey = QUEUE_KEY + eventId;
        Double score = redisTemplate.opsForZSet().score(queueKey, userId.toString());
        return score != null;
    }

    /**
     * Xóa tất cả data của waiting room
     */
    public void clearWaitingRoomData(Long eventId) {
        String preQueueKey = PRE_QUEUE_KEY + eventId;
        String queueKey = QUEUE_KEY + eventId;
        String servingKey = SERVING_COUNTER_KEY + eventId;
        String activeKey = ACTIVE_COUNTER_KEY + eventId;

        redisTemplate.delete(List.of(preQueueKey, queueKey, servingKey, activeKey));

        log.info("Cleared all Redis data for waiting room event {}", eventId);
    }

    /**
     * Lấy danh sách users cần check expired session
     */
    public List<Map.Entry<Long, LocalDateTime>> getSessionsToCheck(Long eventId) {
        return Collections.emptyList();
    }
}