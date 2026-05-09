-- ==========================================
-- SCRIPT ĐỒNG BỘ DỮ LIỆU LÕI (BASE DATA)
-- Dành cho Team chạy trước khi Import 1 triệu dòng
-- ==========================================

-- 1. TẠO CỬA HÀNG (STORES)
INSERT INTO STORES (store_id, store_name, address, is_deleted) 
VALUES ('ST001', N'Siêu thị Trung tâm UIT', N'Làng Đại học Thủ Đức', 0);

-- 2. TẠO NHÀ CUNG CẤP CHUẨN (SUPPLIERS)
INSERT INTO SUPPLIERS (supplier_id, supplier_name, is_deleted) VALUES ('SUP001', N'Công ty CP Hàng Tiêu Dùng Masan', 0);
INSERT INTO SUPPLIERS (supplier_id, supplier_name, is_deleted) VALUES ('SUP002', N'Tập đoàn Suntory PepsiCo', 0);
INSERT INTO SUPPLIERS (supplier_id, supplier_name, is_deleted) VALUES ('SUP003', N'Unilever Việt Nam', 0);
INSERT INTO SUPPLIERS (supplier_id, supplier_name, is_deleted) VALUES ('SUP004', N'Vinafood 1', 0);

-- 3. TẠO DANH MỤC THỰC TẾ (CATEGORIES) - Đúng scope của Vĩ
INSERT INTO CATEGORIES (category_id, category_name, description, is_deleted) 
VALUES ('CAT001', N'Thực phẩm khô', N'Gạo, mì, đường, gia vị', 0);

INSERT INTO CATEGORIES (category_id, category_name, description, is_deleted) 
VALUES ('CAT002', N'Đồ uống & Giải khát', N'Nước suối, nước ngọt, trà', 0);

INSERT INTO CATEGORIES (category_id, category_name, description, is_deleted) 
VALUES ('CAT003', N'Hàng tiêu dùng cá nhân', N'Dầu gội, sữa tắm, kem đánh răng', 0);

-- CHỐT SỔ (BẮT BUỘC)
COMMIT;

-- ==========================================================
-- 4. TẠO PHƯƠNG THỨC THANH TOÁN (PAYMENT_METHODS)
-- Bắt buộc phải có để tạo Hóa đơn
-- ==========================================================
INSERT INTO PAYMENT_METHODS (payment_method_id, is_deleted) VALUES ('PM_CASH', 0);
INSERT INTO PAYMENT_METHODS (payment_method_id, is_deleted) VALUES ('PM_TRANSFER', 0);

INSERT INTO CASH_PAYMENT (payment_method_id, is_deleted) VALUES ('PM_CASH', 0);
INSERT INTO BANK_TRANSFER_PAYMENT (payment_method_id, bank_name, is_deleted) VALUES ('PM_TRANSFER', 'Vietcombank', 0);

-- ==========================================================
-- 5. TẠO NHÂN VIÊN MẪU (EMPLOYEES)
-- Bắt buộc phải có để gán cho Hóa đơn (employee_id)
-- ==========================================================
-- Lưu ý: role_id và shift_id đang bỏ trống (NULL) để đơn giản hóa, 
-- nếu bảng EMPLOYEES của ông bắt buộc có thì phải INSERT thêm SHIFTS và ROLES tương ứng.
INSERT INTO EMPLOYEES (employee_id, employee_name, hire_date, salary_coefficient, is_deleted) 
VALUES ('EMP001', N'Nhân viên Quỳnh', TO_DATE('2024-01-01', 'YYYY-MM-DD'), 1.0, 0);

-- ==========================================================
-- 6. TẠO KHÁCH HÀNG (CUSTOMERS)
-- ==========================================================
INSERT INTO CUSTOMERS (customer_id, customer_name, reward_points, is_deleted) 
VALUES ('CUST001', N'Nguyễn Văn A', 150, 0);

INSERT INTO CUSTOMERS (customer_id, customer_name, reward_points, is_deleted) 
VALUES ('CUST002', N'Trần Thị B', 500, 0);

INSERT INTO CUSTOMERS (customer_id, customer_name, reward_points, is_deleted) 
VALUES ('CUST003', N'Lê Tấn Vĩ', 1200, 0);

-- ==========================================================
-- 7. TẠO HÓA ĐƠN MUA HÀNG (ORDERS)
-- (Khách mua bao nhiêu tiền là nằm ở đây)
-- ==========================================================
-- Đơn hàng 1: Khách A mua 150,000 VND
INSERT INTO ORDERS (order_id, customer_id, payment_method_id, order_date, status, total_amount, employee_id, is_deleted) 
VALUES ('ORD_001', 'CUST001', 'PM_CASH', SYSDATE - 2, N'Hoàn thành', 150000, 'EMP001', 0);

-- Đơn hàng 2: Khách B mua 500,000 VND
INSERT INTO ORDERS (order_id, customer_id, payment_method_id, order_date, status, total_amount, employee_id, is_deleted) 
VALUES ('ORD_002', 'CUST002', 'PM_TRANSFER', SYSDATE - 1, N'Hoàn thành', 500000, 'EMP001', 0);

-- Đơn hàng 3: Khách Vĩ mua VIP (1,200,000 VND)
INSERT INTO ORDERS (order_id, customer_id, payment_method_id, order_date, status, total_amount, employee_id, is_deleted) 
VALUES ('ORD_003', 'CUST003', 'PM_TRANSFER', SYSDATE, N'Hoàn thành', 1200000, 'EMP001', 0);

-- ==========================================================
-- 8. TẠO CHI TIẾT HÓA ĐƠN (ORDER_DETAILS) 
-- Giả sử đã có sản phẩm SP0000001, SP0000002 trong DB
-- ==========================================================
-- Khách A mua 2 món
INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_price, is_deleted) 
VALUES ('OD_001', 'ORD_001', 'SP0000001', 2, 50000, 0);
INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_price, is_deleted) 
VALUES ('OD_002', 'ORD_001', 'SP0000002', 1, 50000, 0);

-- Chốt dữ liệu xuống Oracle
COMMIT;

-- Cập nhật cho Khách hàng A
UPDATE CUSTOMERS 
SET email = 'nva@gmail.com', 
    phone = '0901111222', 
    address = N'123 Đường 3/2, Quận 10, TP.HCM' 
WHERE customer_id = 'CUST001';

-- Cập nhật cho Khách hàng B
UPDATE CUSTOMERS 
SET email = 'tranthib@uit.edu.vn', 
    phone = '0903333444', 
    address = N'Ký túc xá khu B ĐHQG, Dĩ An, Bình Dương' 
WHERE customer_id = 'CUST002';

-- Cập nhật cho Khách hàng Vĩ (Khách VIP)
UPDATE CUSTOMERS 
SET email = 'tanvi3001@gmail.com', 
    phone = '0988999888', 
    address = N'Ký túc xá khu A ĐHQG, Dĩ An, Bình Dương' 
WHERE customer_id = 'CUST003';

-- Bắt buộc phải COMMIT để lưu vĩnh viễn xuống ổ cứng nhé
COMMIT;

SELECT 
    c.customer_id, 
    c.customer_name, 
    c.phone, 
    c.email, 
    c.address,
    NVL(SUM(o.total_amount), 0) AS total_spending,
    CASE 
        WHEN NVL(SUM(o.total_amount), 0) >= 50000000 THEN N'Kim cương'
        WHEN NVL(SUM(o.total_amount), 0) >= 20000000 THEN N'Vàng'
        WHEN NVL(SUM(o.total_amount), 0) >= 5000000 THEN N'Bạc'
        ELSE N'Đồng' 
    END AS member_rank
FROM CUSTOMERS c
LEFT JOIN ORDERS o ON c.customer_id = o.customer_id AND o.is_deleted = 0
WHERE c.is_deleted = 0
GROUP BY c.customer_id, c.customer_name, c.phone, c.email, c.address;
-- Thêm rank cho customer --


-- 
-- Đổi các trạng thái đã active thành "Đã cấp"
UPDATE ACCOUNTS 
SET status = N'Đã cấp' 
WHERE UPPER(status) IN ('HOẠT ĐỘNG', 'ACTIVE', 'COMPLETED');

-- Đổi các trạng thái chưa active thành "Chưa cấp"
UPDATE ACCOUNTS 
SET status = N'Chưa cấp' 
WHERE UPPER(status) IN ('CHỜ KÍCH HOẠT', 'PENDING');

COMMIT;
