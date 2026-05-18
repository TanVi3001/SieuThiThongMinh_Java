@echo off
setlocal

set CONTAINER_NAME=supermarket-oracle
set ORACLE_USER=system
set ORACLE_PASSWORD=Admin123
set ORACLE_SERVICE=FREEPDB1

set DUMP_FILE=SMART_SUPERMARKET_DEMO.DMP
set IMPORT_LOG=smart_supermarket_import.log
set CONTAINER_BACKUP_DIR=/opt/oracle/backup
set DIRECTORY_NAME=DATA_PUMP_DIR_SMART

cd /d "%~dp0"

echo Checking DMP file...
if not exist "%DUMP_FILE%" (
    echo [ERROR] Cannot find %DUMP_FILE%
    pause
    exit /b 1
)

echo Checking container...
docker ps --format "{{.Names}}" | findstr /x "%CONTAINER_NAME%" >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Container %CONTAINER_NAME% is not running.
    echo Please run docker compose up -d first.
    pause
    exit /b 1
)

echo Creating backup folder inside container...
docker exec %CONTAINER_NAME% bash -lc "mkdir -p %CONTAINER_BACKUP_DIR%"

echo Copying DMP into container...
docker cp "%DUMP_FILE%" %CONTAINER_NAME%:%CONTAINER_BACKUP_DIR%/%DUMP_FILE%

echo Creating Oracle DIRECTORY...
(
echo CREATE OR REPLACE DIRECTORY %DIRECTORY_NAME% AS '%CONTAINER_BACKUP_DIR%';
echo GRANT READ, WRITE ON DIRECTORY %DIRECTORY_NAME% TO %ORACLE_USER%;
echo EXIT;
) | docker exec -i %CONTAINER_NAME% sqlplus -L %ORACLE_USER%/%ORACLE_PASSWORD%@%ORACLE_SERVICE%

echo Importing DMP...
docker exec -i %CONTAINER_NAME% impdp %ORACLE_USER%/%ORACLE_PASSWORD%@%ORACLE_SERVICE% DIRECTORY=%DIRECTORY_NAME% DUMPFILE=%DUMP_FILE% LOGFILE=%IMPORT_LOG% TABLE_EXISTS_ACTION=REPLACE

if errorlevel 1 (
    echo [ERROR] Import failed.
    pause
    exit /b 1
)

echo Copying import log back...
docker cp %CONTAINER_NAME%:%CONTAINER_BACKUP_DIR%/%IMPORT_LOG% "%~dp0%IMPORT_LOG%"

echo.
echo [DONE] Restore completed.
pause