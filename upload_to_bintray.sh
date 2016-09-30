#!/usr/bin/env bash
set -ex
if [ -n "${TRAVIS_TAG}" ]; then
    if [ -z "${BINTRAY_USER}" ]; then
        echo "BINTRAY_USER is unset or set to the empty string"
        exit 1
    fi
    if [ -z "${BINTRAY_KEY}" ]; then
        echo "BINTRAY_KEY is unset or set to the empty string"
        exit 1
    fi
    SNAPSHOT=false
	./gradlew bintray
else
    echo "Will not upload to Bintray."
fi
