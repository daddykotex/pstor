#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

set -eo pipefail

#env vars are loaded via .envrc
/usr/bin/open -a "/Applications/Android Studio.app" "$SCRIPT_DIR/.."
