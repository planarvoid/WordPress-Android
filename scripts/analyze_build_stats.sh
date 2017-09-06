#!/bin/bash

set -e
set -u
set -o pipefail

## This should run after a successful assemble task and requires an APK in app/build/outputs/apk/...

FILE_BUILD_STATS='build-stats.txt'
FILE_DEPENDENCY_TREE='dependency-tree.txt'
FILE_MASTER_BUILD_STATS='master-build-stats.txt'
FILE_MASTER_DEPENDENCY_TREE='master-dependency-tree.txt'
FILE_STATS='stats.txt'
FILE_DIFF='diff.txt'

MASTER_URL="http://mobile-jenkins.int.s-cloud.net/job/Android_Listener_Master_Pipeline/lastSuccessfulBuild/artifact"

## $1 file
## $2 value name
function getValueFromFile {
   LINE=$(cat $1 | grep $2)
   IFS=: read -ra pair <<< "${LINE}"
   echo "${pair[1]:=0}"
}

function formatFileSize {
    awk "BEGIN {printf \"%.3fMB\n\", $1 / 1000000}"
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

function apkSize {
    MASTER=$(getValueFromFile ${FILE_MASTER_BUILD_STATS} apksize)
    LOCAL=$(getValueFromFile ${FILE_BUILD_STATS} apksize)
    DIFF=$((${LOCAL} - ${MASTER}))

    MESSAGE_MASTER=$(formatFileSize ${MASTER})
    MESSAGE_BRANCH=$(formatFileSize ${LOCAL})
    MESSAGE_DIFF=$(formatFileSize ${DIFF})

    OUT=$(printf "| **APK Size** | $MESSAGE_MASTER | $MESSAGE_BRANCH | $MESSAGE_DIFF | \n")
    echo ${OUT}
}

function rxJavaMigration {
    MASTER_RX=$(getValueFromFile ${FILE_MASTER_BUILD_STATS} rxjava)
    MASTER_RX2=$(getValueFromFile ${FILE_MASTER_BUILD_STATS} rx2)
    LOCAL_RX=$(getValueFromFile ${FILE_BUILD_STATS} rxjava)
    LOCAL_RX2=$(getValueFromFile ${FILE_BUILD_STATS} rx2)
    MASTER_PERCENTAGE=$(percentage ${MASTER_RX2} $(($MASTER_RX+$MASTER_RX2)))
    LOCAL_PERCENTAGE=$(percentage ${LOCAL_RX2} $(($LOCAL_RX+$LOCAL_RX2)))
    DIFF_IMPORTS=$((${LOCAL_RX2} - ${MASTER_RX2}))
    DIFF_PERCENTAGE=`bc <<< ${LOCAL_PERCENTAGE}-${MASTER_PERCENTAGE}`

    MASTER_PERCENTAGE_FORMATTED=$(formatPercentage ${MASTER_PERCENTAGE})
    LOCAL_PERCENTAGE_FORMATTED=$(formatPercentage ${LOCAL_PERCENTAGE})
    DIFF_PERCENTAGE_FORMATTED=$(formatPercentage ${DIFF_PERCENTAGE})

    OUT=$(printf "| **RxJava2 imports** | $MASTER_RX2 | $LOCAL_RX2 | $DIFF_IMPORTS | \n")
    echo ${OUT}
    OUT=$(printf "| **RxJava2 %%** | $MASTER_PERCENTAGE_FORMATTED | $LOCAL_PERCENTAGE_FORMATTED | $DIFF_PERCENTAGE_FORMATTED | \n")
    echo ${OUT}
}

function methodCount {
    MASTER=$(getValueFromFile ${FILE_MASTER_BUILD_STATS} methodcount)
    LOCAL=$(getValueFromFile ${FILE_BUILD_STATS} methodcount)
    DIFF=$((${LOCAL} - ${MASTER}))

    OUT=$(printf "| **Method Count** | $MASTER | $LOCAL | $DIFF | \n")
    echo ${OUT}
}

## Download stats from latest master build
curl -s -o ${FILE_MASTER_BUILD_STATS} "${MASTER_URL}/${FILE_BUILD_STATS}"
curl -s -o ${FILE_MASTER_DEPENDENCY_TREE} "${MASTER_URL}/${FILE_DEPENDENCY_TREE}"

DIFF=$(git diff --no-index ${FILE_MASTER_DEPENDENCY_TREE} ${FILE_DEPENDENCY_TREE} | cat || true)
WORDCOUNT=$(echo ${DIFF} | wc -c)
if [[ ${WORDCOUNT} -gt 1 ]]; then
    rm -f ${FILE_DIFF}
    printf "\`\`\`diff \n" >> $FILE_DIFF
    printf "$DIFF \n" >> $FILE_DIFF
    printf "\`\`\`" >> $FILE_DIFF
    ./scripts/github/create_github_comment.sh ${ghprbPullId} "`cat $FILE_DIFF`"
fi

rm -f $FILE_STATS
printf "| Metric | master | $ghprbSourceBranch | diff | \n" >> ${FILE_STATS}
printf "| ------ | ------ | ------------------ | ---- | \n" >> ${FILE_STATS}
apkSize >> ${FILE_STATS}
methodCount >> ${FILE_STATS}
rxJavaMigration >> ${FILE_STATS}

./scripts/github/create_github_comment.sh ${ghprbPullId} "`cat $FILE_STATS`"
