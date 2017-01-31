#!/bin/bash

readonly PROGNAME=$(basename $0)
readonly TOKEN="$GITHUB_ACCESS_TOKEN"

usage() {
  cat <<EOF
usage: $PROGNAME SHA options
$PROGNAME updates a GitHub commit status.
OPTIONS:
   -s --state         The state of the status. Can be one of pending, success, error, or failure. (Required)
   -t --target_url    The target URL to associate with this status. (Required)
   -d --description   A short description of the status. (Required)
   -c --context       A string label to differentiate this status from the status of other systems. (Required)
   -h --help          Show this help
EOF
  exit 1
}

update_status() {
  curl -X "POST" https://api.github.com/repos/soundcloud/android-listeners/statuses/$SHA \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"description\":\"$DESCRIPTION\",\"state\":\"$STATE\",\"target_url\":\"$TARGET_URL\",\"context\":\"$CONTEXT\"}"
}

SHA="$1"; shift

while [[ $# > 1 ]]
do
  key="$1"

  case $key in
    -s|--state)
      STATE="$2"
      shift
      ;;
    -t|--target_url)
      TARGET_URL="$2"
      shift
      ;;
    -d|--description)
      DESCRIPTION="$2"
      shift
      ;;
    -c|--context)
      CONTEXT="$2"
      shift
      ;;
    -h|--help)
      usage
      ;;
    *)
      echo -e "Unknown option: $key"
      usage
      ;;
  esac
  shift # past argument or value
done

if [ -z "$SHA" ] || [ -z "$STATE" ] || [ -z "$TARGET_URL" ] || [ -z "$DESCRIPTION" ] || [ -z "$CONTEXT" ]; then
  usage
fi

echo "sha:          $SHA"
echo "state:        $STATE"
echo "description:  $DESCRIPTION"
echo "target_url:   $TARGET_URL"
echo "context:      $CONTEXT"

update_status "$SHA" "$STATE" "$TARGET_URL" "$DESCRIPTION" "$CONTEXT"
