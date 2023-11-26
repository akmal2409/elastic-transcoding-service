package io.github.akmal2409.ets.orchestrator.upload

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

/**
 * Upload request to generate pre-signed S3 upload url
 * @param filename name of the file
 * @param contentLength length of file in bytes
 * @param contentType of the file (should be one of video content types)
 */
data class UploadRequest(
    @field:NotEmpty(message = "filename is empty")
    val filename: String?,
    @field:Min(1L, message = "contentLength must be at least 1 byte long")
    val contentLength: Long?,
    @field:NotEmpty(message = "contentType is empty")
    val contentType: String?
) {
}
