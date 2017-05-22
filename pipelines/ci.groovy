// ----------------------------------------------------------------------------------------------------
// PIPELINE used to build PRs and master
// ----------------------------------------------------------------------------------------------------

def NODE_NAME = "chaos-slave"

timestamps {
  ansiColor('xterm') {
    def success = true
    def error

    try {
      timeout(time: 1, unit: 'HOURS') {
        stage(Stages.CHECKOUT.name) {
          node(NODE_NAME) {
            checkoutStage()
          }
        }
        parallel 'compile_and_acceptance_tests': {
          stage(Stages.COMPILE_AND_UI_TESTS.name) {
            node(NODE_NAME) {
              compileAndAcceptanceTestStage()
            }
          }
        }, 'unit_test': {
          stage(Stages.UNIT_TESTS.name) {
            node(NODE_NAME) {
              unitTestStage()
            }
          }
        }, 'checks': {
          stage(Stages.STATIC_ANALYSIS.name) {
            node(NODE_NAME) {
              staticAnalysisStage()
            }
          }
        }
      }
    } catch (exc) {
      success = false
      error = exc
    } finally {
      stage(Stages.REPORTING.name) {
        node(NODE_NAME) {
          reportingStage(success, error)
        }
      }
    }
  } // timestamps
} // color

// ----------------------------------------------------------------------------------------------------
// BUILD METADATA
// ----------------------------------------------------------------------------------------------------

enum Builds {
  BUILD("Compile"),
  ACCEPTANCE_TESTS("Acceptance-Tests"),
  UNIT_TESTS("Unit-Tests"),
  STATIC_ANALYSIS("Static Analysis")

  def final name

  Builds(def name) {
    this.name = name
  }
}

enum Status {
  QUEUED("pending", "Queued..."),
  RUNNING("pending", "Running..."),
  SUCCESS("success", "Passed"),
  CANCELLED("failure", "Cancelled"),
  ERROR("error", "Failed")

  def final status
  def final message

  Status(def status, def message) {
    this.status = status
    this.message = message
  }
}

enum Stages {
  CHECKOUT("Checkout"),
  COMPILE_AND_UI_TESTS("Compile and UI Tests"),
  UNIT_TESTS("Unit Tests"),
  STATIC_ANALYSIS("Static Analysis"),
  REPORTING("Reporting")

  def final name

  Stages(def name) {
    this.name = name
  }
}

// ----------------------------------------------------------------------------------------------------
// STAGES
// ----------------------------------------------------------------------------------------------------

def checkoutStage() {
  if (isPr()) {
    checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '${sha1}']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout'], [$class: 'WipeWorkspace']], submoduleCfg: [], userRemoteConfigs: [[name: 'origin', refspec: '+refs/pull/*:refs/remotes/origin/pr/* +refs/heads/*:refs/remotes/origin/*', url: 'git@github.com:soundcloud/android-listeners.git']]]

    env.PIPELINE_VERSION = BUILD_NUMBER + '-' + ghprbPullAuthorLogin + '-PR' + ghprbPullId

    updateGitHub(Builds.BUILD, Status.QUEUED)
    updateGitHub(Builds.ACCEPTANCE_TESTS, Status.QUEUED)
    updateGitHub(Builds.UNIT_TESTS, Status.QUEUED)
    updateGitHub(Builds.STATIC_ANALYSIS, Status.QUEUED)
  } else {
    checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanBeforeCheckout'], [$class: 'WipeWorkspace']], submoduleCfg: [], userRemoteConfigs: [[url: 'git@github.com:soundcloud/android-listeners.git']]]
    def gitCommit = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
    env.GIT_SHA = gitCommit

    def shortCommit = gitCommit.substring(0, 7)
    env.PIPELINE_VERSION = BUILD_NUMBER + '-' + shortCommit

  }
  currentBuild.displayName = env.PIPELINE_VERSION

  stash name: 'repository', useDefaultExcludes: false
}

def compileAndAcceptanceTestStage() {
  deleteDir()
  unstash 'repository'
  try {
    try {
      updateGitHub(Builds.BUILD, Status.RUNNING)
      if (isReleasePr()) {
        setBuildType 'preRelease'
        gradle 'buildPreReleasePR'
      } else {
        gradle 'buildDebugPR'
      }
      updateGitHub(Builds.BUILD, Status.SUCCESS)
    } catch (e) {
      updateGitHub(Builds.BUILD, Status.ERROR)
      updateGitHub(Builds.ACCEPTANCE_TESTS, Status.CANCELLED)
      throw e
    }
    try {
      updateGitHub(Builds.ACCEPTANCE_TESTS, Status.RUNNING)
      if (isReleasePr()) {
        gradle 'runMarshmallowTestsReleasePr'
      } else if (isPr()) {
        gradle 'runMarshmallowTestsPr'
      } else {
        gradle 'runMarshmallowTestsMaster'
      }
      updateGitHub(Builds.ACCEPTANCE_TESTS, Status.SUCCESS)
    } catch (e) {
      updateGitHub(Builds.ACCEPTANCE_TESTS, Status.ERROR)
      throw e
    }
  } finally {
    archiveArtifacts artifacts: "app/build/outputs/apk/soundcloud-android-*-${env.PIPELINE_VERSION}-*.apk", onlyIfSuccessful: true
    junit 'results/xml/*.xml'
    publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'results/', reportFiles: 'index.html', reportName: 'Test Results'])
    publishHTML([allowMissing: false, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'results/', reportFiles: 'collection_index.html', reportName: 'Test Collection Results'])
  }
}

def unitTestStage() {
  deleteDir()
  unstash 'repository'
  try {
    updateGitHub(Builds.UNIT_TESTS, Status.RUNNING)
    gradle 'clean runUnitTests'
    updateGitHub(Builds.UNIT_TESTS, Status.SUCCESS)
  } catch (e) {
    updateGitHub(Builds.UNIT_TESTS, Status.ERROR)
    throw e
  } finally {
    junit '**/build/test-results/**/*.xml'
    publishHTML([allowMissing: true, alwaysLinkToLastBuild: false, keepAll: true, reportDir: 'app/build/reports/tests/testDevDebugUnitTest/', reportFiles: 'index.html', reportName: 'Unit Test Report'])
  }
} // color

def staticAnalysisStage() {
  deleteDir()
  unstash 'repository'
  if (isPr()) {
    try {
      updateGitHub(Builds.STATIC_ANALYSIS, Status.RUNNING)
      gradle 'clean runStaticAnalysisAndReportViolationsToGitHub'
      updateGitHub(Builds.STATIC_ANALYSIS, Status.SUCCESS)
    } catch (e) {
      updateGitHub(Builds.STATIC_ANALYSIS, Status.ERROR)
      throw e
    }
  } else {
    gradle 'clean staticAnalysis'
  }
  pmd canComputeNew: false, defaultEncoding: '', healthy: '', pattern: 'app/build/reports/pmd/pmd.xml', unHealthy: ''
  checkstyle canComputeNew: false, defaultEncoding: '', healthy: '', pattern: 'app/build/reports/checkstyle/checkstyle.xml', unHealthy: ''
  findbugs canComputeNew: false, defaultEncoding: '', excludePattern: '', healthy: '', includePattern: '', pattern: 'app/build/reports/findbugs/findbugs.xml', unHealthy: ''
  androidLint canComputeNew: false, defaultEncoding: '', failedTotalHigh: '0', failedTotalNormal: '0', healthy: '', pattern: 'app/build/reports/lint-results-*.xml', unHealthy: ''
}

def reportingStage(def isSuccess, def error) {
  def status
  if (isSuccess) {
    status = "SUCCESS"
  } else {
    status = "FAILED"
  }

  if (isPr()) {
    sh "./scripts/pr_slack_status.sh $status"

    def emailSubject = "$ghprbSourceBranch"
    if (isSuccess) {
      emailSubject = "🚀 " + emailSubject + " can be merged!"
    } else {
      emailSubject = "🔥 Jenkins is not happy with " + emailSubject
    }

    emailext body: '<p>${SCRIPT, template="random-gif.template"}</p><p>See it on GitHub: ${ghprbPullLink}</p><p>See it on Jenkins: $BUILD_URL</p>', mimeType: 'text/html', replyTo: '$DEFAULT_REPLYTO', subject: emailSubject, to: '${ghprbActualCommitAuthorEmail}'

    if (!isSuccess) {
      // to mark build as failed
      throw error
    }

  } else {

    sh "./scripts/update_master_status_in_slack.sh $status"

    if (isSuccess) {
      sh "./scripts/update_green_master.sh ${env.GIT_SHA}"
    } else {
      // to mark build as failed
      throw error
    }
  }
}

// ----------------------------------------------------------------------------------------------------
// UTILS
// ----------------------------------------------------------------------------------------------------

def updateGitHub(Builds task, Status status) {
  if (isPr()) {
    sh "./scripts/update_pr_status.sh \"$ghprbActualCommit\" -s \"${status.status}\" -d \"${status.message}\" -c \"${task.name}\" -t \"$BUILD_URL\""
  }
}

def isPr() {
  return env.ghprbPullId ? true : false
}

def isReleasePr() {
  return isPr() && "release".equalsIgnoreCase(env.ghprbTargetBranch)
}

def setBuildType(String buildType) {
  env.BUILD_TYPE = buildType
}

def gradle(String tasks) {
  withEnv(['GRADLE_OPTS=-Dorg.gradle.daemon=false']) {
    sh "./gradlew " + tasks
  }
}
