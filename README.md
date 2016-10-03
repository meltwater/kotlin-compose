# Overview

Kotlin-compose is a [docker-compose](https://docs.docker.com/compose/) wrapper for the JVM.
This library is mainly intended to be used in embedded integration tests. 

[![Build Status](https://travis-ci.org/meltwater/kotlin-compose.svg?branch=master)](https://travis-ci.org/meltwater/kotlin-compose)
[![jcenter](https://api.bintray.com/packages/meltwater/opensource/kotlin-compose/images/download.svg) ](https://bintray.com/meltwater/opensource/kotlin-compose/_latestVersion)

## How to use it

### Dependencies

*Gradle:*

```groovy
    
repositories {
   jcenter()
}
...

dependencies {
    compile 'com.meltwater.docker:kotlin-compose:$VERSION'
    ...
}
```
       
*Maven:*

```xml  
<repositories>
   <repository>
      <id>jcenter</id>
      <url>http://jcenter.bintray.com</url>
      <snapshots>
        <enabled>true</enabled>
        <updatePolicy>never</updatePolicy>
        <checksumPolicy>warn</checksumPolicy>
      </snapshots>
       <releases>
         <enabled>true</enabled>
         <checksumPolicy>warn</checksumPolicy>
      </releases>
   </repository>
   ...
</repositories>
...

<dependencies>
    <dependency>
      <groupId>com.meltwater.docker</groupId>
      <artifactId>kotlin-compose</artifactId>
      <version>$VERSION</version>
      <type>jar</type>
    </dependency>
    ...
</dependencies>    
```

### Java code example
```java
// create the docker compose context giving a path to a docker compose yaml file, a machine uniqe prefix and 
// optinally provide some ENVIRONMENT variables that will be resolved in the yaml file
Map<String,String> env = new HashMap<>();
DockerCompose compose = new DockerCompose("docker-compose.yml", "uniqe-namespace", env);

// start the docker containers (note this method will return as soon as all containers are running)
compose.up();

// verify that all services inside the containers has started

// do your tests

// kill the containers (this method reurns when all containers has stopped)
compose.kill();
```


For more examples have a look at [the unit tests](src/test)


### Building locally

    ./gradlew clean build
    
Install to your local .m2 directory:

    ./gradlew clean build publishJavaArtifactsPublicationToMavenLocal
 
### Upload to jcenter

If you're an authorized owner of the project, you may upload a released version to Bintray.

    BINTRAY_USER=[USER] BINTRAY_KEY=[API_KEY] ./gradlew clean build bintray
    
### Upload to Nexus

Upload to custom Nexus repository:

    MAVEN_USER=[USER] MAVEN_PASSWORD=[PASSWORD] MAVEN_URL=[MAVEN_REPO]repositories/[REPOSITORY] ./gradlew clean build publishJavaArtifactsPublicationToMavenRepository
    
### License
[MIT](LICENSE.txt)

