package com.meltwater.docker.compose.data

data class ProcessResult(val exitCode: Int, val standardOutput: String, val errorOutput: String)