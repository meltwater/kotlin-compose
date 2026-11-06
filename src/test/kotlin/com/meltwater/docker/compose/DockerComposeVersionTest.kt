package com.meltwater.docker.compose

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class DockerComposeVersionTest {

    private val min = DockerComposeVersion.parse("1.5.0")

    @Test
    fun `older version is below the minimum`() {
        assertThat(DockerComposeVersion.parse("1.4.9")).isLessThan(min)
    }

    @Test
    fun `equal and newer versions are at least the minimum`() {
        assertThat(DockerComposeVersion.parse("1.5.0")).isGreaterThanOrEqualTo(min)
        assertThat(DockerComposeVersion.parse("2.29.1")).isGreaterThanOrEqualTo(min)
    }

    @Test
    fun `components compare as numbers, not text`() {
        assertThat(DockerComposeVersion.parse("1.10.0")).isGreaterThan(min)
        assertThat(DockerComposeVersion.parse("1.5.10")).isGreaterThan(DockerComposeVersion.parse("1.5.9"))
    }

    @Test
    fun `missing or malformed version is rejected`() {
        assertThatThrownBy { DockerComposeVersion.parse(null) }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { DockerComposeVersion.parse("1.5") }.isInstanceOf(IllegalArgumentException::class.java)
        assertThatThrownBy { DockerComposeVersion.parse("1.x.0") }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
