#!/bin/bash

set -o nounset
set -o errexit
set -o pipefail

# Runs dev builds and prints how long they take. Used to see how a change will impact developer build speed.
# Note that to get reliable data you should kill all non-critical processes on your machine before running this
# script.

function create_or_update_foo_dot_java() {
  echo "package com.soundcloud.android; class Foo { String s =\"`date`\"; }" >  app/src/main/java/com/soundcloud/android/Foo.java
}

function update_foo_dot_java() {
  echo "Make one-line change"
  create_or_update_foo_dot_java
}

# Lets the system cool off between builds.
function rest() {
  sleep 5s
}

# Builds and prints the specified message and runtime.
function build() {
  echo -n "Build "
  TIMEFORMAT="(%0lR)"; time ./gradlew :app:assembleDevDebug &> /dev/null
  rest
}

# Cleans and kills the Daemon to ensure no state leaks between runs.
function clean_and_kill_daemon() {
  echo "Kill Daemon"
  ./gradlew --stop &> /dev/null
  echo "Clean"
  ./gradlew clean &> /dev/null
  rest
}

function clean() {
  echo -n "Clean "
  TIMEFORMAT="(%0lR)"; time ./gradlew clean &> /dev/null
  rest
}

# Cleans up after this script finishes.
function finish() {
  rm -f app/src/main/java/com/soundcloud/android/Foo.java
}

trap finish EXIT

create_or_update_foo_dot_java # Create a new class that we can modify below to simulate a one-line change.
clean_and_kill_daemon
build
clean
build
build                        # Not a noop due to a bug in the Android plugin.
build                        # Should be a noop.
update_foo_dot_java          # One line change.
build
