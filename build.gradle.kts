import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI
import java.net.URL

plugins {
    groovy
    kotlin("jvm") version "1.5.31"
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.5.30"
}

project.group = "com.meltwater.docker"

java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.slf4j:slf4j-api:1.7.32")
    implementation("com.fasterxml.jackson.core:jackson-core:2.12.4")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.4")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.maven:maven-artifact:3.8.2")

    testImplementation("org.spockframework:spock-core:2.0-groovy-3.0")
    testImplementation("ch.qos.logback:logback-classic:1.2.6")
}

tasks {
    register("version") {
        println(project.version)
    }

    register<Jar>("dokkaJavadocJar") {
        archiveClassifier.set("javadoc")
        from(dokkaJavadoc.get().outputDirectory)
        dependsOn(dokkaJavadoc)
    }

    register<Jar>("sourceJar") {
        from(sourceSets.main.get().allJava)
        archiveClassifier.set("sources")
    }

    dokkaJavadoc<DokkaTask> {
        dokkaSourceSets.configureEach {
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/meltwater/kotlin-compose"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}

artifacts {
    archives(tasks.named<Jar>("sourceJar"))
    archives(tasks.named<Jar>("dokkaJavadocJar"))
}

companion object ArtifactoryConstants {
    private const val ARTIFACTORY_URL = "https://meltwater.jfrog.io/meltwater"
    const val REPO_URL = "$ARTIFACTORY_URL/team-horace"
}

var userName = ""
var passWord = ""

if (project.hasProperty("artifactory_user")) {
    userName = "${properties["artifactory_user"]}"
    passWord = "${properties["artifactory_password"]}"
} else {
    userName = System.getProperty("artifactory_user")
    passWord = System.getProperty("artifactory_password")
}

publishing {
    //Defines Maven BOM as valid publication
    publications {
        register<MavenPublication>("kotlinCompose") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()

            from(components["java"])

            artifact(tasks.named<Jar>("sourceJar"))
            artifact(tasks.named<Jar>("dokkaJavadocJar"))
        }
    }
    repositories {
        maven {
            name = "artifactory"
            url = URI(ArtifactoryConstants.REPO_URL)
            credentials {
                username = userName
                password = passWord
            }
        }
    }
}
