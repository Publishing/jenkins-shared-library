def call(Map args) {
    try {
        // Extract required arguments or use defaults
        def appName = args.appName ?: env.APP_NAME
        def tmpDir = args.tmpDir ?: env.TMP_DIR
        def deployer = args.deployer ?: params.DEPLOYER
        def sonarUrl = args.sonarUrl ?: env.SONAR_URL
        def sonarStatus = args.sonarStatus ?: "Unknown"
        def emailRecipients = args.emailRecipients ?: 'abhishek.tiwari@rte.ie'
        def workflowTriggerUrl = args.workflowTriggerUrl ?: "https://prod-230.westeurope.logic.azure.com:443/workflows/9a393f61a96145c7acf7f906e7e2151b/triggers/manual/paths/invoke"

        // Trigger external workflow only for CI-CD or UD workflows
        if (params.SELECT_WORK_FLOW in ['CI-CD', 'UD']) {
            if (
                (params.SELECT_TARGET_OPTION == 'SERVER' && params.TARGET_SERVER == 'djangopybeta.rtegroup.ie') || 
                (params.SELECT_TARGET_OPTION == 'ENVIRONMENT' && params.TARGET_ENVIRONMENT == 'beta')
            ) {
                def branchOrTag = params.SELECT_CLONING_OPTION == 'BRANCH' ? params.BRANCH : params.TAG
                echo "Triggering external workflow for environment ${params.TARGET_ENVIRONMENT}"
                sh """
                curl -X POST -H "Content-Type: application/json" -d '{
                    "Environment": "${params.TARGET_ENVIRONMENT}",
                    "Application": "${appName}",
                    "BranchOrTag": "${branchOrTag}",
                    "Deployer": "${deployer}",
                    "JenkinsLogs": "${env.BUILD_URL}"
                }' "${workflowTriggerUrl}?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=MMVGSEtXit1LArXuRD2LV3slgsv31K49ORRaOTkBCGM" || true
                """
            }
        }

        // Parse the email log
        def emailLog = sh(script: "${tmpDir}/parse_log.sh", returnStdout: true).trim()
        def totalTests = ""
        def timeTaken = ""
        def testCoverage = ""
        def failedCases = ""

        if (appName in ['api', 'dotie']) {
            emailLog.split('\n').each { line ->
                if (line.startsWith('Total Tests:')) {
                    totalTests = line.replace('Total Tests:', '').trim()
                } else if (line.startsWith('Time Taken:')) {
                    timeTaken = line.replace('Time Taken:', '').trim()
                } else if (line.startsWith('Test Coverage:')) {
                    testCoverage = line.replace('Test Coverage:', '').trim()
                } else if (line.startsWith('Failed Case')) {
                    failedCases += line + "<br/>"
                }
            }
        }

        // Define dynamic content for the email
        def deploymentDetails = """\
            <table border="1" cellpadding="5" cellspacing="0">
            <tr style="background-color: lightblue;">
                <th colspan="2" style="text-align: center;">DEPLOYMENT DETAILS</th>
            </tr>
            <tr><th>Environment</th><td>${params.TARGET_ENVIRONMENT}</td></tr>
            <tr><th>Server</th><td>${params.TARGET_SERVER}</td></tr>
            <tr><th>Settings File</th><td>${params.SETTINGS_FILE}</td></tr>
            <tr><th>Application</th><td>${appName}</td></tr>
            <tr><th>Branch/Commit/Tag</th><td>${params.SELECT_CLONING_OPTION == 'TAG' ? params.TAG : params.BRANCH}</td></tr>
            <tr><th>Deployer</th><td>${deployer}</td></tr>
            <tr><th>Status</th><td>Successful</td></tr>
            </table>
        """

        def testingDetails = """\
            <table border="1" cellpadding="5" cellspacing="0">
            <tr style="background-color: lightblue;">
                <th colspan="2" style="text-align: center;">TESTING DETAILS</th>
            </tr>
            ${appName in ['api', 'dotie'] ? """
            <tr><th>Total Tests</th><td>${totalTests}</td></tr>
            <tr><th>Time Taken</th><td>${timeTaken}</td></tr>
            <tr><th>Test Coverage</th><td>${testCoverage}</td></tr>
            <tr><th>Failed Cases</th><td>${failedCases}</td></tr>
            """ : ""}
            <tr><th>SonarQube Status</th><td>${sonarStatus}</td></tr>
            </table>
        """

        // Construct the email body based on workflow
        def emailBody = "<html><body>"

        if (params.SELECT_WORK_FLOW == 'CI') {
            emailBody += "<p>${testingDetails}</p>"
        } else if (params.SELECT_WORK_FLOW == 'CI-CD') {
            emailBody += "<p>${deploymentDetails}</p><p>${testingDetails}</p>"
        } else if (params.SELECT_WORK_FLOW == 'UD') {
            emailBody += "<p>URGENT DEPLOYMENT!</p><p>${deploymentDetails}</p>"
        }

        emailBody += """
        <p>SonarQube Dashboard: <a href="${sonarUrl}">Sonar Dashboard</a></p>
        <p>Jenkins Logs: <a href="${env.BUILD_URL}">Jenkins Logs</a></p>
        </body></html>
        """

        def subjectLine = "Build ${currentBuild.fullDisplayName} succeeded"

        // Send email
        mail to: emailRecipients,
             subject: subjectLine,
             body: emailBody,
             mimeType: 'text/html'

        echo "Success notification sent."

    } catch (Exception e) {
        echo "Error in postSuccess: ${e.getMessage()}"
        throw e
    }
}
