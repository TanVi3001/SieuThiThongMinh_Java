package business.sql.report;

import business.service.SessionManager;
import common.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public final class ImportSalesEfficiencySql {

    private static final ImportSalesEfficiencySql INSTANCE = new ImportSalesEfficiencySql();

    private ImportSalesEfficiencySql() {
    }

    public static ImportSalesEfficiencySql getInstance() {
        return INSTANCE;
    }

    public static final class EfficiencyRow {
        public String storeId;
        public String storeName;
        public long totalOrders;
        public double totalRevenue;
        public double totalImportCost;
        public double grossProfit;
        public double grossProfitMargin;
    }

    public static final class EfficiencySummary {
        public double totalRevenue;
        public double totalImportCost;
        public double grossProfit;
        public double grossProfitMargin;
    }

    public List<EfficiencyRow> selectCurrentMonthByStoreForAdmin() {
        return selectCurrentMonthByStore(null);
    }

    public EfficiencySummary selectCurrentMonthSummaryForAdmin() {
        return selectCurrentMonthSummary(null);
    }

    public List<EfficiencyRow> selectCurrentMonthByStoreForCurrentManager() {
        return selectCurrentMonthByStore(resolveCurrentStoreId());
    }

    public EfficiencySummary selectCurrentMonthSummaryForCurrentManager() {
        return selectCurrentMonthSummary(resolveCurrentStoreId());
    }

    public List<EfficiencyRow> selectCurrentMonthByStore(String storeId) {
        List<EfficiencyRow> rows = new ArrayList<>();

        StringBuilder sql = new StringBuilder("""
            SELECT s.store_id,
                   NVL(s.store_name, s.address) AS store_name,
                   NVL(sales.total_orders, 0) AS total_orders,
                   NVL(sales.total_revenue, 0) AS total_revenue,
                   NVL(imports.total_import_cost, 0) AS total_import_cost,
                   NVL(sales.total_revenue, 0) - NVL(imports.total_import_cost, 0) AS gross_profit,
                   CASE
                       WHEN NVL(sales.total_revenue, 0) = 0 THEN 0
                       ELSE ROUND(
                           (NVL(sales.total_revenue, 0) - NVL(imports.total_import_cost, 0))
                           / NVL(sales.total_revenue, 0) * 100,
                           2
                       )
                   END AS gross_profit_margin
            FROM STORES s
            LEFT JOIN (
                SELECT store_id,
                       COUNT(order_id) AS total_orders,
                       SUM(total_amount) AS total_revenue
                FROM ORDERS
                WHERE NVL(is_deleted, 0) = 0
                  AND order_date >= TRUNC(SYSDATE, 'MM')
                  AND order_date < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)
                  AND (
                       UPPER(NVL(status, '')) = 'COMPLETED'
                       OR UPPER(NVL(status, '')) LIKE '%HOÀN THÀNH%'
                       OR UPPER(NVL(status, '')) LIKE '%HOAN THANH%'
                  )
                GROUP BY store_id
            ) sales
                ON sales.store_id = s.store_id
            LEFT JOIN (
                SELECT store_id,
                       SUM(total_after_tax) AS total_import_cost
                FROM PURCHASE_RECEIPTS
                WHERE NVL(is_deleted, 0) = 0
                  AND created_at >= TRUNC(SYSDATE, 'MM')
                  AND created_at < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)
                GROUP BY store_id
            ) imports
                ON imports.store_id = s.store_id
            WHERE NVL(s.is_deleted, 0) = 0
        """);

        boolean scoped = storeId != null && !storeId.trim().isEmpty();

        if (scoped) {
            sql.append(" AND s.store_id = ? ");
        }

        sql.append(" ORDER BY gross_profit DESC, total_revenue DESC, s.store_id ");

        try (
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())
        ) {
            if (scoped) {
                ps.setString(1, storeId.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
            }

        } catch (Exception ex) {
            System.err.println("[ImportSalesEfficiencySql] selectCurrentMonthByStore error: " + ex.getMessage());
        }

        return rows;
    }

    public EfficiencySummary selectCurrentMonthSummary(String storeId) {
        EfficiencySummary summary = new EfficiencySummary();

        StringBuilder sql = new StringBuilder("""
            SELECT NVL(SUM(t.total_revenue), 0) AS total_revenue,
                   NVL(SUM(t.total_import_cost), 0) AS total_import_cost,
                   NVL(SUM(t.total_revenue), 0) - NVL(SUM(t.total_import_cost), 0) AS gross_profit,
                   CASE
                       WHEN NVL(SUM(t.total_revenue), 0) = 0 THEN 0
                       ELSE ROUND(
                           (NVL(SUM(t.total_revenue), 0) - NVL(SUM(t.total_import_cost), 0))
                           / NVL(SUM(t.total_revenue), 0) * 100,
                           2
                       )
                   END AS gross_profit_margin
            FROM (
                SELECT s.store_id,
                       NVL(sales.total_revenue, 0) AS total_revenue,
                       NVL(imports.total_import_cost, 0) AS total_import_cost
                FROM STORES s
                LEFT JOIN (
                    SELECT store_id,
                           SUM(total_amount) AS total_revenue
                    FROM ORDERS
                    WHERE NVL(is_deleted, 0) = 0
                      AND order_date >= TRUNC(SYSDATE, 'MM')
                      AND order_date < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)
                      AND (
                           UPPER(NVL(status, '')) = 'COMPLETED'
                           OR UPPER(NVL(status, '')) LIKE '%HOÀN THÀNH%'
                           OR UPPER(NVL(status, '')) LIKE '%HOAN THANH%'
                      )
                    GROUP BY store_id
                ) sales
                    ON sales.store_id = s.store_id
                LEFT JOIN (
                    SELECT store_id,
                           SUM(total_after_tax) AS total_import_cost
                    FROM PURCHASE_RECEIPTS
                    WHERE NVL(is_deleted, 0) = 0
                      AND created_at >= TRUNC(SYSDATE, 'MM')
                      AND created_at < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)
                    GROUP BY store_id
                ) imports
                    ON imports.store_id = s.store_id
                WHERE NVL(s.is_deleted, 0) = 0
        """);

        boolean scoped = storeId != null && !storeId.trim().isEmpty();

        if (scoped) {
            sql.append(" AND s.store_id = ? ");
        }

        sql.append(" ) t ");

        try (
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql.toString())
        ) {
            if (scoped) {
                ps.setString(1, storeId.trim());
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    summary.totalRevenue = rs.getDouble("total_revenue");
                    summary.totalImportCost = rs.getDouble("total_import_cost");
                    summary.grossProfit = rs.getDouble("gross_profit");
                    summary.grossProfitMargin = rs.getDouble("gross_profit_margin");
                }
            }

        } catch (Exception ex) {
            System.err.println("[ImportSalesEfficiencySql] selectCurrentMonthSummary error: " + ex.getMessage());
        }

        return summary;
    }

    private EfficiencyRow mapRow(ResultSet rs) throws Exception {
        EfficiencyRow row = new EfficiencyRow();

        row.storeId = rs.getString("store_id");
        row.storeName = rs.getString("store_name");
        row.totalOrders = rs.getLong("total_orders");
        row.totalRevenue = rs.getDouble("total_revenue");
        row.totalImportCost = rs.getDouble("total_import_cost");
        row.grossProfit = rs.getDouble("gross_profit");
        row.grossProfitMargin = rs.getDouble("gross_profit_margin");

        return row;
    }

    private String resolveCurrentStoreId() {
        try {
            String storeId = SessionManager.getCurrentStoreId();
            return storeId == null || storeId.trim().isEmpty() ? null : storeId.trim();
        } catch (Exception ignored) {
            return null;
        }
    }
}