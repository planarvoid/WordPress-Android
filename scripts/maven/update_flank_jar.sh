#!/usr/bin/env bash

##
## $1 Flank.jar file
## $2 Flank version
##

set -e

display_help() {
    echo ""
    echo "Helper script to upload a new version of Flank to our internal maven repository."
    echo "Usage: ./update_flank_jar.sh [path to flank.jar] [version (x.y.z)]"
    echo ""
    echo "e.g.: ./update_flank_jar.sh flank.jar 1.0.0"
    exit 1
}

verify_inputs() {
    if [ $# -ne 2 ]
    then
        echo "Not all parameters provided"
        display_help
    fi

    if ! test -f $1
    then
        echo "Path to jar is not a file."
        display_help
        exit 1
    fi

    VERSION_REGEX=[0-9]\.[0-9]\.[0-9]
    if ! [[ "$2" =~ ${VERSION_REGEX} ]]
    then
        echo "Version number incorrect. It should have the pattern x.y.z (e.g.: 1.0.0)"
        display_help
        exit 1
    fi
}

verify_inputs $1 $2

mvn deploy:deploy-file \
    -DgroupId=com.flank \
    -DartifactId=flank \
    -Dpackaging=java-source \
    -Dfile=$1 \
    -Dversion=$2 \
    -DrepositoryId=thirdparty_releases
    -Durl=http://maven.int.s-cloud.net/content/repositories/thirdparty_releases \
    -DgeneratePom=false
