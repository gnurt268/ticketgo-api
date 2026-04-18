package com.gnxrt.ticketgoapi.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.gnxrt.ticketgoapi.config.VNPayConfig;
import com.gnxrt.ticketgoapi.dto.response.payment.VNPayCallbackDTO;
import com.gnxrt.ticketgoapi.model.Payment;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayConfig vnPayConfig;
    private final RestTemplate restTemplate;

    private static final String HMAC_SHA512 = "HmacSHA512";
    private static final String VNP_TIMEZONE = "Etc/GMT+7";
    private static final String RECONCILE_IP = "127.0.0.1";

    /**
     * Tạo URL thanh toán VNPay
     */
    public String createPaymentUrl(String txnRef, BigDecimal amount, String description, String ipAddress) {
        log.info("Creating VNPay payment URL for txnRef: {}, amount: {}", txnRef, amount);

        Map<String, String> vnpParams = new TreeMap<>();

        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount.multiply(new BigDecimal("100")).longValue()));
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrencyCode());
        vnpParams.put("vnp_TxnRef", txnRef);
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
        log.info("Created VNPay payment URL for txnRef: {}", txnRef);

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
     * Gọi VNPay querydr để truy vấn trạng thái giao dịch.
     * Trả về VNPayCallbackDTO nếu query thành công (vnp_ResponseCode = "00"),
     * Optional.empty() nếu VNPay không có dữ liệu giao dịch hoặc query fail — caller nên giữ Payment ở PENDING.
     */
    public Optional<VNPayCallbackDTO> queryTransaction(Payment payment) {
        String vnpTxnRef = payment.getVnpTxnRef();
        log.info("Querying VNPay transaction status: vnpTxnRef={}", vnpTxnRef);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone(VNP_TIMEZONE));

        String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String version = vnPayConfig.getVersion();
        String command = "querydr";
        String tmnCode = vnPayConfig.getTmnCode();
        String orderInfo = "Query transaction " + vnpTxnRef;
        Date createdAtDate = Date.from(payment.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant());
        String transactionDate = formatter.format(createdAtDate);
        String createDate = formatter.format(new Date());
        String ipAddr = payment.getIpAddress() != null ? payment.getIpAddress() : RECONCILE_IP;

        String hashData = String.join("|",
                requestId, version, command, tmnCode, vnpTxnRef,
                transactionDate, createDate, ipAddr, orderInfo);
        String secureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("vnp_RequestId", requestId);
        body.put("vnp_Version", version);
        body.put("vnp_Command", command);
        body.put("vnp_TmnCode", tmnCode);
        body.put("vnp_TxnRef", vnpTxnRef);
        body.put("vnp_OrderInfo", orderInfo);
        body.put("vnp_TransactionDate", transactionDate);
        body.put("vnp_CreateDate", createDate);
        body.put("vnp_IpAddr", ipAddr);
        body.put("vnp_SecureHash", secureHash);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JsonNode response;
        try {
            response = restTemplate.postForObject(
                    vnPayConfig.getApiUrl(),
                    new HttpEntity<>(body, headers),
                    JsonNode.class
            );
        } catch (Exception e) {
            log.error("VNPay querydr call failed: vnpTxnRef={}", vnpTxnRef, e);
            return Optional.empty();
        }

        if (response == null) {
            log.warn("VNPay querydr empty response: vnpTxnRef={}", vnpTxnRef);
            return Optional.empty();
        }

        String responseCode = textOrNull(response, "vnp_ResponseCode");
        if (!"00".equals(responseCode)) {
            log.warn("VNPay querydr not OK: vnpTxnRef={}, responseCode={}, message={}",
                    vnpTxnRef, responseCode, textOrNull(response, "vnp_Message"));
            return Optional.empty();
        }

        String returnedTxnRef = textOrNull(response, "vnp_TxnRef");
        if (!vnpTxnRef.equals(returnedTxnRef)) {
            log.error("VNPay querydr txnRef mismatch: sent={}, received={}", vnpTxnRef, returnedTxnRef);
            return Optional.empty();
        }

        VNPayCallbackDTO dto = VNPayCallbackDTO.builder()
                .vnpTmnCode(textOrNull(response, "vnp_TmnCode"))
                .vnpAmount(textOrNull(response, "vnp_Amount"))
                .vnpBankCode(textOrNull(response, "vnp_BankCode"))
                .vnpBankTranNo(textOrNull(response, "vnp_BankTranNo"))
                .vnpCardType(textOrNull(response, "vnp_CardType"))
                .vnpPayDate(textOrNull(response, "vnp_PayDate"))
                .vnpOrderInfo(textOrNull(response, "vnp_OrderInfo"))
                .vnpTransactionNo(textOrNull(response, "vnp_TransactionNo"))
                .vnpResponseCode(textOrNull(response, "vnp_TransactionStatus"))
                .vnpTransactionStatus(textOrNull(response, "vnp_TransactionStatus"))
                .vnpTxnRef(returnedTxnRef)
                .build();

        log.info("VNPay querydr success: vnpTxnRef={}, transactionStatus={}",
                vnpTxnRef, dto.getVnpTransactionStatus());
        return Optional.of(dto);
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
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