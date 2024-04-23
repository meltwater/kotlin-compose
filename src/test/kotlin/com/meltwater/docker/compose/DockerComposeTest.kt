package com.meltwater.docker.compose

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class DockerComposeTest {

    private val compose = DockerCompose("simple-busybox.yml", "composetest", HashMap<String, String>())

    @AfterEach
    fun cleanup() {
        compose.kill()
        compose.rm()
    }

    @Test
    fun `test simple compose up`() {
        val inspectData = compose.up(Recreate.DEFAULT)
        val barHost = inspectData.getData("bar-host-1")
        val bar = inspectData.getData("bar-1")!!
        val foo = inspectData.getData("foo-1")!!

        assertThat(inspectData.asList()).hasSize(3)

        assertThat(foo.name).isIn(
            "composetest_foo_1",
            "composetest-foo-1"
        ) // docker-compose v2 changed from underscore to dash, allow both)
        assertThat(foo.state.running).isTrue()
        assertThat(foo.state.dead).isFalse()

        assertThat(bar.name).isIn("composetest_bar_1", "composetest-bar-1")
        assertThat(bar.state.running).isTrue()
        assertThat(bar.state.dead).isFalse()

        assertThat(barHost!!.name).isIn("composetest_bar-host_1", "composetest-bar-host-1")
        assertThat(barHost.state.running).isTrue()
        assertThat(barHost.state.dead).isFalse()
    }

    @Test
    fun `get an existing environment variable`() {
        // "A docker compose object with an environment variable"
        val compose = DockerCompose("simple-busybox.yml", "composetest", hashMapOf("key" to "value"))
        //"The variable is requested"
        val result = compose.getEnv("key")
        //"The variable is returned"
        assertThat(result).isEqualTo("value")
    }

    @Test
    fun `getting a non-existent environment variable returns null`() {
        //"A docker compose object with an environment variable"
        val compose = DockerCompose("simple-busybox.yml", "composetest", hashMapOf("key" to "value"))
        //"The variable is requested"
        val result = compose.getEnv("foo")
        //"The variable is returned"
        assertThat(result).isEqualTo(null)
    }

    @Test
    fun `test port mappings`() {
        val inspectData = compose.up (Recreate.DEFAULT)
        val barHost = inspectData.getData("bar-host-1")!!
        val bar = inspectData.getData("bar-1")!!
        val foo = inspectData.getData("foo-1")!!

        assertThat(inspectData.asList()).hasSize(3)
        assertThat(bar.bindingForTcpPort("8080")).isNotEmpty()
        assertThat(foo.bindingForTcpPort("9090")).isNotEmpty()
    }

    @Test
    fun `can set pull policy to never`() {
        val result = compose.up (Recreate.DEFAULT, pullPolicy = PullPolicy.NEVER)
        assertThat(result.asList()).hasSize(3)
    }

    @Test
    fun `can set pull policy to always`() {
        val result = compose.up (Recreate.DEFAULT, pullPolicy = PullPolicy.ALWAYS)
        assertThat(result.asList()).hasSize(3)
    }

    @Test
    fun `can set quiet pull flag`() {
        val result = compose.up (Recreate.DEFAULT, quietPull = true)
        assertThat(result.asList()).hasSize(3)
    }
}
