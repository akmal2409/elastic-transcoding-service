package io.github.akmal2409.ets.orchestrator.upload

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.BAD_GATEWAY)
class PresignedUrlGenerationFailed(
    val dataStore: String,
    val objectKey: String,
    cause: Throwable? = null
) : RuntimeException("Failed to generate presigned URL to $dataStore with key $objectKey", cause) {
}
