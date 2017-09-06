#!/bin/bash

set -e
set -u
set -o pipefail

FILE_BOT_COMMENTS='bot_comments.txt'
FILE_PR_UPDATED_AT='updated_at.txt'

PRS_JSON=$(./scripts/github/get_open_prs.sh)

function finish {
  rm -f ${FILE_BOT_COMMENTS}
  rm -f ${FILE_PR_UPDATED_AT}
}
trap finish EXIT

##
## $1 - PR number
## $2 - GitHub message
## $3 - PR Author
## $4 - Slack message
##
function notify {
    ./scripts/github/create_github_comment.sh $1 "$2"
    SLACK_USERNAME=$(./scripts/username_github_to_slack.sh $3)
    if [ -n "$SLACK_USERNAME" ]
    then
        ./scripts/post_to_slack.sh $SLACK_USERNAME "Stale PR Sheriff" $4 ":wave:"
    fi
}

##
## $1 - PR number
## $2 - PR Author
## $3 - PR Branch
## $4 - PR URL
##
function notifyPR {
    GITHUB_MESSAGE="This PR hasn't been updated in 7 days and looks stale. If there is no new activity I will close this PR in 3 days!"
    SLACK_MESSAGE="Friendly reminder: The PR of your branch $3 looks pretty stale. Please take a look at it and update or close it in the next 3 days or I will close it. \n $4"
    notify $1 "$GITHUB_MESSAGE" $2 "$SLACK_MESSAGE"
}

##
## $1 - PR number
## $2 - PR Author
## $3 - PR Branch
## $4 - PR URL
##
function notifyAndClosePR {
    GITHUB_MESSAGE="This PR looks stale and has been warned before - closing. Feel free to re-open the PR if necessary."
    SLACK_MESSAGE="The PR of your branch $3 was not updated since the friendly reminder. I'll go ahead and close it now. Feel free to reopen it if you want to continue working on it. \n $4"
    notify $1 "$GITHUB_MESSAGE" $2 "$SLACK_MESSAGE"
    ./scripts/github/close_pr.sh $1
}

function prContainsWarningComment {
    RESULT=false
    curl $1 -H "Authorization: token $GITHUB_ACCESS_TOKEN" | jq '.[] | select(.user.login == "sc-mobile-ci") | .body' > $FILE_BOT_COMMENTS
    while IFS='' read -r line || [[ -n "$line" ]]; do
        if [ "$line" == "Friendly reminder: *" ]
        then
            RESULT=true
        fi
    done < $FILE_BOT_COMMENTS
    echo $RESULT
}

## Get updated at field
echo ${PRS_JSON} | jq '.[].updated_at' > $FILE_PR_UPDATED_AT

## Read updated fields and determine stale
NOW=$(date +%s)
WARNING_DIFF=604800 # seven days
CLOSE_DIFF=259200 # three days
WARNING_THRESHOLD=$(($NOW - $WARNING_DIFF))
CLOSE_THRESHOLD=$(($NOW - $WARNING_DIFF - $CLOSE_DIFF))
I=0
while IFS='' read -r line || [[ -n "$line" ]]; do
    DATE=$(echo $line | sed 's/"//g')
    UNIXDATE=$(date -j -u -f "%Y-%m-%dT%H:%M:%SZ" $DATE +%s)

    if [ "${UNIXDATE}" -lt "${WARNING_THRESHOLD}" ]
    then
        ## PR is considered stale
        PR_URL=$(cat prs.txt | jq ".[${I}].html_url")
        PR_COMMENTS_URL=$(cat prs.txt | jq ".[${I}].comments_url")
        PR_BRANCH=$(cat prs.txt | jq ".[${I}].head.ref")
        PR_AUTHOR=$(cat prs.txt | jq ".[${I}].user.login")
        PR_NUMBER=$(cat prs.txt | jq ".[${I}].number")

        ALREADY_WARNED=$(prContainsWarningComment $PR_COMMENTS_URL)
        if [ "$ALREADY_WARNED" = true ]
        then
            if [ "$UNIXDATE" -lt "$CLOSE_THRESHOLD" ]
            then
                notifyAndClosePR "$PR_NUMBER" "$PR_AUTHOR" "$PR_BRANCH" "$PR_URL"
            fi
        else
            notifyPR "$PR_NUMBER" "$PR_AUTHOR" "$PR_BRANCH" "$PR_URL"
        fi

    fi
    I=$((I + 1))
done < $FILE_PR_UPDATED_AT
