timestamps {
ansiColor('xterm') {
def success = true
def error

try {
  stage('Checkout') {
    node('chaos-slave') {
      checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout'], [$class: 'WipeWorkspace']], submoduleCfg: [], userRemoteConfigs: [[url: 'git@github.com:soundcloud/android.git']]]

      def gitCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
      env.GIT_SHA=gitCommit

      def shortCommit = gitCommit.substring(0, 7)
      env.PIPELINE_VERSION=BUILD_NUMBER+'-'+shortCommit

      currentBuild.displayName=env.PIPELINE_VERSION
      stash name: 'repository'
    }
  }
  parallel 'compile_and_acceptance_tests': {
    stage('Compile and UI Tests') {
    node('chaos-slave') {
      deleteDir()
      unstash 'repository'
      try {
        gradle 'clean assembleDebugApk assembleAcceptanceTest downloadNetworkManagerApkCI'
        gradle 'runLollipopTests'
      } finally {
        archiveArtifacts artifacts: "app/build/outputs/apk/soundcloud-android-*-${env.PIPELINE_VERSION}-debug-*.apk", onlyIfSuccessful: true
        junit 'results/xml/*.xml'
        publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'results/', reportFiles: 'index.html', reportName: 'Test results'])
        publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'results/', reportFiles: 'collection_index.html', reportName: 'Collection Test results'])
        }
      }
    }
  }, 'unit_test': {
    stage('Unit Test') {
      node('chaos-slave') {
        deleteDir()
        unstash 'repository'
        try {
          gradle 'clean runUnitTests'
        } finally {
          junit '**/build/test-results/**/*.xml'
          publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'app/build/reports/tests/testDevDebugUnitTest/', reportFiles: 'index.html', reportName: 'Unit Test Report'])
        }
      }
    }
  }, 'checks': {
    stage('Lint') {
      node('chaos-slave') {
        deleteDir()
        unstash 'repository'
        gradle 'clean lintProdDebug'
        androidLint canComputeNew: false, defaultEncoding: '', failedTotalHigh: '0', failedTotalNormal: '0', healthy: '', pattern: 'app/build/reports/lint-results-prodDebug.xml', unHealthy: ''
      }
    }
  }
} catch (exc) {
  success = false
  error = exc
} finally {
  stage('Reporting') {
    node('chaos-slave') {
      def status
      def emailSubject = "$JOB_NAME - Build # $BUILD_NUMBER - "
      if (success) {
        status = "SUCCESS"
      } else {
        status = "FAILED"
      }
      emailSubject = emailSubject + status + "!"

      sh "./scripts/update_master_status_in_slack.sh $status"
      emailext body: '<p>${SCRIPT, template="random-gif.template"}</p>See this build on Jenkins: $JOB_URL', mimeType: 'text/html', replyTo: '$DEFAULT_REPLYTO', subject: emailSubject, to: 'marvin.ramin@soundcloud.com'

      if (success) {
        sh "./scripts/update_green_master.sh $GIT_SHA"
      } else {
        // to mark build as failed
        throw exc
      }
    }
  }
}
} // timestamps
} // color

def gradle(String tasks) {
  withEnv(['GRADLE_OPTS=-Dorg.gradle.daemon=false']) {
    sh "./gradlew " + tasks
  }
}
