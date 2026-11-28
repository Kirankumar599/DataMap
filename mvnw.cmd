@echo off
call "%~dp0backend\mvnw.cmd" -f "%~dp0backend\pom.xml" %*
exit /b %ERRORLEVEL%
