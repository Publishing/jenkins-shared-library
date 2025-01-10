def call() {
    stage('SonarQube Analysis') {
        steps {
            script {
                dir(env.RELEASE_NAME) {
                    try {
                        // Run SonarQube analysis
                        sh """
                        sonar-scanner \
                        -Dsonar.projectKey=${env.APP_NAME}.app \
                        -Dsonar.sources=. \
                        -Dsonar.host.url=http://localhost:9000/sonar \
                        -Dsonar.login=${env.SONAR_TOKEN} \
                        -Dsonar.python.version=3.11 \
                        -Dsonar.python.coverage.reportPaths=coverage.xml
                        """

                        // Capture the SonarQube Quality Gate status and dashboard URL
                        env.SONAR_URL = "https://djg-jenkins.rtegroup.ie/sonar/dashboard?id=${env.APP_NAME}.app"

                        // Retrieve the SonarQube quality gate status with debugging
                        def response = sh(
                            script: "curl -s -u ${env.SONAR_TOKEN}: http://localhost:9000/sonar/api/qualitygates/project_status?projectKey=${env.APP_NAME}.app", 
                            returnStdout: true
                        ).trim()

                        // Debug: Output the raw response
                        echo "Raw SonarQube API Response: ${response}"

                        // Check if the response is 404 or invalid
                        if (response.contains("HTTP Status 404")) {
                            error("SonarQube project not found: Received 404 Not Found from SonarQube API.")
                        }

                        // Attempt to parse the response with jq
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

                        echo "SonarQube dashboard URL: ${env.SONAR_URL}"

                    } catch (Exception e) {
                        echo "Error during SonarQube analysis: ${e.getMessage()}"
                        currentBuild.result = 'FAILURE'
                        error("Stopping pipeline due to SonarQube analysis failure.")
                    }
                }
            }
        }
    }
}
