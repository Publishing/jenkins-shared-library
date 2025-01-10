def call(Map args) {
    post {
        always {
            cleanWs()  // Cleanup workspace after every run
        }

        success {
            script {
                // Extract required arguments or use defaults
                def appName = args.appName ?: env.APP_NAME
                def tmpDir = args.tmpDir ?: '/tmp'
                def deployer = args.deployer ?: params.DEPLOYER
                def sonarUrl = args.sonarUrl ?: env.SONAR_URL
                def sonarStatus = args.sonarStatus ?: env.SONAR_STATUS
                def emailRecipients = args.emailRecipients ?: 'abhishek.tiwari@rte.ie'
                def workflowTriggerUrl = args.workflowTriggerUrl ?: "https://prod-230.westeurope.logic.azure.com:443/workflows/9a393f61a96145c7acf7f906e7e2151b/triggers/manual/paths/invoke"

                // Validate and trigger the workflow if conditions are met
                if (params.TARGET_SERVER == 'djangopybeta.rtegroup.ie' || params.TARGET_ENVIRONMENT == 'beta') {
                    def branchOrTag = params.SELECT_CLONING_OPTION == 'BRANCH' ? params.BRANCH : params.TAG

                    echo "Triggering external workflow for environment ${params.TARGET_ENVIRONMENT}"
                    sh """
                    curl -X POST -H "Content-Type: application/json" -d '{
                        "Environment": "${params.TARGET_ENVIRONMENT}",
                        "Application": "${appName}",
                        "BranchOrTag": "${branchOrTag}",
                        "Deployer": "${deployer}",
                        "JenkinsLogs": "${env.BUILD_URL}"
                    }' "${workflowTriggerUrl}" || true
                    """
                }

                // Parse log file
                def emailLog = sh(script: "${tmpDir}/parse_log.sh", returnStdout: true).trim()
                def emailBody = ""
                def subjectLine = ""

                // Initialize variables
                def lintStatus = ""
                def totalTests = ""
                def timeTaken = ""
                def testCoverage = ""
                def failedCases = ""

                // Parse the email log into variables
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

                // Define deployment details table
                def environment = params.TARGET_ENVIRONMENT
                def server = params.TARGET_SERVER
                def settingsFile = params.SETTINGS_FILE
                def branch = params.SELECT_CLONING_OPTION == 'TAG' ? params.TAG : params.BRANCH
                def status = "Successful"

                def tableContent = """\
                    <table border="1" cellpadding="5" cellspacing="0">
                    <tr style="background-color: lightblue;">
                        <th colspan="2" style="text-align: center;">DEPLOYMENT DETAILS</th>
                    </tr>
                    <tr><th>Environment</th><td>${environment}</td></tr>
                    <tr><th>Server</th><td>${server}</td></tr>
                    <tr><th>Settings File</th><td>${settingsFile}</td></tr>
                    <tr><th>Application</th><td>${appName}</td></tr>
                    <tr><th>Branch/Commit/Tag</th><td>${branch}</td></tr>
                    <tr><th>Deployer</th><td>${deployer}</td></tr>
                    <tr><th>Status</th><td>${status}</td></tr>
                    </table>
                """

                def testingDetailsTable = """\
                    <table border="1" cellpadding="5" cellspacing="0">
                    <tr style="background-color: lightblue;">
                        <th colspan="2" style="text-align: center;">TESTING DETAILS</th>
                    </tr>
                    <tr><th>Lint Status</th><td>${lintStatus}</td></tr>
                    <tr><th>SonarQube Status</th><td>${sonarStatus}</td></tr>
                    </table>
                """

                // Construct email body
                if (params.SELECT_WORK_FLOW == 'CI-CD') {
                    emailBody = """\
<html>
    <body>
        <p>Deployment was successful! [CI-CD]</p>
        <p>Build ${currentBuild.fullDisplayName} succeeded.</p>
        <p>${tableContent}</p>
        <p>${testingDetailsTable}</p>
        <p>Checkout the SonarQube Dashboard: <a href="${sonarUrl}">Sonar Dashboard</a></p>
        <p>Checkout the Jenkins logs: <a href="${env.BUILD_URL}">Jenkins Logs</a></p>
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
        <p>${testingDetailsTable}</p>
        <p>Checkout the SonarQube Dashboard: <a href="${sonarUrl}">Sonar Dashboard</a></p>
        <p>Checkout the Jenkins logs: <a href="${env.BUILD_URL}">Jenkins Logs</a></p>
    </body>
</html>
                    """
                    subjectLine = "CI Build ${currentBuild.fullDisplayName} succeeded"
                }

                // Send email
                mail to: emailRecipients,
                     subject: subjectLine,
                     body: emailBody,
                     mimeType: 'text/html'
            }
        }

        failure {
            script {
                mail to: 'abhishek.tiwari@rte.ie',
                     subject: "Build ${currentBuild.fullDisplayName} failed",
                     body: "Build ${currentBuild.fullDisplayName} has failed. Please check the logs at ${env.BUILD_URL}.",
                     mimeType: 'text/plain'
            }
        }
    }
}
