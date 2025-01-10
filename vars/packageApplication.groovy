def call() {
    stage('Package Application') {
        when {
            expression { return params.SELECT_WORK_FLOW == 'CI-CD' }
        }
        steps {
            script {
                sh "tar -cf ${env.RELEASE_NAME}.tar ${env.RELEASE_NAME}"
                sh "cp -p ${env.RELEASE_NAME}.tar ${env.BACKUP_DIR}"
            }
        }
    }
}
