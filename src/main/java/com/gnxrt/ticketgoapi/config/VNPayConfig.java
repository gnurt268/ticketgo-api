package com.gnxrt.ticketgoapi.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VNPayConfig {

    /**
     *
     */
    private String tmnCode;

    /**
     *
     */
    private String hashSecret;

    /**
     * - Sandbox: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html
     * - Production: https://pay.vnpay.vn/vpcpay.html
     */
    private String payUrl;

    /**
     *
     */
    private String apiUrl;

    /**
     *
     */
    private String returnUrl;

    /**
     *
     */
    private String ipnUrl;

    /**
     *
     */
    private String version = "2.1.0";

    /**
     *
     */
    private String command = "pay";

    /**
     *
     */
    private String orderType = "other";

    /**
     *
     */
    private String currencyCode = "VND";

    /**
     *
     */
    private String locale = "vn";
}