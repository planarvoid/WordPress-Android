timestamps {
  ansiColor('xterm') {
    stage('Checkout') {
      node('chaos-slave') {
        checkout([$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanCheckout']], submoduleCfg: [], userRemoteConfigs: [[url: 'git@github.com:soundcloud/android-listeners.git']]])

        def gitCommit = sh(returnStdout: true, script: 'git rev-parse --short=7 HEAD').trim()
        env.PIPELINE_VERSION=BUILD_NUMBER+'-'+gitCommit
        currentBuild.displayName=env.PIPELINE_VERSION
        stash name: 'repository'
      }
    }
    stage('Build Time Analysis') {
      node('chaos-slave') {
        deleteDir()
        unstash 'repository'

        def now = System.currentTimeMillis()
        sh './scripts/time_dev_build.sh'
        def totalTime = (System.currentTimeMillis() - now) / 1000

        currentBuild.description=totalTime + " seconds"
      }
    }
  } // timestamps
} // color
