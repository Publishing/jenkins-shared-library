def call(Map config) {
    stage('Deploy to Server') {
        steps {
            script {
                if (params.SELECT_TARGET_OPTION == 'ENVIRONMENT') {
                    def targetServers = []
                    if (params.TARGET_ENVIRONMENT == 'test') {
                        targetServers = ['djangopytest.rtegroup.ie']
                    } else if (params.TARGET_ENVIRONMENT == 'uat') {
                        targetServers = ['uat.rtegroup.ie']
                    } else if (params.TARGET_ENVIRONMENT == 'beta') {
                        targetServers = ['djangopybeta.rtegroup.ie']
                    } else if (params.TARGET_ENVIRONMENT == 'development') {
                        targetServers = ['djangopydev.rtegroup.ie']
                    } else if (params.TARGET_ENVIRONMENT == 'next') {
                        targetServers = ['djangopynext.rtegroup.ie']
                    } else if (params.TARGET_ENVIRONMENT == 'production') {
                        targetServers = [] // Add production servers here if needed
                    }

                    def parallelStages = targetServers.collectEntries { target ->
                        ["Deploy to ${target}": {
                            script {
                                deployToTarget(target)
                            }
                        }]
                    }
                    parallel parallelStages
                } else {
                    // Single deployment for a specific server
                    deployToTarget(params.TARGET_SERVER)
                }
            }
        }
    }
}

def deployToTarget(target) {
    sh "scp ${env.RELEASE_NAME}.tar ${env.TARGET_SERVER_USER}@${target}:${env.TARGET_DIR}"
    sh "sh ${env.DEPLOY_SCRIPT_PATH} ${env.RELEASE_NAME} ${env.TARGET_SERVER_USER} ${target} ${env.TARGET_DIR} ${params.SETTINGS_FILE} ${env.DEPLOYMENT_LOGS} ${env.APP_NAME} ${params.TARGET_ENVIRONMENT}"
    sh "sh ${env.DB_SCRIPT_PATH} ${params.BRANCH} ${params.DEPLOYER} ${currentBuild.currentResult} ${target} ${params.SETTINGS_FILE} ${env.APP_NAME} ${env.BUILD_URL} ${env.APPROVER} ${env.BUILD_NUMBER}"
    sh "python ${HTML_REPORT_PATH}"
    sh "scp ${env.TARGET_SERVER_USER}@${target}:${env.DEPLOYMENT_LOGS}/${env.APP_NAME} ${env.APP_NAME}_target_log"
    sh "cat ${env.APP_NAME}_target_log"
    sh "rm -rf ${env.APP_NAME}_target_log"
}

