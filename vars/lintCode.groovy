def call() {
    stage('Lint Code') {
        steps {
            script {
                dir(env.RELEASE_NAME) {
                    sh "pipenv run pip install ruff > ${TMP_DIR}/lint_install.log"
                    env.LINT_OUT = sh(script: "pipenv run ruff check .", returnStdout: true).trim()
                    if (!env.LINT_OUT.contains("All checks passed!")) {
                        echo "Issue: ${env.LINT_OUT}"
                        currentBuild.result = 'FAILURE'
                        error("Stopping pipeline due to lint issues.")
                    } else {
                        echo "${env.LINT_OUT}"
                    }
                }
            }
        }
    }
}
