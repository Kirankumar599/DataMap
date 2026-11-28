@echo off
setlocal enabledelayedexpansion

REM Auto-configure JAVA_HOME >= 21 if available on Windows
if exist "C:\Program Files\Java\jdk-24\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-24"
    set "PATH=!JAVA_HOME!\bin;!PATH!"
) else if exist "C:\Program Files\Java\jdk-21\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Java\jdk-21"
    set "PATH=!JAVA_HOME!\bin;!PATH!"
)

REM Use system mvn if available, otherwise search IntelliJ IDEA bundled Maven
where mvn >nul 2>&1
if %ERRORLEVEL%==0 (
    mvn %*
    exit /b %ERRORLEVEL%
)

for %%P in (
    "C:\Program Files\JetBrains\IntelliJ IDEA 2025.3.3\plugins\maven\lib\maven3\bin\mvn.cmd"
    "C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2.6.2\plugins\maven\lib\maven3\bin\mvn.cmd"
) do (
    if exist %%P (
        call %%P %*
        exit /b !ERRORLEVEL!
    )
)

for /d %%D in ("C:\Program Files\JetBrains\IntelliJ*") do (
    if exist "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" (
        call "%%~D\plugins\maven\lib\maven3\bin\mvn.cmd" %*
        exit /b !ERRORLEVEL!
    )
)

echo Error: Maven (mvn) not found in PATH or JetBrains IntelliJ plugins directory. 1>&2
exit /b 1
