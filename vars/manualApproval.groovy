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
            
            def approvalResult = null
            // To ensure we send the reminder email only once
            def reminderSent = false
            
            // Loop until an approval is received.
            while (approvalResult == null) {
                try {
                    // Wrap the input prompt in a 2‑minute timeout.
                    approvalResult = timeout(time: 2, unit: 'MINUTES') {
                        input message: inputMessage,
                              ok: inputOkLabel,
                              submitterParameter: submitterParameter
                    }
                } catch (err) {
                    // Check if the exception is a timeout.
                    if (err.toString().contains("Timeout")) {
                        if (!reminderSent) {
                            echo "No manual approval action taken within 2 minutes. Sending reminder email..."
                            // Send reminder email to the deployer.
                            mail to: params.DEPLOYER,
                                 subject: "Reminder: Deployment Approval Pending",
                                 body: "No manual approval was received within 2 minutes. Please take action if deployment is intended.",
                                 mimeType: 'text/html'
                            reminderSent = true
                        }
                        // Loop will re‑issue the input prompt.
                    } else {
                        // If the user clicks Abort, input throws an exception. Propagate it immediately.
                        throw err
                    }
                }
            }
            
            // Process the approval result after a valid input is received.
            def approverUser = Jenkins.instance.getUser(approvalResult)
            def approverEmail = approverUser?.getProperty(hudson.tasks.Mailer.UserProperty)?.getAddress()
            
            if (approverEmail) {
                echo "Approver's Email: ${approverEmail}"
                env.approver = approverEmail
            } else {
                echo "No email address found for approver: ${approvalResult}"
                env.approver = approvalResult
            }
        } else {
            echo "Manual approval not required for workflow: ${params.SELECT_WORK_FLOW}"
        }
    }
}
