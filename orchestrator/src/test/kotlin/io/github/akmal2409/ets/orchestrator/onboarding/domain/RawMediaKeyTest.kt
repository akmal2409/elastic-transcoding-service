package io.github.akmal2409.ets.orchestrator.onboarding.domain

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.lang.IllegalArgumentException
import java.util.UUID
import kotlin.math.exp

class RawMediaKeyTest {

    @Test
    fun `Throws IllegalArgumentException when key is malformed`() {
        arrayOf(
            "A1D82E11-5ABF-4AB7-9A58-B91EE7F3F8E2_hey there.txt",
            "A1D82E11-5ABF-4AB7-9A58-B91EE7F3F8E2-somename_name.txt",
            "A1D82E11-5ABF-4AB7-9A58-B91EE7F3F8EX_file.mp4",
            "file.txt",
            "file",
            "A1D82E11-5ABF-4AB7-9A58-B91EE7F3F8E2-somename/name.txt",
            "A1D82E11-5ABF-4AB7-9A58-B91EE7F3F8E2-somename\\name.txt",
        ).forEach {
            Assertions.assertThatThrownBy {
                RawMediaKey.fromString(it)
            }.isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    @Test
    fun `Parses object key correctly`() {
        val id = "A1D82E11-5ABF-4AB7-9A58-B91EE7F3F8E2"
        val name = "file.txt"
        val fullKey = "${id}_$name"
        val expectedKey = RawMediaKey(UUID.fromString(id), name)

        val actualKey = RawMediaKey.fromString(fullKey)

        assertThat(actualKey)
            .usingRecursiveComparison()
            .isEqualTo(expectedKey)
    }
}
