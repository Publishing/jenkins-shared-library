def call(Map args) {
    if (params.SELECT_WORK_FLOW == 'CI') {
        echo "Skipping manual approval for workflow CI."
        return
    }

    script {
        if (params.SELECT_WORK_FLOW in ['CI-CD', 'UD']) {
            def inputMessage = args.inputMessage ?: "Do you want to proceed with deployment?"
            def inputOkLabel = "Deploy" // The main "Proceed" button
            def reminderCounter = 0

            echo "Awaiting manual approval..."

            // Keep asking for approval until user clicks "Deploy" or "Cancel"
            while (true) {
                try {
                    // Wait 1 minute for user input
                    timeout(time: 1, unit: 'MINUTES') {
                        // If user clicks "Deploy", the code continues.
                        // If user clicks "Cancel", an AbortException is thrown.
                        input message: inputMessage,
                              ok: inputOkLabel

                        // ✅ If we reach here, user clicked "Deploy"
                        echo "✅ Deployment approved. Proceeding..."
                    }
                    // Break out of the loop if user clicked "Deploy"
                    break

                } catch (hudson.AbortException e) {
                    // The user clicked "Cancel" => Abort the pipeline
                    echo "🚨 Deployment aborted by user. Skipping remaining pipeline stages."
                    currentBuild.result = 'ABORTED'
                    error("Pipeline aborted manually.")

                } catch (Exception e) {
                    // Most likely a timeout after 1 minute
                    reminderCounter++

                    // After 5 minutes of waiting (5 timeouts), send a reminder email
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
