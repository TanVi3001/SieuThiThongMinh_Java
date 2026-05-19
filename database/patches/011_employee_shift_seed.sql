-- ==========================================================
-- 011_employee_shift_seed.sql
-- Purpose:
--   Add the employee shift assignment table and seed enough
--   sale/cashier + warehouse data for Employee Management > Phan ca.
-- Safe to re-run.
-- ==========================================================

--SET DEFINE OFF;

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM user_tables
    WHERE table_name = 'EMPLOYEE_SHIFTS';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE TABLE EMPLOYEE_SHIFTS (
                assignment_id VARCHAR2(50) PRIMARY KEY,
                employee_id   VARCHAR2(50) NOT NULL,
                shift_id      VARCHAR2(50) NOT NULL,
                work_date     DATE NOT NULL,
                status        VARCHAR2(20) DEFAULT ''ASSIGNED'' NOT NULL,
                note          NVARCHAR2(500),
                is_deleted    NUMBER(1) DEFAULT 0,
                created_at    TIMESTAMP DEFAULT SYSTIMESTAMP,
                updated_at    TIMESTAMP,
                CONSTRAINT FK_EMP_SHIFT_EMP FOREIGN KEY (employee_id) REFERENCES EMPLOYEES(employee_id),
                CONSTRAINT FK_EMP_SHIFT_SHIFT FOREIGN KEY (shift_id) REFERENCES SHIFTS(shift_id),
                CONSTRAINT CK_EMP_SHIFT_STATUS CHECK (status IN (''ASSIGNED'', ''COMPLETED'', ''CANCELED''))
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
    WHERE index_name = 'UQ_EMP_SHIFT_ACTIVE';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE '
            CREATE UNIQUE INDEX UQ_EMP_SHIFT_ACTIVE
            ON EMPLOYEE_SHIFTS (
                employee_id,
                shift_id,
                TRUNC(work_date),
                CASE WHEN NVL(is_deleted, 0) = 0 AND NVL(status, ''ASSIGNED'') <> ''CANCELED'' THEN 1 ELSE NULL END
            )
        ';
    END IF;
END;
/

MERGE INTO FUNCTIONS f
USING (SELECT 'F_LOCAL_DEV' function_id, 'Local development access' function_name FROM dual) src
ON (f.function_id = src.function_id)
WHEN MATCHED THEN UPDATE SET f.function_name = src.function_name, f.is_deleted = 0
WHEN NOT MATCHED THEN INSERT (function_id, function_name, is_deleted)
VALUES (src.function_id, src.function_name, 0);

MERGE INTO ROLES r
USING (
    SELECT 'R_STAFF_SALE' role_id, 'Sales Staff' role_name, 'F_LOCAL_DEV' function_id,
           1 can_view, 1 can_add, 0 can_edit, 0 can_delete, 1 can_export FROM dual
    UNION ALL
    SELECT 'R_STAFF_VIEW_PROD', 'Warehouse Staff', 'F_LOCAL_DEV',
           1, 1, 1, 0, 1 FROM dual
) src
ON (r.role_id = src.role_id)
WHEN MATCHED THEN UPDATE SET
    r.role_name = src.role_name,
    r.function_id = src.function_id,
    r.can_view = src.can_view,
    r.can_add = src.can_add,
    r.can_edit = src.can_edit,
    r.can_delete = src.can_delete,
    r.can_export = src.can_export,
    r.is_deleted = 0
WHEN NOT MATCHED THEN INSERT (
    role_id, role_name, function_id, can_view, can_add, can_edit, can_delete, can_export, is_deleted
) VALUES (
    src.role_id, src.role_name, src.function_id, src.can_view, src.can_add, src.can_edit,
    src.can_delete, src.can_export, 0
);

MERGE INTO EMPLOYEES e
USING (
    SELECT 'EMP_LOCAL_SALE' employee_id, 'Local Sale Staff' employee_name, '0900000003' phone, 'sale@local.dev' email, 'R_STAFF_SALE' role_id FROM dual
    UNION ALL
    SELECT 'EMP_LOCAL_CASHIER', 'Local Cashier Staff', '0900000005', 'cashier@local.dev', 'R_STAFF_SALE' FROM dual
    UNION ALL
    SELECT 'EMP_LOCAL_SALE_2', 'Nguyen Thi Lan', '0900000006', 'lan.sale@local.dev', 'R_STAFF_SALE' FROM dual
    UNION ALL
    SELECT 'EMP_LOCAL_WAREHOUSE', 'Local Warehouse Staff', '0900000004', 'warehouse@local.dev', 'R_STAFF_VIEW_PROD' FROM dual
    UNION ALL
    SELECT 'EMP_LOCAL_STOCK_2', 'Tran Van Kho', '0900000007', 'kho@local.dev', 'R_STAFF_VIEW_PROD' FROM dual
) src
ON (e.employee_id = src.employee_id)
WHEN MATCHED THEN UPDATE SET
    e.employee_name = src.employee_name,
    e.phone = src.phone,
    e.email = src.email,
    e.role_id = src.role_id,
    e.salary_coefficient = 1.0,
    e.is_deleted = 0
WHEN NOT MATCHED THEN INSERT (
    employee_id, employee_name, hire_date, phone, email, salary_coefficient, role_id, is_deleted
) VALUES (
    src.employee_id, src.employee_name, DATE '2026-01-01', src.phone, src.email, 1.0, src.role_id, 0
);

MERGE INTO SHIFTS s
USING (
    SELECT 'SHIFT_MORNING' shift_id, N'Ca sáng' shift_name, '07:00' start_text, '15:00' end_text FROM dual
    UNION ALL
    SELECT 'SHIFT_AFTERNOON', N'Ca chiều', '15:00', '23:00' FROM dual
    UNION ALL
    SELECT 'SHIFT_NIGHT', N'Ca tối', '23:00', '07:00' FROM dual
) src
ON (s.shift_id = src.shift_id)
WHEN MATCHED THEN UPDATE SET
    s.shift_name = src.shift_name,
    s.start_time = TO_DATE(src.start_text, 'HH24:MI'),
    s.end_time = TO_DATE(src.end_text, 'HH24:MI'),
    s.is_deleted = 0
WHEN NOT MATCHED THEN INSERT (shift_id, shift_name, start_time, end_time, is_deleted)
VALUES (src.shift_id, src.shift_name, TO_DATE(src.start_text, 'HH24:MI'), TO_DATE(src.end_text, 'HH24:MI'), 0);

MERGE INTO EMPLOYEE_SHIFTS es
USING (
    SELECT 'PC_LOCAL_001' assignment_id, 'EMP_LOCAL_SALE' employee_id, 'SHIFT_MORNING' shift_id,
           TRUNC(SYSDATE) work_date, 'ASSIGNED' status, N'Bán hàng khu thực phẩm tươi' note FROM dual
    UNION ALL
    SELECT 'PC_LOCAL_002', 'EMP_LOCAL_CASHIER', 'SHIFT_AFTERNOON',
           TRUNC(SYSDATE), 'ASSIGNED', N'Thu ngân quầy 2' FROM dual
    UNION ALL
    SELECT 'PC_LOCAL_003', 'EMP_LOCAL_SALE_2', 'SHIFT_NIGHT',
           TRUNC(SYSDATE), 'CANCELED', N'Nghỉ phép' FROM dual
    UNION ALL
    SELECT 'PC_LOCAL_004', 'EMP_LOCAL_WAREHOUSE', 'SHIFT_MORNING',
           TRUNC(SYSDATE), 'COMPLETED', N'Kiểm kê kho đầu ngày' FROM dual
    UNION ALL
    SELECT 'PC_LOCAL_005', 'EMP_LOCAL_STOCK_2', 'SHIFT_AFTERNOON',
           TRUNC(SYSDATE), 'ASSIGNED', N'Nhập hàng và sắp xếp kệ' FROM dual
    UNION ALL
    SELECT 'PC_LOCAL_006', 'EMP_LOCAL_SALE', 'SHIFT_AFTERNOON',
           TRUNC(SYSDATE) + 1, 'ASSIGNED', N'Thu ngân quầy 1' FROM dual
    UNION ALL
    SELECT 'PC_LOCAL_007', 'EMP_LOCAL_WAREHOUSE', 'SHIFT_NIGHT',
           TRUNC(SYSDATE) + 1, 'ASSIGNED', N'Trực kho ca tối' FROM dual
    UNION ALL
    SELECT 'PC_LOCAL_008', 'EMP_LOCAL_CASHIER', 'SHIFT_MORNING',
           TRUNC(SYSDATE) - 1, 'COMPLETED', N'Ca sáng đã hoàn tất' FROM dual
) src
ON (es.assignment_id = src.assignment_id)
WHEN MATCHED THEN UPDATE SET
    es.employee_id = src.employee_id,
    es.shift_id = src.shift_id,
    es.work_date = src.work_date,
    es.status = src.status,
    es.note = src.note,
    es.is_deleted = 0,
    es.updated_at = SYSTIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    assignment_id, employee_id, shift_id, work_date, status, note, is_deleted
) VALUES (
    src.assignment_id, src.employee_id, src.shift_id, src.work_date, src.status, src.note, 0
);

COMMIT;
