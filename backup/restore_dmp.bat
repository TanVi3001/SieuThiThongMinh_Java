@echo off
setlocal enabledelayedexpansion

REM ==========================================================
REM Smart Supermarket - Restore Latest Backup
REM Purpose: Import latest .DMP file in backup folder
REM ==========================================================

set DB_HOST=10.0.216.238
set DB_PORT=1521
set DB_SERVICE=orcl

set DB_ADMIN_USER=system
set DB_ADMIN_PASSWORD=Admin123

set APP_SCHEMA=SYSTEM

set BACKUP_DIR=%~dp0
set ORACLE_DIR_NAME=DATA_PUMP_DIR_SMART

echo.
echo ==========================================================
echo Smart Supermarket - Restore Latest Backup
echo ==========================================================
echo Host       : %DB_HOST%
echo Port       : %DB_PORT%
echo Service    : %DB_SERVICE%
echo Schema     : %APP_SCHEMA%
echo Backup dir : %BACKUP_DIR%
echo ==========================================================
echo.

set LATEST_DMP=

for /f "delims=" %%F in ('dir /b /o-d "%BACKUP_DIR%*.DMP" 2^>nul') do (
    set LATEST_DMP=%%F
    goto FOUND_DMP
)

:FOUND_DMP

if "%LATEST_DMP%"=="" (
    echo [ERROR] No .DMP file found in backup folder.
    pause
    exit /b 1
)

echo Latest dump file:
echo %LATEST_DMP%
echo.

set LOG_FILE=%LATEST_DMP:.DMP=_RESTORE.log%
set LOG_FILE=%LOG_FILE:.dmp=_RESTORE.log%

set /p CONFIRM=Type YES to restore latest backup: 

if /I not "%CONFIRM%"=="YES" (
    echo [CANCELLED] Restore cancelled.
    pause
    exit /b 0
)

where sqlplus >nul 2>&1
if errorlevel 1 (
    echo [ERROR] sqlplus not found. Please check Oracle Client / PATH.
    pause
    exit /b 1
)

where impdp >nul 2>&1
if errorlevel 1 (
    echo [ERROR] impdp not found. Please check Oracle Client / PATH.
    pause
    exit /b 1
)

(
echo CREATE OR REPLACE DIRECTORY %ORACLE_DIR_NAME% AS '%BACKUP_DIR:\=/%';
echo GRANT READ, WRITE ON DIRECTORY %ORACLE_DIR_NAME% TO %DB_ADMIN_USER%;
echo EXIT;
) > "%TEMP%\smart_create_dp_dir.sql"

sqlplus -L %DB_ADMIN_USER%/%DB_ADMIN_PASSWORD%@//%DB_HOST%:%DB_PORT%/%DB_SERVICE% @"%TEMP%\smart_create_dp_dir.sql"

if errorlevel 1 (
    echo [ERROR] Cannot create Oracle DIRECTORY.
    pause
    exit /b 1
)

echo.
echo [INFO] Restoring %LATEST_DMP%...

impdp %DB_ADMIN_USER%/%DB_ADMIN_PASSWORD%@//%DB_HOST%:%DB_PORT%/%DB_SERVICE% ^
    DIRECTORY=%ORACLE_DIR_NAME% ^
    DUMPFILE=%LATEST_DMP% ^
    LOGFILE=%LOG_FILE% ^
    SCHEMAS=%APP_SCHEMA% ^
    TABLE_EXISTS_ACTION=REPLACE ^
    EXCLUDE=STATISTICS

if errorlevel 1 (
    echo.
    echo [ERROR] Restore failed.
    echo Check log file: %BACKUP_DIR%%LOG_FILE%
    pause
    exit /b 1
)

echo.
echo ==========================================================
echo [DONE] Restore completed successfully.
echo Dump file: %LATEST_DMP%
echo Log file : %LOG_FILE%
echo ==========================================================
echo.

pause
exit /b 0