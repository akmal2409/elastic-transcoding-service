package io.github.akmal2409.ets.orchestrator.upload

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URL
import java.time.Clock
import java.time.Duration
import java.time.Instant

@ExtendWith(MockKExtension::class)
class UploadServiceTest {

    @MockK
    lateinit var presigner: S3Presigner

    @MockK
    lateinit var clock: Clock

    @SpyK
    var configuration = UploadConfigurationProperties(
        Duration.ofHours(1), mediaBucket = "testBucket"
    )

    @InjectMockKs
    lateinit var uploadService: UploadService

    @Test
    fun `Fails when content length is negative`() {
        val uploadRequest = UploadRequest("test.mkv",
            -1, "video/mp4")

        assertThatThrownBy {
            uploadService.generatePresignedUrl(uploadRequest)
        }.isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("contentLength")
    }

    @Test
    fun `Fails when mime type is invalid`() {
        val request = UploadRequest("test.mkv", 10, "something")

        assertThatThrownBy { uploadService.generatePresignedUrl(request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("contentType")
            .hasMessageContaining("type/*")
    }

    @Test
    fun `Fails when mime type is not a video`() {
        val request = UploadRequest("test.mkv", 10, "audio/mp3")

        assertThatThrownBy { uploadService.generatePresignedUrl(request) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("contentType")
            .hasMessageContaining("video")
    }

    @Test
    fun `Generates valid presigned URL with validity`(@MockK presignedRequest: PresignedPutObjectRequest) {
        val generatedAt = Instant.EPOCH
        val expectedValidTo = generatedAt.plus(configuration.preSignedUploadUrlValidity)
        val request = UploadRequest("test.mkv", 10, "video/x-matroska")
        val url = URL("https://test.com/some-data")
        val expectedPresignedUrl =
            PresignedUploadUrl(url, expectedValidTo)

        every { presigner.presignPutObject(any<PutObjectPresignRequest>()) } returns presignedRequest
        every { presignedRequest.url() } returns url
        every { clock.instant() } returns generatedAt

        val actualPresignedUrl = uploadService.generatePresignedUrl(request)

        assertThat(actualPresignedUrl)
            .usingRecursiveComparison()
            .isEqualTo(expectedPresignedUrl)
    }
}
