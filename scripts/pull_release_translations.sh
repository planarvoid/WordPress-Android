#!/bin/bash

tx pull

if [ -f app/src/main/res/values-b+es+419/strings.xml ]; then
  mkdir -p app/src/main/res/values-es-rUS
  cp app/src/main/res/values-b+es+419/strings.xml app/src/main/res/values-es-rUS/strings.xml
fi