package io.github.akmal2409.ets.orchestrator.onboarding.domain

import io.github.akmal2409.ets.orchestrator.commons.db.OptimisticLockingException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

const val RAW_MEDIA_TABLE_NAME = "raw_media"

@Repository
data class RawMediaRepository(
    val jdbcTemplate: JdbcTemplate
) {

    fun insert(rawMedia: RawMedia): RawMedia {
        jdbcTemplate.update("""
            INSERT INTO $RAW_MEDIA_TABLE_NAME (id, name, unboxed, version)
            VALUES (?, ?, ?, 0)
        """.trimMargin(), rawMedia.key.id, rawMedia.key.name, rawMedia.unboxed)
        return rawMedia
    }

    fun update(rawMedia: RawMedia): RawMedia {
        val withHigherVersion = rawMedia.copy(version = rawMedia.version + 1)

        val rowsAffected = jdbcTemplate.update("""
            UPDATE $RAW_MEDIA_TABLE_NAME
            SET name = ?, unboxed = ?, version = ?
            WHERE id = ? AND version = ?
        """.trimIndent(), withHigherVersion.key.name, withHigherVersion.unboxed, withHigherVersion.version,
            withHigherVersion.key.id, rawMedia.version)

        if (rowsAffected == 0) {
            throw OptimisticLockingException(RawMedia::class.simpleName ?: "Raw Media",
                withHigherVersion.key.id, rawMedia.version, withHigherVersion.version)
        }

        return withHigherVersion
    }
}
