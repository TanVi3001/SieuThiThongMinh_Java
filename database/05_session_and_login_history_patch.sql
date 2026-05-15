-- ==========================================================
-- 05_session_and_login_history_patch.sql
-- Patch for account online status, multi-session and login history
-- Run by SYSTEM/Admin123 before launching Java app
-- ==========================================================

-- ==========================================================
-- 1. Patch ACCOUNTS columns
-- ==========================================================

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNTS ADD ACTIVE_SESSIONS NUMBER(10) DEFAULT 0';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNTS ADD CURRENT_SESSION_ID VARCHAR2(100)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNTS ADD ONLINE_STATUS VARCHAR2(20) DEFAULT ''OFFLINE''';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNTS ADD LAST_HEARTBEAT_AT TIMESTAMP';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNTS ADD LAST_LOGIN_AT TIMESTAMP';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNTS ADD LAST_LOGOUT_AT TIMESTAMP';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

UPDATE ACCOUNTS
SET ACTIVE_SESSIONS = NVL(ACTIVE_SESSIONS, 0),
    ONLINE_STATUS = NVL(ONLINE_STATUS, 'OFFLINE');

COMMIT;

-- ==========================================================
-- 2. Reset runtime login status
-- ==========================================================

UPDATE ACCOUNTS
SET ACTIVE_SESSIONS = 0,
    CURRENT_SESSION_ID = NULL,
    ONLINE_STATUS = 'OFFLINE',
    LAST_HEARTBEAT_AT = NULL,
    LAST_LOGIN_AT = NULL,
    LAST_LOGOUT_AT = NULL;

COMMIT;

-- ==========================================================
-- 3. Check LOGIN_HISTORY structure
-- ==========================================================

-- Nếu LOGIN_HISTORY chưa có sequence cho LOG_ID thì tạo
BEGIN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE LOGIN_HISTORY_SEQ START WITH 1 INCREMENT BY 1 NOCACHE';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- Nếu LOG_ID đang không tự sinh, tạo trigger tự sinh LOG_ID
CREATE OR REPLACE TRIGGER TRG_LOGIN_HISTORY_ID
BEFORE INSERT ON LOGIN_HISTORY
FOR EACH ROW
BEGIN
    IF :NEW.LOG_ID IS NULL THEN
        :NEW.LOG_ID := LOGIN_HISTORY_SEQ.NEXTVAL;
    END IF;

    IF :NEW.LOGIN_TIME IS NULL THEN
        :NEW.LOGIN_TIME := CURRENT_TIMESTAMP;
    END IF;

    IF :NEW.STATUS IS NULL THEN
        :NEW.STATUS := 'UNKNOWN';
    END IF;

    IF :NEW.ACTION_TYPE IS NULL THEN
        :NEW.ACTION_TYPE := 'LOGIN';
    END IF;

    IF :NEW.IP_ADDRESS IS NULL THEN
        :NEW.IP_ADDRESS := 'unknown';
    END IF;

    IF :NEW.DEVICE_INFO IS NULL THEN
        :NEW.DEVICE_INFO := 'unknown';
    END IF;
END;
/

COMMIT;

-- ==========================================================
-- 4. Verify
-- ==========================================================

SELECT COLUMN_NAME, DATA_TYPE
FROM USER_TAB_COLUMNS
WHERE TABLE_NAME = 'ACCOUNTS'
  AND COLUMN_NAME IN (
      'ACTIVE_SESSIONS',
      'CURRENT_SESSION_ID',
      'ONLINE_STATUS',
      'LAST_HEARTBEAT_AT',
      'LAST_LOGIN_AT',
      'LAST_LOGOUT_AT'
  )
ORDER BY COLUMN_NAME;

SELECT COLUMN_NAME, NULLABLE
FROM USER_TAB_COLUMNS
WHERE TABLE_NAME = 'LOGIN_HISTORY'
ORDER BY COLUMN_ID;