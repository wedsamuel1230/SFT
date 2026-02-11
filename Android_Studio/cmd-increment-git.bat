@echo off &setlocal

TITLE Git Auto Pusher

cls
git checkout "auto commit" 2>nul || git checkout -b "auto commit"
:DoGit

git add .
git commit -m "Auto Commit by Git Auto Pusher at %time%"
git push
Timeout /t 600 >nul

GOTO DoGit
