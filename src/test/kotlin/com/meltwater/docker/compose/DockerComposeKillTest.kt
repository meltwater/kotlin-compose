package com.meltwater.docker.compose


import com.meltwater.docker.compose.data.PsResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class DockerComposeKillTest {
    companion object {
        val compose = DockerCompose("simple-busybox.yml", "composeKillTest", hashMapOf())

        @AfterAll
        @JvmStatic
        fun cleanupSpec() {
            try {
                compose.kill()
            } catch (e: RuntimeException) {
                if (e.message!!.contains("Cannot kill container") && e.message!!.endsWith(" is not running\n")) {
                    // In docker compose v2, kill fails when a container is already killed. Treat as OK for this test
                    LoggerFactory.getLogger(
                        DockerComposeKillTest::class.java)
                            .warn("Kill failed, probably because container was already killed. Swallowing exception", e)
                } else {
                    throw e
                }
            }

            // rm appears to be flaky if ran too fast after the partially failed kill, sometimes not removing all containers.
            Thread.sleep(1000)
            compose.rm()
        }
    }

    @Test
    fun `kill single container`() {
            val upResult = compose.up(Recreate.DEFAULT)
            compose.kill(upResult.getData("foo-1")!!.name)

            val inspectData = compose.ps()
            val foo = inspectData.getData("foo-1")!!
            val bar = inspectData.getData("bar-1")!!
            val barHost = inspectData.getData("bar-host-1")!!

            assertThat(foo.state.running).isFalse()
            assertThat(bar.state.running).isTrue()
            assertThat(barHost.state.running).isTrue()
    }

}
