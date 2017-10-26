set -e

echo "Firing a full Flank at Firebase..."

## Cleanup Flank jar
function finish {
  rm Flank-*.jar
}
trap finish EXIT

## Read flankVersion from dependencies.gradle
function getFlankVersion {
    VERSION=$(cat buildsystem/dependencies.gradle | grep flankVersion | cut -d'=' -f2 | sed 's/ //g' | sed "s/'//g")
    echo ${VERSION}
}

function getApkName {
	local
	APKNAME=$(find app/build/outputs/apk -maxdepth 1 -type f -name "soundcloud-android*.apk" -print -quit 2>/dev/null)
	echo ${APKNAME}
}

function getTestApkName {
	local
	APKNAME=$(find app/build/outputs/apk -maxdepth 1 -type f -name "*androidTest.apk" -print -quit 2>/dev/null)
	echo ${APKNAME}
}


FLANK_VERSION=$(getFlankVersion)
DEVAPKNAME=$(getApkName)
TESTAPKNAME=$(getTestApkName)

echo "Using Flank v${FLANK_VERSION} with application APK: ${DEVAPKNAME} and test Apk: ${TESTAPKNAME}"
URL="http://maven.int.s-cloud.net/service/local/repositories/thirdparty_releases/content/com/flank/flank/${FLANK_VERSION}/flank-${FLANK_VERSION}-sources.jar"
curl --location --fail --silent ${URL} --output Flank-${FLANK_VERSION}.jar

java -jar Flank-${FLANK_VERSION}.jar ${DEVAPKNAME} ${TESTAPKNAME}
