package io.github.akmal2409.ets.orchestrator.onboarding.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.akmal2409.ets.orchestrator.config.MessagingProperties
import io.github.akmal2409.ets.orchestrator.onboarding.controller.dto.unboxing.UnboxingCompletedEvent
import io.github.akmal2409.ets.orchestrator.onboarding.domain.NonRecoverableRawFileException
import io.github.akmal2409.ets.orchestrator.onboarding.domain.RawFileCreateRequest
import io.github.akmal2409.ets.orchestrator.onboarding.domain.RecoverableRawFileException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@JsonIgnoreProperties(ignoreUnknown = true)
data class UploadEvent(
    @JsonProperty("EventName") val type: String,
    @JsonProperty("Key") val fullKey: String // including path
) {

    val objectKey: String
        get() {
            val lastSlashIdx = fullKey.lastIndexOf('/')
            require(lastSlashIdx != -1 || lastSlashIdx == fullKey.length - 1) {
                "Invalid object key supplied. Expected separator / Received: $fullKey"
            }

            return fullKey.substring(lastSlashIdx + 1)
        }

}

// When recoverable exception occurs we can re-queue the event back and
// retry up until certain threshold
const val RETRY_COUNT_HEADER_KEY = "X-RETRY-COUNT"
const val MAX_RETRY_COUNT = 3

/**
 * Listener class that reacts to newly uploaded files to S3 bucket
 */
@Component
data class RawFileUploadListener(
    val rawMediaService: RawMediaService,
    val messagingProperties: MessagingProperties,
    val rabbitTemplate: RabbitTemplate
) {

    @RabbitListener(
        queues = ["\${app.messaging.rabbit-queues.raw-file-uploaded}", "\${app.messaging.rabbit-queues.raw-file-uploaded-retry}"],
        messageConverter = "jsonMessageConverter"
    )
    fun listenToRawFileUploaded(
        msg: UploadEvent,
        @Header(RETRY_COUNT_HEADER_KEY) retryCount: Int?
    ) {

        if ((retryCount ?: 0) > MAX_RETRY_COUNT) {
            logger.error { "message=Dropping raw file onboarding. Max retries exceeded;key=${msg.objectKey};service=rabbitmq" }
            rejectToDlq(msg, retryCount ?: 0)
            return
        }

        logger.info { "message=Received upload event for key ${msg.fullKey};service=rabbitmq" }

        val rawFileRequest = try {
            RawFileCreateRequest.fromObjectKey(msg.objectKey)
        } catch (ex: IllegalArgumentException) {
            null
        }

        rawFileRequest?.let {

            try {
                rawMediaService.onboardRawFile(it)
            } catch (nonRecoverable: NonRecoverableRawFileException) {
                logger.error(nonRecoverable) { "message=Cannot onboard new file due to client/system exception" }
                rejectToDlq(msg, retryCount ?: 0)
            } catch (recoverable: RecoverableRawFileException) {
                val nextRetry = (retryCount ?: 0) + 1
                logger.error(recoverable) { "message=Recoverable exception occurred. Going to retry $nextRetry" }
                rabbitTemplate.convertAndSend(
                    messagingProperties.rabbitQueues.rawFileUploadedRetry,
                    msg
                ) { processor ->
                    processor.messageProperties.headers[RETRY_COUNT_HEADER_KEY] = nextRetry
                    processor
                }
                logger.debug { "message=Sent retry for raw file ${msg.objectKey}" }
            }
        } ?: rejectToDlq(msg, retryCount ?: 0)
    }

    @RabbitListener(
        queues = ["\${app.messaging.rabbit-queues.raw-file-uploaded-dlq}"],
        messageConverter = "jsonMessageConverter"
    )
    fun listenToDql(msg: UploadEvent) {
        logger.error { "message=Received DLQ message $msg;service=rabbitmq" }
    }


    @RabbitListener(
        queues = ["\${app.messaging.rabbit-queues.media-unboxing-job-completion-queue}"],
        messageConverter = "jsonMessageConverter"
    ) // TODO: Better error handling
    fun listenToUnboxingCompletion(event: UnboxingCompletedEvent) = rawMediaService.onUnboxingComplete(event)

    private fun rejectToDlq(event: UploadEvent, retryCount: Int) =
        this.rabbitTemplate.convertAndSend(
            messagingProperties.rabbitQueues.rawFileUploadedDlq,
            event
        ) {
            it.messageProperties.headers[RETRY_COUNT_HEADER_KEY] = retryCount
            it
        }
}
