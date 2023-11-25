package io.github.akmal2409.ets.orchestrator.upload

import java.net.URL
import java.time.Instant

data class PresignedUploadUrl(
    val url: URL,
    val expiresAt: Instant
) {
}
