package com.gnxrt.ticketgoapi.dto.request.organizer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizerRegistrationRequest {

    @NotBlank(message = "Tên tổ chức không được để trống")
    @Size(max = 255, message = "Tên tổ chức tối đa 255 ký tự")
    private String organizationName;

    @Size(max = 2000, message = "Mô tả tối đa 2000 ký tự")
    private String organizationDescription;

    @Size(max = 500, message = "Website tối đa 500 ký tự")
    private String website;

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    private String contactPhone;

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    private String address;

    @Size(max = 50, message = "Mã số thuế tối đa 50 ký tự")
    private String taxCode;

    // INDIVIDUAL, COMPANY, ORGANIZATION
    private String organizationType;

    @Size(max = 255, message = "Lĩnh vực hoạt động tối đa 255 ký tự")
    private String businessField;

    private String verificationDocumentUrl;

    @Size(max = 2000, message = "Lý do tối đa 2000 ký tự")
    private String reason;
}