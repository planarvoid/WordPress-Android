#!/bin/bash

set -e

## This should run after a successful assemble task and requires an APK in app/build/outputs/apk/...

FILE_BUILD_STATS='build-stats.txt'
FILE_DEPENDENCY_TREE='dependency-tree.txt'

APK_SIZE=`stat --printf=%s app/build/outputs/apk/soundcloud-android-*.apk`

echo "apksize:${APK_SIZE}" > ${FILE_BUILD_STATS}

./gradlew -q :app:dependencies --configuration _devDebugCompile > ${FILE_DEPENDENCY_TREE}
