def call(String appName) {
    sh "tar -cf ${appName}.tar ."
}

