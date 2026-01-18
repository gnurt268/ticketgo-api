package com.gnxrt.ticketgoapi.dto.request.waitingroom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinQueueRequest {

    private String visitorToken;

    private String fingerprint;

    private String captchaToken;
}