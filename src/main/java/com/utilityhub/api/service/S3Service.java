package com.utilityhub.api.service;

import org.springframework.stereotype.Service;

import com.utilityhub.api.config.AwsProperties;

import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class S3Service {

    private final S3Presigner s3Presigner;
    private final AwsProperties aws;

    public S3Service(S3Presigner s3Presigner, AwsProperties aws) {
        this.s3Presigner = s3Presigner;
        this.aws = aws;
    }

    /** A short-lived URL the browser can PUT the file to directly, so uploads never pass through this API. */
    public S3PresignedUrlResponse generatePresignedUploadUrl(String fileName, String fileType) {
        String s3Key = fileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(aws.s3().bucketName())
                .key(s3Key)
                .contentType(fileType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(aws.s3().presignedUrlExpiration())
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
        return new S3PresignedUrlResponse(presigned.url().toString(), s3Key);
    }

    /** The object's permanent (non-presigned) URL. */
    public String getPublicS3Url(String s3Key) {
        return "https://%s.s3.%s.amazonaws.com/%s".formatted(aws.s3().bucketName(), aws.region(), s3Key);
    }

    public record S3PresignedUrlResponse(String presignedUrl, String s3Key) {}
}
