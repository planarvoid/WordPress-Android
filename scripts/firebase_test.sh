#!/usr/bin/env bash
set -e

echo "Running tests on Firebase..."

usage() {
  cat <<EOF
Pass a test class or a single test to run them on firebase
Samples:
    ./firebase_test.sh com.soundcloud.android.tests.discovery.SystemPlaylistTest (runs all tests in that class)
    ./firebase_test.sh com.soundcloud.android.tests.discovery.SystemPlaylistTest#testSystemPlaylistPlayback (runs that single test)
EOF
  exit 1
}

verify_inputs() {
    if [[ "$#" -ne 1 ]]
    then
        echo "Missing parameters"
        usage
    fi
}

verify_inputs $#

CLASS=$1
gcloud firebase test android run \
      --use-orchestrator \
      --type instrumentation \
      --app app/build/outputs/apk/app-dev-debug.apk \
      --test app/build/outputs/apk/app-prod-debug-androidTest.apk \
      --device model=Nexus5,version=23,locale=en,orientation=portrait \
      --test-targets="class ${CLASS}"
