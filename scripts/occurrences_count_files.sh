#!/bin/bash

##
## $1 File extension to grep
##

set -e
set -u
set -o pipefail

EXTENSION=$1

# Find files with extension
# count number of lines
# trim whitespaces from return
git ls-files "*/*.$EXTENSION" | wc -l | tr -d ' '
