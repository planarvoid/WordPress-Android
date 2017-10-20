timestamps {
  ansiColor('xterm') {
    stage('Checkout') {
      node('android') {
        checkout([$class: 'GitSCM', branches: [[name: 'green_master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanCheckout']], submoduleCfg: [], userRemoteConfigs: [[url: 'git@github.com:soundcloud/android-listeners.git']]])

        def alphaCommit = sh(returnStdout: true, script: 'git rev-parse --short=7 HEAD').trim()
        env.PIPELINE_VERSION = BUILD_NUMBER + '-' + alphaCommit
        currentBuild.displayName = env.PIPELINE_VERSION
        stash name: 'repository'

        sh "git checkout master"
        sh "git pull"
        def masterCommit = sh(returnStdout: true, script: 'git rev-parse --short=7 HEAD').trim()
        if (alphaCommit != masterCommit) {
          sh "./scripts/post_to_slack.sh \"#android-build-status\" \"Android Alpha Build\" \"There are newer commits on \\`master\\` that are not on \\`green_master\\`. The new Alpha is based off of SHA: \\`${alphaCommit}\\`.\" \":watchout:\""
        }
      }
    }
    stage('Build') {
      node('android') {
        deleteDir()
        unstash 'repository'
        sh './scripts/pull_all_translations.sh'
        gradle 'buildAlpha'
        gradle 'deployAlpha'
        archiveArtifacts artifacts: "app/build/outputs/apk/app-prod-alpha.apk", onlyIfSuccessful: true
      }
    }
  } // timestamps
} // color

def gradle(String tasks) {
  withEnv(['GRADLE_OPTS=-Dorg.gradle.daemon=false']) {
    sh "./gradlew " + tasks + " --console=plain"
  }
}
