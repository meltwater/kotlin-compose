package com.meltwater.docker.compose

import com.meltwater.docker.compose.data.ProcessResult
import com.meltwater.docker.compose.data.StreamReader
import org.apache.commons.lang3.SystemUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException

object ExecUtils {

    private val LOGGER: Logger = LoggerFactory.getLogger(ExecUtils::class.java)

    val NOOP_CONSUMER: (line: String) -> Unit = {}

    // Never run processes from here: their reader threads call lambdas compiled onto ExecUtils
    // and would block on this class's initialization, deadlocking with the thread waiting for them.
    init {
        if (SystemUtils.IS_OS_WINDOWS) {
            throw RuntimeException("Windows not supported by kotlin-compose")
        }
    }

    fun executeCommand(command: String): String {
        return executeCommand(command, hashMapOf(), NOOP_CONSUMER)
    }

    fun executeCommand(command: String, env: HashMap<String, String>, listener: (String) -> Unit): String {
        return execLocal(command, env, listener)
    }

    fun executeCommandAsync(command: String, env: HashMap<String, String>, stdOutListener: (String) -> Unit, stdErrListener: (String) -> Unit): ProcessWrapper {
        return execLocalAsync(command, env, stdOutListener, stdErrListener)
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

            val result = execProcessWithCapturedOutput(pb, listener)

            if (result.exitCode != 0) {
                LOGGER.info("Failed to execute command {}.\nstderr: {}\nstdout: {}", pb.command(), result.errorOutput, result.standardOutput)
                throw RuntimeException(result.errorOutput)
            } else {
                LOGGER.trace("stdout: {}", result)
            }
            return result.standardOutput
        } catch (e: IOException) {
            throw RuntimeException(e)
        } catch (e: InterruptedException) {
            throw RuntimeException(e)
        }
    }

    private fun execProcessWithCapturedOutput(pb: ProcessBuilder, listener: (String) -> Unit): ProcessResult {
        val process = pb.start()

        val stdoutReader = StreamReader(process.inputStream, listener)
        val stderrReader = StreamReader(process.errorStream) {}

        val stdoutThread = Thread(stdoutReader)
        val stderrThread = Thread(stderrReader)
        stdoutThread.start()
        stderrThread.start()

        val commandExitCode = process.waitFor()
        stderrThread.join()
        stdoutThread.join()
        val result = stdoutReader.getData()
        val errors = stderrReader.getData()

        return ProcessResult(commandExitCode, result, errors)
    }

    fun execLocalAsync(command: String, env: HashMap<String, String>, stdOutListener: (String) -> Unit, stdErrListener: (String) -> Unit): ProcessWrapper {
        val commands = ArrayList<String>()
        commands.add("sh")
        commands.add("-c")
        commands.add(command)

        val pb = ProcessBuilder(commands)
        pb.environment().putAll(env)

        LOGGER.info("Running: ${pb.command()} \n with env $env")
        return ProcessWrapper(pb, stdOutListener, stdErrListener)
    }
}
