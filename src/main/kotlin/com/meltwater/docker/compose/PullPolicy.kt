package com.meltwater.docker.compose

enum class PullPolicy(val commandLine: String) {
    DEFAULT(""),
    ALWAYS("always"),
    MISSING("missing"),
    NEVER("never")
}