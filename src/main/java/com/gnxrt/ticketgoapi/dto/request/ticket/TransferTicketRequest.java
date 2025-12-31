package com.gnxrt.ticketgoapi.dto.request.ticket;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferTicketRequest {

    @NotBlank(message = "Recipient name is required")
    @Size(max = 255, message = "Recipient name must not exceed 255 characters")
    private String recipientName;

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Invalid email format")
    private String recipientEmail;

    @NotBlank(message = "Recipient phone is required")
    @Size(max = 20, message = "Recipient phone must not exceed 20 characters")
    private String recipientPhone;

    @Size(max = 50, message = "Recipient ID number must not exceed 50 characters")
    private String recipientIdNumber;
}