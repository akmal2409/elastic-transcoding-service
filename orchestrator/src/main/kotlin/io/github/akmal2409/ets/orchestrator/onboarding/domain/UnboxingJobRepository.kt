package io.github.akmal2409.ets.orchestrator.onboarding.domain

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp

private const val UNBOXING_JOB_TABLE_NAME = "unboxing_job"


@Repository
data class UnboxingJobRepository(
    val jdbcTemplate: JdbcTemplate,
    val objectMapper: ObjectMapper
) {

    fun insert(unboxingJob: UnboxingJob): UnboxingJob {
        val filesJson = objectMapper.writeValueAsString(unboxingJob.unboxedFiles)
        jdbcTemplate.update("""
            INSERT INTO $UNBOXING_JOB_TABLE_NAME (
                id, raw_media_id, raw_media_name, status, started_at,
                completed_at, version, unboxed_files
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::JSONB)
        """.trimIndent(), unboxingJob.id,
            unboxingJob.rawMediaKey.id, unboxingJob.rawMediaKey.name,
            unboxingJob.status.name, Timestamp.from(unboxingJob.startedAt), unboxingJob.completedAt?.let(Timestamp::from),
            unboxingJob.version, filesJson)

        return unboxingJob
    }

}
