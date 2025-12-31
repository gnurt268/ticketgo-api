package com.gnxrt.ticketgoapi.service;

import com.gnxrt.ticketgoapi.config.VNPayConfig;
import com.gnxrt.ticketgoapi.dto.response.payment.VNPayCallbackDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayConfig vnPayConfig;

    private static final String HMAC_SHA512 = "HmacSHA512";

    /**
     * Tạo URL thanh toán VNPay
     */
    public String createPaymentUrl(String orderCode, BigDecimal amount, String description, String ipAddress) {
        log.info("Creating VNPay payment URL for order: {}, amount: {}", orderCode, amount);

        Map<String, String> vnpParams = new TreeMap<>();

        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount.multiply(new BigDecimal("100")).longValue()));
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrencyCode());
        vnpParams.put("vnp_TxnRef", orderCode);
        vnpParams.put("vnp_OrderInfo", description);
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String createDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_CreateDate", createDate);

        calendar.add(Calendar.MINUTE, 15);
        String expireDate = formatter.format(calendar.getTime());
        vnpParams.put("vnp_ExpireDate", expireDate);

        StringBuilder queryBuilder = new StringBuilder();
        StringBuilder hashDataBuilder = new StringBuilder();

        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                hashDataBuilder.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                        .append("&");

                queryBuilder.append(URLEncoder.encode(entry.getKey(), StandardCharsets.US_ASCII))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                        .append("&");
            }
        }

        String hashData = hashDataBuilder.substring(0, hashDataBuilder.length() - 1);
        String query = queryBuilder.substring(0, queryBuilder.length() - 1);

        String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData);
        query += "&vnp_SecureHash=" + secureHash;

        String paymentUrl = vnPayConfig.getPayUrl() + "?" + query;
        log.info("Created VNPay payment URL for order: {}", orderCode);

        return paymentUrl;
    }

    /**
     * Xử lý callback từ VNPay (return URL)
     */
    public VNPayCallbackDTO processCallback(HttpServletRequest request) {
        log.info("Processing VNPay callback");

        Map<String, String> fields = new HashMap<>();
        Enumeration<String> params = request.getParameterNames();

        while (params.hasMoreElements()) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                fields.put(fieldName, fieldValue);
            }
        }

        return VNPayCallbackDTO.builder()
                .vnpTmnCode(fields.get("vnp_TmnCode"))
                .vnpAmount(fields.get("vnp_Amount"))
                .vnpBankCode(fields.get("vnp_BankCode"))
                .vnpBankTranNo(fields.get("vnp_BankTranNo"))
                .vnpCardType(fields.get("vnp_CardType"))
                .vnpPayDate(fields.get("vnp_PayDate"))
                .vnpOrderInfo(fields.get("vnp_OrderInfo"))
                .vnpTransactionNo(fields.get("vnp_TransactionNo"))
                .vnpResponseCode(fields.get("vnp_ResponseCode"))
                .vnpTransactionStatus(fields.get("vnp_TransactionStatus"))
                .vnpTxnRef(fields.get("vnp_TxnRef"))
                .vnpSecureHash(fields.get("vnp_SecureHash"))
                .build();
    }

    /**
     * Xác thực chữ ký từ VNPay
     */
    public boolean validateSignature(HttpServletRequest request) {
        log.info("Validating VNPay signature");

        Map<String, String> fields = new TreeMap<>();
        Enumeration<String> params = request.getParameterNames();

        while (params.hasMoreElements()) {
            String fieldName = params.nextElement();
            String fieldValue = request.getParameter(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                fields.put(fieldName, fieldValue);
            }
        }

        String vnpSecureHash = fields.remove("vnp_SecureHash");
        fields.remove("vnp_SecureHashType");

        if (vnpSecureHash == null) {
            log.warn("Missing vnp_SecureHash in callback");
            return false;
        }

        StringBuilder hashDataBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                hashDataBuilder.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII))
                        .append("&");
            }
        }

        String hashData = hashDataBuilder.substring(0, hashDataBuilder.length() - 1);
        String calculatedHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData);

        boolean isValid = calculatedHash.equalsIgnoreCase(vnpSecureHash);
        log.info("VNPay signature validation result: {}", isValid);

        return isValid;
    }

    /**
     * Lấy IP address từ request
     */
    public String getIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");

        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }

        if (ipAddress != null && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }

        return ipAddress;
    }

    /**
     * Tạo HMAC SHA512 hash
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance(HMAC_SHA512);
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA512);
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder();
            for (byte b : result) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("Error creating HMAC SHA512 hash", e);
            throw new RuntimeException("Error creating hash", e);
        }
    }

    /**
     * Parse ngày thanh toán từ VNPay
     */
    public Date parsePayDate(String vnpPayDate) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            return formatter.parse(vnpPayDate);
        } catch (Exception e) {
            log.error("Error parsing VNPay pay date: {}", vnpPayDate, e);
            return new Date();
        }
    }

    /**
     * Chuyển đổi số tiền từ VNPay (đã nhân 100) về giá trị thực
     */
    public BigDecimal parseAmount(String vnpAmount) {
        try {
            return new BigDecimal(vnpAmount).divide(new BigDecimal("100"));
        } catch (Exception e) {
            log.error("Error parsing VNPay amount: {}", vnpAmount, e);
            return BigDecimal.ZERO;
        }
    }
}