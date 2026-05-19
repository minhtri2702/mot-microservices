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
     * Get a presigned URL for direct access to an object in MinIO.
     * The URL is valid for the specified duration (default 1 hour).
     * Frontend can use this URL to load images directly from MinIO.
     */
    public String getPresignedUrl(String objectPath) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectPath)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Error generating presigned URL for {}: {}", objectPath, e.getMessage());
            // Fallback to public URL
            return getPublicUrl(objectPath);
        }
    }

    /**
     * Build public MinIO URL for an object.
     * e.g. http://100.94.58.103:9000/manga-images/one-piece/one-piece.jpg
     */
    public String getPublicUrl(String objectPath) {
        return String.format("%s/%s/%s", endpoint, bucket, objectPath);
    }
}
