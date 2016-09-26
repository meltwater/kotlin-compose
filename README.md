# Overview

Docker-compose-java is a [docker-compose](https://docs.docker.com/compose/) wrapper for the JVM.
This library is mainly intended to be used in embedded integration tests. 

![Build Status](https://travis-ci.com/meltwater/docker-compose-java.svg?token=4xk1R5pGndzXY3VX7h8C&branch=master)

## How to use it

### Dependencies

*Gradle:*

```groovy
    
repositories {
   jcenter()
}
...

dependencies {
    compile 'com.meltwater.docker:docker-compose-java:<insert-version>'
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

<dependency>
  <groupId>com.meltwater.docker</groupId>
  <artifactId>docker-compose-java</artifactId>
  <version><insert-version></version>
  <type>jar</type>
  ...
</dependency>
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

    SNAPSHOT=true MAVEN_USER=[USER] MAVEN_PASSWORD=[PASSWORD] MAVEN_URL=[MAVEN_REPO]repositories/[REPOSITORY] ./gradlew clean build publishJavaArtifactsPublicationToMavenRepository
    
To upload a non-snapshot version, omit the snapshot env variable.

### License
[MIT](LICENSE.txt)

