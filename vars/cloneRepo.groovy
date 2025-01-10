def call(Map config) {
    stage('Clone Repository') {
            steps {
                script {
                    // Create the release directory
                    sh "mkdir -p ${env.RELEASE_NAME}"
                    
                    // Navigate to the release directory
                    dir(env.RELEASE_NAME) {
                        if (params.SELECT_CLONING_OPTION == 'BRANCH') {
                            // Cloning the branch
                            echo "Cloning repository using branch: ${params.BRANCH}"
                            git branch: params.BRANCH, credentialsId: "${env.GIT_CREDENTIALS_ID}", url: "${env.GIT_REPO_URL}"
                            
                        } else if (params.SELECT_CLONING_OPTION == 'TAG') {
                            // Cloning the repository and then checking out the tag
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
