def call(Map args) {
    script {
        // Dynamically generate the release name if not provided
        def RELEASE_DATE = args.RELEASE_DATE ?: sh(script: 'date +%d%m%y%H%M', returnStdout: true).trim()
        def VERSION = args.VERSION ?: sh(script: "cat ${env.VERSION_FILE} || echo 'V1.0.0'", returnStdout: true).trim()
        def RELEASE_NAME = args.RELEASE_NAME ?: "${args.appName}_release.${RELEASE_DATE}.${VERSION}"
        def GIT_CREDENTIALS_ID = args.gitCredentialsId
        def GIT_REPO_URL = args.gitRepoUrl

        echo "Setting dynamically generated RELEASE_NAME to: ${RELEASE_NAME}"
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
        
        // Clone the repository using Jenkins `git` step
        git credentialsId: GIT_CREDENTIALS_ID, url: GIT_REPO_URL

        // Use credentials for `git fetch` and `git checkout` commands
        withCredentials([usernamePassword(credentialsId: GIT_CREDENTIALS_ID, usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD')]) {
            sh """
                git config credential.helper 'store'
                echo "https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com" > ~/.git-credentials
                git fetch --tags
                git checkout -b temp-branch ${params.TAG}
            """
                }
            } else {
                error "Invalid SELECT_CLONING_OPTION value: ${params.SELECT_CLONING_OPTION}. Must be either 'BRANCH' or 'TAG'."
            }
        }

        // Return the release name for reference in subsequent stages
        return RELEASE_NAME
    }
}
