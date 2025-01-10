def call(Map config) {
    stage('Clone Repository') {
        steps {
            script {
                // Ensure RELEASE_DATE is defined if not already set
                if (!env.RELEASE_DATE) {
                    env.RELEASE_DATE = sh(script: 'date +%d%m%y%H%M', returnStdout: true).trim()
                }

                // Define RELEASE_NAME if not already set
                if (!env.RELEASE_NAME) {
                    env.RELEASE_NAME = "${config.appName}_release.${env.RELEASE_DATE}"
                    echo "Setting RELEASE_NAME to: ${env.RELEASE_NAME}"
                }

                // Create the release directory
                sh "mkdir -p ${env.RELEASE_NAME}"

                // Navigate to the release directory
                dir(env.RELEASE_NAME) {
                    if (params.SELECT_CLONING_OPTION == 'BRANCH') {
                        echo "Cloning repository using branch: ${params.BRANCH}"
                        git branch: params.BRANCH, credentialsId: "${env.GIT_CREDENTIALS_ID}", url: "${env.GIT_REPO_URL}"
                    } else if (params.SELECT_CLONING_OPTION == 'TAG') {
                        echo "Cloning repository and checking out tag: ${params.TAG}"
                        git credentialsId: "${env.GIT_CREDENTIALS_ID}", url: "${env.GIT_REPO_URL}"
                        // Checkout the specific tag after cloning
                        sh "git checkout tags/${params.TAG}"
                    } else {
                        error "Invalid SELECT_CLONING_OPTION value: ${params.SELECT_CLONING_OPTION}. Must be either 'BRANCH' or 'TAG'."
                    }
                }
            }
        }
    }
}
