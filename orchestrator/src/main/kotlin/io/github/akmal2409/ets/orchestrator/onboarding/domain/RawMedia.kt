package io.github.akmal2409.ets.orchestrator.onboarding.domain

import java.time.Clock
import java.time.Instant
import java.util.*
import java.util.regex.Pattern

const val S3_BUCKET_NAME = "raw"
const val NEW_UPLOADS_S3_KEY_PREFIX = "uploaded"
const val UNBOXED_S3_KEY_PREFIX = "unboxed"

data class RawMediaKey(val id: UUID, val name: String) {

    companion object {
        private val KEY_PATTERN: Pattern = Pattern.compile(
            "(?<uuid>[a-f\\d]{8}(-[a-f\\d]{4}){3}-[a-f\\d]{12})_(?<name>[^\\s\\\\/]+)\$",
            Pattern.CASE_INSENSITIVE
        )

        fun fromString(key: String): RawMediaKey {
            val matcher = KEY_PATTERN.matcher(key)

            require(matcher.matches()) { "Key $key doesn't conform to pattern $KEY_PATTERN" }

            val uuidStr: String = matcher.group("uuid")
            val name: String = matcher.group("name")

            return RawMediaKey(
                UUID.fromString(uuidStr), name
            )
        }
    }

    override fun toString(): String {
        return "${id}_$name"
    }
}

data class RawMedia(
    val key: RawMediaKey, val unboxed: Boolean, // whether the media container was unboxed
    val version: Long
) {

    val rawFileS3Key = "${S3_BUCKET_NAME}/${NEW_UPLOADS_S3_KEY_PREFIX}/${key.id}_${key.name}"
    val unboxedFilesS3KeyPrefix = "${S3_BUCKET_NAME}/${UNBOXED_S3_KEY_PREFIX}/${key.id}_${key.name}"

    fun beginUnboxingJob(
        jobId: UUID = UUID.randomUUID(), clock: Clock = Clock.systemUTC()
    ): Pair<UnboxingJob, BeginUnboxingJobEvent> {
        check(unboxed) { "Media is already unboxed" }

        return UnboxingJob.newStarted(this.key, jobId, clock) to BeginUnboxingJobEvent(
            jobId, "s3://$rawFileS3Key", "s3://$unboxedFilesS3KeyPrefix"
        )
    }
}

data class UnboxingJob(
    val id: UUID,
    val rawMediaKey: RawMediaKey,
    val status: Status,
    val startedAt: Instant,
    val completedAt: Instant?,
    val version: Long,
    val unboxedFiles: UnboxedFiles?
) {

    companion object {

        fun newStarted(
            mediaKey: RawMediaKey, id: UUID = UUID.randomUUID(), clock: Clock = Clock.systemUTC()
        ): UnboxingJob {
            return UnboxingJob(
                id, mediaKey, Status.STARTED, Instant.now(clock), null, 0, null
            )
        }
    }

    enum class Status {
        STARTED, FAILED_START, FAILED, COMPLETED
    }

    data class UnboxedFiles(
        val videos: List<Video>, val audio: List<Audio>, val subtitles: List<Subtitles>
    ) {

        data class Video(
            val filename: String, val codec: String, val width: Int, val height: Int
        )

        data class Audio(
            val filename: String, val codec: String, val lang: String
        )

        data class Subtitles(
            val filename: String, val codec: String, val lang: String
        )
    }
}
