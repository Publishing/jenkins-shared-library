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
            
            // Remove any existing flag file (to start fresh)
            if (fileExists('approvalFlag.txt')) {
                echo "Removing previous approval flag file..."
                sh "rm -f approvalFlag.txt"
            }
            
            def approvalResult = null
            
            // Define two parallel branches that share a flag file in the workspace.
            def branches = [:]
            
            branches['approval'] = {
                try {
                    // This input step waits indefinitely.
                    approvalResult = input message: inputMessage,
                                           ok: inputOkLabel,
                                           submitterParameter: submitterParameter
                    // Write a flag file to signal that input was received.
                    writeFile file: 'approvalFlag.txt', text: 'approved'
                } catch (err) {
                    // If the user clicks Abort, write a flag so the reminder branch won’t send an email.
                    writeFile file: 'approvalFlag.txt', text: 'aborted'
                    throw err  // Abort the pipeline immediately.
                }
            }
            
            branches['reminder'] = {
                // Sleep for 2 minutes.
                sleep time: 120, unit: 'SECONDS'
                // Check if the flag file exists; if not, send a reminder.
                if (!fileExists('approvalFlag.txt')) {
                    echo "No manual approval action taken within 2 minutes. Sending reminder email..."
                    mail to: params.DEPLOYER,
                         subject: "Reminder: Deployment Approval Pending",
                         body: "No manual approval was received within 2 minutes. Please take action if deployment is intended.",
                         mimeType: 'text/html'
                    // After sending the email, wait until the flag file is created.
                    waitUntil {
                        return fileExists('approvalFlag.txt')
                    }
                }
            }
            
            parallel branches
            
            // Process the approval result after both branches complete.
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
