-- ==========================================================
-- 07_inventory_receipt_history.sql
-- Purpose:
-- - Luu phieu nhap hang
-- - Luu chi tiet phieu nhap
-- - Luu lich su bien dong ton kho
-- ==========================================================

SET SERVEROUTPUT ON;

BEGIN
    EXECUTE IMMEDIATE '
        CREATE TABLE PURCHASE_RECEIPTS (
            receipt_id         VARCHAR2(50) PRIMARY KEY,
            supplier_id        VARCHAR2(50),
            created_by         VARCHAR2(50),
            note               NVARCHAR2(1000),
            total_before_tax   NUMBER(18,2) DEFAULT 0,
            total_tax          NUMBER(18,2) DEFAULT 0,
            total_after_tax    NUMBER(18,2) DEFAULT 0,
            created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            is_deleted         NUMBER(1) DEFAULT 0
        )
    ';
    DBMS_OUTPUT.PUT_LINE('Created PURCHASE_RECEIPTS.');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('PURCHASE_RECEIPTS already exists. Skip.');
        ELSE
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE '
        CREATE TABLE PURCHASE_RECEIPT_DETAILS (
            receipt_detail_id  VARCHAR2(50) PRIMARY KEY,
            receipt_id         VARCHAR2(50) NOT NULL,
            product_id         VARCHAR2(50) NOT NULL,
            quantity           NUMBER(10) NOT NULL,
            unit               NVARCHAR2(50),
            unit_import_price  NUMBER(18,2) NOT NULL,
            sale_price         NUMBER(18,2) NOT NULL,
            vat_rate           NUMBER(5,2) DEFAULT 0,
            line_before_tax    NUMBER(18,2) DEFAULT 0,
            line_tax           NUMBER(18,2) DEFAULT 0,
            line_after_tax     NUMBER(18,2) DEFAULT 0,
            created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            is_deleted         NUMBER(1) DEFAULT 0
        )
    ';
    DBMS_OUTPUT.PUT_LINE('Created PURCHASE_RECEIPT_DETAILS.');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('PURCHASE_RECEIPT_DETAILS already exists. Skip.');
        ELSE
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE '
        CREATE TABLE INVENTORY_TRANSACTIONS (
            transaction_id     VARCHAR2(50) PRIMARY KEY,
            receipt_id         VARCHAR2(50),
            product_id         VARCHAR2(50) NOT NULL,
            transaction_type   VARCHAR2(30) NOT NULL,
            quantity           NUMBER(10) NOT NULL,
            unit               NVARCHAR2(50),
            store_id           VARCHAR2(50),
            unit_import_price  NUMBER(18,2),
            sale_price         NUMBER(18,2),
            vat_rate           NUMBER(5,2),
            vat_amount         NUMBER(18,2),
            total_amount       NUMBER(18,2),
            note               NVARCHAR2(1000),
            created_by         VARCHAR2(50),
            created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            is_deleted         NUMBER(1) DEFAULT 0
        )
    ';
    DBMS_OUTPUT.PUT_LINE('Created INVENTORY_TRANSACTIONS.');
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('INVENTORY_TRANSACTIONS already exists. Skip.');
        ELSE
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE '
        CREATE INDEX IDX_INV_TRANS_PRODUCT_TIME
        ON INVENTORY_TRANSACTIONS(product_id, created_at)
    ';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('IDX_INV_TRANS_PRODUCT_TIME already exists. Skip.');
        ELSE
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE '
        CREATE INDEX IDX_PUR_RECEIPT_CREATED_AT
        ON PURCHASE_RECEIPTS(created_at)
    ';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('IDX_PUR_RECEIPT_CREATED_AT already exists. Skip.');
        ELSE
            RAISE;
        END IF;
END;
/

COMMIT;

PROMPT DONE: inventory receipt and transaction history patch completed.