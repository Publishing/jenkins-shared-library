def call(Map args) {
 
        
            script {
                // Define variables from args
                def releaseName = args.releaseName
                def appName = args.appName
                def sonarHost = args.sonarHost ?: 'http://localhost:9000/sonar' // Default SonarQube host
                def sonarToken = args.sonarToken
                def pythonVersion = args.pythonVersion ?: '3.11' // Default Python version
                def coverageReportPath = args.coverageReportPath ?: 'coverage.xml'

                // Validate required parameters
                if (!releaseName || !appName || !sonarToken) {
                    error "releaseName, appName, and sonarToken are required but not provided in the configuration."
                }

                dir(releaseName) {
                    try {
                        echo "Running SonarQube analysis for project: ${appName}.app"
                        
                        // Run SonarQube analysis
                        sh """
                        sonar-scanner \
                        -Dsonar.projectKey=${appName}.app \
                        -Dsonar.sources=. \
                        -Dsonar.host.url=${sonarHost} \
                        -Dsonar.login=${sonarToken} \
                        -Dsonar.python.version=${pythonVersion} \
                        -Dsonar.python.coverage.reportPaths=${coverageReportPath}
                        """

                        // Set SonarQube dashboard URL
                        def sonarUrl = "${sonarHost}/dashboard?id=${appName}.app"
                        echo "SonarQube dashboard URL: ${sonarUrl}"
                        env.SONAR_URL = sonarUrl

                        // Retrieve the SonarQube quality gate status
                        def response = sh(
                            script: "curl -s -u ${sonarToken}: ${sonarHost}/api/qualitygates/project_status?projectKey=${appName}.app",
                            returnStdout: true
                        ).trim()

                        // Debug: Output the raw response
                        echo "Raw SonarQube API Response: ${response}"

                        // Check if the response is invalid
                        if (response.contains("HTTP Status 404")) {
                            error("SonarQube project not found: Received 404 Not Found from SonarQube API.")
                        }

                        // Parse the response to get quality gate status
                        try {
                            env.SONAR_STATUS = sh(script: "echo '${response}' | jq -r '.projectStatus.status'", returnStdout: true).trim()
                        } catch (Exception e) {
                            error("Failed to parse SonarQube API response: ${e.getMessage()}")
                        }

                        // Check the SonarQube status
                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
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
                    } catch (Exception e) {
                        echo "Error during SonarQube analysis: ${e.getMessage()}"
                        currentBuild.result = 'FAILURE'
                        error("Stopping pipeline due to SonarQube analysis failure.")
                    }
                }
            }
        }
    

