def NODE_NAME = "android"

timestamps {
  ansiColor('xterm') {
    def success = true
    def error

    try {
      timeout(time: 1, unit: 'HOURS') {
        stage('Checkout') {
          node(NODE_NAME) {
            checkout([$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanCheckout']], submoduleCfg: [], userRemoteConfigs: [[url: 'git@github.com:soundcloud/android-listeners.git']]])

            def gitCommit = sh(returnStdout: true, script: 'git rev-parse --short=7 HEAD').trim()
            env.PIPELINE_VERSION = BUILD_NUMBER + '-' + gitCommit
            currentBuild.displayName = env.PIPELINE_VERSION
            stash name: 'repository'
          }
        }
        stage('Build and Test') {
          node(NODE_NAME) {
            deleteDir()
            unstash 'repository'
            env.BUILD_TYPE = 'preRelease'
            gradle 'clean'
            gradle 'buildPreRelease assembleAcceptanceTest'
            stash name: 'ui-test', useDefaultExcludes: false
          }
          node('chaos-slave') {
            try {
              deleteDir()
              unstash 'ui-test'
              gradle 'runMarshmallowTestsRelease'
            } finally {
              junit 'results/xml/*.xml'
              publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'results/', reportFiles: 'index.html', reportName: 'Test results'])
              publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'results/', reportFiles: 'collection_index.html', reportName: 'Collection Test results'])
            }
          }
        }
      }
    } catch (exc) {
      success = false
      error = exc
    } finally {
      stage('Reporting') {
        node(NODE_NAME) {
          reportingStage(success, error)
        }
      }
    }
  } // timestamps
} // color

def gradle(String tasks) {
  withEnv(['GRADLE_OPTS=-Dorg.gradle.daemon=false']) {
    sh "./gradlew " + tasks + " --console=plain"
  }
}

def reportingStage(def isSuccess, def error) {
  deleteDir()
  unstash 'repository'

  def status
  if (isSuccess) {
    status = "SUCCESS"
  } else {
    status = "FAILED"
  }

  sh "./scripts/release_build_of_master_branch_acceptance_tests_report.sh $status"

  sh "./scripts/detect_stale_prs.sh"
  if (!isSuccess) {
    // to mark build as failed
    throw error
  }
}
