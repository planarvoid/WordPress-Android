#!/bin/bash

set -e

URL=$( cat /home/mobile/installs/webhook.properties | grep -e "url=.*" | cut -d = -f 2)
TARGET_CHANNEL='@marvin.ramin'
USERNAME='ci-slackbot'
PASS_ICON=':green_build:'
FAIL_ICON=':red_build:'

CURRENT_BUILD=${BUILD_NUMBER}
PREVIOUS_BUILD=$(expr ${CURRENT_BUILD} - 1)

BASE_URL=$(echo ${JOB_URL} | sed 's/mobile-jenkins.int.s-cloud.net/chaos-hare.mobile.s-cloud.net:8080/g')
function payload {
    echo "{\"channel\":\"$TARGET_CHANNEL\", \"username\": \"$USERNAME\", \"text\": \"$1\", \"icon_emoji\": \"$2\"}"
}

function getSubBuildData {
    local SUBRESULTS=$(curl -s "${BASE_URL}${CURRENT_BUILD}/wfapi/describe" | jq -r '.stages[] | select(.name | contains("Reporting") | not) | .name , .status')
    local FORMATTED=$(echo ${SUBRESULTS} | sed 's/SUCCESS/:green_build:\\n/g' | sed 's/FAILED/:red_build:\\n/g')
    echo ${FORMATTED}
}

function getBuildStatus {
    echo $(curl -s "${BASE_URL}$1/wfapi/describe" | jq -r '.status')
}

CURRENT_BUILD_STATUS=$1
PREVIOUS_BUILD_STATUS=$(getBuildStatus ${PREVIOUS_BUILD})

echo "Determining if build status needs to be updated in Slack"
if [ "$CURRENT_BUILD_STATUS" = "SUCCESS" -a "$PREVIOUS_BUILD_STATUS" = "FAILED" ]; then
    echo Sending :green-build: message to ${TARGET_CHANNEL}
    curl -X POST --data-urlencode payload="$(payload "\`${JOB_NAME}\` is :green_build: again." ${PASS_ICON})" ${URL}
elif [ "$CURRENT_BUILD_STATUS" = "FAILED" -a "$PREVIOUS_BUILD_STATUS" = "SUCCESS" ]; then
    SUB_BUILD_INFO=$(getSubBuildData)
    echo Sending :red-build: message to ${TARGET_CHANNEL}
    curl -X POST --data-urlencode payload="$(payload "\`${JOB_NAME}\` is :red_build: \n ${SUB_BUILD_INFO} See the build: ${BASE_URL}${CURRENT_BUILD}" ${FAIL_ICON})" ${URL}
else
    echo "No status to show, status unchanged at ${CURRENT_BUILD_STATUS}"
fi


