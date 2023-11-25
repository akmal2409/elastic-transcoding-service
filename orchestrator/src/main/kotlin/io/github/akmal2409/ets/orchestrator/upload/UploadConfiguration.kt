package io.github.akmal2409.ets.orchestrator.upload

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.time.Duration

@ConfigurationProperties(prefix = "app.upload")
data class UploadConfigurationProperties(
    /**
     * Duration within which the pre-signed aws s3 url
     * will be valid for upload.
     * Default: 30 minutes
     */
    val preSignedUploadUrlValidity: Duration = Duration.ofMinutes(30),
    /**
     * Bucket, where the raw media from the users will be uploaded
     * Default: raw
     */
    val mediaBucket: String = "raw"
)

@Configuration
@EnableConfigurationProperties(UploadConfigurationProperties::class)
class UploadConfiguration {
}
