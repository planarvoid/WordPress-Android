#!/bin/sh

BASEDIR=$(dirname "$0")

read -r -d '' PACT_TEMPLATE << EOM
{
  "provider": {
    "name": "api-mobile"
  },
  "consumer": {
    "name": "android"
  },
  "interactions": INTERACTIONS_ARRAY
}
EOM

interactions_array=()
for file in "$BASEDIR"/*_interaction.json
do
  interactions_array=("${interactions_array[@]}" "`cat $file`")
done

interactions="[$(IFS=, ; echo "${interactions_array[*]}")]"

echo ${PACT_TEMPLATE//INTERACTIONS_ARRAY/$interactions} > "$BASEDIR"/android-api-mobile.json
