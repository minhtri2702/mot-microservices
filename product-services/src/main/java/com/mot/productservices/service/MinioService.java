package com.mot.productservices.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    /**
     * Get an object from MinIO as InputStream.
     * Object path format: "slug/slug.jpg" (e.g. "one-piece/one-piece.jpg")
     */
    public InputStream getObject(String objectPath) {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectPath)
                            .build()
            );
        } catch (MinioException e) {
            log.error("MinIO error fetching {}: {}", objectPath, e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Error fetching {} from MinIO: {}", objectPath, e.getMessage());
            return null;
        }
    }

    /**
     * Get a URL for an image via the internal image proxy.
     * Images are served through Nginx → product-services → MinIO.
     * This keeps MinIO internal and avoids exposing internal hostnames.
     * The proxy URL is deterministic, so caching it in application memory
     * would only create an unbounded map without saving any MinIO request.
     */
    public String getPresignedUrl(String objectPath) {
        if (objectPath == null) return null;

        return getPublicUrl(objectPath);
    }

    /**
     * Build public URL for an object via image proxy (through Nginx).
     * Frontend calls: /api/v1/images/{objectPath}
     * This keeps MinIO internal and serves images through the API proxy.
     */
    public String getPublicUrl(String objectPath) {
        return String.format("/api/v1/images/%s", objectPath);
    }
}
