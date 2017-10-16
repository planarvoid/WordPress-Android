#!/bin/bash

##
## $1 Text to grep
## $2 (optional) Directory in which to grep
##

# grep supressing filenames
# count number of lines
# trim whitespaces from return
if [ -z "$2" ]; then # check if directory is set
    git grep -h "$1" | wc -l | tr -d ' '
else
    git grep -h "$1" -- "$2" | wc -l | tr -d ' '
fi
