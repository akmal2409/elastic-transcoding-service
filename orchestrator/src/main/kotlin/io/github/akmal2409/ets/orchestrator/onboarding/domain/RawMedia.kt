package io.github.akmal2409.ets.orchestrator.onboarding.domain

import java.util.UUID
import java.util.regex.Pattern

const val S3_BUCKET_NAME = "raw"
const val NEW_UPLOADS_S3_KEY_PREFIX = "uploaded"
const val UNBOXED_S3_KEY_PREFIX = "unboxed"

data class RawMediaKey(val id: UUID, val name: String) {

    companion object {
        private val KEY_PATTERN: Pattern = Pattern.compile("(?<uuid>[a-f\\d]{8}(-[a-f\\d]{4}){3}-[a-f\\d]{12})_(?<name>[^\\s\\\\/]+)\$", Pattern.CASE_INSENSITIVE)
        fun fromString(key: String): RawMediaKey {
            val matcher = KEY_PATTERN.matcher(key)

            require(matcher.matches()) { "Key $key doesn't conform to pattern $KEY_PATTERN"}

            val uuidStr: String = matcher.group("uuid")
            val name: String = matcher.group("name")

            return RawMediaKey(
                UUID.fromString(uuidStr),
                name
            )
        }
    }
}

data class RawMedia(
    val key: RawMediaKey,
    val unboxed: Boolean, // whether the media container was unboxed
    val version: Long
) {

    val rawFileS3Key = "${S3_BUCKET_NAME}/${NEW_UPLOADS_S3_KEY_PREFIX}/${key.id}-${key.name}"
    val unboxedFilesS3KeyPrefix = "${S3_BUCKET_NAME}/${UNBOXED_S3_KEY_PREFIX}/${key.id}-${key.name}"
}
