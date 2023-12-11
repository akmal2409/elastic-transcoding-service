package io.github.akmal2409.ets.orchestrator.onboarding.domain

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.akmal2409.ets.orchestrator.commons.db.OptimisticLockingException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

const val UNBOXING_JOB_TABLE_NAME = "unboxing_job"


@Repository
data class UnboxingJobRepository(
    val jdbcTemplate: JdbcTemplate,
    val objectMapper: ObjectMapper
) {

    fun insert(unboxingJob: UnboxingJob): UnboxingJob {
        val filesJson = objectMapper.writeValueAsString(unboxingJob.unboxedFiles)
        jdbcTemplate.update(
            """
            INSERT INTO $UNBOXING_JOB_TABLE_NAME (
                id, raw_media_id, raw_media_name, status, started_at,
                completed_at, version, unboxed_files
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?::JSONB)
        """.trimIndent(),
            unboxingJob.id,
            unboxingJob.rawMediaKey.id,
            unboxingJob.rawMediaKey.name,
            unboxingJob.status.name,
            Timestamp.from(unboxingJob.startedAt),
            unboxingJob.completedAt?.let(Timestamp::from),
            unboxingJob.version,
            filesJson
        )

        return unboxingJob
    }

    fun update(unboxingJob: UnboxingJob): UnboxingJob {
        val updatedJob = unboxingJob.copy(version = unboxingJob.version + 1)

        val rowsAffected = jdbcTemplate.update(
            """
            UPDATE $UNBOXING_JOB_TABLE_NAME
            SET status = ?, started_at = ?, completed_at = ?, version = ?, unboxed_files = ?::JSONB
            WHERE id = ? AND version = ?
        """.trimIndent(),
            updatedJob.status.name,
            Timestamp.from(unboxingJob.startedAt),
            updatedJob.completedAt?.let(Timestamp::from),
            updatedJob.version,
            objectMapper.writeValueAsString(updatedJob.unboxedFiles),
            updatedJob.id,
            unboxingJob.version
        )

        if (rowsAffected == 0) {
            throw OptimisticLockingException(
                UnboxingJob::class.simpleName ?: "UnboxingJob",
                unboxingJob.id, unboxingJob.version, updatedJob.version
            )
        }

        return updatedJob
    }

    fun findById(id: UUID): UnboxingJob? {
        return jdbcTemplate.query(
            """
            SELECT id, raw_media_id, raw_media_name, status, started_at,
            completed_at, version, unboxed_files
            FROM $UNBOXING_JOB_TABLE_NAME
            WHERE id = ?
        """.trimIndent(), this::mapRow, id
        )
            .firstOrNull()
    }

    private fun mapRow(resultSet: ResultSet, rowNumber: Int): UnboxingJob {
        return UnboxingJob(
            resultSet.getObject("id", UUID::class.java),
            RawMediaKey(
                resultSet.getObject("raw_media_id", UUID::class.java),
                resultSet.getString("raw_media_name")
            ),
            UnboxingJob.Status.valueOf(resultSet.getString("status")),
            resultSet.getTimestamp("started_at").toInstant(),
            resultSet.getTimestamp("completed_at")?.let { it.toInstant() },
            resultSet.getLong("version"),
            objectMapper.readValue(
                resultSet.getString("unboxed_files"),
                UnboxingJob.UnboxedFiles::class.java
            )
        )
    }
}
