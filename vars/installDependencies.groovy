def call(Map args) {
    stage('Install Dependencies') {
        steps {
            script {
                // Define variables from args
                def releaseName = args.releaseName
                def pythonBinary = args.pythonBinary ?: '/usr/bin/python3' // Default to Python 3 if not provided

                // Validate required parameters
                if (!releaseName) {
                    error "releaseName is required but not provided in the configuration."
                }

                // Navigate to the release directory
                dir(releaseName) {
                    catchError(buildResult: 'FAILURE') {
                        if (params.VERBOSE_LOGS) { // Directly use params.VERBOSE_LOGS
                            echo "Installing dependencies with verbose logs..."
                            sh "${pythonBinary} -m pipenv install --ignore-pipfile --verbose"
                        } else {
                            echo "Installing dependencies..."
                            sh "${pythonBinary} -m pipenv install --ignore-pipfile"
                        }
                    }
                }
            }
        }
    }
}
