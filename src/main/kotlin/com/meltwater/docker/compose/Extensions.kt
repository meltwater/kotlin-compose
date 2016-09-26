package com.meltwater.docker.compose

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.stream.Collectors

fun InputStream.readString(): String {
    val reader = BufferedReader(InputStreamReader(this))
    return reader.lines().collect(Collectors.joining(
            System.getProperty("line.separator"))
    )
}
