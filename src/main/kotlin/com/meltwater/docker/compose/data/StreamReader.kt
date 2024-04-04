package com.meltwater.docker.compose.data

import java.io.BufferedReader
import java.io.InputStream
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class StreamReader(private val stream: InputStream, val listener: (String) -> Unit) : Runnable {

    val out = StringBuilder()
    val completed = AtomicBoolean(false)

    override fun run() {
        val buf: BufferedReader = stream.bufferedReader(Charsets.UTF_8)
        var line: String? = buf.readLine()
        do {
            if (line != null) {
                out.append(line).append("\n")
                listener(line)
            }
            line = buf.readLine()
        } while (line != null)
        completed.set(true)
    }

    fun getData(): String {
        if (!completed.get()) {
            throw RuntimeException("Data incomplete")
        }
        return out.toString()
    }
}