package io.github.akmal2409.ets.orchestrator.onboarding.domain

import io.github.akmal2409.ets.orchestrator.MockClock
import io.github.akmal2409.ets.orchestrator.onboarding.controller.dto.unboxing.UnboxingCompletedEvent
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.*

val id: UUID = UUID.fromString("19FFC231-8D62-47BF-9E36-5509F95CD0F4")
val baseRawMedia = RawMedia(RawMediaKey(UUID.fromString("00A7405D-DBE5-46AB-9D0E-094BB9DAA89F"), "name"), false, 0)

class RawMediaTest {

    @Test
    fun `Throws exception when media is unboxed`() {
        val updateEvent = UnboxingCompletedEvent(id, listOf(), listOf(), listOf(), "bucket/some_prefix")
        val unboxedMedia = baseRawMedia.copy(unboxed = true)

        assertThatThrownBy {
            unboxedMedia.completeUnboxing(MockClock(Instant.EPOCH), updateEvent.toDomainUnboxedFiles())
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("already unboxed")
    }

    @Test
    fun `Throws exception if no pending job is present`() {
        val updateEvent = UnboxingCompletedEvent(id, listOf(), listOf(), listOf(), "bucket/some_prefix")
        val unboxedMedia = baseRawMedia.copy(pendingUnboxingJob = null)

        assertThatThrownBy {
            unboxedMedia.completeUnboxing(MockClock(Instant.EPOCH), updateEvent.toDomainUnboxedFiles())
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Media does not have associated pending job")
    }

    @Test
    fun `Throws exception if pending job is not in started state`() {
        val updateEvent = UnboxingCompletedEvent(id, listOf(), listOf(), listOf(), "bucket/some_prefix")
        val pendingJobNotStarted = UnboxingJob(UUID.randomUUID(), baseRawMedia.key, UnboxingJob.Status.COMPLETED, Instant.EPOCH, Instant.EPOCH, 0,
            updateEvent.toDomainUnboxedFiles())
        val unboxedMedia = baseRawMedia.copy(pendingUnboxingJob = pendingJobNotStarted)

        assertThatThrownBy {
            unboxedMedia.completeUnboxing(MockClock(Instant.EPOCH), updateEvent.toDomainUnboxedFiles())
        }.isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Job is not marked as started")
    }

    @Test
    fun `Moves the raw media to unboxed state with job updated`() {
        val updateEvent = UnboxingCompletedEvent(id, listOf(), listOf(), listOf(), "bucket/some_prefix")
        val pendingJobNotStarted = UnboxingJob(UUID.randomUUID(), baseRawMedia.key, UnboxingJob.Status.STARTED, Instant.EPOCH, null, 0,
            updateEvent.toDomainUnboxedFiles())
        val unboxedMedia = baseRawMedia.copy(pendingUnboxingJob = pendingJobNotStarted)

        val expectedJob = pendingJobNotStarted.copy(status = UnboxingJob.Status.COMPLETED, completedAt = Instant.EPOCH)
        val expectedMedia = unboxedMedia.copy(pendingUnboxingJob = null, unboxed = true)

        val (updatedMedia, updatedJob) = unboxedMedia.completeUnboxing(MockClock(Instant.EPOCH), updateEvent.toDomainUnboxedFiles())

        assertThat(updatedJob)
            .usingRecursiveComparison()
            .isEqualTo(expectedJob)

        assertThat(updatedMedia)
            .usingRecursiveComparison()
            .isEqualTo(expectedMedia)
    }
}
