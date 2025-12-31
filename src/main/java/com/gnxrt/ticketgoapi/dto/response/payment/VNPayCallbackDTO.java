package com.gnxrt.ticketgoapi.dto.response.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VNPayCallbackDTO {

    private String vnpTmnCode;
    private String vnpAmount;
    private String vnpBankCode;
    private String vnpBankTranNo;
    private String vnpCardType;
    private String vnpPayDate;
    private String vnpOrderInfo;
    private String vnpTransactionNo;
    private String vnpResponseCode;
    private String vnpTransactionStatus;
    private String vnpTxnRef;
    private String vnpSecureHash;

    /**
     *
     */
    public boolean isSuccess() {
        return "00".equals(vnpResponseCode) && "00".equals(vnpTransactionStatus);
    }

    /**
     *
     */
    public String getResponseMessage() {
        if (vnpResponseCode == null) return "Unknown error";

        return switch (vnpResponseCode) {
            case "00" -> "Giao dịch thành công";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ (liên quan tới lừa đảo, giao dịch bất thường)";
            case "09" -> "Giao dịch không thành công do: Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking";
            case "10" -> "Giao dịch không thành công do: Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần";
            case "11" -> "Giao dịch không thành công do: Đã hết hạn chờ thanh toán";
            case "12" -> "Giao dịch không thành công do: Thẻ/Tài khoản bị khóa";
            case "13" -> "Giao dịch không thành công do: Nhập sai mật khẩu xác thực (OTP)";
            case "24" -> "Giao dịch không thành công do: Khách hàng hủy giao dịch";
            case "51" -> "Giao dịch không thành công do: Tài khoản không đủ số dư";
            case "65" -> "Giao dịch không thành công do: Tài khoản đã vượt quá hạn mức giao dịch trong ngày";
            case "75" -> "Ngân hàng thanh toán đang bảo trì";
            case "79" -> "Giao dịch không thành công do: Nhập sai mật khẩu thanh toán quá số lần quy định";
            case "99" -> "Các lỗi khác";
            default -> "Lỗi không xác định: " + vnpResponseCode;
        };
    }
}