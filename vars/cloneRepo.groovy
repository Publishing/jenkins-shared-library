def call(Map config) {
    stage('Clone Repository') {
        steps {
            script {
                sh "mkdir -p ${env.RELEASE_NAME}"
                dir(env.RELEASE_NAME) {
                    if (config.cloningOption == 'BRANCH') {
                        echo "Cloning repository using branch: ${config.branch}"
                        git branch: config.branch, credentialsId: config.credentialsId, url: config.repoUrl
                    } else if (config.cloningOption == 'TAG') {
                        echo "Cloning repository and checking out tag: ${config.tag}"
                        git credentialsId: config.credentialsId, url: config.repoUrl
                        sh "git checkout tags/${config.tag}"
                    } else {
                        error "Invalid cloningOption: ${config.cloningOption}. Must be 'BRANCH' or 'TAG'."
                    }
                }
            }
        }
    }
}
