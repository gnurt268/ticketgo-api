package com.gnxrt.ticketgoapi.dto.request.user;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesRequest {

    private Boolean emailNotification;

    private Boolean marketingEmail;

    @Size(max = 10, message = "Language code must be at most 10 characters")
    private String language;

    @Size(max = 50, message = "Timezone must be at most 50 characters")
    private String timezone;
}
