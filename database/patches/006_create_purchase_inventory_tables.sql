-- ==========================================================
-- 006_create_purchase_inventory_tables.sql
-- Purpose:
--   Create purchase receipt and inventory transaction tables
--   for warehouse import flow.
-- Safe to re-run.
-- ==========================================================

SET DEFINE OFF;

-- ==========================================================
-- 1. PURCHASE_RECEIPTS
-- ==========================================================
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tables
    WHERE table_name = 'PURCHASE_RECEIPTS';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE PURCHASE_RECEIPTS (
                receipt_id         VARCHAR2(50) PRIMARY KEY,
                supplier_id        VARCHAR2(50),
                created_by         VARCHAR2(50),
                note               NVARCHAR2(500),
                total_before_tax   NUMBER(15,2) DEFAULT 0,
                total_tax          NUMBER(15,2) DEFAULT 0,
                total_after_tax    NUMBER(15,2) DEFAULT 0,
                created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_deleted         NUMBER(1) DEFAULT 0
            )
        ';
    END IF;
END;
/

-- ==========================================================
-- 2. PURCHASE_RECEIPT_DETAILS
-- ==========================================================
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tables
    WHERE table_name = 'PURCHASE_RECEIPT_DETAILS';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE PURCHASE_RECEIPT_DETAILS (
                receipt_detail_id  VARCHAR2(50) PRIMARY KEY,
                receipt_id         VARCHAR2(50) NOT NULL,
                product_id         VARCHAR2(50) NOT NULL,
                quantity           NUMBER(10) DEFAULT 0,
                unit               NVARCHAR2(50),
                unit_import_price  NUMBER(15,2) DEFAULT 0,
                sale_price         NUMBER(15,2) DEFAULT 0,
                vat_rate           NUMBER(5,2) DEFAULT 0,
                line_before_tax    NUMBER(15,2) DEFAULT 0,
                line_tax           NUMBER(15,2) DEFAULT 0,
                line_after_tax     NUMBER(15,2) DEFAULT 0,
                created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_deleted         NUMBER(1) DEFAULT 0
            )
        ';
    END IF;
END;
/

-- ==========================================================
-- 3. INVENTORY_TRANSACTIONS
-- ==========================================================
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tables
    WHERE table_name = 'INVENTORY_TRANSACTIONS';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE INVENTORY_TRANSACTIONS (
                transaction_id     VARCHAR2(50) PRIMARY KEY,
                receipt_id         VARCHAR2(50),
                product_id         VARCHAR2(50) NOT NULL,
                transaction_type   VARCHAR2(30) NOT NULL,
                quantity           NUMBER(10) DEFAULT 0,
                unit               NVARCHAR2(50),
                store_id           VARCHAR2(50),
                unit_import_price  NUMBER(15,2) DEFAULT 0,
                sale_price         NUMBER(15,2) DEFAULT 0,
                vat_rate           NUMBER(5,2) DEFAULT 0,
                vat_amount         NUMBER(15,2) DEFAULT 0,
                total_amount       NUMBER(15,2) DEFAULT 0,
                note               NVARCHAR2(500),
                created_by         VARCHAR2(50),
                created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                is_deleted         NUMBER(1) DEFAULT 0
            )
        ';
    END IF;
END;
/

-- ==========================================================
-- 4. ADD FOREIGN KEYS SAFELY
-- ==========================================================

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_constraints
    WHERE constraint_name = 'FK_PR_SUPPLIER';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE PURCHASE_RECEIPTS
            ADD CONSTRAINT FK_PR_SUPPLIER
            FOREIGN KEY (supplier_id)
            REFERENCES SUPPLIERS(supplier_id)
        ';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Skip FK_PR_SUPPLIER: ' || SQLERRM);
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_constraints
    WHERE constraint_name = 'FK_PRD_RECEIPT';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE PURCHASE_RECEIPT_DETAILS
            ADD CONSTRAINT FK_PRD_RECEIPT
            FOREIGN KEY (receipt_id)
            REFERENCES PURCHASE_RECEIPTS(receipt_id)
        ';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Skip FK_PRD_RECEIPT: ' || SQLERRM);
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_constraints
    WHERE constraint_name = 'FK_PRD_PRODUCT';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE PURCHASE_RECEIPT_DETAILS
            ADD CONSTRAINT FK_PRD_PRODUCT
            FOREIGN KEY (product_id)
            REFERENCES PRODUCTS(product_id)
        ';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Skip FK_PRD_PRODUCT: ' || SQLERRM);
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_constraints
    WHERE constraint_name = 'FK_IVT_PRODUCT';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE INVENTORY_TRANSACTIONS
            ADD CONSTRAINT FK_IVT_PRODUCT
            FOREIGN KEY (product_id)
            REFERENCES PRODUCTS(product_id)
        ';
    END IF;
EXCEPTION
    WHEN OTHERS THEN
        DBMS_OUTPUT.PUT_LINE('Skip FK_IVT_PRODUCT: ' || SQLERRM);
END;
/

-- ==========================================================
-- 5. ADD INDEXES SAFELY
-- ==========================================================

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_PR_SUPPLIER';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX IDX_PR_SUPPLIER ON PURCHASE_RECEIPTS(supplier_id)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_PRD_RECEIPT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX IDX_PRD_RECEIPT ON PURCHASE_RECEIPT_DETAILS(receipt_id)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_PRD_PRODUCT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX IDX_PRD_PRODUCT ON PURCHASE_RECEIPT_DETAILS(product_id)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_IVT_PRODUCT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX IDX_IVT_PRODUCT ON INVENTORY_TRANSACTIONS(product_id)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_IVT_RECEIPT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX IDX_IVT_RECEIPT ON INVENTORY_TRANSACTIONS(receipt_id)';
    END IF;
END;
/

COMMIT;