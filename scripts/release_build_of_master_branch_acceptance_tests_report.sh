#!/bin/bash

set -e

ANDROID_TESTING_CHANNEL='#android-testing'
USERNAME='ci-slackbot'
PASS_ICON=':green_build:'
FAIL_ICON=':red_build:'

BUILD_STATUS=$1

if [ "$BUILD_STATUS" = 'SUCCESS' ]; then
    ./scripts/post_to_slack.sh ${ANDROID_TESTING_CHANNEL} ${USERNAME} "<$BUILD_URL|Nightly Release Build of Master Branch #$BUILD_NUMBER>... Passed! :rocket:" ${PASS_ICON}
else
    ./scripts/post_to_slack.sh ${ANDROID_TESTING_CHANNEL} ${USERNAME} "<$BUILD_URL|Nightly Release Build of Master Branch #$BUILD_NUMBER>... Failed! :fire:" ${FAIL_ICON}
fi
