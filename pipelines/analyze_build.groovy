timestamps {
ansiColor('xterm') {
stage('Checkout') {
  node('chaos-slave') {
    checkout([$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanCheckout']], submoduleCfg: [], userRemoteConfigs: [[url: 'git@github.com:soundcloud/android.git']]])

    def gitCommit = sh(returnStdout: true, script: 'git rev-parse --short=7 HEAD').trim()
    env.PIPELINE_VERSION = BUILD_NUMBER + '-' + gitCommit
    currentBuild.displayName = env.PIPELINE_VERSION
    stash name: 'repository'
  }
}
stage('Build & Analyze') {
  node('chaos-slave') {
    deleteDir()
    unstash 'repository'
    gradle 'assembleRelease'
    gradle 'ndUpload ndGetProfile'
  }
}
} // timestamps
} // color

def gradle(String tasks) {
  withEnv(['GRADLE_OPTS=-Dorg.gradle.daemon=false']) {
    sh "./gradlew "+tasks
  }
}
