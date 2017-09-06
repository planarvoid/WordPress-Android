#!/bin/bash

##
## $1 Text to grep
##

# grep supressing filenames
# count number of lines
# trim whitespaces from return
git grep -h "$1" | wc -l | tr -d ' '
