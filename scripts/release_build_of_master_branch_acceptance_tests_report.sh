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

BUILD_STATUS=$1

if [ "$BUILD_STATUS" = 'SUCCESS' ]; then
    curl -X POST --data-urlencode payload="$(payload "<$BUILD_URL|Nightly Release Build of Master Branch #$BUILD_NUMBER>... Passed! :rocket:" $PASS_ICON)" $URL
else
    curl -X POST --data-urlencode payload="$(payload "<$BUILD_URL|Nightly Release Build of Master Branch #$BUILD_NUMBER>... Failed! :fire:" $FAIL_ICON)" $URL
fi
