CREATE OR REPLACE FUNCTION FUNC_GET_STORE_FINAL_REVENUE (
    p_from_date IN DATE DEFAULT NULL,
    p_to_date   IN DATE DEFAULT NULL,
    p_vat_rate  IN NUMBER DEFAULT 0.1
)
RETURN STORE_REVENUE_TABLE
PIPELINED
IS
BEGIN
    FOR r IN (
        SELECT
            s.store_id,

            /* Tổng bán hàng trước VAT */
            NVL((
                SELECT SUM(o.total_amount)
                FROM ORDERS o
                WHERE o.store_id = s.store_id
                  AND NVL(o.is_deleted, 0) = 0
                  AND (
                        UPPER(o.status) IN ('PAID', 'COMPLETED', 'SUCCESS')
                        OR o.status IN (N'Đã thanh toán', N'Hoàn thành')
                  )
                  AND (p_from_date IS NULL OR o.order_date >= p_from_date)
                  AND (p_to_date IS NULL OR o.order_date < p_to_date + 1)
            ), 0) AS total_sales_before_tax,

            /* Tổng nhập hàng đã gồm VAT */
            NVL((
                SELECT SUM(pr.total_after_tax)
                FROM PURCHASE_RECEIPTS pr
                WHERE pr.store_id = s.store_id
                  AND NVL(pr.is_deleted, 0) = 0
                  AND (p_from_date IS NULL OR pr.created_at >= CAST(p_from_date AS TIMESTAMP))
                  AND (p_to_date IS NULL OR pr.created_at < CAST(p_to_date + 1 AS TIMESTAMP))
            ), 0) AS total_import_after_tax,

            /* Tổng VAT nhập hàng */
            NVL((
                SELECT SUM(pr.total_tax)
                FROM PURCHASE_RECEIPTS pr
                WHERE pr.store_id = s.store_id
                  AND NVL(pr.is_deleted, 0) = 0
                  AND (p_from_date IS NULL OR pr.created_at >= CAST(p_from_date AS TIMESTAMP))
                  AND (p_to_date IS NULL OR pr.created_at < CAST(p_to_date + 1 AS TIMESTAMP))
            ), 0) AS total_import_tax

        FROM STORES s
        WHERE NVL(s.is_deleted, 0) = 0
    )
    LOOP
        PIPE ROW (
            STORE_REVENUE_OBJ(
                r.store_id,

                /* Tổng bán hàng gồm VAT */
                ROUND(r.total_sales_before_tax * (1 + p_vat_rate), 2),

                /* Tổng nhập hàng gồm VAT */
                ROUND(r.total_import_after_tax, 2),

                /* Tổng VAT = VAT bán hàng + VAT nhập hàng */
                ROUND(
                    r.total_sales_before_tax * p_vat_rate + r.total_import_tax,
                    2
                ),

                /* Doanh thu cuối = bán hàng gồm VAT - nhập hàng gồm VAT */
                ROUND(
                    r.total_sales_before_tax * (1 + p_vat_rate)
                    - r.total_import_after_tax,
                    2
                )
            )
        );
    END LOOP;

    RETURN;
END;
/

SELECT object_name, object_type, status
FROM user_objects
WHERE object_name = 'FUNC_GET_STORE_FINAL_REVENUE';

SELECT *
FROM TABLE(
    FUNC_GET_STORE_FINAL_REVENUE(
        DATE '2026-05-01',
        DATE '2026-05-31',
        0.1
    )
);

SELECT
    store_id AS "Mã chi nhánh",
    total_sales_amount AS "Tổng bán hàng gồm VAT",
    total_import_amount AS "Tổng nhập hàng gồm VAT",
    vat_amount AS "Tổng VAT",
    final_revenue AS "Doanh thu cuối cùng"
FROM TABLE(
    FUNC_GET_STORE_FINAL_REVENUE(
        DATE '2026-05-01',
        DATE '2026-05-31',
        0.1
    )
);