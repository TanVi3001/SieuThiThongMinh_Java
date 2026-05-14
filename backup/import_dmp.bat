@echo off
REM Import Oracle database demo for Smart Supermarket
REM Put smart_supermarket_demo.dmp into C:\oracle_backup before running this file.

impdp SMART_SUPERMARKET/123456 DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=smart_supermarket_demo.dmp LOGFILE=smart_supermarket_import.log TABLE_EXISTS_ACTION=REPLACE

echo.
echo Import finished. Check DataGrip and run the Java app.
pause
