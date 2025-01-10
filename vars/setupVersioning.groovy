def call(Map args) {
    stage('Setup Versioning') {
        steps {
            script {
                def versionFile = args.versionFile
                def appName = args.appName
                
                env.RELEASE_DATE = sh(script: 'date +%d%m%y%H%M', returnStdout: true).trim()
                def currentVersion = sh(script: "if [ -f ${versionFile} ]; then cat ${versionFile}; else echo 'V1.0.0'; fi", returnStdout: true).trim()
                def (major, minor, patch) = currentVersion.replaceAll("V", "").tokenize('.')
                def newPatch = patch.toInteger() + 1
                env.NEW_VERSION = "V${major}.${minor}.${newPatch}"
                sh "echo ${env.NEW_VERSION} > ${versionFile}"
                env.RELEASE_NAME = "${appName}_release.${env.RELEASE_DATE}.${env.NEW_VERSION}"
                echo "New release version: ${env.NEW_VERSION}"
                echo "Release name: ${env.RELEASE_NAME}"
            }
        }
    }
}
