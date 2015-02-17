#!/bin/bash
## Deploy the artifact in the current directory to Maven central. The deployment stages the artifact
## for release.

read -p "Are you sure you wish to deploy to Maven Central (y/N)? "

if ( [ "$REPLY" == "y" ] ) then
  ssh-add ~/.ssh/id_jorgamundus_github_rsa
  ssh-add -l
  mvn release:clean release:prepare release:perform -B -e | tee maven-central-deployment.log
  ssh-add -D
else
  echo 'Not deploying to Maven Central.'
fi