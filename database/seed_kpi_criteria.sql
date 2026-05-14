-- ==========================================================
-- SEED DỮ LIỆU CÁC TIÊU CHÍ ĐÁNH GIÁ KPI (KPI_CRITERIA)
-- Chạy script này một lần để nạp bộ tiêu chí KPI mặc định cho nhân viên SALES
-- ==========================================================

-- Xóa các tiêu chí cũ nếu có (tuỳ chọn - bỏ dòng này nếu muốn giữ dữ liệu cũ)
-- DELETE FROM KPI_CRITERIA WHERE kpi_id IN ('KPI_ORDERS', 'KPI_REVENUE', 'KPI_COMPLETION', 'KPI_DELIVERY', 'KPI_ATTENDANCE');

-- ==========================================================
-- 1. Tiêu chí Số đơn hoàn thành (Trọng số 25% - 0.25, Target: 50 đơn)
-- ==========================================================
INSERT INTO KPI_CRITERIA (kpi_id, criteria_name, criteria_type, weight, recorded_time, minimum_target, is_deleted)
VALUES ('KPI_ORDERS', N'Số đơn hoàn thành', 'SALES', 0.25, SYSTIMESTAMP, 50, 0);

-- ==========================================================
-- 2. Tiêu chí Doanh thu (Trọng số 25% - 0.25, Target: 10 triệu)
-- ==========================================================
INSERT INTO KPI_CRITERIA (kpi_id, criteria_name, criteria_type, weight, recorded_time, minimum_target, is_deleted)
VALUES ('KPI_REVENUE', N'Doanh thu', 'SALES', 0.25, SYSTIMESTAMP, 10000000, 0);

-- ==========================================================
-- 3. Tiêu chí Tỷ lệ hoàn thành (Trọng số 20% - 0.20, Target: 90%)
-- ==========================================================
INSERT INTO KPI_CRITERIA (kpi_id, criteria_name, criteria_type, weight, recorded_time, minimum_target, is_deleted)
VALUES ('KPI_COMPLETION', N'Tỷ lệ hoàn thành', 'SERVICE', 0.20, SYSTIMESTAMP, 90, 0);

-- ==========================================================
-- 4. Tiêu chí Tỷ lệ giao hàng thành công (Trọng số 15% - 0.15, Target: 90%)
-- ==========================================================
INSERT INTO KPI_CRITERIA (kpi_id, criteria_name, criteria_type, weight, recorded_time, minimum_target, is_deleted)
VALUES ('KPI_DELIVERY', N'Tỷ lệ giao hàng', 'SERVICE', 0.15, SYSTIMESTAMP, 90, 0);

-- ==========================================================
-- 5. Tiêu chí Điểm chuyên cần (Trọng số 15% - 0.15, Target: 8/10)
-- ==========================================================
INSERT INTO KPI_CRITERIA (kpi_id, criteria_name, criteria_type, weight, recorded_time, minimum_target, is_deleted)
VALUES ('KPI_ATTENDANCE', N'Điểm chuyên cần', 'ATTENDANCE', 0.15, SYSTIMESTAMP, 8, 0);

-- ==========================================================
-- LƯU LẠI
-- ==========================================================
COMMIT;

-- Xác nhận dữ liệu đã được nạp
SELECT * FROM KPI_CRITERIA WHERE NVL(is_deleted, 0) = 0 ORDER BY kpi_id;
