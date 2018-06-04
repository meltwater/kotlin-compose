package com.meltwater.docker.compose

import com.meltwater.docker.compose.ExecUtils.NOOP_CONSUMER
import com.meltwater.docker.compose.ExecUtils.executeCommand
import com.meltwater.docker.compose.ExecUtils.executeCommandAsync
import com.meltwater.docker.compose.data.InspectData
import org.apache.commons.io.IOUtils
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset
import java.util.HashMap
import kotlin.concurrent.thread

/**
 * This class takes a path to a docker-compose yaml file and copies it to a temporary location when initialized.
 *
 * After initialization it is possible to start, stop, inspect and kill the containers that are configured in the yaml file.
 */
class DockerCompose(classPathYmlResource: String,
                    private val prefix: String,
                    private val env: HashMap<String, String>) {

    val LOGGER: Logger = LoggerFactory.getLogger(DockerCompose::class.java)

    val EXEC_INFO_LOGGER = { line: String ->
        LOGGER.info(line)
    }

    val STDOUT_LOG_CONSUMER = { line: String ->
        LOGGER.debug("OUT: " + line)
    }

    val STDERR_LOG_CONSUMER = { line: String ->
        LOGGER.info("ERR: " + line)
    }


    private var MIN_DOCKER_COMPOSE_VERSION: DefaultArtifactVersion = DefaultArtifactVersion("1.5.0")
    private var ymlTmpfilename: String

    init {
        ymlTmpfilename = saveResourceToTmpFolder(classPathYmlResource)

        if (!File(ymlTmpfilename).exists()) {
            throw RuntimeException("File $ymlTmpfilename could not be found.")
        }

        verifyDockerComposeInstallation()
    }

    @Suppress("UNCHECKED_CAST")
    private fun saveResourceToTmpFolder(classPathYmlResource: String): String {
        try {
            val ymlStream: InputStream = DockerCompose::class.java.classLoader.getResourceAsStream(classPathYmlResource)
            val tmpFile = File.createTempFile(prefix, ".yml")
            tmpFile.writeBytes(IOUtils.toString(ymlStream, Charset.forName("utf-8")).toByteArray())
            ymlStream.close()
            return tmpFile.path
        } catch (ex: Exception) {
            throw RuntimeException("Could not load yml file from classpath: $classPathYmlResource", ex)
        }
    }

    fun up(): List<InspectData> {
        exec("up -d", EXEC_INFO_LOGGER)
        val logCmd = execAsync("logs -f", STDOUT_LOG_CONSUMER, STDERR_LOG_CONSUMER)
        forwardDockerLog(logCmd)
        val ps: List<InspectData> = ps()
        val deadServices = ps.filter { it.state.dead || !it.state.running }
                .filter { !it.name.contains("puppy") }
        if (deadServices.isNotEmpty()) {
            throw RuntimeException("Failed to start up all containers, the dead services are: ${deadServices.map { it.name }}")
        }
        return ps
    }

    private fun forwardDockerLog(procLogger: ProcessWrapper) {
        thread {
            try {
                val exitCode = procLogger.waitFor()
                if (exitCode != 0) {
                    LOGGER.error("The 'logs -f' command finished with a non 0 exit code '{}'", exitCode)
                } else {
                    LOGGER.debug("The 'logs -f' command terminated successfully.")
                }
            } catch (e: Exception) {
                LOGGER.error("The 'logs -f' command threw an unexpected error", e)
            }
        }
    }

    fun pull() {
        exec("pull --ignore-pull-failures", EXEC_INFO_LOGGER)
    }

    fun build() {
        exec("build", EXEC_INFO_LOGGER)
    }

    fun kill() {
        exec("kill")
        var lastCheck: List<InspectData> = arrayListOf()
        fun anyoneStillUp(): Boolean {
            lastCheck = ps()
            return lastCheck.filter { it.state.running }.isNotEmpty()
        }
        while (anyoneStillUp()) {
            Thread.sleep(100)
            LOGGER.info("Waiting for all containers to die: $lastCheck")
        }
    }

    fun kill(containerName: String) {
        Docker.kill(containerName)
    }

    fun rm() {
        exec("rm --force")
    }

    fun ps(): List<InspectData> {
        val psResults = exec("ps -q", EXEC_INFO_LOGGER)
        val containerIDs: List<String> = psResults.lines().filter { it.isNotEmpty() }
        return Docker.inspect(*containerIDs.toTypedArray())
    }

    fun getPrefix(): String {
        return prefix
    }

    private fun exec(command: String): String {
        return executeCommand("docker-compose --project-name $prefix --file $ymlTmpfilename $command", env, NOOP_CONSUMER)
    }

    private fun exec(command: String, listener: (String) -> Unit): String {
        return executeCommand("docker-compose --project-name $prefix --file $ymlTmpfilename $command", env, listener)
    }

    private fun execAsync(command: String, stdOut: (String) -> Unit, stdErr: (String) -> Unit): ProcessWrapper {
        return executeCommandAsync("docker-compose --project-name $prefix --file $ymlTmpfilename $command", env, stdOut, stdErr)
    }

    private fun verifyDockerComposeInstallation() {
        try {
            val versionOutput = executeCommand("docker-compose --version")
            val v: MatchResult? = Regex(".* (\\d+\\.\\d+\\.\\d+)").find(versionOutput)
            val version = v?.groups?.get(1)?.value
            if (MIN_DOCKER_COMPOSE_VERSION.compareTo(DefaultArtifactVersion(version)) > 0) {
                throw RuntimeException("The installed docker-compose version should be at least $MIN_DOCKER_COMPOSE_VERSION but the one currently installed is $version")
            }
        } catch (e: Exception) {
            throw RuntimeException("Unable to determine the docker-compose version. " +
                    "Please make sure it's installed and available to the user. " +
                    "The version should be at least $MIN_DOCKER_COMPOSE_VERSION", e)
        }

    }
}
