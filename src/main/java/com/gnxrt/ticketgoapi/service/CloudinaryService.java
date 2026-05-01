package com.gnxrt.ticketgoapi.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import com.gnxrt.ticketgoapi.config.CloudinaryConfig;
import com.gnxrt.ticketgoapi.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;
    private final CloudinaryConfig cloudinaryConfig;

    public String uploadAvatar(MultipartFile file, Long userId) {
        String publicId = "user_" + userId;
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", cloudinaryConfig.getFolder(),
                            "public_id", publicId,
                            "overwrite", true,
                            "resource_type", "image",
                            "transformation", new Transformation<>()
                                    .width(400).height(400)
                                    .crop("fill").gravity("face")
                                    .fetchFormat("auto").quality("auto")
                    )
            );
            String secureUrl = (String) result.get("secure_url");
            log.info("Uploaded avatar for user {}: {}", userId, secureUrl);
            return secureUrl;
        } catch (IOException e) {
            log.error("Failed to upload avatar for user {}: {}", userId, e.getMessage(), e);
            throw new BadRequestException("Không thể upload ảnh, vui lòng thử lại");
        }
    }

    public String uploadQRCode(byte[] qrBytes, String ticketCode) {
        String publicId = "ticket_" + ticketCode;
        try {
            Map<?, ?> result = cloudinary.uploader().upload(
                    qrBytes,
                    ObjectUtils.asMap(
                            "folder", "ticketgo/qrcodes",
                            "public_id", publicId,
                            "overwrite", true,
                            "resource_type", "image",
                            "format", "png"
                    )
            );
            String secureUrl = (String) result.get("secure_url");
            log.info("Uploaded QR code for ticket {}: {}", ticketCode, secureUrl);
            return secureUrl;
        } catch (IOException e) {
            log.error("Failed to upload QR code for ticket {}: {}", ticketCode, e.getMessage(), e);
            throw new BadRequestException("Không thể upload QR code, vui lòng thử lại");
        }
    }

    public void deleteByPublicId(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "image"));
            log.info("Deleted Cloudinary asset: {}", publicId);
        } catch (IOException e) {
            log.warn("Failed to delete Cloudinary asset {}: {}", publicId, e.getMessage());
        }
    }
}
