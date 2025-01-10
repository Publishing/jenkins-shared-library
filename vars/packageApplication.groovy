def call(Map args) {
    script {
        // Check if the workflow is CI-CD
        if (params.SELECT_WORK_FLOW == 'CI-CD') {
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
            echo "Contents of backup directory (${backupDir}):"
            sh "ls -l ${backupDir}"

            sh "cp -p ${releaseName}.tar ${backupDir}"
            echo "Application packaged successfully and stored in ${backupDir}"
        } else {
            echo "Package Application stage skipped as workflow is not CI-CD."
        }
    }
}
