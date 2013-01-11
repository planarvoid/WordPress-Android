#!/bin/sh
NORMALIZED_GIT_BRANCH=`echo $GIT_BRANCH | sed "s/[^a-zA-Z0-9]/\-/g"`

deploy_to_dropbox() {
  for f in `find app/target/ -name "*.apk" -exec basename {} ';'`; do
    cp app/target/$f ~/Dropbox/android/`echo $f | sed "s/\.apk/-$NORMALIZED_GIT_BRANCH\.apk/g"`
  done
}