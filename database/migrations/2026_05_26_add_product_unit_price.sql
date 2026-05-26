-- ==========================================================
-- Add selling price per product unit
-- Example:
-- Base unit: Hop = 1
-- Sale unit: Loc = 4 Hop
-- ==========================================================

ALTER TABLE PRODUCT_UNITS ADD (
    selling_price NUMBER(15, 2)
);

COMMENT ON COLUMN PRODUCT_UNITS.selling_price IS
'Gia ban theo don vi nay. VD: Hop=9000, Loc=32000, Thung=360000';

-- Đồng bộ dữ liệu cũ: đơn vị base lấy giá PRODUCTS.base_price
UPDATE PRODUCT_UNITS pu
SET pu.selling_price = (
    SELECT p.base_price
    FROM PRODUCTS p
    WHERE p.product_id = pu.product_id
)
WHERE pu.selling_price IS NULL;

COMMIT;

MERGE INTO UNITS u
USING (SELECT 'U_HOP' unit_id, N'Hộp' unit_name FROM dual) src
ON (u.unit_id = src.unit_id)
WHEN MATCHED THEN
    UPDATE SET u.unit_name = src.unit_name, u.is_deleted = 0
WHEN NOT MATCHED THEN
    INSERT (unit_id, unit_name, is_deleted)
    VALUES (src.unit_id, src.unit_name, 0);

MERGE INTO UNITS u
USING (SELECT 'U_LOC' unit_id, N'Lốc' unit_name FROM dual) src
ON (u.unit_id = src.unit_id)
WHEN MATCHED THEN
    UPDATE SET u.unit_name = src.unit_name, u.is_deleted = 0
WHEN NOT MATCHED THEN
    INSERT (unit_id, unit_name, is_deleted)
    VALUES (src.unit_id, src.unit_name, 0);

MERGE INTO PRODUCT_UNITS pu
USING (
    SELECT 'SP100503' product_id, 'U_HOP' unit_id, 1 conversion_rate_to_base, 1 is_base_unit, 9000 selling_price FROM dual
) src
ON (pu.product_id = src.product_id AND pu.unit_id = src.unit_id)
WHEN MATCHED THEN
    UPDATE SET
        pu.conversion_rate_to_base = src.conversion_rate_to_base,
        pu.is_base_unit = src.is_base_unit,
        pu.selling_price = src.selling_price,
        pu.is_deleted = 0
WHEN NOT MATCHED THEN
    INSERT (product_id, unit_id, conversion_rate_to_base, is_base_unit, selling_price, is_deleted)
    VALUES (src.product_id, src.unit_id, src.conversion_rate_to_base, src.is_base_unit, src.selling_price, 0);

MERGE INTO PRODUCT_UNITS pu
USING (
    SELECT 'SP100503' product_id, 'U_LOC' unit_id, 4 conversion_rate_to_base, 0 is_base_unit, 32000 selling_price FROM dual
) src
ON (pu.product_id = src.product_id AND pu.unit_id = src.unit_id)
WHEN MATCHED THEN
    UPDATE SET
        pu.conversion_rate_to_base = src.conversion_rate_to_base,
        pu.is_base_unit = src.is_base_unit,
        pu.selling_price = src.selling_price,
        pu.is_deleted = 0
WHEN NOT MATCHED THEN
    INSERT (product_id, unit_id, conversion_rate_to_base, is_base_unit, selling_price, is_deleted)
    VALUES (src.product_id, src.unit_id, src.conversion_rate_to_base, src.is_base_unit, src.selling_price, 0);

UPDATE PRODUCTS
SET base_unit_id = 'U_HOP'
WHERE product_id = 'SP100503';

COMMIT;