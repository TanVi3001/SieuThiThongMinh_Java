-- =========================================================
-- 20260526_import_products_from_csv_no_duplicate.sql
-- Nguồn import: products1_1m_with_image_path(3).csv
-- Mục tiêu:
-- 1) Import lại sản phẩm theo file CSV, KHÔNG tạo trùng tên.
-- 2) Xóa mềm sản phẩm active chưa có image_path.
-- 3) Ưu tiên giữ sản phẩm có mã trong file CSV và có ảnh.
-- 4) Đồng bộ STORE_PRODUCTS + INVENTORY để app tìm/thêm được ảnh và tồn kho.
--
-- Lưu ý ảnh:
-- image_path trong CSV đang dạng: products/ten_anh.png
-- Hãy đặt ảnh ở: src/main/resources/view/image/products/
-- =========================================================

-- =========================================================
-- A. BACKUP AN TOÀN
-- =========================================================
BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE PRODUCTS_BK_CSV_IMPORT_20260526 AS SELECT * FROM PRODUCTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE INVENTORY_BK_CSV_IMPORT_20260526 AS SELECT * FROM INVENTORY';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE STORE_PRODUCTS_BK_CSV_IMPORT_20260526 AS SELECT * FROM STORE_PRODUCTS';
EXCEPTION
    WHEN OTHERS THEN
        IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- =========================================================
-- B. ĐẢM BẢO CATEGORIES CÓ VAT_RATE / STATUS
-- =========================================================
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME = 'CATEGORIES'
      AND COLUMN_NAME = 'VAT_RATE';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE CATEGORIES ADD VAT_RATE NUMBER(5,2)';
    END IF;

    SELECT COUNT(*) INTO v_count
    FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME = 'CATEGORIES'
      AND COLUMN_NAME = 'STATUS';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE CATEGORIES ADD STATUS NVARCHAR2(30) DEFAULT N''Hoạt động''';
    END IF;
END;
/

-- =========================================================
-- C. ĐỒNG BỘ DANH MỤC THEO CSV
-- =========================================================
MERGE INTO CATEGORIES c
USING (
    SELECT 'CAT001' category_id, N'Thực phẩm khô' category_name, 8 vat_rate, N'Gạo, mì, gia vị, dầu ăn, nước mắm, thực phẩm đóng gói' description FROM dual UNION ALL
    SELECT 'CAT002' category_id, N'Đồ uống & Giải khát' category_name, 10 vat_rate, N'Nước suối, nước ngọt, trà, cà phê, nước tăng lực' description FROM dual UNION ALL
    SELECT 'CAT003' category_id, N'Hóa mỹ phẩm' category_name, 10 vat_rate, N'Dầu gội, sữa tắm, kem đánh răng, giấy vệ sinh, khăn ướt' description FROM dual UNION ALL
    SELECT 'CAT004' category_id, N'Bánh kẹo' category_name, 8 vat_rate, N'Bánh quy, kẹo, snack, socola, đồ ăn vặt' description FROM dual UNION ALL
    SELECT 'CAT005' category_id, N'Thực phẩm tươi sống' category_name, 5 vat_rate, N'Thịt, cá, trứng, rau củ, thực phẩm tươi' description FROM dual
) src
ON (c.category_id = src.category_id)
WHEN MATCHED THEN UPDATE SET
    c.category_name = src.category_name,
    c.description = src.description,
    c.vat_rate = src.vat_rate,
    c.status = N'Hoạt động',
    c.is_deleted = 0
WHEN NOT MATCHED THEN INSERT (
    category_id, category_name, description, vat_rate, status, is_deleted
) VALUES (
    src.category_id, src.category_name, src.description, src.vat_rate, N'Hoạt động', 0
);

-- =========================================================
-- D. XÓA MỀM SẢN PHẨM ACTIVE CHƯA CÓ ẢNH
-- =========================================================
UPDATE PRODUCTS p
SET p.is_deleted = 1
WHERE NVL(p.is_deleted, 0) = 0
  AND (
        p.image_path IS NULL
     OR TRIM(p.image_path) IS NULL
     OR LOWER(TRIM(p.image_path)) IN ('-', 'null', 'none', 'no_image', 'no-image', 'noimage', 'image_not_found')
  );

-- =========================================================
-- E. IMPORT SẢN PHẨM TỪ CSV
-- Không insert trùng tên:
-- - Nếu trùng product_id thì UPDATE.
-- - Nếu product_id chưa có nhưng tên đã tồn tại active thì sau bước F sẽ chỉ giữ bản CSV.
-- =========================================================
MERGE INTO PRODUCTS p
USING (
    SELECT 'SP000001' product_id, N'Mì Hảo Hảo tôm chua cay' product_name, 5000 base_price, 50 quantity, 'CAT001' category_id, 'products/mi_hao_hao_tom_chua_cay.png' image_path FROM dual UNION ALL
    SELECT 'SP000002' product_id, N'Mì Omachi sườn hầm ngũ quả' product_name, 8000 base_price, 50 quantity, 'CAT001' category_id, 'products/mi_omachi_suon_ham_ngu_qua.png' image_path FROM dual UNION ALL
    SELECT 'SP000003' product_id, N'Phở bò Vifon túi' product_name, 7500 base_price, 50 quantity, 'CAT001' category_id, 'products/pho_bo_vifon_tui.png' image_path FROM dual UNION ALL
    SELECT 'SP000004' product_id, N'Gạo ST25 Ông Cua túi 5kg' product_name, 180000 base_price, 50 quantity, 'CAT001' category_id, 'products/gao_st25_ong_cua_tui_5kg.png' image_path FROM dual UNION ALL
    SELECT 'SP000005' product_id, N'Nước mắm Nam Ngư 750ml' product_name, 35000 base_price, 50 quantity, 'CAT001' category_id, 'products/nuoc_mam_nam_ngu_750ml.png' image_path FROM dual UNION ALL
    SELECT 'SP000006' product_id, N'Nước mắm Chinsu cá hồi' product_name, 45000 base_price, 50 quantity, 'CAT001' category_id, 'products/nuoc_mam_chinsu_ca_hoi.png' image_path FROM dual UNION ALL
    SELECT 'SP000007' product_id, N'Dầu ăn Tường An 2L' product_name, 52000 base_price, 50 quantity, 'CAT001' category_id, 'products/dau_an_tuong_an_2l.png' image_path FROM dual UNION ALL
    SELECT 'SP000008' product_id, N'Dầu ăn Simply 2L' product_name, 60000 base_price, 50 quantity, 'CAT001' category_id, 'products/dau_an_simply_2l.png' image_path FROM dual UNION ALL
    SELECT 'SP000009' product_id, N'Dầu ăn Happy Koki 2L' product_name, 60000 base_price, 50 quantity, 'CAT001' category_id, 'products/dau_an_happy_koki_2l.png' image_path FROM dual UNION ALL
    SELECT 'SP000010' product_id, N'Đường tinh luyện Biên Hòa 1kg' product_name, 28000 base_price, 50 quantity, 'CAT001' category_id, 'products/duong_tinh_luyen_bien_hoa_1kg.png' image_path FROM dual UNION ALL
    SELECT 'SP000011' product_id, N'Sữa đặc Ông Thọ đỏ lon' product_name, 22000 base_price, 50 quantity, 'CAT001' category_id, 'products/sua_dac_ong_tho_do_lon.png' image_path FROM dual UNION ALL
    SELECT 'SP000012' product_id, N'Hạt nêm Knorr thịt thăn 400g' product_name, 38000 base_price, 50 quantity, 'CAT001' category_id, 'products/hat_nem_knorr_thit_than_400g.png' image_path FROM dual UNION ALL
    SELECT 'SP000013' product_id, N'Tương ớt Chinsu 250g' product_name, 15000 base_price, 50 quantity, 'CAT001' category_id, 'products/tuong_ot_chinsu_250g.png' image_path FROM dual UNION ALL
    SELECT 'SP000101' product_id, N'Nước suối Aquafina 500ml' product_name, 5000 base_price, 50 quantity, 'CAT002' category_id, 'products/nuoc_suoi_aquafina_500ml.png' image_path FROM dual UNION ALL
    SELECT 'SP000102' product_id, N'Nước khoáng Lavie 500ml' product_name, 5000 base_price, 50 quantity, 'CAT002' category_id, 'products/nuoc_khoang_lavie_500ml.png' image_path FROM dual UNION ALL
    SELECT 'SP000103' product_id, N'Coca Cola lon 330ml' product_name, 10000 base_price, 50 quantity, 'CAT002' category_id, 'products/coca_cola_lon_330ml.png' image_path FROM dual UNION ALL
    SELECT 'SP000104' product_id, N'Pepsi lon 330ml' product_name, 10000 base_price, 50 quantity, 'CAT002' category_id, 'products/pepsi_lon_330ml.png' image_path FROM dual UNION ALL
    SELECT 'SP000105' product_id, N'Sprite chai 1.5L' product_name, 20000 base_price, 50 quantity, 'CAT002' category_id, 'products/sprite_chai_1_5l.png' image_path FROM dual UNION ALL
    SELECT 'SP000106' product_id, N'Trà Ô long Tea Plus' product_name, 10000 base_price, 50 quantity, 'CAT002' category_id, 'products/tra_o_long_tea_plus.png' image_path FROM dual UNION ALL
    SELECT 'SP000107' product_id, N'Nước tăng lực Redbull lon' product_name, 15000 base_price, 50 quantity, 'CAT002' category_id, 'products/nuoc_tang_luc_redbull_lon.png' image_path FROM dual UNION ALL
    SELECT 'SP000108' product_id, N'Cà phê lon Birdy xanh' product_name, 12000 base_price, 50 quantity, 'CAT002' category_id, 'products/ca_phe_lon_birdy_xanh.png' image_path FROM dual UNION ALL
    SELECT 'SP000201' product_id, N'Dầu gội Clear Men 630g' product_name, 145000 base_price, 50 quantity, 'CAT003' category_id, 'products/dau_goi_clear_men_630g.png' image_path FROM dual UNION ALL
    SELECT 'SP000202' product_id, N'Dầu gội Sunsilk mềm mượt' product_name, 135000 base_price, 50 quantity, 'CAT003' category_id, 'products/dau_goi_sunsilk_mem_muot.png' image_path FROM dual UNION ALL
    SELECT 'SP000203' product_id, N'Kem đánh răng PS 123' product_name, 35000 base_price, 50 quantity, 'CAT003' category_id, 'products/kem_danh_rang_ps_123.png' image_path FROM dual UNION ALL
    SELECT 'SP000204' product_id, N'Sữa tắm Lifebuoy bảo vệ' product_name, 160000 base_price, 30 quantity, 'CAT003' category_id, 'products/sua_tam_lifebuoy_bao_ve.png' image_path FROM dual UNION ALL
    SELECT 'SP000205' product_id, N'Sữa rửa mặt Hazeline' product_name, 55000 base_price, 55 quantity, 'CAT003' category_id, 'products/sua_rua_mat_hazeline.png' image_path FROM dual UNION ALL
    SELECT 'SP000206' product_id, N'Giấy vệ sinh Bless You lốc 10' product_name, 85000 base_price, 50 quantity, 'CAT003' category_id, 'products/giay_ve_sinh_bless_you_loc_10.png' image_path FROM dual UNION ALL
    SELECT 'SP000207' product_id, N'Khăn ướt Baby gói 100 tờ' product_name, 40000 base_price, 50 quantity, 'CAT003' category_id, 'products/khan_uot_baby_goi_100_to.png' image_path FROM dual UNION ALL
    SELECT 'SP000301' product_id, N'Bánh Oreo nhân socola' product_name, 15000 base_price, 50 quantity, 'CAT004' category_id, 'products/banh_oreo_nhan_socola.png' image_path FROM dual UNION ALL
    SELECT 'SP000302' product_id, N'Kẹo Alpenliebe caramen' product_name, 12000 base_price, 50 quantity, 'CAT004' category_id, 'products/keo_alpenliebe_caramen.png' image_path FROM dual UNION ALL
    SELECT 'SP000303' product_id, N'Bánh Cosy quy sữa 240g' product_name, 25000 base_price, 50 quantity, 'CAT004' category_id, 'products/banh_cosy_quy_sua_240g.png' image_path FROM dual UNION ALL
    SELECT 'SP000304' product_id, N'Snack khoai tây Pringles' product_name, 35000 base_price, 50 quantity, 'CAT004' category_id, 'products/snack_khoai_tay_pringles.png' image_path FROM dual UNION ALL
    SELECT 'SP000305' product_id, N'Socola KitKat trà xanh' product_name, 20000 base_price, 50 quantity, 'CAT004' category_id, 'products/socola_kitkat_tra_xanh.png' image_path FROM dual UNION ALL
    SELECT 'SP000306' product_id, N'Bánh ChocoPie Orion hộp 6' product_name, 32000 base_price, 50 quantity, 'CAT004' category_id, 'products/banh_chocopie_orion_hop_6.png' image_path FROM dual UNION ALL
    SELECT 'SP000307' product_id, N'Bánh gạo One One phô mai' product_name, 22000 base_price, 50 quantity, 'CAT004' category_id, 'products/banh_gao_one_one_pho_mai.png' image_path FROM dual UNION ALL
    SELECT 'SP000308' product_id, N'Kẹo dẻo Haribo gấu' product_name, 18000 base_price, 50 quantity, 'CAT004' category_id, 'products/keo_deo_haribo_gau.png' image_path FROM dual UNION ALL
    SELECT 'SP000401' product_id, N'Thịt ba chỉ bò Mỹ 500g' product_name, 150000 base_price, 20 quantity, 'CAT005' category_id, 'products/thit_ba_chi_bo_my_500g.png' image_path FROM dual UNION ALL
    SELECT 'SP000402' product_id, N'Ức gà phi lê 500g' product_name, 45000 base_price, 25 quantity, 'CAT005' category_id, 'products/uc_ga_phi_le_500g.png' image_path FROM dual UNION ALL
    SELECT 'SP000403' product_id, N'Cá hồi phi lê tươi 200g' product_name, 120000 base_price, 15 quantity, 'CAT005' category_id, 'products/ca_hoi_phi_le_tuoi_200g.png' image_path FROM dual UNION ALL
    SELECT 'SP000404' product_id, N'Trứng gà ta vỉ 10 quả' product_name, 45000 base_price, 50 quantity, 'CAT005' category_id, 'products/trung_ga_ta_vi_10_qua.png' image_path FROM dual UNION ALL
    SELECT 'SP000405' product_id, N'Khoai tây vàng túi 1kg' product_name, 25000 base_price, 40 quantity, 'CAT005' category_id, 'products/khoai_tay_vang_tui_1kg.png' image_path FROM dual
) src
ON (p.product_id = src.product_id)
WHEN MATCHED THEN UPDATE SET
    p.product_name = src.product_name,
    p.base_price = src.base_price,
    p.category_id = src.category_id,
    p.image_path = src.image_path,
    p.is_deleted = 0
WHEN NOT MATCHED THEN INSERT (
    product_id, product_name, base_price, category_id, supplier_id, base_unit_id, image_path, is_deleted
) VALUES (
    src.product_id, src.product_name, src.base_price, src.category_id, NULL, NULL, src.image_path, 0
);

-- =========================================================
-- F. DỌN TRÙNG TÊN SAU IMPORT
-- Ưu tiên giữ:
-- 1) product_id nằm trong file CSV
-- 2) có image_path hợp lệ
-- 3) mã SP0...
-- 4) product_id nhỏ hơn
-- =========================================================
UPDATE PRODUCTS p
SET p.is_deleted = 1
WHERE p.rowid IN (
    SELECT rid
    FROM (
        SELECT
            p.rowid AS rid,
            p.product_id,
            p.product_name,
            ROW_NUMBER() OVER (
                PARTITION BY LOWER(TRIM(p.product_name))
                ORDER BY
                    CASE
                        WHEN p.product_id IN ('SP000001', 'SP000002', 'SP000003', 'SP000004', 'SP000005', 'SP000006', 'SP000007', 'SP000008', 'SP000009', 'SP000010', 'SP000011', 'SP000012', 'SP000013', 'SP000101', 'SP000102', 'SP000103', 'SP000104', 'SP000105', 'SP000106', 'SP000107', 'SP000108', 'SP000201', 'SP000202', 'SP000203', 'SP000204', 'SP000205', 'SP000206', 'SP000207', 'SP000301', 'SP000302', 'SP000303', 'SP000304', 'SP000305', 'SP000306', 'SP000307', 'SP000308', 'SP000401', 'SP000402', 'SP000403', 'SP000404', 'SP000405') THEN 0
                        ELSE 1
                    END,
                    CASE
                        WHEN p.image_path IS NOT NULL
                         AND TRIM(p.image_path) IS NOT NULL
                         AND LOWER(TRIM(p.image_path)) NOT IN ('-', 'null', 'none', 'no_image', 'no-image', 'noimage', 'image_not_found')
                        THEN 0
                        ELSE 1
                    END,
                    CASE
                        WHEN REGEXP_LIKE(p.product_id, '^SP0[0-9]+$') THEN 0
                        ELSE 1
                    END,
                    p.product_id
            ) AS rn
        FROM PRODUCTS p
        WHERE NVL(p.is_deleted, 0) = 0
    )
    WHERE rn > 1
);

-- =========================================================
-- G. ẨN STORE_PRODUCTS / INVENTORY CỦA SẢN PHẨM ĐÃ XÓA MỀM
-- =========================================================
UPDATE STORE_PRODUCTS sp
SET sp.is_deleted = 1,
    sp.is_active = 0
WHERE sp.product_id IN (
    SELECT product_id
    FROM PRODUCTS
    WHERE NVL(is_deleted, 0) = 1
);

UPDATE INVENTORY i
SET i.is_deleted = 1
WHERE i.product_id IN (
    SELECT product_id
    FROM PRODUCTS
    WHERE NVL(is_deleted, 0) = 1
);

-- =========================================================
-- H. BẬT SẢN PHẨM CSV CHO CHI NHÁNH + TỒN KHO THEO CSV
-- =========================================================
MERGE INTO STORE_PRODUCTS sp
USING (
    SELECT s.store_id,
           p.product_id,
           p.base_price AS selling_price
    FROM STORES s
    CROSS JOIN PRODUCTS p
    WHERE NVL(s.is_deleted, 0) = 0
      AND NVL(p.is_deleted, 0) = 0
      AND p.product_id IN ('SP000001', 'SP000002', 'SP000003', 'SP000004', 'SP000005', 'SP000006', 'SP000007', 'SP000008', 'SP000009', 'SP000010', 'SP000011', 'SP000012', 'SP000013', 'SP000101', 'SP000102', 'SP000103', 'SP000104', 'SP000105', 'SP000106', 'SP000107', 'SP000108', 'SP000201', 'SP000202', 'SP000203', 'SP000204', 'SP000205', 'SP000206', 'SP000207', 'SP000301', 'SP000302', 'SP000303', 'SP000304', 'SP000305', 'SP000306', 'SP000307', 'SP000308', 'SP000401', 'SP000402', 'SP000403', 'SP000404', 'SP000405')
) src
ON (sp.store_id = src.store_id AND sp.product_id = src.product_id)
WHEN MATCHED THEN UPDATE SET
    sp.selling_price = src.selling_price,
    sp.is_active = 1,
    sp.is_deleted = 0,
    sp.min_stock = NVL(sp.min_stock, 30),
    sp.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT (
    store_id, product_id, selling_price, is_active, min_stock, max_stock, is_deleted
) VALUES (
    src.store_id, src.product_id, src.selling_price, 1, 30, NULL, 0
);

MERGE INTO INVENTORY i
USING (
    SELECT s.store_id,
           src.product_id,
           src.quantity
    FROM STORES s
    CROSS JOIN (
        SELECT 'SP000001' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000002' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000003' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000004' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000005' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000006' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000007' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000008' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000009' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000010' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000011' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000012' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000013' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000101' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000102' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000103' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000104' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000105' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000106' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000107' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000108' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000201' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000202' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000203' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000204' product_id, 30 quantity FROM dual UNION ALL
        SELECT 'SP000205' product_id, 55 quantity FROM dual UNION ALL
        SELECT 'SP000206' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000207' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000301' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000302' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000303' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000304' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000305' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000306' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000307' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000308' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000401' product_id, 20 quantity FROM dual UNION ALL
        SELECT 'SP000402' product_id, 25 quantity FROM dual UNION ALL
        SELECT 'SP000403' product_id, 15 quantity FROM dual UNION ALL
        SELECT 'SP000404' product_id, 50 quantity FROM dual UNION ALL
        SELECT 'SP000405' product_id, 40 quantity FROM dual
    ) src
    WHERE NVL(s.is_deleted, 0) = 0
) src
ON (i.store_id = src.store_id AND i.product_id = src.product_id)
WHEN MATCHED THEN UPDATE SET
    i.quantity = src.quantity,
    i.unit = N'Cái',
    i.last_updated = SYSDATE,
    i.is_deleted = 0
WHEN NOT MATCHED THEN INSERT (
    product_id, store_id, quantity, unit, last_updated, is_deleted
) VALUES (
    src.product_id, src.store_id, src.quantity, N'Cái', SYSDATE, 0
);

COMMIT;

-- =========================================================
-- I. CHECK SAU KHI CHẠY
-- =========================================================

-- 1. Kiểm tra còn trùng tên active không. Query này không nên ra dòng nào.
SELECT
    LOWER(TRIM(product_name)) AS normalized_name,
    COUNT(*) AS duplicate_count,
    LISTAGG(product_id, ', ') WITHIN GROUP (ORDER BY product_id) AS product_ids,
    MIN(product_name) AS product_name
FROM PRODUCTS
WHERE NVL(is_deleted, 0) = 0
GROUP BY LOWER(TRIM(product_name))
HAVING COUNT(*) > 1
ORDER BY normalized_name;

-- 2. Kiểm tra còn sản phẩm active chưa có ảnh không. Query này không nên ra dòng nào.
SELECT product_id, product_name, category_id, image_path
FROM PRODUCTS
WHERE NVL(is_deleted, 0) = 0
  AND (
        image_path IS NULL
     OR TRIM(image_path) IS NULL
     OR LOWER(TRIM(image_path)) IN ('-', 'null', 'none', 'no_image', 'no-image', 'noimage', 'image_not_found')
  )
ORDER BY product_id;

-- 3. Kiểm tra sản phẩm CSV đã import đủ chưa.
SELECT COUNT(*) AS total_csv_products_imported
FROM PRODUCTS
WHERE NVL(is_deleted, 0) = 0
  AND product_id IN ('SP000001', 'SP000002', 'SP000003', 'SP000004', 'SP000005', 'SP000006', 'SP000007', 'SP000008', 'SP000009', 'SP000010', 'SP000011', 'SP000012', 'SP000013', 'SP000101', 'SP000102', 'SP000103', 'SP000104', 'SP000105', 'SP000106', 'SP000107', 'SP000108', 'SP000201', 'SP000202', 'SP000203', 'SP000204', 'SP000205', 'SP000206', 'SP000207', 'SP000301', 'SP000302', 'SP000303', 'SP000304', 'SP000305', 'SP000306', 'SP000307', 'SP000308', 'SP000401', 'SP000402', 'SP000403', 'SP000404', 'SP000405');

-- 4. Tổng sản phẩm theo danh mục.
SELECT
    c.category_id,
    c.category_name,
    COUNT(p.product_id) AS total_active_products
FROM CATEGORIES c
LEFT JOIN PRODUCTS p
    ON p.category_id = c.category_id
   AND NVL(p.is_deleted, 0) = 0
WHERE NVL(c.is_deleted, 0) = 0
GROUP BY c.category_id, c.category_name
ORDER BY c.category_id;

-- 5. Danh sách image_path cần có trong project.
SELECT product_id, product_name, image_path
FROM PRODUCTS
WHERE NVL(is_deleted, 0) = 0
  AND product_id IN ('SP000001', 'SP000002', 'SP000003', 'SP000004', 'SP000005', 'SP000006', 'SP000007', 'SP000008', 'SP000009', 'SP000010', 'SP000011', 'SP000012', 'SP000013', 'SP000101', 'SP000102', 'SP000103', 'SP000104', 'SP000105', 'SP000106', 'SP000107', 'SP000108', 'SP000201', 'SP000202', 'SP000203', 'SP000204', 'SP000205', 'SP000206', 'SP000207', 'SP000301', 'SP000302', 'SP000303', 'SP000304', 'SP000305', 'SP000306', 'SP000307', 'SP000308', 'SP000401', 'SP000402', 'SP000403', 'SP000404', 'SP000405')
ORDER BY product_id;
