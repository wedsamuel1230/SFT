@echo off &setlocal

TITLE Git Auto Pusher

cls
:DoGit
Set allm=

set "Allm=%Hour%:%Min%:%Sec%"

git add .
git commit -m "%allm%"
git push
Timeout /t 1 >nul
)

GOTO DoGit
