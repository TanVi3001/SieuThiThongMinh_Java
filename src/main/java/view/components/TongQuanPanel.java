package view.components;

import business.sql.sales_order.StatisticSql;
import common.db.DatabaseConnection;
import java.awt.*;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

// IMPORT EVENTBUS ĐỂ LÀM REAL-TIME
import common.events.EventBus;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;

/**
 * TongQuanPanel — Dashboard tổng quan.
 */
public class TongQuanPanel extends JPanel {

    // =============================================
    // MÀU CHỦ ĐẠO
    // =============================================
    private static final Color COLOR_BG = new Color(243, 245, 250);
    private static final Color COLOR_PURPLE = new Color(99, 102, 241);
    private static final Color COLOR_BLUE = new Color(14, 165, 233);
    private static final Color COLOR_ORANGE = new Color(249, 115, 22);
    private static final Color COLOR_GREEN = new Color(34, 197, 94);
    private static final Color COLOR_WHITE = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_TEXT_DARK = new Color(30, 41, 59);
    private static final Color COLOR_TEXT_GRAY = new Color(100, 116, 139);

    // =============================================
    // FIELDS
    // =============================================
    private JLabel lblRevenue;
    private JLabel lblCustomers;
    private JLabel lblOrders;

    private DefaultCategoryDataset barDataset;
    private DefaultCategoryDataset lineDataset;
    private DefaultPieDataset pieDataset;

    private JPanel statsPanel;
    private DefaultTableModel tableModel;

    // =============================================
    // CONSTRUCTOR
    // =============================================
    public TongQuanPanel() {
        barDataset = new DefaultCategoryDataset();
        lineDataset = new DefaultCategoryDataset();
        pieDataset = new DefaultPieDataset();
        initComponents();
        loadRealData();

        // ==========================================================
        // 🌟 BẮT SÓNG REAL-TIME TẠI ĐÂY LÀ CHUẨN NHẤT
        // ==========================================================
        EventBus.subscribe(AppDataChangedEvent.class, event -> {
            if (event.getType() == AppEventType.ORDERS) {
                // Ép chạy trên luồng giao diện để không bị treo
                SwingUtilities.invokeLater(() -> {
                    System.out.println("TongQuanPanel đang tự update Real-time...");
                    loadRealData(); // Gọi lại hàm load dữ liệu
                });
            }
        });
    }

    // =============================================
    // INIT UI
    // =============================================
    private void initComponents() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(COLOR_BG);
        setBorder(new EmptyBorder(24, 24, 24, 24));

        // ------------------------------------------
        // DÒNG 1: 3 THẺ TỔNG QUAN
        // ------------------------------------------
        lblRevenue = new JLabel("0");
        lblCustomers = new JLabel("0");
        lblOrders = new JLabel("0");

        JPanel topCardsPanel = new JPanel(new GridLayout(1, 3, 16, 0));
        topCardsPanel.setOpaque(false);
        topCardsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        topCardsPanel.setPreferredSize(new Dimension(1000, 120));

        topCardsPanel.add(createSummaryCard("Hôm nay", "DOANH THU", lblRevenue, COLOR_PURPLE, IconHelper.revenue(24)));
        topCardsPanel.add(createSummaryCard("Hôm nay", "KHÁCH HÀNG", lblCustomers, COLOR_BLUE, IconHelper.customer(24)));
        topCardsPanel.add(createSummaryCard("Hôm nay", "ĐƠN HÀNG", lblOrders, COLOR_ORANGE, IconHelper.order(24)));

        add(topCardsPanel);
        add(Box.createRigidArea(new Dimension(0, 20)));

        // ------------------------------------------
        // DÒNG 2: 3 BIỂU ĐỒ
        // ------------------------------------------
        JPanel middleChartsPanel = new JPanel(new GridLayout(1, 3, 16, 0));
        middleChartsPanel.setOpaque(false);
        middleChartsPanel.setPreferredSize(new Dimension(1000, 290));
        middleChartsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 290));

        middleChartsPanel.add(wrapChart(createBarChartPanel(), "DOANH THU 5 THÁNG QUA", IconHelper.barChart(16)));
        middleChartsPanel.add(wrapChart(createLineChartPanel(), "LƯỢT KHÁCH TRONG TUẦN", IconHelper.lineChart(16)));
        middleChartsPanel.add(wrapChart(createPieChartPanel(), "TỶ TRỌNG NGÀNH HÀNG", IconHelper.pieChart(16)));

        add(middleChartsPanel);
        add(Box.createRigidArea(new Dimension(0, 20)));

        // ------------------------------------------
        // DÒNG 3: TỒN KHO + BẢNG ĐƠN HÀNG
        // ------------------------------------------
        JPanel bottomPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setPreferredSize(new Dimension(1000, 240));
        bottomPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));

        statsPanel = new JPanel();
        statsPanel.setLayout(new BoxLayout(statsPanel, BoxLayout.Y_AXIS));
        statsPanel.setBackground(COLOR_WHITE);
        statsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1, true),
                new EmptyBorder(16, 16, 16, 16)
        ));

        bottomPanel.add(statsPanel);
        bottomPanel.add(createOrderTablePanel());
        add(bottomPanel);
    }

    // =============================================
    // SUMMARY CARD — Bo góc, vòng tròn trang trí
    // =============================================
    private JPanel createSummaryCard(String subtitle, String title, JLabel valueLabel, Color bgColor, ImageIcon icon) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 15, 10, 15));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        JLabel lblSub = new JLabel(subtitle);
        lblSub.setForeground(new Color(255, 255, 255, 200));
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setIcon(icon);
        lblTitle.setIconTextGap(10);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);

        leftPanel.add(lblSub);
        leftPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        leftPanel.add(lblTitle);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        valueLabel.setForeground(Color.WHITE);
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        card.add(leftPanel, BorderLayout.WEST);
        card.add(valueLabel, BorderLayout.EAST);

        return card;
    }

    // =============================================
    // WRAP CHART
    // =============================================
    private JPanel wrapChart(JPanel chartPanel, String title, ImageIcon icon) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        header.setOpaque(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));

        header.add(new JLabel(icon));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(COLOR_TEXT_DARK);
        header.add(lblTitle);

        wrapper.add(header, BorderLayout.NORTH);
        wrapper.add(chartPanel, BorderLayout.CENTER);
        return wrapper;
    }

    // =============================================
    // TIÊU ĐỀ BOX (có icon)
    // =============================================
    private JLabel createBoxTitle(String titleStr, ImageIcon icon) {
        JLabel title = new JLabel("  " + titleStr, icon, JLabel.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(COLOR_TEXT_DARK);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        return title;
    }

    // =============================================
    // PROGRESS ROW
    // =============================================
    private JPanel createProgressRow(String name, int percent, String displayValue, Color color) {
        JPanel row = new JPanel(new BorderLayout(10, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JLabel lblName = new JLabel(name);
        lblName.setPreferredSize(new Dimension(145, 20));
        lblName.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblName.setForeground(COLOR_TEXT_DARK);

        JPanel progressTrack = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int h = 8, y = (getHeight() - h) / 2;
                g2.setColor(new Color(226, 232, 240));
                g2.fillRoundRect(0, y, getWidth(), h, h, h);
                int fillW = (int) (getWidth() * percent / 100.0);
                g2.setColor(color);
                g2.fillRoundRect(0, y, Math.max(fillW, h), h, h, h);
                g2.dispose();
            }
        };
        progressTrack.setOpaque(false);
        progressTrack.setPreferredSize(new Dimension(100, 20));

        JLabel lblVal = new JLabel(displayValue);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblVal.setForeground(color);
        lblVal.setPreferredSize(new Dimension(65, 20));
        lblVal.setHorizontalAlignment(JLabel.RIGHT);

        row.add(lblName, BorderLayout.WEST);
        row.add(progressTrack, BorderLayout.CENTER);
        row.add(lblVal, BorderLayout.EAST);
        return row;
    }

    // =============================================
    // BẢNG ĐƠN HÀNG MỚI NHẤT
    // =============================================
    private JPanel createOrderTablePanel() {
        JPanel tableContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
            }
        };
        tableContainer.setOpaque(false);

        JPanel tableHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        tableHeader.setOpaque(false);
        tableHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));
        tableHeader.add(new JLabel(IconHelper.bill(16)));
        JLabel tblTitle = new JLabel("ĐƠN HÀNG MỚI NHẤT");
        tblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        tblTitle.setForeground(COLOR_TEXT_DARK);
        tableHeader.add(tblTitle);
        tableContainer.add(tableHeader, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(
                new Object[][]{},
                new String[]{"STT", "MÃ ĐƠN", "TRẠNG THÁI", "TIẾN ĐỘ"}
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        JTable table = new JTable(tableModel);
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(241, 245, 249));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setSelectionBackground(new Color(239, 246, 255));
        table.setSelectionForeground(COLOR_TEXT_DARK);
        table.setBackground(COLOR_WHITE);
        table.setIntercellSpacing(new Dimension(0, 0));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(new Color(248, 250, 252));
        headerRenderer.setForeground(COLOR_TEXT_GRAY);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 11));
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setBorder(new EmptyBorder(8, 8, 8, 8));
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                setHorizontalAlignment(JLabel.CENTER);
                setFont(new Font("Segoe UI", Font.PLAIN, 12));
                setBorder(new EmptyBorder(0, 8, 0, 8));
                if (!sel) {
                    setBackground(row % 2 == 0 ? COLOR_WHITE : new Color(250, 252, 255));
                    setForeground(COLOR_TEXT_DARK);
                }
                if (col == 2 && val != null) {
                    String s = val.toString();
                    if (s.equalsIgnoreCase("COMPLETED") || s.equalsIgnoreCase("Hoàn thành")) {
                        setForeground(new Color(22, 163, 74));
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else if (s.equalsIgnoreCase("PROCESSING") || s.equalsIgnoreCase("Đang xử lý")) {
                        setForeground(new Color(217, 119, 6));
                        setFont(new Font("Segoe UI", Font.BOLD, 12));
                    } else {
                        setForeground(new Color(220, 38, 38));
                    }
                }
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(COLOR_WHITE);
        tableContainer.add(scroll, BorderLayout.CENTER);
        return tableContainer;
    }

    // =============================================
    // BIỂU ĐỒ
    // =============================================
    private JPanel createBarChartPanel() {
        JFreeChart chart = ChartFactory.createBarChart(
                null, "Tháng", "Triệu VNĐ",
                barDataset, PlotOrientation.VERTICAL, false, true, false);
        chart.setBackgroundPaint(COLOR_WHITE);
        chart.setBorderVisible(false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(COLOR_WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(new Color(241, 245, 249));
        plot.setDomainGridlinesVisible(false);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, COLOR_PURPLE);
        renderer.setDrawBarOutline(false);
        renderer.setShadowVisible(false);
        renderer.setMaximumBarWidth(0.35);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        domainAxis.setAxisLineVisible(false);
        domainAxis.setTickMarksVisible(false);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        rangeAxis.setAxisLineVisible(false);
        rangeAxis.setTickMarksVisible(false);

        ChartPanel panel = new ChartPanel(chart);
        panel.setPopupMenu(null);
        panel.setBackground(COLOR_WHITE);
        return panel;
    }

    private JPanel createLineChartPanel() {
        JFreeChart chart = ChartFactory.createLineChart(
                null, "Ngày", "Số đơn",
                lineDataset, PlotOrientation.VERTICAL, false, true, false);
        chart.setBackgroundPaint(COLOR_WHITE);
        chart.setBorderVisible(false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(COLOR_WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(new Color(241, 245, 249));
        plot.setDomainGridlinesVisible(false);

        LineAndShapeRenderer renderer = (LineAndShapeRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, COLOR_BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        renderer.setSeriesShapesVisible(0, true);

        plot.getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        plot.getDomainAxis().setAxisLineVisible(false);
        plot.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        plot.getRangeAxis().setAxisLineVisible(false);

        ChartPanel panel = new ChartPanel(chart);
        panel.setPopupMenu(null);
        panel.setBackground(COLOR_WHITE);
        return panel;
    }

    private JPanel createPieChartPanel() {
        JFreeChart chart = ChartFactory.createPieChart(
                null, pieDataset, true, true, false);
        chart.setBackgroundPaint(COLOR_WHITE);
        chart.setBorderVisible(false);

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(COLOR_WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        plot.setLabelBackgroundPaint(new Color(255, 255, 255, 200));
        plot.setLabelOutlinePaint(null);
        plot.setLabelShadowPaint(null);

        ChartPanel panel = new ChartPanel(chart);
        panel.setPopupMenu(null);
        panel.setBackground(COLOR_WHITE);
        return panel;
    }

    // =============================================
    // LOAD DỮ LIỆU THẬT TỪ DATABASE
    // =============================================
    public void loadRealData() {
        try (Connection con = DatabaseConnection.getConnection()) {
            DecimalFormat df = new DecimalFormat("#,###");

            // 1. THẺ TỔNG QUAN
            String sqlRev = "SELECT NVL(SUM(total_amount), 0) FROM ORDERS "
                    + "WHERE NVL(is_deleted, 0) = 0 "
                    + "AND UPPER(NVL(status, '')) <> 'CANCELLED' AND UPPER(NVL(status, '')) <> 'ĐÃ HỦY' "
                    + "AND TRUNC(order_date) = TRUNC(SYSDATE)";
            try (PreparedStatement ps = con.prepareStatement(sqlRev); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblRevenue.setText(df.format(rs.getDouble(1)));
                }
            }

            String sqlCus = "SELECT COUNT(*) FROM CUSTOMERS WHERE NVL(is_deleted, 0) = 0";
            try (PreparedStatement ps = con.prepareStatement(sqlCus); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblCustomers.setText(String.valueOf(rs.getInt(1)));
                }
            }

            String sqlOrd = "SELECT COUNT(*) FROM ORDERS "
                    + "WHERE NVL(is_deleted, 0) = 0 AND TRUNC(order_date) = TRUNC(SYSDATE)";
            try (PreparedStatement ps = con.prepareStatement(sqlOrd); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    lblOrders.setText(String.valueOf(rs.getInt(1)));
                }
            }

            // 2. BIỂU ĐỒ
            StatisticSql sqlDao = StatisticSql.getInstance();

            barDataset.clear();
            for (Map.Entry<String, Double> e : sqlDao.getRevenueByMonth().entrySet()) {
                barDataset.addValue(e.getValue() / 1_000_000.0, "Doanh thu", e.getKey());
            }

            lineDataset.clear();
            for (Map.Entry<String, Integer> e : sqlDao.getOrdersByDay().entrySet()) {
                lineDataset.addValue(e.getValue(), "Đơn", e.getKey());
            }

            pieDataset.clear();
            for (Map.Entry<String, Integer> e : sqlDao.getCategoryDistribution().entrySet()) {
                pieDataset.setValue(e.getKey(), e.getValue());
            }

            // 3. TỒN KHO
            statsPanel.removeAll();
            statsPanel.add(createBoxTitle("THỐNG KÊ TỒN KHO", IconHelper.stock(16)));
            statsPanel.add(Box.createRigidArea(new Dimension(0, 14)));

            String sqlStock = "SELECT p.product_name, NVL(i.quantity, 0) as qty "
                    + "FROM PRODUCTS p JOIN INVENTORY i ON p.product_id = i.product_id "
                    + "WHERE p.is_deleted = 0 AND i.is_deleted = 0 "
                    + "ORDER BY i.quantity DESC FETCH FIRST 4 ROWS ONLY";

            Color[] colors = {COLOR_PURPLE, COLOR_BLUE, COLOR_ORANGE, COLOR_GREEN};
            int idx = 0;
            try (PreparedStatement ps = con.prepareStatement(sqlStock); ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String name = rs.getString("product_name");
                    int qty = rs.getInt("qty");
                    statsPanel.add(createProgressRow(name, Math.min(qty, 100), qty + " cái", colors[idx % 4]));
                    statsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
                    idx++;
                }
            }
            statsPanel.revalidate();
            statsPanel.repaint();

            // 4. BẢNG ĐƠN HÀNG MỚI
            tableModel.setRowCount(0);
            int stt = 1;
            for (Map<String, Object> order : sqlDao.getRecentOrders(4)) {
                String orderId = (String) order.get("order_id");
                String status = (String) order.get("status");
                String progress = (status.equalsIgnoreCase("Hoàn thành") || status.equalsIgnoreCase("COMPLETED")) ? "100%"
                        : (status.equalsIgnoreCase("Đang xử lý") || status.equalsIgnoreCase("PROCESSING")) ? "50%"
                        : "20%";
                tableModel.addRow(new Object[]{stt++, orderId, status, progress});
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Lỗi khi kết nối dữ liệu: " + ex.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }
}
