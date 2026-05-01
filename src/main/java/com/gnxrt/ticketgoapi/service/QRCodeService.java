package com.gnxrt.ticketgoapi.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class QRCodeService {

    private final CloudinaryService cloudinaryService;

    @Value("${app.qrcode.width:300}")
    private int qrCodeWidth;

    @Value("${app.qrcode.height:300}")
    private int qrCodeHeight;

    @Value("${app.qrcode.secret-key}")
    private String qrSecretKey;

    public String generateQRCodeBase64(String content) {
        try {
            byte[] qrCodeBytes = generateQRCodeBytes(content);
            return Base64.getEncoder().encodeToString(qrCodeBytes);
        } catch (Exception e) {
            log.error("Error generating QR code Base64 for content: {}", content, e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    public byte[] generateQRCodeBytes(String content) {
        try {
            BitMatrix bitMatrix = createBitMatrix(content);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", outputStream);

            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("Error generating QR code bytes for content: {}", content, e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    public byte[] generateQRCodeWithLogo(String content, byte[] logoBytes) {
        try {
            BitMatrix bitMatrix = createBitMatrix(content);
            BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

            if (logoBytes != null && logoBytes.length > 0) {
                BufferedImage logoImage = ImageIO.read(new java.io.ByteArrayInputStream(logoBytes));
                qrImage = addLogoToQRCode(qrImage, logoImage);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(qrImage, "PNG", outputStream);

            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("Error generating QR code with logo for content: {}", content, e);
            throw new RuntimeException("Failed to generate QR code with logo", e);
        }
    }

    public String generateTicketQRCodeUrl(String ticketCode, String qrCode) {
        String content = buildTicketQRContent(ticketCode, qrCode);
        byte[] qrBytes = generateQRCodeBytes(content);
        return cloudinaryService.uploadQRCode(qrBytes, ticketCode);
    }

    public String generateTicketQRCodeBase64(String ticketCode, String qrCode) {
        String content = buildTicketQRContent(ticketCode, qrCode);
        return "data:image/png;base64," + generateQRCodeBase64(content);
    }

    /**
     * Build QR content with HMAC signature for security
     * Format: ticketCode|qrSecret|timestamp|signature
     */
    private String buildTicketQRContent(String ticketCode, String qrCode) {
        long timestamp = System.currentTimeMillis();
        String data = String.format("%s|%s|%d", ticketCode, qrCode, timestamp);
        String signature = generateHMAC(data);
        return String.format("%s|%s", data, signature);
    }

    /**
     * Generate HMAC-SHA256 signature
     */
    private String generateHMAC(String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    qrSecretKey.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // Return first 16 chars of hex for shorter QR
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(8, hmacBytes.length); i++) {
                hexString.append(String.format("%02x", hmacBytes[i]));
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Error generating HMAC", e);
            throw new RuntimeException("Failed to generate signature", e);
        }
    }

    /**
     * Verify HMAC signature
     */
    private boolean verifyHMAC(String data, String signature) {
        String expectedSignature = generateHMAC(data);
        return expectedSignature.equals(signature);
    }

    /**
     * Validate QR code with HMAC signature verification
     */
    public QRCodeValidationResult validateQRCode(String qrContent) {
        try {
            if (qrContent == null || qrContent.isEmpty()) {
                return QRCodeValidationResult.invalid("QR code content is empty");
            }

            String[] parts = qrContent.split("\\|");
            if (parts.length < 4) {
                // Legacy format without signature (backward compatible)
                if (parts.length >= 2) {
                    log.warn("QR code without signature detected: {}", parts[0]);
                    return QRCodeValidationResult.valid(parts[0], parts[1]);
                }
                return QRCodeValidationResult.invalid("Invalid QR code format");
            }

            String ticketCode = parts[0];
            String qrCode = parts[1];
            String timestamp = parts[2];
            String signature = parts[3];

            // Verify signature
            String data = String.format("%s|%s|%s", ticketCode, qrCode, timestamp);
            if (!verifyHMAC(data, signature)) {
                log.warn("Invalid QR signature for ticket: {}", ticketCode);
                return QRCodeValidationResult.invalid("Invalid QR code signature - possible forgery");
            }

            return QRCodeValidationResult.valid(ticketCode, qrCode);
        } catch (Exception e) {
            log.error("Error validating QR code: {}", qrContent, e);
            return QRCodeValidationResult.invalid("Error parsing QR code");
        }
    }

    private BitMatrix createBitMatrix(String content) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        return qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, qrCodeWidth, qrCodeHeight, hints);
    }

    private BufferedImage addLogoToQRCode(BufferedImage qrImage, BufferedImage logoImage) {
        int qrWidth = qrImage.getWidth();
        int qrHeight = qrImage.getHeight();

        int logoWidth = qrWidth / 5;
        int logoHeight = qrHeight / 5;

        Image scaledLogo = logoImage.getScaledInstance(logoWidth, logoHeight, Image.SCALE_SMOOTH);

        int logoX = (qrWidth - logoWidth) / 2;
        int logoY = (qrHeight - logoHeight) / 2;

        BufferedImage combined = new BufferedImage(qrWidth, qrHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = combined.createGraphics();

        g.drawImage(qrImage, 0, 0, null);

        g.setColor(Color.WHITE);
        int padding = 5;
        g.fillRoundRect(logoX - padding, logoY - padding,
                logoWidth + padding * 2, logoHeight + padding * 2, 10, 10);

        g.drawImage(scaledLogo, logoX, logoY, null);
        g.dispose();

        return combined;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class QRCodeValidationResult {
        private boolean valid;
        private String ticketCode;
        private String qrCode;
        private String errorMessage;

        public static QRCodeValidationResult valid(String ticketCode, String qrCode) {
            return QRCodeValidationResult.builder()
                    .valid(true)
                    .ticketCode(ticketCode)
                    .qrCode(qrCode)
                    .build();
        }

        public static QRCodeValidationResult invalid(String errorMessage) {
            return QRCodeValidationResult.builder()
                    .valid(false)
                    .errorMessage(errorMessage)
                    .build();
        }
    }
}