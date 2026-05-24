-- ============================================================
-- 4.3.3.3. FUNC_GET_FINAL_SYSTEM_REVENUE
-- Chức năng: Tính doanh thu cuối cùng của toàn bộ hệ thống
-- Schema chạy: HQT_DEMO
-- ============================================================
--
-- Nghiệp vụ:
--   Function dùng để tổng hợp doanh thu cuối cùng của toàn bộ hệ thống
--   trong một khoảng thời gian.
--
-- Công thức:
--   Doanh thu cuối cùng = Tổng doanh thu bán hàng - Tổng chi phí nhập hàng
--
-- Trong đồ án:
--   1. Bán hàng KHÔNG có VAT riêng.
--      Doanh thu bán hàng lấy trực tiếp từ ORDERS.TOTAL_AMOUNT.
--      Cột này được hiểu là số tiền cuối cùng của đơn hàng sau khi đã áp dụng:
--          - Giảm giá thành viên
--          - Giảm giá chương trình khuyến mãi
--
--   2. Nhập hàng CÓ VAT.
--      Chi phí nhập hàng lấy từ PURCHASE_RECEIPTS.TOTAL_AFTER_TAX.
--      Đây là tổng tiền nhập hàng sau thuế.
--
-- Giá trị trả về:
--   - Trả về NUMBER là doanh thu cuối cùng của toàn hệ thống.
--   - Nếu không có dữ liệu thì trả về 0.
--   - Nếu có lỗi hệ thống thì trả về -1.
--
-- Tham số:
--   p_from_date: ngày bắt đầu thống kê.
--   p_to_date  : ngày kết thúc thống kê.
--
-- Nếu truyền NULL hoặc không truyền tham số:
--   Function sẽ tính trên toàn bộ dữ liệu hiện có.
-- ============================================================

CREATE OR REPLACE FUNCTION FUNC_GET_FINAL_SYSTEM_REVENUE (
    p_from_date IN DATE DEFAULT NULL,
    p_to_date   IN DATE DEFAULT NULL
) RETURN NUMBER
IS
    -- Lưu tổng doanh thu bán hàng từ bảng ORDERS
    v_total_sales   NUMBER := 0;

    -- Lưu tổng chi phí nhập hàng từ bảng PURCHASE_RECEIPTS
    v_total_import  NUMBER := 0;

    -- Lưu kết quả doanh thu cuối cùng
    v_final_revenue NUMBER := 0;
BEGIN
    -- ========================================================
    -- BƯỚC 1: TÍNH TỔNG DOANH THU BÁN HÀNG
    -- ========================================================
    --
    -- Lấy tổng ORDERS.TOTAL_AMOUNT của các đơn hàng hợp lệ.
    --
    -- Điều kiện:
    --   - Đơn hàng chưa bị xóa mềm: IS_DELETED = 0
    --   - Trạng thái đơn hàng là hoàn thành/thành công/đã thanh toán
    --   - Nếu có truyền ngày thì lọc theo ORDER_DATE
    --
    -- Lưu ý:
    --   Bán hàng không cộng VAT riêng vì TOTAL_AMOUNT đã là số tiền cuối cùng
    --   sau khi áp dụng giảm giá thành viên và khuyến mãi.
    -- ========================================================

    SELECT NVL(SUM(o.total_amount), 0)
    INTO v_total_sales
    FROM ORDERS o
    WHERE NVL(o.is_deleted, 0) = 0
      AND (
            UPPER(NVL(o.status, '')) = 'COMPLETED'
            OR UPPER(NVL(o.status, '')) = 'SUCCESS'
            OR UPPER(NVL(o.status, '')) = 'PAID'
            OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
            OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
            OR UPPER(NVL(o.status, '')) LIKE '%ĐÃ THANH TOÁN%'
            OR UPPER(NVL(o.status, '')) LIKE '%DA THANH TOAN%'
          )
      -- Nếu p_from_date NULL thì bỏ qua điều kiện ngày bắt đầu
      AND (p_from_date IS NULL OR o.order_date >= p_from_date)

      -- Dùng < p_to_date + 1 để lấy trọn ngày kết thúc,
      -- kể cả dữ liệu có giờ/phút/giây trong ngày đó.
      AND (p_to_date IS NULL OR o.order_date < p_to_date + 1);


    -- ========================================================
    -- BƯỚC 2: TÍNH TỔNG CHI PHÍ NHẬP HÀNG
    -- ========================================================
    --
    -- Lấy tổng PURCHASE_RECEIPTS.TOTAL_AFTER_TAX.
    --
    -- Điều kiện:
    --   - Phiếu nhập chưa bị xóa mềm: IS_DELETED = 0
    --   - Nếu có truyền ngày thì lọc theo CREATED_AT
    --
    -- Lưu ý:
    --   CREATED_AT là TIMESTAMP, còn p_from_date/p_to_date là DATE,
    --   nên cần CAST DATE sang TIMESTAMP để so sánh chính xác.
    -- ========================================================

    SELECT NVL(SUM(pr.total_after_tax), 0)
    INTO v_total_import
    FROM PURCHASE_RECEIPTS pr
    WHERE NVL(pr.is_deleted, 0) = 0
      -- Nếu p_from_date NULL thì bỏ qua điều kiện ngày bắt đầu
      AND (p_from_date IS NULL OR pr.created_at >= CAST(p_from_date AS TIMESTAMP))

      -- Dùng < p_to_date + 1 để lấy trọn ngày kết thúc
      AND (p_to_date IS NULL OR pr.created_at < CAST(p_to_date + 1 AS TIMESTAMP));


    -- ========================================================
    -- BƯỚC 3: TÍNH DOANH THU CUỐI CÙNG
    -- ========================================================
    --
    -- Công thức:
    --   Doanh thu cuối cùng = Tổng bán hàng - Tổng nhập hàng
    --
    -- Nếu kết quả âm:
    --   Nghĩa là trong khoảng thời gian đó, chi phí nhập hàng lớn hơn
    --   doanh thu bán hàng. Đây không phải lỗi.
    -- ========================================================

    v_final_revenue := v_total_sales - v_total_import;


    -- ========================================================
    -- BƯỚC 4: TRẢ KẾT QUẢ
    -- ========================================================
    --
    -- Làm tròn 2 chữ số thập phân trước khi trả về.
    -- ========================================================

    RETURN ROUND(v_final_revenue, 2);


-- ============================================================
-- XỬ LÝ NGOẠI LỆ
-- ============================================================
--
-- Nếu có lỗi hệ thống như:
--   - Sai tên bảng
--   - Sai tên cột
--   - Thiếu quyền truy cập
--   - Lỗi dữ liệu bất thường
--
-- Function trả về -1 để Java hoặc người dùng biết là function bị lỗi.
-- ============================================================

EXCEPTION
    WHEN OTHERS THEN
        RETURN -1;
END FUNC_GET_FINAL_SYSTEM_REVENUE;
/

-- Muc test he thong
SELECT FUNC_GET_FINAL_SYSTEM_REVENUE() AS doanh_thu_cuoi_cung
FROM dual;

SELECT FUNC_GET_FINAL_SYSTEM_REVENUE(
    DATE '2026-05-01',
    DATE '2026-05-31'
) AS doanh_thu_cuoi_cung
FROM dual;