def call(Map args) {
    if (params.SELECT_WORK_FLOW == 'CI') {
        echo "Skipping manual approval for workflow CI"
        return
    }
    script {
        if (params.SELECT_WORK_FLOW in ['CI-CD', 'UD']) {
            //def inputMessage = args.inputMessage ?: "Proceed with deployment to server?"
            def inputMessage = ""
            if (params.SELECT_CLONING_OPTION == 'TAG') {
            inputMessage = "Deploying ${args.appName} TAG : ${params.TAG}, on ${params.TARGET_ENVIRONMENT} environemnt."
             } else {
            inputMessage = "Deploying ${args.appName} TAG : ${params.BRANCH}, on ${params.TARGET_ENVIRONMENT} environemnt."
            }
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
                // Poll for the flag file every 5 seconds, up to 2 minutes.
                def waited = 0
                def interval = 5
                while (waited < 120 && !fileExists('approvalFlag.txt')) {
                    sleep time: interval, unit: 'SECONDS'
                    waited += interval
                }
                // If after polling 2 minutes the flag file is still absent, send a reminder email.
                if (!fileExists('approvalFlag.txt')) {
                    echo "No manual approval action taken within 2 minutes. Sending reminder email..."
                    mail to: params.DEPLOYER,
                     subject: "Reminder: ${args.appName} Deployment Approval Pending",
                     body: """
                       <html>
                         <body>
                           <p>No manual approval was received within 2 minutes. Please take action if deployment is intended.</p>
                           <p>
                             <a href="https://djg-jenkins.rtegroup.ie/job/CI-CD/job/${args.appName}/" 
                                style="background-color: #4CAF50; color: white; padding: 10px 20px; text-align: center;
                                       text-decoration: none; display: inline-block; font-size: 16px; border-radius: 4px;">
                                Approve Deployment
                             </a>
                           </p>
                         </body>
                       </html>
                     """,
                     mimeType: 'text/html'
                    // After sending the reminder, wait until the flag file is created.
                    waitUntil { fileExists('approvalFlag.txt') }
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
