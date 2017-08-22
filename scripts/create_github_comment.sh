#!/usr/bin/env bash

##
## $1 PR number
## $2 Body of the message
##

curl -verbose -X "POST" https://api.github.com/repos/soundcloud/android-listeners/issues/$1/comments \
-H "Authorization: token $GITHUB_ACCESS_TOKEN" \
-H "Content-Type: application/json" \
-d "{\"body\": \"$2\"}"
