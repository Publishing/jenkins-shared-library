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
    > "\${DEPLOYMENT_LOGS}/\${APP_NAME}"

    # Define logging functions
    log_info() { echo "INFO - \$1" >> "\${DEPLOYMENT_LOGS}/\${APP_NAME}"; }
    log_error() { echo "ERROR - \$1" >> "\${DEPLOYMENT_LOGS}/\${APP_NAME}"; exit 1; }

    # Define version comparison function
    compare_versions() {
        local version1=\$1
        local version2=\$2
        local IFS=.
        local i v1=(\$version1) v2=(\$version2)
        for ((i=0; i<\${#v1[@]}; i++)); do
            if [[ \${v1[i]} -lt \${v2[i]} ]]; then return 1
            elif [[ \${v1[i]} -gt \${v2[i]} ]]; then return 2
            fi
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

    # Save current deployment symlink target (if exists)
    PREV_DEPLOYMENT=\$(readlink current 2>/dev/null || echo "")

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
    if python -m pipenv run ollectstatic --settings "\${SETTINGS_FILE}"; then
         log_info "collectstatic command succeeded."
         COLLECTSTATIC_SUCCESS=true
    else
         log_info "collectstatic command failed."
         COLLECTSTATIC_SUCCESS=false
    fi

    if [ "\${COLLECTSTATIC_SUCCESS}" = "true" ]; then
    
         # Change back to TARGET_DIR before cleaning up
         cd "${TARGET_DIR}" || log_error "Failed to change directory to ${TARGET_DIR}"
         
         # Verify current directory
         log_info "Current directory: \$(pwd)"

         # Print directories to be deleted (excluding @tmp)
         log_info "Directories to be deleted:"
         ls -dt "${TARGET_DIR}"/api_release.* | grep -v '@tmp' | tail -n +3 | while IFS= read -r old_release; do
             echo "Removing: \"\$old_release\""
         done

         # Check permissions and timestamps (excluding @tmp)
         log_info "Checking permissions and timestamps for directories:"
         for dir in \$(ls -dt "${TARGET_DIR}"/api_release.* | grep -v '@tmp'); do
             echo "Directory: \$dir"
             stat "\$dir"
         done

         # Remove older releases (excluding @tmp)
         ls -dt "${TARGET_DIR}"/api_release.* | grep -v '@tmp' | tail -n +3 | while IFS= read -r old_release; do
             echo "Removing: \"\$old_release\""
             rm -rf -- "\$old_release"
         done

         # Log the kept directories
         log_info "Kept the latest two release folders."

         # Change back to TARGET_DIR so the symlink is created in the correct location
         cd "${TARGET_DIR}" || log_error "Failed to change directory to ${TARGET_DIR}"
            
         # Update symlink to new release
         log_info "Updating symlink to current release"
         ln -sfn "\${TARGET_DIR}/${RELEASE_NAME}" current || log_error "Failed to update symlink"

         # Restart the application
         log_info "Restarting application"
         touch "\${TARGET_DIR}/${RELEASE_NAME}/conf/wsgi.py" || log_error "Failed to touch wsgi.py for restart"

         log_info "*--------------- DEPLOYMENT COMPLETED FOR \${APP_NAME} ---------------*"
    else
         log_error "collectstatic command failed. Reverting to previous deployment."
         if [[ -n "\${PREV_DEPLOYMENT}" ]]; then
              ln -sfn "\${PREV_DEPLOYMENT}" current || log_error "Failed to revert symlink"
              touch "\${PREV_DEPLOYMENT}/conf/wsgi.py" || log_error "Failed to touch wsgi.py for restart"
              log_info "Reverted to previous deployment: \${PREV_DEPLOYMENT}"
         fi
         exit 1
    fi

EOF
