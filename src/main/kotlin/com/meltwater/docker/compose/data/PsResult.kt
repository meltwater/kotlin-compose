package com.meltwater.docker.compose.data

class PsResult(prefix: String, private val rawResult: List<InspectData>) {

    private val result: Map<String, InspectData> = parse(prefix, rawResult)

    fun getData(shortName: String): InspectData {
        return result[shortName] ?: throw IllegalArgumentException("shortName not found: $shortName")
    }

    fun getContainerName(shortName: String): String {
        return getData(shortName).name
    }

    fun asList(): List<InspectData> {
        return rawResult
    }

    companion object {
        private fun parse(prefix: String, rawResult: List<InspectData>): Map<String, InspectData> {
            val result = HashMap<String, InspectData>()
            val shortNameRegex = Regex("${prefix}[-_](.*)[-_](\\d+)")

            rawResult.forEach {
                val shortName = parseShortName(shortNameRegex, it.name)
                if (result.contains(shortName)) {
                    throw IllegalStateException("Spawned two containers with the same shortName: $shortName")
                }
                result[shortName] = it
            }

            return result
        }

        private fun parseShortName(shortNameRegex: Regex, containerName: String): String {
            val matchResult = shortNameRegex.matchEntire(containerName)
                ?: throw IllegalStateException("Failed to parse short name for container $containerName")

            val name = matchResult.groupValues[1]
            val index = matchResult.groupValues[2]
            return "$name-$index"
        }
    }

}
