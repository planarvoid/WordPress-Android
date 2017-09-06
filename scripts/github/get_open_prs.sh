#!/bin/bash

##
## Returns all open PRs
##

PRS=$(curl -verbose -X "GET" https://api.github.com/repos/soundcloud/android-listeners/pulls?state=open \
-H "Authorization: token $GITHUB_ACCESS_TOKEN" \
-H "Content-Type: application/json")

echo ${PRS}
