package com.meltwater.docker.compose

import com.meltwater.docker.compose.ExecUtils.NOOP_CONSUMER
import com.meltwater.docker.compose.ExecUtils.executeCommand
import com.meltwater.docker.compose.ExecUtils.executeCommandAsync
import com.meltwater.docker.compose.Recreate.DEFAULT
import com.meltwater.docker.compose.data.InspectData
import com.meltwater.docker.compose.data.PsResult
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.regex.Pattern
import java.util.stream.Collectors
import kotlin.concurrent.thread

/**
 * This class takes a path to a docker-compose yaml file and copies it to a temporary location when initialized.
 *
 * After initialization it is possible to start, stop, inspect and kill the containers that are configured in the yaml file.
 */
class DockerCompose private constructor(
    prefix: String,
    private val env: HashMap<String, String>,
    private val dockerComposeFilename: String
) {

    // docker-compose 2.2.2 seems to use lowercase internally and have issues with uppercase
    private val prefix = prefix.lowercase()

    companion object {

        private val LOGGER: Logger = LoggerFactory.getLogger(DockerCompose::class.java)

        private val EXEC_INFO_LOGGER = { line: String ->
            LOGGER.info(line)
        }

        private val STDOUT_LOG_CONSUMER = { line: String ->
            LOGGER.debug("OUT: $line")
        }

        private val STDERR_LOG_CONSUMER = { line: String ->
            LOGGER.info("ERR: $line")
        }

        private val MIN_DOCKER_COMPOSE_VERSION: DefaultArtifactVersion = DefaultArtifactVersion("1.5.0")

        private const val YAML_FILE_EXTENSION = ".yml"

        private const val ENVIRONMENT_FILE_EXTENSION = ".env"

        private val ENVIRONMENT_VARIABLE_SEPARATOR = Pattern.compile("=")

        private fun initializeNewDockerComposeSession(classPathYmlResource: String, env: HashMap<String, String>): String {
            val composeFile = File.createTempFile(classPathYmlResource, YAML_FILE_EXTENSION)
            val dockerComposeFilename = composeFile.absolutePath
            saveResourceToTmpFolder(classPathYmlResource, composeFile)
            if (!composeFile.exists()) {
                throw RuntimeException("File $dockerComposeFilename could not be found.")
            }
            val environmentFileName = dockerComposeFilename.substringBeforeLast(YAML_FILE_EXTENSION) + ENVIRONMENT_FILE_EXTENSION
            saveEnvironmentFile(env, environmentFileName)
            return dockerComposeFilename
        }

        private fun saveResourceToTmpFolder(classPathYmlResource: String, destinationFile: File) {
            try {
                DockerCompose::class.java.classLoader.getResourceAsStream(classPathYmlResource).copyTo(destinationFile.outputStream())
            } catch (ex: Exception) {
                throw RuntimeException("Could not load yml file from classpath: $classPathYmlResource", ex)
            }
        }

        private fun saveEnvironmentFile(env: HashMap<String, String>, environmentFileName: String) {
            val environmentPath = Paths.get(environmentFileName)
            val environment = env.entries.joinToString("\n") { "${it.key}=${it.value}" }
            Files.write(environmentPath, environment.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE, StandardOpenOption.CREATE)
        }

        private fun loadEnvironmentFile(fileNamePrefix: String): HashMap<String, String> {
            val environmentPath = Paths.get(fileNamePrefix + ENVIRONMENT_FILE_EXTENSION)
            if (!(Files.exists(environmentPath) && Files.isRegularFile(environmentPath))) {
                LOGGER.warn("Cannot find the environment file: $environmentPath")
                return hashMapOf()
            }
            val env: Map<String, String> = Files.lines(environmentPath)
                    .map { it.trim() }
                    .filter { !it.startsWith("#") }
                    .filter { it.isNotBlank() }
                    .map { it.split(ENVIRONMENT_VARIABLE_SEPARATOR, 2) }
                    .filter { it.size == 2 }
                    .collect(Collectors.toMap({ it[0] }, { it[1] }))
            return HashMap(env)
        }

    }

    init {
        verifyDockerComposeInstallation()
    }

    constructor(classPathYmlResource: String, applicationPrefix: String, env: HashMap<String, String>) : this(applicationPrefix, env, initializeNewDockerComposeSession(classPathYmlResource, env))

    constructor(applicationPrefix: String, fileNamePrefix: String) : this(applicationPrefix, loadEnvironmentFile(fileNamePrefix), fileNamePrefix + YAML_FILE_EXTENSION)

    constructor(classPathYmlResource: String, applicationPrefix: String, fileNamePrefix: String, env: HashMap<String, String>) : this(applicationPrefix, env, fileNamePrefix + YAML_FILE_EXTENSION) {
        saveResourceToTmpFolder(classPathYmlResource, File(fileNamePrefix + YAML_FILE_EXTENSION))
        saveEnvironmentFile(env, fileNamePrefix + ENVIRONMENT_FILE_EXTENSION)
    }

    fun up(): PsResult = up(Recreate.DEFAULT)

    fun up(recreate: Recreate = DEFAULT): PsResult {
        exec("up -d ${recreate.commandLine}", EXEC_INFO_LOGGER)
        val logCmd = execAsync("logs -f", STDOUT_LOG_CONSUMER, STDERR_LOG_CONSUMER)
        forwardDockerLog(logCmd)
        val ps: PsResult = ps()
        val deadServices = ps.asList().filter { it.state.dead || !it.state.running }
                .filter { !it.name.contains("puppy") }
        if (deadServices.isNotEmpty()) {
            throw RuntimeException("Failed to start up all containers, the dead services are: ${deadServices.map { it.name }}")
        }
        return ps
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
            lastCheck = ps().asList()
            return lastCheck.any { it.state.running }
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

    fun ps(): PsResult {
        val psResults = exec("ps -q", EXEC_INFO_LOGGER)
        val containerIDs: List<String> = psResults.lines().filter { it.isNotEmpty() }
        val inspectResult = Docker.inspect(*containerIDs.toTypedArray())
        return PsResult(prefix, inspectResult)
    }

    fun getPrefix(): String {
        return prefix
    }

    fun getEnv(name: String): String? {
        return env[name]
    }

    private fun exec(command: String): String {
        return executeCommand("docker-compose --project-name $prefix --file $dockerComposeFilename $command", env, NOOP_CONSUMER)
    }

    private fun exec(command: String, listener: (String) -> Unit): String {
        return executeCommand("docker-compose --project-name $prefix --file $dockerComposeFilename $command", env, listener)
    }

    private fun execAsync(command: String, stdOut: (String) -> Unit, stdErr: (String) -> Unit): ProcessWrapper {
        return executeCommandAsync("docker-compose --project-name $prefix --file $dockerComposeFilename $command", env, stdOut, stdErr)
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

    private fun verifyDockerComposeInstallation() {
        try {
            val versionOutput = executeCommand("docker-compose --version")
            val v: MatchResult? = Regex(".* (\\d+\\.\\d+\\.\\d+)").find(versionOutput)
            val version = v?.groups?.get(1)?.value
            if (MIN_DOCKER_COMPOSE_VERSION > DefaultArtifactVersion(version)) {
                throw RuntimeException("The installed docker-compose version should be at least $MIN_DOCKER_COMPOSE_VERSION but the one currently installed is $version")
            }
        } catch (e: Exception) {
            throw RuntimeException("Unable to determine the docker-compose version. " +
                    "Please make sure it's installed and available to the user. " +
                    "The version should be at least $MIN_DOCKER_COMPOSE_VERSION", e)
        }

    }
}
