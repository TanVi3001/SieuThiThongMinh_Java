-- Seed data for JasperReports SalesInvoiceReport.jrxml
-- Oracle-safe INSERT statements. Re-runnable because every row checks its primary key first.
-- Uses only columns present in the live Oracle schema.

-- 1. Reference data required by product/order foreign keys
INSERT INTO CATEGORIES (category_id, category_name, description, is_deleted)
SELECT 'CAT_RPT01', N'Invoice Test - Groceries', N'Data for Jasper invoice demo', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CATEGORIES WHERE category_id = 'CAT_RPT01');

INSERT INTO CATEGORIES (category_id, category_name, description, is_deleted)
SELECT 'CAT_RPT02', N'Invoice Test - Drinks', N'Data for Jasper invoice demo', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CATEGORIES WHERE category_id = 'CAT_RPT02');

INSERT INTO SUPPLIERS (supplier_id, supplier_name, email, address, phone_number, is_deleted)
SELECT 'SUP_RPT01', N'Fresh Market Supplier', 'fresh.supplier@example.com', N'Di An, Binh Duong', '0900000101', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM SUPPLIERS WHERE supplier_id = 'SUP_RPT01');

INSERT INTO UNITS (unit_id, unit_name, is_deleted)
SELECT 'U_RPT_EACH', N'Cai', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM UNITS WHERE unit_id = 'U_RPT_EACH');

INSERT INTO UNITS (unit_id, unit_name, is_deleted)
SELECT 'U_RPT_PACK', N'Goi', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM UNITS WHERE unit_id = 'U_RPT_PACK');

-- 2. Payment methods
INSERT INTO PAYMENT_METHODS (payment_method_id, is_deleted)
SELECT 'PM_RPT_CASH', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PAYMENT_METHODS WHERE payment_method_id = 'PM_RPT_CASH');

INSERT INTO PAYMENT_METHODS (payment_method_id, is_deleted)
SELECT 'PM_RPT_CARD', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PAYMENT_METHODS WHERE payment_method_id = 'PM_RPT_CARD');

INSERT INTO PAYMENT_METHODS (payment_method_id, is_deleted)
SELECT 'PM_RPT_TRANSFER', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PAYMENT_METHODS WHERE payment_method_id = 'PM_RPT_TRANSFER');

-- 3. Employees/cashiers
INSERT INTO EMPLOYEES (employee_id, employee_name, hire_date, salary_coefficient, total_completed_orders, role_id, shift_id, is_deleted)
SELECT 'EMP_RPT01', N'Nguyen Minh Anh', DATE '2024-01-10', 1.25, 0, NULL, NULL, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM EMPLOYEES WHERE employee_id = 'EMP_RPT01');

INSERT INTO EMPLOYEES (employee_id, employee_name, hire_date, salary_coefficient, total_completed_orders, role_id, shift_id, is_deleted)
SELECT 'EMP_RPT02', N'Tran Gia Bao', DATE '2024-03-15', 1.10, 0, NULL, NULL, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM EMPLOYEES WHERE employee_id = 'EMP_RPT02');

INSERT INTO EMPLOYEES (employee_id, employee_name, hire_date, salary_coefficient, total_completed_orders, role_id, shift_id, is_deleted)
SELECT 'EMP_RPT03', N'Le Thu Ha', DATE '2023-11-20', 1.35, 0, NULL, NULL, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM EMPLOYEES WHERE employee_id = 'EMP_RPT03');

-- 4. Customers
INSERT INTO CUSTOMERS (customer_id, customer_name, role_id, reward_points, is_deleted)
SELECT 'CUS_RPT01', N'Pham Hoang Nam', NULL, 85, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CUSTOMERS WHERE customer_id = 'CUS_RPT01');

INSERT INTO CUSTOMERS (customer_id, customer_name, role_id, reward_points, is_deleted)
SELECT 'CUS_RPT02', N'Vo Thanh Truc', NULL, 210, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CUSTOMERS WHERE customer_id = 'CUS_RPT02');

INSERT INTO CUSTOMERS (customer_id, customer_name, role_id, reward_points, is_deleted)
SELECT 'CUS_RPT03', N'Dang Quoc Viet', NULL, 520, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM CUSTOMERS WHERE customer_id = 'CUS_RPT03');

-- 5. Products
INSERT INTO PRODUCTS (product_id, product_name, base_price, category_id, supplier_id, base_unit_id, is_deleted)
SELECT 'PRD_RPT001', N'Gao thom ST25 5kg', 145000, 'CAT_RPT01', 'SUP_RPT01', 'U_RPT_PACK', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PRODUCTS WHERE product_id = 'PRD_RPT001');

INSERT INTO PRODUCTS (product_id, product_name, base_price, category_id, supplier_id, base_unit_id, is_deleted)
SELECT 'PRD_RPT002', N'Sua tuoi khong duong 1L', 32000, 'CAT_RPT02', 'SUP_RPT01', 'U_RPT_EACH', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PRODUCTS WHERE product_id = 'PRD_RPT002');

INSERT INTO PRODUCTS (product_id, product_name, base_price, category_id, supplier_id, base_unit_id, is_deleted)
SELECT 'PRD_RPT003', N'Mi goi bo rau thom', 4500, 'CAT_RPT01', 'SUP_RPT01', 'U_RPT_PACK', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PRODUCTS WHERE product_id = 'PRD_RPT003');

INSERT INTO PRODUCTS (product_id, product_name, base_price, category_id, supplier_id, base_unit_id, is_deleted)
SELECT 'PRD_RPT004', N'Dau an huong duong 1L', 58000, 'CAT_RPT01', 'SUP_RPT01', 'U_RPT_EACH', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PRODUCTS WHERE product_id = 'PRD_RPT004');

INSERT INTO PRODUCTS (product_id, product_name, base_price, category_id, supplier_id, base_unit_id, is_deleted)
SELECT 'PRD_RPT005', N'Ca phe rang xay 500g', 89000, 'CAT_RPT02', 'SUP_RPT01', 'U_RPT_PACK', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PRODUCTS WHERE product_id = 'PRD_RPT005');

INSERT INTO PRODUCTS (product_id, product_name, base_price, category_id, supplier_id, base_unit_id, is_deleted)
SELECT 'PRD_RPT006', N'Nuoc suoi 500ml', 6000, 'CAT_RPT02', 'SUP_RPT01', 'U_RPT_EACH', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PRODUCTS WHERE product_id = 'PRD_RPT006');

INSERT INTO PRODUCTS (product_id, product_name, base_price, category_id, supplier_id, base_unit_id, is_deleted)
SELECT 'PRD_RPT007', N'Trung ga hop 10 qua', 36000, 'CAT_RPT01', 'SUP_RPT01', 'U_RPT_PACK', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PRODUCTS WHERE product_id = 'PRD_RPT007');

INSERT INTO PRODUCTS (product_id, product_name, base_price, category_id, supplier_id, base_unit_id, is_deleted)
SELECT 'PRD_RPT008', N'Banh quy bo 300g', 42000, 'CAT_RPT01', 'SUP_RPT01', 'U_RPT_PACK', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM PRODUCTS WHERE product_id = 'PRD_RPT008');

-- 6. Orders for Jasper invoice testing
INSERT INTO ORDERS (order_id, customer_id, payment_method_id, order_date, status, total_amount, note, employee_id, is_deleted)
SELECT 'INV_RPT001', 'CUS_RPT01', 'PM_RPT_CASH', SYSDATE - 2, N'Hoan thanh', 182000, N'Invoice Jasper demo order 1', 'EMP_RPT01', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDERS WHERE order_id = 'INV_RPT001');

INSERT INTO ORDERS (order_id, customer_id, payment_method_id, order_date, status, total_amount, note, employee_id, is_deleted)
SELECT 'INV_RPT002', 'CUS_RPT02', 'PM_RPT_CARD', SYSDATE - 1, N'Hoan thanh', 377000, N'Invoice Jasper demo order 2', 'EMP_RPT02', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDERS WHERE order_id = 'INV_RPT002');

INSERT INTO ORDERS (order_id, customer_id, payment_method_id, order_date, status, total_amount, note, employee_id, is_deleted)
SELECT 'INV_RPT003', 'CUS_RPT03', 'PM_RPT_TRANSFER', SYSDATE, N'Hoan thanh', 630000, N'Invoice Jasper demo order 3', 'EMP_RPT03', 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDERS WHERE order_id = 'INV_RPT003');

-- 7. Order details. Report line totals are computed as quantity * unit_price.
INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT001_01', 'INV_RPT001', 'PRD_RPT002', 2, 'U_RPT_EACH', 2, 32000, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT001_01');

INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT001_02', 'INV_RPT001', 'PRD_RPT003', 4, 'U_RPT_PACK', 4, 4500, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT001_02');

INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT001_03', 'INV_RPT001', 'PRD_RPT004', 1, 'U_RPT_EACH', 1, 58000, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT001_03');

INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT001_04', 'INV_RPT001', 'PRD_RPT006', 7, 'U_RPT_EACH', 7, 6000, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT001_04');

INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT002_01', 'INV_RPT002', 'PRD_RPT001', 1, 'U_RPT_PACK', 1, 145000, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT002_01');

INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT002_02', 'INV_RPT002', 'PRD_RPT005', 2, 'U_RPT_PACK', 2, 89000, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT002_02');

INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT002_03', 'INV_RPT002', 'PRD_RPT006', 2, 'U_RPT_EACH', 2, 6000, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT002_03');

INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT002_04', 'INV_RPT002', 'PRD_RPT008', 1, 'U_RPT_PACK', 1, 42000, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT002_04');

INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT003_01', 'INV_RPT003', 'PRD_RPT001', 2, 'U_RPT_PACK', 2, 145000, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT003_01');

INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT003_02', 'INV_RPT003', 'PRD_RPT005', 2, 'U_RPT_PACK', 2, 89000, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT003_02');

INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT003_03', 'INV_RPT003', 'PRD_RPT007', 3, 'U_RPT_PACK', 3, 36000, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT003_03');

INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT003_04', 'INV_RPT003', 'PRD_RPT008', 1, 'U_RPT_PACK', 1, 42000, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT003_04');

INSERT INTO ORDER_DETAILS (order_detail_id, order_id, product_id, quantity, unit_id, quantity_base, unit_price, is_deleted)
SELECT 'INV_RPT003_05', 'INV_RPT003', 'PRD_RPT006', 2, 'U_RPT_EACH', 2, 6000, 0
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM ORDER_DETAILS WHERE order_detail_id = 'INV_RPT003_05');

COMMIT;
