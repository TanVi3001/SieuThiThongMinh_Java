-- ==========================================================
-- 008_backfill_product_supplier_from_receipts.sql
-- Purpose:
--   Link PRODUCTS.supplier_id from latest purchase receipts.
--   Fix Supplier Dashboard / Power BI product count.
-- Safe to re-run.
-- ==========================================================

SET DEFINE OFF;

-- ==========================================================
-- 1. Backfill PRODUCTS.supplier_id from latest purchase receipt
-- ==========================================================

MERGE INTO PRODUCTS p
USING (
    SELECT product_id, supplier_id
    FROM (
        SELECT
            d.product_id,
            r.supplier_id,
            r.created_at,
            ROW_NUMBER() OVER (
                PARTITION BY d.product_id
                ORDER BY r.created_at DESC
            ) AS rn
        FROM PURCHASE_RECEIPTS r
        JOIN PURCHASE_RECEIPT_DETAILS d
            ON d.receipt_id = r.receipt_id
        WHERE NVL(r.is_deleted, 0) = 0
          AND NVL(d.is_deleted, 0) = 0
          AND r.supplier_id IS NOT NULL
    )
    WHERE rn = 1
) src
ON (p.product_id = src.product_id)
WHEN MATCHED THEN UPDATE SET
    p.supplier_id = src.supplier_id
WHERE NVL(p.is_deleted, 0) = 0;

COMMIT;

-- ==========================================================
-- 2. Optional fallback:
--    If products still have no supplier_id, assign default SUP_01.
--    This helps old seed products appear in supplier dashboard.
-- ==========================================================

UPDATE PRODUCTS
SET supplier_id = 'SUP_01'
WHERE supplier_id IS NULL
  AND NVL(is_deleted, 0) = 0
  AND EXISTS (
      SELECT 1
      FROM SUPPLIERS s
      WHERE s.supplier_id = 'SUP_01'
        AND NVL(s.is_deleted, 0) = 0
  );

COMMIT;

-- ==========================================================
-- 3. Check result
-- ==========================================================

SELECT
    s.supplier_id,
    s.supplier_name,
    COUNT(DISTINCT p.product_id) AS product_count
FROM SUPPLIERS s
LEFT JOIN PRODUCTS p
    ON p.supplier_id = s.supplier_id
    AND NVL(p.is_deleted, 0) = 0
WHERE NVL(s.is_deleted, 0) = 0
GROUP BY s.supplier_id, s.supplier_name
ORDER BY product_count DESC, s.supplier_id;