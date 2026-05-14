@echo off
REM Export Oracle database demo for Smart Supermarket
REM Make sure C:\oracle_backup exists before running this file.

if not exist C:\oracle_backup mkdir C:\oracle_backup

expdp SMART_SUPERMARKET/123456 DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=smart_supermarket_demo.dmp LOGFILE=smart_supermarket_export.log REUSE_DUMPFILES=Y

echo.
echo Export finished. Check file: C:\oracle_backup\smart_supermarket_demo.dmp
pause
