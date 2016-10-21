package com.meltwater.docker.compose

import com.beust.klaxon.*
import com.meltwater.docker.compose.ExecUtils.executeCommand
import com.meltwater.docker.compose.data.ContainerState
import com.meltwater.docker.compose.data.InspectData
import com.meltwater.docker.compose.data.NetworkSettings
import com.meltwater.docker.compose.data.PortBinding
import java.io.ByteArrayInputStream


object Docker {

    val parser: Parser = Parser()

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
        val ids: String = containerId.fold(" ", { a, b -> "$a $b" })
        val inspectJson = exec("inspect $ids")

        return parseJson(inspectJson)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseJson(inspectJson: String): List<InspectData> {
        val inspectData: Iterable<JsonObject> = parser.parse(ByteArrayInputStream(inspectJson.toByteArray())) as Iterable<JsonObject>

        val response = inspectData.map { containerData ->
            val settings = containerData["NetworkSettings"] as JsonObject
            val ports: JsonObject? = settings["Ports"] as JsonObject?

            val portMappings: Map<String, List<PortBinding>>? = ports?.mapValues { entry ->
                (entry.value as JsonArray<JsonObject>).map {
                    PortBinding(it.string("HostIp")!!, it.string("HostPort")!!)
                }
            }

            val state = containerData["State"] as JsonObject
            val containerState = ContainerState(state.boolean("Running")!!, state.boolean("Dead")!!)

            val networkSettings = NetworkSettings(portMappings ?: hashMapOf())

            InspectData(
                    id = containerData.string("Id")!!,
                    name = containerData.string("Name")!!,
                    state = containerState,
                    networkSettings = networkSettings
            )
        }

        return response
    }

    private fun exec(command: String): String {
        return executeCommand("docker " + command)
    }
}
