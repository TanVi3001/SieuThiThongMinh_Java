-- =========================================================
-- 20260526_fix_products_no_duplicate_with_images.sql
-- Mục tiêu:
-- 1) Xóa mềm sản phẩm chưa có ảnh.
-- 2) Xóa mềm sản phẩm trùng tên, ưu tiên giữ mã SP0... và có ảnh.
-- 3) Import lại danh mục + sản phẩm demo KHÔNG trùng tên.
-- 4) Mã sản phẩm mới dùng dạng SP000xxx, không dùng SP100xxx nữa.
-- =========================================================

-- A. BACKUP TRƯỚC KHI DỌN
BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE PRODUCTS_BACKUP_BEFORE_CLEAN_20260526 AS SELECT * FROM PRODUCTS';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE INVENTORY_BACKUP_BEFORE_CLEAN_20260526 AS SELECT * FROM INVENTORY';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

BEGIN
    EXECUTE IMMEDIATE 'CREATE TABLE STORE_PRODUCTS_BACKUP_BEFORE_CLEAN_20260526 AS SELECT * FROM STORE_PRODUCTS';
EXCEPTION WHEN OTHERS THEN IF SQLCODE != -955 THEN RAISE; END IF;
END;
/

-- B. ĐẢM BẢO CATEGORIES CÓ VAT_RATE / STATUS
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME = 'CATEGORIES' AND COLUMN_NAME = 'VAT_RATE';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE CATEGORIES ADD VAT_RATE NUMBER(5,2)';
    END IF;

    SELECT COUNT(*) INTO v_count FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME = 'CATEGORIES' AND COLUMN_NAME = 'STATUS';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE CATEGORIES ADD STATUS NVARCHAR2(30) DEFAULT N''Hoạt động''';
    END IF;
END;
/

-- C. ĐỒNG BỘ DANH MỤC
MERGE INTO CATEGORIES c
USING (
    SELECT 'CAT001' id, N'Thực phẩm khô' name, 8 vat, N'Gạo, mì, đường, gia vị, thực phẩm đóng gói' des FROM dual UNION ALL
    SELECT 'CAT002', N'Đồ uống & Giải khát', 10, N'Nước suối, nước ngọt, trà, cà phê, sữa uống' FROM dual UNION ALL
    SELECT 'CAT003', N'Hóa mỹ phẩm', 10, N'Dầu gội, sữa tắm, bột giặt, nước rửa chén' FROM dual UNION ALL
    SELECT 'CAT004', N'Thực phẩm tươi sống', 5, N'Thịt, cá, rau củ, trái cây tươi' FROM dual UNION ALL
    SELECT 'CAT005', N'Bánh kẹo', 8, N'Bánh quy, kẹo, socola, snack, đồ ăn vặt' FROM dual UNION ALL
    SELECT 'CAT006', N'Sữa & Sản phẩm từ sữa', 8, N'Sữa tươi, sữa chua, phô mai, bơ sữa' FROM dual UNION ALL
    SELECT 'CAT007', N'Đông lạnh & Chế biến sẵn', 8, N'Xúc xích, cá viên, chả giò, thực phẩm đông lạnh' FROM dual UNION ALL
    SELECT 'CAT008', N'Gia dụng nhà bếp', 10, N'Khăn giấy, túi rác, màng bọc, hộp đựng thực phẩm' FROM dual UNION ALL
    SELECT 'CAT009', N'Chăm sóc cá nhân', 10, N'Kem đánh răng, bàn chải, dao cạo, nước súc miệng' FROM dual UNION ALL
    SELECT 'CAT010', N'Mẹ & Bé', 5, N'Tã, khăn ướt, sữa tắm em bé, đồ dùng trẻ em' FROM dual UNION ALL
    SELECT 'CAT011', N'Văn phòng phẩm', 8, N'Tập, bút, giấy note, hồ sơ, dụng cụ học tập' FROM dual UNION ALL
    SELECT 'CAT012', N'Thức ăn thú cưng', 8, N'Hạt chó mèo, pate, cát vệ sinh, đồ chăm sóc thú cưng' FROM dual
) s
ON (c.category_id = s.id)
WHEN MATCHED THEN UPDATE SET
    c.category_name = s.name,
    c.description = s.des,
    c.vat_rate = s.vat,
    c.status = N'Hoạt động',
    c.is_deleted = 0
WHEN NOT MATCHED THEN INSERT(category_id, category_name, description, vat_rate, status, is_deleted)
VALUES(s.id, s.name, s.des, s.vat, N'Hoạt động', 0);

-- D. XÓA MỀM SẢN PHẨM CHƯA CÓ ẢNH
UPDATE PRODUCTS p
SET p.is_deleted = 1
WHERE NVL(p.is_deleted, 0) = 0
  AND (
        p.image_path IS NULL
     OR TRIM(p.image_path) IS NULL
     OR LOWER(TRIM(p.image_path)) IN ('-', 'null', 'none', 'no_image', 'no-image', 'noimage', 'image_not_found')
  );

-- E. IMPORT SẢN PHẨM DEMO KHÔNG TRÙNG
-- Cơ chế:
-- - Nếu product_id đã có thì update.
-- - Nếu product_id chưa có nhưng tên đã tồn tại active thì KHÔNG insert trùng.
MERGE INTO PRODUCTS p
USING (
    SELECT 'SP000001' id, N'Mì Hảo Hảo tôm chua cay' name, 5000 price, 'CAT001' cat, 'mi_hao_hao_tom_chua_cay.png' img FROM dual UNION ALL
    SELECT 'SP000002', N'Mì Omachi sườn hầm ngũ quả', 8000, 'CAT001', 'mi_omachi_suon_ham_ngu_qua.png' FROM dual UNION ALL
    SELECT 'SP000003', N'Phở bò Vifon túi', 7500, 'CAT001', 'pho_bo_vifon_tui.png' FROM dual UNION ALL
    SELECT 'SP000004', N'Gạo ST25 Ông Cua túi 5kg', 180000, 'CAT001', 'gao_st25_ong_cua_5kg.png' FROM dual UNION ALL
    SELECT 'SP000005', N'Nước mắm Nam Ngư 750ml', 35000, 'CAT001', 'nuoc_mam_nam_ngu_750ml.png' FROM dual UNION ALL
    SELECT 'SP000006', N'Nước mắm Chinsu cá hồi 750ml', 45000, 'CAT001', 'nuoc_mam_chinsu_ca_hoi_750ml.png' FROM dual UNION ALL
    SELECT 'SP000007', N'Đường tinh luyện Biên Hòa 1kg', 28000, 'CAT001', 'duong_bien_hoa_1kg.png' FROM dual UNION ALL
    SELECT 'SP000008', N'Dầu ăn Tường An 2L', 52000, 'CAT001', 'dau_an_tuong_an_2l.png' FROM dual UNION ALL

    SELECT 'SP000101', N'Nước suối Aquafina 500ml', 5000, 'CAT002', 'aquafina_500ml.png' FROM dual UNION ALL
    SELECT 'SP000102', N'Coca Cola lon 330ml', 10000, 'CAT002', 'coca_cola_lon_330ml.png' FROM dual UNION ALL
    SELECT 'SP000103', N'Pepsi lon 330ml', 10000, 'CAT002', 'pepsi_lon_330ml.png' FROM dual UNION ALL
    SELECT 'SP000104', N'Trà Ô Long Tea Plus chai 455ml', 10000, 'CAT002', 'tra_olong_tea_plus_455ml.png' FROM dual UNION ALL
    SELECT 'SP000105', N'Sting dâu chai 330ml', 9000, 'CAT002', 'sting_dau_330ml.png' FROM dual UNION ALL
    SELECT 'SP000106', N'Sprite chai 1.5L', 20000, 'CAT002', 'sprite_15l.png' FROM dual UNION ALL
    SELECT 'SP000107', N'Nước tăng lực Redbull lon 250ml', 15000, 'CAT002', 'redbull_lon_250ml.png' FROM dual UNION ALL
    SELECT 'SP000108', N'Cà phê lon Birdy xanh 170ml', 12000, 'CAT002', 'ca_phe_birdy_xanh_170ml.png' FROM dual UNION ALL

    SELECT 'SP000201', N'Dầu gội Clear Men 630g', 145000, 'CAT003', 'dau_goi_clear_men_630g.png' FROM dual UNION ALL
    SELECT 'SP000202', N'Dầu gội Sunsilk mềm mượt 650g', 135000, 'CAT003', 'dau_goi_sunsilk_mem_muot_650g.png' FROM dual UNION ALL
    SELECT 'SP000203', N'Sữa tắm Lifebuoy 850g', 155000, 'CAT003', 'sua_tam_lifebuoy_850g.png' FROM dual UNION ALL
    SELECT 'SP000204', N'Nước rửa chén Sunlight chanh 3.6kg', 115000, 'CAT003', 'nuoc_rua_chen_sunlight_chanh_36kg.png' FROM dual UNION ALL
    SELECT 'SP000205', N'Bột giặt OMO đỏ 5.7kg', 230000, 'CAT003', 'bot_giat_omo_do_57kg.png' FROM dual UNION ALL
    SELECT 'SP000206', N'Nước giặt Ariel 3.6kg', 210000, 'CAT003', 'nuoc_giat_ariel_36kg.png' FROM dual UNION ALL

    SELECT 'SP000301', N'Ức gà phi lê 500g', 45000, 'CAT004', 'uc_ga_phi_le_500g.png' FROM dual UNION ALL
    SELECT 'SP000302', N'Thịt ba chỉ heo 500g', 150000, 'CAT004', 'thit_ba_chi_heo_500g.png' FROM dual UNION ALL
    SELECT 'SP000303', N'Cá hồi phi lê 200g', 120000, 'CAT004', 'ca_hoi_phi_le_200g.png' FROM dual UNION ALL
    SELECT 'SP000304', N'Khoai tây vàng túi 1kg', 25000, 'CAT004', 'khoai_tay_vang_1kg.png' FROM dual UNION ALL
    SELECT 'SP000305', N'Cà chua Đà Lạt 500g', 18000, 'CAT004', 'ca_chua_da_lat_500g.png' FROM dual UNION ALL
    SELECT 'SP000306', N'Rau cải ngọt 500g', 16000, 'CAT004', 'rau_cai_ngot_500g.png' FROM dual UNION ALL

    SELECT 'SP000401', N'Bánh Oreo Socola 133g', 22000, 'CAT005', 'banh_oreo_socola_133g.png' FROM dual UNION ALL
    SELECT 'SP000402', N'Snack khoai tây Lay''s vị tự nhiên 95g', 25000, 'CAT005', 'snack_lays_tu_nhien_95g.png' FROM dual UNION ALL
    SELECT 'SP000403', N'KitKat socola 4 thanh', 20000, 'CAT005', 'kitkat_4_thanh.png' FROM dual UNION ALL
    SELECT 'SP000404', N'Kẹo dẻo Alpenliebe Jelly 90g', 18000, 'CAT005', 'keo_deo_alpenliebe_jelly_90g.png' FROM dual UNION ALL
    SELECT 'SP000405', N'Bánh gạo One One vị bò nướng 150g', 22000, 'CAT005', 'banh_gao_one_one_bo_nuong_150g.png' FROM dual UNION ALL

    SELECT 'SP000501', N'Sữa tươi TH True Milk ít đường 1L', 37000, 'CAT006', 'sua_th_true_milk_it_duong_1l.png' FROM dual UNION ALL
    SELECT 'SP000502', N'Sữa tươi Vinamilk không đường 1L', 36000, 'CAT006', 'sua_vinamilk_khong_duong_1l.png' FROM dual UNION ALL
    SELECT 'SP000503', N'Sữa chua Vinamilk có đường lốc 4 hộp', 32000, 'CAT006', 'sua_chua_vinamilk_co_duong_4_hop.png' FROM dual UNION ALL
    SELECT 'SP000504', N'Phô mai Con Bò Cười hộp 8 miếng', 42000, 'CAT006', 'pho_mai_con_bo_cuoi_8_mieng.png' FROM dual UNION ALL
    SELECT 'SP000505', N'Sữa đặc Ông Thọ đỏ lon 380g', 27000, 'CAT006', 'sua_dac_ong_tho_do_380g.png' FROM dual UNION ALL

    SELECT 'SP000601', N'Xúc xích Đức Việt gói 500g', 78000, 'CAT007', 'xuc_xich_duc_viet_500g.png' FROM dual UNION ALL
    SELECT 'SP000602', N'Cá viên CP gói 500g', 65000, 'CAT007', 'ca_vien_cp_500g.png' FROM dual UNION ALL
    SELECT 'SP000603', N'Chả giò Cầu Tre hải sản 500g', 72000, 'CAT007', 'cha_gio_cau_tre_hai_san_500g.png' FROM dual UNION ALL
    SELECT 'SP000604', N'Khoai tây chiên đông lạnh 1kg', 85000, 'CAT007', 'khoai_tay_chien_dong_lanh_1kg.png' FROM dual UNION ALL
    SELECT 'SP000605', N'Pizza hải sản đông lạnh 300g', 69000, 'CAT007', 'pizza_hai_san_dong_lanh_300g.png' FROM dual UNION ALL

    SELECT 'SP000701', N'Khăn giấy Pulppy 2 lớp 10 cuộn', 62000, 'CAT008', 'khan_giay_pulppy_10_cuon.png' FROM dual UNION ALL
    SELECT 'SP000702', N'Túi rác tự hủy 3 cuộn', 35000, 'CAT008', 'tui_rac_tu_huy_3_cuon.png' FROM dual UNION ALL
    SELECT 'SP000703', N'Màng bọc thực phẩm Ringo 30cm x 30m', 29000, 'CAT008', 'mang_boc_thuc_pham_ringo_30cm.png' FROM dual UNION ALL
    SELECT 'SP000704', N'Hộp nhựa Lock Lock 1L', 55000, 'CAT008', 'hop_nhua_lock_lock_1l.png' FROM dual UNION ALL
    SELECT 'SP000705', N'Nước lau sàn Sunlight hương hoa 1kg', 45000, 'CAT008', 'nuoc_lau_san_sunlight_1kg.png' FROM dual UNION ALL

    SELECT 'SP000801', N'Kem đánh răng P/S trà xanh 180g', 32000, 'CAT009', 'kem_danh_rang_ps_tra_xanh_180g.png' FROM dual UNION ALL
    SELECT 'SP000802', N'Bàn chải Oral-B mềm', 28000, 'CAT009', 'ban_chai_oral_b_mem.png' FROM dual UNION ALL
    SELECT 'SP000803', N'Nước súc miệng Listerine 500ml', 95000, 'CAT009', 'nuoc_suc_mieng_listerine_500ml.png' FROM dual UNION ALL
    SELECT 'SP000804', N'Dao cạo râu Gillette Blue 3', 45000, 'CAT009', 'dao_cao_rau_gillette_blue_3.png' FROM dual UNION ALL
    SELECT 'SP000805', N'Lăn khử mùi Nivea Men 50ml', 72000, 'CAT009', 'lan_khu_mui_nivea_men_50ml.png' FROM dual UNION ALL

    SELECT 'SP000901', N'Tã Bobby quần size M 68 miếng', 285000, 'CAT010', 'ta_bobby_quan_m_68.png' FROM dual UNION ALL
    SELECT 'SP000902', N'Khăn ướt Mamamy 100 tờ', 35000, 'CAT010', 'khan_uot_mamamy_100_to.png' FROM dual UNION ALL
    SELECT 'SP000903', N'Sữa tắm gội Johnson Baby 500ml', 89000, 'CAT010', 'sua_tam_goi_johnson_baby_500ml.png' FROM dual UNION ALL
    SELECT 'SP000904', N'Phấn rôm Johnson Baby 200g', 55000, 'CAT010', 'phan_rom_johnson_baby_200g.png' FROM dual UNION ALL
    SELECT 'SP000905', N'Bình sữa Pigeon 240ml', 165000, 'CAT010', 'binh_sua_pigeon_240ml.png' FROM dual UNION ALL

    SELECT 'SP001001', N'Tập Campus 200 trang', 16000, 'CAT011', 'tap_campus_200_trang.png' FROM dual UNION ALL
    SELECT 'SP001002', N'Bút bi Thiên Long TL-027', 5000, 'CAT011', 'but_bi_thien_long_tl027.png' FROM dual UNION ALL
    SELECT 'SP001003', N'Giấy note Pronoti 3x3', 18000, 'CAT011', 'giay_note_pronoti_3x3.png' FROM dual UNION ALL
    SELECT 'SP001004', N'Bìa hồ sơ A4 Plus', 12000, 'CAT011', 'bia_ho_so_a4_plus.png' FROM dual UNION ALL
    SELECT 'SP001005', N'Băng keo trong 5cm', 15000, 'CAT011', 'bang_keo_trong_5cm.png' FROM dual UNION ALL

    SELECT 'SP001101', N'Hạt mèo Whiskas cá ngừ 1.2kg', 145000, 'CAT012', 'hat_meo_whiskas_ca_ngu_12kg.png' FROM dual UNION ALL
    SELECT 'SP001102', N'Hạt chó Pedigree vị bò 1.5kg', 135000, 'CAT012', 'hat_cho_pedigree_vi_bo_15kg.png' FROM dual UNION ALL
    SELECT 'SP001103', N'Pate mèo Me-O cá ngừ 80g', 15000, 'CAT012', 'pate_meo_meo_ca_ngu_80g.png' FROM dual UNION ALL
    SELECT 'SP001104', N'Cát vệ sinh mèo Min 5L', 79000, 'CAT012', 'cat_ve_sinh_meo_min_5l.png' FROM dual UNION ALL
    SELECT 'SP001105', N'Sữa tắm chó mèo Joyce Dolls 400ml', 115000, 'CAT012', 'sua_tam_cho_meo_joyce_dolls_400ml.png' FROM dual
) src
ON (p.product_id = src.id)
WHEN MATCHED THEN UPDATE SET
    p.product_name = src.name,
    p.base_price = src.price,
    p.category_id = src.cat,
    p.image_path = src.img,
    p.is_deleted = 0
WHEN NOT MATCHED THEN INSERT(product_id, product_name, base_price, category_id, supplier_id, base_unit_id, image_path, is_deleted)
SELECT src.id, src.name, src.price, src.cat, NULL, NULL, src.img, 0
FROM dual
WHERE NOT EXISTS (
    SELECT 1
    FROM PRODUCTS existing
    WHERE NVL(existing.is_deleted, 0) = 0
      AND LOWER(TRIM(existing.product_name)) = LOWER(TRIM(src.name))
);

-- F. XÓA MỀM LẦN NỮA SẢN PHẨM TRÙNG TÊN
UPDATE PRODUCTS p
SET p.is_deleted = 1
WHERE p.rowid IN (
    SELECT rid
    FROM (
        SELECT
            p.rowid AS rid,
            ROW_NUMBER() OVER (
                PARTITION BY LOWER(TRIM(p.product_name))
                ORDER BY
                    CASE WHEN REGEXP_LIKE(p.product_id, '^SP0[0-9]+$') THEN 0 ELSE 1 END,
                    CASE
                        WHEN p.image_path IS NOT NULL
                         AND TRIM(p.image_path) IS NOT NULL
                         AND LOWER(TRIM(p.image_path)) NOT IN ('-', 'null', 'none', 'no_image', 'no-image', 'noimage', 'image_not_found')
                        THEN 0 ELSE 1
                    END,
                    p.product_id
            ) AS rn
        FROM PRODUCTS p
        WHERE NVL(p.is_deleted, 0) = 0
    )
    WHERE rn > 1
);

-- G. BẬT SẢN PHẨM ACTIVE CHO CHI NHÁNH + TỒN KHO DEMO
MERGE INTO STORE_PRODUCTS sp
USING (
    SELECT s.store_id, p.product_id, p.base_price AS selling_price
    FROM STORES s
    CROSS JOIN PRODUCTS p
    WHERE NVL(s.is_deleted, 0) = 0
      AND NVL(p.is_deleted, 0) = 0
      AND p.image_path IS NOT NULL
      AND TRIM(p.image_path) IS NOT NULL
      AND LOWER(TRIM(p.image_path)) NOT IN ('-', 'null', 'none', 'no_image', 'no-image', 'noimage', 'image_not_found')
) src
ON (sp.store_id = src.store_id AND sp.product_id = src.product_id)
WHEN MATCHED THEN UPDATE SET
    sp.selling_price = src.selling_price,
    sp.is_active = 1,
    sp.is_deleted = 0,
    sp.min_stock = NVL(sp.min_stock, 30),
    sp.updated_at = CURRENT_TIMESTAMP
WHEN NOT MATCHED THEN INSERT(store_id, product_id, selling_price, is_active, min_stock, max_stock, is_deleted)
VALUES(src.store_id, src.product_id, src.selling_price, 1, 30, NULL, 0);

MERGE INTO INVENTORY i
USING (
    SELECT s.store_id,
           p.product_id,
           CASE
               WHEN p.category_id = 'CAT004' THEN 80
               WHEN p.category_id = 'CAT007' THEN 120
               WHEN p.category_id = 'CAT010' THEN 60
               ELSE 200
           END AS quantity
    FROM STORES s
    CROSS JOIN PRODUCTS p
    WHERE NVL(s.is_deleted, 0) = 0
      AND NVL(p.is_deleted, 0) = 0
      AND p.image_path IS NOT NULL
      AND TRIM(p.image_path) IS NOT NULL
      AND LOWER(TRIM(p.image_path)) NOT IN ('-', 'null', 'none', 'no_image', 'no-image', 'noimage', 'image_not_found')
) src
ON (i.store_id = src.store_id AND i.product_id = src.product_id)
WHEN MATCHED THEN UPDATE SET
    i.quantity = CASE WHEN NVL(i.quantity, 0) < src.quantity THEN src.quantity ELSE i.quantity END,
    i.unit = N'Cái',
    i.last_updated = SYSDATE,
    i.is_deleted = 0
WHEN NOT MATCHED THEN INSERT(product_id, store_id, quantity, unit, last_updated, is_deleted)
VALUES(src.product_id, src.store_id, src.quantity, N'Cái', SYSDATE, 0);

-- H. ẨN STORE_PRODUCTS / INVENTORY CỦA SẢN PHẨM ĐÃ XÓA MỀM
UPDATE STORE_PRODUCTS sp
SET sp.is_deleted = 1,
    sp.is_active = 0
WHERE sp.product_id IN (
    SELECT product_id FROM PRODUCTS WHERE NVL(is_deleted, 0) = 1
);

UPDATE INVENTORY i
SET i.is_deleted = 1
WHERE i.product_id IN (
    SELECT product_id FROM PRODUCTS WHERE NVL(is_deleted, 0) = 1
);

COMMIT;

-- I. CHECK SAU KHI CHẠY

-- 1. Còn trùng tên active không?
SELECT LOWER(TRIM(product_name)) AS normalized_name,
       COUNT(*) AS duplicate_count,
       LISTAGG(product_id, ', ') WITHIN GROUP (ORDER BY product_id) AS product_ids,
       MIN(product_name) AS product_name
FROM PRODUCTS
WHERE NVL(is_deleted, 0) = 0
GROUP BY LOWER(TRIM(product_name))
HAVING COUNT(*) > 1
ORDER BY normalized_name;

-- 2. Còn sản phẩm active chưa có ảnh không?
SELECT product_id, product_name, category_id, image_path
FROM PRODUCTS
WHERE NVL(is_deleted, 0) = 0
  AND (
        image_path IS NULL
     OR TRIM(image_path) IS NULL
     OR LOWER(TRIM(image_path)) IN ('-', 'null', 'none', 'no_image', 'no-image', 'noimage', 'image_not_found')
  )
ORDER BY product_id;

-- 3. Tổng sản phẩm active theo danh mục.
SELECT c.category_id,
       c.category_name,
       COUNT(p.product_id) AS total_active_products
FROM CATEGORIES c
LEFT JOIN PRODUCTS p
    ON p.category_id = c.category_id
   AND NVL(p.is_deleted, 0) = 0
WHERE NVL(c.is_deleted, 0) = 0
GROUP BY c.category_id, c.category_name
ORDER BY c.category_id;

-- 4. Danh sách sản phẩm active cuối cùng.
SELECT product_id, product_name, category_id, base_price, image_path, is_deleted
FROM PRODUCTS
WHERE NVL(is_deleted, 0) = 0
ORDER BY category_id, product_id;


-- Còn trùng tên active không?
SELECT LOWER(TRIM(product_name)) AS normalized_name,
       COUNT(*) AS duplicate_count,
       LISTAGG(product_id, ', ') WITHIN GROUP (ORDER BY product_id) AS product_ids,
       MIN(product_name) AS product_name
FROM PRODUCTS
WHERE NVL(is_deleted, 0) = 0
GROUP BY LOWER(TRIM(product_name))
HAVING COUNT(*) > 1
ORDER BY normalized_name;

-- Còn sản phẩm active chưa có ảnh không?
SELECT product_id, product_name, category_id, image_path
FROM PRODUCTS
WHERE NVL(is_deleted, 0) = 0
  AND (
        image_path IS NULL
     OR TRIM(image_path) IS NULL
     OR LOWER(TRIM(image_path)) IN ('-', 'null', 'none', 'no_image', 'no-image', 'noimage', 'image_not_found')
  )
ORDER BY product_id;

___ Chạy lại nếu lỗi ___

MERGE INTO CATEGORIES c
USING (
    SELECT 'CAT006' AS category_id, N'Sữa & Sản phẩm từ sữa' AS category_name, N'Sữa tươi, sữa chua, phô mai, bơ sữa' AS description FROM dual UNION ALL
    SELECT 'CAT007', N'Đông lạnh & Chế biến sẵn', N'Xúc xích, cá viên, chả giò, thực phẩm đông lạnh' FROM dual UNION ALL
    SELECT 'CAT008', N'Gia dụng nhà bếp', N'Khăn giấy, túi rác, màng bọc, hộp đựng thực phẩm' FROM dual UNION ALL
    SELECT 'CAT009', N'Chăm sóc cá nhân', N'Kem đánh răng, bàn chải, dao cạo, nước súc miệng' FROM dual UNION ALL
    SELECT 'CAT010', N'Mẹ & Bé', N'Tã, khăn ướt, sữa tắm em bé, đồ dùng trẻ em' FROM dual UNION ALL
    SELECT 'CAT011', N'Văn phòng phẩm', N'Tập, bút, giấy note, hồ sơ, dụng cụ học tập' FROM dual UNION ALL
    SELECT 'CAT012', N'Thức ăn thú cưng', N'Hạt chó mèo, pate, cát vệ sinh, đồ chăm sóc thú cưng' FROM dual UNION ALL
    SELECT 'CAT013', N'Thực phẩm hữu cơ', N'Rau củ, ngũ cốc, hạt dinh dưỡng, sản phẩm organic' FROM dual UNION ALL
    SELECT 'CAT014', N'Đồ hộp & Ăn liền', N'Cá hộp, thịt hộp, cháo ăn liền, mì ly, đồ ăn nhanh' FROM dual UNION ALL
    SELECT 'CAT015', N'Vệ sinh nhà cửa', N'Nước lau sàn, nước tẩy, xịt phòng, dụng cụ vệ sinh' FROM dual
) src
ON (c.category_id = src.category_id)
WHEN MATCHED THEN UPDATE SET
    c.category_name = src.category_name,
    c.description = src.description,
    c.is_deleted = 0
WHEN NOT MATCHED THEN INSERT (
    category_id,
    category_name,
    description,
    is_deleted
) VALUES (
    src.category_id,
    src.category_name,
    src.description,
    0
);

COMMIT;