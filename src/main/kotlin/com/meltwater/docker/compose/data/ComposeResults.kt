package com.meltwater.docker.compose.data

import com.fasterxml.jackson.annotation.JsonProperty

data class InspectData(
        @param:JsonProperty("Id") val id: String,
        @param:JsonProperty("Name") val name: String,
        @param:JsonProperty("State") val state: ContainerState,
        @param:JsonProperty("HostConfig") val hostConfig: HostConfig,
        @param:JsonProperty("NetworkSettings") val networkSettings: NetworkSettings) {
    /**
     * Finds a single binding for an internal TCP port
     */
    fun bindingForTcpPort(internalPort: String): String {
        val ports  = if (hostConfig.networkMode == "host") hostConfig.ports else networkSettings.ports

        return ports?.get("$internalPort/tcp")?.first()?.hostPort
                ?: throw RuntimeException("Port mapping not found for internalPort $internalPort. Available ports are: $ports")
    }

}


data class ContainerState(
        @param:JsonProperty("Running") val running: Boolean,
        @param:JsonProperty("Dead") val dead: Boolean)

data class HostConfig(
        @param:JsonProperty("NetworkMode") val networkMode: String,
        @param:JsonProperty("PortBindings") val ports: LinkedHashMap<String, Array<PortBinding>>?)

data class NetworkSettings(
        @param:JsonProperty("Ports") val ports: Map<String, Array<PortBinding>>?) {

    @Deprecated("Use the InspectData.bindingForTcpPort() instead", replaceWith = ReplaceWith("InspectData.bindingForTcpPort()"))
    fun bindingForTcpPort(internalPort: String): String {
        return ports?.get("$internalPort/tcp")?.first()?.hostPort ?:
                throw RuntimeException("Port mapping not found for internalPort $internalPort. Available ports are: $ports")
    }
}

data class PortBinding(
        @param:JsonProperty("HostIp") val hostIp: String,
        @param:JsonProperty("HostPort") val hostPort: String)