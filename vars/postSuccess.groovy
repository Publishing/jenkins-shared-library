import groovy.json.JsonOutput
// import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
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

        // Define the New Relic API URL
        def NEW_RELIC_API_URL = 'https://api.newrelic.com/v2/applications/{appName}/deployments.json'

        // Define the app ID arrays
        def NEW_RELIC_APP_IDS = [ // Production
            'archives': 1162084919,
            'api': 1354151907,
            'dotie': 1273358703,
            'feeds': 1278183236,
            'jpegresizer': 1354153467,
            'mediafeeds': 1288836505,
            'news': 1309460001,
            'newsapi': 1175287917,
            'rteavgen': 1384472309,
            'webhooks': 1244934873
        ]

        def NEW_RELIC_APP_IDS_BETA = [
            'archives': 1343752849,
            'api': 1354574881,
            'dotie': 1326627090,
            'feeds': 1204841593,
            'jpegresizer': 1121917174,
            'mediafeeds': 1165377349,
            'news': 1362707706,
            'newsapi': 1296099945,
            'rteavgen': 1250547311,
            'webhooks': 1244732569
        ]

        def NEW_RELIC_APP_IDS_DEV = [
            'api': 1348165422,
            'dotie': 1245684946,
            'archives': 1295804988,
            'feeds': 1265649480,
            'jpegresizer': 1292889609,
            'news': 1291326095,
            'newsapi': 1260879196,
            'rteavgen': 1384442476,
            'webhooks': 1183285327
        ]

        def NEW_RELIC_APP_IDS_NEXT = [
            'api': 1352406894,
            'dotie': 1258781423,
            'archives': 1295178005,
            'feeds': 1265670781,
            'jpegresizer': 1319499070,
            'news': 1309456381,
            'newsapi': 1260886891,
            'rteavgen': 1384470052
        ]

        def NEW_RELIC_APP_IDS_UAT = [
            'api': 1363437947,
            'dotie': 1347178004,
            'archives': 1381026066,
            'feeds': 1379506271,
            'jpegresizer': 1386066164,
            'newsapi': 1376763271,
            'rteavgen': 1386120661
        ]


        // Determine the app ID array to use based on the environment
        def appIds
        switch (params.TARGET_ENVIRONMENT) {
            case 'beta':
                appIds = NEW_RELIC_APP_IDS_BETA
                break
            case 'development':
                appIds = NEW_RELIC_APP_IDS_DEV
                break
            case 'prod':
                appIds = NEW_RELIC_APP_IDS
                break
            case 'next':
                appIds = NEW_RELIC_APP_IDS_NEXT
                break
            case 'uat':
                appIds = NEW_RELIC_APP_IDS_UAT
                break
            default:
                appIds = NEW_RELIC_APP_IDS_TEST // Default to test app IDs
        }

        // Determine the app ID to use
        def appId = appIds[appName]

        // Define other parameters
        def NEW_RELIC_API_KEY = credentials('new-relic') 
        

        // Get the current timestamp in the required format
        def timestamp = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))

        // Create the JSON payload
        def payload = [
            deployment: [
                description: "${deployer} deployed ${branchOrTag}",
                revision: branchOrTag,
                changelog: branchOrTag,
                user: deployer,
                timestamp: timestamp
            ]
        ]

        // Make the POST request
        def url = NEW_RELIC_API_URL.replace("{appName}", appId.toString())
        new URL(url).openConnection().with { connection ->
            connection.setRequestMethod("POST")
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-Api-Key", NEW_RELIC_API_KEY.toString())
            connection.doOutput = true

            // Write the payload to the request
            def writer = new OutputStreamWriter(connection.outputStream)
            writer.write(JsonOutput.toJson(payload))
            writer.flush()
            writer.close()

            // Get the response
            def responseCode = connection.responseCode
            def responseMessage = connection.responseMessage
            println "Response Code: ${responseCode}"
            println "Response Message: ${responseMessage}"
        }

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
