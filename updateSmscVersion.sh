#!/bin/bash

# Variables
BITBUCKET_ORG="paicdb"
REPOS=("db-insert-data" "ss7-client-module" "smsc-routing-module" "smsc-retries-module" "smsc-orchestrator" "smpp-server-module" "smpp-client-module" "http-server-module" "http-client-module") # Add the necessary repositories
NEW_VERSION=$1
BRANCH="UPDATE-UTILS-$NEW_VERSION"
BITBUCKET_USER=$2
BITBUCKET_APP_PASSWORD=$3
REVIEWERS=("Nicaragua23" "ecalero97") # Add the usernames of the reviewers

# Check if the version parameter was passed
if [ -z "$NEW_VERSION" ]; then
    echo "Error: You must provide the version as a parameter to the script."
    exit 1
fi

# Function to update the pom.xml file and create a PR
update_pom_version() {
    local repo=$1

    echo "Getting the last commit of the opensource branch..."
    LAST_COMMIT=$(curl -s -u "$BITBUCKET_USER:$BITBUCKET_APP_PASSWORD" \
      "https://api.bitbucket.org/2.0/repositories/$BITBUCKET_ORG/$repo/refs/branches/opensource" | jq -r '.target.hash')
    if [ -z "$LAST_COMMIT" ]; then
        echo "Error: Could not get the last commit of the opensource branch in the repository $repo"
        return 1
    fi
    echo "Last commit: $LAST_COMMIT"

    echo "Creating a new branch $BRANCH..."
    curl -X POST -u "$BITBUCKET_USER:$BITBUCKET_APP_PASSWORD" \
      -H "Content-Type: application/json" \
      -d '{
            "name": "'"$BRANCH"'",
            "target": {
              "hash": "'"$LAST_COMMIT"'"
            }
          }' \
      "https://api.bitbucket.org/2.0/repositories/$BITBUCKET_ORG/$repo/refs/branches"
    if [ $? -ne 0 ]; then
        echo "Error: Could not create the new branch $BRANCH in the repository $repo"
        return 1
    fi
    echo "Branch $BRANCH created successfully."

    echo "Downloading the pom.xml file..."
    curl -u "$BITBUCKET_USER:$BITBUCKET_APP_PASSWORD" \
      -O "https://api.bitbucket.org/2.0/repositories/$BITBUCKET_ORG/$repo/src/$BRANCH/pom.xml"
    if [ $? -ne 0 ]; then
        echo "Error: Could not download the pom.xml file in the repository $repo"
        return 1
    fi
    echo "pom.xml file downloaded successfully."

    echo "Updating the pom.xml file..."
    sed -i "s/<smsc-utils.version>.*<\/smsc-utils.version>/<smsc-utils.version>$NEW_VERSION<\/smsc-utils.version>/" pom.xml
    if [ $? -ne 0 ]; then
        echo "Error: Could not update the pom.xml file in the repository $repo"
        return 1
    fi
    echo "pom.xml file updated successfully."

    echo "Uploading the updated pom.xml file to Bitbucket... $BRANCH"
    curl -X POST -u "$BITBUCKET_USER:$BITBUCKET_APP_PASSWORD" \
         -H "Content-Type: multipart/form-data" \
         -F pom.xml=@pom.xml \
         -F branch=$BRANCH \
         "https://api.bitbucket.org/2.0/repositories/$BITBUCKET_ORG/$repo/src"
    if [ $? -ne 0 ]; then
        echo "Error: Could not upload the pom.xml file to the repository $repo"
        return 1
    fi
    echo "pom.xml file uploaded successfully."

    echo "Deleting the local pom.xml file..."
    rm -f pom.xml
    if [ $? -ne 0 ]; then
        echo "Error: Could not delete the local pom.xml file in the repository $repo"
        return 1
    fi
    echo "Local pom.xml file deleted successfully."

    # Prepare reviewers array for the JSON payload
    reviewers_json=$(printf ",{\"username\":\"%s\"}" "${REVIEWERS[@]}")
    reviewers_json=${reviewers_json:1}

    echo "Creating a pull request..."
    curl -X POST -u "$BITBUCKET_USER:$BITBUCKET_APP_PASSWORD" \
          -H "Content-Type: application/json" \
          -d '{
                "title": "Update smsc-utils version to '"$NEW_VERSION"'",
                "source": {
                  "branch": {
                    "name": "'"$BRANCH"'"
                  }
                },
                "destination": {
                  "branch": {
                    "name": "opensource"
                  }
                },
                "close_source_branch": true,
                "reviewers": ['"$reviewers_json"']
              }' \
          "https://api.bitbucket.org/2.0/repositories/$BITBUCKET_ORG/$repo/pullrequests"
    if [ $? -ne 0 ]; then
        echo "Error: Could not create a pull request for the repository $repo"
        return 1
    fi
    echo "Pull request created successfully."
}

# Update each repository
for repo in "${REPOS[@]}"; do
    update_pom_version $repo
done

echo "The update process has finished."
