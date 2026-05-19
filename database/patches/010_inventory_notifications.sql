SET SERVEROUTPUT ON;
SET DEFINE OFF;

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tables
    WHERE table_name = 'INVENTORY_NOTIFICATIONS';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE INVENTORY_NOTIFICATIONS (
                notification_id VARCHAR2(50) PRIMARY KEY,
                product_id      VARCHAR2(50) NOT NULL,
                title           NVARCHAR2(255),
                message         NVARCHAR2(500),
                notify_type     VARCHAR2(30) DEFAULT ''LOW_STOCK'',
                target_role     VARCHAR2(50) DEFAULT ''WAREHOUSE'',
                status          VARCHAR2(30) DEFAULT ''PENDING'',
                click_count     NUMBER DEFAULT 1,
                created_by      VARCHAR2(50),
                created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                resolved_at     TIMESTAMP,
                is_deleted      NUMBER(1) DEFAULT 0,
                CONSTRAINT FK_INV_NOTI_PRODUCT
                    FOREIGN KEY (product_id)
                    REFERENCES PRODUCTS(product_id)
            )
        ';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_indexes
    WHERE index_name = 'IDX_INV_NOTI_PRODUCT_STATUS';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE INDEX IDX_INV_NOTI_PRODUCT_STATUS
            ON INVENTORY_NOTIFICATIONS(product_id, status, is_deleted)
        ';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_indexes
    WHERE index_name = 'IDX_INV_NOTI_TARGET_STATUS';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE INDEX IDX_INV_NOTI_TARGET_STATUS
            ON INVENTORY_NOTIFICATIONS(target_role, status, is_deleted)
        ';
    END IF;
END;
/


COMMIT;