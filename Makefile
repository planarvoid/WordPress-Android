# Make commands for GoCD pipeline (or to simulate pipeline steps locally)

unit-test:
	crun android-dev-compile -- ./gradlew clean runUnitTests --no-build-cache

debug-apk:
	crun android-dev-compile -- ./gradlew buildDebugPR

static-analysis:
	crun android-dev-compile -- ./gradlew staticAnalysis

acceptance-test-firebase:
	crun android-dev-gcloud -- ./scripts/flank_test.sh
