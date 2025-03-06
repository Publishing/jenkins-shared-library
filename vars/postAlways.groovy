def call() {
    try {
        //cleanWs() // Cleanup workspace
        echo "Workspace cleanup completed."
    } catch (Exception e) {
        echo "Failed during postAlways: ${e.getMessage()}"
        throw e
    }
}
