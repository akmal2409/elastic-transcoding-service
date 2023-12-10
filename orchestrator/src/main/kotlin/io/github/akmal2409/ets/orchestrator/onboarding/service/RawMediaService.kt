package io.github.akmal2409.ets.orchestrator.onboarding.service

import io.github.akmal2409.ets.orchestrator.commons.db.OptimisticLockingException
import io.github.akmal2409.ets.orchestrator.config.MessagingProperties
import io.github.akmal2409.ets.orchestrator.onboarding.domain.*
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.amqp.AmqpException
import org.springframework.amqp.AmqpIOException
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.dao.DataAccessException
import org.springframework.dao.NonTransientDataAccessException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.util.*

private val logger = KotlinLogging.logger {}

@Service
data class RawMediaService(
    val rawMediaRepository: RawMediaRepository,
    val rabbitTemplate: RabbitTemplate,
    val messagingProperties: MessagingProperties,
    val unboxingJobRepository: UnboxingJobRepository,
    val clock: Clock
) {

    /**
     * When raw file is uploaded to a special bucket in S3 with a key
     * we need to persist it in the database and automatically trigger unboxing
     * job.
     */
    @Transactional
    fun onboardRawFile(rawFileRequest: RawFileCreateRequest): RawMedia {
        val rawMedia = try {
            saveNewRawFile(rawFileRequest)
        } catch (nonRecoverableDataEx: NonTransientDataAccessException) {
            throw InvalidRawFileException(
                rawFileRequest.mediaKey,
                "Invalid parameters",
                nonRecoverableDataEx
            )
        } catch (recoverableDataEx: DataAccessException) {
            throw RawFileOnboardingException(
                rawFileRequest.mediaKey,
                "Exception when saving",
                recoverableDataEx
            )
        }

        beginUnboxingJob(rawMedia)

        return rawMedia
    }

    private fun beginUnboxingJob(rawMedia: RawMedia): UnboxingJob {
        val (unboxingJob, beginUnboxingEvent) = try {
            rawMedia.beginUnboxingJob(clock = clock)
        } catch (stateEx: IllegalStateException) {
            throw AlreadyUnboxedRawFileException(rawMedia.key)
        }

        try {
            unboxingJobRepository.insert(unboxingJob)
        } catch (nonRecoverableDataEx: NonTransientDataAccessException) {
            throw InvalidRawFileException(
                rawMedia.key,
                "Invalid parameters for unboxing job",
                nonRecoverableDataEx
            )
        } catch (recoverableDataEx: DataAccessException) {
            throw RawFileOnboardingException(
                rawMedia.key,
                "Exception when saving unboxing job",
                recoverableDataEx
            )
        }

        // TODO: Add retries with resilience4j a circuit breaker
        // TODO: whatever fails, needs to go to a retry queue (add header with Retry-Attempt: n and drop if exceeds retries)
        try {
            rabbitTemplate.convertAndSend(
                messagingProperties.rabbitQueues.mediaUnboxingQueue,
                beginUnboxingEvent
            )
        } catch (ioEx: AmqpIOException) {
            throw UnboxingJobFailedStartException(
                rawMedia.key,
                "Could not publish event due to IO exception",
                ioEx
            )
        } catch (amqpEx: AmqpException) {
            throw NonRecoverableJobStartException(rawMedia.key, "Cannot start due to misconfiguration", amqpEx)
        }

        logger.debug { "message=Triggered unboxing job;service=media-unboxer;jobId=${unboxingJob.id};key=${rawMedia.key}" }

        return unboxingJob
    }

    private fun saveNewRawFile(rawFileRequest: RawFileCreateRequest): RawMedia {
        val media = RawMedia(rawFileRequest.mediaKey, false, 0)

        rawMediaRepository.insert(media)

        logger.debug { "message=Saved new raw media ${rawFileRequest.mediaKey};service=postgres;key=${rawFileRequest.mediaKey}" }
        return media
    }

    fun findAll(page: Int, size: Int): Page<RawMedia> {
        return rawMediaRepository.findAll(PageRequest.of(page, size))
    }
}
