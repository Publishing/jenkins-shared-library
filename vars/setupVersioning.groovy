def call(Map args) {

        
            script {
                    
                def versionFile = args.versionFile
                def appName = args.appName
                def branchOrTag = params.SELECT_CLONING_OPTION == 'BRANCH' ? params.BRANCH : params.TAG
                env.RELEASE_DATE = sh(script: 'date +%d%m%y%H%M', returnStdout: true).trim()
                def currentVersion = sh(script: "if [ -f ${versionFile} ]; then cat ${versionFile}; else echo 'V1.0.0'; fi", returnStdout: true).trim()
                def (major, minor, patch) = currentVersion.replaceAll("V", "").tokenize('.')
                def newPatch = patch.toInteger() + 1
                def deploymentWorkflowTriggerUrl = "https://prod-03.westeurope.logic.azure.com:443/workflows/00bd6ccdd7294f05b7976ce4ab486184/triggers/manual/paths/invoke?api-version=2016-06-01&sp=%2Ftriggers%2Fmanual%2Frun&sv=1.0&sig=ZIlo779OiLoT4AYlsf9D04qxVmXfAyLU_mCgG3FEudg"

                if (params.SELECT_WORK_FLOW in ['CI-CD', 'UD']) {
                    if (
                        (params.SELECT_TARGET_OPTION == 'SERVER' && params.TARGET_SERVER == 'djangopytest.rtegroup.ie') || 
                        (params.SELECT_TARGET_OPTION == 'ENVIRONMENT' && params.TARGET_ENVIRONMENT == 'test')
                    ) {
                        echo "Triggering external deployment workflow for environment: ${params.TARGET_ENVIRONMENT}"
                
                        // Trigger the external workflow (Deployment start)
                        sh """
                        curl -X POST -H "Content-Type: application/json" -d '{
                            "Environment": "${params.TARGET_ENVIRONMENT}",
                            "Application": "${appName}",
                            "BranchOrTag": "${branchOrTag}",
                            "Deployer": "${params.DEPLOYER}",
                            "JenkinsLogs": "${env.BUILD_URL}"
                        }' "${deploymentWorkflowTriggerUrl}" || true
                        """
                    }
                }
                    
                env.NEW_VERSION = "V${major}.${minor}.${newPatch}"
                sh "echo ${env.NEW_VERSION} > ${versionFile}"
                env.RELEASE_NAME = "${appName}_release.${env.RELEASE_DATE}.${env.NEW_VERSION}"
                echo "New release version: ${env.NEW_VERSION}"
                echo "Release name: ${env.RELEASE_NAME}"
            }
        }

