# kotlin-compose
## 1.8.0 - 2020-12-03
### Changes
- Push only to Artifactory

## 1.7.0 - 2020-05-25
### Changes
- Upgrades version of kotlin to 1.3.72
- Upgrades version of gradle to 6.3
- Removes the non maintained travis build config

## 1.6.0 - 2018-07-04
### Changes
- Add ability to get environment variables set during object creation

## 1.5.1 - 2018-06-26
### Changes
- Fix compatibility issues from 1.5.0

## 1.5.0 - 2018-06-26
### Changes
- Add ability to attach to an existing container

## 1.4.0 - 2018-06-04
### Changes
- Now possible to run docker pull from within the DockerCompose class

## 1.3.1 - 2018-04-05
### Changes
- Adds back removed method in NetworkSettings. The method is now marked as deprecated but still there.

## 1.3.0 - 2018-04-05
### Changes
- Add support for host networking #8
- Uses kotlin v 1.2.31

## 1.2.1 - 2017-11-25
### Changes
- Adds scm, license and other project metadata to the generated pom file
- All usages of IOUtils.toString now uses utf-8 charset

## 1.2.0 - 2017-11-24
### Changes
- Uses kotlin version 1.1.60
- Updates a number of other dependencies. For example jackson, commons-lang3 and commons-io.
- Adds javadoc as an additional jar artifact
- Uses gradle 4.3.1

## 1.1.1 - 2016-10-03
### Changes
- Changed build logic. SNAPSHOT is not appended by gradle anymore.

## 1.1.0 - 2016-09-30
### Changes
- It is possible to forward the logs inside the docker containers out to slf4j
- Improvements to the build system and the documentation

## 1.0.0 - 2016-09-26
### Changes
- First public release.
