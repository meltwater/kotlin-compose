package com.meltwater.docker.compose

enum class PullPolicy(val commandLine: String) {
    ALWAYS("always"),
    MISSING("missing"),
    NEVER("never"),
}