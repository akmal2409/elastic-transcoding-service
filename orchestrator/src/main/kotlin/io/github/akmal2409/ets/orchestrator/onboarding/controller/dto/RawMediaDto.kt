package io.github.akmal2409.ets.orchestrator.onboarding.controller.dto

import io.github.akmal2409.ets.orchestrator.onboarding.domain.RawMedia
import java.util.UUID

data class RawMediaDto(
    val id: UUID,
    val name: String,
    val unboxed: Boolean
) {

    companion object {

        fun from(rawMedia: RawMedia): RawMediaDto {
            return RawMediaDto(
                rawMedia.key.id,
                rawMedia.key.name,
                rawMedia.unboxed
            )
        }
    }
}
