#!/bin/bash

STORE_PW=$(pass IT/Android-PKS/store)
PSTOR_PW=$(pass IT/Android-PKS/store/keys/pstor)

# see https://docs.gradle.org/current/userguide/build_environment.html#sec:project_properties
export ORG_GRADLE_PROJECT_store_password="$STORE_PW"
export ORG_GRADLE_PROJECT_pstor_password="$PSTOR_PW"

./gradlew :app:bundleRelease
