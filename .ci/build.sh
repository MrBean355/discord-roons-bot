#!/bin/bash
set -e

if [ "$TRAVIS_BRANCH" = "develop" ] && [ "$TRAVIS_PULL_REQUEST" != "true" ]; then
    ./gradlew --no-daemon clean downloadSoundFiles bootJar
else
    ./gradlew --no-daemon clean bootJar
fi
