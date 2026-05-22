package view;

import common.db.DatabaseConnection;
import common.report.ReportViewer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.util.HashMap;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

/**
 * AdminSystemPanel
 *
 * Trung tâm quản lý hệ thống cho Admin.
 * Gồm 3 tab:
 * - Tổng quan hệ thống
 * - Quản lý chi nhánh
 * - Báo cáo tổng hợp
 */
public class AdminSystemPanel extends JPanel {

    private static final int LOW_STOCK_THRESHOLD = 20;

    private final Color bg = new Color(245, 247, 251);
    private final Color white = Color.WHITE;
    private final Color text = new Color(15, 23, 42);
    private final Color muted = new Color(100, 116, 139);
    private final Color mutedLight = new Color(148, 163, 184);
    private final Color border = new Color(226, 232, 240);

    private final Color primary = new Color(255, 111, 0);
    private final Color blue = new Color(37, 99, 235);
    private final Color green = new Color(16, 185, 129);
    private final Color orange = new Color(245, 158, 11);
    private final Color red = new Color(239, 68, 68);
    private final Color purple = new Color(124, 58, 237);

    private final DecimalFormat moneyFmt = new DecimalFormat("#,###");

    private JLabel lblStoreTotal;
    private JLabel lblStoreActive;
    private JLabel lblTodayRevenue;
    private JLabel lblMonthRevenue;
    private JLabel lblOrderTotal;
    private JLabel lblLowStock;
    private JLabel lblOnlineSessions;

    private JTable tblOverviewRevenueByStore;
    private JTable tblOverviewInventoryByStore;
    private JTable tblReportRevenueByStore;
    private JTable tblReportInventoryByStore;
    private JTable tblTopEmployee;
    private JTable tblLowStock;

    public AdminSystemPanel() {
        setLayout(new BorderLayout());
        setBackground(bg);
        setBorder(new EmptyBorder(22, 30, 22, 30));
        initUI();
        reloadAll();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setOpaque(false);

        root.add(createHeader(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabs.setBackground(bg);
        tabs.setForeground(text);
        tabs.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));

        tabs.addTab("Tổng quan hệ thống", createOverviewTab());
        tabs.addTab("Quản lý chi nhánh", new StoreManagementPanel());
        tabs.addTab("Báo cáo tổng hợp", createReportTab());

        root.add(tabs, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);
    }

    private JPanel createHeader() {
        JPanel p = new JPanel(new BorderLayout(16, 0));
        p.setOpaque(false);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Quản lý hệ thống");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(text);

        JLabel sub = new JLabel("Quản trị toàn bộ chi nhánh, doanh thu, tồn kho, nhân viên và báo cáo vận hành");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(mutedLight);

        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(sub);

        JButton btnExportReport = createPrimaryButton("Xuất report", primary);
        btnExportReport.addActionListener(e -> exportSystemReport());

        JButton btnReload = createPrimaryButton("Làm mới", blue);
        btnReload.addActionListener(e -> reloadAll());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(btnExportReport);
        actions.add(btnReload);

        p.add(textPanel, BorderLayout.WEST);
        p.add(actions, BorderLayout.EAST);

        return p;
    }

    private JButton createPrimaryButton(String text, Color bgColor) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 18, 10, 18));
        return btn;
    }

    private JPanel createOverviewTab() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(18, 0, 0, 0));

        page.add(createPageIntro(
                "Tổng quan hệ thống",
                "Theo dõi nhanh tình hình chi nhánh, doanh thu, tồn kho và phiên hoạt động"
        ), BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(2, 4, 14, 14));
        cards.setOpaque(false);

        lblStoreTotal = new JLabel("0");
        lblStoreActive = new JLabel("0");
        lblTodayRevenue = new JLabel("0 đ");
        lblMonthRevenue = new JLabel("0 đ");
        lblOrderTotal = new JLabel("0");
        lblLowStock = new JLabel("0");
        lblOnlineSessions = new JLabel("0");

        cards.add(statCard("Tổng chi nhánh", lblStoreTotal, blue));
        cards.add(statCard("Đang hoạt động", lblStoreActive, green));
        cards.add(statCard("Doanh thu hôm nay", lblTodayRevenue, orange));
        cards.add(statCard("Doanh thu tháng hợp lệ", lblMonthRevenue, orange));
        cards.add(statCard("Tổng đơn hàng", lblOrderTotal, blue));
        cards.add(statCard("Tồn kho thấp", lblLowStock, red));
        cards.add(statCard("Phiên online", lblOnlineSessions, green));
        cards.add(statCard("Trạng thái hệ thống", new JLabel("OK"), purple));

        content.add(cards, BorderLayout.NORTH);

        JPanel tables = new JPanel(new GridLayout(1, 2, 18, 0));
        tables.setOpaque(false);

        tblOverviewRevenueByStore = table(new String[]{"Mã CN", "Chi nhánh", "Số đơn tháng", "Doanh thu tháng"});
        tblOverviewInventoryByStore = table(new String[]{"Mã CN", "Chi nhánh", "Mặt hàng", "Tổng tồn", "Tồn thấp"});

        tables.add(cardWithTable(
                "Doanh thu tháng theo chi nhánh",
                "Chỉ tính hóa đơn hoàn thành trong tháng hiện tại",
                tblOverviewRevenueByStore
        ));
        tables.add(cardWithTable(
                "Tồn kho theo chi nhánh",
                "Tổng hợp số lượng tồn và số mặt hàng tồn thấp",
                tblOverviewInventoryByStore
        ));

        content.add(tables, BorderLayout.CENTER);
        page.add(content, BorderLayout.CENTER);

        return page;
    }

    private JPanel createReportTab() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(18, 0, 0, 0));

        page.add(createPageIntro(
                "Báo cáo tổng hợp",
                "Dữ liệu tổng hợp toàn hệ thống phục vụ báo cáo, kiểm tra và xuất report"
        ), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, 18, 18));
        grid.setOpaque(false);

        tblReportRevenueByStore = table(new String[]{"Mã CN", "Chi nhánh", "Số đơn tháng", "Doanh thu tháng"});
        tblReportInventoryByStore = table(new String[]{"Mã CN", "Chi nhánh", "Mặt hàng", "Tổng tồn", "Tồn thấp"});
        tblTopEmployee = table(new String[]{"Chi nhánh", "Mã NV", "Nhân viên", "Số đơn", "Doanh thu"});
        tblLowStock = table(new String[]{"Chi nhánh", "Mã SP", "Sản phẩm", "Tồn", "Mức cảnh báo"});

        grid.add(cardWithTable(
                "Doanh thu tháng toàn hệ thống",
                "Tổng hợp doanh thu tháng theo từng chi nhánh",
                tblReportRevenueByStore
        ));
        grid.add(cardWithTable(
                "Tồn kho toàn hệ thống",
                "Tổng tồn kho và số mặt hàng cần xử lý",
                tblReportInventoryByStore
        ));
        grid.add(cardWithTable(
                "Top nhân viên theo doanh thu",
                "Ẩn Manager, chỉ thống kê nhân viên bán hàng/kho",
                tblTopEmployee
        ));
        grid.add(cardWithTable(
                "Cảnh báo tồn kho toàn hệ thống",
                "Các sản phẩm có tồn kho nhỏ hơn hoặc bằng " + LOW_STOCK_THRESHOLD,
                tblLowStock
        ));

        page.add(grid, BorderLayout.CENTER);

        return page;
    }

    private JPanel createPageIntro(String title, String subTitle) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setForeground(text);

        JLabel lblSub = new JLabel(subTitle);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(mutedLight);

        p.add(lblTitle);
        p.add(Box.createVerticalStrut(4));
        p.add(lblSub);
        return p;
    }

    private JPanel statCard(String title, JLabel value, Color accent) {
        RoundedPanel card = new RoundedPanel(18, white);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        RoundedPanel icon = new RoundedPanel(14, withAlpha(accent, 28));
        icon.setPreferredSize(new Dimension(44, 44));
        icon.setLayout(new BorderLayout());

        JLabel dot = new JLabel("●", SwingConstants.CENTER);
        dot.setFont(new Font("Segoe UI", Font.BOLD, 22));
        dot.setForeground(accent);
        icon.add(dot, BorderLayout.CENTER);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(muted);

        value.setFont(new Font("Segoe UI", Font.BOLD, 24));
        value.setForeground(text);

        textPanel.add(t);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(value);

        card.add(icon, BorderLayout.WEST);
        card.add(textPanel, BorderLayout.CENTER);

        return wrapWithBorder(card);
    }

    private JPanel cardWithTable(String title, String subtitle, JTable table) {
        RoundedPanel card = new RoundedPanel(18, white);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(text);

        JLabel sub = new JLabel(subtitle);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(mutedLight);

        header.add(lbl);
        header.add(Box.createVerticalStrut(3));
        header.add(sub);

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(border));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(14);

        card.add(header, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);

        return wrapWithBorder(card);
    }

    private JPanel wrapWithBorder(JPanel panel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private JTable table(String[] columns) {
        DefaultTableModel model = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable t = new JTable(model);
        t.setRowHeight(36);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setSelectionBackground(new Color(219, 234, 254));
        t.setSelectionForeground(text);

        JTableHeader header = t.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setPreferredSize(new Dimension(0, 38));
        header.setBackground(new Color(243, 246, 250));
        header.setForeground(new Color(15, 23, 42));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer renderer = new ZebraCenterRenderer();
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        return t;
    }

    private void reloadAll() {
        reloadCards();
        reloadRevenueByStore(tblOverviewRevenueByStore);
        reloadRevenueByStore(tblReportRevenueByStore);
        reloadInventoryByStore(tblOverviewInventoryByStore);
        reloadInventoryByStore(tblReportInventoryByStore);
        reloadTopEmployee();
        reloadLowStock();
    }

    private void reloadCards() {
        lblStoreTotal.setText(String.valueOf(scalarLong("""
            SELECT COUNT(*)
            FROM STORES
            WHERE NVL(is_deleted, 0) = 0
        """)));

        lblStoreActive.setText(String.valueOf(scalarLong("""
            SELECT COUNT(*)
            FROM STORES
            WHERE NVL(is_deleted, 0) = 0
        """)));

        lblTodayRevenue.setText(money(scalarDouble("""
            SELECT NVL(SUM(o.total_amount), 0)
            FROM STORES s
            JOIN ORDERS o
                ON o.store_id = s.store_id
               AND NVL(o.is_deleted, 0) = 0
               AND TRUNC(o.order_date) = TRUNC(SYSDATE)
               AND (
                    UPPER(NVL(o.status, '')) = 'COMPLETED'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
               )
            WHERE NVL(s.is_deleted, 0) = 0
        """)));

        lblMonthRevenue.setText(money(scalarDouble("""
            SELECT NVL(SUM(o.total_amount), 0)
            FROM STORES s
            JOIN ORDERS o
                ON o.store_id = s.store_id
               AND NVL(o.is_deleted, 0) = 0
               AND TRUNC(o.order_date, 'MM') = TRUNC(SYSDATE, 'MM')
               AND (
                    UPPER(NVL(o.status, '')) = 'COMPLETED'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
               )
            WHERE NVL(s.is_deleted, 0) = 0
        """)));

        lblOrderTotal.setText(String.valueOf(scalarLong("""
            SELECT COUNT(*)
            FROM STORES s
            JOIN ORDERS o
                ON o.store_id = s.store_id
               AND NVL(o.is_deleted, 0) = 0
               AND (
                    UPPER(NVL(o.status, '')) = 'COMPLETED'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
               )
            WHERE NVL(s.is_deleted, 0) = 0
        """)));

        lblLowStock.setText(String.valueOf(scalarLong("""
            SELECT COUNT(*)
            FROM INVENTORY
            WHERE NVL(is_deleted, 0) = 0
              AND NVL(quantity, 0) <= 20
        """)));

        lblOnlineSessions.setText(String.valueOf(scalarLong("""
            SELECT COUNT(*)
            FROM ACCOUNT_SESSIONS
            WHERE status = 'ACTIVE'
              AND NVL(is_deleted, 0) = 0
              AND last_heartbeat_at >= SYSTIMESTAMP - INTERVAL '30' SECOND
        """)));
    }

    private void reloadRevenueByStore(JTable targetTable) {
        fillTable(targetTable, """
            SELECT s.store_id,
                   NVL(s.store_name, s.address) AS store_name,
                   COUNT(o.order_id) AS total_orders,
                   NVL(SUM(o.total_amount), 0) AS revenue
            FROM STORES s
            LEFT JOIN ORDERS o
                ON o.store_id = s.store_id
               AND NVL(o.is_deleted, 0) = 0
               AND TRUNC(o.order_date, 'MM') = TRUNC(SYSDATE, 'MM')
               AND (
                    UPPER(NVL(o.status, '')) = 'COMPLETED'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
               )
            WHERE NVL(s.is_deleted, 0) = 0
            GROUP BY s.store_id, NVL(s.store_name, s.address)
            ORDER BY revenue DESC, s.store_id
        """, 4, true);
    }

    private void reloadInventoryByStore(JTable targetTable) {
        fillTable(targetTable, """
            SELECT s.store_id,
                   NVL(s.store_name, s.address) AS store_name,
                   COUNT(DISTINCT i.product_id) AS product_count,
                   NVL(SUM(NVL(i.quantity, 0)), 0) AS total_stock,
                   SUM(CASE WHEN NVL(i.quantity, 0) <= 20 THEN 1 ELSE 0 END) AS low_stock
            FROM STORES s
            LEFT JOIN INVENTORY i
                ON i.store_id = s.store_id
               AND NVL(i.is_deleted, 0) = 0
            WHERE NVL(s.is_deleted, 0) = 0
            GROUP BY s.store_id, NVL(s.store_name, s.address)
            ORDER BY s.store_id
        """, 5, false);
    }

    private void reloadTopEmployee() {
        fillTable(tblTopEmployee, """
            SELECT *
            FROM (
                SELECT NVL(s.store_name, e.store_id) AS store_name,
                       e.employee_id,
                       e.employee_name,
                       COUNT(o.order_id) AS total_orders,
                       NVL(SUM(o.total_amount), 0) AS revenue
                FROM EMPLOYEES e
                LEFT JOIN STORES s
                    ON s.store_id = e.store_id
                LEFT JOIN ORDERS o
                    ON o.employee_id = e.employee_id
                   AND o.store_id = e.store_id
                   AND NVL(o.is_deleted, 0) = 0
                   AND TRUNC(o.order_date, 'MM') = TRUNC(SYSDATE, 'MM')
                   AND (
                        UPPER(NVL(o.status, '')) = 'COMPLETED'
                        OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                        OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
                   )
                WHERE NVL(e.is_deleted, 0) = 0
                  AND e.role_id IN ('R_STAFF_SALE', 'R_STAFF_VIEW_PROD')
                GROUP BY NVL(s.store_name, e.store_id), e.employee_id, e.employee_name
                ORDER BY revenue DESC, total_orders DESC
            )
            WHERE ROWNUM <= 10
        """, 5, true);
    }

    private void reloadLowStock() {
        fillTable(tblLowStock, """
            SELECT NVL(s.store_name, i.store_id) AS store_name,
                   i.product_id,
                   p.product_name,
                   NVL(i.quantity, 0) AS quantity,
                   20 AS min_quantity
            FROM INVENTORY i
            LEFT JOIN PRODUCTS p
                ON p.product_id = i.product_id
            LEFT JOIN STORES s
                ON s.store_id = i.store_id
            WHERE NVL(i.is_deleted, 0) = 0
              AND NVL(i.quantity, 0) <= 20
            ORDER BY NVL(i.quantity, 0) ASC
        """, 5, false);
    }

    private void exportSystemReport() {
        try {
            HashMap<String, Object> params = new HashMap<>();

            params.put("P_REPORT_TITLE", "BÁO CÁO QUẢN LÝ HỆ THỐNG");
            params.put("P_REPORT_SUBTITLE", "Tổng hợp chi nhánh, doanh thu, tồn kho, nhân viên và cảnh báo vận hành");
            params.put("P_TOTAL_STORES", safeText(lblStoreTotal));
            params.put("P_ACTIVE_STORES", safeText(lblStoreActive));
            params.put("P_TODAY_REVENUE", safeText(lblTodayRevenue));
            params.put("P_MONTH_REVENUE", safeText(lblMonthRevenue));
            params.put("P_TOTAL_ORDERS", safeText(lblOrderTotal));
            params.put("P_LOW_STOCK", safeText(lblLowStock));
            params.put("P_ONLINE_SESSIONS", safeText(lblOnlineSessions));

            ReportViewer.showReport("/reports/AdminSystemReport.jrxml", params);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể mở báo cáo hệ thống:\n" + ex.getMessage(),
                    "Lỗi report",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private String safeText(JLabel label) {
        return label == null || label.getText() == null ? "0" : label.getText();
    }

    private long scalarLong(String sql) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0L;

        } catch (Exception e) {
            System.err.println("[AdminSystemPanel] scalarLong error: " + e.getMessage());
            return 0L;
        }
    }

    private double scalarDouble(String sql) {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getDouble(1) : 0.0;

        } catch (Exception e) {
            System.err.println("[AdminSystemPanel] scalarDouble error: " + e.getMessage());
            return 0.0;
        }
    }

    private void fillTable(JTable table, String sql, int columnCount, boolean lastMoney) {
        if (table == null) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Object[] row = new Object[columnCount];

                for (int i = 0; i < columnCount; i++) {
                    Object value = rs.getObject(i + 1);

                    if (lastMoney && i == columnCount - 1 && value instanceof Number) {
                        row[i] = money(((Number) value).doubleValue());
                    } else {
                        row[i] = value == null ? "—" : value;
                    }
                }

                model.addRow(row);
            }

        } catch (Exception e) {
            System.err.println("[AdminSystemPanel] fillTable error: " + e.getMessage());
        }
    }

    private String money(double value) {
        return moneyFmt.format(value) + " đ";
    }

    private Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private static class RoundedPanel extends JPanel {

        private final int radius;
        private final Color backgroundColor;

        RoundedPanel(int radius, Color backgroundColor) {
            this.radius = radius;
            this.backgroundColor = backgroundColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private class ZebraCenterRenderer extends DefaultTableCellRenderer {

        ZebraCenterRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(new EmptyBorder(0, 6, 0, 6));
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (isSelected) {
                c.setBackground(new Color(219, 234, 254));
                c.setForeground(text);
            } else {
                c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                c.setForeground(text);
            }

            setFont(new Font("Segoe UI", Font.PLAIN, 12));
            return c;
        }
    }
}
