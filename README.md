# Jenkins Shared Library

## Overview
This repository serves as a centralized **Jenkins Shared Library**, designed to streamline CI/CD pipeline development by providing reusable and modular functions. By encapsulating common scripts and configurations, it helps enhance consistency, reduce redundancy, and simplify pipeline maintenance across multiple projects.

## Features
- **Reusable Functions**: Encapsulate frequently used steps (e.g., linting, testing, deployment) into shared methods.
- **Pipeline Simplification**: Minimize repetitive code in Jenkins pipelines.
- **Consistency**: Standardize CI/CD workflows across all projects.
- **Ease of Maintenance**: Centralized updates to shared logic.

## How to Use

### Configure the Library in Jenkins
1. Go to **Manage Jenkins > Configure System > Global Pipeline Libraries**.
2. Add a new library:
   - **Name**: `shared-library` (or any name you prefer)
   - **Default Version**: Branch or tag to use.
   - **Repository URL**: Link to this repository.
   - **Credentials**: Set if private access is required.

### Include in Pipelines
Reference the library in your pipeline:
```groovy
@Library('shared-library') _

pipeline {
    agent any
    stages {
        stage('Example') {
            steps {
                script {
                    exampleFunction()
                }
            }
        }
    }
}
```
## Example Functions

### `exampleFunction.groovy`
A sample function for reusability:
```groovy
def call() {
    echo "This is an example shared library function!"
}
```
This function can be used in the pipeline as follows:
```groovy
@Library('shared-library') _

pipeline {
    agent any
    stages {
        stage('Example') {
            steps {
                script {
                    exampleFunction()
                }
            }
        }
    }
}
```

### `printBuildNumber.groovy`
A function to print the build number:
```groovy
def call(String buildNumber) {
    echo "The current build number is ${buildNumber}"
}
```
This function can be used in the pipeline as follows:

```groovy
@Library('shared-library') _

pipeline {
    agent any
    stages {
        stage('Print Build Number') {
            steps {
                script {
                    printBuildNumber("${env.BUILD_NUMBER}")
                }
            }
        }
    }
}

```



### Explanation:

1. **Separation of Functions**:
   - `exampleFunction` and `printBuildNumber` are distinct functions and should be documented separately.

2. **Contextual Clarity**:
   - For each function, explain how it can be integrated into the pipeline.

3. **Logical Progression**:
   - First, define the function.
   - Next, provide the pipeline snippet for its usage.

This structure makes the documentation cohesive, easy to read, and ensures everything adds up logically. Let me know if you'd like further adjustments!
