def call() {
    stage('Install Dependencies') {
        steps {
            script {
                dir(env.RELEASE_NAME) {
                    catchError(buildResult: 'FAILURE') {
                        if (params.VERBOSE_LOGS) {
                            sh "${env.PYTHON_BINARY} -m pipenv install --ignore-pipfile --verbose"
                        } else {
                            sh "${env.PYTHON_BINARY} -m pipenv install --ignore-pipfile"
                        }
                    }
                }
            }
        }
    }
}

