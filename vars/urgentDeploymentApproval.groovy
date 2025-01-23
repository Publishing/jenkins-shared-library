def call(Map args = [:]) {

    if (params.SELECT_WORK_FLOW in ['CI', 'CI-CD']) {
        echo "Skipping manual approval for workflow ${params.SELECT_WORK_FLOW}."
        return
    }

    // Define the approver's Jenkins ID
    def approverID = 'abhishek'

    // Fetch the email associated with the Jenkins ID directly
    def approverEmail = Jenkins.instance.getUser(approverID)?.getProperty(hudson.tasks.Mailer.UserProperty)?.getAddress()

    if (!approverEmail) {
        error("No email address found for Jenkins user ID: ${approverID}")
    }

    def consoleUrl = args.consoleUrl ?: error("consoleUrl is required.")
    def currentBuild = args.currentBuild ?: error("currentBuild is required.")

    // Send an email notification to the approver
    mail to: approverEmail,
         subject: "Approval Required for Deployment - ${currentBuild.fullDisplayName}",
         body: """\
<html>
    <body>
        <p>Hi,</p>
        <p>The pipeline is awaiting your approval to proceed with the deployment for build <strong>${currentBuild.fullDisplayName}</strong>.</p>
        <p>Please click the following button to approve the deployment:</p>
        <p>
            <a href="${consoleUrl}" style="display: inline-block; 
               padding: 10px 20px; 
               background-color: lightblue; 
               color: black; 
               text-decoration: none; 
               border-radius: 5px; font-weight: bold;">
               APPROVE
            </a>
        </p>
        <br>
        <p>Best Regards,</p>
        <p>Jenkins CI-CD Pipeline</p>
    </body>
</html>
         """,
         mimeType: 'text/html'

    // Request approval from the approver
    def approver = input message: "Do you want to proceed with the deployment?",
                  ok: "Approve",
                  submitter: approverID,  // Dynamically set the approver Jenkins ID
                  submitterParameter: 'approver'  // Capture who approved

    // Verify the actual approver
    if (approver != approverID) {
        error("Unauthorized approval detected. Only ${approverID} can approve this step.")
    }

    echo "Approval granted by: ${approver}"
}
