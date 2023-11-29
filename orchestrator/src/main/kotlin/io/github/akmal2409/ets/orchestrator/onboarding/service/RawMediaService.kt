package io.github.akmal2409.ets.orchestrator.onboarding.service

import io.github.akmal2409.ets.orchestrator.onboarding.domain.RawMedia
import io.github.akmal2409.ets.orchestrator.onboarding.domain.RawMediaKey
import io.github.akmal2409.ets.orchestrator.onboarding.domain.RawMediaRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}
@Service
data class RawMediaService(
    val rawMediaRepository: RawMediaRepository
) {

    fun saveNewRawFile(objectKey: String): RawMedia {
        val key = RawMediaKey.fromString(objectKey)
        val media = RawMedia(key, false, 0)

        rawMediaRepository.insert(media)

        logger.debug { "message=Saved new raw media ${key.id}-${key.name};service=postgres;key=${key.id}" }
        return media
    }
}
