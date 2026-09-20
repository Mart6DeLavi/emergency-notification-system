package com.sensa.filesystemservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client

@Configuration
class S3Config {

    @Value("\${aws.access-key-id:}")
    private lateinit var accessKeyId: String

    @Value("\${aws.secret-access-key:}")
    private lateinit var secretAccessKey: String

    @Value("\${aws.region:eu-west-1}")
    private lateinit var region: String

    @Bean
    fun s3Client(): S3Client {
        val builder = S3Client.builder().region(Region.of(region))

        if (accessKeyId.isNotBlank() && secretAccessKey.isNotBlank()) {
            val credentials = AwsBasicCredentials.create(accessKeyId, secretAccessKey)
            builder.credentialsProvider(StaticCredentialsProvider.create(credentials))
        }

        return builder.build()
    }
}
