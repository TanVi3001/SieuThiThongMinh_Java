@echo off
setlocal enabledelayedexpansion

REM ==========================================================
REM Smart Supermarket - Oracle Data Pump Import
REM Purpose: Import .DMP file into application schema
REM ==========================================================

REM ====== CONNECTION CONFIG ======
set DB_HOST=10.0.216.238
set DB_PORT=1521
set DB_SERVICE=orcl

set DB_ADMIN_USER=system
set DB_ADMIN_PASSWORD=Admin123

REM Schema đích.
REM Nếu bảng đồ án nằm trong SYSTEM thì để SYSTEM.
set APP_SCHEMA=SYSTEM

REM ====== BACKUP CONFIG ======
set BACKUP_DIR=%~dp0
set ORACLE_DIR_NAME=DATA_PUMP_DIR_SMART

echo.
echo ==========================================================
echo Smart Supermarket - Database Import
echo ==========================================================
echo Host       : %DB_HOST%
echo Port       : %DB_PORT%
echo Service    : %DB_SERVICE%
echo Schema     : %APP_SCHEMA%
echo Backup dir : %BACKUP_DIR%
echo ==========================================================
echo.

echo Available .DMP files:
echo ----------------------------------------------------------
dir /b "%BACKUP_DIR%*.DMP"
echo ----------------------------------------------------------
echo.

set /p DUMP_FILE=Enter dump file name, example SMART_SUPERMARKET_SYSTEM_20260518_150000.DMP: 

if "%DUMP_FILE%"=="" (
    echo [ERROR] Dump file name is empty.
    pause
    exit /b 1
)

if not exist "%BACKUP_DIR%%DUMP_FILE%" (
    echo [ERROR] Dump file not found: %BACKUP_DIR%%DUMP_FILE%
    pause
    exit /b 1
)

set LOG_FILE=%DUMP_FILE:.DMP=_IMPORT.log%
set LOG_FILE=%LOG_FILE:.dmp=_IMPORT.log%

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

REM ====== CREATE OR REPLACE ORACLE DIRECTORY ======
echo [INFO] Creating Oracle DIRECTORY %ORACLE_DIR_NAME%...

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
echo [WARN] Import will replace existing tables if they already exist.
echo.
set /p CONFIRM=Type YES to continue import: 

if /I not "%CONFIRM%"=="YES" (
    echo [CANCELLED] Import cancelled.
    pause
    exit /b 0
)

echo.
echo [INFO] Importing dump file %DUMP_FILE%...

impdp %DB_ADMIN_USER%/%DB_ADMIN_PASSWORD%@//%DB_HOST%:%DB_PORT%/%DB_SERVICE% ^
    DIRECTORY=%ORACLE_DIR_NAME% ^
    DUMPFILE=%DUMP_FILE% ^
    LOGFILE=%LOG_FILE% ^
    SCHEMAS=%APP_SCHEMA% ^
    TABLE_EXISTS_ACTION=REPLACE ^
    EXCLUDE=STATISTICS

if errorlevel 1 (
    echo.
    echo [ERROR] Import failed.
    echo Check log file: %BACKUP_DIR%%LOG_FILE%
    pause
    exit /b 1
)

echo.
echo ==========================================================
echo [DONE] Import completed successfully.
echo Log file: %BACKUP_DIR%%LOG_FILE%
echo ==========================================================
echo.

pause
exit /b 0