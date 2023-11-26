package io.github.akmal2409.ets.orchestrator.upload

import io.github.akmal2409.ets.orchestrator.configureLocalstack
import io.github.akmal2409.ets.orchestrator.configurePostgres
import io.github.akmal2409.ets.orchestrator.createLocalStack
import io.github.akmal2409.ets.orchestrator.createPostgres
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.util.MultiValueMapAdapter
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import software.amazon.awssdk.services.s3.S3Client
import java.util.Map

const val TEST_BUCKET_NAME = "raw"

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration-test")
@Testcontainers
class UploadServiceIT {

    companion object {
        @Container
        val postgres = createPostgres()

        @Container
        val localstack = createLocalStack()
            .withServices(LocalStackContainer.Service.S3)

        @Bean
        @DynamicPropertySource
        @JvmStatic
        fun configure(registry: DynamicPropertyRegistry) {
            configurePostgres(postgres, registry)
            configureLocalstack(localstack, registry)
        }

        @JvmStatic
        @BeforeAll
        fun setup() {
            localstack.execInContainer("awslocal", "s3", "mb", "s3://$TEST_BUCKET_NAME")
        }
    }

    @TestConfiguration
    class Config {

        @Bean
        fun restTemplate(): RestTemplate = RestTemplateBuilder().build()
    }

    @Autowired
    lateinit var restTemplate: RestTemplate

    @LocalServerPort
    lateinit var port: String

    @Autowired
    lateinit var s3Client: S3Client

    @Test
    fun `Returns bad request 400 when invalid configuration supplied`() {
        val requestWithoutFileName = UploadRequest(
            null, -1, null
        )

        assertThatThrownBy {
            restTemplate.postForEntity(
                "http://localhost:$port$UPLOAD_BASE_API_PATH",
                requestWithoutFileName,
                PresignedUploadUrl::class.java
            )
        }
            .isInstanceOf(HttpClientErrorException.BadRequest::class.java)
            .hasMessageContaining("filename is empty")
            .hasMessageContaining("contentType is empty")
            .hasMessageContaining("contentLength")

    }

    @Test
    fun `Returns valid pre-signed upload URL`() {
        val request = UploadRequest("test.mkv", 100, "video/x-matroska")

        val response = restTemplate.postForEntity(
            "http://localhost:$port$UPLOAD_BASE_API_PATH",
            request, PresignedUploadUrl::class.java
        )

        assertThat(response).isNotNull
            .extracting(ResponseEntity<PresignedUploadUrl>::getStatusCode)
            .isEqualTo(HttpStatusCode.valueOf(201))

        val presignedUrl = response.body

        assertThat(presignedUrl)
            .isNotNull
            .extracting("url", "expiresAt")
            .doesNotContainNull()

        val data = ByteArray(request.contentLength!!.toInt()) { 127 }

        val requestEntity = HttpEntity<ByteArray>(
            data,
            MultiValueMapAdapter(
                Map.of(
                    HttpHeaders.CONTENT_TYPE, listOf(request.contentType!!),
                    HttpHeaders.CONTENT_LENGTH, listOf(request.contentLength!!.toString())
                )
            )
        )
        val s3Response = restTemplate.exchange(
            presignedUrl?.url?.toURI()!!, HttpMethod.PUT, requestEntity, Any::class.java
        )

        assertThat(s3Response.statusCode.is2xxSuccessful).isTrue()

        assertThatNoException().isThrownBy {
            s3Client.headObject {
                it.bucket(TEST_BUCKET_NAME)
                    .key(presignedUrl?.objectKey)
            }
        }

    }
}
