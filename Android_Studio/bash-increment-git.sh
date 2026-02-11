#!/bin/sh

clear
echo "Welcome to git-auto-pusher! This was created by Srujan Deshpande"
duration=600

git config --global credential.helper 'cache --timeout 10800'
git checkout "auto-commit" || git checkout -b "auto-commit"

while true; do
  sleep $duration
  git add .
  git commit -m "Auto Commit by Git Auto Pusher at $(date +%T)"
  git push
done
