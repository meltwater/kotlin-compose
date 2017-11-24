package com.meltwater.docker.compose

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ProcessWrapper(val pb: ProcessBuilder, stdOutListener: (String) -> Unit, stdErrListener: (String) -> Unit) {

    val LOGGER: Logger = LoggerFactory.getLogger(ExecUtils::class.java)

    val process: Process
    val stdOutReader: Thread
    val stdErrReader: Thread
    var exitCode: Int = -1

    init {
        process = pb.start()
        stdOutReader = readThread("OUT:", process.inputStream, stdOutListener)
        stdErrReader = readThread("ERR:", process.errorStream, stdErrListener)
        //Add a shutdown hook so that if the JVM is stopped the os process is also terminated
        Runtime.getRuntime().addShutdownHook(Thread() {
            stop(5)
        })
    }

    fun waitFor(): Int {
        return process.waitFor()
    }

    fun stop(timeoutSecs: Long) {
        if (isRunning()) {
            process.destroyForcibly()
            if (process.waitFor(timeoutSecs, TimeUnit.SECONDS)) {
                exitCode = process.exitValue()
            } else {
                exitCode = -1
            }
        }
    }

    fun isRunning(): Boolean {
        return process.isAlive
    }

    private fun readThread(prefix: String, stream: InputStream, listener: (String) -> Unit): Thread {
        return thread {
            try {
                val buf: BufferedReader = stream.bufferedReader()
                var line: String? = buf.readLine()
                do {
                    if (line != null) {
                        listener("$line")
                    }
                    line = buf.readLine()
                } while (line != null)
            } catch (e: Exception) {
                LOGGER.warn("$prefix Unexpected IO error.", e.message)
            }
        }
    }

    fun command(): MutableList<String>? {
        return pb.command()
    }
}