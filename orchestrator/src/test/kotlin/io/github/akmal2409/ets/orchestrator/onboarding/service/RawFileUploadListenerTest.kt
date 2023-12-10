package io.github.akmal2409.ets.orchestrator.onboarding.service

import io.github.akmal2409.ets.orchestrator.config.MessagingProperties
import io.github.akmal2409.ets.orchestrator.config.RabbitQueues
import io.github.akmal2409.ets.orchestrator.onboarding.domain.InvalidRawFileException
import io.github.akmal2409.ets.orchestrator.onboarding.domain.NonRecoverableRawFileException
import io.github.akmal2409.ets.orchestrator.onboarding.domain.RawFileCreateRequest
import io.github.akmal2409.ets.orchestrator.onboarding.domain.UnboxingJobFailedStartException
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.rabbit.core.RabbitTemplate
import java.util.UUID

private const val UPLOAD_EVENT_S3_NAME = "S3:ObjectPut"
private const val DLQ_QUEUE = "dlq"
private const val RETRY_QUEUE = "retry"

private fun randomValidUploadEvent() =
    UploadEvent(UPLOAD_EVENT_S3_NAME, "bucket/${UUID.randomUUID()}_name.txt")

@ExtendWith(MockKExtension::class)
class RawFileUploadListenerTest {

    @MockK
    lateinit var mediaService: RawMediaService

    @MockK
    lateinit var rabbitTemplate: RabbitTemplate

    @SpyK
    var messagingProps = MessagingProperties(
        RabbitQueues(
            "uploaded", RETRY_QUEUE, DLQ_QUEUE,
            "unboxing", "completed"
        )
    )

    @InjectMockKs
    lateinit var listener: RawFileUploadListener

    @Test
    fun `Rejects to DLQ when key is invalid`() {
        val malformedEvent = UploadEvent(UPLOAD_EVENT_S3_NAME, "just_bucket")
        assertRejectedToDlq(malformedEvent)
        confirmVerified(mediaService, rabbitTemplate)
    }

    @Test
    fun `Rejects to DLQ when max retries exceeded`() {
        val event = randomValidUploadEvent()
        val retryCount = MAX_RETRY_COUNT + 1
        assertRejectedToDlq(event, retryCount)
        confirmVerified(mediaService, rabbitTemplate)
    }

    @Test
    fun `Rejects to DLQ when non recoverable exception`() {
        val event = randomValidUploadEvent()
        val createRequest = RawFileCreateRequest.fromObjectKey(event.objectKey)

        every {
            mediaService.onboardRawFile(createRequest)
        } throws InvalidRawFileException(createRequest.mediaKey, "")

        assertRejectedToDlq(event)
        verify(exactly = 1) { mediaService.onboardRawFile(any()) }
        confirmVerified(mediaService, rabbitTemplate)
    }


    @Test
    fun `Will retry when recoverable exception thrown`() {
        val event = randomValidUploadEvent()
        val createRequest = RawFileCreateRequest.fromObjectKey(event.objectKey)

        every {
            mediaService.onboardRawFile(createRequest)
        } throws UnboxingJobFailedStartException(createRequest.mediaKey, "")

        every {
            rabbitTemplate.convertAndSend(
                RETRY_QUEUE,
                event, any<MessagePostProcessor>()
            )
        } answers {}

        listener.listenToRawFileUploaded(event, null)

        verify(exactly = 1) {
            mediaService.onboardRawFile(createRequest)
            rabbitTemplate.convertAndSend(
                RETRY_QUEUE,
                event, any<MessagePostProcessor>()
            )
        }

        confirmVerified(rabbitTemplate, mediaService)
    }

    private fun assertRejectedToDlq(event: UploadEvent, retries: Int? = null) {
        every {
            rabbitTemplate.convertAndSend(
                DLQ_QUEUE,
                event, any<MessagePostProcessor>()
            )
        } answers {}

        listener.listenToRawFileUploaded(event, retries)

        verify(exactly = 1) {
            rabbitTemplate.convertAndSend(
                DLQ_QUEUE,
                event, any<MessagePostProcessor>()
            )
        }
    }
}
