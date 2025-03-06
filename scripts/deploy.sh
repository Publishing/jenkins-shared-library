#!/bin/bash

# Input Parameters
RELEASE_NAME=$1
TARGET_SERVER_USER=$2
TARGET_SERVER=$3
TARGET_DIR=$4
SETTINGS_FILE=$5
DEPLOYMENT_LOGS=$6
APP_NAME=$7
TARGET_ENVIRONMENT=$8
CACHEBUST=$9

# Derived Variables
CURRENT_VERSION=$(echo "$RELEASE_NAME" | grep -oE 'V[0-9]+\.[0-9]+\.[0-9]+$')
SOURCE_WSGI_PATH="/var/lib/jenkins/deployments/scripts/wsgi.py"

# Create a modified version of the wsgi.py content
MODIFIED_CONTENT=$(cat "${SOURCE_WSGI_PATH}" | sed "s/{ENVIRONMENT}/${TARGET_ENVIRONMENT}/" | sed "s|{SETTINGS_FILE}|${SETTINGS_FILE}|" | sed "s|{PYCACHE_PREFIX}|/srv/wsgiapps/pycache/|")

# Execute deployment commands and transfer wsgi.py contents directly
ssh "${TARGET_SERVER_USER}@${TARGET_SERVER}" <<EOF

    # Define variables
    CURRENT_VERSION="${CURRENT_VERSION}"
    TARGET_DIR="${TARGET_DIR}"
    DEPLOYMENT_LOGS="${DEPLOYMENT_LOGS}"
    RELEASE_NAME="${RELEASE_NAME}"
    APP_NAME="${APP_NAME}"
    SETTINGS_FILE="${SETTINGS_FILE}"
    CACHEBUST="${CACHEBUST}"

    # Clear the log file at the start of each deployment
    > "\${DEPLOYMENT_LOGS}/${APP_NAME}"

    # Define logging function
    log_info() { echo "INFO - \$1" >> "\${DEPLOYMENT_LOGS}/${APP_NAME}"; }
    log_error() { echo "ERROR - \$1" >> "\${DEPLOYMENT_LOGS}/${APP_NAME}"; exit 1; }

    # Define version comparison function
    compare_versions() {
        local version1=\$1
        local version2=\$2
        local IFS=.
        local i v1=(\$version1) v2=(\$version2)
        for ((i=0; i<\${#v1[@]}; i++)); do
            if [[ \${v1[i]} -lt \${v2[i]} ]]; then return 1; elif [[ \${v1[i]} -gt \${v2[i]} ]]; then return 2; fi
        done
        return 0
    }

    log_info "*--------------- STARTING DEPLOYMENT FOR \${APP_NAME} ---------------*"

    # Navigate to the target directory
    log_info "Navigating to target directory"
    cd "\${TARGET_DIR}" || log_error "Failed to change directory to \${TARGET_DIR}"

    # Extract the release tarball
    log_info "Extracting the release tarball"
    tar -xf "\${TARGET_DIR}/${RELEASE_NAME}.tar" || log_error "Failed to extract \${RELEASE_NAME}.tar"
    rm -rf "\${TARGET_DIR}/${RELEASE_NAME}.tar" || log_error "Failed to delete \${RELEASE_NAME}.tar"

    # Clean up old releases
    for dir in \${APP_NAME}_release.*; do
        DIR_VERSION=\$(echo "\$dir" | grep -oE 'V[0-9]+\.[0-9]+\.[0-9]+$')
        if [[ "\$dir" != "\${RELEASE_NAME}" ]] && [[ "\$DIR_VERSION" =~ ^V[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
            compare_versions "\${DIR_VERSION#V}" "\${CURRENT_VERSION#V}"
            if [[ \$? -eq 1 ]]; then
                log_info "Deleting old release folder: \${dir}"
                rm -rf "\${dir}"
            else
                log_info "Keeping release folder: \${dir}"
            fi
        fi
    done

    # Update symlink
    log_info "Updating symlink to current release"
    ln -sfn "\${TARGET_DIR}/${RELEASE_NAME}" current || log_error "Failed to update symlink"

    # Write the modified wsgi.py content to the target file
    log_info "Writing modified wsgi.py content to the target file"
    cat > "\${TARGET_DIR}/${RELEASE_NAME}/conf/wsgi.py" <<WSGI
${MODIFIED_CONTENT}
WSGI

    # If CACHEBUST is true, append CACHEBUSTER_STRING to settings.py
    if [[ "\${CACHEBUST}" == "true" ]]; then
        log_info "Appending CACHEBUSTER_STRING to settings.py"
        CB_VALUE=\$(python3 -c "import datetime, random; print('CACHEBUSTER_STRING = \\'' + datetime.datetime.today().strftime('%Y%m%d') + 'v' + str(random.randint(1, 99999)) + '\\'')")
        echo "\${CB_VALUE}" >> "\${TARGET_DIR}/${RELEASE_NAME}/conf/settings.py" || log_error "Failed to append CACHEBUSTER_STRING to settings.py"
    else
        log_info "CACHEBUST is false; skipping CACHEBUSTER_STRING appending"
    fi

    # Run collectstatic command
    log_info "Running collectstatic command"
    mkdir -p /srv/wsgiapps/web/static/\${APP_NAME}
    cd "\${TARGET_DIR}/${RELEASE_NAME}/" || log_error "Failed to change directory to \${TARGET_DIR}/${RELEASE_NAME}/"
    python -m pipenv run collectstatic --settings "\${SETTINGS_FILE}" || log_error "Failed to run collectstatic"

    # Restart the application
    log_info "Restarting application"
    touch "\${TARGET_DIR}/${RELEASE_NAME}/conf/wsgi.py" || log_error "Failed to touch wsgi.py for restart"

    log_info "*--------------- TESTING DEPLOYMENT COMPLETED FOR \${APP_NAME} ---------------*"

EOF
