-- ==========================================================
-- 007_seed_suppliers.sql
-- Purpose:
--   Seed default suppliers for Smart Supermarket.
-- Safe to re-run.
-- ==========================================================

SET DEFINE OFF;

MERGE INTO SUPPLIERS s
USING (
    SELECT 'SUP_01' AS supplier_id,
           N'Nhà cung cấp Tổng hợp' AS supplier_name,
           'supplier01@smartmarket.vn' AS email,
           N'TP.HCM' AS address,
           '0901000001' AS phone_number
    FROM dual

    UNION ALL

    SELECT 'SUP_02',
           N'Công ty TNHH Acecook Việt Nam',
           'acecook@acecook.vn',
           N'36 Tân Thắng, Bình Tân, TP.HCM',
           '02837608888'
    FROM dual

    UNION ALL

    SELECT 'SUP_03',
           N'Công ty TNHH Vifon',
           'vifon@vifon.com.vn',
           N'28 Đồng Nai, Quận 10, TP.HCM',
           '02838333888'
    FROM dual

    UNION ALL

    SELECT 'SUP_04',
           N'Công ty CP Masan Consumer',
           'masan@masan.com.vn',
           N'12 Tân Trào, Tân Phú, TP.HCM',
           '02873008888'
    FROM dual

    UNION ALL

    SELECT 'SUP_05',
           N'Công ty CP Dầu thực vật Tường An',
           'tuongan@tuongan.com.vn',
           N'48 Trường Sơn, Tân Bình, TP.HCM',
           '02838111333'
    FROM dual

    UNION ALL

    SELECT 'SUP_06',
           N'Công ty CP Sữa Việt Nam (Vinamilk)',
           'vinamilk@vinamilk.com.vn',
           N'10 Tân Trào, Quận 7, TP.HCM',
           '18001557'
    FROM dual

    UNION ALL

    SELECT 'SUP_07',
           N'Công ty TNHH Unilever Việt Nam',
           'unilever@unilever.com',
           N'KCN Biên Hòa 2, Đồng Nai',
           '02513836333'
    FROM dual

    UNION ALL

    SELECT 'SUP_08',
           N'Công ty TNHH Nestlé Việt Nam',
           'nestle@nestle.com.vn',
           N'KCN Biên Hòa 1, Đồng Nai',
           '02513836111'
    FROM dual

    UNION ALL

    SELECT 'SUP_09',
           N'Công ty TNHH Coca-Cola Việt Nam',
           'cocacola@coca-cola.com.vn',
           N'KCN Tam Bình, Bình Dương',
           '02743820222'
    FROM dual

    UNION ALL

    SELECT 'SUP_10',
           N'Công ty CP Pepsico Việt Nam',
           'pepsico@pepsico.com.vn',
           N'KCN Việt Nam Singapore, Bình Dương',
           '02743750888'
    FROM dual

    UNION ALL

    SELECT 'SUP_11',
           N'Công ty CP Tân Hiệp Phát',
           'thp@thp.com.vn',
           N'KCN Mỹ Phước, Bình Dương',
           '02743771222'
    FROM dual

    UNION ALL

    SELECT 'SUP_12',
           N'Công ty CP Orion Vina',
           'orion@orion.vn',
           N'KCN Yên Phong, Bắc Ninh',
           '02223871999'
    FROM dual

    UNION ALL

    SELECT 'SUP_13',
           N'Công ty CP Kinh Đô (Kido)',
           'kido@kidobrands.com',
           N'141 Nguyễn Du, Quận 1, TP.HCM',
           '02839326262'
    FROM dual

    UNION ALL

    SELECT 'SUP_14',
           N'Công ty TNHH Haribo GmbH (Phân phối)',
           'haribo@haribo.com.vn',
           N'12 Lê Duẩn, Quận 1, TP.HCM',
           '02838221111'
    FROM dual

    UNION ALL

    SELECT 'SUP_15',
           N'Công ty TNHH Thương mại Thực phẩm Tươi Sống',
           'tuoisong@sgfresh.com.vn',
           N'50 Lý Thường Kiệt, Quận 10, TP.HCM',
           '02838556789'
    FROM dual
) src
ON (s.supplier_id = src.supplier_id)
WHEN MATCHED THEN UPDATE SET
    s.supplier_name = src.supplier_name,
    s.email = src.email,
    s.address = src.address,
    s.phone_number = src.phone_number,
    s.is_deleted = 0
WHEN NOT MATCHED THEN INSERT (
    supplier_id,
    supplier_name,
    email,
    address,
    phone_number,
    is_deleted
)
VALUES (
    src.supplier_id,
    src.supplier_name,
    src.email,
    src.address,
    src.phone_number,
    0
);

COMMIT;