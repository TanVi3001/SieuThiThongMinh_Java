package business.sql.report;

import business.service.SessionManager;
import common.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
        LocalDateTime from = currentMonthFrom();
        return selectByStore(null, from, from.plusMonths(1));
    }

    public EfficiencySummary selectCurrentMonthSummaryForAdmin() {
        LocalDateTime from = currentMonthFrom();
        return selectSummary(null, from, from.plusMonths(1));
    }

    public List<EfficiencyRow> selectCurrentMonthByStoreForCurrentManager() {
        LocalDateTime from = currentMonthFrom();
        return selectByStore(resolveCurrentStoreId(), from, from.plusMonths(1));
    }

    public EfficiencySummary selectCurrentMonthSummaryForCurrentManager() {
        LocalDateTime from = currentMonthFrom();
        return selectSummary(resolveCurrentStoreId(), from, from.plusMonths(1));
    }

    public List<EfficiencyRow> selectByStoreForAdmin(LocalDateTime from, LocalDateTime to) {
        return selectByStore(null, from, to);
    }

    public EfficiencySummary selectSummaryForAdmin(LocalDateTime from, LocalDateTime to) {
        return selectSummary(null, from, to);
    }

    public List<EfficiencyRow> selectByStoreForCurrentManager(LocalDateTime from, LocalDateTime to) {
        return selectByStore(resolveCurrentStoreId(), from, to);
    }

    public EfficiencySummary selectSummaryForCurrentManager(LocalDateTime from, LocalDateTime to) {
        return selectSummary(resolveCurrentStoreId(), from, to);
    }

    public List<EfficiencyRow> selectCurrentMonthByStore(String storeId) {
        LocalDateTime from = currentMonthFrom();
        return selectByStore(storeId, from, from.plusMonths(1));
    }

    public EfficiencySummary selectCurrentMonthSummary(String storeId) {
        LocalDateTime from = currentMonthFrom();
        return selectSummary(storeId, from, from.plusMonths(1));
    }

    private List<EfficiencyRow> selectByStore(String storeId, LocalDateTime from, LocalDateTime to) {
        List<EfficiencyRow> rows = new ArrayList<>();
        LocalDateTime[] range = normalizeRange(from, to);

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
                  AND order_date >= ?
                  AND order_date < ?
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
                  AND created_at >= ?
                  AND created_at < ?
                GROUP BY store_id
            ) imports
                ON imports.store_id = s.store_id
            WHERE NVL(s.is_deleted, 0) = 0
        """);

        boolean scoped = storeId != null && !storeId.trim().isEmpty();

        if (scoped) {
            sql.append(" AND s.store_id = ? ");
        }

        sql.append(" ORDER BY total_revenue DESC, gross_profit DESC, s.store_id ");

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            bindRangeAndScope(ps, range[0], range[1], storeId, scoped);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapRow(rs));
                }
            }

        } catch (Exception ex) {
            System.err.println("[ImportSalesEfficiencySql] selectByStore error: " + ex.getMessage());
        }

        return rows;
    }

    private EfficiencySummary selectSummary(String storeId, LocalDateTime from, LocalDateTime to) {
        EfficiencySummary summary = new EfficiencySummary();
        LocalDateTime[] range = normalizeRange(from, to);

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
                      AND order_date >= ?
                      AND order_date < ?
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
                      AND created_at >= ?
                      AND created_at < ?
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

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            bindRangeAndScope(ps, range[0], range[1], storeId, scoped);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    summary.totalRevenue = rs.getDouble("total_revenue");
                    summary.totalImportCost = rs.getDouble("total_import_cost");
                    summary.grossProfit = rs.getDouble("gross_profit");
                    summary.grossProfitMargin = rs.getDouble("gross_profit_margin");
                }
            }

        } catch (Exception ex) {
            System.err.println("[ImportSalesEfficiencySql] selectSummary error: " + ex.getMessage());
        }

        return summary;
    }

    private void bindRangeAndScope(
            PreparedStatement ps,
            LocalDateTime from,
            LocalDateTime to,
            String storeId,
            boolean scoped
    ) throws Exception {
        ps.setTimestamp(1, Timestamp.valueOf(from));
        ps.setTimestamp(2, Timestamp.valueOf(to));
        ps.setTimestamp(3, Timestamp.valueOf(from));
        ps.setTimestamp(4, Timestamp.valueOf(to));

        if (scoped) {
            ps.setString(5, storeId.trim());
        }
    }

    private LocalDateTime[] normalizeRange(LocalDateTime from, LocalDateTime to) {
        LocalDateTime safeFrom = from;
        LocalDateTime safeTo = to;

        if (safeFrom == null) {
            safeFrom = currentMonthFrom();
        }
        if (safeTo == null || !safeTo.isAfter(safeFrom)) {
            safeTo = safeFrom.plusMonths(1);
        }

        return new LocalDateTime[]{safeFrom, safeTo};
    }

    private LocalDateTime currentMonthFrom() {
        return LocalDateTime.now()
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);
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
