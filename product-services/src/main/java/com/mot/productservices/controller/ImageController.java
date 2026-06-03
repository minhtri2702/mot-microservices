package com.mot.productservices.controller;

import com.mot.productservices.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.io.InputStream;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Slf4j
public class ImageController {

    private final MinioService minioService;

    /**
     * Proxy image from MinIO.
     * Frontend calls: GET /api/v1/images/{objectPath}
     * e.g. GET /api/v1/images/one-piece/one-piece.jpg
     * 
     * Supports both:
     *   - /api/v1/images/slug/filename.jpg  (2 segments)
     *   - /api/v1/images/path/to/image.jpg  (multiple segments)
     */
    @GetMapping("/**")
    public ResponseEntity<byte[]> getImage(HttpServletRequest request) {
        // Extract path after /api/v1/images/
        String requestPath = request.getRequestURI();
        String prefix = "/api/v1/images/";
        String objectPath = requestPath.substring(requestPath.indexOf(prefix) + prefix.length());

        if (objectPath.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        InputStream inputStream = minioService.getObject(objectPath);

        if (inputStream == null) {
            log.warn("Image not found in MinIO: {}", objectPath);
            return ResponseEntity.notFound().build();
        }

        try {
            byte[] bytes = inputStream.readAllBytes();
            String contentType = getContentType(objectPath);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, contentType)
                    .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                    .body(bytes);
        } catch (Exception e) {
            log.error("Error reading image {} from MinIO: {}", objectPath, e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }

    private String getContentType(String filename) {
        if (filename == null) return MediaType.APPLICATION_OCTET_STREAM_VALUE;

        String lower = filename.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".avif")) return "image/avif";

        return MediaType.APPLICATION_OCTET_STREAM_VALUE;
    }
}
