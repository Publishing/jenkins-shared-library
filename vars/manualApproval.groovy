def call(Map args) {
    if (params.SELECT_WORK_FLOW == 'CI') {
        echo "Skipping manual approval for workflow CI"
        return
    }
    script {
        if (params.SELECT_WORK_FLOW in ['CI-CD', 'UD']) {
            def inputMessage = args.inputMessage ?: "Proceed with deployment to server?"
            def inputOkLabel = args.inputOkLabel ?: "Deploy"
            def submitterParameter = args.submitterParameter ?: 'approver'
            
            echo "Awaiting manual approval..."
            
            // Flags to track the approval state
            def approvalReceived = false
            def aborted = false
            
            // Start a background thread that waits 2 minutes and sends a reminder email if needed
            def reminderThread = Thread.start {
                sleep(2 * 60 * 1000) // 2 minutes (in milliseconds)
                // Only send the email if no approval has been received and the input wasn't aborted
                if (!approvalReceived && !aborted) {
                    echo "No manual approval action within 2 minutes. Sending reminder email..."
                    mail to: params.DEPLOYER,
                         subject: "Reminder: Deployment Approval Pending",
                         body: "No manual approval was received within 2 minutes. Please take action if deployment is intended.",
                         mimeType: 'text/html'
                }
            }
            
            def approval = null
            try {
                approval = input message: inputMessage, 
                                 ok: inputOkLabel,
                                 submitterParameter: submitterParameter
                // Mark that approval has been received
                approvalReceived = true
            } catch (err) {
                // If the user clicks Abort, mark as aborted so the reminder email isn’t sent
                aborted = true
                // Optionally, you can interrupt the reminder thread:
                // reminderThread.interrupt()
                throw err  // Immediately abort the pipeline
            }
            
            // Retrieve and log the approver's email after approval
            def approverUser = Jenkins.instance.getUser(approval)
            def approverEmail = approverUser?.getProperty(hudson.tasks.Mailer.UserProperty)?.getAddress()
            
            if (approverEmail) {
                echo "Approver's Email: ${approverEmail}"
                env.approver = approverEmail
            } else {
                echo "No email address found for approver: ${approval}"
                env.approver = approval
            }
        } else {
            echo "Manual approval not required for workflow: ${params.SELECT_WORK_FLOW}"
        }
    }
}
