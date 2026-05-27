-- Fix ORA-00904: EMPLOYEES.UPDATED_AT invalid identifier
-- AccountRoleAssignmentPanel updates EMPLOYEES.updated_at when changing role/store.
-- Run this once on existing databases.

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tab_columns
    WHERE table_name = 'EMPLOYEES'
      AND column_name = 'UPDATED_AT';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE EMPLOYEES ADD (updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)';
    END IF;
END;
/

COMMIT;
