#!/usr/bin/env bash

##
## $1 username
##

USERNAME=""

IFS=$'\n' users=($(<${WORKSPACE}/scripts/.slack))
for i in "${users[@]}"
do
   IFS=': ' read -ra map <<< "$i"
   if [ "$1" = "${map[0]}" ]; then
      USERNAME=${map[1]}
   fi
done

echo $USERNAME
