package com.gnxrt.ticketgoapi.dto.request.order;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "Event ID is required")
    private Long eventId;

    @NotNull(message = "Ticket zone ID is required")
    private Long ticketZoneId;

    /**
     *
     */
    @Min(value = 1, message = "Quantity must be at least 1")
    @Max(value = 10, message = "Quantity must not exceed 10")
    private Integer quantity;

    /**
     *
     */
    @Size(max = 10, message = "Cannot order more than 10 seats")
    private List<Long> seatIds;

    @NotBlank(message = "Buyer name is required")
    @Size(max = 255, message = "Buyer name must not exceed 255 characters")
    private String buyerName;

    @NotBlank(message = "Buyer email is required")
    @Email(message = "Invalid email format")
    private String buyerEmail;

    @NotBlank(message = "Buyer phone is required")
    @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Invalid phone number format")
    private String buyerPhone;

    private List<AttendeeInfo> attendees;

    private String notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttendeeInfo {
        @NotBlank(message = "Attendee name is required")
        private String name;

        @NotBlank(message = "Attendee email is required")
        @Email(message = "Invalid email format")
        private String email;

        @NotBlank(message = "Attendee phone is required")
        private String phone;

        private String idNumber;
    }
}