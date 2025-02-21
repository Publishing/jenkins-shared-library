def call(Map args) {
    if (params.SELECT_WORK_FLOW == 'UD') {
        echo "Skipping SonarQube analysis for workflow UD."
        return
    }

    script {
        // Define variables from args
        def releaseName = args.releaseName
        def appName = args.appName
        def pythonVersion = args.pythonVersion ?: '3.11' // Default Python version
        def coverageReportPath = args.coverageReportPath ?: 'coverage.xml'

        // Validate required parameters
        if (!releaseName || !appName) {
            error "releaseName and appName are required but not provided in the configuration."
        }

        dir(releaseName) {
            try {
                echo "Running SonarQube analysis for project: ${appName}.app"

                // Use Jenkins SonarQube Integration
                withSonarQubeEnv('sonarqube') { // Name should match the configured SonarQube server in Jenkins
                    sh """
                    sonar-scanner \
                    -Dsonar.projectKey=${appName}.app \
                    -Dsonar.sources=. \
                    -Dsonar.python.version=${pythonVersion} \
                    -Dsonar.python.coverage.reportPaths=${coverageReportPath}
                    """
                }

                // Define SonarQube URL dynamically
                def sonarUrl = "${env.SONAR_HOST_URL}/dashboard?id=${appName}.app"
                env.SONAR_URL = sonarUrl

                // Wait for SonarQube Quality Gate Result
                timeout(time: 10, unit: 'MINUTES') { // Adjust timeout based on project size
                    def qg = waitForQualityGate()
                    if (qg.status == 'OK') {
                        echo "SonarQube Quality Gate PASSED: ${qg.status}"
                    } else if (qg.status == 'ERROR') {
                        echo "SonarQube Quality Gate FAILED: ${qg.status}"
                        error("Build failed due to SonarQube Quality Gate failure.")
                    } else {
                        echo "SonarQube Quality Gate returned UNKNOWN status: ${qg.status}"
                        currentBuild.result = 'UNSTABLE'
                    }
                }

            } catch (Exception e) {
                echo "Error during SonarQube analysis: ${e.getMessage()}"
                currentBuild.result = 'FAILURE'
                error("Stopping pipeline due to SonarQube analysis failure.")
            }
        }
    }
}
