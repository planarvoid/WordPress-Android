#!/bin/bash

set -e

URL=$( cat /home/mobile/installs/webhook.properties | grep -e "url=.*" | cut -d = -f 2)

TRIGGER_AUTHOR=$ghprbTriggerAuthorLogin
BUILD_STATUS=$1

IFS=$'\n' users=($(<${WORKSPACE}/scripts/.slack))
for i in "${users[@]}"
do
   IFS=': ' read -ra map <<< "$i"
   if [ "$TRIGGER_AUTHOR" = "${map[0]}" ]; then
      if [ "$BUILD_STATUS" = 'SUCCESS' ]; then
          ICON=':green_build:'
          USERNAME="Green PR Build"
          MESSAGE="... can be merged! :rocket:"
      else
          ICON=':red_build:'
          USERNAME="Failing PR Build"
          MESSAGE="... is failing on CI! :fire:"
      fi

      JENKINS_BLUE_OCEAN_URL="${JENKINS_URL}blue/organizations/jenkins/${JOB_NAME}/detail/${JOB_NAME}/${BUILD_NUMBER}/pipeline"

      ./scripts/post_to_slack.sh "@${map[1]}" "${USERNAME}" "*PR$ghprbPullId: $ghprbPullTitle*\n$MESSAGE\n Jenkins: ${JOB_URL}${BUILD_NUMBER} \n Blue Ocean: ${JENKINS_BLUE_OCEAN_URL} \n GitHub: ${ghprbPullLink}" "${ICON}"
   fi
done
