package com.meltwater.docker.compose

import org.apache.commons.lang3.SystemUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.*
import kotlin.text.Charsets.UTF_8

object ExecUtils {

    val LOGGER: Logger = LoggerFactory.getLogger(ExecUtils::class.java)

    private var dockerMachineInstance: String = "notNeededOnLinux"
    private var useDockerMachine: Boolean = false

    val NOOP_CONSUMER: (line: String) -> Unit = {}

    init {
        if (SystemUtils.IS_OS_MAC_OSX) {
            // Use Docker for MAC if installed
            if (File("/Applications/Docker.app/").exists()) {
                try {
                    execLocal("docker ps")
                } catch (ex: Exception) {
                    LOGGER.warn("Could not execute 'docker ps': " + ex.message!!.trim())
                    LOGGER.warn("Looks like you have Docker for Mac installed (/Applications/Docker.app) " +
                            "Please make sure the setup is finished and Docker is up and running.")

                    LOGGER.warn("Trying to use docker-machine vm instead")
                    dockerMachineInstance = findRunningDockerMachineInstance()
                    useDockerMachine = true
                }
            } else {
                dockerMachineInstance = findRunningDockerMachineInstance()
                useDockerMachine = true
            }
        } else if (SystemUtils.IS_OS_WINDOWS) {
            throw RuntimeException("Windows not supported by kotlin-compose")
        }
    }

    private fun findRunningDockerMachineInstance(): String {
        return execLocal("docker-machine ls").lines()
                .find { it.contains("Running") }
                ?.split(" ")?.get(0) ?: throw RuntimeException("Could not determine what docker-machine or local Docker For Mac instance to use. Make sure you have at least one up and running")
    }

    fun executeCommand(command: String): String {
        return executeCommand(command, hashMapOf(), NOOP_CONSUMER)
    }

    fun executeCommand(command: String, env: HashMap<String, String>, listener: (String) -> Unit): String {
        return execLocal(adjustForOs(command), env, listener)
    }

    fun execLocal(command: String): String {
        return execLocal(command, hashMapOf(), NOOP_CONSUMER)
    }

    fun execLocal(command: String, env: HashMap<String, String>, listener: (String) -> Unit): String {
        try {
            val commands = ArrayList<String>()
            commands.add("sh")
            commands.add("-c")

            commands.add(command)

            val pb = ProcessBuilder(commands)
            pb.environment().putAll(env)

            LOGGER.info("Running: ${pb.command()} \n with env $env")

            val process = pb.start()

            val result = collectAndLogResult(process.inputStream, listener)
            val errors = process.errorStream.readString()

            if (process.waitFor() != 0) {
                throw RuntimeException(errors)
            }
            return result
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

    private fun collectAndLogResult(inputStream: InputStream, listener: (String) -> Unit): String {
        val out = StringBuilder()
        val buf: BufferedReader = inputStream.bufferedReader(UTF_8)
        var line: String? = buf.readLine()
        do {
            if (line != null) {
                out.append(line).append("\n")
                listener(line)
            }
            line = buf.readLine()
        } while (line != null)
        return out.toString()
    }

    private fun adjustForOs(command: String): String {
        if (useDockerMachine) {
            return "eval $(docker-machine env $dockerMachineInstance --shell=bash) && $command"
        }
        return command
    }
}
