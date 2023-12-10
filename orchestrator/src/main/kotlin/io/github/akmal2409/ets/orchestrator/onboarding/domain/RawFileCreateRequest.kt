package io.github.akmal2409.ets.orchestrator.onboarding.domain

data class RawFileCreateRequest(
    val mediaKey: RawMediaKey
) {

    companion object {

        /**
         * Constructs an instance based on the object key that follows
         * pattern of UUID_filename
         */
        fun fromObjectKey(objectKey: String): RawFileCreateRequest {
            return RawFileCreateRequest(RawMediaKey.fromString(objectKey))
        }
    }
}
