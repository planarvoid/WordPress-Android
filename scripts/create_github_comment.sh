#!/bin/bash

##
## $1 PR number
## $2 Body of the message
##

COMMENT="$2"
REQ_BODY=`jq -Rn --arg body "$COMMENT" '{body:$body}'`
echo "${REQ_BODY}"
curl -verbose -X "POST" https://api.github.com/repos/soundcloud/android-listeners/issues/$1/comments \
-H "Authorization: token $GITHUB_ACCESS_TOKEN" \
-H "Content-Type: application/json" \
-d "${REQ_BODY}"
