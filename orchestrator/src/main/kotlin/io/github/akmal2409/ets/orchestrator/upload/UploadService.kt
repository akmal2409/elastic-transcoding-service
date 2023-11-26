package io.github.akmal2409.ets.orchestrator.upload

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.InvalidMediaTypeException
import org.springframework.stereotype.Service
import org.springframework.util.MimeType
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.lang.RuntimeException
import java.net.URL
import java.time.Clock
import java.time.Instant
import java.util.*

private val allowedMediaMimeType = MimeType.valueOf("video/*")
private val logger = KotlinLogging.logger {}

@Service
data class UploadService(
    val s3Presigner: S3Presigner,
    val uploadConfigProperties: UploadConfigurationProperties,
    val clock: Clock
) {

    fun generatePresignedUrl(uploadRequest: UploadRequest): PresignedUploadUrl {
        require(uploadRequest.contentLength != null &&
                uploadRequest.contentLength > 0) {
            "Invalid contentLength supplied. Expected at least 1 byte. " +
                    "Received: ${uploadRequest.contentLength}"
        }

        val mimeType = try {
            uploadRequest.contentType?.let(MimeType::valueOf)
        } catch (e: RuntimeException) {
            throw IllegalArgumentException(
                "Invalid contentType supplied. Excepted matching type/*. Received: ${uploadRequest.contentType}",
            e)
        }

        require(allowedMediaMimeType.isCompatibleWith(mimeType)) {
            "Invalid contentType supplied. Excepted matching video/*. Received: ${uploadRequest.contentType}"
        }

        requireNotNull(uploadRequest.filename) { "filename must not be null" }

        val mediaKey = "${UUID.randomUUID()}_${uploadRequest.filename}"

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(uploadConfigProperties.mediaBucket)
            .contentType(mimeType.toString())
            .key(mediaKey)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(uploadConfigProperties.preSignedUploadUrlValidity)
            .putObjectRequest(putObjectRequest)
            .build()

        try {
            val presignedUrl: URL = s3Presigner.presignPutObject(presignRequest)
                .url()
            val validity = Instant.now(clock).plus(uploadConfigProperties.preSignedUploadUrlValidity)

            logger.debug { "message=Generated pre-signed url. Validity: $validity;service=s3;bucket=${uploadConfigProperties.mediaBucket};" +
                    "key=$mediaKey" }
            return PresignedUploadUrl(
                presignedUrl,
                validity,
                mediaKey
            )
        } catch (exception: S3Exception) {
            logger.error(exception) {
                "message=Presigned url generation failed;service=s3;bucket=${uploadConfigProperties.mediaBucket};" +
                        "key=$mediaKey"
            }
            throw PresignedUrlGenerationFailed(
                uploadConfigProperties.mediaBucket,
                mediaKey, exception
            )
        }
    }
}
