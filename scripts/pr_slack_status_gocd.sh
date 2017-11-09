#!/bin/bash

set -e

TRIGGER_AUTHOR=$PR_AUTHOR
BUILD_STATUS=$1

if [ "$BUILD_STATUS" = 'SUCCESS' ]; then
    ICON=':green_build:'
    USERNAME="Green PR Build"
    MESSAGE="... can be merged! :rocket:"
else
    ICON=':red_build:'
    USERNAME="Failing PR Build"
    MESSAGE="... is failing on CI! :fire:"
fi

#The "GO_SCM_...." environment variable names are based off the name of the material in GoCD, in this case, "testing-pr". Any changes to the material name will thus reflect a change in the variables used here. 
GO_CD_URL="${GO_SCM_ANDROID_PR_SERVER_URL}/${GO_SCM_ANDROID_PR_PIPELINE_LABEL}/${GO_SCM_ANDROID_PR_STAGE_NAME}/${GO_SCM_ANDROID_PR_STAGE_COUNTER}/${GO_SCM_ANDROID_PR_JOB_NAME}"

$(dirname "${BASH_SOURCE[0]}")/post_to_slack_gocd.sh "gocd-test-alerts" "$USERNAME" "*PR$GO_SCM_ANDROID_PR_PR_BRANCH: $GO_SCM_ANDROID_PR_PR_TITLE*\n$MESSAGE\n GoCD: $GO_CD_URL\n GitHub: ${GO_SCM_ANDROID_PR_PR_URL}" "${ICON}"
