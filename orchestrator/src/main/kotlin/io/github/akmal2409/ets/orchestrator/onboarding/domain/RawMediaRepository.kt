package io.github.akmal2409.ets.orchestrator.onboarding.domain

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.akmal2409.ets.orchestrator.commons.db.OptimisticLockingException
import org.apache.logging.log4j.util.Unbox
import org.springframework.dao.DataRetrievalFailureException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.ResultSet
import java.util.*

const val RAW_MEDIA_TABLE_NAME = "raw_media"

@Repository
data class RawMediaRepository(
    val jdbcTemplate: JdbcTemplate,
    val objectMapper: ObjectMapper
) {

    fun findAll(pageable: Pageable): Page<RawMedia> {
        val itemCount = jdbcTemplate.query("SELECT COUNT(id) as total FROM $RAW_MEDIA_TABLE_NAME",
            RowMapper { rs, _ -> rs.getLong("total") })
            .firstOrNull() ?: throw DataRetrievalFailureException("Could not fetch count of rows")

        val items = jdbcTemplate.query("""
            SELECT m.id as id, m.name as name, m.unboxed as unboxed, m.version as version,
                uj.id as job_id, uj.status as job_status, uj.started_at as job_started_at,
                uj.completed_at as job_completed_at, uj.version as job_version, 
                uj.unboxed_files as job_unboxed_files
            FROM $RAW_MEDIA_TABLE_NAME m
            LEFT JOIN $UNBOXING_JOB_TABLE_NAME uj ON m.id = uj.raw_media_id AND uj.status = '${UnboxingJob.Status.STARTED}'
            LIMIT ? OFFSET ?
        """.trimIndent(),
            ::rowMapper, pageable.pageSize, pageable.offset
        )

        return PageImpl(items, pageable, itemCount)
    }

    fun insert(rawMedia: RawMedia): RawMedia {
        jdbcTemplate.update(
            """
            INSERT INTO $RAW_MEDIA_TABLE_NAME (id, name, unboxed, version)
            VALUES (?, ?, ?, 0)
        """.trimMargin(), rawMedia.key.id, rawMedia.key.name, rawMedia.unboxed
        )
        return rawMedia
    }

    fun update(rawMedia: RawMedia): RawMedia {
        val withHigherVersion = rawMedia.copy(version = rawMedia.version + 1)

        val rowsAffected = jdbcTemplate.update(
            """
            UPDATE $RAW_MEDIA_TABLE_NAME
            SET name = ?, unboxed = ?, version = ?
            WHERE id = ? AND version = ?
        """.trimIndent(),
            withHigherVersion.key.name,
            withHigherVersion.unboxed,
            withHigherVersion.version,
            withHigherVersion.key.id,
            rawMedia.version
        )

        if (rowsAffected == 0) {
            throw OptimisticLockingException(
                RawMedia::class.simpleName ?: "Raw Media",
                withHigherVersion.key.id, rawMedia.version, withHigherVersion.version
            )
        }

        return withHigherVersion
    }

    private fun rowMapper(resultSet: ResultSet, rowNum: Int): RawMedia {
        val mediaKey = RawMediaKey(
            resultSet.getObject("id", UUID::class.java),
            resultSet.getString("name")
        )

        val pendingJob: UnboxingJob? = resultSet.getObject("job_id", UUID::class.java)
            ?.let {
                UnboxingJob(it, mediaKey, UnboxingJob.Status.valueOf(resultSet.getString("job_status")),
                    resultSet.getTimestamp("job_started_at").toInstant(),
                    resultSet.getTimestamp("job_completed_at")?.let { it.toInstant() },
                    resultSet.getLong("job_version"),
                    objectMapper.readValue(resultSet.getString("job_unboxed_files"), UnboxingJob.UnboxedFiles::class.java))
            }
        return RawMedia(
            mediaKey,
            resultSet.getBoolean("unboxed"),
            resultSet.getLong("version"),
            pendingJob
        )
    }
}
