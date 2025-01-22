def call(Map args) {

        if (params.SELECT_WORK_FLOW == 'CI') {
        echo "Skipping package approval stage for workflow UD."
        return
    }
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

