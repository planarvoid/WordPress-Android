#!/bin/bash

set -e

URL=$( cat /home/mobile/installs/webhook.properties | grep -e "url=.*" | cut -d = -f 2)
ANDROID_TESTING_CHANNEL='#android-testing'
USERNAME='ci-slackbot'
PASS_ICON=':green_build:'
FAIL_ICON=':red_build:'

function payload {
    echo "{\"channel\":\"$ANDROID_TESTING_CHANNEL\", \"username\": \"$USERNAME\", \"text\": \"$1\", \"icon_emoji\": \"$2\"}"
}

function getNumberOfFailingTests {
	echo $(curl -s "http://chaos-hare.mobile.s-cloud.net:8080/job/${JOB_NAME}/${BUILD_NUMBER}/testReport/api/json?pretty=true" | \
                               python -c "import sys, json; print json.load(sys.stdin)['failCount']")
}

FAILED_TEST_COUNT=$(getNumberOfFailingTests)

if [ "$FAILED_TEST_COUNT" -le 0 ]; then
    curl -X POST --data-urlencode payload="$(payload "<$BUILD_URL|Nightly Release Build of Master Branch #$BUILD_NUMBER> Passed" $PASS_ICON)" $URL
else
    curl -X POST --data-urlencode payload="$(payload "<$BUILD_URL|Nightly Release Build of Master Branch #$BUILD_NUMBER> had $FAILED_TEST_COUNT failing tests" $FAIL_ICON)" $URL
fi
