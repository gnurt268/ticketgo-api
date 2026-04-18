package com.gnxrt.ticketgoapi.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesDTO {

    private Boolean emailNotification;
    private Boolean marketingEmail;
    private String language;
    private String timezone;
}
