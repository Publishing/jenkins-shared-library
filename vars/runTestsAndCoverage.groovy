def call(Map args) {
    script {
        // Retrieve arguments or set defaults
        def releaseName = args.releaseName
        def tmpDir = args.tmpDir ?: "/tmp"
        def unitTestSettings = args.unitTestSettings ?: "conf.unit_test_settings"
        def coverageXmlPath = args.coverageXmlPath ?: "coverage.xml"
        def appName = args.appName

        // Validate required parameters
        if (!releaseName) {
            error "releaseName is required but not provided in the configuration."
        }

        // Conditional check for appName
        if (appName != 'api' && appName != 'dotie') {
            echo "Skipping tests and coverage stage as appName '${appName}' is not 'api' or 'dotie'."
            return
        }

        dir(releaseName) {
            catchError(buildResult: 'FAILURE') {
                try {
                    // Install coverage tool
                    echo "Installing coverage tool..."
                    sh "pipenv run pip install coverage"

                    // Run tests with coverage
                    echo "Running unit tests with settings: ${unitTestSettings}"
                    sh """
                    pipenv run coverage run manage.py test --settings ${unitTestSettings} 2>&1 | tee ${tmpDir}/test.log
                    """

                    // Generate coverage report
                    echo "Generating coverage report..."
                    sh """
                    pipenv run coverage report -m | tee ${tmpDir}/coverage.log
                    pipenv run coverage xml -o ${coverageXmlPath}
                    """

                    // Parse test results for failures
                    echo "Parsing test results..."
                    def failedTests = sh(script: "${tmpDir}/pipe_parser.sh fail", returnStatus: true)
                    if (failedTests > 0) {
                        echo "Tests found issues, ${failedTests} tests failed."
                        currentBuild.result = 'FAILURE'
                        error("Stopping pipeline due to test failures.")
                    } else {
                        echo "Unit tests PASSED successfully."
                        currentBuild.result = 'SUCCESS'
                    }
                } catch (Exception e) {
                    echo "Error encountered during test execution: ${e.message}"
                    throw e // Rethrow the exception to propagate the failure
                }
            }
        }
    }
}
