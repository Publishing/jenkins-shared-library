def call(Map args) {
    script {
        // Dynamically generate the release name if not provided
        def RELEASE_DATE = args.RELEASE_DATE ?: sh(script: 'date +%d%m%y%H%M', returnStdout: true).trim()
        def VERSION = args.VERSION ?: sh(script: "cat ${env.VERSION_FILE} || echo 'V1.0.0'", returnStdout: true).trim()
        def RELEASE_NAME = args.RELEASE_NAME ?: "${args.appName}_release.${RELEASE_DATE}.${VERSION}"
        def GIT_REPO_URL = args.gitRepoUrl

        echo "Setting dynamically generated RELEASE_NAME to: ${RELEASE_NAME}"
        echo "Using Git Repository: ${GIT_REPO_URL}"
        
        // Create the release directory
        sh "mkdir -p ${RELEASE_NAME}"

        // Navigate to the release directory
        dir(RELEASE_NAME) {
            if (params.SELECT_CLONING_OPTION == 'BRANCH') {
                echo "Cloning repository using branch: ${params.BRANCH}"
                // Use the SSH key stored with ID equal to args.appName
                withCredentials([sshUserPrivateKey(credentialsId: args.appName, keyFileVariable: 'SSH_KEY')]) {
                    sh "GIT_SSH_COMMAND='ssh -i ${SSH_KEY}' git clone -b ${params.BRANCH} ${GIT_REPO_URL} ."
                }
            } else if (params.SELECT_CLONING_OPTION == 'TAG') {
                echo "Cloning repository and checking out tag: ${params.TAG}"
                withCredentials([sshUserPrivateKey(credentialsId: args.appName, keyFileVariable: 'SSH_KEY')]) {
                    sh "GIT_SSH_COMMAND='ssh -i ${SSH_KEY}' git clone ${GIT_REPO_URL} ."
                    sh "git fetch --tags"
                    sh "git checkout -b temp-branch ${params.TAG}"
                }
            } else {
                error "Invalid SELECT_CLONING_OPTION value: ${params.SELECT_CLONING_OPTION}. Must be either 'BRANCH' or 'TAG'."
            }
        }

        // Return the release name for reference in subsequent stages
        return RELEASE_NAME
    }
}
