@echo off
REM Export Oracle database demo for Smart Supermarket

expdp system/Admin123@//localhost:1522/FREEPDB1 DIRECTORY=DATA_PUMP_DIR_SMART DUMPFILE=smart_supermarket_demo.dmp LOGFILE=smart_supermarket_export.log REUSE_DUMPFILES=Y

if errorlevel 1 (
    echo.
    echo EXPORT FAILED. Please check Oracle service name, username/password, or DATA_PUMP_DIR_SMART.
    pause
    exit /b 1
)

echo.
echo Export success.
echo Check file in Oracle DIRECTORY path.
pause