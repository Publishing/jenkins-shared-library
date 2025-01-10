def call(Map config) {
    stage('Lint Code') {
        steps {
            script {
                // Validate required parameters
                if (!config.releaseName) {
                    error "releaseName is required but not provided in the configuration."
                }

                // Ensure TMP_DIR is passed or fallback to default
                def tmpDir = config.tmpDir ?: '/tmp'

                // Navigate to the release directory
                dir(config.releaseName) {
                    echo "Installing and running ruff linter in ${config.releaseName}..."
                    
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
    }
}
