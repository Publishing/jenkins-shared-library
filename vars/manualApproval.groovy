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
            
            def approval = null
            // Shared flag to indicate if approval has been provided
            def approvalDone = false

            // Run two parallel branches:
            // 1. One branch waits for the input.
            // 2. The other waits 2 minutes, then sends a reminder email if no input has been given.
            parallel(
                approvalBranch: {
                    approval = input message: inputMessage, 
                                     ok: inputOkLabel,
                                     submitterParameter: submitterParameter
                    approvalDone = true  // Mark that approval was received
                },
                reminderBranch: {
                    // Check every 5 seconds for up to 2 minutes
                    int waited = 0
                    while (waited < 120 && !approvalDone) {
                        sleep 5
                        waited += 5
                    }
                    if (!approvalDone) {
                        echo "No manual approval action taken within 2 minutes. Sending reminder email..."
                        mail to: params.DEPLOYER,
                             subject: "Reminder: Deployment Approval Pending",
                             body: "No manual approval was received within 2 minutes. Please take action if deployment is intended.",
                             mimeType: 'text/html'
                    }
                }
            )

            // After input is received, retrieve and log the approver's email
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
