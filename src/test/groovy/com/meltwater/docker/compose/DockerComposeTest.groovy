package com.meltwater.docker.compose

import com.meltwater.docker.compose.data.PsResult
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
            PsResult inspectData = compose.up(Recreate.DEFAULT)
            def barHost = inspectData.getData("bar-host-1")
            def bar = inspectData.getData("bar-1")
            def foo = inspectData.getData("foo-1")
        then:
            inspectData.asList().size() == 3

            foo.name == "composetest_foo_1" || foo.name == "composetest-foo-1" // docker-compose v2 changed from underscore to dash, allow both
            foo.state.running
            !foo.state.dead

            bar.name == "composetest_bar_1" || bar.name == "composetest-bar-1"
            bar.state.running
            !bar.state.dead

            barHost.name == "composetest_bar-host_1" || barHost.name == "composetest-bar-host-1"
            barHost.state.running
            !barHost.state.dead
    }

    def 'get an existing environment variable'() {
        given: 'A docker compose object with an environment variable'
            compose = new DockerCompose("simple-busybox.yml", "composetest", new HashMap<String, String>(["key": "value"]))
        when: 'The variable is requested'
            def result = compose.getEnv("key")
        then: 'The variable is returned'
            result == "value"
    }

    def 'getting a non-existent environment variable returns null'() {
        given: 'A docker compose object with an environment variable'
            compose = new DockerCompose("simple-busybox.yml", "composetest", new HashMap<String, String>(["key": "value"]))
        when: 'The variable is requested'
            def result = compose.getEnv("foo")
        then: 'The variable is returned'
            result == null
    }

    def 'test port mappings'() {
        when:
            PsResult inspectData = compose.up(Recreate.DEFAULT)
            def barHost = inspectData.getData("bar-host-1")
            def bar = inspectData.getData("bar-1")
            def foo = inspectData.getData("foo-1")
        then:
            inspectData.asList().size() == 3
            barHost.bindingForTcpPort("8080") == "38080"
            barHost.bindingForTcpPort("8081") == "38081"
            !bar.bindingForTcpPort("8080").isEmpty()
            !foo.bindingForTcpPort("9090").isEmpty()
    }

    def 'can pull updates'() {
        def dockerCompose = new DockerCompose("test-pull.yml", "pull_test", new HashMap<String, String>())
        dockerCompose.pull()
    }
}
