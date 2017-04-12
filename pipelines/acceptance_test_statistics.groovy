timestamps {
ansiColor('xterm') {
stage('Publish statistics') {
    node('chaos-slave') {
        sh './scripts/acceptance_test_slack_stats.sh'
    }
}
} // timestamps
} // color
