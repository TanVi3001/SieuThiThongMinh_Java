-- ==========================================================
-- 2026_05_21_full_store_scope_patch.sql
-- Full store-scope patch for Smart Supermarket multi-branch model
-- IMPORTANT:
--   CUSTOMERS remains GLOBAL across the whole chain.
--   Do NOT add store_id to CUSTOMERS.
--   Customer analytics per branch must join through ORDERS.store_id.
-- ==========================================================

SET DEFINE OFF;

-- ==========================================================
-- 1. REQUIRED MASTER TABLES / COLUMNS
-- ==========================================================

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'STORES';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE STORES (
                store_id VARCHAR2(50) PRIMARY KEY,
                store_name NVARCHAR2(255) NOT NULL,
                address NVARCHAR2(500),
                phone VARCHAR2(20),
                is_deleted NUMBER(1) DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tab_cols WHERE table_name = 'EMPLOYEES' AND column_name = 'STORE_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE EMPLOYEES ADD store_id VARCHAR2(50)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tab_cols WHERE table_name = 'ORDERS' AND column_name = 'STORE_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE ORDERS ADD store_id VARCHAR2(50)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tab_cols WHERE table_name = 'INVENTORY' AND column_name = 'STORE_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE INVENTORY ADD store_id VARCHAR2(50)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tab_cols WHERE table_name = 'PURCHASE_RECEIPTS' AND column_name = 'STORE_ID';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PURCHASE_RECEIPTS ADD store_id VARCHAR2(50)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tables WHERE table_name = 'STORE_PRODUCTS';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE STORE_PRODUCTS (
                store_id VARCHAR2(50) NOT NULL,
                product_id VARCHAR2(50) NOT NULL,
                selling_price NUMBER(18,2),
                is_active NUMBER(1) DEFAULT 1,
                min_stock NUMBER(10) DEFAULT 0,
                is_deleted NUMBER(1) DEFAULT 0,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                CONSTRAINT pk_store_products PRIMARY KEY (store_id, product_id)
            )';
    END IF;
END;
/

-- Add missing STORE_PRODUCTS columns safely.
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tab_cols WHERE table_name = 'STORE_PRODUCTS' AND column_name = 'SELLING_PRICE';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE STORE_PRODUCTS ADD selling_price NUMBER(18,2)'; END IF;
    SELECT COUNT(*) INTO v_count FROM user_tab_cols WHERE table_name = 'STORE_PRODUCTS' AND column_name = 'IS_ACTIVE';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE STORE_PRODUCTS ADD is_active NUMBER(1) DEFAULT 1'; END IF;
    SELECT COUNT(*) INTO v_count FROM user_tab_cols WHERE table_name = 'STORE_PRODUCTS' AND column_name = 'MIN_STOCK';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE STORE_PRODUCTS ADD min_stock NUMBER(10) DEFAULT 0'; END IF;
    SELECT COUNT(*) INTO v_count FROM user_tab_cols WHERE table_name = 'STORE_PRODUCTS' AND column_name = 'IS_DELETED';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE STORE_PRODUCTS ADD is_deleted NUMBER(1) DEFAULT 0'; END IF;
    SELECT COUNT(*) INTO v_count FROM user_tab_cols WHERE table_name = 'STORE_PRODUCTS' AND column_name = 'CREATED_AT';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE STORE_PRODUCTS ADD created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP'; END IF;
    SELECT COUNT(*) INTO v_count FROM user_tab_cols WHERE table_name = 'STORE_PRODUCTS' AND column_name = 'UPDATED_AT';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE STORE_PRODUCTS ADD updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP'; END IF;
END;
/

-- ==========================================================
-- 2. FOREIGN KEYS / INDEXES, added idempotently by name
-- ==========================================================

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_constraints WHERE constraint_name = 'FK_EMPLOYEES_STORE';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE EMPLOYEES ADD CONSTRAINT fk_employees_store FOREIGN KEY (store_id) REFERENCES STORES(store_id)'; END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_constraints WHERE constraint_name = 'FK_ORDERS_STORE';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE ORDERS ADD CONSTRAINT fk_orders_store FOREIGN KEY (store_id) REFERENCES STORES(store_id)'; END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_constraints WHERE constraint_name = 'FK_INVENTORY_STORE';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE INVENTORY ADD CONSTRAINT fk_inventory_store FOREIGN KEY (store_id) REFERENCES STORES(store_id)'; END IF;
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_EMPLOYEES_STORE';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'CREATE INDEX idx_employees_store ON EMPLOYEES(store_id)'; END IF;
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_ORDERS_STORE_DATE';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'CREATE INDEX idx_orders_store_date ON ORDERS(store_id, order_date)'; END IF;
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_INVENTORY_STORE_PRODUCT';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'CREATE INDEX idx_inventory_store_product ON INVENTORY(store_id, product_id)'; END IF;
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_STORE_PRODUCTS_STORE_PRODUCT';
    IF v_count = 0 THEN EXECUTE IMMEDIATE 'CREATE INDEX idx_store_products_store_product ON STORE_PRODUCTS(store_id, product_id)'; END IF;
END;
/

-- ==========================================================
-- 3. TEST DATA: at least two stores, distinct manager scope
-- ==========================================================

MERGE INTO STORES s
USING (SELECT 'ST001' store_id, N'Làng Đại học Thủ Đức' store_name, N'Khu đô thị Đại học Quốc gia TP.HCM, Thủ Đức' address FROM dual) x
ON (s.store_id = x.store_id)
WHEN MATCHED THEN UPDATE SET s.store_name = x.store_name, s.address = x.address, s.is_deleted = 0, s.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (store_id, store_name, address, is_deleted) VALUES (x.store_id, x.store_name, x.address, 0);

MERGE INTO STORES s
USING (SELECT 'ST_DI_AN' store_id, N'Dĩ An, Bình Dương' store_name, N'Dĩ An, Bình Dương' address FROM dual) x
ON (s.store_id = x.store_id)
WHEN MATCHED THEN UPDATE SET s.store_name = x.store_name, s.address = x.address, s.is_deleted = 0, s.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (store_id, store_name, address, is_deleted) VALUES (x.store_id, x.store_name, x.address, 0);

-- Keep these updates tolerant because seed IDs/names can vary by local DB.
UPDATE EMPLOYEES SET store_id = 'ST001'
WHERE NVL(is_deleted, 0) = 0
  AND (LOWER(employee_name) LIKE LOWER(N'%Tùng%') OR LOWER(employee_name) LIKE LOWER('%Tung%'));

UPDATE EMPLOYEES SET store_id = 'ST_DI_AN'
WHERE NVL(is_deleted, 0) = 0
  AND (LOWER(employee_name) LIKE LOWER(N'%Tiệp%') OR LOWER(employee_name) LIKE LOWER('%Tiep%'));

-- Backfill missing store_id for old operational data only when NULL, never hard-code in Java.
UPDATE ORDERS SET store_id = 'ST001' WHERE store_id IS NULL;
UPDATE INVENTORY SET store_id = 'ST001' WHERE store_id IS NULL;
UPDATE PURCHASE_RECEIPTS SET store_id = 'ST001' WHERE store_id IS NULL;

-- Backfill STORE_PRODUCTS from current inventory, preserving per-store independence.
MERGE INTO STORE_PRODUCTS sp
USING (
    SELECT DISTINCT i.store_id, i.product_id, p.base_price
    FROM INVENTORY i
    JOIN PRODUCTS p ON p.product_id = i.product_id
    WHERE i.store_id IS NOT NULL
      AND NVL(i.is_deleted, 0) = 0
      AND NVL(p.is_deleted, 0) = 0
) x
ON (sp.store_id = x.store_id AND sp.product_id = x.product_id)
WHEN MATCHED THEN UPDATE SET sp.selling_price = NVL(sp.selling_price, x.base_price), sp.is_active = 1, sp.is_deleted = 0, sp.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (store_id, product_id, selling_price, is_active, min_stock, is_deleted, created_at, updated_at)
VALUES (x.store_id, x.product_id, x.base_price, 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

COMMIT;

-- ==========================================================
-- 4. QUICK CHECK QUERIES
-- ==========================================================

PROMPT === Check managers by store ===
SELECT e.employee_id, e.employee_name, e.role_id, e.store_id, s.store_name
FROM EMPLOYEES e
LEFT JOIN STORES s ON s.store_id = e.store_id
WHERE NVL(e.is_deleted, 0) = 0
  AND e.role_id = 'R_STORE_MNG'
ORDER BY e.employee_name;

PROMPT === Check inventory by store ===
SELECT i.store_id, s.store_name, COUNT(*) product_count, SUM(i.quantity) total_quantity
FROM INVENTORY i
LEFT JOIN STORES s ON s.store_id = i.store_id
WHERE NVL(i.is_deleted, 0) = 0
GROUP BY i.store_id, s.store_name
ORDER BY i.store_id;

PROMPT === Check orders revenue by store ===
SELECT o.store_id, s.store_name, COUNT(*) total_orders, SUM(o.total_amount) revenue
FROM ORDERS o
LEFT JOIN STORES s ON s.store_id = o.store_id
WHERE NVL(o.is_deleted, 0) = 0
GROUP BY o.store_id, s.store_name
ORDER BY o.store_id;

PROMPT === CUSTOMERS must remain global: no store_id column expected ===
SELECT column_name
FROM user_tab_cols
WHERE table_name = 'CUSTOMERS'
  AND column_name = 'STORE_ID';
