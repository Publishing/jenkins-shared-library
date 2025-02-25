def call(Map args) {
    if (params.SELECT_WORK_FLOW == 'CI') {
        echo "Skipping manual approval for workflow CI."
        return
    }

    script {
        if (params.SELECT_WORK_FLOW in ['CI-CD', 'UD']) {

            def inputMessage = args.inputMessage ?: "Do you want to proceed with deployment?"
            def reminderCounter = 0

            echo "Awaiting manual approval..."

            // Loop until user either Deploys or Aborts
            while (true) {
                try {
                    // Wait 1 minute for user input
                    timeout(time: 1, unit: 'MINUTES') {
                        // Show two buttons: "Deploy" and "Abort"
                        def userAction = input(
                            message: inputMessage,
                            ok: "Deploy",            // main button
                            submitter: "Abort",      // second button
                            submitterParameter: "clickedBy"
                        )

                        echo "User clicked: ${userAction}"
                        
                        // If user clicked "Abort"
                        if ("Abort".equals(userAction)) {
                            echo "🚨 Deployment aborted by user. Skipping remaining pipeline stages."
                            currentBuild.result = 'ABORTED'
                            error("Pipeline aborted manually.")
                        }

                        // Otherwise, user clicked "Deploy" => break out of loop & proceed
                        echo "✅ Deployment approved. Proceeding..."
                    }
                    break // Once we succeed (user clicked Deploy), exit the while loop

                } catch (hudson.AbortException e) {
                    // Catches the top-level Jenkins "Cancel" link if used instead of "Abort" button
                    echo "🚨 Deployment manually aborted."
                    currentBuild.result = 'ABORTED'
                    error("Pipeline stopped due to manual abort.")

                } catch (Exception timeoutError) {
                    // We timed out after 1 minute (no one clicked anything)
                    reminderCounter++

                    // After 5 consecutive timeouts (5 min), send a one-time reminder
                    if (reminderCounter == 5) {
                        echo "Approval request pending for 5 minutes. Sending reminder email..."

                        def adminEmail = "${params.DEPLOYER}"
                        def subjectLine = "⚠️ Reminder: Approval Request Still Pending"

                        def emailBody = """\
                            <html>
                                <body>
                                    <p>Hello,</p>
                                    <p>The deployment request is still awaiting approval.</p>
                                    <ul>
                                        <li><b>Workflow:</b> ${params.SELECT_WORK_FLOW}</li>
                                        <li><b>Deployer:</b> ${params.DEPLOYER}</li>
                                    </ul>
                                    <p>Please review and approve the request at your earliest convenience.</p>
                                    <p>
                                        <a href="${env.BUILD_URL}" style="background-color:#008CBA;color:white;padding:12px 20px;text-decoration:none;font-size:16px;border-radius:5px;display:inline-block;">
                                            ✅ Approve Request
                                        </a>
                                    </p>
                                    <p>Regards,<br>Jenkins</p>
                                </body>
                            </html>
                        """

                        try {
                            mail to: adminEmail,
                                 subject: subjectLine,
                                 body: emailBody,
                                 mimeType: 'text/html'
                            echo "Reminder email sent to ${adminEmail}"
                        } catch (Exception emailError) {
                            echo "Failed to send reminder email: ${emailError.getMessage()}"
                        }
                    }
                }
            }

            echo "✅ Approval granted. Proceeding..."

        } else {
            echo "ℹ️ Manual approval not required for workflow: ${params.SELECT_WORK_FLOW}"
        }
    }
}
