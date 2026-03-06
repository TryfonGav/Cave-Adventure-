@REM Maven Wrapper for CaveAdventure
@REM Downloads Maven automatically if not present, no global install needed
@echo off
setlocal enabledelayedexpansion

set "MAVEN_VERSION=3.9.6"
set "MAVEN_DIR=%~dp0.mvn\maven"
set "MAVEN_HOME=%MAVEN_DIR%\apache-maven-%MAVEN_VERSION%"
set "MVN_CMD=%MAVEN_HOME%\bin\mvn.cmd"

if not exist "%MVN_CMD%" (
    echo [CaveAdventure] Downloading Maven %MAVEN_VERSION%...
    set "MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip"
    set "MAVEN_ZIP=%MAVEN_DIR%\maven.zip"

    if not exist "%MAVEN_DIR%" mkdir "%MAVEN_DIR%"

    curl.exe -o "!MAVEN_ZIP!" "!MAVEN_URL!" -L --progress-bar
    if errorlevel 1 (
        echo [CaveAdventure] ERROR: Failed to download Maven. Check your internet connection.
        exit /b 1
    )

    echo [CaveAdventure] Extracting Maven...
    powershell -Command "Expand-Archive -Path '!MAVEN_ZIP!' -DestinationPath '%MAVEN_DIR%' -Force"
    if errorlevel 1 (
        echo [CaveAdventure] ERROR: Failed to extract Maven.
        exit /b 1
    )

    del "!MAVEN_ZIP!" 2>nul
    echo [CaveAdventure] Maven %MAVEN_VERSION% ready!
)

"%MVN_CMD%" %*
