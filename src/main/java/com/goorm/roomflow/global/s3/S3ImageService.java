package com.goorm.roomflow.global.s3;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3ImageService {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.region.static}")
    private String region;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    public String upload(MultipartFile file, String keyPrefix) {
        log.info("S3 업로드 시작 - originalFilename={}, size={}, prefix={}",
                file.getOriginalFilename(),
                file.getSize(),
                keyPrefix
        );

        validateFile(file);

        String extension = extractExtension(file.getOriginalFilename());
        String key = keyPrefix + "/" + UUID.randomUUID() + "." + extension;

        log.info("S3 업로드 key 생성 완료 - key={}", key);

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            log.info("S3 putObject 요청 시작 - bucket={}, key={}", bucket, key);

            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromBytes(file.getBytes())
            );

            String imageUrl =
                    "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;

            log.info("S3 업로드 완료 - imageUrl={}", imageUrl);

            return imageUrl;

        } catch (IOException e) {
            log.error("S3 업로드 실패 - filename={}, error={}",
                    file.getOriginalFilename(),
                    e.getMessage(),
                    e
            );

            throw new IllegalStateException("이미지 업로드에 실패했습니다.", e);
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.warn("S3 업로드 실패 - 파일 없음");
            throw new IllegalArgumentException("업로드할 이미지 파일이 없습니다.");
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("허용되지 않은 확장자 업로드 시도 - extension={}", extension);
            throw new IllegalArgumentException("jpg, jpeg, png, webp 형식만 업로드할 수 있습니다.");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            log.warn("이미지 타입 아님 - contentType={}", contentType);
            throw new IllegalArgumentException("이미지 파일만 업로드할 수 있습니다.");
        }

        log.info("파일 검증 완료 - extension={}, contentType={}",
                extension,
                contentType
        );
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            log.warn("확장자 추출 실패 - filename={}", fileName);
            throw new IllegalArgumentException("파일 확장자를 확인할 수 없습니다.");
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
