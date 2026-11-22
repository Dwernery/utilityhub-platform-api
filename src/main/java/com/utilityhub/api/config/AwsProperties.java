package com.utilityhub.api.config;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.validation.annotation.Validated;

/**
 * Typed view of the {@code aws.*} properties. Spring binds and validates these at
 * startup, so a missing bucket name stops the app immediately with a clear
 * message instead of failing on the first upload.
 *
 * @param profileName optional named profile from ~/.aws/credentials (local dev only)
 */
@Validated
@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
        @NotBlank String region,
        String profileName,
        @Valid @NotNull S3 s3) {

    /**
     * @param bucketName Library covers and files
     * @param mcuBucketName MCU posters, kept apart from the Library
     * @param presignedUrlExpiration plain numbers are minutes; "30s" or "1h" also work
     */
    public record S3(
            @NotBlank String bucketName,
            @NotBlank String mcuBucketName,
            @NotNull @DurationUnit(ChronoUnit.MINUTES) Duration presignedUrlExpiration) {}
}
