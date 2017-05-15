#!/bin/bash

set -e

URL=$( cat /home/mobile/installs/webhook.properties | grep -e "url=.*" | cut -d = -f 2)

TRIGGER_AUTHOR=$ghprbTriggerAuthorLogin

USERNAME='ci-slackbot'
ICON=':rightshark:'

BUILD_STATUS=$1

function payload {
    echo "{\"channel\":\"$1\", \"username\": \"$USERNAME\", \"text\": \"$2\", \"icon_emoji\": \"$ICON\"}"
}

IFS=$'\n' users=($(<${WORKSPACE}/scripts/.slack))
for i in "${users[@]}"
do
   IFS=': ' read -ra map <<< "$i"
   if [ "$TRIGGER_AUTHOR" = "${map[0]}" ]; then
      if [ "$BUILD_STATUS" = 'SUCCESS' ]; then
          MESSAGE="... can be merged! :rocket:"
      else
          MESSAGE="... is failing on CI! :fire:"
      fi

      curl -X POST --data-urlencode payload="$(payload "@${map[1]}" "*PR$ghprbPullId: $ghprbPullTitle*\n$MESSAGE\n Jenkins: ${JOB_URL}${BUILD_NUMBER} \n GitHub: ${ghprbPullLink}")" $URL
   fi
done
