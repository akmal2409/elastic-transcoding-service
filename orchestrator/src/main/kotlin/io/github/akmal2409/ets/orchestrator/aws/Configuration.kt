package io.github.akmal2409.ets.orchestrator.aws

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.*
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

@ConfigurationProperties(prefix = "aws.credentials")
internal data class AwsCredentialsConfigProperties(
    val accessKeyId: String?,
    val secretAccessKey: String?
)

internal class AwsSpringPropertiesCredentialsProvider(
    private val accessKeyId: String,
    private val secretAccessKey: String
) : AwsCredentialsProvider {
    override fun resolveCredentials(): AwsCredentials {
        return AwsBasicCredentials.create(
            accessKeyId, secretAccessKey
        )
    }
}

@ConfigurationProperties(prefix = "app.aws")
internal class AwsConfigurationProps(
    regionName: String?,
    val s3Endpoint: URI
) {

    val region = if (regionName != null) Region.of(regionName) else Region.US_EAST_1
}

@Configuration
@EnableConfigurationProperties(value = [AwsConfigurationProps::class, AwsCredentialsConfigProperties::class])
internal class Configuration(
    private val properties: AwsConfigurationProps,
    private val awsCredentials: AwsCredentialsConfigProperties
) {

    @Bean
    fun credentialsProvider(): AwsCredentialsProvider {
        val providers: MutableList<AwsCredentialsProvider> =
            mutableListOf(EnvironmentVariableCredentialsProvider.create())

        if (awsCredentials.accessKeyId !== null && awsCredentials.secretAccessKey !== null) {
            providers.add(
                AwsSpringPropertiesCredentialsProvider(
                    awsCredentials.accessKeyId,
                    awsCredentials.secretAccessKey
                )
            )
        }

        val builder = AwsCredentialsProviderChain.builder()
        providers.forEach(builder::addCredentialsProvider)

        return builder.build()
    }

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
                .build()
        )
        .build();
}
