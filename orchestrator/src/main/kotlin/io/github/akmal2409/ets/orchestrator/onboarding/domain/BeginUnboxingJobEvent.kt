package io.github.akmal2409.ets.orchestrator.onboarding.domain

import java.util.*

data class BeginUnboxingJobEvent(
    val jobId: UUID,
    val source: String, // source bucket with key
    val out: String // output bucket with key prefix
) {
}
