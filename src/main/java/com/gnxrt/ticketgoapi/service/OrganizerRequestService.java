package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.dto.request.organizer.OrganizerRegistrationRequest;
import com.gnxrt.ticketgoapi.dto.request.organizer.OrganizerRequestReviewRequest;
import com.gnxrt.ticketgoapi.dto.response.organizer.OrganizerRequestDTO;
import com.gnxrt.ticketgoapi.enums.OrganizerRequestStatus;
import com.gnxrt.ticketgoapi.enums.Role;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import com.gnxrt.ticketgoapi.exception.ConflictException;
import com.gnxrt.ticketgoapi.exception.ResourceNotFoundException;
import com.gnxrt.ticketgoapi.model.OrganizerRequest;
import com.gnxrt.ticketgoapi.model.User;
import com.gnxrt.ticketgoapi.repository.OrganizerRequestRepository;
import com.gnxrt.ticketgoapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerRequestService {

    private final OrganizerRequestRepository organizerRequestRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Transactional
    public OrganizerRequestDTO submitRequest(Long userId, OrganizerRegistrationRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));

        if (user.getRole() == Role.ORGANIZER || user.getRole() == Role.ADMIN) {
            throw new ConflictException("Bạn đã là Organizer hoặc Admin");
        }

        if (organizerRequestRepository.existsByUserIdAndStatus(userId, OrganizerRequestStatus.PENDING)) {
            throw new ConflictException("Bạn đã có yêu cầu đang chờ xử lý");
        }

        OrganizerRequest organizerRequest = OrganizerRequest.builder()
                .user(user)
                .organizationName(request.getOrganizationName())
                .organizationDescription(request.getOrganizationDescription())
                .website(request.getWebsite())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .taxCode(request.getTaxCode())
                .organizationType(request.getOrganizationType())
                .businessField(request.getBusinessField())
                .verificationDocumentUrl(request.getVerificationDocumentUrl())
                .reason(request.getReason())
                .status(OrganizerRequestStatus.PENDING)
                .build();

        organizerRequest = organizerRequestRepository.save(organizerRequest);
        log.info("User {} submitted organizer request {}", userId, organizerRequest.getId());

        emailService.sendOrganizerRequestReceivedEmail(user, organizerRequest);

        return mapToDTO(organizerRequest);
    }

    public OrganizerRequestDTO getMyRequest(Long userId) {
        OrganizerRequest request = organizerRequestRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Bạn chưa có yêu cầu đăng ký Organizer"));
        return mapToDTO(request);
    }

    @Transactional
    public void cancelMyRequest(Long userId) {
        OrganizerRequest request = organizerRequestRepository
                .findByUserIdAndStatus(userId, OrganizerRequestStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu đang chờ xử lý"));

        request.setStatus(OrganizerRequestStatus.CANCELLED);
        organizerRequestRepository.save(request);
        log.info("User {} cancelled organizer request {}", userId, request.getId());
    }

    public Page<OrganizerRequestDTO> getAllRequests(
            OrganizerRequestStatus status,
            String keyword,
            Pageable pageable
    ) {
        Page<OrganizerRequest> requests = organizerRequestRepository.findAllWithFilters(status, keyword, pageable);
        return requests.map(this::mapToDTO);
    }

    public Page<OrganizerRequestDTO> getPendingRequests(Pageable pageable) {
        Page<OrganizerRequest> requests = organizerRequestRepository.findByStatus(OrganizerRequestStatus.PENDING, pageable);
        return requests.map(this::mapToDTO);
    }

    public OrganizerRequestDTO getRequestById(Long requestId) {
        OrganizerRequest request = organizerRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại"));
        return mapToDTO(request);
    }

    @Transactional
    public OrganizerRequestDTO reviewRequest(Long requestId, Long adminId, OrganizerRequestReviewRequest reviewRequest) {
        OrganizerRequest request = organizerRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Yêu cầu không tồn tại"));

        if (request.getStatus() != OrganizerRequestStatus.PENDING) {
            throw new BadRequestException("Yêu cầu này đã được xử lý");
        }

        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin không tồn tại"));

        if (reviewRequest.getApproved()) {
            request.setStatus(OrganizerRequestStatus.APPROVED);

            User user = request.getUser();
            user.setRole(Role.ORGANIZER);
            userRepository.save(user);

            log.info("Admin {} approved organizer request {} for user {}", adminId, requestId, user.getId());

            emailService.sendOrganizerRequestApprovedEmail(user, request);
        } else {
            // Từ chối yêu cầu
            if (reviewRequest.getRejectionReason() == null || reviewRequest.getRejectionReason().isBlank()) {
                throw new BadRequestException("Vui lòng nhập lý do từ chối");
            }
            request.setStatus(OrganizerRequestStatus.REJECTED);
            request.setRejectionReason(reviewRequest.getRejectionReason());

            log.info("Admin {} rejected organizer request {}", adminId, requestId);

            emailService.sendOrganizerRequestRejectedEmail(request.getUser(), request);
        }

        request.setReviewedBy(admin);
        request.setReviewedAt(LocalDateTime.now());
        request.setAdminNotes(reviewRequest.getAdminNotes());

        return mapToDTO(organizerRequestRepository.save(request));
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalRequests", organizerRequestRepository.count());
        stats.put("pendingRequests", organizerRequestRepository.countByStatus(OrganizerRequestStatus.PENDING));
        stats.put("approvedRequests", organizerRequestRepository.countByStatus(OrganizerRequestStatus.APPROVED));
        stats.put("rejectedRequests", organizerRequestRepository.countByStatus(OrganizerRequestStatus.REJECTED));

        return stats;
    }

    private OrganizerRequestDTO mapToDTO(OrganizerRequest request) {
        User user = request.getUser();

        return OrganizerRequestDTO.builder()
                .id(request.getId())
                .userId(user.getId())
                .userEmail(user.getEmail())
                .userFullName(user.getFullName())
                .userPhone(user.getPhone())
                .userAvatarUrl(user.getAvatarUrl())
                .organizationName(request.getOrganizationName())
                .organizationDescription(request.getOrganizationDescription())
                .website(request.getWebsite())
                .contactPhone(request.getContactPhone())
                .address(request.getAddress())
                .taxCode(request.getTaxCode())
                .organizationType(request.getOrganizationType())
                .businessField(request.getBusinessField())
                .verificationDocumentUrl(request.getVerificationDocumentUrl())
                .reason(request.getReason())
                .status(request.getStatus().name())
                .rejectionReason(request.getRejectionReason())
                .reviewedById(request.getReviewedBy() != null ? request.getReviewedBy().getId() : null)
                .reviewedByName(request.getReviewedBy() != null ? request.getReviewedBy().getFullName() : null)
                .reviewedAt(request.getReviewedAt() != null ? request.getReviewedAt().format(FORMATTER) : null)
                .adminNotes(request.getAdminNotes())
                .createdAt(request.getCreatedAt().format(FORMATTER))
                .updatedAt(request.getUpdatedAt().format(FORMATTER))
                .build();
    }
}