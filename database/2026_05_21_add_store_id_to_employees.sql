-- ==========================================================
-- Migration: Add store_id to EMPLOYEES
-- Purpose  : Scope Manager employee management by store_id
-- ==========================================================

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_columns
    WHERE table_name = 'EMPLOYEES'
      AND column_name = 'STORE_ID';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE EMPLOYEES ADD store_id VARCHAR2(50)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_constraints
    WHERE constraint_name = 'FK_EMPLOYEES_STORES';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            ALTER TABLE EMPLOYEES
            ADD CONSTRAINT FK_EMPLOYEES_STORES
            FOREIGN KEY (store_id)
            REFERENCES STORES(store_id)
        ';
    END IF;
END;
/

UPDATE EMPLOYEES
SET store_id = 'ST001'
WHERE store_id IS NULL
  AND NVL(is_deleted, 0) = 0
  AND EXISTS (
      SELECT 1
      FROM STORES
      WHERE store_id = 'ST001'
  );

COMMIT;