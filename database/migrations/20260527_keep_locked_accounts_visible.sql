-- Fix: tai khoan bi khoa van phai hien thi trong man Admin > Quan ly tai khoan
-- Khong dung IS_DELETED de khoa/mở tai khoan nua, vi IS_DELETED = 1 lam man hinh quan ly an mat tai khoan.
-- Trang thai khoa/mở se dung ACCOUNTS.STATUS:
--   Hoạt động  => duoc dang nhap
--   Bị khóa    => bi chan dang nhap, nhung van hien trong admin de mo lai

BEGIN
    EXECUTE IMMEDIATE 'DROP TRIGGER TRG_ACCOUNTS_LOCK_SYNC';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -4080 THEN -- ORA-04080: trigger does not exist
            RAISE;
        END IF;
END;
/

UPDATE ACCOUNTS
SET is_deleted = 0,
    online_status = 'OFFLINE',
    active_sessions = 0,
    current_session_id = NULL,
    last_logout_at = CURRENT_TIMESTAMP,
    updated_at = CURRENT_TIMESTAMP
WHERE UPPER(TRIM(NVL(status, ''))) IN (
    'BỊ KHÓA', 'BI KHOA', 'LOCKED', 'DISABLED', 'INACTIVE', 'TẠM KHÓA', 'TAM KHOA'
);

COMMIT;
