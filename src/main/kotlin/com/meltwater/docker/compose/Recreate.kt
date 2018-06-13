package com.meltwater.docker.compose

enum class Recreate(val commandLine: String) {
    DEFAULT(""),
    FORCE("--force-recreate"),
    NONE("--no-recreate")
}
