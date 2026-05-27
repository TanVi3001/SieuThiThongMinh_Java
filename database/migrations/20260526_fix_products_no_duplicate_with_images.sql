-- ==========================================================
-- SMART SUPERMARKET - Seed cấu hình đơn vị bán theo danh sách sản phẩm
-- Generated from uploaded CSV/text files: 121 products
-- Ý tưởng:
--   1) PRODUCT_UNITS.selling_price = giá bán theo đơn vị.
--   2) PRODUCTS.base_unit_id = đơn vị nhỏ nhất để trừ kho.
--   3) INVENTORY.quantity sẽ được nhân quy đổi 1 lần nếu chuyển từ đơn vị gói/lốc/hộp sang đơn vị nhỏ hơn.
--      Điều kiện chặn nhân lại: chỉ nhân khi PRODUCTS.base_unit_id chưa bằng base unit mới.
-- ==========================================================

SET DEFINE OFF;

BEGIN
    EXECUTE IMMEDIATE 'ALTER TABLE PRODUCT_UNITS ADD (selling_price NUMBER(15,2))';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -1430 THEN -- ORA-01430: column being added already exists
            RAISE;
        END IF;
END;
/

CREATE OR REPLACE PROCEDURE CFG_PRODUCT_UOM_BY_NAME (
    p_product_name     IN NVARCHAR2,
    p_base_unit_id     IN VARCHAR2,
    p_base_unit_name   IN NVARCHAR2,
    p_base_price       IN NUMBER,
    p_pack_unit_id     IN VARCHAR2 DEFAULT NULL,
    p_pack_unit_name   IN NVARCHAR2 DEFAULT NULL,
    p_pack_rate        IN NUMBER DEFAULT NULL,
    p_pack_price       IN NUMBER DEFAULT NULL
) IS
    v_product_id       PRODUCTS.product_id%TYPE;
    v_old_base_unit_id PRODUCTS.base_unit_id%TYPE;
    v_need_multiply    NUMBER := 0;
BEGIN
    BEGIN
        SELECT product_id, base_unit_id
        INTO v_product_id, v_old_base_unit_id
        FROM PRODUCTS
        WHERE product_name = p_product_name
          AND NVL(is_deleted, 0) = 0
        FETCH FIRST 1 ROWS ONLY;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            DBMS_OUTPUT.PUT_LINE('SKIP - not found: ' || p_product_name);
            RETURN;
    END;

    MERGE INTO UNITS u
    USING (SELECT p_base_unit_id AS unit_id, p_base_unit_name AS unit_name FROM dual) src
    ON (u.unit_id = src.unit_id)
    WHEN MATCHED THEN
        UPDATE SET u.unit_name = src.unit_name, u.is_deleted = 0
    WHEN NOT MATCHED THEN
        INSERT (unit_id, unit_name, is_deleted)
        VALUES (src.unit_id, src.unit_name, 0);

    IF p_pack_unit_id IS NOT NULL THEN
        MERGE INTO UNITS u
        USING (SELECT p_pack_unit_id AS unit_id, p_pack_unit_name AS unit_name FROM dual) src
        ON (u.unit_id = src.unit_id)
        WHEN MATCHED THEN
            UPDATE SET u.unit_name = src.unit_name, u.is_deleted = 0
        WHEN NOT MATCHED THEN
            INSERT (unit_id, unit_name, is_deleted)
            VALUES (src.unit_id, src.unit_name, 0);
    END IF;

    IF p_pack_unit_id IS NOT NULL
       AND NVL(p_pack_rate, 1) > 1
       AND (v_old_base_unit_id IS NULL OR v_old_base_unit_id <> p_base_unit_id) THEN
        v_need_multiply := 1;
    END IF;

    IF v_need_multiply = 1 THEN
        UPDATE INVENTORY
        SET quantity = quantity * p_pack_rate,
            unit = p_base_unit_name,
            last_updated = SYSDATE
        WHERE product_id = v_product_id
          AND NVL(is_deleted, 0) = 0;
    ELSE
        UPDATE INVENTORY
        SET unit = p_base_unit_name,
            last_updated = SYSDATE
        WHERE product_id = v_product_id
          AND NVL(is_deleted, 0) = 0;
    END IF;

    MERGE INTO PRODUCT_UNITS pu
    USING (
        SELECT v_product_id AS product_id,
               p_base_unit_id AS unit_id,
               1 AS conversion_rate_to_base,
               p_base_price AS selling_price
        FROM dual
    ) src
    ON (pu.product_id = src.product_id AND pu.unit_id = src.unit_id)
    WHEN MATCHED THEN
        UPDATE SET
            pu.conversion_rate_to_base = src.conversion_rate_to_base,
            pu.selling_price = src.selling_price,
            pu.is_base_unit = 1,
            pu.is_deleted = 0
    WHEN NOT MATCHED THEN
        INSERT (product_id, unit_id, conversion_rate_to_base, selling_price, is_base_unit, is_deleted)
        VALUES (src.product_id, src.unit_id, src.conversion_rate_to_base, src.selling_price, 1, 0);

    UPDATE PRODUCT_UNITS
    SET is_base_unit = 0
    WHERE product_id = v_product_id
      AND unit_id <> p_base_unit_id;

    IF p_pack_unit_id IS NOT NULL THEN
        MERGE INTO PRODUCT_UNITS pu
        USING (
            SELECT v_product_id AS product_id,
                   p_pack_unit_id AS unit_id,
                   p_pack_rate AS conversion_rate_to_base,
                   p_pack_price AS selling_price
            FROM dual
        ) src
        ON (pu.product_id = src.product_id AND pu.unit_id = src.unit_id)
        WHEN MATCHED THEN
            UPDATE SET
                pu.conversion_rate_to_base = src.conversion_rate_to_base,
                pu.selling_price = src.selling_price,
                pu.is_base_unit = 0,
                pu.is_deleted = 0
        WHEN NOT MATCHED THEN
            INSERT (product_id, unit_id, conversion_rate_to_base, selling_price, is_base_unit, is_deleted)
            VALUES (src.product_id, src.unit_id, src.conversion_rate_to_base, src.selling_price, 0, 0);
    END IF;

    UPDATE PRODUCTS
    SET base_unit_id = p_base_unit_id
    WHERE product_id = v_product_id;

    DBMS_OUTPUT.PUT_LINE('OK - ' || v_product_id || ' - ' || p_product_name);
END;
/


-- ==========================================================
-- IMPORTANT: Dùng CALL thay vì gọi trần procedure.
-- Lỗi cũ ORA-00900 là do chạy dòng CFG_PRODUCT_UOM_BY_NAME(...) như SQL thường.
-- CALL là statement hợp lệ trong Oracle/DataGrip.
-- ==========================================================

CALL CFG_PRODUCT_UOM_BY_NAME(N'Mì Hảo Hảo tôm chua cay', 'U_GOI', N'Gói', 5000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Mì Omachi sườn hầm ngũ quả', 'U_GOI', N'Gói', 8000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Phở bò Vifon túi', 'U_TUI', N'Túi', 7500, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Gạo ST25 Ông Cua túi 5kg', 'U_TUI', N'Túi', 180000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Nước mắm Nam Ngư 750ml', 'U_CAI', N'Cái', 35000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Nước mắm Chinsu cá hồi', 'U_GOI', N'Gói', 45000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Dầu ăn Tường An 2L', 'U_CAI', N'Cái', 52000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Dầu ăn Simply 2L', 'U_CAI', N'Cái', 60000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Dầu ăn Happy Koki 2L', 'U_CAI', N'Cái', 60000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Đường tinh luyện Biên Hòa 1kg', 'U_TUI', N'Túi', 28000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sữa đặc Ông Thọ đỏ lon', 'U_LON', N'Lon', 22000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Hạt nêm Knorr thịt thăn 400g', 'U_GOI', N'Gói', 38000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Tương ớt Chinsu 250g', 'U_CAI', N'Cái', 15000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Nước suối Aquafina 500ml', 'U_CAI', N'Cái', 5000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Nước khoáng Lavie 500ml', 'U_CAI', N'Cái', 5000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Coca Cola lon 330ml', 'U_LON', N'Lon', 10000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Pepsi lon 330ml', 'U_LON', N'Lon', 10000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sprite chai 1.5L', 'U_CHAI', N'Chai', 20000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Trà Ô long Tea Plus', 'U_CAI', N'Cái', 10000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Nước tăng lực Redbull lon', 'U_LON', N'Lon', 15000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Cà phê lon Birdy xanh', 'U_LON', N'Lon', 12000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Dầu gội Clear Men 630g', 'U_CHAI', N'Chai', 145000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Dầu gội Sunsilk mềm mượt', 'U_CHAI', N'Chai', 135000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Kem đánh răng PS 123', 'U_CAI', N'Cái', 35000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sữa tắm Lifebuoy bảo vệ', 'U_CHAI', N'Chai', 160000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sữa rửa mặt Hazeline', 'U_CAI', N'Cái', 55000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Giấy vệ sinh Bless You lốc 10', 'U_CAI', N'Cái', 85000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Khăn ướt Baby gói 100 tờ', 'U_TO', N'Tờ', 500, 'U_GOI', N'Gói', 100, 40000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bánh Oreo nhân socola', 'U_GOI', N'Gói', 15000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Kẹo Alpenliebe caramen', 'U_GOI', N'Gói', 12000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bánh Cosy quy sữa 240g', 'U_GOI', N'Gói', 25000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Snack khoai tây Pringles', 'U_GOI', N'Gói', 35000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Socola KitKat trà xanh', 'U_GOI', N'Gói', 20000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bánh ChocoPie Orion hộp 6', 'U_CAI', N'Cái', 5500, 'U_HOP', N'Hộp', 6, 32000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bánh gạo One One phô mai', 'U_GOI', N'Gói', 22000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Kẹo dẻo Haribo gấu', 'U_GOI', N'Gói', 18000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Thịt ba chỉ bò Mỹ 500g', 'U_GOI', N'Gói', 150000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Ức gà phi lê 500g', 'U_GOI', N'Gói', 45000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Cá hồi phi lê tươi 200g', 'U_GOI', N'Gói', 120000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Trứng gà ta vỉ 10 quả', 'U_QUA', N'Quả', 4500, 'U_VI', N'Vỉ', 10, 45000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Khoai tây vàng túi 1kg', 'U_TUI', N'Túi', 25000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sữa tươi TH True Milk ít đường 1L', 'U_TUI', N'Túi', 37000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sữa tươi Vinamilk không đường 1L', 'U_TUI', N'Túi', 36000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sữa chua Vinamilk có đường lốc 4 hộp', 'U_HOP', N'Hộp', 8000, 'U_LOC', N'Lốc', 4, 32000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Phô mai Con Bò Cười hộp 8 miếng', 'U_MIENG', N'Miếng', 5500, 'U_HOP', N'Hộp', 8, 42000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sữa đặc Ngôi Sao Phương Nam lon 380g', 'U_LON', N'Lon', 26000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sữa hạt Vinamilk óc chó 180ml lốc 4 hộp', 'U_HOP', N'Hộp', 10000, 'U_LOC', N'Lốc', 4, 39000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bơ lạt Anchor hộp 227g', 'U_HOP', N'Hộp', 89000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sữa chua uống Yakult lốc 5 chai', 'U_CHAI', N'Chai', 6000, 'U_LOC', N'Lốc', 5, 30000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Xúc xích Đức Việt gói 500g', 'U_GOI', N'Gói', 78000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Cá viên CP gói 500g', 'U_GOI', N'Gói', 65000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Chả giò Cầu Tre hải sản 500g', 'U_CAI', N'Cái', 72000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Khoai tây chiên đông lạnh 1kg', 'U_GOI', N'Gói', 85000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Pizza hải sản đông lạnh 300g', 'U_CAI', N'Cái', 69000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Gà viên chiên CP 400g', 'U_CAI', N'Cái', 59000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bò viên Vissan gói 500g', 'U_GOI', N'Gói', 76000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Há cảo tôm đông lạnh 300g', 'U_CAI', N'Cái', 68000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Khăn giấy Pulppy 2 lớp 10 cuộn', 'U_CUON', N'Cuộn', 6500, 'U_LOC', N'Lốc', 10, 62000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Túi rác tự hủy 3 cuộn', 'U_CUON', N'Cuộn', 12000, 'U_LOC', N'Lốc', 3, 35000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Màng bọc thực phẩm Ringo 30cm x 30m', 'U_CAI', N'Cái', 29000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Hộp nhựa Lock Lock 1L', 'U_HOP', N'Hộp', 55000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Giấy bạc nướng thực phẩm 30cm x 5m', 'U_CAI', N'Cái', 42000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Ly giấy dùng một lần 50 cái', 'U_CAI', N'Cái', 1000, 'U_GOI', N'Gói', 50, 36000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Ống hút giấy pack 100 cái', 'U_CAI', N'Cái', 500, 'U_PACK', N'Pack', 100, 28000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Túi zipper đựng thực phẩm 20 túi', 'U_TUI', N'Túi', 2500, 'U_GOI', N'Gói', 20, 48000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Kem đánh răng P/S trà xanh 180g', 'U_CAI', N'Cái', 32000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bàn chải Oral-B mềm', 'U_CAI', N'Cái', 28000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Nước súc miệng Listerine 500ml', 'U_CHAI', N'Chai', 95000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Dao cạo râu Gillette Blue 3', 'U_CAI', N'Cái', 45000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Lăn khử mùi Nivea Men 50ml', 'U_CAI', N'Cái', 72000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Dầu gội Dove phục hồi hư tổn 640g', 'U_CHAI', N'Chai', 145000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sữa rửa mặt Simple 150ml', 'U_CAI', N'Cái', 115000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bông tẩy trang Jomi 120 miếng', 'U_MIENG', N'Miếng', 500, 'U_GOI', N'Gói', 120, 39000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Tã Bobby quần size M 68 miếng', 'U_MIENG', N'Miếng', 4500, 'U_GOI', N'Gói', 68, 285000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Khăn ướt Mamamy 100 tờ', 'U_TO', N'Tờ', 500, 'U_GOI', N'Gói', 100, 35000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sữa tắm gội Johnson Baby 500ml', 'U_CHAI', N'Chai', 89000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Phấn rôm Johnson Baby 200g', 'U_CAI', N'Cái', 55000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bình sữa Pigeon 240ml', 'U_CAI', N'Cái', 165000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Nước giặt Dnee hồng 3L', 'U_CAI', N'Cái', 165000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Kem chống hăm Bepanthen 30g', 'U_CAI', N'Cái', 115000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bột ăn dặm Heinz rau củ 200g', 'U_GOI', N'Gói', 89000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Tập Campus 200 trang', 'U_CAI', N'Cái', 16000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bút bi Thiên Long TL-027', 'U_CAI', N'Cái', 5000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Giấy note Pronoti 3x3', 'U_CAI', N'Cái', 18000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bìa hồ sơ A4 Plus', 'U_CAI', N'Cái', 12000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Băng keo trong 5cm', 'U_CAI', N'Cái', 15000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Kéo học sinh Deli 17cm', 'U_CAI', N'Cái', 25000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bút highlight Stabilo vàng', 'U_CAI', N'Cái', 22000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'File lá A4 20 túi', 'U_TUI', N'Túi', 2000, 'U_GOI', N'Gói', 20, 32000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Hạt mèo Whiskas cá ngừ 1.2kg', 'U_CAI', N'Cái', 145000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Hạt chó Pedigree vị bò 1.5kg', 'U_CAI', N'Cái', 135000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Pate mèo Me-O cá ngừ 80g', 'U_GOI', N'Gói', 15000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Cát vệ sinh mèo Min 5L', 'U_CAI', N'Cái', 79000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Sữa tắm chó mèo Joyce Dolls 400ml', 'U_CHAI', N'Chai', 115000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Snack thưởng mèo Ciao Churu 4 thanh', 'U_THANH', N'Thanh', 14000, 'U_GOI', N'Gói', 4, 55000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Pate chó SmartHeart bò 130g', 'U_GOI', N'Gói', 22000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Vòng cổ chó mèo size M', 'U_CAI', N'Cái', 35000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Rau cải hữu cơ Vinamit 300g', 'U_GOI', N'Gói', 28000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Cà rốt hữu cơ Đà Lạt 500g', 'U_GOI', N'Gói', 35000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Yến mạch Quaker nguyên chất 400g', 'U_TUI', N'Túi', 79000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Hạt chia Úc Organic 250g', 'U_TUI', N'Túi', 99000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Gạo lứt hữu cơ Hoa Sữa 1kg', 'U_TUI', N'Túi', 65000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Mật ong hoa nhãn nguyên chất 500ml', 'U_CAI', N'Cái', 145000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bột cacao nguyên chất 200g', 'U_TUI', N'Túi', 85000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Hạnh nhân rang bơ 250g', 'U_TUI', N'Túi', 125000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Cá hộp Ba Cô Gái sốt cà 155g', 'U_HOP', N'Hộp', 25000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Thịt hộp Spam Classic 340g', 'U_HOP', N'Hộp', 98000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Pate gan Vissan hộp 170g', 'U_HOP', N'Hộp', 32000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Cháo ăn liền Gấu Đỏ thịt bằm 50g', 'U_GOI', N'Gói', 7000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Mì ly Modern lẩu Thái 65g', 'U_GOI', N'Gói', 11000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bún bò Huế Vifon tô 120g', 'U_TO', N'Tô', 18000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Cơm cháy chà bông hộp 200g', 'U_HOP', N'Hộp', 55000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Súp nui ăn liền Knorr 60g', 'U_GOI', N'Gói', 15000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Nước lau sàn Sunlight hương hoa 1kg', 'U_CHAI', N'Chai', 45000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Nước tẩy Javel Gift 1L', 'U_CHAI', N'Chai', 26000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Xịt phòng Glade hương hoa 280ml', 'U_CHAI', N'Chai', 65000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Nước lau kính Gift 580ml', 'U_CHAI', N'Chai', 32000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Bình xịt côn trùng Raid 600ml', 'U_CHAI', N'Chai', 72000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Miếng rửa chén Scotch Brite 3 miếng', 'U_MIENG', N'Miếng', 8500, 'U_GOI', N'Gói', 3, 25000);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Cây lau nhà 360 độ bộ', 'U_BO', N'Bộ', 185000, NULL, NULL, NULL, NULL);
CALL CFG_PRODUCT_UOM_BY_NAME(N'Nước thông cống Hando 1L', 'U_CHAI', N'Chai', 58000, NULL, NULL, NULL, NULL);

COMMIT;

-- Nếu muốn dọn thủ tục sau khi seed xong thì chạy dòng dưới:
-- DROP PROCEDURE CFG_PRODUCT_UOM_BY_NAME;

-- Kiểm tra nhanh:
-- SELECT p.product_id, p.product_name, u.unit_name AS base_unit, i.quantity AS stock_base
-- FROM PRODUCTS p
-- LEFT JOIN UNITS u ON u.unit_id = p.base_unit_id
-- LEFT JOIN INVENTORY i ON i.product_id = p.product_id
-- WHERE NVL(p.is_deleted,0)=0
-- ORDER BY p.product_id;
