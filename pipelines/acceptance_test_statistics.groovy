timestamps {
ansiColor('xterm') {
stage('Publish statistics') {
    node('chaos-slave') {
        checkout([$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanCheckout']], submoduleCfg: [], userRemoteConfigs: [[url: 'git@github.com:soundcloud/android-listeners.git']]])
        sh './scripts/acceptance_test_slack_stats.sh'
    }
}
} // timestamps
} // color
