#!/bin/bash

set -e

URL=$( cat /home/mobile/installs/webhook.properties | grep -e "url=.*" | cut -d = -f 2)

TRIGGER_AUTHOR=$ghprbTriggerAuthorLogin
BUILD_STATUS=$1

function payload {
    echo "{\"channel\":\"$1\", \"username\": \"$2\", \"text\": \"$3\", \"icon_emoji\": \"$4\"}"
}

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

      curl -X POST --data-urlencode payload="$(payload "@${map[1]}" "${USERNAME}" "*PR$ghprbPullId: $ghprbPullTitle*\n$MESSAGE\n Jenkins: ${JOB_URL}${BUILD_NUMBER} \n GitHub: ${ghprbPullLink}" "${ICON}")" $URL
   fi
done
