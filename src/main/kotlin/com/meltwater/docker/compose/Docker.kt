package com.meltwater.docker.compose

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.meltwater.docker.compose.ExecUtils.executeCommand
import com.meltwater.docker.compose.data.InspectData


object Docker {

    private var mapper: ObjectMapper

    init {
        mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun kill(containerName: String) {
        exec("kill $containerName")
    }

    fun start(containerName: String) {
        exec("start $containerName")
    }

    fun inspect(vararg containerId: String): List<InspectData> {
        if (containerId.isEmpty()) {
            return arrayListOf()
        }
        val ids: String = containerId.joinToString(" ")
        val message = exec("inspect $ids")

        val typeRef = object : TypeReference<List<InspectData>>() {}
        return mapper.readValue(message, typeRef)
    }

    private fun exec(command: String): String {
        return executeCommand("docker " + command)
    }
}
