package io.github.akmal2409.ets.orchestrator.onboarding.controller

import io.github.akmal2409.ets.orchestrator.onboarding.controller.dto.PageDto
import io.github.akmal2409.ets.orchestrator.onboarding.controller.dto.RawMediaDto
import io.github.akmal2409.ets.orchestrator.onboarding.service.RawMediaService
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(RawMediaController.BASE_PATH)
data class RawMediaController(
    val mediaService: RawMediaService
) {

    companion object {
        const val BASE_PATH = "/v1/raw-media"
    }

    @GetMapping
    fun findAll(
        @Valid @Min(0) @RequestParam("page", required = false, defaultValue = "0") page: Int,
        @Valid @Min(1) @Max(100) @RequestParam(
            "size",
            required = false,
            defaultValue = "25"
        ) size: Int
    ): PageDto<RawMediaDto> = PageDto.of(
        mediaService.findAll(page, size)
            .map(RawMediaDto::from)
    )

}
