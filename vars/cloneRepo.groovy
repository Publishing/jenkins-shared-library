def call(Map args) {
    script {
        
        def RELEASE_NAME = args.RELEASE_NAME
        def GIT_CREDENTIALS_ID = args.gitCredentialsId
        def GIT_REPO_URL = args.gitRepoUrl

        echo "Setting RELEASE_NAME to: ${RELEASE_NAME}"
        echo "Using Git Repository: ${GIT_REPO_URL}"

        // Create the release directory
        sh "mkdir -p ${RELEASE_NAME}"

        // Navigate to the release directory
        dir(RELEASE_NAME) {
            if (params.SELECT_CLONING_OPTION == 'BRANCH') {
                echo "Cloning repository using branch: ${params.BRANCH}"
                git branch: params.BRANCH, credentialsId: GIT_CREDENTIALS_ID, url: GIT_REPO_URL
            } else if (params.SELECT_CLONING_OPTION == 'TAG') {
                echo "Cloning repository and checking out tag: ${params.TAG}"
                git credentialsId: GIT_CREDENTIALS_ID, url: GIT_REPO_URL
                // Fetch all tags explicitly
                sh "git fetch --tags"
                // Checkout the specific tag
                sh "git checkout -b temp-branch tags/${params.TAG}"
            } else {
                error "Invalid SELECT_CLONING_OPTION value: ${params.SELECT_CLONING_OPTION}. Must be either 'BRANCH' or 'TAG'."
            }
            // Verify the directory contents
            echo "Verifying contents of ${RELEASE_NAME}..."
            sh "ls -l && cat Pipfile"
        }
    }
}
