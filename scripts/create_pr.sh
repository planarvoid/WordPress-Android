#!/bin/bash

readonly PROGNAME=$(basename $0)
readonly TOKEN="$GITHUB_ACCESS_TOKEN"

usage() {
  cat <<EOF
usage: $PROGNAME options
$PROGNAME updates a GitHub commit status.
OPTIONS:
   -d --head          The name of the branch where your changes are implemented.  (Required)
   -b --base          The name of the branch you want the changes pulled into. (Required)
   -t --title         The title of the pull request. (Required)
   -h --help          Show this help
EOF
  exit 1
}

update_status() {
  curl -X "POST" https://api.github.com/repos/soundcloud/android-listeners/pulls \
    -H "Authorization: token $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"title\": \"$TITLE\",\"head\": \"$HEAD\",\"base\": \"$BASE\"}"
}

SHA="$1"; shift

while [[ $# > 1 ]]
do
  key="$1"

  case $key in
    -d|--head)
      HEAD="$2"
      shift
      ;;
    -b|--base)
      BASE="$2"
      shift
      ;;
    -t|--title)
      TITLE="$2"
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

if [ -z "$SHA" ] || [ -z "$HEAD" ] || [ -z "$TITLE" ]; then
  usage
fi

echo "head:         $HEAD"
echo "base:         $BASE"
echo "title:        $TITLE"

update_status "$HEAD" "$BASE" "$TITLE"
