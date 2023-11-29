package io.github.akmal2409.ets.orchestrator.onboarding.service

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.rabbit.annotation.RabbitListener
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
                "Invalid object key supplied. Expected separator / Received: ${fullKey}"
            }

            return fullKey.substring(lastSlashIdx + 1)
        }

}

/**
 * Listener class that reacts to newly uploaded files to S3 bucket
 */
@Component
data class RawFileUploadListener(
    val rawMediaService: RawMediaService
) {

    @RabbitListener(
        queues = ["\${app.messaging.rabbit-queues.raw-file-uploaded}"],
        messageConverter = "jsonMessageConverter"
    )
    fun listenToRawFileUploaded(msg: UploadEvent) {
        logger.info { "message=Received upload event for key ${msg.fullKey};service=rabbitmq" }

        rawMediaService.saveNewRawFile(msg.objectKey)
    }
}
