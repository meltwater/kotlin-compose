package com.meltwater.docker.compose


import com.meltwater.docker.compose.data.PsResult
import org.slf4j.LoggerFactory
import spock.lang.Shared
import spock.lang.Specification

class DockerComposeKillTest extends Specification {
    @Shared
    private static DockerCompose compose

    void setupSpec() throws Exception {
        compose = new DockerCompose("simple-busybox.yml", "composeKillTest", new HashMap<String, String>())
    }

    void cleanupSpec() throws Exception {
        try {
            compose.kill()
        }
        catch (RuntimeException e) {
            if (e.message.contains("Cannot kill container") && e.message.endsWith(" is not running\n")) {
                // In docker compose v2, kill fails when a container is already killed. Treat as OK for this test
                LoggerFactory.getLogger(DockerComposeKillTest.class)
                        .warn("Kill failed, probably because container was already killed. Swallowing exception", e)
            } else {
                throw e
            }
        }

        // rm appears to be flaky if ran too fast after the partially failed kill, sometimes not removing all containers.
        Thread.sleep(1000)
        compose.rm()
    }

    def 'kill single container'() {
        given:
            def upResult = compose.up(Recreate.DEFAULT)
        when:
            compose.kill(upResult.getData("foo-1").name)
        then:
            PsResult inspectData = compose.ps()
            def foo = inspectData.getData("foo-1")
            def bar = inspectData.getData("bar-1")
            def barHost = inspectData.getData("bar-host-1")

            !foo.state.running
            bar.state.running
            barHost.state.running
    }

}
