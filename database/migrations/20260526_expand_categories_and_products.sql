-- =========================================================
-- 20260526_expand_categories_and_products.sql
-- Mục tiêu:
-- 1) Đồng bộ thiếu CAT005 giữa màn "Sản phẩm & Danh mục" và "Danh mục & Thuế VAT".
-- 2) Thêm nhiều danh mục mới để demo đẹp hơn.
-- 3) Thêm sản phẩm cụ thể theo từng danh mục, có image_path để gắn hình.
-- =========================================================

-- Nếu DB hiện tại chưa có VAT_RATE / STATUS trong CATEGORIES thì thêm an toàn.
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count
    FROM USER_TAB_COLUMNS
    WHERE TABLE_NAME = 'CATEGORIES'
      AND COLUMN_NAME = 'VAT_RATE';

    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE CATEGORIES ADD VAT_RATE NUMBER(5,2) DEFAULT 8';
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
-- A. ĐỒNG BỘ DANH MỤC
-- =========================================================

MERGE INTO CATEGORIES c
USING (
    SELECT 'CAT001' category_id, N'Thực phẩm khô' category_name, 8 vat_rate, N'Gạo, mì, đường, gia vị, thực phẩm đóng gói' description FROM dual UNION ALL
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
-- B. THÊM SẢN PHẨM CỤ THỂ THEO DANH MỤC
-- image_path để trong src/main/resources/view/image/
-- =========================================================

MERGE INTO PRODUCTS p
USING (
    -- CAT001 - Thực phẩm khô
    SELECT 'SP100001' product_id, N'Mì Hảo Hảo tôm chua cay' product_name, 5000 base_price, 'CAT001' category_id, 'mi_hao_hao_tom_chua_cay.png' image_path FROM dual UNION ALL
    SELECT 'SP100002', N'Mì Omachi sườn hầm ngũ quả', 8000, 'CAT001', 'mi_omachi_suon_ham_ngu_qua.png' FROM dual UNION ALL
    SELECT 'SP100003', N'Phở bò Vifon túi', 7500, 'CAT001', 'pho_bo_vifon_tui.png' FROM dual UNION ALL
    SELECT 'SP100004', N'Gạo ST25 Ông Cua túi 5kg', 180000, 'CAT001', 'gao_st25_ong_cua_5kg.png' FROM dual UNION ALL
    SELECT 'SP100005', N'Nước mắm Nam Ngư 750ml', 35000, 'CAT001', 'nuoc_mam_nam_ngu_750ml.png' FROM dual UNION ALL

    -- CAT002 - Đồ uống
    SELECT 'SP100101', N'Nước suối Aquafina 500ml', 5000, 'CAT002', 'aquafina_500ml.png' FROM dual UNION ALL
    SELECT 'SP100102', N'Coca Cola lon 330ml', 10000, 'CAT002', 'coca_cola_lon_330ml.png' FROM dual UNION ALL
    SELECT 'SP100103', N'Pepsi lon 330ml', 10000, 'CAT002', 'pepsi_lon_330ml.png' FROM dual UNION ALL
    SELECT 'SP100104', N'Trà Ô Long Tea Plus chai 455ml', 10000, 'CAT002', 'tra_olong_tea_plus_455ml.png' FROM dual UNION ALL
    SELECT 'SP100105', N'Sting dâu chai 330ml', 9000, 'CAT002', 'sting_dau_330ml.png' FROM dual UNION ALL

    -- CAT003 - Hóa mỹ phẩm
    SELECT 'SP100201', N'Dầu gội Clear Men 630g', 145000, 'CAT003', 'dau_goi_clear_men_630g.png' FROM dual UNION ALL
    SELECT 'SP100202', N'Dầu gội Sunsilk mềm mượt 650g', 135000, 'CAT003', 'dau_goi_sunsilk_mem_muot_650g.png' FROM dual UNION ALL
    SELECT 'SP100203', N'Sữa tắm Lifebuoy 850g', 155000, 'CAT003', 'sua_tam_lifebuoy_850g.png' FROM dual UNION ALL
    SELECT 'SP100204', N'Nước rửa chén Sunlight chanh 3.6kg', 115000, 'CAT003', 'nuoc_rua_chen_sunlight_chanh_36kg.png' FROM dual UNION ALL
    SELECT 'SP100205', N'Bột giặt OMO đỏ 5.7kg', 230000, 'CAT003', 'bot_giat_omo_do_57kg.png' FROM dual UNION ALL

    -- CAT004 - Tươi sống
    SELECT 'SP100301', N'Ức gà phi lê 500g', 45000, 'CAT004', 'uc_ga_phi_le_500g.png' FROM dual UNION ALL
    SELECT 'SP100302', N'Thịt ba chỉ heo 500g', 150000, 'CAT004', 'thit_ba_chi_heo_500g.png' FROM dual UNION ALL
    SELECT 'SP100303', N'Cá hồi phi lê 200g', 120000, 'CAT004', 'ca_hoi_phi_le_200g.png' FROM dual UNION ALL
    SELECT 'SP100304', N'Khoai tây vàng túi 1kg', 25000, 'CAT004', 'khoai_tay_vang_1kg.png' FROM dual UNION ALL
    SELECT 'SP100305', N'Cà chua Đà Lạt 500g', 18000, 'CAT004', 'ca_chua_da_lat_500g.png' FROM dual UNION ALL

    -- CAT005 - Bánh kẹo
    SELECT 'SP100401', N'Bánh Oreo Socola 133g', 22000, 'CAT005', 'banh_oreo_socola_133g.png' FROM dual UNION ALL
    SELECT 'SP100402', N'Snack khoai tây Lay''s vị tự nhiên 95g', 25000, 'CAT005', 'snack_lays_tu_nhien_95g.png' FROM dual UNION ALL
    SELECT 'SP100403', N'KitKat socola 4 thanh', 20000, 'CAT005', 'kitkat_4_thanh.png' FROM dual UNION ALL
    SELECT 'SP100404', N'Kẹo dẻo Alpenliebe Jelly 90g', 18000, 'CAT005', 'keo_deo_alpenliebe_jelly_90g.png' FROM dual UNION ALL
    SELECT 'SP100405', N'Bánh gạo One One vị bò nướng 150g', 22000, 'CAT005', 'banh_gao_one_one_bo_nuong_150g.png' FROM dual UNION ALL

    -- CAT006 - Sữa
    SELECT 'SP100501', N'Sữa tươi TH True Milk ít đường 1L', 37000, 'CAT006', 'sua_th_true_milk_it_duong_1l.png' FROM dual UNION ALL
    SELECT 'SP100502', N'Sữa tươi Vinamilk không đường 1L', 36000, 'CAT006', 'sua_vinamilk_khong_duong_1l.png' FROM dual UNION ALL
    SELECT 'SP100503', N'Sữa chua Vinamilk có đường lốc 4 hộp', 32000, 'CAT006', 'sua_chua_vinamilk_co_duong_4_hop.png' FROM dual UNION ALL
    SELECT 'SP100504', N'Phô mai Con Bò Cười hộp 8 miếng', 42000, 'CAT006', 'pho_mai_con_bo_cuoi_8_mieng.png' FROM dual UNION ALL
    SELECT 'SP100505', N'Sữa đặc Ông Thọ đỏ lon 380g', 27000, 'CAT006', 'sua_dac_ong_tho_do_380g.png' FROM dual UNION ALL

    -- CAT007 - Đông lạnh
    SELECT 'SP100601', N'Xúc xích Đức Việt gói 500g', 78000, 'CAT007', 'xuc_xich_duc_viet_500g.png' FROM dual UNION ALL
    SELECT 'SP100602', N'Cá viên CP gói 500g', 65000, 'CAT007', 'ca_vien_cp_500g.png' FROM dual UNION ALL
    SELECT 'SP100603', N'Chả giò Cầu Tre hải sản 500g', 72000, 'CAT007', 'cha_gio_cau_tre_hai_san_500g.png' FROM dual UNION ALL
    SELECT 'SP100604', N'Khoai tây chiên đông lạnh 1kg', 85000, 'CAT007', 'khoai_tay_chien_dong_lanh_1kg.png' FROM dual UNION ALL
    SELECT 'SP100605', N'Pizza hải sản đông lạnh 300g', 69000, 'CAT007', 'pizza_hai_san_dong_lanh_300g.png' FROM dual UNION ALL

    -- CAT008 - Gia dụng
    SELECT 'SP100701', N'Khăn giấy Pulppy 2 lớp 10 cuộn', 62000, 'CAT008', 'khan_giay_pulppy_10_cuon.png' FROM dual UNION ALL
    SELECT 'SP100702', N'Túi rác tự hủy 3 cuộn', 35000, 'CAT008', 'tui_rac_tu_huy_3_cuon.png' FROM dual UNION ALL
    SELECT 'SP100703', N'Màng bọc thực phẩm Ringo 30cm x 30m', 29000, 'CAT008', 'mang_boc_thuc_pham_ringo_30cm.png' FROM dual UNION ALL
    SELECT 'SP100704', N'Hộp nhựa Lock&Lock 1L', 55000, 'CAT008', 'hop_nhua_lock_lock_1l.png' FROM dual UNION ALL
    SELECT 'SP100705', N'Nước lau sàn Sunlight hương hoa 1kg', 45000, 'CAT008', 'nuoc_lau_san_sunlight_1kg.png' FROM dual UNION ALL

    -- CAT009 - Cá nhân
    SELECT 'SP100801', N'Kem đánh răng P/S trà xanh 180g', 32000, 'CAT009', 'kem_danh_rang_ps_tra_xanh_180g.png' FROM dual UNION ALL
    SELECT 'SP100802', N'Bàn chải Oral-B mềm', 28000, 'CAT009', 'ban_chai_oral_b_mem.png' FROM dual UNION ALL
    SELECT 'SP100803', N'Nước súc miệng Listerine 500ml', 95000, 'CAT009', 'nuoc_suc_mieng_listerine_500ml.png' FROM dual UNION ALL
    SELECT 'SP100804', N'Dao cạo râu Gillette Blue 3', 45000, 'CAT009', 'dao_cao_rau_gillette_blue_3.png' FROM dual UNION ALL
    SELECT 'SP100805', N'Lăn khử mùi Nivea Men 50ml', 72000, 'CAT009', 'lan_khu_mui_nivea_men_50ml.png' FROM dual UNION ALL

    -- CAT010 - Mẹ & Bé
    SELECT 'SP100901', N'Tã Bobby quần size M 68 miếng', 285000, 'CAT010', 'ta_bobby_quan_m_68.png' FROM dual UNION ALL
    SELECT 'SP100902', N'Khăn ướt Mamamy 100 tờ', 35000, 'CAT010', 'khan_uot_mamamy_100_to.png' FROM dual UNION ALL
    SELECT 'SP100903', N'Sữa tắm gội Johnson Baby 500ml', 89000, 'CAT010', 'sua_tam_goi_johnson_baby_500ml.png' FROM dual UNION ALL
    SELECT 'SP100904', N'Phấn rôm Johnson Baby 200g', 55000, 'CAT010', 'phan_rom_johnson_baby_200g.png' FROM dual UNION ALL
    SELECT 'SP100905', N'Bình sữa Pigeon 240ml', 165000, 'CAT010', 'binh_sua_pigeon_240ml.png' FROM dual UNION ALL

    -- CAT011 - VPP
    SELECT 'SP101001', N'Tập Campus 200 trang', 16000, 'CAT011', 'tap_campus_200_trang.png' FROM dual UNION ALL
    SELECT 'SP101002', N'Bút bi Thiên Long TL-027', 5000, 'CAT011', 'but_bi_thien_long_tl027.png' FROM dual UNION ALL
    SELECT 'SP101003', N'Giấy note Pronoti 3x3', 18000, 'CAT011', 'giay_note_pronoti_3x3.png' FROM dual UNION ALL
    SELECT 'SP101004', N'Bìa hồ sơ A4 Plus', 12000, 'CAT011', 'bia_ho_so_a4_plus.png' FROM dual UNION ALL
    SELECT 'SP101005', N'Băng keo trong 5cm', 15000, 'CAT011', 'bang_keo_trong_5cm.png' FROM dual UNION ALL

    -- CAT012 - Thú cưng
    SELECT 'SP101101', N'Hạt mèo Whiskas cá ngừ 1.2kg', 145000, 'CAT012', 'hat_meo_whiskas_ca_ngu_12kg.png' FROM dual UNION ALL
    SELECT 'SP101102', N'Hạt chó Pedigree vị bò 1.5kg', 135000, 'CAT012', 'hat_cho_pedigree_vi_bo_15kg.png' FROM dual UNION ALL
    SELECT 'SP101103', N'Pate mèo Me-O cá ngừ 80g', 15000, 'CAT012', 'pate_meo_meo_ca_ngu_80g.png' FROM dual UNION ALL
    SELECT 'SP101104', N'Cát vệ sinh mèo Min 5L', 79000, 'CAT012', 'cat_ve_sinh_meo_min_5l.png' FROM dual UNION ALL
    SELECT 'SP101105', N'Sữa tắm chó mèo Joyce&Dolls 400ml', 115000, 'CAT012', 'sua_tam_cho_meo_joyce_dolls_400ml.png' FROM dual
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
-- C. BẬT SẢN PHẨM CHO TẤT CẢ CHI NHÁNH VÀ THÊM TỒN KHO DEMO
-- =========================================================

MERGE INTO STORE_PRODUCTS sp
USING (
    SELECT s.store_id, p.product_id, p.base_price selling_price
    FROM STORES s
    CROSS JOIN PRODUCTS p
    WHERE NVL(s.is_deleted, 0) = 0
      AND p.product_id BETWEEN 'SP100001' AND 'SP101105'
      AND NVL(p.is_deleted, 0) = 0
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
      AND p.product_id BETWEEN 'SP100001' AND 'SP101105'
      AND NVL(p.is_deleted, 0) = 0
) src
ON (i.store_id = src.store_id AND i.product_id = src.product_id)
WHEN MATCHED THEN UPDATE SET
    i.quantity = CASE WHEN NVL(i.quantity, 0) < src.quantity THEN src.quantity ELSE i.quantity END,
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
-- D. CHECK NHANH
-- =========================================================

-- =========================================================
-- CLEAN DUPLICATE PRODUCTS BY NAME
-- Giữ sản phẩm có mã SP0... như SP000001, SP000002
-- Xóa mềm các sản phẩm trùng tên còn lại như SP100001...
-- =========================================================

-- 1. Xem trước sản phẩm bị trùng
SELECT
    LOWER(TRIM(product_name)) AS normalized_name,
    COUNT(*) AS duplicate_count,
    LISTAGG(product_id, ', ') WITHIN GROUP (ORDER BY product_id) AS product_ids,
    MIN(product_name) AS product_name
FROM products
WHERE NVL(is_deleted, 0) = 0
GROUP BY LOWER(TRIM(product_name))
HAVING COUNT(*) > 1
ORDER BY normalized_name;


-- 2. Backup nhanh trước khi xóa mềm
CREATE TABLE PRODUCTS_DUP_BACKUP_20260526 AS
SELECT *
FROM products
WHERE NVL(is_deleted, 0) = 0
  AND LOWER(TRIM(product_name)) IN (
      SELECT LOWER(TRIM(product_name))
      FROM products
      WHERE NVL(is_deleted, 0) = 0
      GROUP BY LOWER(TRIM(product_name))
      HAVING COUNT(*) > 1
  );


-- 3. Xóa mềm sản phẩm trùng
UPDATE products p
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
                        WHEN REGEXP_LIKE(p.product_id, '^SP0[0-9]+$') THEN 0
                        ELSE 1
                    END,
                    p.product_id
            ) AS rn
        FROM products p
        WHERE NVL(p.is_deleted, 0) = 0
    )
    WHERE rn > 1
);


-- 4. Ẩn tồn kho của sản phẩm đã bị xóa mềm
UPDATE inventory i
SET i.is_deleted = 1
WHERE i.product_id IN (
    SELECT product_id
    FROM products
    WHERE NVL(is_deleted, 0) = 1
);


-- 5. Ẩn sản phẩm ở chi nhánh
UPDATE store_products sp
SET sp.is_deleted = 1,
    sp.is_active = 0
WHERE sp.product_id IN (
    SELECT product_id
    FROM products
    WHERE NVL(is_deleted, 0) = 1
);


COMMIT;


-- 6. Check lại còn trùng không
SELECT
    LOWER(TRIM(product_name)) AS normalized_name,
    COUNT(*) AS duplicate_count,
    LISTAGG(product_id, ', ') WITHIN GROUP (ORDER BY product_id) AS product_ids,
    MIN(product_name) AS product_name
FROM products
WHERE NVL(is_deleted, 0) = 0
GROUP BY LOWER(TRIM(product_name))
HAVING COUNT(*) > 1
ORDER BY normalized_name;


-- 7. Check danh sách sản phẩm active còn lại
SELECT
    product_id,
    product_name,
    category_id,
    image_path,
    is_deleted
FROM products
WHERE NVL(is_deleted, 0) = 0
ORDER BY product_id;