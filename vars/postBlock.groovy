    post {
    always {
        cleanWs()  // Cleanup workspace after every run
    }

    success {
        script {
            // Validate and trigger the workflow
            if (params.TARGET_SERVER == 'djangopybeta.rtegroup.ie' || params.TARGET_ENVIRONMENT == 'beta') {
                // Determine the Branch/Tag value
                def branchOrTag = params.SELECT_CLONING_OPTION == 'BRANCH' ? params.BRANCH : params.TAG

                // Trigger the workflow using curl
                sh """
                curl -X POST -H "Content-Type: application/json" -d '{
                    "Environment": "${params.TARGET_ENVIRONMENT}",
                    "Application": "${env.APP_NAME}",
                    "BranchOrTag": "${branchOrTag}",
                    "Deployer": "${params.DEPLOYER}",
                    "JenkinsLogs": "${env.BUILD_URL}"
                }' "https://prod-230.westeurope.logic.azure.com:443/workflows/9a393f61a96145c7acf7f906e7e2151b/triggers/manual/paths/invoke?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=MMVGSEtXit1LArXuRD2LV3slgsv31K49ORRaOTkBCGM" || true
                """
            }
            def emailLog = sh(script: "${env.TMP_DIR}/parse_log.sh", returnStdout: true).trim()
            def emailBody = ""
            def subjectLine = ""

            // Initialize variables
            def lintStatus = ""

            // Parse the emailLog into variables
            emailLog.split('\n').each { line ->
                if (line.startsWith('Lint Status:')) {
                    lintStatus = line.replace('Lint Status:', '').trim()
                } else if (line.startsWith('Total Tests:')) {
                    totalTests = line.replace('Total Tests:', '').trim()
                } else if (line.startsWith('Time Taken:')) {
                    timeTaken = line.replace('Time Taken:', '').trim()
                } else if (line.startsWith('Test Coverage:')) {
                    testCoverage = line.replace('Test Coverage:', '').trim()
                } else if (line.startsWith('Failed Case')) {
                    failedCases += line + "<br/>"  // Add a line break for HTML formatting
                }
            }

            // Add SonarQube details
            def sonarStatus = env.SONAR_STATUS ?: "Unknown"
            def sonarUrl = env.SONAR_URL ?: "N/A"

            // Define the dynamic content for the table
            def environment = params.TARGET_ENVIRONMENT  // Placeholder for Environment
            def server = params.TARGET_SERVER
            def settingsFile = params.SETTINGS_FILE
            def appName = env.APP_NAME
            def branch = (params.SELECT_CLONING_OPTION == 'TAG') ? params.TAG : params.BRANCH
            def deployer = env.DEPLOYER ?: "Not Available"
            def status = "Successful"

            // Construct the HTML table for deployment details
            def tableContent = """\
                <table border="1" cellpadding="5" cellspacing="0">
                <tr style="background-color: lightblue;">
                    <th colspan="2" style="text-align: center;">DEPLOYMENT DETAILS</th>
                </tr>
                <tr>
                    <th>Environment</th><td>${environment}</td>
                </tr>
                <tr>
                    <th>Server</th><td>${server}</td>
                </tr>
                <tr>
                    <th>Settings File</th><td>${settingsFile}</td>
                </tr>
                <tr>
                    <th>Application</th><td>${appName}</td>
                </tr>
                <tr>
                    <th>Branch/Commit/Tag</th><td>${branch}</td>
                </tr>
                <tr>
                    <th>Deployer</th><td>${deployer}</td>
                </tr>
                <tr>
                    <th>Status</th><td>${status}</td>
                </tr>
            </table>
            """

            // Construct the HTML table for testing details
            def testingDetailsTable = """\
                <table border="1" cellpadding="5" cellspacing="0">
                <tr style="background-color: lightblue;">
                    <th colspan="2" style="text-align: center;">TESTING DETAILS</th>
                </tr>
                <tr>
                    <th>Lint Status</th><td>${lintStatus}</td>
                </tr>
                <tr>
                    <th>SonarQube Status</th><td>${sonarStatus}</td>
                </tr>
            </table>
            """

            // Construct the email body and add SonarQube analysis details
            if (params.SELECT_WORK_FLOW == 'CI-CD') {
                emailBody = """\
<html>
    <body>
        <p>Deployment was successful! [CI-CD]</p>
        <p>Build ${currentBuild.fullDisplayName} succeeded.</p>
        
        <p> </p>
        ${tableContent}

        <p> </p>
        ${testingDetailsTable}
        <p>Checkout the Sonarqube Dashboard:</p>
        <a href="${sonarUrl}" style="display: inline-block; padding: 10px 20px; background-color: lightblue; color: black; text-decoration: none; border-radius: 5px; font-weight: bold;">Sonar Dashboard</a>
        <p>Checkout the Jenkins logs:</p>
        <a href="${env.BUILD_URL}" style="display: inline-block; padding: 10px 20px; background-color: lightblue; color: black; text-decoration: none; border-radius: 5px; font-weight: bold;">Jenkins Logs</a>
    </body>
</html>
                """
                subjectLine = "CI-CD Build ${currentBuild.fullDisplayName} succeeded"
            } else if (params.SELECT_WORK_FLOW == 'CI') {
                emailBody = """\
<html>
    <body>
        <p>Testing was successful! [CI]</p>
        <p>Build ${currentBuild.fullDisplayName} succeeded.</p>

        <p> </p>
        ${testingDetailsTable}
        <p>Checkout the Sonarqube Dashboard:</p>
        <a href="${sonarUrl}" style="display: inline-block; padding: 10px 20px; background-color: lightblue; color: black; text-decoration: none; border-radius: 5px; font-weight: bold;">Sonar Dashboard</a>
        <p>Checkout the Jenkins logs:</p>
        <a href="${env.BUILD_URL}" style="display: inline-block; padding: 10px 20px; background-color: lightblue; color: black; text-decoration: none; border-radius: 5px; font-weight: bold;">Jenkins Logs</a>
    </body>
</html>
                """
                subjectLine = "CI Build ${currentBuild.fullDisplayName} succeeded"
            }

            // Send the email with HTML content
            //mail to: 'abhishek.tiwari@rte.ie, hasan.agha@rte.ie, mehdi.khrichfa@rte.ie, vadym.proskurin@rte.ie, stephen.hanrahan@rte.ie',
            mail to: 'abhishek.tiwari@rte.ie',
                 subject: subjectLine,
                 body: emailBody,
                 mimeType: 'text/html'  // Set the MIME type to HTML
        }
    }

    failure {
        script {
            // Send a failure notification
            //mail to: 'abhishek.tiwari@rte.ie, hasan.agha@rte.ie, mehdi.khrichfa@rte.ie, vadym.proskurin@rte.ie, stephen.hanrahan@rte.ie',
            mail to: 'abhishek.tiwari@rte.ie',
                 subject: "Build ${currentBuild.fullDisplayName} failed",
                 body: "Build ${currentBuild.fullDisplayName} has failed. Please check the logs at ${env.BUILD_URL}.",
                 mimeType: 'text/plain'
        }
    }
}
