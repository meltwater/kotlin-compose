package com.meltwater.docker.compose.data

data class InspectData(
        val id: String,
        val name: String,
        val state: ContainerState,
        val networkSettings: NetworkSettings)

data class ContainerState(val running: Boolean, val dead: Boolean)

data class NetworkSettings(val ports: Map<String, Iterable<PortBinding>>) {
    /**
     * Finds a single binding for an internal TCP port
     */
    fun bindingForTcpPort(internalPort: String): String {
        val hostPort = ports["$internalPort/tcp"]?.first()?.hostPort ?:
                throw RuntimeException("Port mapping not found for internalPort $internalPort. Available ports are: $ports")
        return hostPort
    }
}

data class PortBinding(val hostIp: String, val hostPort: String)