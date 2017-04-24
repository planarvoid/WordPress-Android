#!/bin/bash

set -o nounset
set -o errexit
set -o pipefail

if [[ $# -ne 1 || -z $1 ]]; then
  echo "No sha specified." >&2
  exit 1;
fi

SHA=$1

echo "Pointing green_master to $SHA."
curl -X PATCH "https://api.github.com/repos/soundcloud/android-listeners/git/refs/heads/green_master?access_token=0330604618ffbc5232a62f3e96f9d3b25377b9cb" -d "{\"sha\": \"$SHA\"}"
