package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.user.ChangePasswordRequest;
import com.gnxrt.ticketgoapi.dto.request.user.DeleteAccountRequest;
import com.gnxrt.ticketgoapi.dto.request.user.UserPreferencesRequest;
import com.gnxrt.ticketgoapi.dto.request.user.UserProfileUpdateRequest;
import com.gnxrt.ticketgoapi.dto.response.user.UserPreferencesDTO;
import com.gnxrt.ticketgoapi.dto.response.user.UserProfileDTO;
import com.gnxrt.ticketgoapi.enums.EventStatus;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.Event;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.model.UserPreference;
import com.gnxrt.ticketgoapi.repository.EventRepository;
import com.gnxrt.ticketgoapi.repository.UserPreferenceRepository;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import com.gnxrt.ticketgoapi.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private static final long MAX_AVATAR_SIZE_BYTES = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_AVATAR_CONTENT_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;
    private final EventRepository eventRepository;
    private final JwtService jwtService;
    private final TokenBlacklistService tokenBlacklistService;
    private final UserPreferenceRepository userPreferenceRepository;

    /**
     * Lấy profile của user đang đăng nhập
     */
    public UserProfileDTO getMyProfile() {
        User user = getCurrentUser();
        return mapToProfileDTO(user);
    }

    /**
     * Cập nhật profile
     */
    @Transactional
    public UserProfileDTO updateMyProfile(UserProfileUpdateRequest request) {
        User user = getCurrentUser();

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            User finalUser = user;
            userRepository.findByPhone(request.getPhone())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getId().equals(finalUser.getId())) {
                            throw new BadRequestException("Số điện thoại đã được sử dụng bởi tài khoản khác");
                        }
                    });
            user.setPhone(request.getPhone());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        user = userRepository.save(user);
        log.info("User {} updated profile successfully", user.getId());

        return mapToProfileDTO(user);
    }

    /**
     * Đổi mật khẩu
     */
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();

        // Validate mật khẩu mới và xác nhận khớp nhau
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException("Mật khẩu mới và xác nhận mật khẩu không khớp");
        }

        // Validate mật khẩu hiện tại
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }

        // Validate mật khẩu mới khác mật khẩu cũ
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        log.info("User {} changed password successfully", user.getId());
    }

    /**
     * Upload avatar lên Cloudinary và cập nhật user.avatarUrl.
     */
    @Transactional
    public UserProfileDTO uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File ảnh không được để trống");
        }
        if (file.getSize() > MAX_AVATAR_SIZE_BYTES) {
            throw new BadRequestException("Ảnh vượt quá dung lượng cho phép (tối đa 5MB)");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_AVATAR_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Định dạng ảnh không hợp lệ, chỉ chấp nhận JPEG, PNG, WEBP");
        }

        User user = getCurrentUser();
        String secureUrl = cloudinaryService.uploadAvatar(file, user.getId());
        user.setAvatarUrl(secureUrl);
        userRepository.save(user);
        log.info("User {} uploaded avatar", user.getId());

        return mapToProfileDTO(user);
    }

    /**
     * Soft-delete tài khoản: verify password, chặn nếu ORGANIZER còn event đang hoạt động,
     * rename email, set isActive=false + deletedAt, blacklist access token hiện tại.
     */
    @Transactional
    public void deleteAccount(DeleteAccountRequest request, String accessToken) {
        User user = getCurrentUser();

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu không đúng");
        }

        if (user.isOrganizer()) {
            List<Event> activeEvents = eventRepository.findByOrganizerIdAndStatusIn(
                    user.getId(),
                    List.of(EventStatus.APPROVED, EventStatus.PUBLISHED)
            );
            boolean hasOngoing = activeEvents.stream()
                    .anyMatch(e -> e.getEndDate() != null
                            && e.getEndDate().isAfter(LocalDateTime.now()));
            if (hasOngoing) {
                throw new BadRequestException(
                        "Bạn còn sự kiện đang hoạt động, không thể xoá tài khoản");
            }
        }

        long epochMillis = System.currentTimeMillis();
        user.setEmail(user.getEmail() + "_deleted_" + epochMillis);
        user.setIsActive(false);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);

        blacklistAccessToken(accessToken);

        log.info("Account deleted for user id: {} (reason: {})",
                user.getId(),
                request.getReason() != null ? request.getReason() : "N/A");

        SecurityContextHolder.clearContext();
    }

    private void blacklistAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            String jti = jwtService.extractJti(accessToken);
            Date expiration = jwtService.extractExpiration(accessToken);
            if (jti == null || expiration == null) {
                return;
            }
            long millisLeft = expiration.getTime() - System.currentTimeMillis();
            if (millisLeft > 0) {
                tokenBlacklistService.blacklist(jti, Duration.ofMillis(millisLeft));
            }
        } catch (Exception e) {
            log.warn("Failed to blacklist access token on delete: {}", e.getMessage());
        }
    }

    /**
     * Lấy preferences của user hiện tại. Nếu chưa có record, trả về default (không tạo row).
     */
    public UserPreferencesDTO getMyPreferences() {
        User user = getCurrentUser();
        return userPreferenceRepository.findById(user.getId())
                .map(this::mapToPreferencesDTO)
                .orElseGet(this::defaultPreferences);
    }

    /**
     * Upsert preferences: null field = không đổi. Tạo row mới nếu chưa tồn tại.
     */
    @Transactional
    public UserPreferencesDTO updateMyPreferences(UserPreferencesRequest request) {
        User user = getCurrentUser();
        UserPreference pref = userPreferenceRepository.findById(user.getId())
                .orElseGet(() -> {
                    UserPreference p = new UserPreference();
                    p.setUser(user);
                    return p;
                });

        if (request.getEmailNotification() != null) {
            pref.setEmailNotification(request.getEmailNotification());
        }
        if (request.getMarketingEmail() != null) {
            pref.setMarketingEmail(request.getMarketingEmail());
        }
        if (request.getLanguage() != null) {
            pref.setLanguage(request.getLanguage());
        }
        if (request.getTimezone() != null) {
            pref.setTimezone(request.getTimezone());
        }

        pref = userPreferenceRepository.save(pref);
        log.info("User {} updated preferences", user.getId());
        return mapToPreferencesDTO(pref);
    }

    private UserPreferencesDTO mapToPreferencesDTO(UserPreference pref) {
        return UserPreferencesDTO.builder()
                .emailNotification(pref.getEmailNotification())
                .marketingEmail(pref.getMarketingEmail())
                .language(pref.getLanguage())
                .timezone(pref.getTimezone())
                .build();
    }

    private UserPreferencesDTO defaultPreferences() {
        return UserPreferencesDTO.builder()
                .emailNotification(true)
                .marketingEmail(false)
                .language("vi")
                .timezone("Asia/Ho_Chi_Minh")
                .build();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private UserProfileDTO mapToProfileDTO(User user) {
        Integer totalOrders = userRepository.countOrdersByUserId(user.getId());
        Integer totalTickets = userRepository.countTicketsPurchasedByUserId(user.getId());
        BigDecimal totalSpent = userRepository.getTotalSpentByUserId(user.getId());
        Integer totalReviews = userRepository.countReviewsByUserId(user.getId());
        Integer totalEvents = userRepository.countEventsOrganizedByUserId(user.getId());

        return UserProfileDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .emailVerified(user.getEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .totalOrders(totalOrders)
                .totalTickets(totalTickets)
                .totalSpent(totalSpent != null ? totalSpent : BigDecimal.ZERO)
                .totalReviews(totalReviews)
                .totalEventsOrganized(totalEvents)
                .build();
    }
}