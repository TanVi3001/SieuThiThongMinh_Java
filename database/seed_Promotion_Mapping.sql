/* 1. PROMOTIONS: thêm % giảm rõ nghĩa + đơn tối thiểu */
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE PROMOTIONS ADD discount_percent NUMBER(5,2)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

UPDATE PROMOTIONS
SET discount_percent = NVL(discount_percent, NVL(discount_amount, 0))
WHERE discount_percent IS NULL;

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE PROMOTIONS ADD min_order_amount NUMBER(15,2) DEFAULT 100000';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

/* 2. Bảng mapping KM -> sản phẩm */
BEGIN
    EXECUTE IMMEDIATE '
        CREATE TABLE PROMOTION_PRODUCTS (
            promotion_id VARCHAR2(50) NOT NULL,
            product_id   VARCHAR2(50) NOT NULL,
            is_deleted   NUMBER(1) DEFAULT 0,
            created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            CONSTRAINT PK_PROMOTION_PRODUCTS PRIMARY KEY (promotion_id, product_id)
        )
    ';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

/* 3. ORDER_DETAILS: lưu giảm giá từng dòng để report dùng lại chính xác */
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ORDER_DETAILS ADD promotion_id VARCHAR2(50)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ORDER_DETAILS ADD program_discount_percent NUMBER(5,2) DEFAULT 0';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ORDER_DETAILS ADD program_discount_amount NUMBER(15,2) DEFAULT 0';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ORDER_DETAILS ADD line_net_total NUMBER(15,2)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

/* 4. ORDERS: lưu tổng giảm để mở lại hóa đơn không bị suy sai */
BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ORDERS ADD promotion_id VARCHAR2(50)';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ORDERS ADD member_discount_amount NUMBER(15,2) DEFAULT 0';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE ORDERS ADD program_discount_amount NUMBER(15,2) DEFAULT 0';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

COMMIT;

SELECT *
FROM PROMOTION_PRODUCTS;

SELECT promotion_id, discount_percent, min_order_amount
FROM PROMOTIONS;

SELECT promotion_id, product_id
FROM PROMOTION_PRODUCTS
WHERE promotion_id = 'BaoCaoDoAn';


BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE PROMOTION_PRODUCTS ADD discount_percent NUMBER(5,2) DEFAULT 0';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

/* Run once before using product-level promotion percentages */

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE PROMOTION_PRODUCTS ADD discount_percent NUMBER(5,2) DEFAULT 0';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN RAISE; END IF;
END;
/

UPDATE PROMOTION_PRODUCTS pp
SET pp.discount_percent = (
    SELECT NVL(p.discount_percent, NVL(p.discount_amount, 0))
    FROM PROMOTIONS p
    WHERE p.promotion_id = pp.promotion_id
)
WHERE NVL(pp.discount_percent, 0) = 0;

-- Fix trạng thái cũ và tránh ORA-12704: không dùng N'...' với cột VARCHAR2
UPDATE PROMOTIONS
SET status = 'Tạm ngưng'
WHERE status = 'Tạm ngưng / Kết thúc';

COMMIT;

-- Kiểm tra nhanh
SELECT promotion_id, promotion_name, status
FROM PROMOTIONS
ORDER BY promotion_id;
