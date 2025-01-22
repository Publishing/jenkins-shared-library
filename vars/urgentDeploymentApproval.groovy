def call(Map args) {

    // Define the approver's Jenkins ID
    def approverID = 'abhishek'

    // Fetch the email associated with the Jenkins ID
    def approverUser = Jenkins.instance.getUser(approverID)
    def approverEmail = approverUser?.getProperty(hudson.tasks.Mailer.UserProperty)?.getAddress()

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
    input message: "Do you want to proceed with the deployment?",
          ok: "Approve",
          submitter: approverID,  // Dynamically set the approver Jenkins ID
          submitterParameter: 'approver'  // Capture who approved
}
