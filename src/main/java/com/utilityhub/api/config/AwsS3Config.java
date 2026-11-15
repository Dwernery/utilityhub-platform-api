package com.utilityhub.api.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class AwsS3Config {

    /**
     * Locally, a named profile from ~/.aws/credentials. Everywhere else, the default
     * chain (environment variables, then the EC2 instance role).
     */
    @Bean
    AwsCredentialsProvider awsCredentialsProvider(AwsProperties properties) {
        String profile = properties.profileName();
        return profile != null && !profile.isBlank()
                ? ProfileCredentialsProvider.create(profile)
                : DefaultCredentialsProvider.builder().build();
    }

    @Bean(destroyMethod = "close")
    S3Presigner s3Presigner(AwsProperties properties, AwsCredentialsProvider credentials) {
        return S3Presigner.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentials)
                .build();
    }
}
