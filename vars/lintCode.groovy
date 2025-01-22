def call(Map args) {
       
       if (params.SELECT_WORK_FLOW == 'UD') {
        echo "Skipping Lint Code for workflow UD."
        return
            }
            script {
                // Define variables from args
                def releaseName = args.releaseName
                def tmpDir = args.tmpDir ?: '/tmp' // Use fallback if tmpDir is not provided

                // Validate required parameters
                if (!releaseName) {
                    error "releaseName is required but not provided in the configuration."
                }

                // Navigate to the release directory
                dir(releaseName) {
                       sh "pwd"
                       sh "ls -ltr"
                    echo "Installing and running ruff linter in ${releaseName}..."
                    
                    // Install ruff linter
                    sh "pipenv run pip install ruff > ${tmpDir}/lint_install.log"

                    // Run ruff linter and capture the output
                    def lintOutput = sh(script: "pipenv run ruff check .", returnStdout: true).trim()

                    // Check the linter output
                    if (!lintOutput.contains("All checks passed!")) {
                        echo "Linting issues detected:\n${lintOutput}"
                        currentBuild.result = 'FAILURE'
                        error("Stopping pipeline due to lint issues.")
                    } else {
                        echo "Linting passed:\n${lintOutput}"
                    }
                
            }
        }
    }

