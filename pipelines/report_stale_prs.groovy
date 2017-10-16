timestamps {
ansiColor('xterm') {
stage('Publish statistics') {
    node('android') {
        deleteDir()
        checkout([$class: 'GitSCM', branches: [[name: 'master']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CleanCheckout']], submoduleCfg: [], userRemoteConfigs: [[url: 'git@github.com:soundcloud/android-listeners.git']]])
        sh "./scripts/detect_stale_prs.sh"
    }
}
} // timestamps
} // color
