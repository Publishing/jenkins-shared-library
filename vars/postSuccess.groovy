import groovy.json.JsonOutput
def call(Map args) {
    
    try {
        // Extract required arguments or use defaults
        def appName = args.appName ?: env.APP_NAME
        def tmpDir = args.tmpDir ?: env.TMP_DIR
        def deployer = args.deployer ?: params.DEPLOYER
        def sonarUrl = "https://djg-jenkins.rtegroup.ie/sonar/dashboard?id=${appName}.app"
        def sonarStatus = args.sonarStatus ?: "Unknown"
        def emailRecipients = args.emailRecipients ?: params.DEPLOYER
        def branchOrTag = params.SELECT_CLONING_OPTION == 'BRANCH' ? params.BRANCH : params.TAG
        // Define workflow URLs
        def deploymentWorkflowTriggerUrl = "https://prod-230.westeurope.logic.azure.com:443/workflows/9a393f61a96145c7acf7f906e7e2151b/triggers/manual/paths/invoke?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=MMVGSEtXit1LArXuRD2LV3slgsv31K49ORRaOTkBCGM"
        def udWorkflowTriggerUrl = "https://prod-96.westeurope.logic.azure.com:443/workflows/5eb03b72dde44648aab564d9754309f2/triggers/manual/paths/invoke?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=urdJ4vOyPLzcUK5Cxnv5OHVwxsb_IL2JujbDDacGIV0"
        def testNotificationWorkflowUrl = "https://prod-28.westeurope.logic.azure.com:443/workflows/627e297c3a034f44b60a723a67656dfc/triggers/manual/paths/invoke?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=ZOpRzUn460kzxkMLwf1nh0etTnJM3GT2PdSecXasm-w"


        // Trigger external workflow only for CI-CD or UD workflows
        if (params.SELECT_WORK_FLOW in ['CI-CD', 'UD']) {
            def triggerUrl = params.SELECT_WORK_FLOW == 'CI-CD' ? deploymentWorkflowTriggerUrl : udWorkflowTriggerUrl
            if (
                (params.SELECT_TARGET_OPTION == 'SERVER' && params.TARGET_SERVER == 'djangopybeta.rtegroup.ie') || 
                (params.SELECT_TARGET_OPTION == 'ENVIRONMENT' && params.TARGET_ENVIRONMENT == 'beta')
            ) {
                
                echo "Triggering external deployment workflow for environment: ${params.TARGET_ENVIRONMENT}"
        
                // Trigger the external workflow (Deployment)
                sh """
                curl -X POST -H "Content-Type: application/json" -d '{
                    "Environment": "${params.TARGET_ENVIRONMENT}",
                    "Application": "${appName}",
                    "BranchOrTag": "${branchOrTag}",
                    "Deployer": "${deployer}",
                    "JenkinsLogs": "${env.BUILD_URL}"
                }' "${triggerUrl}" || true
                """

                // Define Jenkins base URL and credentials
                def jenkinsBaseUrl = "https://djg-jenkins.rtegroup.ie/job/Testing/job/Pipelines/job"
                def apiToken = "111873919e378af63c1f145faf448f8e6e"
                def jenkinsUser = "abhishek"

                // List of Jenkins pipelines to trigger
                def pipelines = ["dotie_all_pages_245", "RTEAPIEndpointsTests", "RTEAPISportsPageTests", "RTEUIHomePageTests"]

                // Initialize an empty list to store pipeline monitoring URLs
                def monitoringData = []

                // Trigger all pipelines
                pipelines.each { pipelineName ->
                    echo "Triggering pipeline: ${pipelineName}"
                    
                    sh """
                    CRUMB=\$(curl -k -s -u ${jenkinsUser}:${apiToken} 'https://djg-jenkins.rtegroup.ie/crumbIssuer/api/xml?xpath=concat(//crumbRequestField,":",//crumb)')
                    curl -k -s -u ${jenkinsUser}:${apiToken} -X POST -H "\$CRUMB" \
                    "${jenkinsBaseUrl}/${pipelineName}/buildWithParameters" \
                    --data-urlencode "UI_Test_Environment=prod"
                    """
                
                    
                }
                
                // Convert monitoring data to JSON format & escape quotes
                def jsonData = JsonOutput.toJson(monitoringData).replace('"', '\\"')
                
                // Send all pipeline statuses to Power Automate for Teams notification
                echo "Sending workflow trigger with all pipeline monitoring URLs"
                sh """
                curl -X POST '${testNotificationWorkflowUrl}'
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
            <tr><th>Branch/Commit/Tag</th><td>${branchOrTag}</td></tr>
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

        if (params.SELECT_WORK_FLOW == 'UD') {
            emailBody += """
                <p>Jenkins Logs: <a href="${env.BUILD_URL}">Jenkins Logs</a></p>
                </body></html>
                """
        } else {
            emailBody += """
            <p>
              <a href="${sonarUrl}"
                 style="
                   background-color: #ADDDE6; 
                   color: black; 
                   padding: 10px 20px; 
                   text-decoration: none; 
                   border-radius: 4px; 
                   font-weight: bold; 
                   margin-right: 10px;
                 ">
                Sonar Dashboard
              </a>
              <a href="${env.BUILD_URL}"
                 style="
                   background-color: #ADDDE6; 
                   color: black; 
                   padding: 10px 20px; 
                   text-decoration: none; 
                   border-radius: 4px; 
                   font-weight: bold;
                 ">
                Jenkins Logs
              </a>
            </p>
            </body>
            </html>
            """


        }


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
