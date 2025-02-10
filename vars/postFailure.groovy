def call(Map args) {
    try {
        // Extract required arguments or use defaults
        def buildUrl = args.BUILD_URL ?: env.BUILD_URL
        def emailRecipients = args.emailRecipients ?: params.DEPLOYER
        def errorMessage = args.errorMessage ?: "Unknown Error"

        // Construct the email body
        def emailBody = """\
Build ${currentBuild.fullDisplayName} has failed.
Error Details: ${errorMessage}
Please check the logs at: ${buildUrl}.
        """

        // Send email notification
        mail to: emailRecipients,
             subject: "Build ${currentBuild.fullDisplayName} failed",
             body: emailBody,
             mimeType: 'text/plain'

        echo "Failure notification sent."

    } catch (Exception e) {
        echo "Error in postFailure: ${e.getMessage()}"
        throw e
    }
}
