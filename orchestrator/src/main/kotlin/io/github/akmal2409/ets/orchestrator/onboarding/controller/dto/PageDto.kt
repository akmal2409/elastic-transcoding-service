package io.github.akmal2409.ets.orchestrator.onboarding.controller.dto

import org.springframework.data.domain.Page

data class PageDto<T>(
    val items: Iterable<T>,
    val page: Int,
    val itemsPerPage: Int,
    val totalItems: Long
) {

    companion object {

        fun <T> of(page: Page<T>) =
            PageDto(
                page.content.toList(),
                page.number, page.size, page.totalElements
            )
    }
}
