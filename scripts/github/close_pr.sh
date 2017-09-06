#!/bin/bash

##
## Close a Pull Request
## $1 - PR number
##

REQ_BODY=$(jq -n '{"state": "closed"}')

curl -verbose -X "PATCH" https://api.github.com/repos/soundcloud/android-listeners/pulls/$1 \
-H "Authorization: token $GITHUB_ACCESS_TOKEN" \
-H "Content-Type: application/json" \
-d "${REQ_BODY}"
