#!/bin/bash

set -e
set -u
set -o pipefail

## This should run after a successful assemble task and requires an APK in app/build/outputs/apk/...

FILE_BUILD_STATS='build-stats.txt'
FILE_DEPENDENCY_TREE='dependency-tree.txt'
FILE_MASTER_BUILD_STATS='artifacts/build-stats.txt'
FILE_MASTER_DEPENDENCY_TREE='artifacts/dependency-tree.txt'
FILE_STATS='stats.txt'
FILE_DIFF='diff.txt'

## $1 file
## $2 value name
function getValueFromFile {
   LINE=$(cat $1 | grep $2)
   IFS=: read -ra pair <<< "${LINE}"
   echo "${pair[1]:=0}"
}

function formatFileSize {
    awk "BEGIN {printf \"%.4fMB\n\", $1 / 1000000}"
}

function formatPercentage {
    perc=`awk "BEGIN {printf \"%.1f\n\", $1}"`
    echo ${perc}%%
}

## $1 value
## $2 total (denominator)
function percentage {
    if [ $2 -gt 0 ]
    then
      awk "BEGIN {printf \"%.2f\n\", ($1*100 / $2)}"
    else
      awk "BEGIN {printf \"0\n\"}"
    fi
}

# $1 metric name
# $2 master value
# $3 local value
# $4 diff value
# $5 value to consider whether printing should happen
function writeToStatsTable {
    SHOULD_PRINT=`bc <<< "$5 != 0"`
    if [ ${SHOULD_PRINT} -eq 1 ]; then
        echo $(printf "| **$1** | $2 | $3 | $4 | \n")
    fi
}

function apkSize {
    MASTER=$(getValueFromFile ${FILE_MASTER_BUILD_STATS} apksize)
    LOCAL=$(getValueFromFile ${FILE_BUILD_STATS} apksize)
    DIFF=$((${LOCAL} - ${MASTER}))

    MESSAGE_MASTER=$(formatFileSize ${MASTER})
    MESSAGE_BRANCH=$(formatFileSize ${LOCAL})
    MESSAGE_DIFF=$(formatFileSize ${DIFF})

    writeToStatsTable "APK Size" ${MESSAGE_MASTER} ${MESSAGE_BRANCH} ${MESSAGE_DIFF} ${DIFF}
}

## $1 original metric key
## $2 original metric name
## $3 new metric key
## $4 new metric name
## $5 percentage diff name
function statsForMigration {
    MASTER_ORIG_METRIC=$(getValueFromFile ${FILE_MASTER_BUILD_STATS} $1)
    MASTER_NEW_METRIC=$(getValueFromFile ${FILE_MASTER_BUILD_STATS} $3)
    LOCAL_ORIG_METRIC=$(getValueFromFile ${FILE_BUILD_STATS} $1)
    LOCAL_NEW_METRIC=$(getValueFromFile ${FILE_BUILD_STATS} $3)
    MASTER_PERCENTAGE=$(percentage ${MASTER_NEW_METRIC} $(($MASTER_ORIG_METRIC+$MASTER_NEW_METRIC)))
    LOCAL_PERCENTAGE=$(percentage ${LOCAL_NEW_METRIC} $(($LOCAL_ORIG_METRIC+$LOCAL_NEW_METRIC)))
    DIFF_ORIG_METRIC=$((${LOCAL_ORIG_METRIC} - ${MASTER_ORIG_METRIC}))
    DIFF_NEW_METRIC=$((${LOCAL_NEW_METRIC} - ${MASTER_NEW_METRIC}))
    DIFF_PERCENTAGE=`bc <<< ${LOCAL_PERCENTAGE}-${MASTER_PERCENTAGE}`

    MASTER_PERCENTAGE_FORMATTED=$(formatPercentage ${MASTER_PERCENTAGE})
    LOCAL_PERCENTAGE_FORMATTED=$(formatPercentage ${LOCAL_PERCENTAGE})
    DIFF_PERCENTAGE_FORMATTED=$(formatPercentage ${DIFF_PERCENTAGE})

    writeToStatsTable "$2" ${MASTER_ORIG_METRIC} ${LOCAL_ORIG_METRIC} ${DIFF_ORIG_METRIC} ${DIFF_ORIG_METRIC}
    writeToStatsTable "$4" ${MASTER_NEW_METRIC} ${LOCAL_NEW_METRIC} ${DIFF_NEW_METRIC} ${DIFF_NEW_METRIC}
    writeToStatsTable "$5" ${MASTER_PERCENTAGE_FORMATTED} ${LOCAL_PERCENTAGE_FORMATTED} ${DIFF_PERCENTAGE_FORMATTED} ${DIFF_PERCENTAGE}
}

function rxJavaMigration {
    statsForMigration rxjava "RxJava imports" rx2 "RxJava2 imports" "RxJava2 %%"
}

function kotlinMigration {
    statsForMigration javafiles "Java files" kotlinfiles "Kotlin files" "Kotlin %%"
}

function uniflowMigration {
    statsForMigration oldarchitecture "Old arch. presenters" uniflow "Uniflow fragments" "Uniflow %%"
}

function methodCount {
    MASTER=$(getValueFromFile ${FILE_MASTER_BUILD_STATS} methodcount)
    LOCAL=$(getValueFromFile ${FILE_BUILD_STATS} methodcount)
    DIFF=$((${LOCAL} - ${MASTER}))

    writeToStatsTable "Method Count" ${MASTER} ${LOCAL} ${DIFF} ${DIFF}
}

DIFF=$(git diff --no-index ${FILE_MASTER_DEPENDENCY_TREE} ${FILE_DEPENDENCY_TREE} | cat || true)
WORDCOUNT=$(echo ${DIFF} | wc -c)
if [[ ${WORDCOUNT} -gt 1 ]]; then
    rm -f ${FILE_DIFF}
    printf "\`\`\`diff \n" >> $FILE_DIFF
    printf "$DIFF \n" >> $FILE_DIFF
    printf "\`\`\`" >> $FILE_DIFF
    # Disabled until we can use a github material in GoCD to get the PR ID
    #./scripts/github/create_github_comment.sh ${GO_SCM_ANDROID_PR_ID} "`cat $FILE_DIFF`"
fi

rm -f $FILE_STATS
printf "| Metric | master | $(git rev-parse --abbrev-ref HEAD) | diff | \n" >> ${FILE_STATS}
printf "| ------ | ------ | ------------------ | ---- | \n" >> ${FILE_STATS}
apkSize >> ${FILE_STATS}
methodCount >> ${FILE_STATS}
rxJavaMigration >> ${FILE_STATS}
kotlinMigration >> ${FILE_STATS}
uniflowMigration >> ${FILE_STATS}

# Disabled until we can use a github material in GoCD to get the PR ID
#./scripts/github/create_github_comment.sh ${GO_SCM_ANDROID_PR_ID} "`cat $FILE_STATS`"
