-- Fix bug: tai khoan da khoa van dang nhap duoc
-- Ly do thuong gap: STATUS da doi sang 'Bi khoa' / 'Bị khóa' nhung IS_DELETED van = 0.
-- Login hien tai chi lay account voi NVL(IS_DELETED,0)=0, nen can dong bo khoa = IS_DELETED 1.

-- 1) Dong bo data hien co
UPDATE ACCOUNTS
SET is_deleted = 1,
    online_status = 'OFFLINE',
    active_sessions = 0,
    current_session_id = NULL,
    last_logout_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE UPPER(TRIM(NVL(status, 'Hoạt động'))) IN (
    'BỊ KHÓA', 'BI KHOA', 'LOCKED', 'DISABLED', 'INACTIVE', 'TẠM KHÓA', 'TAM KHOA'
);

UPDATE ACCOUNTS
SET status = N'Bị khóa',
    online_status = 'OFFLINE',
    active_sessions = 0,
    current_session_id = NULL,
    last_logout_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE NVL(is_deleted, 0) = 1
  AND UPPER(TRIM(NVL(status, ''))) NOT IN ('BỊ KHÓA', 'BI KHOA', 'LOCKED', 'DISABLED', 'INACTIVE', 'TẠM KHÓA', 'TAM KHOA');

COMMIT;

-- 2) Chan lech trang thai ve sau: cu set status khoa thi tu dong is_deleted = 1.
--    Cu set status hoat dong thi tu dong is_deleted = 0.
CREATE OR REPLACE TRIGGER TRG_ACCOUNTS_LOCK_SYNC
BEFORE INSERT OR UPDATE OF status, is_deleted ON ACCOUNTS
FOR EACH ROW
BEGIN
    IF NVL(:NEW.is_deleted, 0) = 1
       OR UPPER(TRIM(NVL(:NEW.status, N'Hoạt động'))) IN (
            'BỊ KHÓA', 'BI KHOA', 'LOCKED', 'DISABLED', 'INACTIVE', 'TẠM KHÓA', 'TAM KHOA'
       ) THEN
        :NEW.is_deleted := 1;
        :NEW.status := N'Bị khóa';
        :NEW.online_status := 'OFFLINE';
        :NEW.active_sessions := 0;
        :NEW.current_session_id := NULL;
        :NEW.last_logout_at := CURRENT_TIMESTAMP;
    ELSIF UPPER(TRIM(NVL(:NEW.status, N'Hoạt động'))) IN (
            'HOẠT ĐỘNG', 'HOAT DONG', 'ACTIVE', 'ENABLED'
          ) THEN
        :NEW.is_deleted := 0;
        :NEW.status := N'Hoạt động';
    END IF;

    :NEW.updated_at := CURRENT_TIMESTAMP;
END;
/

COMMIT;
