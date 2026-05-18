@echo off
setlocal enabledelayedexpansion

REM ==========================================================
REM Smart Supermarket - Oracle Data Pump Export
REM Purpose: Backup full application schema to .DMP file
REM ==========================================================

REM ====== CONNECTION CONFIG ======
set DB_HOST=10.0.216.238
set DB_PORT=1521
set DB_SERVICE=orcl

set DB_ADMIN_USER=system
set DB_ADMIN_PASSWORD=Admin123

REM Schema chứa bảng đồ án.
REM Nếu bảng đang nằm trong SYSTEM thì để SYSTEM.
REM Nếu dùng user riêng, đổi thành SMARTUSER hoặc tên user của bạn.
set APP_SCHEMA=SYSTEM

REM ====== BACKUP CONFIG ======
set BACKUP_DIR=%~dp0
set ORACLE_DIR_NAME=DATA_PUMP_DIR_SMART

REM Timestamp: yyyyMMdd_HHmmss
for /f "tokens=1-4 delims=/ " %%a in ("%date%") do (
    set DD=%%a
    set MM=%%b
    set YYYY=%%c
)

for /f "tokens=1-3 delims=:." %%a in ("%time%") do (
    set HH=%%a
    set MI=%%b
    set SS=%%c
)

set HH=%HH: =0%
set TIMESTAMP=%YYYY%%MM%%DD%_%HH%%MI%%SS%

set DUMP_FILE=SMART_SUPERMARKET_%APP_SCHEMA%_%TIMESTAMP%.DMP
set LOG_FILE=SMART_SUPERMARKET_%APP_SCHEMA%_%TIMESTAMP%_EXPORT.log

echo.
echo ==========================================================
echo Smart Supermarket - Database Backup
echo ==========================================================
echo Host       : %DB_HOST%
echo Port       : %DB_PORT%
echo Service    : %DB_SERVICE%
echo Schema     : %APP_SCHEMA%
echo Backup dir : %BACKUP_DIR%
echo Dump file  : %DUMP_FILE%
echo Log file   : %LOG_FILE%
echo ==========================================================
echo.

REM ====== CHECK COMMANDS ======
where sqlplus >nul 2>&1
if errorlevel 1 (
    echo [ERROR] sqlplus not found. Please check Oracle Client / PATH.
    pause
    exit /b 1
)

where expdp >nul 2>&1
if errorlevel 1 (
    echo [ERROR] expdp not found. Please check Oracle Client / PATH.
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
    echo Please check DB connection, username/password, or permission.
    pause
    exit /b 1
)

REM ====== EXPORT SCHEMA ======
echo.
echo [INFO] Exporting schema %APP_SCHEMA%...

expdp %DB_ADMIN_USER%/%DB_ADMIN_PASSWORD%@//%DB_HOST%:%DB_PORT%/%DB_SERVICE% ^
    DIRECTORY=%ORACLE_DIR_NAME% ^
    DUMPFILE=%DUMP_FILE% ^
    LOGFILE=%LOG_FILE% ^
    SCHEMAS=%APP_SCHEMA% ^
    FLASHBACK_TIME=SYSTIMESTAMP ^
    EXCLUDE=STATISTICS ^
    REUSE_DUMPFILES=Y

if errorlevel 1 (
    echo.
    echo [ERROR] Export failed.
    echo Check log file: %BACKUP_DIR%%LOG_FILE%
    pause
    exit /b 1
)

echo.
echo ==========================================================
echo [DONE] Backup completed successfully.
echo Dump file: %BACKUP_DIR%%DUMP_FILE%
echo Log file : %BACKUP_DIR%%LOG_FILE%
echo ==========================================================
echo.

pause
exit /b 0