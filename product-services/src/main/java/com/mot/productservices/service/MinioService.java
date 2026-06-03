package com.mot.productservices.service;

import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    @Value("${minio.endpoint}")
    private String endpoint;

    // Cache presigned URLs for 50 minutes (URLs expire in 1 hour)
    private final ConcurrentHashMap<String, CacheEntry> presignedUrlCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 50 * 60 * 1000L;

    private static class CacheEntry {
        final String url;
        final long createdAt;

        CacheEntry(String url) {
            this.url = url;
            this.createdAt = System.currentTimeMillis();
        }

        boolean isValid() {
            return System.currentTimeMillis() - createdAt < CACHE_DURATION_MS;
        }
    }

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
     * Results are cached for 50 minutes.
     */
    public String getPresignedUrl(String objectPath) {
        if (objectPath == null) return null;

        // Check cache first
        CacheEntry cached = presignedUrlCache.get(objectPath);
        if (cached != null && cached.isValid()) {
            return cached.url;
        }

        // Use image proxy URL instead of presigned MinIO URL
        // This keeps MinIO internal and serves images through the API
        String url = getPublicUrl(objectPath);
        presignedUrlCache.put(objectPath, new CacheEntry(url));
        return url;
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
