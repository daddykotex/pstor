#!/bin/bash

#env vars are loaded via .envrc

./gradlew -Pstore_password="$STORE_PW" -Ppstor_password="$PSTOR_PW" :app:bundleRelease
