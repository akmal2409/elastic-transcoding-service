package io.github.akmal2409.ets.orchestrator.aws

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@ConfigurationProperties(prefix = "app.aws")
internal class AwsConfigurationProps(
    regionName: String?,
    val s3Endpoint: URI
) {

    val region = if (regionName != null) Region.of(regionName) else Region.US_EAST_1
}

@Configuration
@EnableConfigurationProperties(AwsConfigurationProps::class)
internal class Configuration(
    private val properties: AwsConfigurationProps
) {

    @Bean
    fun credentialsProvider(): AwsCredentialsProvider =
        EnvironmentVariableCredentialsProvider.create()


    @Bean
    fun s3Client(credentialProvider: AwsCredentialsProvider): S3Client = S3Client.builder()
        .credentialsProvider(credentialProvider)
        .region(properties.region)
        .endpointOverride(properties.s3Endpoint)
        .forcePathStyle(true)
        .build()

    @Bean
    fun s3PreSigner(credentialProvider: AwsCredentialsProvider): S3Presigner = S3Presigner.builder()
        .endpointOverride(properties.s3Endpoint)
        .credentialsProvider(credentialProvider)
        .serviceConfiguration(
            S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build())
        .build();
}
