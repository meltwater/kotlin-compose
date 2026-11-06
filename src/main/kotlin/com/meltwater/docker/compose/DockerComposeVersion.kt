package com.meltwater.docker.compose

/** A `major.minor.patch` Docker Compose version, compared numerically. */
internal data class DockerComposeVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<DockerComposeVersion> {

    override fun compareTo(other: DockerComposeVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /** Parses `major.minor.patch`; throws if [version] is null or not of that form. */
        fun parse(version: String?): DockerComposeVersion {
            val parts = requireNotNull(version) { "No Docker Compose version" }.split(".")
            require(parts.size == 3) { "Not a major.minor.patch version: $version" }
            val numbers = parts.map { requireNotNull(it.toIntOrNull()) { "Not a major.minor.patch version: $version" } }
            return DockerComposeVersion(numbers[0], numbers[1], numbers[2])
        }
    }
}
