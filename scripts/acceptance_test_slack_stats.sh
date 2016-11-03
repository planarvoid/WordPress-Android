#!/bin/bash

set -e

MYSQL_CONFIG_FILE='/home/mobile/installs/config'
URL=$( cat /home/mobile/installs/webhook.properties | grep -e "url=.*" | cut -d = -f 2)
ANDROID_CHANNEL='#android'
TEST_AND_BUILD_CHANNEL='#testisthebest'
USERNAME='ci-slackbot'
ICON=':chart_with_upwards_trend:'

function payload {
    echo "{\"channel\":\"$1\", \"username\": \"$USERNAME\", \"text\": \"$2\", \"icon_emoji\": \"$ICON\"}"
}

RESULT=$(mysql --defaults-extra-file=$MYSQL_CONFIG_FILE -t mobile_testrunner <<< "
SELECT device.serial_number as SerialNumber, COUNT(*) as FailedTestCount
FROM result
INNER JOIN device
ON result.device_id=device.id
INNER JOIN test
ON result.test_id=test.id
WHERE result.result=0
AND test.class_name regexp 'com.soundcloud.android.tests.*'
AND result.created_at > DATE_SUB(NOW(), INTERVAL 24 HOUR)
GROUP BY device_id
ORDER BY FailedTestCount DESC
LIMIT 10;")

curl -X POST --data-urlencode payload="$(payload $TEST_AND_BUILD_CHANNEL "*Devices with the highest number of test failures over the last 24 hours:*\n\`\`\`$RESULT\`\`\`")" $URL

RESULT2=$(mysql --defaults-extra-file=$MYSQL_CONFIG_FILE -t mobile_testrunner <<< "
SELECT test.class_name as ClassName, test.test_name as TestName, COUNT(*) as FailureCount
FROM result
INNER JOIN test
ON result.test_id=test.id
WHERE result.result=0
AND result.retry=1
AND test.class_name regexp 'com.soundcloud.android.tests.*'
AND result.created_at > DATE_SUB(NOW(), INTERVAL 24 HOUR)
GROUP BY test_id
ORDER BY FailureCount DESC
LIMIT 10;")

curl -X POST --data-urlencode payload="$(payload $ANDROID_CHANNEL "*Most frequently failing tests over the last 24 hours:*\n\`\`\`$RESULT2\`\`\`")" $URL
