@echo off
REM Import Oracle database demo for Smart Supermarket

impdp system/Admin123@//10.0.232.16:1521/orcl DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=smart_supermarket_demo.dmp LOGFILE=smart_supermarket_import.log TABLE_EXISTS_ACTION=REPLACE

echo.
echo Import finished. Check DataGrip and run the Java app.
pause