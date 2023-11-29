package io.github.akmal2409.ets.orchestrator.onboarding.domain

import io.github.akmal2409.ets.orchestrator.commons.db.OptimisticLockingException
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
    val jdbcTemplate: JdbcTemplate
) {

    fun findAll(pageable: Pageable): Page<RawMedia> {
        val itemCount = jdbcTemplate.query("SELECT COUNT(id) as total FROM $RAW_MEDIA_TABLE_NAME",
            RowMapper { rs, _ -> rs.getLong("total") })
            .firstOrNull() ?: throw DataRetrievalFailureException("Could not fetch count of rows")

        val items = jdbcTemplate.query(
            "SELECT id, name, unboxed, version FROM $RAW_MEDIA_TABLE_NAME",
            ::rowMapper
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

    private fun rowMapper(resultSet: ResultSet, rowNum: Int) =
        RawMedia(
            RawMediaKey(
                resultSet.getObject("id", UUID::class.java),
                resultSet.getString("name")
            ),
            resultSet.getBoolean("unboxed"),
            resultSet.getLong("version")
        )
}
