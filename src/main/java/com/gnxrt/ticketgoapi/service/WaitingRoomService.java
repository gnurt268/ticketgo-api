package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.waitingroom.CreateWaitingRoomRequest;
import com.gnxrt.ticketgoapi.dto.request.waitingroom.JoinQueueRequest;
import com.gnxrt.ticketgoapi.dto.response.waitingroom.JoinQueueResponse;
import com.gnxrt.ticketgoapi.dto.response.waitingroom.QueueStatusResponse;
import com.gnxrt.ticketgoapi.dto.response.waitingroom.WaitingRoomResponse;
import com.gnxrt.ticketgoapi.enums.QueueEntryStatus;
import com.gnxrt.ticketgoapi.enums.WaitingRoomStatus;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.QueueEntry;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.model.WaitingRoom;
import com.gnxrt.ticketgoapi.repository.EventRepository;
import com.gnxrt.ticketgoapi.repository.QueueEntryRepository;
import com.gnxrt.ticketgoapi.repository.WaitingRoomRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WaitingRoomService {

    private final WaitingRoomRepository waitingRoomRepository;
    private final QueueEntryRepository queueEntryRepository;
    private final EventRepository eventRepository;
    private final WaitingRoomRedisService redisService;
    private final BotDetectionService botDetectionService;

    /**
     * Tạo waiting room cho event
     */
    @Transactional
    public WaitingRoomResponse createWaitingRoom(CreateWaitingRoomRequest request) {
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện"));

        if (waitingRoomRepository.existsByEventId(request.getEventId())) {
            throw new BadRequestException("Sự kiện đã có phòng chờ");
        }

        if (request.getSaleStart().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Thời gian mở bán phải trong tương lai");
        }

        LocalDateTime preQueueStart = request.getPreQueueStart();
        if (preQueueStart == null) {
            preQueueStart = request.getSaleStart().minusMinutes(30);
        }

        if (preQueueStart.isAfter(request.getSaleStart())) {
            throw new BadRequestException("Thời gian mở phòng chờ phải trước thời gian mở bán");
        }

        WaitingRoom waitingRoom = WaitingRoom.builder()
                .event(event)
                .preQueueStart(preQueueStart)
                .saleStart(request.getSaleStart())
                .saleEnd(request.getSaleEnd())
                .throughputPerMinute(request.getThroughputPerMinute())
                .maxConcurrentUsers(request.getMaxConcurrentUsers())
                .sessionTimeoutMinutes(request.getSessionTimeoutMinutes())
                .status(WaitingRoomStatus.SCHEDULED)
                .isEnabled(true)
                .build();

        waitingRoom = waitingRoomRepository.save(waitingRoom);
        log.info("Created waiting room {} for event {}", waitingRoom.getId(), event.getId());

        return toResponse(waitingRoom);
    }

    /**
     * Lấy thông tin waiting room theo event ID
     */
    public WaitingRoomResponse getWaitingRoomByEventId(Long eventId) {
        WaitingRoom waitingRoom = waitingRoomRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chờ cho sự kiện này"));
        return toResponse(waitingRoom);
    }

    /**
     * Lấy thông tin waiting room theo event slug
     */
    public WaitingRoomResponse getWaitingRoomBySlug(String slug) {
        WaitingRoom waitingRoom = waitingRoomRepository.findByEventSlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chờ cho sự kiện này"));
        return toResponse(waitingRoom);
    }

    /**
     * Bật/tắt waiting room
     */
    @Transactional
    public WaitingRoomResponse toggleWaitingRoom(Long waitingRoomId, boolean enabled) {
        WaitingRoom waitingRoom = waitingRoomRepository.findById(waitingRoomId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chờ"));

        waitingRoom.setIsEnabled(enabled);
        waitingRoom = waitingRoomRepository.save(waitingRoom);

        log.info("Waiting room {} {}", waitingRoomId, enabled ? "enabled" : "disabled");
        return toResponse(waitingRoom);
    }
    
    /**
     * User join vào waiting room
     */
    @Transactional
    public JoinQueueResponse joinQueue(Long eventId, User user, HttpServletRequest request,
                                       JoinQueueRequest joinRequest) {
        WaitingRoom waitingRoom = waitingRoomRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Sự kiện này không có phòng chờ"));

        if (!waitingRoom.getIsEnabled()) {
            return JoinQueueResponse.failure("Phòng chờ đã bị tắt");
        }

        LocalDateTime now = LocalDateTime.now();

        switch (waitingRoom.getStatus()) {
            case SCHEDULED:
                if (now.isBefore(waitingRoom.getPreQueueStart())) {
                    long secondsUntil = Duration.between(now, waitingRoom.getPreQueueStart()).getSeconds();
                    return JoinQueueResponse.failure(
                            String.format("Phòng chờ sẽ mở sau %d phút", secondsUntil / 60));
                }
                waitingRoom.setStatus(WaitingRoomStatus.PRE_QUEUE);
                waitingRoom = waitingRoomRepository.save(waitingRoom);
                break;

            case PRE_QUEUE:
                break;

            case SELLING:
                return handleLateArrival(waitingRoom, user, request, joinRequest);

            case ENDED:
            case PAUSED:
                return JoinQueueResponse.failure("Phòng chờ đã kết thúc hoặc tạm dừng");
        }

        if (redisService.isUserInQueue(eventId, user.getId())) {
            String existingToken = redisService.getVisitorToken(eventId, user.getId());
            int totalInQueue = (int) redisService.getPreQueueSize(eventId);

            log.info("User {} reconnected to pre-queue for event {}", user.getId(), eventId);

            return JoinQueueResponse.success(
                    existingToken,
                    waitingRoom.getId(),
                    eventId,
                    waitingRoom.getEvent().getTitle(),
                    LocalDateTime.now(),
                    waitingRoom.getSaleStart(),
                    totalInQueue,
                    true
            );
        }

        if (joinRequest != null && joinRequest.getVisitorToken() != null) {
            JoinQueueResponse reconnectResponse = attemptReconnect(waitingRoom, user, joinRequest.getVisitorToken());
            if (reconnectResponse != null) {
                log.info("User {} reconnected with visitorToken for event {}", user.getId(), eventId);
                return reconnectResponse;
            }
        }

        String fingerprint = joinRequest != null ? joinRequest.getFingerprint() : null;

        long timeSinceOpen = waitingRoom.getPreQueueStart() != null
                ? Math.max(0, Duration.between(waitingRoom.getPreQueueStart(), now).getSeconds())
                : 0;

        boolean isBot = botDetectionService.isBot(
                fingerprint, request.getHeader("User-Agent"), getClientIp(request),
                true, timeSinceOpen, 0.0
        );

        if (isBot) {
            log.warn("Bot detected for fingerprint: {}, event: {}", fingerprint, eventId);
            throw new BadRequestException("Hành vi của bạn bị phát hiện là bất thường. Vui lòng thử lại sau.");
        }

        String visitorToken = generateVisitorToken();

        redisService.addToPreQueue(eventId, user.getId(), visitorToken);

        QueueEntry entry = QueueEntry.builder()
                .waitingRoom(waitingRoom)
                .user(user)
                .visitorToken(visitorToken)
                .joinedAt(now)
                .status(QueueEntryStatus.WAITING)
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .fingerprint(fingerprint)
                .build();
        queueEntryRepository.save(entry);

        waitingRoom.setTotalQueueEntries(waitingRoom.getTotalQueueEntries() + 1);
        waitingRoomRepository.save(waitingRoom);

        int totalInQueue = (int) redisService.getPreQueueSize(eventId);

        log.info("User {} joined pre-queue for event {}. Total in queue: {}",
                user.getId(), eventId, totalInQueue);

        return JoinQueueResponse.success(
                visitorToken,
                waitingRoom.getId(),
                eventId,
                waitingRoom.getEvent().getTitle(),
                now,
                waitingRoom.getSaleStart(),
                totalInQueue,
                false
        );
    }

    /**
     * Attempt to reconnect user với existing visitorToken
     */
    private JoinQueueResponse attemptReconnect(WaitingRoom waitingRoom, User user, String providedToken) {
        Long eventId = waitingRoom.getEvent().getId();

        return queueEntryRepository.findByVisitorTokenAndWaitingRoomId(providedToken, waitingRoom.getId())
                .filter(entry -> entry.getUser().getId().equals(user.getId()))
                .filter(entry -> entry.getStatus() == QueueEntryStatus.WAITING ||
                        entry.getStatus() == QueueEntryStatus.READY)
                .map(entry -> {
                    if (!redisService.isUserInQueue(eventId, user.getId())) {
                        if (waitingRoom.getStatus() == WaitingRoomStatus.PRE_QUEUE) {
                            redisService.addToPreQueue(eventId, user.getId(), providedToken);
                        } else {
                            Integer position = entry.getQueuePosition();
                            if (position != null) {
                                redisService.addToQueueAtPosition(eventId, user.getId(), providedToken, position);
                            } else {
                                redisService.addToQueueEnd(eventId, user.getId(), providedToken);
                            }
                        }
                    }

                    int totalInQueue = waitingRoom.getStatus() == WaitingRoomStatus.PRE_QUEUE
                            ? (int) redisService.getPreQueueSize(eventId)
                            : (int) redisService.getQueueSize(eventId);

                    return JoinQueueResponse.success(
                            providedToken,
                            waitingRoom.getId(),
                            eventId,
                            waitingRoom.getEvent().getTitle(),
                            entry.getJoinedAt(),
                            waitingRoom.getSaleStart(),
                            totalInQueue,
                            true // isReconnect
                    );
                })
                .orElse(null);
    }

    /**
     * Handle late arrival (sau khi đã shuffle)
     */
    private JoinQueueResponse handleLateArrival(WaitingRoom waitingRoom, User user,
                                                HttpServletRequest request, JoinQueueRequest joinRequest) {
        Long eventId = waitingRoom.getEvent().getId();

        if (redisService.isUserInQueue(eventId, user.getId())) {
            String existingToken = redisService.getVisitorToken(eventId, user.getId());
            Integer position = redisService.getQueuePosition(eventId, user.getId());
            int totalInQueue = (int) redisService.getQueueSize(eventId);

            return JoinQueueResponse.builder()
                    .success(true)
                    .message("Bạn đã có vị trí trong hàng chờ")
                    .visitorToken(existingToken)
                    .waitingRoomId(waitingRoom.getId())
                    .eventId(eventId)
                    .eventTitle(waitingRoom.getEvent().getTitle())
                    .totalInQueue(totalInQueue)
                    .isReconnect(true)
                    .build();
        }

        if (joinRequest != null && joinRequest.getVisitorToken() != null) {
            JoinQueueResponse reconnectResponse = attemptReconnect(waitingRoom, user, joinRequest.getVisitorToken());
            if (reconnectResponse != null) {
                return reconnectResponse;
            }
        }

        String fingerprint = joinRequest != null ? joinRequest.getFingerprint() : null;

        String visitorToken = generateVisitorToken();
        int position = redisService.addToQueueEnd(eventId, user.getId(), visitorToken);

        QueueEntry entry = QueueEntry.builder()
                .waitingRoom(waitingRoom)
                .user(user)
                .visitorToken(visitorToken)
                .joinedAt(LocalDateTime.now())
                .queuePosition(position)
                .status(QueueEntryStatus.WAITING)
                .ipAddress(getClientIp(request))
                .userAgent(request.getHeader("User-Agent"))
                .fingerprint(fingerprint)
                .build();
        queueEntryRepository.save(entry);

        waitingRoom.setTotalQueueEntries(waitingRoom.getTotalQueueEntries() + 1);
        waitingRoomRepository.save(waitingRoom);

        int totalInQueue = (int) redisService.getQueueSize(eventId);
        int peopleAhead = redisService.getPeopleAhead(eventId, user.getId());

        log.info("User {} late arrival, position {} in queue for event {}",
                user.getId(), position, eventId);

        return JoinQueueResponse.builder()
                .success(true)
                .message(String.format("Bạn đã vào hàng chờ ở vị trí #%d", position))
                .visitorToken(visitorToken)
                .waitingRoomId(waitingRoom.getId())
                .eventId(eventId)
                .eventTitle(waitingRoom.getEvent().getTitle())
                .joinedAt(LocalDateTime.now())
                .totalInQueue(totalInQueue)
                .isReconnect(false)
                .build();
    }

    /**
     * Lấy status hiện tại của user trong queue
     */
    public QueueStatusResponse getQueueStatus(Long eventId, User user) {
        WaitingRoom waitingRoom = waitingRoomRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chờ"));

        Long userId = user.getId();

        if (!redisService.isUserInQueue(eventId, userId)) {
            throw new BadRequestException("Bạn chưa vào phòng chờ");
        }

        Map<String, String> session = redisService.getSession(eventId, userId);
        String status = session.get("status");
        String visitorToken = session.get("visitorToken");

        switch (waitingRoom.getStatus()) {
            case PRE_QUEUE:
                int totalPreQueue = (int) redisService.getPreQueueSize(eventId);
                long secondsUntil = Duration.between(LocalDateTime.now(), waitingRoom.getSaleStart()).getSeconds();

                return QueueStatusResponse.preQueueStatus(
                        waitingRoom.getId(),
                        eventId,
                        waitingRoom.getEvent().getTitle(),
                        visitorToken,
                        LocalDateTime.parse(session.getOrDefault("joinedAt", LocalDateTime.now().toString())),
                        totalPreQueue,
                        Math.max(0, secondsUntil)
                );

            case SELLING:
                return getSellingPhaseStatus(waitingRoom, userId, session);

            case ENDED:
                return QueueStatusResponse.builder()
                        .waitingRoomId(waitingRoom.getId())
                        .eventId(eventId)
                        .eventTitle(waitingRoom.getEvent().getTitle())
                        .roomStatus(WaitingRoomStatus.ENDED)
                        .message("Phiên mua vé đã kết thúc")
                        .build();

            default:
                return QueueStatusResponse.builder()
                        .waitingRoomId(waitingRoom.getId())
                        .eventId(eventId)
                        .roomStatus(waitingRoom.getStatus())
                        .message("Phòng chờ chưa mở")
                        .build();
        }
    }

    /**
     * Get status during SELLING phase
     */
    private QueueStatusResponse getSellingPhaseStatus(WaitingRoom waitingRoom, Long userId, Map<String, String> session) {
        Long eventId = waitingRoom.getEvent().getId();
        String status = session.get("status");
        String visitorToken = session.get("visitorToken");

        switch (status) {
            case "WAITING":
                Integer position = redisService.getQueuePosition(eventId, userId);
                int peopleAhead = redisService.getPeopleAhead(eventId, userId);
                int totalInQueue = (int) redisService.getQueueSize(eventId);

                // Estimate wait time: peopleAhead / throughputPerMinute * 60
                int estimatedWait = (int) ((peopleAhead * 60.0) / waitingRoom.getThroughputPerMinute());

                return QueueStatusResponse.waitingInQueue(
                        waitingRoom.getId(),
                        eventId,
                        waitingRoom.getEvent().getTitle(),
                        visitorToken,
                        position != null ? position : 0,
                        peopleAhead,
                        totalInQueue,
                        estimatedWait
                );

            case "READY":
                String accessToken = session.get("accessToken");
                String actionUrl = "/events/" + waitingRoom.getEvent().getSlug() + "/book";

                return QueueStatusResponse.readyToEnter(
                        waitingRoom.getId(),
                        eventId,
                        waitingRoom.getEvent().getTitle(),
                        visitorToken,
                        accessToken,
                        actionUrl,
                        waitingRoom.getSessionTimeoutMinutes()
                );

            case "SHOPPING":
                String accessTokenShopping = session.get("accessToken");
                LocalDateTime expiresAt = LocalDateTime.parse(session.get("expiresAt"));

                if (LocalDateTime.now().isAfter(expiresAt)) {
                    redisService.setUserExpired(eventId, userId);
                    return QueueStatusResponse.builder()
                            .waitingRoomId(waitingRoom.getId())
                            .eventId(eventId)
                            .eventTitle(waitingRoom.getEvent().getTitle())
                            .status(QueueEntryStatus.EXPIRED)
                            .message("Phiên mua vé của bạn đã hết hạn")
                            .build();
                }

                return QueueStatusResponse.shopping(
                        waitingRoom.getId(),
                        eventId,
                        waitingRoom.getEvent().getTitle(),
                        visitorToken,
                        accessTokenShopping,
                        expiresAt
                );

            case "COMPLETED":
                return QueueStatusResponse.builder()
                        .waitingRoomId(waitingRoom.getId())
                        .eventId(eventId)
                        .eventTitle(waitingRoom.getEvent().getTitle())
                        .status(QueueEntryStatus.COMPLETED)
                        .message("Bạn đã hoàn tất mua vé")
                        .build();

            case "EXPIRED":
                return QueueStatusResponse.builder()
                        .waitingRoomId(waitingRoom.getId())
                        .eventId(eventId)
                        .eventTitle(waitingRoom.getEvent().getTitle())
                        .status(QueueEntryStatus.EXPIRED)
                        .message("Phiên mua vé của bạn đã hết hạn")
                        .build();

            default:
                return QueueStatusResponse.builder()
                        .waitingRoomId(waitingRoom.getId())
                        .eventId(eventId)
                        .message("Trạng thái không xác định")
                        .build();
        }
    }

    /**
     * User xác nhận vào protected zone (click "Vào mua vé")
     */
    @Transactional
    public QueueStatusResponse enterProtectedZone(Long eventId, User user, String accessToken) {
        WaitingRoom waitingRoom = waitingRoomRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chờ"));

        Long userId = user.getId();
        Map<String, String> session = redisService.getSession(eventId, userId);

        String storedToken = session.get("accessToken");
        if (storedToken == null || !storedToken.equals(accessToken)) {
            throw new BadRequestException("Access token không hợp lệ");
        }

        String status = session.get("status");
        if (!"READY".equals(status)) {
            throw new BadRequestException("Bạn không trong trạng thái sẵn sàng");
        }

        int activeSessions = redisService.getActiveShoppersCount(eventId);
        if (activeSessions >= waitingRoom.getMaxConcurrentUsers()) {
            throw new BadRequestException("Khu vực mua vé đang đầy, vui lòng chờ");
        }

        redisService.setUserShopping(eventId, userId, waitingRoom.getSessionTimeoutMinutes());

        queueEntryRepository.findByWaitingRoomIdAndUserId(waitingRoom.getId(), userId)
                .ifPresent(entry -> {
                    entry.setStatus(QueueEntryStatus.SHOPPING);
                    entry.setEnteredAt(LocalDateTime.now());
                    entry.setExpiresAt(LocalDateTime.now().plusMinutes(waitingRoom.getSessionTimeoutMinutes()));
                    queueEntryRepository.save(entry);
                });

        waitingRoom.setTotalAdmitted(waitingRoom.getTotalAdmitted() + 1);
        waitingRoomRepository.save(waitingRoom);

        log.info("User {} entered protected zone for event {}", userId, eventId);

        return getQueueStatus(eventId, user);
    }

    /**
     * User hoàn tất mua vé
     */
    @Transactional
    public void completeSession(Long eventId, Long userId) {
        WaitingRoom waitingRoom = waitingRoomRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chờ"));

        redisService.setUserCompleted(eventId, userId);

        queueEntryRepository.findByWaitingRoomIdAndUserId(waitingRoom.getId(), userId)
                .ifPresent(entry -> {
                    entry.setStatus(QueueEntryStatus.COMPLETED);
                    entry.setCompletedAt(LocalDateTime.now());
                    queueEntryRepository.save(entry);
                });

        waitingRoom.setTotalCompleted(waitingRoom.getTotalCompleted() + 1);
        waitingRoomRepository.save(waitingRoom);

        log.info("User {} completed purchase for event {}", userId, eventId);
    }

    /**
     * User rời khỏi queue
     */
    @Transactional
    public void leaveQueue(Long eventId, Long userId) {
        WaitingRoom waitingRoom = waitingRoomRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy phòng chờ"));

        if (!redisService.isUserInQueue(eventId, userId)) {
            throw new BadRequestException("Bạn không có trong hàng chờ");
        }

        String status = redisService.getSessionStatus(eventId, userId);

        if (waitingRoom.getStatus() == WaitingRoomStatus.PRE_QUEUE) {
            redisService.removeFromPreQueue(eventId, userId);
        } else {
            if ("SHOPPING".equals(status)) {
                redisService.updateActiveShoppersCount(eventId, -1);
            }
            redisService.setUserExpired(eventId, userId);
        }

        queueEntryRepository.findByWaitingRoomIdAndUserId(waitingRoom.getId(), userId)
                .ifPresent(entry -> {
                    entry.setStatus(QueueEntryStatus.LEFT);
                    entry.setCompletedAt(LocalDateTime.now());
                    queueEntryRepository.save(entry);
                });

        log.info("User {} left queue for event {}", userId, eventId);
    }

    private String generateVisitorToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private String generateAccessToken() {
        return "AT-" + UUID.randomUUID().toString().replace("-", "");
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private WaitingRoomResponse toResponse(WaitingRoom waitingRoom) {
        LocalDateTime now = LocalDateTime.now();
        Long eventId = waitingRoom.getEvent().getId();

        int currentlyWaiting;
        int currentlyShopping;

        if (waitingRoom.getStatus() == WaitingRoomStatus.PRE_QUEUE) {
            currentlyWaiting = (int) redisService.getPreQueueSize(eventId);
            currentlyShopping = 0;
        } else {
            currentlyWaiting = (int) redisService.getQueueSize(eventId);
            currentlyShopping = redisService.getActiveShoppersCount(eventId);
        }

        long secondsUntilPreQueue = 0;
        long secondsUntilSaleStart = 0;

        if (now.isBefore(waitingRoom.getPreQueueStart())) {
            secondsUntilPreQueue = Duration.between(now, waitingRoom.getPreQueueStart()).getSeconds();
        }
        if (now.isBefore(waitingRoom.getSaleStart())) {
            secondsUntilSaleStart = Duration.between(now, waitingRoom.getSaleStart()).getSeconds();
        }

        boolean canJoinNow = waitingRoom.getIsEnabled() &&
                (waitingRoom.getStatus() == WaitingRoomStatus.PRE_QUEUE ||
                        waitingRoom.getStatus() == WaitingRoomStatus.SELLING);

        return WaitingRoomResponse.builder()
                .id(waitingRoom.getId())
                .eventId(waitingRoom.getEvent().getId())
                .eventTitle(waitingRoom.getEvent().getTitle())
                .eventSlug(waitingRoom.getEvent().getSlug())
                .preQueueStart(waitingRoom.getPreQueueStart())
                .saleStart(waitingRoom.getSaleStart())
                .saleEnd(waitingRoom.getSaleEnd())
                .throughputPerMinute(waitingRoom.getThroughputPerMinute())
                .maxConcurrentUsers(waitingRoom.getMaxConcurrentUsers())
                .sessionTimeoutMinutes(waitingRoom.getSessionTimeoutMinutes())
                .status(waitingRoom.getStatus())
                .isEnabled(waitingRoom.getIsEnabled())
                .shuffledAt(waitingRoom.getShuffledAt())
                .totalQueueEntries(waitingRoom.getTotalQueueEntries())
                .totalAdmitted(waitingRoom.getTotalAdmitted())
                .totalCompleted(waitingRoom.getTotalCompleted())
                .currentlyWaiting(currentlyWaiting)
                .currentlyShopping(currentlyShopping)
                .secondsUntilPreQueue(secondsUntilPreQueue)
                .secondsUntilSaleStart(secondsUntilSaleStart)
                .canJoinNow(canJoinNow)
                .createdAt(waitingRoom.getCreatedAt())
                .updatedAt(waitingRoom.getUpdatedAt())
                .build();
    }
}