#!/bin/bash

###
### Required parameters
### $1 - channel to post the message to (#some_channel or @someone)
### $2 - username the message will be posted as
### $3 - text of the message
### $4 - user icon (as an emoji)
###

set -e

URL=$( cat /home/mobile/installs/webhook.properties | grep -e "url=.*" | cut -d = -f 2)

function payload {
    echo "{\"channel\":\"$1\", \"username\": \"$2\", \"text\": \"$3\", \"icon_emoji\": \"$4\"}"
}

curl -X POST --data-urlencode payload="$(payload "$1" "$2" "$3" "$4")" ${URL}
