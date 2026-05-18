-- ==========================================================
-- EMPLOYEE_KPI_HISTORY
-- Lưu lịch sử dữ liệu KPI nhân viên
-- Safe script: chạy nhiều lần không lỗi ORA-00955
-- ==========================================================

SET SERVEROUTPUT ON;

-- ==========================================================
-- 1. CREATE TABLE EMPLOYEE_KPI_HISTORY
-- ==========================================================

BEGIN
    EXECUTE IMMEDIATE '
        CREATE TABLE EMPLOYEE_KPI_HISTORY (
            kpi_history_id        NUMBER PRIMARY KEY,
            employee_id           VARCHAR2(50) NOT NULL,
            total_orders          NUMBER DEFAULT 0,
            revenue               NUMBER(15,2) DEFAULT 0,
            completion_rate       NUMBER(5,2) DEFAULT 0,
            delivery_success_rate NUMBER(5,2) DEFAULT 0,
            attendance_score      NUMBER(5,2) DEFAULT 0,
            performance_score     NUMBER(5,2) DEFAULT 0,
            import_date           DATE DEFAULT SYSDATE,
            created_at            DATE DEFAULT SYSDATE,
            updated_at            DATE DEFAULT SYSDATE,
            is_deleted            NUMBER(1) DEFAULT 0,
            CONSTRAINT fk_kpi_history_employee
                FOREIGN KEY (employee_id)
                REFERENCES EMPLOYEES(employee_id)
        )
    ';

    DBMS_OUTPUT.PUT_LINE('Created table EMPLOYEE_KPI_HISTORY.');

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('Table EMPLOYEE_KPI_HISTORY already exists. Skip create table.');
        ELSE
            RAISE;
        END IF;
END;
/

-- ==========================================================
-- 2. CREATE SEQUENCE
-- ==========================================================

BEGIN
    EXECUTE IMMEDIATE '
        CREATE SEQUENCE seq_kpi_history_id
            START WITH 1
            INCREMENT BY 1
            NOCACHE
    ';

    DBMS_OUTPUT.PUT_LINE('Created sequence seq_kpi_history_id.');

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('Sequence seq_kpi_history_id already exists. Skip create sequence.');
        ELSE
            RAISE;
        END IF;
END;
/

-- ==========================================================
-- 3. CREATE OR REPLACE TRIGGER
-- ==========================================================

CREATE OR REPLACE TRIGGER trg_kpi_history_id
BEFORE INSERT ON EMPLOYEE_KPI_HISTORY
FOR EACH ROW
BEGIN
    IF :NEW.kpi_history_id IS NULL THEN
        SELECT seq_kpi_history_id.NEXTVAL
        INTO :NEW.kpi_history_id
        FROM DUAL;
    END IF;
END;
/

-- ==========================================================
-- 4. CREATE INDEXES
-- ==========================================================

BEGIN
    EXECUTE IMMEDIATE '
        CREATE INDEX idx_kpi_employee_id
        ON EMPLOYEE_KPI_HISTORY(employee_id)
    ';

    DBMS_OUTPUT.PUT_LINE('Created index idx_kpi_employee_id.');

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('Index idx_kpi_employee_id already exists. Skip create index.');
        ELSE
            RAISE;
        END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE '
        CREATE INDEX idx_kpi_import_date
        ON EMPLOYEE_KPI_HISTORY(import_date)
    ';

    DBMS_OUTPUT.PUT_LINE('Created index idx_kpi_import_date.');

EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE = -955 THEN
            DBMS_OUTPUT.PUT_LINE('Index idx_kpi_import_date already exists. Skip create index.');
        ELSE
            RAISE;
        END IF;
END;
/

-- ==========================================================
-- 5. CREATE OR REPLACE VIEW
-- ==========================================================

CREATE OR REPLACE VIEW v_latest_kpi AS
WITH latest_kpi_history AS (
    SELECT
        employee_id,
        MAX(kpi_history_id) AS kpi_history_id
    FROM EMPLOYEE_KPI_HISTORY
    WHERE NVL(is_deleted, 0) = 0
    GROUP BY employee_id
)
SELECT
    e.employee_id,
    e.employee_name,
    kh.total_orders,
    kh.revenue,
    kh.completion_rate,
    kh.delivery_success_rate,
    kh.attendance_score,
    kh.performance_score,
    kh.import_date
FROM EMPLOYEES e
LEFT JOIN latest_kpi_history lkh
    ON e.employee_id = lkh.employee_id
LEFT JOIN EMPLOYEE_KPI_HISTORY kh
    ON kh.kpi_history_id = lkh.kpi_history_id
WHERE NVL(e.is_deleted, 0) = 0;

COMMIT;

PROMPT DONE: EMPLOYEE_KPI_HISTORY script completed.