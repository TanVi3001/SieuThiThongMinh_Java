-- ==========================================
-- SCRIPT ĐỒNG BỘ DỮ LIỆU LÕI (BASE DATA)
-- Dành cho Team chạy trước khi Import 1 triệu dòng
-- ==========================================

-- 1. TẠO CỬA HÀNG (STORES)
-- 2. Thêm Cửa hàng mặc định (Để không bị lỗi khóa ngoại khi nạp vào KHO)
INSERT INTO STORES (store_id, email, address, phone_number)
VALUES ('ST01', 'contact@smartmart.vn', 'Dĩ An, Bình Dương', '0123456789');

-- 2. TẠO NHÀ CUNG CẤP CHUẨN (SUPPLIERS)
INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_02', 'Công ty TNHH Acecook Việt Nam', 'acecook@acecook.vn', '36 Tân Thắng, Bình Tân, TP.HCM', '02837608888', 0);
-- Sản phẩm: Mì Hảo Hảo, Mì Omachi

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_03', 'Công ty TNHH Vifon', 'vifon@vifon.com.vn', '28 Đồng Nai, Quận 10, TP.HCM', '02838333888', 0);
-- Sản phẩm: Phở bò Vifon

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_04', 'Công ty CP Masan Consumer', 'masan@masan.com.vn', '12 Tân Trào, Tân Phú, TP.HCM', '02873008888', 0);
-- Sản phẩm: Nước mắm Nam Ngư, Nước mắm Chinsu, Tương ớt Chinsu, Dầu Simply

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_05', 'Công ty CP Dầu thực vật Tường An', 'tuongan@tuongan.com.vn', '48 Trường Sơn, Tân Bình, TP.HCM', '02838111333', 0);
-- Sản phẩm: Dầu ăn Tường An

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_06', 'Công ty CP Sữa Việt Nam (Vinamilk)', 'vinamilk@vinamilk.com.vn', '10 Tân Trào, Quận 7, TP.HCM', '18001557', 0);
-- Sản phẩm: Sữa đặc Ông Thọ, Đường Biên Hòa

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_07', 'Công ty TNHH Unilever Việt Nam', 'unilever@unilever.com', 'KCN Biên Hòa 2, Đồng Nai', '02513836333', 0);
-- Sản phẩm: Dầu gội Clear, Sunsilk, Kem đánh răng PS, Sữa tắm Lifebuoy, Hazeline

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_08', 'Công ty TNHH Nestlé Việt Nam', 'nestle@nestle.com.vn', 'KCN Biên Hòa 1, Đồng Nai', '02513836111', 0);
-- Sản phẩm: Hạt nêm Knorr, Bánh Oreo, KitKat, Milo

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_09', 'Công ty TNHH Coca-Cola Việt Nam', 'cocacola@coca-cola.com.vn', 'KCN Tam Bình, Bình Dương', '02743820222', 0);
-- Sản phẩm: Coca Cola, Sprite, Aquafina

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_10', 'Công ty CP Pepsico Việt Nam', 'pepsico@pepsico.com.vn', 'KCN Việt Nam Singapore, Bình Dương', '02743750888', 0);
-- Sản phẩm: Pepsi, Sting, 7Up, Pringles

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_11', 'Công ty CP Tân Hiệp Phát', 'thp@thp.com.vn', 'KCN Mỹ Phước, Bình Dương', '02743771222', 0);
-- Sản phẩm: Trà Ô Long Tea Plus, Redbull (phân phối)

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_12', 'Công ty CP Orion Vina', 'orion@orion.vn', 'KCN Yên Phong, Bắc Ninh', '02223871999', 0);
-- Sản phẩm: ChocoPie Orion, Bánh gạo One One

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_13', 'Công ty CP Kinh Đô (Kido)', 'kido@kidobrands.com', '141 Nguyễn Du, Quận 1, TP.HCM', '02839326262', 0);
-- Sản phẩm: Bánh Cosy

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_14', 'Công ty TNHH Haribo GmbH (Phân phối)', 'haribo@haribo.com.vn', '12 Lê Duẩn, Quận 1, TP.HCM', '02838221111', 0);
-- Sản phẩm: Kẹo dẻo Haribo

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
VALUES ('SUP_15', 'Công ty TNHH Thương mại Thực phẩm Tươi Sống Sài Gòn', 'tuoisong@sgfresh.com.vn', '50 Lý Thường Kiệt, Quận 10, TP.HCM', '02838556789', 0);
-- Sản phẩm: Thịt bò, ức gà, cá hồi, trứng gà, khoai tây

COMMIT;
-- 3. Thêm Nhà cung cấp mặc định (Để code Java có chỗ bám vào)
INSERT INTO SUPPLIERS (supplier_id, supplier_name)
VALUES ('SUP_01', 'Nhà cung cấp Tổng hợp');

-- 4. Thêm Đơn vị tính mặc định
INSERT INTO UNITS (unit_id, unit_name)
VALUES ('UN_01', 'Đơn vị tiêu chuẩn');

-- 5. LƯU LẠI
COMMIT;


-- 3. TẠO DANH MỤC THỰC TẾ (CATEGORIES) - Đúng scope của Vĩ
-- ==========================================================
-- RẢI DỮ LIỆU GỐC (CHUẨN THEO ẢNH CỦA BẠN)
-- ==========================================================

-- 1. Thêm 5 Danh mục (Chuẩn 100% theo ảnh)
INSERT INTO CATEGORIES (category_id, category_name, description)
VALUES ('CAT001', 'Các loại thực phẩm khô', 'Cung cấp các loại thực phẩm khô');

INSERT INTO CATEGORIES (category_id, category_name, description)
VALUES ('CAT002', 'Đồ uống', 'Nước giải khát');

INSERT INTO CATEGORIES (category_id, category_name, description)
VALUES ('CAT003', 'Hóa mỹ phẩm', 'Sản phẩm chăm sóc cá nhân');

INSERT INTO CATEGORIES (category_id, category_name, description)
VALUES ('CAT004', 'Bánh kẹo', 'Bánh kẹo và đồ ăn vặt');

INSERT INTO CATEGORIES (category_id, category_name, description)
VALUES ('CAT005', 'Thực phẩm tươi sống', 'Thực phẩm tươi sống các loại');


-- CHỐT SỔ (BẮT BUỘC)
COMMIT;

-- ==========================================================
-- 4. TẠO PHƯƠNG THỨC THANH TOÁN (PAYMENT_METHODS)
-- Bắt buộc phải có để tạo Hóa đơn
-- ==========================================================
INSERT INTO PAYMENT_METHODS (payment_method_id, is_deleted)
SELECT 'PM_CASH', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PAYMENT_METHODS WHERE payment_method_id = 'PM_CASH');

INSERT INTO PAYMENT_METHODS (payment_method_id, is_deleted)
SELECT 'PM_TRANSFER', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PAYMENT_METHODS WHERE payment_method_id = 'PM_TRANSFER');

INSERT INTO CASH_PAYMENT (payment_method_id, is_deleted)
SELECT 'PM_CASH', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CASH_PAYMENT WHERE payment_method_id = 'PM_CASH');

INSERT INTO BANK_TRANSFER_PAYMENT (payment_method_id, bank_name, is_deleted)
SELECT 'PM_TRANSFER', 'Vietcombank', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM BANK_TRANSFER_PAYMENT WHERE payment_method_id = 'PM_TRANSFER');

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
-- 7. TAO SAN PHAM MAU (PRODUCTS)
-- ==========================================================
INSERT INTO PRODUCTS (product_id, product_name, base_price, category_id, supplier_id, base_unit_id, is_deleted)
VALUES ('SP0000001', N'San pham mau 1', 50000, 'CAT001', 'SUP_01', 'UN_01', 0);

INSERT INTO PRODUCTS (product_id, product_name, base_price, category_id, supplier_id, base_unit_id, is_deleted)
VALUES ('SP0000002', N'San pham mau 2', 50000, 'CAT001', 'SUP_01', 'UN_01', 0);
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
