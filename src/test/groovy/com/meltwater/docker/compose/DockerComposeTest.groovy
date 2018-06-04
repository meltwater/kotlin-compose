package com.meltwater.docker.compose

import com.meltwater.docker.compose.data.InspectData
import spock.lang.Shared
import spock.lang.Specification

class DockerComposeTest extends Specification {
    @Shared
    private static DockerCompose compose

    void setupSpec() throws Exception {
        compose = new DockerCompose("simple-busybox.yml", "composetest", new HashMap<String, String>())
    }

    void cleanup() throws Exception {
        compose.kill()
        compose.rm()
    }

    void cleanupSpec() throws Exception {
        compose.kill()
        compose.rm()
    }

    def 'test simple compose.up'() {
        when:
            List<InspectData> inspectData = compose.up()
        then:
            inspectData.size() == 3
            inspectData[0].state.running
            inspectData[0].name == "/composetest_bar-host_1"
            !inspectData[0].state.dead
            inspectData[1].state.running
            inspectData[1].name == "/composetest_bar_1"
            !inspectData[1].state.dead
            inspectData[2].name == "/composetest_foo_1"
            inspectData[2].state.running
            !inspectData[2].state.dead
    }

    def 'test port mappings'() {
        when:
            List<InspectData> inspectData = compose.up()
        then:
            inspectData.size() == 3
            inspectData[0].bindingForTcpPort("8080") == "38080"
            inspectData[0].bindingForTcpPort("8081") == "38081"
            !inspectData[1].bindingForTcpPort("8080").isEmpty()
            !inspectData[2].bindingForTcpPort("9090").isEmpty()
    }

    def 'kill single container'() {
        given:
            compose.up()
        when:
            compose.kill("composetest_foo_1")
        then:
            List<InspectData> inspectData = compose.ps()
            inspectData.size() == 3
            inspectData.each {
                it.state.dead
                !it.state.running
            }
    }

    def 'can pull updates'() {
        def dockerCompose = new DockerCompose("test-pull.yml", "pull_test", new HashMap<String, String>())
        dockerCompose.pull()
    }
}
