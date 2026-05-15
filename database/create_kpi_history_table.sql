-- Script tạo bảng EMPLOYEE_KPI_HISTORY để lưu lịch sử dữ liệu KPI
-- Chạy script này trên database Oracle nếu bạn muốn lưu trữ lịch sử import KPI

CREATE TABLE EMPLOYEE_KPI_HISTORY (
    kpi_history_id       NUMBER PRIMARY KEY,
    employee_id          VARCHAR2(50) NOT NULL REFERENCES EMPLOYEES(employee_id),
    total_orders         NUMBER DEFAULT 0,
    revenue              NUMBER(15,2) DEFAULT 0,
    completion_rate      NUMBER(5,2) DEFAULT 0,     -- Tỷ lệ hoàn thành (%)
    delivery_success_rate NUMBER(5,2) DEFAULT 0,    -- Tỷ lệ giao hàng thành công (%)
    attendance_score     NUMBER(5,2) DEFAULT 0,     -- Điểm chuyên cần (0-10)
    performance_score    NUMBER(5,2) DEFAULT 0,     -- Điểm KPI tổng hợp
    import_date          DATE DEFAULT SYSDATE,
    created_at           DATE DEFAULT SYSDATE,
    updated_at           DATE DEFAULT SYSDATE,
    is_deleted           NUMBER(1) DEFAULT 0
);

-- Tạo sequence cho primary key
CREATE SEQUENCE seq_kpi_history_id
    START WITH 1
    INCREMENT BY 1
    NOCACHE;

-- Tạo trigger tự động gán ID
CREATE OR REPLACE TRIGGER trg_kpi_history_id
BEFORE INSERT ON EMPLOYEE_KPI_HISTORY
FOR EACH ROW
BEGIN
    IF :NEW.kpi_history_id IS NULL THEN
        SELECT seq_kpi_history_id.NEXTVAL INTO :NEW.kpi_history_id FROM DUAL;
    END IF;
END;
/

-- Tạo index để tối ưu query
CREATE INDEX idx_kpi_employee_id ON EMPLOYEE_KPI_HISTORY(employee_id);
CREATE INDEX idx_kpi_import_date ON EMPLOYEE_KPI_HISTORY(import_date);

-- Tạo view để lấy dữ liệu KPI mới nhất cho mỗi nhân viên
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
LEFT JOIN latest_kpi_history lkh ON e.employee_id = lkh.employee_id
LEFT JOIN EMPLOYEE_KPI_HISTORY kh ON kh.kpi_history_id = lkh.kpi_history_id
WHERE NVL(e.is_deleted, 0) = 0;

-- Query để lấy dữ liệu KPI trong khoảng thời gian
-- SELECT * FROM EMPLOYEE_KPI_HISTORY 
-- WHERE import_date BETWEEN TO_DATE('01/01/2024', 'DD/MM/YYYY') AND TO_DATE('31/12/2024', 'DD/MM/YYYY')
-- AND NVL(is_deleted, 0) = 0
-- ORDER BY import_date DESC, employee_id;

COMMIT;
