def call(Map args) {
    if (params.SELECT_WORK_FLOW == 'CI') {
        echo "Skipping manual approval for workflow CI"
        return
    }

    script {
        if (params.SELECT_WORK_FLOW in ['CI-CD', 'UD']) {
            def appName = args.appName 
            def inputMessage = args.inputMessage ?: "Proceed with deployment to server?"
            def inputOkLabel = args.inputOkLabel ?: "Deploy"
            def submitterParameter = args.submitterParameter ?: 'approver'

            echo "Awaiting manual approval..."

            def jenkinsApprovalLink = "https://djg-jenkins.rtegroup.ie/job/CI-CD/job/${appName}" // Approval link

            def approvalRequest = null
            def reminderSent = false

            while (!approvalRequest) {
                try {
                    timeout(time: 1, unit: 'MINUTES') { // Check every 1 minute
                        approvalRequest = input message: inputMessage, 
                                               ok: inputOkLabel,
                                               submitterParameter: submitterParameter
                    }
                } catch (Exception e) {
                    if (!reminderSent) { // Send reminder only once after 5 minutes
                        reminderSent = true
                        echo "Approval request pending for 5 minutes. Sending reminder email..."

                        def adminEmail = "${params.DEPLOYER}"  // Change to actual admin email

                        def subject = "⚠️ Reminder: Approval Request Still Pending"
                        def body = """\
                            <html>
                                <body>
                                    <p>Hello,</p>
                                    <p>The deployment request is still awaiting approval.</p>
                                    <ul>
                                        <li><b>Workflow:</b> ${params.SELECT_WORK_FLOW}</li>
                                        <li><b>Approver:</b> ${submitterParameter}</li>
                                    </ul>
                                    <p>Please review and approve the request at your earliest convenience.</p>
                                    <p>
                                        <a href="${jenkinsApprovalLink}" style="background-color:#008CBA;color:white;padding:12px 20px;text-decoration:none;font-size:16px;border-radius:5px;display:inline-block;">
                                            ✅ Approve Request
                                        </a>
                                    </p>
                                    <p>Regards,<br>Jenkins</p>
                                </body>
                            </html>
                        """

                        // Send email notification with HTML button
                        emailext(
                            subject: subject,
                            body: body,
                            to: adminEmail,
                            mimeType: 'text/html' // Ensure email supports HTML formatting
                        )

                        echo "Reminder email sent to ${params.DEPLOYER}"
                    }
                }
            }

            // Retrieve the approver's email from Jenkins User API
            def approverUser = Jenkins.instance.getUser(approvalRequest)
            def approverEmail = approverUser?.getProperty(hudson.tasks.Mailer.UserProperty)?.getAddress()

            if (approverEmail) {
                echo "Approver's Email: ${approverEmail}"
                env.approver = approverEmail
            } else {
                echo "No email address found for approver: ${approvalRequest}"
                env.approver = approvalRequest
            }
        } else {
            echo "Manual approval not required for workflow: ${params.SELECT_WORK_FLOW}"
        }
    }
}
