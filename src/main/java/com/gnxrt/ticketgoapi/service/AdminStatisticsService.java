package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.response.admin.AdminStatisticsDTO;
import com.gnxrt.ticketgoapi.enums.CheckInMethod;
import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.enums.PaymentStatus;
import com.gnxrt.ticketgoapi.enums.Role;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.Order;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;

    public AdminStatisticsDTO getAllStatistics() {
        log.info("Calculating all statistics for admin dashboard");

        return AdminStatisticsDTO.builder()
                .userStats(getUserStatistics())
                .eventStats(getEventStatistics())
                .revenueStats(getRevenueStatistics())
                .ticketStats(getTicketStatistics())
                .recentActivities(getRecentActivities())
                .trendData(getTrendData())
                .build();
    }

    private AdminStatisticsDTO.UserStats getUserStatistics() {
        LocalDateTime today = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime weekAgo = today.minusDays(7);
        LocalDateTime monthAgo = today.minusDays(30);

        Long totalUsers = userRepository.count();
        Long totalAdmins = userRepository.countByRole(Role.ADMIN);
        Long totalOrganizers = userRepository.countByRole(Role.ORGANIZER);
        Long totalRegularUsers = userRepository.countByRole(Role.USER);
        Long activeUsers = userRepository.countByIsActive(true);
        Long inactiveUsers = userRepository.countByIsActive(false);

        List<User> usersToday = userRepository.findUsersCreatedBetween(today, LocalDateTime.now());
        List<User> usersThisWeek = userRepository.findUsersCreatedBetween(weekAgo, LocalDateTime.now());
        List<User> usersThisMonth = userRepository.findUsersCreatedBetween(monthAgo, LocalDateTime.now());

        return AdminStatisticsDTO.UserStats.builder()
                .totalUsers(totalUsers)
                .totalAdmins(totalAdmins)
                .totalOrganizers(totalOrganizers)
                .totalRegularUsers(totalRegularUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .newUsersToday((long) usersToday.size())
                .newUsersThisWeek((long) usersThisWeek.size())
                .newUsersThisMonth((long) usersThisMonth.size())
                .build();
    }

    private AdminStatisticsDTO.EventStats getEventStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime monthAgo = now.minusDays(30);

        Long totalEvents = eventRepository.count();
        Long draftEvents = eventRepository.countByStatus(EventStatus.DRAFT);
        Long pendingEvents = eventRepository.countByStatus(EventStatus.PENDING);
        Long approvedEvents = eventRepository.countByStatus(EventStatus.APPROVED);
        Long publishedEvents = eventRepository.countByStatus(EventStatus.PUBLISHED);
        Long cancelledEvents = eventRepository.countByStatus(EventStatus.CANCELLED);
        Long completedEvents = eventRepository.countByStatus(EventStatus.COMPLETED);

        Long featuredEvents = eventRepository.findFeaturedEvents(now, PageRequest.of(0, Integer.MAX_VALUE))
                .getTotalElements();

        List<Event> eventsThisMonth = eventRepository.findEventsBetween(monthAgo, now);

        List<Event> allPublished = eventRepository.findByStatus(EventStatus.PUBLISHED, PageRequest.of(0, Integer.MAX_VALUE))
                .getContent();

        Long upcomingEvents = allPublished.stream().filter(e -> e.getStartDate().isAfter(now)).count();
        Long ongoingEvents = allPublished.stream()
                .filter(e -> e.getStartDate().isBefore(now) && e.getEndDate().isAfter(now))
                .count();
        Long pastEvents = allPublished.stream().filter(e -> e.getEndDate().isBefore(now)).count();

        return AdminStatisticsDTO.EventStats.builder()
                .totalEvents(totalEvents)
                .draftEvents(draftEvents)
                .pendingEvents(pendingEvents)
                .approvedEvents(approvedEvents)
                .publishedEvents(publishedEvents)
                .cancelledEvents(cancelledEvents)
                .completedEvents(completedEvents)
                .featuredEvents(featuredEvents)
                .eventsThisMonth((long) eventsThisMonth.size())
                .upcomingEvents(upcomingEvents)
                .ongoingEvents(ongoingEvents)
                .pastEvents(pastEvents)
                .build();
    }

    private AdminStatisticsDTO.RevenueStats getRevenueStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.with(LocalTime.MIN);
        LocalDateTime weekAgo = today.minusDays(7);
        LocalDateTime monthAgo = today.minusDays(30);
        LocalDateTime yearAgo = today.minusYears(1);

        BigDecimal totalRevenue = orderRepository.getTotalRevenue();
        BigDecimal revenueToday = orderRepository.getRevenueBetween(today, now);
        BigDecimal revenueThisWeek = orderRepository.getRevenueBetween(weekAgo, now);
        BigDecimal revenueThisMonth = orderRepository.getRevenueBetween(monthAgo, now);
        BigDecimal revenueThisYear = orderRepository.getRevenueBetween(yearAgo, now);

        Long totalOrders = orderRepository.count();
        Long pendingOrders = orderRepository.countByPaymentStatus(PaymentStatus.PENDING);
        Long paidOrders = orderRepository.countByPaymentStatus(PaymentStatus.COMPLETED);
        Long failedOrders = orderRepository.countByPaymentStatus(PaymentStatus.FAILED);
        Long cancelledOrders = orderRepository.countByPaymentStatus(PaymentStatus.CANCELLED);

        BigDecimal averageOrderValue = orderRepository.getAverageOrderValue();

        List<Object[]> topEvents = orderRepository.getTopEventsByRevenue(PageRequest.of(0, 1));
        String topEventName = "N/A";
        BigDecimal topEventRevenue = BigDecimal.ZERO;
        if (!topEvents.isEmpty()) {
            Object[] topEvent = topEvents.get(0);
            topEventName = (String) topEvent[1];
            topEventRevenue = (BigDecimal) topEvent[2];
        }

        return AdminStatisticsDTO.RevenueStats.builder()
                .totalRevenue(totalRevenue)
                .revenueToday(revenueToday)
                .revenueThisWeek(revenueThisWeek)
                .revenueThisMonth(revenueThisMonth)
                .revenueThisYear(revenueThisYear)
                .totalOrders(totalOrders)
                .pendingOrders(pendingOrders)
                .paidOrders(paidOrders)
                .failedOrders(failedOrders)
                .cancelledOrders(cancelledOrders)
                .averageOrderValue(averageOrderValue)
                .topEventRevenue(topEventRevenue)
                .topEventName(topEventName)
                .build();
    }

    private AdminStatisticsDTO.TicketStats getTicketStatistics() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime today = now.with(LocalTime.MIN);
        LocalDateTime weekAgo = today.minusDays(7);
        LocalDateTime monthAgo = today.minusDays(30);

        Long totalTicketsSold = ticketRepository.count();
        Long totalTicketsCheckedIn = ticketRepository.countCheckedInTickets();
        Long ticketsSoldToday = ticketRepository.countTicketsSoldBetween(today, now);
        Long ticketsSoldThisWeek = ticketRepository.countTicketsSoldBetween(weekAgo, now);
        Long ticketsSoldThisMonth = ticketRepository.countTicketsSoldBetween(monthAgo, now);

        Double checkInRate = ticketRepository.getCheckInRate();
        if (checkInRate == null) checkInRate = 0.0;

        Long faceRecognitionCheckIns = ticketRepository.countByCheckInMethod(CheckInMethod.FACE_RECOGNITION);
        Long qrCodeCheckIns = ticketRepository.countByCheckInMethod(CheckInMethod.QR_CODE);
        Long manualCheckIns = ticketRepository.countByCheckInMethod(CheckInMethod.MANUAL);

        return AdminStatisticsDTO.TicketStats.builder()
                .totalTicketsSold(totalTicketsSold)
                .totalTicketsCheckedIn(totalTicketsCheckedIn)
                .ticketsSoldToday(ticketsSoldToday)
                .ticketsSoldThisWeek(ticketsSoldThisWeek)
                .ticketsSoldThisMonth(ticketsSoldThisMonth)
                .checkInRate(checkInRate)
                .faceRecognitionCheckIns(faceRecognitionCheckIns)
                .qrCodeCheckIns(qrCodeCheckIns)
                .manualCheckIns(manualCheckIns)
                .build();
    }

    private AdminStatisticsDTO.RecentActivities getRecentActivities() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<User> recentUsersList = userRepository.findAll(PageRequest.of(0, 5,
                org.springframework.data.domain.Sort.by("createdAt").descending())).getContent();
        List<AdminStatisticsDTO.RecentUser> recentUsers = recentUsersList.stream()
                .map(u -> AdminStatisticsDTO.RecentUser.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .fullName(u.getFullName())
                        .role(u.getRole().name())
                        .createdAt(u.getCreatedAt().format(formatter))
                        .build())
                .collect(Collectors.toList());

        List<Event> recentEventsList = eventRepository.findAll(PageRequest.of(0, 5,
                org.springframework.data.domain.Sort.by("createdAt").descending())).getContent();
        List<AdminStatisticsDTO.RecentEvent> recentEvents = recentEventsList.stream()
                .map(e -> AdminStatisticsDTO.RecentEvent.builder()
                        .id(e.getId())
                        .title(e.getTitle())
                        .organizerName(e.getOrganizer().getFullName())
                        .status(e.getStatus().name())
                        .createdAt(e.getCreatedAt().format(formatter))
                        .build())
                .collect(Collectors.toList());

        List<Order> recentOrdersList = orderRepository.findRecentOrders(PageRequest.of(0, 5));
        List<AdminStatisticsDTO.RecentOrder> recentOrders = recentOrdersList.stream()
                .map(o -> AdminStatisticsDTO.RecentOrder.builder()
                        .id(o.getId())
                        .orderCode(o.getOrderCode())
                        .buyerName(o.getBuyerName())
                        .totalAmount(o.getTotalAmount())
                        .paymentStatus(o.getPaymentStatus().name())
                        .createdAt(o.getCreatedAt().format(formatter))
                        .build())
                .collect(Collectors.toList());

        List<Event> pendingList = eventRepository.findPendingEvents(PageRequest.of(0, 10)).getContent();
        List<AdminStatisticsDTO.PendingApproval> pendingApprovals = pendingList.stream()
                .map(e -> AdminStatisticsDTO.PendingApproval.builder()
                        .id(e.getId())
                        .title(e.getTitle())
                        .organizerName(e.getOrganizer().getFullName())
                        .submittedAt(e.getCreatedAt().format(formatter))
                        .build())
                .collect(Collectors.toList());

        return AdminStatisticsDTO.RecentActivities.builder()
                .recentUsers(recentUsers)
                .recentEvents(recentEvents)
                .recentOrders(recentOrders)
                .pendingApprovals(pendingApprovals)
                .build();
    }

    private AdminStatisticsDTO.TrendData getTrendData() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7).with(LocalTime.MIN);

        List<AdminStatisticsDTO.DataPoint> userTrend = getUserRegistrationTrend(sevenDaysAgo, now);

        List<AdminStatisticsDTO.DataPoint> revenueTrend = getRevenueTrend(sevenDaysAgo, now);

        List<AdminStatisticsDTO.DataPoint> ticketTrend = getTicketSalesTrend(sevenDaysAgo, now);

        Map<String, Long> eventsByCategory = getEventsByCategory();

        Map<String, Long> eventsByCity = getEventsByCity();

        Map<String, BigDecimal> revenueByCategory = getRevenueByCategory();

        return AdminStatisticsDTO.TrendData.builder()
                .userRegistrationTrend(userTrend)
                .revenueTrend(revenueTrend)
                .ticketSalesTrend(ticketTrend)
                .eventsByCategory(eventsByCategory)
                .eventsByCity(eventsByCity)
                .revenueByCategory(revenueByCategory)
                .build();
    }

    private List<AdminStatisticsDTO.DataPoint> getUserRegistrationTrend(LocalDateTime start, LocalDateTime end) {
        List<User> users = userRepository.findUsersCreatedBetween(start, end);
        Map<String, Long> countByDate = users.stream()
                .collect(Collectors.groupingBy(
                        u -> u.getCreatedAt().toLocalDate().toString(),
                        Collectors.counting()
                ));

        return buildDataPoints(countByDate, start.toLocalDate(), end.toLocalDate());
    }

    private List<AdminStatisticsDTO.DataPoint> getRevenueTrend(LocalDateTime start, LocalDateTime end) {
        List<Object[]> revenueData = orderRepository.getRevenueTrendByDay(start, end);
        Map<String, BigDecimal> revenueByDate = new HashMap<>();
        for (Object[] row : revenueData) {
            String date = row[0].toString();
            BigDecimal amount = (BigDecimal) row[1];
            revenueByDate.put(date, amount);
        }

        List<AdminStatisticsDTO.DataPoint> dataPoints = new ArrayList<>();
        LocalDate current = start.toLocalDate();
        while (!current.isAfter(end.toLocalDate())) {
            String dateStr = current.toString();
            BigDecimal amount = revenueByDate.getOrDefault(dateStr, BigDecimal.ZERO);
            dataPoints.add(AdminStatisticsDTO.DataPoint.builder()
                    .date(dateStr)
                    .value(0L)
                    .amount(amount)
                    .build());
            current = current.plusDays(1);
        }
        return dataPoints;
    }

    private List<AdminStatisticsDTO.DataPoint> getTicketSalesTrend(LocalDateTime start, LocalDateTime end) {
        List<Object[]> ticketData = ticketRepository.getTicketSalesTrendByDay(start, end);
        Map<String, Long> ticketsByDate = new HashMap<>();
        for (Object[] row : ticketData) {
            String date = row[0].toString();
            Long count = ((Number) row[1]).longValue();
            ticketsByDate.put(date, count);
        }

        return buildDataPoints(ticketsByDate, start.toLocalDate(), end.toLocalDate());
    }

    private List<AdminStatisticsDTO.DataPoint> buildDataPoints(Map<String, Long> dataMap, LocalDate start, LocalDate end) {
        List<AdminStatisticsDTO.DataPoint> dataPoints = new ArrayList<>();
        LocalDate current = start;
        while (!current.isAfter(end)) {
            String dateStr = current.toString();
            Long value = dataMap.getOrDefault(dateStr, 0L);
            dataPoints.add(AdminStatisticsDTO.DataPoint.builder()
                    .date(dateStr)
                    .value(value)
                    .amount(null)
                    .build());
            current = current.plusDays(1);
        }
        return dataPoints;
    }

    private Map<String, Long> getEventsByCategory() {
        List<Event> events = eventRepository.findAll();
        return events.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().getName(),
                        Collectors.counting()
                ));
    }

    private Map<String, Long> getEventsByCity() {
        List<Event> events = eventRepository.findAll();
        return events.stream()
                .filter(e -> e.getCity() != null)
                .collect(Collectors.groupingBy(
                        Event::getCity,
                        Collectors.counting()
                ));
    }

    private Map<String, BigDecimal> getRevenueByCategory() {
        List<Object[]> revenueData = orderRepository.getRevenueByCategory();
        Map<String, BigDecimal> result = new HashMap<>();
        for (Object[] row : revenueData) {
            String categoryName = (String) row[0];
            BigDecimal revenue = (BigDecimal) row[1];
            result.put(categoryName, revenue);
        }
        return result;
    }
}