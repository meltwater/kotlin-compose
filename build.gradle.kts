import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URI
import java.net.URL

plugins {
    kotlin("jvm") version "2.3.21"
    id("maven-publish")
    id("org.jetbrains.dokka") version "2.0.0"
}

project.group = "com.meltwater.docker"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    all {
        resolutionStrategy {
            eachDependency {
                // Align with platform-common-dependencies. jackson-annotations is versioned without a patch number since 2.20
                if(requested.group.startsWith("com.fasterxml.jackson") && requested.name != "jackson-annotations")
                    useVersion("2.21.7")
            }
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.slf4j:slf4j-api:2.0.20")
    implementation("com.fasterxml.jackson.core:jackson-core:2.21.7")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.7")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("commons-io:commons-io:2.21.0")
    implementation("org.apache.maven:maven-artifact:3.8.2")

    testImplementation("ch.qos.logback:logback-classic:1.5.38")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.3")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
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
        from(sourceSets.main.get().allSource)
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

    // This is needed due to Jenkins using "artifactoryPublish" in scripts instead of publish.
    register("artifactoryPublish") {
        dependsOn(publish)
    }

    test {
        useJUnitPlatform()
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

repositories {
    mavenCentral()
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
