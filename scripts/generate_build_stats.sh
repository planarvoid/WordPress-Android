#!/bin/bash

set -e

## This should run after a successful assemble task and requires an APK in app/build/outputs/apk/...

FILE_BUILD_STATS='build-stats.txt'
FILE_DEPENDENCY_TREE='dependency-tree.txt'

## Download dependencies
curl -o ./scripts/lib/dex-method-counts.jar http://maven.int.s-cloud.net/service/local/repositories/snapshots/content/com/mihaip/dex-method-count/1.0.0-SNAPSHOT/dex-method-count-1.0.0-20170828.113408-1-sources.jar

## Calculate
APK_SIZE=$(stat --printf=%s app/build/outputs/apk/soundcloud-android-*.apk)
METHOD_COUNT=$(./scripts/lib/dex-method-counts app/build/outputs/apk/soundcloud-android-*.apk | grep "Overall method count: " | sed 's/Overall method count: //g')

## Write
rm -f ${FILE_BUILD_STATS}
printf "apksize:${APK_SIZE} \n" >> ${FILE_BUILD_STATS}
printf "methodcount:${METHOD_COUNT} \n" >> ${FILE_BUILD_STATS}

./gradlew -q :app:dependencies --configuration _devDebugCompile > ${FILE_DEPENDENCY_TREE}
