package com.gnxrt.ticketgoapi.dto.event;

import com.gnxrt.ticketgoapi.enums.EmailType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailEvent implements Serializable {

    private EmailType type;
    private String to;
    private String subject;
    private String templateName;
    private Map<String, Object> templateVariables;
    private Map<String, byte[]> inlineImages;
}
