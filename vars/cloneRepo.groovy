def call(Map args) {
    stage('Clone Repository') {
        
            script {
                // Define variables from args or defaults
                def RELEASE_DATE = args.RELEASE_DATE ?: sh(script: 'date +%d%m%y%H%M', returnStdout: true).trim()
                def RELEASE_NAME = args.RELEASE_NAME ?: "${args.appName}_release.${RELEASE_DATE}"
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
                        // Checkout the specific tag after cloning
                        sh "git checkout tags/${params.TAG}"
                    } else {
                        error "Invalid SELECT_CLONING_OPTION value: ${params.SELECT_CLONING_OPTION}. Must be either 'BRANCH' or 'TAG'."
                    }
                }
            
        }
    }
}
