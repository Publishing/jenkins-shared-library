def call() {
    sh "pip install ruff"
    sh "ruff check ."
}

