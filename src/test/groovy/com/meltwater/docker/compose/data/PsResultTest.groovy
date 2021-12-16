package com.meltwater.docker.compose.data

import spock.lang.Specification

class PsResultTest extends Specification {

    def 'test parseShortName'() {
        when:
            def result = new PsResult("prefix123",
                    [stubData("prefix123-my-name-1"),
                     stubData("prefix123_with_underscore_1"),
                     stubData("prefix123-second-2"),
                     stubData("prefix123_third_3")])
        then:
            result.getData("my-name-1").name == "prefix123-my-name-1"
            result.getData("with_underscore-1").name == "prefix123_with_underscore_1"
            result.getData("second-2").name == "prefix123-second-2"
            result.getData("third-3").name == "prefix123_third_3"
    }

    static def stubData(name) {
        return new InspectData(
                "",
                name,
                new ContainerState(true, true),
                new HostConfig("", [:]),
                new NetworkSettings([:])
        )
    }

}
