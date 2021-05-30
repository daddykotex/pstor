#!/bin/bash

STORE_PW=$(pass IT/Android-PKS/store)
PSTOR_PW=$(pass IT/Android-PKS/store/keys/pstor)
./gradlew --stacktrace -Pstore.password="$STORE_PW" -Ppstor.password="$PSTOR_PW" :app:bundleRelease
