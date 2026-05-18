-- ==========================================================
-- 06_inventory_notifications.sql
-- Purpose:
-- Luu thong bao quan ly gui cho nhan vien kho.
-- Thong bao chi mat khi san pham duoc nhap them.
-- ==========================================================

SET SERVEROUTPUT ON;

BEGIN
    EXECUTE IMMEDIATE '
        CREATE TABLE INVENTORY_NOTIFICATIONS (
            notification_id VARCHAR2(50) PRIMARY KEY,
            product_id      VARCHAR2(50) NOT NULL,
            product_name    NVARCHAR2(255),
            message         NVARCHAR2(1000),
            target_role     VARCHAR2(50) DEFAULT ''WAREHOUSE'',
            status          VARCHAR2(20) DEFAULT ''PENDING'',
            remind_count    NUMBER DEFAULT 1,
            created_by      VARCHAR2(50),
            created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            resolved_at     TIMESTAMP,
            is_deleted      NUMBER(1) DEFAULT 0
        )
    ';

    DBMS_OUTPUT.PUT_LINE('Created table INVENTORY_NOTIFICATIONS.');

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('Table INVENTORY_NOTIFICATIONS already exists. Skip create table.');
        ELSE
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE '
        CREATE INDEX IDX_INV_NOTI_PRODUCT_STATUS
        ON INVENTORY_NOTIFICATIONS(product_id, status)
    ';

    DBMS_OUTPUT.PUT_LINE('Created index IDX_INV_NOTI_PRODUCT_STATUS.');

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('Index IDX_INV_NOTI_PRODUCT_STATUS already exists. Skip create index.');
        ELSE
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE '
        CREATE INDEX IDX_INV_NOTI_TARGET_STATUS
        ON INVENTORY_NOTIFICATIONS(target_role, status)
    ';

    DBMS_OUTPUT.PUT_LINE('Created index IDX_INV_NOTI_TARGET_STATUS.');

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('Index IDX_INV_NOTI_TARGET_STATUS already exists. Skip create index.');
        ELSE
            RAISE;
        END IF;
END;
/

COMMIT;


