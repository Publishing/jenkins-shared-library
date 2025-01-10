def call(Map args) {
    stage('Deploy to Server') {
        
            script {
                // Define variables from args
                def releaseName = args.releaseName
                def targetServerUser = args.targetServerUser
                def targetDir = args.targetDir
                def deployScriptPath = args.deployScriptPath
                def dbScriptPath = args.dbScriptPath
                def htmlReportPath = args.htmlReportPath
                def deploymentLogs = args.deploymentLogs
                def appName = args.appName

                // Validate required variables
                if (!releaseName || !targetServerUser || !targetDir || !deployScriptPath || !dbScriptPath || !htmlReportPath || !deploymentLogs || !appName) {
                    error "Missing required variables for deployment. Ensure all required arguments are provided in the configuration."
                }

                if (params.SELECT_TARGET_OPTION == 'ENVIRONMENT') {
                    // Determine target servers based on environment
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

                    // Deploy to target servers in parallel
                    def parallelStages = targetServers.collectEntries { target ->
                        ["Deploy to ${target}": {
                            script {
                                deployToTarget(target, releaseName, targetServerUser, targetDir, deployScriptPath, dbScriptPath, htmlReportPath, deploymentLogs, appName)
                            }
                        }]
                    }
                    parallel parallelStages
                } else {
                    // Single deployment for a specific server
                    deployToTarget(params.TARGET_SERVER, releaseName, targetServerUser, targetDir, deployScriptPath, dbScriptPath, htmlReportPath, deploymentLogs, appName)
                }
            
        }
    }
}
def deployToTarget(target, releaseName, targetServerUser, targetDir, deployScriptPath, dbScriptPath, htmlReportPath, deploymentLogs, appName) {
    sh "scp ${releaseName}.tar ${targetServerUser}@${target}:${targetDir}"
    sh "sh ${deployScriptPath} ${releaseName} ${targetServerUser} ${target} ${targetDir} ${params.SETTINGS_FILE} ${deploymentLogs} ${appName} ${params.TARGET_ENVIRONMENT}"
    sh "sh ${dbScriptPath} ${params.BRANCH} ${params.DEPLOYER} ${currentBuild.currentResult} ${target} ${params.SETTINGS_FILE} ${appName} ${env.BUILD_URL} ${env.APPROVER} ${env.BUILD_NUMBER}"
    sh "python ${htmlReportPath}"
    sh "scp ${targetServerUser}@${target}:${deploymentLogs}/${appName} ${appName}_target_log"
    sh "cat ${appName}_target_log"
    sh "rm -rf ${appName}_target_log"
}
