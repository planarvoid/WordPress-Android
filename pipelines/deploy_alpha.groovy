timestamps {
ansiColor('xterm') {
stage('Checkout') {
  node('chaos-slave') {
    checkout([$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanCheckout']], submoduleCfg: [], userRemoteConfigs: [[url: 'git@github.com:soundcloud/android.git']]])

    def gitCommit = sh(returnStdout: true, script: 'git rev-parse --short=7 HEAD').trim()
    env.PIPELINE_VERSION=BUILD_NUMBER+'-'+gitCommit
    currentBuild.displayName=env.PIPELINE_VERSION
    stash name: 'repository'
  }
}
stage('Build') {
    node('chaos-slave') {
      deleteDir()
        unstash 'repository'
        sh './scripts/pull_all_translations.sh'
        gradle 'buildAlpha'
        stash includes: 'app/build/outputs/apk/app-prod-alpha.apk', name: 'apk'
        archiveArtifacts artifacts: "app/build/outputs/apk/app-prod-alpha.apk", onlyIfSuccessful: true
    }
}
stage('Upload') {
  node('chaos-slave') {
    deleteDir()
    unstash 'apk'
    gradle 'deployAlpha'
  }
}
} // timestamps
} // color

def gradle(String tasks) {
  withEnv(['GRADLE_OPTS=-Dorg.gradle.daemon=false']) {
    sh "./gradlew "+tasks
  }
}
