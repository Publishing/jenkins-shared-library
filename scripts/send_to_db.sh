#!/bin/bash

# Usage: send_to_db.sh branch deployer status target_server settings_file app_name redeploy_url approver

# Validate the number of arguments
#if [ "$#" -ne 8 ]; then
#    echo "Usage: $0 branch deployer status target_server settings_file app_name redeploy_url approver"
#    exit 1
#fi

# Assigning parameters to variables
BRANCH=$1             # Branch name
DEPLOYER=$2           # Deployer name
STATUS=$3             # Deployment status
TARGET_SERVER=$4      # Target server
SETTINGS_FILE=$5      # Settings file
APP_NAME=$6           # Application name
REDEPLOY=$7           # Redeploy URL
APPROVER=$8           # Approver name
BUILD_ID=$9

# Database connection details
DB_HOST="dig-buildertest.rtegroup.ie"
DB_PORT="3306"
DB_NAME="djbuilder"
DB_USER="djbuilder"
DB_PASSWORD='vn&{gBFI>2v@GD#<@bkidicQPc`;0{k!Y]rFA`]<z~G5T(b~91cl,<Acs|M>cRs97,'

# Insert data into the database
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" <<EOF
INSERT INTO jenkins_deployments (application, branch, deployer, status, target_server, settings_file, redeploy, approver, build_id)
VALUES ('$APP_NAME', '$BRANCH', '$DEPLOYER', '$STATUS', '$TARGET_SERVER', '$SETTINGS_FILE', '$REDEPLOY', '$APPROVER','$BUILD_ID');
EOF

# Check for errors
if [ $? -eq 0 ]; then
    echo "Deployment information successfully recorded in the database."
else
    echo "Error: Failed to insert deployment information into the database."
fi
