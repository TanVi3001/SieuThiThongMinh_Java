<<<<<<< HEAD
@echo off
setlocal enabledelayedexpansion

REM ==========================================================
REM Smart Supermarket - Oracle Docker Patch Runner
REM Purpose: run database patch SQL files into local Oracle Docker
REM ==========================================================

set CONTAINER_NAME=supermarket-oracle
set ORACLE_USER=system
set ORACLE_PASSWORD=Admin123
set ORACLE_SERVICE=FREEPDB1

cd /d "%~dp0.."

echo.
echo ==========================================================
echo Smart Supermarket - Database Patch Runner
echo ==========================================================
echo Container : %CONTAINER_NAME%
echo Connection: %ORACLE_USER%/%ORACLE_PASSWORD%@%ORACLE_SERVICE%
echo Project   : %CD%
echo.

REM 1. Check Docker CLI
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not installed or Docker CLI is not available.
    echo Please open Docker Desktop first, then run this file again.
    pause
    exit /b 1
)

REM 2. If container is missing, create it from docker-compose.yml
docker ps -a --format "{{.Names}}" | findstr /x "%CONTAINER_NAME%" >nul 2>&1
if errorlevel 1 (
    echo [WARN] Container "%CONTAINER_NAME%" was not found.
    echo [INFO] Running docker compose up -d to create/start Oracle container...
    docker compose up -d
    if errorlevel 1 (
        echo [ERROR] docker compose up -d failed.
        echo Check Docker Desktop and docker-compose.yml.
        pause
        exit /b 1
    )
)

REM 3. Start container if it exists but is not running
docker ps --format "{{.Names}}" | findstr /x "%CONTAINER_NAME%" >nul 2>&1
if errorlevel 1 (
    echo [INFO] Container is not running. Starting %CONTAINER_NAME%...
    docker start %CONTAINER_NAME%
    if errorlevel 1 (
        echo [ERROR] Cannot start container %CONTAINER_NAME%.
        pause
        exit /b 1
    )
)

echo [INFO] Waiting Oracle to be ready. This can take a few minutes on first startup...

REM 4. Test SQLPlus connection inside container with retries
set CONNECT_OK=0
for /L %%I in (1,1,18) do (
    echo [INFO] Testing Oracle connection attempt %%I/18...
    echo EXIT; | docker exec -i %CONTAINER_NAME% sqlplus -L %ORACLE_USER%/%ORACLE_PASSWORD%@%ORACLE_SERVICE% >nul 2>&1
    if not errorlevel 1 (
        set CONNECT_OK=1
        goto CONNECT_READY
    )
    timeout /t 10 /nobreak >nul
)

:CONNECT_READY
if "%CONNECT_OK%"=="0" (
    echo [ERROR] Cannot connect to Oracle inside Docker.
    echo Check container logs:
    echo docker logs -f %CONTAINER_NAME%
    echo.
    echo Common causes:
    echo - Oracle is still starting.
    echo - ORACLE_PASSWORD in docker-compose.yml is different from ORACLE_PASSWORD in this file.
    echo - ORACLE_SERVICE is not FREEPDB1.
    pause
    exit /b 1
)

echo [OK] Oracle connection is ready.

REM 5. Run known patch files
set HAS_PATCH=0

call :RUN_PATCH "database\05_session_and_login_history_patch.sql"

REM Optional: run all .sql files under database\patches if this folder exists
if exist "database\patches" (
    for %%F in (database\patches\*.sql) do (
        call :RUN_PATCH "%%F"
    )
)

if "%HAS_PATCH%"=="0" (
    echo [WARN] No patch file was found.
    echo Expected files like database\05_session_and_login_history_patch.sql or database\patches\*.sql
) else (
    echo.
    echo ==========================================================
    echo [DONE] Database patch process completed.
    echo ==========================================================
)

pause
exit /b 0

:RUN_PATCH
set PATCH_FILE=%~1
if not exist "%PATCH_FILE%" (
    goto :EOF
)

set HAS_PATCH=1
echo.
echo ----------------------------------------------------------
echo [RUN] %PATCH_FILE%
echo ----------------------------------------------------------

docker exec -i %CONTAINER_NAME% sqlplus -L %ORACLE_USER%/%ORACLE_PASSWORD%@%ORACLE_SERVICE% < "%PATCH_FILE%"

if errorlevel 1 (
    echo [ERROR] Failed while running %PATCH_FILE%
    pause
    exit /b 1
)

echo [OK] Finished %PATCH_FILE%
goto :EOF
=======
@echo off
setlocal enabledelayedexpansion

REM ==========================================================
REM Smart Supermarket - Oracle Docker Patch Runner
REM Purpose: run database patch SQL files into local Oracle Docker
REM ==========================================================

set CONTAINER_NAME=supermarket-oracle
set ORACLE_USER=system
set ORACLE_PASSWORD=Admin123
set ORACLE_SERVICE=FREEPDB1

cd /d "%~dp0.."

echo.
echo ==========================================================
echo Smart Supermarket - Database Patch Runner
echo ==========================================================
echo Container : %CONTAINER_NAME%
echo Connection: %ORACLE_USER%/%ORACLE_PASSWORD%@%ORACLE_SERVICE%
echo Project   : %CD%
echo.

REM 1. Check Docker CLI
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker is not installed or Docker CLI is not available.
    echo Please open Docker Desktop first, then run this file again.
    pause
    exit /b 1
)

REM 2. If container is missing, create it from docker-compose.yml
docker ps -a --format "{{.Names}}" | findstr /x "%CONTAINER_NAME%" >nul 2>&1
if errorlevel 1 (
    echo [WARN] Container "%CONTAINER_NAME%" was not found.
    echo [INFO] Running docker compose up -d to create/start Oracle container...
    docker compose up -d
    if errorlevel 1 (
        echo [ERROR] docker compose up -d failed.
        echo Check Docker Desktop and docker-compose.yml.
        pause
        exit /b 1
    )
)

REM 3. Start container if not running
docker ps --format "{{.Names}}" | findstr /x "%CONTAINER_NAME%" >nul 2>&1
if errorlevel 1 (
    echo [INFO] Container is not running. Starting %CONTAINER_NAME%...
    docker start %CONTAINER_NAME%
    if errorlevel 1 (
        echo [ERROR] Cannot start container %CONTAINER_NAME%.
        pause
        exit /b 1
    )
    echo [INFO] Waiting Oracle to be ready...
    timeout /t 20 /nobreak >nul
)

REM 4. Test SQLPlus connection inside container
echo [INFO] Testing Oracle connection...
echo EXIT; | docker exec -i %CONTAINER_NAME% sqlplus -L %ORACLE_USER%/%ORACLE_PASSWORD%@%ORACLE_SERVICE% >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Cannot connect to Oracle inside Docker.
    echo Check container logs or wait until database is ready:
    echo docker logs -f %CONTAINER_NAME%
    pause
    exit /b 1
)

REM 5. Run known patch files
set HAS_PATCH=0

call :RUN_PATCH "database\05_session_and_login_history_patch.sql"

REM Optional: run all .sql files under database\patches if this folder exists
if exist "database\patches" (
    for %%F in (database\patches\*.sql) do (
        call :RUN_PATCH "%%F"
    )
)

if "%HAS_PATCH%"=="0" (
    echo [WARN] No patch file was found.
    echo Expected files like database\05_session_and_login_history_patch.sql or database\patches\*.sql
) else (
    echo.
    echo ==========================================================
    echo [DONE] Database patch process completed.
    echo ==========================================================
)

pause
exit /b 0

:RUN_PATCH
set PATCH_FILE=%~1
if not exist "%PATCH_FILE%" (
    goto :EOF
)

set HAS_PATCH=1
echo.
echo ----------------------------------------------------------
echo [RUN] %PATCH_FILE%
echo ----------------------------------------------------------

docker exec -i %CONTAINER_NAME% sqlplus -L %ORACLE_USER%/%ORACLE_PASSWORD%@%ORACLE_SERVICE% < "%PATCH_FILE%"

if errorlevel 1 (
    echo [ERROR] Failed while running %PATCH_FILE%
    pause
    exit /b 1
)

echo [OK] Finished %PATCH_FILE%
goto :EOF
>>>>>>> 6cbb195 (fix file .bath)
