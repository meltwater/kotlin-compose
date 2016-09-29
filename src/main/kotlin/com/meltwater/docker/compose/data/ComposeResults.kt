package com.meltwater.docker.compose.data

import com.fasterxml.jackson.annotation.JsonProperty

data class InspectData(
        @param:JsonProperty("Id") val id: String,
        @param:JsonProperty("Name") val name: String,
        @param:JsonProperty("State") val state: ContainerState,
        @param:JsonProperty("NetworkSettings") val networkSettings: NetworkSettings)

data class ContainerState(
        @param:JsonProperty("Running") val running: Boolean,
        @param:JsonProperty("Dead") val dead: Boolean)

data class NetworkSettings(
        @param:JsonProperty("Ports") val ports: Map<String, Array<PortBinding>>?) {
    /**
     * Finds a single binding for an internal TCP port
     */
    fun bindingForTcpPort(internalPort: String): String {
        val hostPort = ports?.get("$internalPort/tcp")?.first()?.hostPort ?:
                throw RuntimeException("Port mapping not found for internalPort $internalPort. Available ports are: $ports")
        return hostPort
    }
}

data class PortBinding(
        @param:JsonProperty("HostIp") val hostIp: String,
        @param:JsonProperty("HostPort") val hostPort: String)