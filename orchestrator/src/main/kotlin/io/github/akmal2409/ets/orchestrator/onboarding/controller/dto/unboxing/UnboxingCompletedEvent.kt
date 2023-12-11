package io.github.akmal2409.ets.orchestrator.onboarding.controller.dto.unboxing

import io.github.akmal2409.ets.orchestrator.onboarding.domain.UnboxingJob
import java.util.*

data class VideoDto(
    val filename: String, val codec: String, val width: Int, val height: Int
)

data class AudioDto(
    val filename: String, val codec: String, val lang: String
)

data class SubtitlesDto(
    val filename: String, val codec: String, val lang: String
)

data class UnboxingCompletedEvent(
    val jobId: UUID,
    val videos: List<VideoDto>,
    val audio: List<AudioDto>,
    val subtitles: List<SubtitlesDto>,
    val outputPrefix: String
) {
    fun toDomainUnboxedFiles(): UnboxingJob.UnboxedFiles =
        UnboxingJob.UnboxedFiles(
            videos.map {
                UnboxingJob.UnboxedFiles.Video(
                    it.filename,
                    it.codec,
                    it.width,
                    it.height
                )
            },
            audio.map { UnboxingJob.UnboxedFiles.Audio(it.filename, it.codec, it.lang) },
            subtitles.map { UnboxingJob.UnboxedFiles.Subtitles(it.filename, it.codec, it.lang) }
        )
}
