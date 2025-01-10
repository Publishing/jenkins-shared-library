def call(Map args) {
    stage('Package Application') {
        when {
            expression { return params.SELECT_WORK_FLOW == 'CI-CD' } // Access pipeline parameter directly
        }
        steps {
            script {
                // Define variables from args
                def releaseName = args.releaseName
                def backupDir = args.backupDir

                // Validate required parameters
                if (!releaseName || !backupDir) {
                    error "releaseName and backupDir are required but not provided in the configuration."
                }

                // Create tarball and copy to backup directory
                echo "Packaging application..."
                sh "tar -cf ${releaseName}.tar ${releaseName}"
                sh "cp -p ${releaseName}.tar ${backupDir}"

                echo "Application packaged successfully and stored in ${backupDir}"
            }
        }
    }
}
