def call(Map config) {
    if (config.cloningOption == 'BRANCH') {
        git branch: config.branch, url: config.repoUrl, credentialsId: config.credentialsId
    } else if (config.cloningOption == 'TAG') {
        git url: config.repoUrl, credentialsId: config.credentialsId
        sh "git checkout tags/${config.tag}"
    } else {
        error "Invalid cloningOption: ${config.cloningOption}. Must be 'BRANCH' or 'TAG'."
    }
}
