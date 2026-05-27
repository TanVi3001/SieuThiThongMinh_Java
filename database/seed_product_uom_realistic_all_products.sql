
-- ==========================================================
-- Smart Supermarket - Seed cau hinh don vi ban thuc te
-- Generated for PRODUCT_UNITS / UNITS / INVENTORY
--
-- Cach chay trong DataGrip:
-- 1) Chon dung Oracle connection.
-- 2) Right click file -> Run, hoac Run Script.
-- 3) Khong boi den chay tung dong CALL rieng le.
--
-- Nguyen tac:
-- - Do uong: ban le + loc + thung.
-- - Mi/phở/cháo/súp ăn liền: ban goi/ly/to + thung.
-- - San pham dong goi kin: khong tach le vo ly (VD: ta, khan uot, bong tay trang), chi ban goi/hop + thung.
-- - San pham co pack co the tach le hop/chai/cai hop ly: co don vi nho + pack + thung.
-- - Script co bang UOM_SEED_LOG de tranh nhan ton kho lap lai khi chay nhieu lan.
-- ==========================================================


DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*)
    INTO v_count
    FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME = 'PRODUCT_UNITS'
      AND COLUMN_NAME = 'SELLING_PRICE';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE PRODUCT_UNITS ADD (selling_price NUMBER(15,2))';
    END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE '
        CREATE TABLE UOM_SEED_LOG (
            product_id VARCHAR2(50) NOT NULL,
            seed_code VARCHAR2(100) NOT NULL,
            inventory_factor NUMBER(12,4),
            created_at DATE DEFAULT SYSDATE,
            CONSTRAINT PK_UOM_SEED_LOG PRIMARY KEY (product_id, seed_code)
        )
    ';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN
            RAISE;
        END IF;
END;
/

CREATE OR REPLACE PROCEDURE CFG_PRODUCT_UOM_BY_NAME (
    p_product_name      IN NVARCHAR2,
    p_base_unit_id      IN VARCHAR2,
    p_base_unit_name    IN NVARCHAR2,
    p_base_price        IN NUMBER,
    p_inventory_factor  IN NUMBER DEFAULT 1,

    p_unit1_id          IN VARCHAR2 DEFAULT NULL,
    p_unit1_name        IN NVARCHAR2 DEFAULT NULL,
    p_unit1_rate        IN NUMBER DEFAULT NULL,
    p_unit1_price       IN NUMBER DEFAULT NULL,

    p_unit2_id          IN VARCHAR2 DEFAULT NULL,
    p_unit2_name        IN NVARCHAR2 DEFAULT NULL,
    p_unit2_rate        IN NUMBER DEFAULT NULL,
    p_unit2_price       IN NUMBER DEFAULT NULL,

    p_unit3_id          IN VARCHAR2 DEFAULT NULL,
    p_unit3_name        IN NVARCHAR2 DEFAULT NULL,
    p_unit3_rate        IN NUMBER DEFAULT NULL,
    p_unit3_price       IN NUMBER DEFAULT NULL
) IS
    v_product_id PRODUCTS.product_id%TYPE;
    v_old_base_unit_id PRODUCTS.base_unit_id%TYPE;
    v_logged NUMBER := 0;

    PROCEDURE ensure_unit(p_unit_id VARCHAR2, p_unit_name NVARCHAR2) IS
    BEGIN
        IF p_unit_id IS NULL THEN
            RETURN;
        END IF;

        MERGE INTO UNITS u
        USING (
            SELECT p_unit_id AS unit_id,
                   p_unit_name AS unit_name
            FROM dual
        ) src
        ON (u.unit_id = src.unit_id)
        WHEN MATCHED THEN
            UPDATE SET
                u.unit_name = src.unit_name,
                u.is_deleted = 0
        WHEN NOT MATCHED THEN
            INSERT (unit_id, unit_name, is_deleted)
            VALUES (src.unit_id, src.unit_name, 0);
    END;

    PROCEDURE upsert_product_unit(
        p_unit_id VARCHAR2,
        p_rate NUMBER,
        p_price NUMBER,
        p_is_base NUMBER
    ) IS
    BEGIN
        IF p_unit_id IS NULL OR p_rate IS NULL THEN
            RETURN;
        END IF;

        IF p_is_base = 1 THEN
            UPDATE PRODUCT_UNITS
            SET is_base_unit = 0
            WHERE product_id = v_product_id;
        END IF;

        MERGE INTO PRODUCT_UNITS pu
        USING (
            SELECT v_product_id AS product_id,
                   p_unit_id AS unit_id,
                   p_rate AS conversion_rate_to_base,
                   p_price AS selling_price,
                   p_is_base AS is_base_unit
            FROM dual
        ) src
        ON (pu.product_id = src.product_id AND pu.unit_id = src.unit_id)
        WHEN MATCHED THEN
            UPDATE SET
                pu.conversion_rate_to_base = src.conversion_rate_to_base,
                pu.selling_price = src.selling_price,
                pu.is_base_unit = src.is_base_unit,
                pu.is_deleted = 0
        WHEN NOT MATCHED THEN
            INSERT (
                product_id,
                unit_id,
                conversion_rate_to_base,
                selling_price,
                is_base_unit,
                is_deleted
            )
            VALUES (
                src.product_id,
                src.unit_id,
                src.conversion_rate_to_base,
                src.selling_price,
                src.is_base_unit,
                0
            );
    END;

BEGIN
    BEGIN
        SELECT product_id, base_unit_id
        INTO v_product_id, v_old_base_unit_id
        FROM (
            SELECT product_id, base_unit_id
            FROM PRODUCTS
            WHERE LOWER(TRIM(product_name)) = LOWER(TRIM(p_product_name))
              AND NVL(is_deleted, 0) = 0
            ORDER BY product_id
        )
        WHERE ROWNUM = 1;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            DBMS_OUTPUT.PUT_LINE('SKIP - Khong tim thay san pham: ' || p_product_name);
            RETURN;
    END;

    ensure_unit(p_base_unit_id, p_base_unit_name);
    ensure_unit(p_unit1_id, p_unit1_name);
    ensure_unit(p_unit2_id, p_unit2_name);
    ensure_unit(p_unit3_id, p_unit3_name);

    -- Chi nhan ton kho 1 lan khi chuyen tu don vi ban dau sang don vi goc nho hon.
    SELECT COUNT(*)
    INTO v_logged
    FROM UOM_SEED_LOG
    WHERE product_id = v_product_id
      AND seed_code = 'INV_TO_BASE_V1';

    IF NVL(p_inventory_factor, 1) <> 1
       AND v_logged = 0
       AND NVL(v_old_base_unit_id, '#') <> p_base_unit_id THEN
        UPDATE INVENTORY
        SET quantity = quantity * p_inventory_factor,
            last_updated = SYSDATE
        WHERE product_id = v_product_id
          AND NVL(is_deleted, 0) = 0;

        INSERT INTO UOM_SEED_LOG(product_id, seed_code, inventory_factor)
        VALUES (v_product_id, 'INV_TO_BASE_V1', p_inventory_factor);
    END IF;

    UPDATE PRODUCTS
    SET base_unit_id = p_base_unit_id
    WHERE product_id = v_product_id;

    upsert_product_unit(p_base_unit_id, 1, p_base_price, 1);
    upsert_product_unit(p_unit1_id, p_unit1_rate, p_unit1_price, 0);
    upsert_product_unit(p_unit2_id, p_unit2_rate, p_unit2_price, 0);
    upsert_product_unit(p_unit3_id, p_unit3_rate, p_unit3_price, 0);

    DBMS_OUTPUT.PUT_LINE('OK - ' || v_product_id || ' - ' || p_product_name);
END;
/

BEGIN
    CFG_PRODUCT_UOM_BY_NAME(N'Mì Hảo Hảo tôm chua cay', 'U_GOI', N'Gói', 5000, 1, 'U_THUNG', N'Thùng', 30, 145000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Mì Omachi sườn hầm ngũ quả', 'U_GOI', N'Gói', 8000, 1, 'U_THUNG', N'Thùng', 30, 230000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Phở bò Vifon túi', 'U_TUI', N'Túi', 7500, 1, 'U_THUNG', N'Thùng', 30, 215000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Gạo ST25 Ông Cua túi 5kg', 'U_TUI', N'Túi', 180000, 1, 'U_BAO', N'Bao', 4, 700000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Nước mắm Nam Ngư 750ml', 'U_CHAI', N'Chai', 35000, 1, 'U_THUNG', N'Thùng', 12, 400000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Nước mắm Chinsu cá hồi', 'U_CHAI', N'Chai', 45000, 1, 'U_THUNG', N'Thùng', 12, 515000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Dầu ăn Tường An 2L', 'U_CHAI', N'Chai', 52000, 1, 'U_THUNG', N'Thùng', 6, 300000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Dầu ăn Simply 2L', 'U_CHAI', N'Chai', 60000, 1, 'U_THUNG', N'Thùng', 6, 345000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Dầu ăn Happy Koki 2L', 'U_CHAI', N'Chai', 60000, 1, 'U_THUNG', N'Thùng', 6, 345000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Đường tinh luyện Biên Hòa 1kg', 'U_GOI', N'Gói', 28000, 1, 'U_BAO', N'Bao', 10, 270000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sữa đặc Ông Thọ đỏ lon', 'U_LON', N'Lon', 22000, 1, 'U_THUNG', N'Thùng', 48, 1020000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Hạt nêm Knorr thịt thăn 400g', 'U_GOI', N'Gói', 38000, 1, 'U_THUNG', N'Thùng', 24, 875000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Tương ớt Chinsu 250g', 'U_CHAI', N'Chai', 15000, 1, 'U_THUNG', N'Thùng', 24, 345000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Nước suối Aquafina 500ml', 'U_CHAI', N'Chai', 5000, 1, 'U_LOC', N'Lốc', 6, 29000, 'U_THUNG', N'Thùng', 24, 115000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Nước khoáng Lavie 500ml', 'U_CHAI', N'Chai', 5000, 1, 'U_LOC', N'Lốc', 6, 29000, 'U_THUNG', N'Thùng', 24, 115000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Coca Cola lon 330ml', 'U_LON', N'Lon', 10000, 1, 'U_LOC', N'Lốc', 6, 58000, 'U_THUNG', N'Thùng', 24, 225000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Pepsi lon 330ml', 'U_LON', N'Lon', 10000, 1, 'U_LOC', N'Lốc', 6, 58000, 'U_THUNG', N'Thùng', 24, 225000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sprite chai 1.5L', 'U_CHAI', N'Chai', 20000, 1, 'U_LOC', N'Lốc', 6, 116000, 'U_THUNG', N'Thùng', 12, 230000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Trà Ô long Tea Plus', 'U_CHAI', N'Chai', 10000, 1, 'U_LOC', N'Lốc', 6, 58000, 'U_THUNG', N'Thùng', 24, 225000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Nước tăng lực Redbull lon', 'U_LON', N'Lon', 15000, 1, 'U_LOC', N'Lốc', 6, 87000, 'U_THUNG', N'Thùng', 24, 340000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Cà phê lon Birdy xanh', 'U_LON', N'Lon', 12000, 1, 'U_LOC', N'Lốc', 6, 70000, 'U_THUNG', N'Thùng', 30, 340000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Dầu gội Clear Men 630g', 'U_CHAI', N'Chai', 145000, 1, 'U_THUNG', N'Thùng', 12, 1650000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Dầu gội Sunsilk mềm mượt', 'U_CHAI', N'Chai', 135000, 1, 'U_THUNG', N'Thùng', 12, 1540000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Kem đánh răng PS 123', 'U_TUYP', N'Tuýp', 35000, 1, 'U_HOP', N'Hộp', 12, 400000, 'U_THUNG', N'Thùng', 24, 790000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sữa tắm Lifebuoy bảo vệ', 'U_CHAI', N'Chai', 160000, 1, 'U_THUNG', N'Thùng', 12, 1820000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sữa rửa mặt Hazeline', 'U_TUYP', N'Tuýp', 55000, 1, 'U_THUNG', N'Thùng', 24, 1260000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Giấy vệ sinh Bless You lốc 10', 'U_LOC', N'Lốc', 85000, 1, 'U_THUNG', N'Thùng', 6, 490000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Khăn ướt Baby gói 100 tờ', 'U_GOI', N'Gói', 40000, 1, 'U_THUNG', N'Thùng', 24, 920000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bánh Oreo nhân socola', 'U_GOI', N'Gói', 15000, 1, 'U_THUNG', N'Thùng', 24, 345000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Kẹo Alpenliebe caramen', 'U_GOI', N'Gói', 12000, 1, 'U_THUNG', N'Thùng', 24, 275000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bánh Cosy quy sữa 240g', 'U_HOP', N'Hộp', 25000, 1, 'U_THUNG', N'Thùng', 24, 575000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Snack khoai tây Pringles', 'U_LON', N'Lon', 35000, 1, 'U_THUNG', N'Thùng', 12, 400000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Socola KitKat trà xanh', 'U_THANH', N'Thanh', 20000, 1, 'U_HOP', N'Hộp', 24, 460000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bánh ChocoPie Orion hộp 6', 'U_CAI', N'Cái', 6000, 6, 'U_HOP', N'Hộp', 6, 32000, 'U_THUNG', N'Thùng', 72, 360000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bánh gạo One One phô mai', 'U_GOI', N'Gói', 22000, 1, 'U_THUNG', N'Thùng', 12, 250000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Kẹo dẻo Haribo gấu', 'U_GOI', N'Gói', 18000, 1, 'U_THUNG', N'Thùng', 24, 415000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Thịt ba chỉ bò Mỹ 500g', 'U_KHAY', N'Khay', 150000, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Ức gà phi lê 500g', 'U_KHAY', N'Khay', 45000, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Cá hồi phi lê tươi 200g', 'U_KHAY', N'Khay', 120000, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Trứng gà ta vỉ 10 quả', 'U_QUA', N'Quả', 5000, 10, 'U_VI', N'Vỉ', 10, 45000, 'U_THUNG', N'Thùng', 300, 1250000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Khoai tây vàng túi 1kg', 'U_TUI', N'Túi', 25000, 1, 'U_BAO', N'Bao', 10, 240000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sữa tươi TH True Milk ít đường 1L', 'U_HOP', N'Hộp', 37000, 1, 'U_THUNG', N'Thùng', 12, 425000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sữa tươi Vinamilk không đường 1L', 'U_HOP', N'Hộp', 36000, 1, 'U_THUNG', N'Thùng', 12, 415000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sữa chua Vinamilk có đường lốc 4 hộp', 'U_HOP', N'Hộp', 9000, 4, 'U_LOC', N'Lốc', 4, 32000, 'U_THUNG', N'Thùng', 48, 360000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Phô mai Con Bò Cười hộp 8 miếng', 'U_HOP', N'Hộp', 42000, 1, 'U_THUNG', N'Thùng', 24, 965000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sữa đặc Ngôi Sao Phương Nam lon 380g', 'U_LON', N'Lon', 26000, 1, 'U_THUNG', N'Thùng', 48, 1200000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sữa hạt Vinamilk óc chó 180ml lốc 4 hộp', 'U_HOP', N'Hộp', 10500, 4, 'U_LOC', N'Lốc', 4, 39000, 'U_THUNG', N'Thùng', 48, 450000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bơ lạt Anchor hộp 227g', 'U_HOP', N'Hộp', 89000, 1, 'U_THUNG', N'Thùng', 12, 1015000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sữa chua uống Yakult lốc 5 chai', 'U_CHAI', N'Chai', 6500, 5, 'U_LOC', N'Lốc', 5, 30000, 'U_THUNG', N'Thùng', 50, 290000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Xúc xích Đức Việt gói 500g', 'U_GOI', N'Gói', 78000, 1, 'U_THUNG', N'Thùng', 10, 741000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Cá viên CP gói 500g', 'U_GOI', N'Gói', 65000, 1, 'U_THUNG', N'Thùng', 10, 618000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Chả giò Cầu Tre hải sản 500g', 'U_GOI', N'Gói', 72000, 1, 'U_THUNG', N'Thùng', 10, 684000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Gà viên chiên CP 400g', 'U_GOI', N'Gói', 59000, 1, 'U_THUNG', N'Thùng', 10, 560000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bò viên Vissan gói 500g', 'U_GOI', N'Gói', 76000, 1, 'U_THUNG', N'Thùng', 10, 722000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Há cảo tôm đông lạnh 300g', 'U_GOI', N'Gói', 68000, 1, 'U_THUNG', N'Thùng', 10, 646000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Khoai tây chiên đông lạnh 1kg', 'U_TUI', N'Túi', 85000, 1, 'U_THUNG', N'Thùng', 10, 805000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Pizza hải sản đông lạnh 300g', 'U_HOP', N'Hộp', 69000, 1, 'U_THUNG', N'Thùng', 10, 655000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Khăn giấy Pulppy 2 lớp 10 cuộn', 'U_LOC', N'Lốc', 62000, 1, 'U_THUNG', N'Thùng', 6, 355000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Túi rác tự hủy 3 cuộn', 'U_GOI', N'Gói', 35000, 1, 'U_THUNG', N'Thùng', 20, 665000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Màng bọc thực phẩm Ringo 30cm x 30m', 'U_CUON', N'Cuộn', 29000, 1, 'U_THUNG', N'Thùng', 24, 660000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Hộp nhựa Lock Lock 1L', 'U_CAI', N'Cái', 55000, 1, 'U_THUNG', N'Thùng', 12, 625000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Giấy bạc nướng thực phẩm 30cm x 5m', 'U_CUON', N'Cuộn', 42000, 1, 'U_THUNG', N'Thùng', 24, 965000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Ly giấy dùng một lần 50 cái', 'U_GOI', N'Gói', 36000, 1, 'U_THUNG', N'Thùng', 20, 690000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Ống hút giấy pack 100 cái', 'U_GOI', N'Gói', 28000, 1, 'U_THUNG', N'Thùng', 20, 535000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Túi zipper đựng thực phẩm 20 túi', 'U_GOI', N'Gói', 48000, 1, 'U_THUNG', N'Thùng', 24, 1100000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Kem đánh răng P/S trà xanh 180g', 'U_TUYP', N'Tuýp', 32000, 1, 'U_HOP', N'Hộp', 12, 365000, 'U_THUNG', N'Thùng', 24, 730000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bàn chải Oral-B mềm', 'U_CAI', N'Cái', 28000, 1, 'U_HOP', N'Hộp', 12, 320000, 'U_THUNG', N'Thùng', 48, 1280000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Nước súc miệng Listerine 500ml', 'U_CHAI', N'Chai', 95000, 1, 'U_THUNG', N'Thùng', 12, 1080000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Dao cạo râu Gillette Blue 3', 'U_CAI', N'Cái', 45000, 1, 'U_HOP', N'Hộp', 12, 515000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Lăn khử mùi Nivea Men 50ml', 'U_CAI', N'Cái', 72000, 1, 'U_THUNG', N'Thùng', 24, 1640000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Dầu gội Dove phục hồi hư tổn 640g', 'U_CHAI', N'Chai', 145000, 1, 'U_THUNG', N'Thùng', 12, 1650000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sữa rửa mặt Simple 150ml', 'U_TUYP', N'Tuýp', 115000, 1, 'U_THUNG', N'Thùng', 24, 2620000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bông tẩy trang Jomi 120 miếng', 'U_GOI', N'Gói', 39000, 1, 'U_THUNG', N'Thùng', 24, 890000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Tã Bobby quần size M 68 miếng', 'U_GOI', N'Gói', 285000, 1, 'U_THUNG', N'Thùng', 4, 1100000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Khăn ướt Mamamy 100 tờ', 'U_GOI', N'Gói', 35000, 1, 'U_THUNG', N'Thùng', 24, 805000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sữa tắm gội Johnson Baby 500ml', 'U_CHAI', N'Chai', 89000, 1, 'U_THUNG', N'Thùng', 12, 1015000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Phấn rôm Johnson Baby 200g', 'U_HOP', N'Hộp', 55000, 1, 'U_THUNG', N'Thùng', 24, 1250000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bình sữa Pigeon 240ml', 'U_CAI', N'Cái', 165000, 1, 'U_THUNG', N'Thùng', 12, 1880000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Nước giặt Dnee hồng 3L', 'U_CHAI', N'Chai', 165000, 1, 'U_THUNG', N'Thùng', 4, 630000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Kem chống hăm Bepanthen 30g', 'U_TUYP', N'Tuýp', 115000, 1, 'U_HOP', N'Hộp', 12, 1310000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bột ăn dặm Heinz rau củ 200g', 'U_HOP', N'Hộp', 89000, 1, 'U_THUNG', N'Thùng', 12, 1015000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Tập Campus 200 trang', 'U_CUON', N'Cuốn', 16000, 1, 'U_LOC', N'Lốc', 10, 155000, 'U_THUNG', N'Thùng', 100, 1500000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bút bi Thiên Long TL-027', 'U_CAY', N'Cây', 5000, 1, 'U_HOP', N'Hộp', 20, 95000, 'U_THUNG', N'Thùng', 200, 900000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Giấy note Pronoti 3x3', 'U_XAP', N'Xấp', 18000, 1, 'U_HOP', N'Hộp', 12, 205000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bìa hồ sơ A4 Plus', 'U_CAI', N'Cái', 12000, 1, 'U_XAP', N'Xấp', 10, 115000, 'U_THUNG', N'Thùng', 100, 1100000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Băng keo trong 5cm', 'U_CUON', N'Cuộn', 15000, 1, 'U_LOC', N'Lốc', 6, 87000, 'U_THUNG', N'Thùng', 72, 1020000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Kéo học sinh Deli 17cm', 'U_CAI', N'Cái', 25000, 1, 'U_HOP', N'Hộp', 12, 285000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bút highlight Stabilo vàng', 'U_CAY', N'Cây', 22000, 1, 'U_HOP', N'Hộp', 10, 210000, 'U_THUNG', N'Thùng', 100, 2050000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'File lá A4 20 túi', 'U_CUON', N'Cuốn', 32000, 1, 'U_THUNG', N'Thùng', 24, 735000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Hạt mèo Whiskas cá ngừ 1.2kg', 'U_TUI', N'Túi', 145000, 1, 'U_THUNG', N'Thùng', 6, 830000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Hạt chó Pedigree vị bò 1.5kg', 'U_TUI', N'Túi', 135000, 1, 'U_THUNG', N'Thùng', 6, 770000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Pate mèo Me-O cá ngừ 80g', 'U_GOI', N'Gói', 15000, 1, 'U_HOP', N'Hộp', 12, 170000, 'U_THUNG', N'Thùng', 48, 680000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Cát vệ sinh mèo Min 5L', 'U_TUI', N'Túi', 79000, 1, 'U_THUNG', N'Thùng', 6, 450000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Sữa tắm chó mèo Joyce Dolls 400ml', 'U_CHAI', N'Chai', 115000, 1, 'U_THUNG', N'Thùng', 12, 1310000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Snack thưởng mèo Ciao Churu 4 thanh', 'U_THANH', N'Thanh', 15000, 4, 'U_GOI', N'Gói', 4, 55000, 'U_THUNG', N'Thùng', 48, 620000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Pate chó SmartHeart bò 130g', 'U_GOI', N'Gói', 22000, 1, 'U_HOP', N'Hộp', 12, 250000, 'U_THUNG', N'Thùng', 48, 1000000, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Vòng cổ chó mèo size M', 'U_CAI', N'Cái', 35000, 1, 'U_HOP', N'Hộp', 12, 400000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Rau cải hữu cơ Vinamit 300g', 'U_GOI', N'Gói', 28000, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Cà rốt hữu cơ Đà Lạt 500g', 'U_GOI', N'Gói', 35000, 1, 'U_TUI', N'Túi', 10, 335000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Yến mạch Quaker nguyên chất 400g', 'U_HOP', N'Hộp', 79000, 1, 'U_THUNG', N'Thùng', 12, 900000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Hạt chia Úc Organic 250g', 'U_GOI', N'Gói', 99000, 1, 'U_THUNG', N'Thùng', 12, 1130000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Gạo lứt hữu cơ Hoa Sữa 1kg', 'U_TUI', N'Túi', 65000, 1, 'U_THUNG', N'Thùng', 10, 620000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Mật ong hoa nhãn nguyên chất 500ml', 'U_CHAI', N'Chai', 145000, 1, 'U_THUNG', N'Thùng', 12, 1650000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bột cacao nguyên chất 200g', 'U_GOI', N'Gói', 85000, 1, 'U_THUNG', N'Thùng', 12, 970000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Hạnh nhân rang bơ 250g', 'U_GOI', N'Gói', 125000, 1, 'U_THUNG', N'Thùng', 12, 1425000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Cá hộp Ba Cô Gái sốt cà 155g', 'U_HOP', N'Hộp', 25000, 1, 'U_THUNG', N'Thùng', 48, 1150000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Thịt hộp Spam Classic 340g', 'U_HOP', N'Hộp', 98000, 1, 'U_THUNG', N'Thùng', 24, 2250000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Pate gan Vissan hộp 170g', 'U_HOP', N'Hộp', 32000, 1, 'U_THUNG', N'Thùng', 48, 1470000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Cháo ăn liền Gấu Đỏ thịt bằm 50g', 'U_GOI', N'Gói', 7000, 1, 'U_THUNG', N'Thùng', 50, 335000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Mì ly Modern lẩu Thái 65g', 'U_LY', N'Ly', 11000, 1, 'U_THUNG', N'Thùng', 24, 255000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bún bò Huế Vifon tô 120g', 'U_TO', N'Tô', 18000, 1, 'U_THUNG', N'Thùng', 12, 205000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Cơm cháy chà bông hộp 200g', 'U_HOP', N'Hộp', 55000, 1, 'U_THUNG', N'Thùng', 24, 1250000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Súp nui ăn liền Knorr 60g', 'U_GOI', N'Gói', 15000, 1, 'U_THUNG', N'Thùng', 30, 430000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Nước lau sàn Sunlight hương hoa 1kg', 'U_CHAI', N'Chai', 45000, 1, 'U_THUNG', N'Thùng', 12, 515000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Nước tẩy Javel Gift 1L', 'U_CHAI', N'Chai', 26000, 1, 'U_THUNG', N'Thùng', 12, 295000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Xịt phòng Glade hương hoa 280ml', 'U_CHAI', N'Chai', 65000, 1, 'U_THUNG', N'Thùng', 12, 740000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Nước lau kính Gift 580ml', 'U_CHAI', N'Chai', 32000, 1, 'U_THUNG', N'Thùng', 12, 365000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Bình xịt côn trùng Raid 600ml', 'U_CHAI', N'Chai', 72000, 1, 'U_THUNG', N'Thùng', 12, 820000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Miếng rửa chén Scotch Brite 3 miếng', 'U_GOI', N'Gói', 25000, 1, 'U_THUNG', N'Thùng', 24, 575000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Cây lau nhà 360 độ bộ', 'U_BO', N'Bộ', 185000, 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    CFG_PRODUCT_UOM_BY_NAME(N'Nước thông cống Hando 1L', 'U_CHAI', N'Chai', 58000, 1, 'U_THUNG', N'Thùng', 12, 660000, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL);
    COMMIT;
END;
/


-- Kiem tra nhanh:
-- SELECT p.product_id, p.product_name, u.unit_name AS base_unit, pu.unit_id, uu.unit_name, pu.conversion_rate_to_base, pu.selling_price
-- FROM PRODUCTS p
-- JOIN UNITS u ON u.unit_id = p.base_unit_id
-- JOIN PRODUCT_UNITS pu ON pu.product_id = p.product_id AND NVL(pu.is_deleted,0)=0
-- JOIN UNITS uu ON uu.unit_id = pu.unit_id
-- ORDER BY p.product_id, pu.is_base_unit DESC, pu.conversion_rate_to_base;


-- ==========================================================
-- Xóa cấu hình đơn vị dư cho Nước mắm Nam Ngư 750ml
-- Chỉ giữ lại đơn vị bán: Chai
-- ==========================================================

UPDATE PRODUCT_UNITS pu
SET pu.is_deleted = 1
WHERE pu.product_id = 'SP000005'
  AND pu.unit_id IN ('U_CAI', 'U_THUNG');

MERGE INTO UNITS u
USING (
    SELECT 'U_CHAI' AS unit_id, N'Chai' AS unit_name FROM dual
) src
ON (u.unit_id = src.unit_id)
WHEN MATCHED THEN
    UPDATE SET u.unit_name = src.unit_name,
               u.is_deleted = 0
WHEN NOT MATCHED THEN
    INSERT (unit_id, unit_name, is_deleted)
    VALUES (src.unit_id, src.unit_name, 0);

MERGE INTO PRODUCT_UNITS pu
USING (
    SELECT 'SP000005' AS product_id,
           'U_CHAI' AS unit_id,
           1 AS conversion_rate_to_base,
           35000 AS selling_price,
           1 AS is_base_unit
    FROM dual
) src
ON (pu.product_id = src.product_id AND pu.unit_id = src.unit_id)
WHEN MATCHED THEN
    UPDATE SET pu.conversion_rate_to_base = src.conversion_rate_to_base,
               pu.selling_price = src.selling_price,
               pu.is_base_unit = src.is_base_unit,
               pu.is_deleted = 0
WHEN NOT MATCHED THEN
    INSERT (
        product_id,
        unit_id,
        conversion_rate_to_base,
        selling_price,
        is_base_unit,
        is_deleted
    )
    VALUES (
        src.product_id,
        src.unit_id,
        src.conversion_rate_to_base,
        src.selling_price,
        src.is_base_unit,
        0
    );

UPDATE PRODUCTS
SET base_unit_id = 'U_CHAI'
WHERE product_id = 'SP000005';

COMMIT;


-- ==========================================================
-- FIX TOÀN BỘ ĐƠN VỊ "CÁI" TRONG PRODUCT_UNITS
-- Chuyển U_CAI thành đơn vị thực tế hơn theo tên sản phẩm:
-- Chai / Lon / Gói / Hộp / Túi / Tuýp / Bình / Lọ / Chiếc
-- ==========================================================

CREATE OR REPLACE PROCEDURE REPLACE_PRODUCT_UOM (
    p_product_id    IN VARCHAR2,
    p_old_unit_id   IN VARCHAR2,
    p_new_unit_id   IN VARCHAR2,
    p_new_unit_name IN NVARCHAR2
) IS
    v_rate       NUMBER(18, 4);
    v_price      NUMBER(15, 2);
    v_is_base    NUMBER(1);
    v_exists     NUMBER;
BEGIN
    -- Đảm bảo đơn vị mới tồn tại
    MERGE INTO UNITS u
    USING (
        SELECT p_new_unit_id AS unit_id,
               p_new_unit_name AS unit_name
        FROM dual
    ) src
    ON (u.unit_id = src.unit_id)
    WHEN MATCHED THEN
        UPDATE SET u.unit_name = src.unit_name,
                   u.is_deleted = 0
    WHEN NOT MATCHED THEN
        INSERT (unit_id, unit_name, is_deleted)
        VALUES (src.unit_id, src.unit_name, 0);

    -- Lấy cấu hình cũ của U_CAI
    BEGIN
        SELECT conversion_rate_to_base,
               selling_price,
               NVL(is_base_unit, 0)
        INTO v_rate, v_price, v_is_base
        FROM PRODUCT_UNITS
        WHERE product_id = p_product_id
          AND unit_id = p_old_unit_id
          AND NVL(is_deleted, 0) = 0;
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            RETURN;
    END;

    -- Nếu đơn vị mới đã tồn tại cho sản phẩm thì cập nhật lại
    SELECT COUNT(*)
    INTO v_exists
    FROM PRODUCT_UNITS
    WHERE product_id = p_product_id
      AND unit_id = p_new_unit_id;

    IF v_exists > 0 THEN
        UPDATE PRODUCT_UNITS
        SET conversion_rate_to_base = NVL(conversion_rate_to_base, v_rate),
            selling_price = NVL(selling_price, v_price),
            is_deleted = 0
        WHERE product_id = p_product_id
          AND unit_id = p_new_unit_id;
    ELSE
        INSERT INTO PRODUCT_UNITS (
            product_id,
            unit_id,
            conversion_rate_to_base,
            selling_price,
            is_base_unit,
            is_deleted
        )
        VALUES (
            p_product_id,
            p_new_unit_id,
            v_rate,
            v_price,
            0,
            0
        );
    END IF;

    -- Nếu U_CAI đang là đơn vị gốc thì chuyển đơn vị gốc sang đơn vị mới
    IF v_is_base = 1 THEN
        UPDATE PRODUCT_UNITS
        SET is_base_unit = 0
        WHERE product_id = p_product_id;

        UPDATE PRODUCT_UNITS
        SET is_base_unit = 1
        WHERE product_id = p_product_id
          AND unit_id = p_new_unit_id;

        UPDATE PRODUCTS
        SET base_unit_id = p_new_unit_id
        WHERE product_id = p_product_id;
    END IF;

    -- Ẩn U_CAI khỏi sản phẩm
    UPDATE PRODUCT_UNITS
    SET is_deleted = 1,
        is_base_unit = 0
    WHERE product_id = p_product_id
      AND unit_id = p_old_unit_id;
END;
/


-- ==========================================================
-- Tự động map U_CAI sang đơn vị thực tế theo tên sản phẩm
-- ==========================================================

BEGIN
    FOR r IN (
        SELECT p.product_id,
               LOWER(p.product_name) AS product_name
        FROM PRODUCTS p
        JOIN PRODUCT_UNITS pu
          ON pu.product_id = p.product_id
        WHERE pu.unit_id = 'U_CAI'
          AND NVL(pu.is_deleted, 0) = 0
          AND NVL(p.is_deleted, 0) = 0
    ) LOOP

        IF r.product_name LIKE N'%nước mắm%'
           OR r.product_name LIKE N'%nước tương%'
           OR r.product_name LIKE N'%dầu ăn%'
           OR r.product_name LIKE N'%nước súc miệng%'
           OR r.product_name LIKE N'%dầu gội%'
           OR r.product_name LIKE N'%sữa tắm%'
           OR r.product_name LIKE N'%nước giặt%'
           OR r.product_name LIKE N'%chai%'
        THEN
            REPLACE_PRODUCT_UOM(r.product_id, 'U_CAI', 'U_CHAI', N'Chai');

        ELSIF r.product_name LIKE N'%lon%' THEN
            REPLACE_PRODUCT_UOM(r.product_id, 'U_CAI', 'U_LON', N'Lon');

        ELSIF r.product_name LIKE N'%mì%'
           OR r.product_name LIKE N'%miến%'
           OR r.product_name LIKE N'%phở%'
           OR r.product_name LIKE N'%cháo%'
           OR r.product_name LIKE N'%súp%'
           OR r.product_name LIKE N'%snack%'
           OR r.product_name LIKE N'%gói%'
        THEN
            REPLACE_PRODUCT_UOM(r.product_id, 'U_CAI', 'U_GOI', N'Gói');

        ELSIF r.product_name LIKE N'%hộp%'
           OR r.product_name LIKE N'%sữa đặc%'
           OR r.product_name LIKE N'%phô mai%'
           OR r.product_name LIKE N'%bơ%'
        THEN
            REPLACE_PRODUCT_UOM(r.product_id, 'U_CAI', 'U_HOP', N'Hộp');

        ELSIF r.product_name LIKE N'%túi%'
           OR r.product_name LIKE N'%gạo%'
           OR r.product_name LIKE N'%đường%'
           OR r.product_name LIKE N'%bột%'
        THEN
            REPLACE_PRODUCT_UOM(r.product_id, 'U_CAI', 'U_TUI', N'Túi');

        ELSIF r.product_name LIKE N'%kem đánh răng%'
           OR r.product_name LIKE N'%kem chống hăm%'
           OR r.product_name LIKE N'%tuýp%'
           OR r.product_name LIKE N'%tube%'
        THEN
            REPLACE_PRODUCT_UOM(r.product_id, 'U_CAI', 'U_TUYP', N'Tuýp');

        ELSIF r.product_name LIKE N'%bình%' THEN
            REPLACE_PRODUCT_UOM(r.product_id, 'U_CAI', 'U_BINH', N'Bình');

        ELSIF r.product_name LIKE N'%lăn khử mùi%'
           OR r.product_name LIKE N'%phấn rôm%'
        THEN
            REPLACE_PRODUCT_UOM(r.product_id, 'U_CAI', 'U_LO', N'Lọ');

        ELSIF r.product_name LIKE N'%bàn chải%'
           OR r.product_name LIKE N'%dao cạo%'
        THEN
            REPLACE_PRODUCT_UOM(r.product_id, 'U_CAI', 'U_CHIEC', N'Chiếc');

        ELSE
            -- Fallback cuối cùng: không dùng "Cái", đổi sang "Chiếc"
            REPLACE_PRODUCT_UOM(r.product_id, 'U_CAI', 'U_CHIEC', N'Chiếc');
        END IF;

    END LOOP;
END;
/

SELECT p.product_id,
       p.product_name,
       pu.unit_id,
       u.unit_name,
       pu.conversion_rate_to_base,
       pu.selling_price,
       pu.is_base_unit,
       pu.is_deleted
FROM PRODUCTS p
JOIN PRODUCT_UNITS pu
  ON pu.product_id = p.product_id
JOIN UNITS u
  ON u.unit_id = pu.unit_id
WHERE pu.unit_id = 'U_CAI'
  AND NVL(pu.is_deleted, 0) = 0
ORDER BY p.product_id;



