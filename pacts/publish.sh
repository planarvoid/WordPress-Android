#!/bin/bash

BASEDIR=$(dirname "$0")
PACT_BROKER=pact-broker.dev.s-cloud.net

curl -f -v -X PUT -H "Content-Type: application/json" -d @$BASEDIR/android-api-mobile.json http://$PACT_BROKER/pacts/provider/api-mobile/consumer/android/version/1.0.0
