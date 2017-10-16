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
RX_JAVA_COUNT=`./scripts/occurrences_count.sh "import rx."`
RX_JAVA_2_COUNT=`./scripts/occurrences_count.sh "import io.reactivex."`
JAVA_FILES_COUNT=`./scripts/occurrences_count_files.sh "java"`
KOTLIN_FILES_COUNT=`./scripts/occurrences_count_files.sh "kt"`

OLD_ARCHITECTURE_PRESENTER_CHILDREN_JAVA=`./scripts/occurrences_count.sh "extends RecyclerViewPresenter" "app/"`
OLD_ARCHITECTURE_PRESENTER_CHILDREN_KT=`./scripts/occurrences_count.sh ": RecyclerViewPresenter" "app/"`
OLD_ARCHITECTURE_PRESENTER_CHILDREN=`bc <<< ${OLD_ARCHITECTURE_PRESENTER_CHILDREN_JAVA}+${OLD_ARCHITECTURE_PRESENTER_CHILDREN_KT}`

BASE_FRAGMENT_CHILDREN_JAVA=`./scripts/occurrences_count.sh "extends BaseFragment" "app/"`
BASE_FRAGMENT_CHILDREN_KT=`./scripts/occurrences_count.sh ": BaseFragment" "app/"`
BASE_FRAGMENT_CHILDREN=`bc <<< ${BASE_FRAGMENT_CHILDREN_JAVA}+${BASE_FRAGMENT_CHILDREN_KT}`

## Write
rm -f ${FILE_BUILD_STATS}
printf "apksize:${APK_SIZE} \n" >> ${FILE_BUILD_STATS}
printf "methodcount:${METHOD_COUNT} \n" >> ${FILE_BUILD_STATS}
printf "rxjava:${RX_JAVA_COUNT} \n" >> ${FILE_BUILD_STATS}
printf "rx2:${RX_JAVA_2_COUNT} \n" >> ${FILE_BUILD_STATS}
printf "javafiles:${JAVA_FILES_COUNT} \n" >> ${FILE_BUILD_STATS}
printf "kotlinfiles:${KOTLIN_FILES_COUNT} \n" >> ${FILE_BUILD_STATS}
printf "oldarchitecture:${OLD_ARCHITECTURE_PRESENTER_CHILDREN} \n" >> ${FILE_BUILD_STATS}
printf "uniflow:${BASE_FRAGMENT_CHILDREN} \n" >> ${FILE_BUILD_STATS}

./gradlew -q :app:dependencies --configuration _devDebugCompile > ${FILE_DEPENDENCY_TREE}
