package com.schoolms.file;

import com.schoolms.config.MinioProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.UUID;

/**
 * Thin wrapper around the MinIO (S3-compatible) client. Object keys are
 * tenant-scoped so uploads are naturally isolated per school.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService implements ApplicationRunner {

    private final MinioClient minioClient;
    private final MinioProperties properties;

    @Override
    public void run(ApplicationArguments args) {
        ensureBucket();
    }

    public void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.bucket()).build());
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(properties.bucket()).build());
                log.info("Created MinIO bucket '{}'", properties.bucket());
            }
        } catch (Exception e) {
            log.warn("MinIO bucket setup failed (is MinIO running?): {}", e.getMessage());
        }
    }

    /** Uploads a file under {@code <schoolId>/<folder>/<uuid>.<ext>}. */
    public String upload(UUID schoolId, String folder, MultipartFile file) {
        String extension = extractExtension(file.getOriginalFilename());
        String objectKey = schoolId + "/" + folder + "/" + UUID.randomUUID() + extension;
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(is, file.getSize(), -1)
                    .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                    .build());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read uploaded file", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to upload file to MinIO", e);
        }
        return objectKey;
    }

    /** Uploads raw bytes (e.g. a generated PDF) and returns the object key. */
    public String uploadBytes(UUID schoolId, String folder, String objectKey, byte[] content, String contentType) {
        try (InputStream is = new java.io.ByteArrayInputStream(content)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .stream(is, content.length, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to upload bytes to MinIO", e);
        }
        return objectKey;
    }

    /** Returns a presigned GET URL valid for 1 hour. */
    public String presignedUrl(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .method(Method.GET)
                    .expiry(60 * 60)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to generate presigned URL for {}: {}", objectKey, e.getMessage());
            return null;
        }
    }

    public void delete(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to delete object {}: {}", objectKey, e.getMessage());
        }
    }

    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }
}
