#!/usr/bin/env bash

WEEK=`date +%W`
BRANCH=`date +"transifex-translations-%Y-%m-%d"`
TITLE=`date +"Transifex translations %Y-%m-%d"`

BRANCH=${BRANCH##*( )}
TITLE=${TITLE##*( )}

git config user.email "ci.mobile@soundcloud.com"
git config user.name "sc-mobile-ci"

# pull on odd weeks, push on even
if [ $((WEEK%2)) -eq 0 ];
then
    echo "Pushing translations to Transifex"
    tx push -s
else
    echo "Pulling translations from Transifex"

    git checkout -b ${BRANCH}

    sh scripts/pull_release_translations.sh

    git add .

    # check if there is anything to push
    if git diff-index --quiet HEAD; then
        echo "=== There are no new translations==="
        exit 0
    else

        # commit and push translations to branch
        git commit -m "Updating translations" .
        git push origin HEAD

        # open PR
        curl -verbose -X "POST" https://api.github.com/repos/soundcloud/android-listeners/pulls \
        -H "Authorization: token $GITHUB_ACCESS_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"title\": \"${TITLE}\", \"head\": \"${BRANCH}\",\"base\": \"master\"}"
    fi

fi

