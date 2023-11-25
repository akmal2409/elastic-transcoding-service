package io.github.akmal2409.ets.orchestrator.upload

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

const val UPLOAD_BASE_API_PATH = "/api/v1/upload"

@RestController
@RequestMapping(UPLOAD_BASE_API_PATH)
internal data class MediaUploadController(
    private val uploadService: UploadService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun generatePresignedUploadUrl(@RequestBody @Valid uploadRequest: UploadRequest): PresignedUploadUrl {
        return uploadService.generatePresignedUrl(uploadRequest)
    }
}
