def call() {
    parameters {
        choice(
            name: 'SELECT_CLONING_OPTION',
            choices: ['BRANCH', 'TAG'],
            description: 'Select the option you want to clone from.'
        )
        gitParameter(
            branchFilter: 'origin/(.*)',
            defaultValue: 'master',
            name: 'BRANCH',
            quickFilterEnabled: true,
            type: 'PT_BRANCH'
        )
        gitParameter(
            branchFilter: '.*',   // This filter can be used to control tag names
            defaultValue: 'v1.0.0',  // You can specify any default tag here
            name: 'TAG',  // The name for the tag parameter
            type: 'PT_TAG',  // This specifies that the parameter is for Git tags
            selectedValue: 'NONE',  // Select none by default (or you can use 'DEFAULT' to select the default tag)
            quickFilterEnabled: true,
            sortMode: 'ASCENDING'  // Optional, you can specify sorting mode as needed
        )
        choice(
            name: 'SELECT_WORK_FLOW',
            choices: ['CI', 'CI-CD'],
            description: 'Select the workflow: CI for build & test, CI-CD for build, test & deployment.'
        )
        booleanParam(
            name: 'VERBOSE_LOGS',
            defaultValue: false,
            description: 'Select if you want detailed logs while installing dependencies.'
        )
        string(
            name: 'SETTINGS_FILE',
            defaultValue: 'conf.production_settings',
            description: 'Type the settings file for deployment (e.g.,  conf.djangodev_settings  |  conf.production_settings  |  conf.djangobeta_settings  |  conf.djangonext_settings  )'
        )
        choice(
            name: 'SELECT_TARGET_OPTION',
            choices: ['SERVER', 'ENVIRONMENT'],
            description: 'Select the option you want to clone from.'
        )
        choice(
            name: 'TARGET_SERVER',
            choices: ['djangopytest.rtegroup.ie', 'uat.rtegroup.ie', 'djangopydev.rtegroup.ie', 'djangopynext.rtegroup.ie', 'djangopybeta.rtegroup.ie'],
            description: '*DEPLOYMENT SPECIFIC > Select the target server for deployment'
        )
        choice(
            name: 'TARGET_ENVIRONMENT',
            choices: ['beta', 'uat', 'next', 'test', 'development', 'production'],
            description: '*DEPLOYMENT SPECIFIC > Select the target server for deployment'
        )
        string(
            name: 'DEPLOYER',
            defaultValue: 'abhishek.tiwari@rte.ie',
            description: 'Enter the user details'
        )
    }
}
