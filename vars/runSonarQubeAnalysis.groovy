def call(Map config) {
    sh """
    sonar-scanner \
    -Dsonar.projectKey=${config.projectKey} \
    -Dsonar.sources=. \
    -Dsonar.host.url=${config.sonarHost} \
    -Dsonar.login=${config.sonarToken}
    """
}

