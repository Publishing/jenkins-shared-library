def call(Map args) {
    stage('Manual Approval') {
        when {
            expression { return params.SELECT_WORK_FLOW == 'CI-CD' } // Access pipeline parameter directly
        }
        steps {
            script {
                // Define optional variables from args
                def inputMessage = args.inputMessage ?: "Proceed with deployment to server?"
                def inputOkLabel = args.inputOkLabel ?: "Deploy"
                def submitterParameter = args.submitterParameter ?: 'approver'

                echo "Awaiting manual approval..."

                // Capture the approver's Jenkins ID
                def approval = input message: inputMessage, 
                                    ok: inputOkLabel,
                                    submitterParameter: submitterParameter
                
                // Retrieve the approver's email from Jenkins User API
                def approverUser = Jenkins.instance.getUser(approval)
                def approverEmail = approverUser?.getProperty(hudson.tasks.Mailer.UserProperty)?.getAddress()

                if (approverEmail) {
                    echo "Approver's Email: ${approverEmail}"
                    env.approver = approverEmail // Overwrite with the approver's email
                } else {
                    echo "No email address found for approver: ${approval}"
                    env.approver = approval // Retain the original approver's ID if email is not available
                }
            }
        }
    }
}
