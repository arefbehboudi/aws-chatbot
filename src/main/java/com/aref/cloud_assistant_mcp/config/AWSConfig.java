package com.aref.cloud_assistant_mcp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

@Configuration
public class AWSConfig {

    @Bean
    public Ec2Client ec2Client() {
        Region region = Region.of(System.getProperty("AWS_REGION",
                System.getenv().getOrDefault("AWS_REGION", "us-east-1")));

        return Ec2Client.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .overrideConfiguration(ClientOverrideConfiguration.builder()
                        .apiCallAttemptTimeout(Duration.ofSeconds(30))
                        .apiCallTimeout(Duration.ofMinutes(2))
                        .build())
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.create();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.create();
    }
}
