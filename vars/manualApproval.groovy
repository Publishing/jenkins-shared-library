def call(Map args) {
    if (params.SELECT_WORK_FLOW == 'CI') {
        echo "Skipping manual approval for workflow CI."
        return
    }

    script {
        if (params.SELECT_WORK_FLOW in ['CI-CD', 'UD']) {
            def inputMessage = args.inputMessage ?: "Do you want to proceed with deployment?"
            def inputOkLabel = "Deploy"   // ✅ Creates the "Deploy" button
            def inputAbortLabel = "Abort" // ✅ Creates the "Abort" button
            def reminderCounter = 0  

            echo "Awaiting manual approval..."

            try {
                timeout(time: 1, unit: 'MINUTES') { // Wait for approval for 1 minute
                    def approval = input(
                        message: inputMessage,
                        parameters: [
                            choice(name: 'Approval', choices: ['Deploy', 'Abort'], description: "Select 'Deploy' to continue or 'Abort' to cancel")
                        ]
                    )

                    if (approval == 'Abort') { // 🚨 Handle "Abort" button click
                        echo "🚨 Deployment aborted by user. Skipping remaining pipeline stages."
                        currentBuild.result = 'ABORTED' // ✅ Marks build as "Aborted" (skips next stages)
                        error("Pipeline aborted manually.") // ✅ Stops execution
                    }

                    echo "✅ Deployment approved. Proceeding with deployment..."
                }
            } catch (hudson.AbortException e) { 
                echo "🚨 Deployment manually aborted."
                currentBuild.result = 'ABORTED' // ✅ Mark build as "Aborted"
                error("Pipeline stopped due to manual abort.") // ✅ Stops execution
            } catch (Exception e) { // Handle timeout or errors
                reminderCounter++

                if (reminderCounter == 1) { // Send reminder after 5 minutes
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

            echo "✅ Approval granted. Proceeding..."
        } else {
            echo "ℹ️ Manual approval not required for workflow: ${params.SELECT_WORK_FLOW}"
        }
    }
}
