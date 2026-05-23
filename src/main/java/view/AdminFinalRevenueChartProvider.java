package view;

import common.db.DatabaseConnection;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * Tạo biểu đồ "doanh thu cuối cùng" cho AdminSystemReport.
 *
 * Doanh thu cuối cùng ở đây được hiểu là lãi gộp tạm tính:
 * doanh thu bán hàng - tiền nhập kho sau VAT trong tháng hiện tại.
 */
public final class AdminFinalRevenueChartProvider {

    private AdminFinalRevenueChartProvider() {
    }

    public static BufferedImage createFinalRevenueChartImage() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        double maxAbsMillion = 0.0;
        boolean hasData = false;

        String sql = """
            SELECT s.store_id,
                   NVL(s.store_name, s.address) AS store_name,
                   NVL(sales.total_revenue, 0) AS total_revenue,
                   NVL(imports.total_import_cost, 0) AS total_import_cost,
                   NVL(sales.total_revenue, 0) - NVL(imports.total_import_cost, 0) AS gross_profit
            FROM stores s
            LEFT JOIN (
                SELECT store_id,
                       SUM(total_amount) AS total_revenue
                FROM orders
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
                FROM purchase_receipts
                WHERE NVL(is_deleted, 0) = 0
                  AND created_at >= TRUNC(SYSDATE, 'MM')
                  AND created_at < ADD_MONTHS(TRUNC(SYSDATE, 'MM'), 1)
                GROUP BY store_id
            ) imports
                ON imports.store_id = s.store_id
            WHERE NVL(s.is_deleted, 0) = 0
            ORDER BY gross_profit DESC, total_revenue DESC, s.store_id
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String storeName = rs.getString("store_name");
                if (storeName == null || storeName.trim().isEmpty()) {
                    storeName = rs.getString("store_id");
                }
                storeName = shorten(storeName, 18);

                double revenueMillion = rs.getDouble("total_revenue") / 1_000_000.0;
                double importMillion = rs.getDouble("total_import_cost") / 1_000_000.0;
                double grossProfitMillion = rs.getDouble("gross_profit") / 1_000_000.0;

                dataset.addValue(revenueMillion, "Doanh thu", storeName);
                dataset.addValue(importMillion, "Tiền nhập", storeName);
                dataset.addValue(grossProfitMillion, "Lãi gộp", storeName);

                maxAbsMillion = Math.max(maxAbsMillion, Math.abs(revenueMillion));
                maxAbsMillion = Math.max(maxAbsMillion, Math.abs(importMillion));
                maxAbsMillion = Math.max(maxAbsMillion, Math.abs(grossProfitMillion));
                hasData = true;
            }
        } catch (Exception ex) {
            System.err.println("[AdminFinalRevenueChartProvider] createFinalRevenueChartImage error: " + ex.getMessage());
        }

        if (!hasData) {
            dataset.addValue(0, "Doanh thu", "Không có dữ liệu");
            dataset.addValue(0, "Tiền nhập", "Không có dữ liệu");
            dataset.addValue(0, "Lãi gộp", "Không có dữ liệu");
            maxAbsMillion = 10;
        }

        JFreeChart chart = ChartFactory.createBarChart(
                null,
                "Chi nhánh",
                "Giá trị (triệu VND)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("Segoe UI", Font.BOLD, 13));
            chart.getLegend().setBackgroundPaint(Color.WHITE);
        }

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(248, 250, 252));
        plot.setOutlinePaint(new Color(71, 85, 105));
        plot.setOutlineStroke(new BasicStroke(1.3f));
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(new Color(148, 163, 184));
        plot.setRangeGridlineStroke(new BasicStroke(1.15f));
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(new Color(203, 213, 225));
        plot.setDomainGridlineStroke(new BasicStroke(0.9f));

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        domainAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 14));
        domainAxis.setCategoryMargin(0.25);
        domainAxis.setLowerMargin(0.06);
        domainAxis.setUpperMargin(0.06);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        double upper = maxAbsMillion <= 0 ? 10 : maxAbsMillion * 1.25;
        rangeAxis.setRange(-upper, upper);
        rangeAxis.setTickUnit(new NumberTickUnit(niceTickUnit(upper)));
        rangeAxis.setNumberFormatOverride(new DecimalFormat("#,##0.#"));
        rangeAxis.setTickLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        rangeAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 14));

        BarRenderer renderer = new BarRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.08);
        renderer.setItemMargin(0.12);
        renderer.setSeriesPaint(0, new Color(245, 158, 11, 210));
        renderer.setSeriesPaint(1, new Color(124, 58, 237, 190));
        renderer.setSeriesPaint(2, new Color(16, 185, 129, 210));
        plot.setRenderer(renderer);

        return chart.createBufferedImage(1200, 650);
    }

    private static double niceTickUnit(double maxValue) {
        if (maxValue <= 0) {
            return 1;
        }
        double raw = maxValue / 5.0;
        double pow = Math.pow(10, Math.floor(Math.log10(raw)));
        double normalized = raw / pow;
        if (normalized <= 1) {
            return pow;
        }
        if (normalized <= 2) {
            return 2 * pow;
        }
        if (normalized <= 5) {
            return 5 * pow;
        }
        return 10 * pow;
    }

    private static String shorten(String value, int maxLength) {
        String safe = value == null ? "Không rõ" : value.trim();
        if (safe.length() <= maxLength) {
            return safe;
        }
        return safe.substring(0, maxLength) + "...";
    }
}
