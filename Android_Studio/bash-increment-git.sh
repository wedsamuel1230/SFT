#!/bin/sh

clear
echo "Welcome to git-auto-pusher! This was created by Srujan Deshpande"
echo "Enter the duration in seconds you want to wait before commits"
read duration

git config --global credential.helper 'cache --timeout 10800'

while true; do
  sleep $duration
  git add .
  git commit -m "Auto Commit"
  git push
done
