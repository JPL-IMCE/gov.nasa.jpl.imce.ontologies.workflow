#!/bin/bash

#openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in secring.gpg.enc -out local.secring.gpg -d
#openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in pubring.gpg.enc -out local.pubring.gpg -d
#openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in credentials.sbt.enc -out local.credentials.sbt -d
#openssl aes-256-cbc -pass pass:$ENCRYPTION_PASSWORD -in deploy_key.enc -out local.deploy_key -d

#chmod 600 local.*
#eval `ssh-agent -s`
#ssh-add local.deploy_key
#git config --global push.default simple
#git config --global user.email "nobody@nobody.org"
#git config --global user.name "Jenkins CI"

#sbt packageProfiles
#sbt publishSigned

# Get the tag for this commit
t=$(git name-rev --tags --name-only $(git rev-parse HEAD))

# Bypass the build if the tag is anything but 'undefined'.
[ "undefined" != "$t" ] && exit 0;

echo "Deploying artifacts..."

sbt publishSigned

