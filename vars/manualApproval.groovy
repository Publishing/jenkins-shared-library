def call() {
    stage('Manual Approval') {
        when {
            expression { return params.SELECT_WORK_FLOW == 'CI-CD' }
        }
        steps {
            script {
                // Capture the approver's Jenkins ID
                def approval = input message: "Proceed with deployment to server?", 
                                    ok: "Deploy",
                                    submitterParameter: 'approver' // Store the approver's ID
                
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
