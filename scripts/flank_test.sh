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
FLANK_VERSION=$(getFlankVersion)

echo "Using Flank v${FLANK_VERSION}"
URL="http://maven.int.s-cloud.net/service/local/repositories/thirdparty_releases/content/com/flank/flank/${FLANK_VERSION}/flank-${FLANK_VERSION}-sources.jar"
curl --location --fail --silent ${URL} --output Flank-${FLANK_VERSION}.jar

java -jar Flank-${FLANK_VERSION}.jar app/build/outputs/apk/app-dev-debug.apk app/build/outputs/apk/app-prod-debug-androidTest.apk
