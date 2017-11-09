# Make commands for GoCD pipeline (or to simulate pipeline steps locally)

unit-test:
	crun android-dev-compile -- ./gradlew clean runUnitTests --no-build-cache

debug-apk:
	crun android-dev-compile -- ./gradlew buildDebugPR

static-analysis:
	crun android-dev-compile -- ./gradlew staticAnalysis

acceptance-test-firebase:
	crun android-dev-gcloud -- ./scripts/flank_test.sh

generate-build-stats:
	crun android-dev-compile -- ./scripts/generate_build_stats.sh

analyze-build-stats:
	/bin/sh -c scripts/analyze_build_stats_gocd.sh

slack-status-success:
	crun android-dev-base -- scripts/pr_slack_status_gocd.sh "SUCCESS"

slack-status-failure:
	crun android-dev-base -- scripts/pr_slack_status_gocd.sh

acceptance-test-mtr:
	crun android-dev-compile -- ./gradlew runMarshmallowTestsMaster 