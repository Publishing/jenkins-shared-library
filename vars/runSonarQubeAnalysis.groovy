def call(Map args) {
    if (params.SELECT_WORK_FLOW == 'UD') {
        echo "Skipping SonarQube analysis for workflow UD."
        return
    }

    script {
        // Define variables from args
        def releaseName = args.releaseName
        def appName     = args.appName
        def pythonVersion = args.pythonVersion ?: '3.11'
        def coverageReportPath = args.coverageReportPath ?: 'coverage.xml'

        // (Optional) If you want to use manual approach, you need these:
        def sonarHost  = args.sonarHost  ?: 'https://djg-jenkins.rtegroup.ie/sonar'
        def sonarToken = args.sonarToken // Only needed for the manual fallback

        if (!releaseName || !appName) {
            error "releaseName and appName are required but not provided in the configuration."
        }

        dir(releaseName) {
            try {
                echo "Running SonarQube analysis for project: ${appName}.app"

                // 1) Run Sonar Scanner with Jenkins plugin
                withSonarQubeEnv('sonarqube') {
                    sh """
                    sonar-scanner \
                      -Dsonar.projectKey=${appName}.app \
                      -Dsonar.sources=. \
                      -Dsonar.python.version=${pythonVersion} \
                      -Dsonar.python.coverage.reportPaths=${coverageReportPath}
                    """
                }

                // Keep track if we successfully got QG from Jenkins
                boolean gotQGfromJenkins = false
                env.SONAR_STATUS = ''  // Initialize
                env.SONAR_URL = "${sonarHost}/api/qualitygates/project_status?projectKey=${appName}.app"

                // 2) Attempt short waitForQualityGate (10 seconds)
                try {
                    timeout(time: 10, unit: 'SECONDS') {
                        def qg = waitForQualityGate()
                        // If we reach here, the plugin succeeded
                        gotQGfromJenkins = true

                        if (qg.status == 'OK') {
                            echo "SonarQube Quality Gate PASSED: ${qg.status}"
                            env.SONAR_STATUS = "OK"
                        } else if (qg.status == 'ERROR') {
                            echo "SonarQube Quality Gate FAILED: ${qg.status}"
                            env.SONAR_STATUS = "ERROR"
                            error("Build failed due to SonarQube Quality Gate failure.")
                        } else {
                            echo "SonarQube Quality Gate returned UNKNOWN status: ${qg.status}"
                            env.SONAR_STATUS = qg.status
                            currentBuild.result = 'UNSTABLE'
                        }
                    }
                } catch (err) {
                    echo "waitForQualityGate() short timeout or error: ${err}"
                    echo "Falling back to manual SonarQube API check."
                }

                // 3) If we didn't get a result from Jenkins, do the manual approach
                if (!gotQGfromJenkins) {
                    if (!sonarToken) {
                        echo "Manual fallback requested, but no 'sonarToken' provided. Skipping manual check."
                    } else {
                        // Hit SonarQube API directly for project status
                        def response = sh(
                            script: """
                                curl -s -u ${sonarToken}: \
                                     ${sonarHost}/api/qualitygates/project_status?projectKey=${appName}.app
                            """,
                            returnStdout: true
                        ).trim()

                        echo "Manual SonarQube API Response: ${response}"

                        // Parse JSON for 'status' field
                        try {
                            env.SONAR_STATUS = sh(
                                script: "echo '${response}' | jq -r '.projectStatus.status'",
                                returnStdout: true
                            ).trim()
                        } catch (e) {
                            error("Failed to parse manual SonarQube API response: ${e.getMessage()}")
                        }

                        // Evaluate final status
                        if (env.SONAR_STATUS == "OK") {
                            echo "SonarQube Quality Gate passed: ${env.SONAR_STATUS}"
                        } else if (env.SONAR_STATUS == "ERROR") {
                            echo "SonarQube Quality Gate failed: ${env.SONAR_STATUS}"
                            error("Marking stage as error due to SonarQube Quality Gate failure.")
                        } else {
                            echo "SonarQube Quality Gate returned an unknown status: ${env.SONAR_STATUS}"
                            currentBuild.result = 'UNSTABLE'
                        }
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
