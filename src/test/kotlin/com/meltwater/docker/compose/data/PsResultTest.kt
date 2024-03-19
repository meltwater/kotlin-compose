package com.meltwater.docker.compose.data

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PsResultTest {

    @Test
    fun `test parseShortName`() {
        val result = PsResult(
            "prefix123",
            listOf(
                stubData("prefix123-my-name-1"),
                stubData("prefix123_with_underscore_1"),
                stubData("prefix123-second-2"),
                stubData("prefix123_third_3")
            )
        )

        assertThat(result.getData("my-name-1")!!.name).isEqualTo("prefix123-my-name-1")
        assertThat(result.getData("with_underscore-1")!!.name).isEqualTo("prefix123_with_underscore_1")
        assertThat(result.getData("second-2")!!.name).isEqualTo("prefix123-second-2")
        assertThat(result.getData("third-3")!!.name).isEqualTo("prefix123_third_3")
    }

    fun stubData(name: String): InspectData {
        return InspectData(
            "",
            name,
            ContainerState(true, true),
            HostConfig("", linkedMapOf()),
            NetworkSettings(emptyMap())
        )
    }

}
