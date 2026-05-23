package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import common.db.DatabaseConnection;
import common.report.ReportViewer;
import java.awt.BasicStroke;
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
import java.awt.image.BufferedImage;
import java.awt.geom.Ellipse2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYDifferenceRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.util.Rotation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;

/**
 * AdminSystemPanel
 *
 * Trung tâm quản lý hệ thống cho Admin. Gồm 3 tab: - Tổng quan hệ thống - Quản
 * lý chi nhánh - Báo cáo tổng hợp
 */
public class AdminSystemPanel extends JPanel {

    private static final int LOW_STOCK_THRESHOLD = 20;

    private Timer realtimeReloadTimer;
    private volatile boolean reloading = false;

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
        initRealtime();
        reloadAll();
    }

    private void initRealtime() {
        realtimeReloadTimer = new Timer(350, e -> reloadAllSafely());
        realtimeReloadTimer.setRepeats(false);

        EventBus.subscribe(AppDataChangedEvent.class, event -> {
            if (event == null || event.getType() == null) {
                return;
            }

            AppEventType type = event.getType();

            boolean shouldReload
                    = type == AppEventType.ORDERS
                    || type == AppEventType.INVENTORY
                    || type == AppEventType.PRODUCTS
                    || type == AppEventType.CUSTOMERS
                    || type == AppEventType.EMPLOYEES
                    || type == AppEventType.STORE_INFO
                    || type == AppEventType.ACCOUNT_SECURITY
                    || type == AppEventType.SYSTEM_CONFIG
                    || type == AppEventType.DASHBOARD
                    || type == AppEventType.STATISTICS
                    || type == AppEventType.INVENTORY_ALERT;

            if (!shouldReload) {
                return;
            }

            System.out.println("[AdminSystemPanel] realtime reload: " + event.getMessage());

            SwingUtilities.invokeLater(() -> {
                if (realtimeReloadTimer != null) {
                    realtimeReloadTimer.restart();
                }
            });
        });
    }

    private void reloadAllSafely() {
        if (reloading) {
            return;
        }

        reloading = true;

        SwingUtilities.invokeLater(() -> {
            try {
                reloadAll();
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                reloading = false;
            }
        });
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
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                Color paintColor = getBackground();

                if (getModel().isPressed()) {
                    paintColor = paintColor.darker();
                } else if (getModel().isRollover()) {
                    paintColor = brighten(paintColor, 1.08f);
                }

                g2.setColor(paintColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            protected void paintBorder(Graphics g) {
                // Không vẽ border vuông mặc định
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bgColor);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 20, 10, 20));
        btn.setPreferredSize(new Dimension(112, 42));

        return btn;
    }

    private Color brighten(Color c, float factor) {
        return new Color(
                Math.min(255, Math.round(c.getRed() * factor)),
                Math.min(255, Math.round(c.getGreen() * factor)),
                Math.min(255, Math.round(c.getBlue() * factor))
        );
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
        RoundedPanel card = new RoundedPanel(18, withAlpha(accent, 16));
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(15, 16, 15, 16));

        JPanel stripe = new JPanel();
        stripe.setPreferredSize(new Dimension(5, 0));
        stripe.setBackground(accent);

        RoundedPanel icon = new RoundedPanel(14, withAlpha(accent, 42));
        icon.setPreferredSize(new Dimension(46, 46));
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
        t.setForeground(new Color(71, 85, 105));

        value.setFont(new Font("Segoe UI", Font.BOLD, 24));
        value.setForeground(accent);

        textPanel.add(t);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(value);

        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setOpaque(false);
        center.add(icon, BorderLayout.WEST);
        center.add(textPanel, BorderLayout.CENTER);

        card.add(stripe, BorderLayout.WEST);
        card.add(center, BorderLayout.CENTER);

        return wrapWithBorder(card);
    }

    private JPanel cardWithTable(String title, String subtitle, JTable table) {
        RoundedPanel card = new RoundedPanel(18, white);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JPanel accentBar = new JPanel();
        accentBar.setPreferredSize(new Dimension(5, 34));
        accentBar.setBackground(primary);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(text);

        JLabel sub = new JLabel(subtitle);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(mutedLight);

        titleBox.add(lbl);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(sub);

        header.add(accentBar, BorderLayout.WEST);
        header.add(titleBox, BorderLayout.CENTER);

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

    private double niceTickUnit(double maxValue) {
        if (maxValue <= 0) {
            return 1;
        }

        double raw = maxValue / 5.0;
        double pow = Math.pow(10, Math.floor(Math.log10(raw)));
        double normalized = raw / pow;

        if (normalized <= 1) {
            return pow;
        } else if (normalized <= 2) {
            return 2 * pow;
        } else if (normalized <= 5) {
            return 5 * pow;
        } else {
            return 10 * pow;
        }
    }

    private void compactNumberAxis(NumberAxis axis, double maxValue) {
        double upper = maxValue <= 0 ? 10 : maxValue * 1.12;
        axis.setRange(0, upper);
        axis.setTickUnit(new NumberTickUnit(niceTickUnit(upper)));
        axis.setTickLabelFont(new Font("Segoe UI", Font.BOLD, 14));
        axis.setLabelFont(new Font("Segoe UI", Font.BOLD, 15));
        axis.setNumberFormatOverride(new DecimalFormat("#,##0.#"));
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

            // Visualization charts for AdminSystemReport.jrxml
            params.put("P_REVENUE_ORDER_CHART", createRevenueOrderLineChartImage());
            params.put("P_PRODUCT_BUBBLE_CHART", createProductBubbleChartImage());
            params.put("P_PRODUCT_PIE_3D_CHART", createProductRevenuePie3DChartImage());
            params.put("P_REVENUE_ORDER_DIFFERENCE_CHART", createRevenueOrderDifferenceChartImage());

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

    private void applyStrongCategoryGrid(CategoryPlot plot) {
        if (plot == null) {
            return;
        }

        Color gridColor = new Color(148, 163, 184);

        plot.setBackgroundPaint(new Color(248, 250, 252));
        plot.setOutlinePaint(new Color(71, 85, 105));
        plot.setOutlineStroke(new BasicStroke(1.4f));

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(gridColor);
        plot.setRangeGridlineStroke(new BasicStroke(1.15f));

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(new Color(203, 213, 225));
        plot.setDomainGridlineStroke(new BasicStroke(0.9f));
    }

    private void applyStrongXYGrid(XYPlot plot) {
        if (plot == null) {
            return;
        }

        Color gridColor = new Color(148, 163, 184);

        plot.setBackgroundPaint(new Color(248, 250, 252));
        plot.setOutlinePaint(new Color(71, 85, 105));
        plot.setOutlineStroke(new BasicStroke(1.4f));

        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(gridColor);
        plot.setDomainGridlineStroke(new BasicStroke(1.15f));

        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(gridColor);
        plot.setRangeGridlineStroke(new BasicStroke(1.15f));
    }

    @SuppressWarnings("deprecation")
    private BufferedImage createProductRevenuePie3DChartImage() {
        DefaultPieDataset dataset = new DefaultPieDataset();

        String sql = """
            SELECT product_name, revenue
            FROM (
                SELECT p.product_name AS product_name,
                       SUM(od.quantity * od.unit_price) AS revenue
                FROM stores s
                JOIN orders o
                    ON o.store_id = s.store_id
                   AND NVL(o.is_deleted, 0) = 0
                   AND o.store_id IS NOT NULL
                   AND TRUNC(o.order_date, 'MM') = TRUNC(SYSDATE, 'MM')
                   AND (
                        UPPER(NVL(o.status, '')) = 'COMPLETED'
                        OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                        OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
                   )
                JOIN order_details od
                    ON od.order_id = o.order_id
                   AND NVL(od.is_deleted, 0) = 0
                JOIN products p
                    ON p.product_id = od.product_id
                   AND NVL(p.is_deleted, 0) = 0
                WHERE NVL(s.is_deleted, 0) = 0
                GROUP BY p.product_name
                ORDER BY revenue DESC
            )
            WHERE ROWNUM <= 5
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String productName = rs.getString("product_name");
                double revenue = rs.getDouble("revenue");

                if (revenue > 0) {
                    String shortName = productName == null ? "Không rõ" : productName.trim();
                    if (shortName.length() > 24) {
                        shortName = shortName.substring(0, 24) + "...";
                    }
                    dataset.setValue(shortName, revenue);
                }
            }
        } catch (Exception e) {
            System.err.println("[AdminSystemPanel] createProductRevenuePie3DChartImage error: " + e.getMessage());
        }

        JFreeChart chart = ChartFactory.createPieChart3D(
                null,
                dataset,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("Segoe UI", Font.BOLD, 15));
            chart.getLegend().setBackgroundPaint(Color.WHITE);
        }

        PiePlot3D plot = (PiePlot3D) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(true);
        plot.setOutlinePaint(new Color(71, 85, 105));
        plot.setOutlineStroke(new BasicStroke(1.4f));

        // Style 3D ngang giống demo JFreeChart: dẹt, có mặt bên rõ, không bị dựng đứng.
        plot.setStartAngle(285);
        plot.setDirection(Rotation.CLOCKWISE);
        plot.setDepthFactor(0.24);
        plot.setForegroundAlpha(0.82f);
        plot.setInteriorGap(0.03);

        plot.setLabelFont(new Font("Segoe UI", Font.BOLD, 16));
        plot.setLabelGenerator(new StandardPieSectionLabelGenerator("{0}: {2}"));
        plot.setSimpleLabels(false);
        plot.setMaximumLabelWidth(0.28);
        plot.setLabelBackgroundPaint(new Color(255, 255, 210));
        plot.setLabelOutlinePaint(new Color(71, 85, 105));
        plot.setLabelShadowPaint(null);

        // Tỉ lệ ngang để khi đưa vào JRXML nhìn giống Pie Chart 3D Demo, không bị teo nhỏ.
        return chart.createBufferedImage(1200, 620);
    }

    private BufferedImage createRevenueOrderDifferenceChartImage() {
        XYSeries currentMonthSeries = new XYSeries("Tháng hiện tại");
        XYSeries previousMonthSeries = new XYSeries("Tháng trước");

        String sql = """
        SELECT period_type,
               day_no,
               NVL(SUM(revenue), 0) AS revenue
        FROM (
            SELECT 'CURRENT' AS period_type,
                   TO_NUMBER(TO_CHAR(o.order_date, 'DD')) AS day_no,
                   o.total_amount AS revenue
            FROM stores s
            JOIN orders o
                ON o.store_id = s.store_id
               AND NVL(o.is_deleted, 0) = 0
               AND o.store_id IS NOT NULL
               AND TRUNC(o.order_date, 'MM') = TRUNC(SYSDATE, 'MM')
               AND (
                    UPPER(NVL(o.status, '')) = 'COMPLETED'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
               )
            WHERE NVL(s.is_deleted, 0) = 0

            UNION ALL

            SELECT 'PREVIOUS' AS period_type,
                   TO_NUMBER(TO_CHAR(o.order_date, 'DD')) AS day_no,
                   o.total_amount AS revenue
            FROM stores s
            JOIN orders o
                ON o.store_id = s.store_id
               AND NVL(o.is_deleted, 0) = 0
               AND o.store_id IS NOT NULL
               AND TRUNC(o.order_date, 'MM') = ADD_MONTHS(TRUNC(SYSDATE, 'MM'), -1)
               AND (
                    UPPER(NVL(o.status, '')) = 'COMPLETED'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
               )
            WHERE NVL(s.is_deleted, 0) = 0
        )
        GROUP BY period_type, day_no
        ORDER BY day_no, period_type
    """;

        double maxRevenueMillion = 0;
        int minDay = 32;
        int maxDay = 0;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String periodType = rs.getString("period_type");
                int dayNo = rs.getInt("day_no");
                double revenueMillion = rs.getDouble("revenue") / 1_000_000.0;

                if ("CURRENT".equalsIgnoreCase(periodType)) {
                    currentMonthSeries.add(dayNo, revenueMillion);
                } else {
                    previousMonthSeries.add(dayNo, revenueMillion);
                }

                maxRevenueMillion = Math.max(maxRevenueMillion, revenueMillion);
                minDay = Math.min(minDay, dayNo);
                maxDay = Math.max(maxDay, dayNo);
            }
        } catch (Exception e) {
            System.err.println("[AdminSystemPanel] createRevenueOrderDifferenceChartImage error: " + e.getMessage());
        }

        if (maxDay == 0) {
            minDay = 1;
            maxDay = 2;
            currentMonthSeries.add(1, 0);
            previousMonthSeries.add(1, 0);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(previousMonthSeries);
        dataset.addSeries(currentMonthSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                null,
                "Ngày trong tháng",
                "Doanh thu (triệu VND)",
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

        XYPlot plot = chart.getXYPlot();
        applyStrongXYGrid(plot);

        XYDifferenceRenderer renderer = new XYDifferenceRenderer(
                new Color(245, 158, 11, 85),
                new Color(37, 99, 235, 75),
                true
        );

        renderer.setSeriesPaint(0, blue);
        renderer.setSeriesPaint(1, orange);
        renderer.setSeriesStroke(0, new BasicStroke(3.2f));
        renderer.setSeriesStroke(1, new BasicStroke(3.2f));

        plot.setRenderer(renderer);

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        xAxis.setRange(Math.max(1, minDay - 1), Math.min(31, maxDay + 1));

        int daySpan = Math.max(1, maxDay - minDay);
        if (daySpan <= 8) {
            xAxis.setTickUnit(new NumberTickUnit(1));
        } else if (daySpan <= 16) {
            xAxis.setTickUnit(new NumberTickUnit(2));
        } else {
            xAxis.setTickUnit(new NumberTickUnit(5));
        }

        xAxis.setTickLabelFont(new Font("Segoe UI", Font.BOLD, 13));
        xAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 14));
        xAxis.setNumberFormatOverride(new DecimalFormat("00"));

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        compactNumberAxis(yAxis, maxRevenueMillion);
        yAxis.setTickLabelFont(new Font("Segoe UI", Font.BOLD, 13));
        yAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 14));

        return chart.createBufferedImage(1200, 650);
    }

    private BufferedImage createRevenueOrderLineChartImage() {
        DefaultCategoryDataset revenueDataset = new DefaultCategoryDataset();
        DefaultCategoryDataset orderDataset = new DefaultCategoryDataset();

        double maxRevenueMillion = 0;
        int maxOrderCount = 0;

        String sql = """
        SELECT s.store_id,
               NVL(s.store_name, s.address) AS store_name,
               NVL(SUM(o.total_amount), 0) AS revenue,
               COUNT(o.order_id) AS order_count
        FROM stores s
        LEFT JOIN orders o
            ON o.store_id = s.store_id
           AND NVL(o.is_deleted, 0) = 0
           AND o.store_id IS NOT NULL
           AND TRUNC(o.order_date, 'MM') = TRUNC(SYSDATE, 'MM')
           AND (
                UPPER(NVL(o.status, '')) = 'COMPLETED'
                OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
           )
        WHERE NVL(s.is_deleted, 0) = 0
        GROUP BY s.store_id, NVL(s.store_name, s.address)
        ORDER BY revenue DESC, s.store_id
    """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String storeName = rs.getString("store_name");
                if (storeName == null || storeName.trim().isEmpty()) {
                    storeName = rs.getString("store_id");
                }

                if (storeName.length() > 18) {
                    storeName = storeName.substring(0, 18) + "...";
                }

                double revenueMillion = rs.getDouble("revenue") / 1_000_000.0;
                int orderCount = rs.getInt("order_count");

                revenueDataset.addValue(revenueMillion, "Doanh thu", storeName);
                orderDataset.addValue(orderCount, "Số đơn", storeName);

                maxRevenueMillion = Math.max(maxRevenueMillion, revenueMillion);
                maxOrderCount = Math.max(maxOrderCount, orderCount);
            }
        } catch (Exception e) {
            System.err.println("[AdminSystemPanel] createRevenueOrderLineChartImage error: " + e.getMessage());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                null,
                "Chi nhánh",
                "Doanh thu (triệu VND)",
                revenueDataset,
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
        applyStrongCategoryGrid(plot);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        domainAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 14));
        domainAxis.setLowerMargin(0.12);
        domainAxis.setUpperMargin(0.12);
        domainAxis.setCategoryMargin(0.45);

        NumberAxis revenueAxis = (NumberAxis) plot.getRangeAxis();
        compactNumberAxis(revenueAxis, maxRevenueMillion);
        revenueAxis.setTickLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        revenueAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 14));

        BarRenderer revenueRenderer = new BarRenderer();
        revenueRenderer.setSeriesPaint(0, new Color(245, 158, 11, 190));
        revenueRenderer.setBarPainter(new StandardBarPainter());
        revenueRenderer.setShadowVisible(false);

        // Cái này là phần quan trọng: giảm độ rộng cột để không bị đè hình.
        revenueRenderer.setMaximumBarWidth(0.045);
        revenueRenderer.setItemMargin(0.35);

        plot.setRenderer(0, revenueRenderer);

        NumberAxis orderAxis = new NumberAxis("Số đơn");
        compactNumberAxis(orderAxis, maxOrderCount);
        orderAxis.setTickLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        orderAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 14));

        plot.setRangeAxis(1, orderAxis);
        plot.setDataset(1, orderDataset);
        plot.mapDatasetToRangeAxis(1, 1);

        LineAndShapeRenderer orderRenderer = new LineAndShapeRenderer(true, true);
        orderRenderer.setSeriesPaint(0, blue);
        orderRenderer.setSeriesStroke(0, new BasicStroke(3.4f));
        orderRenderer.setSeriesShapesVisible(0, true);
        orderRenderer.setSeriesShape(
                0,
                new java.awt.geom.Ellipse2D.Double(-5, -5, 10, 10)
        );

        // Renderer số đơn đặt ở dataset 1 nên sẽ vẽ nổi trên cột doanh thu.
        plot.setRenderer(1, orderRenderer);

        return chart.createBufferedImage(1200, 650);
    }

    private BufferedImage createProductBubbleChartImage() {
        XYSeriesCollection dataset = new XYSeriesCollection();
        List<Double> bubbleSizes = new ArrayList<>();

        double maxQuantity = 0;
        double maxRevenueMillion = 0;

        String sql = """
            SELECT *
            FROM (
                SELECT p.product_name AS product_name,
                       SUM(od.quantity) AS quantity_sold,
                       SUM(od.quantity * od.unit_price) AS revenue,
                       COUNT(DISTINCT o.order_id) AS bubble_size
                FROM stores s
                JOIN orders o
                    ON o.store_id = s.store_id
                   AND NVL(o.is_deleted, 0) = 0
                   AND o.store_id IS NOT NULL
                   AND TRUNC(o.order_date, 'MM') = TRUNC(SYSDATE, 'MM')
                   AND (
                        UPPER(NVL(o.status, '')) = 'COMPLETED'
                        OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                        OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
                   )
                JOIN order_details od
                    ON od.order_id = o.order_id
                   AND NVL(od.is_deleted, 0) = 0
                JOIN products p
                    ON p.product_id = od.product_id
                   AND NVL(p.is_deleted, 0) = 0
                WHERE NVL(s.is_deleted, 0) = 0
                GROUP BY p.product_name
                ORDER BY revenue DESC
            )
            WHERE ROWNUM <= 10
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String productName = rs.getString("product_name");
                double quantitySold = rs.getDouble("quantity_sold");
                double revenueMillion = rs.getDouble("revenue") / 1_000_000.0;
                double orderCount = Math.max(1.0, rs.getDouble("bubble_size"));

                String seriesName = productName == null ? "Không rõ" : productName.trim();
                XYSeries series = new XYSeries(seriesName);
                series.add(quantitySold, revenueMillion);
                dataset.addSeries(series);

                bubbleSizes.add(orderCount);

                maxQuantity = Math.max(maxQuantity, quantitySold);
                maxRevenueMillion = Math.max(maxRevenueMillion, revenueMillion);
            }
        } catch (Exception e) {
            System.err.println("[AdminSystemPanel] createProductBubbleChartImage error: " + e.getMessage());
        }

        JFreeChart chart = ChartFactory.createScatterPlot(
                null,
                "Số lượng bán",
                "Doanh thu (triệu VND)",
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

        XYPlot plot = chart.getXYPlot();

        plot.setBackgroundPaint(new Color(248, 250, 252));

// Viền ngoài chart đậm hơn
        plot.setOutlineVisible(true);
        plot.setOutlinePaint(new Color(51, 65, 85));
        plot.setOutlineStroke(new BasicStroke(1.4f));

// Lưới dọc
        plot.setDomainGridlinesVisible(true);
        plot.setDomainGridlinePaint(new Color(148, 163, 184));
        plot.setDomainGridlineStroke(new BasicStroke(1.25f));

// Lưới ngang
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(new Color(148, 163, 184));
        plot.setRangeGridlineStroke(new BasicStroke(1.25f));

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);

        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            double orderCount = bubbleSizes.get(i);
            double size = 20 + Math.min(42, orderCount * 5.0);
            renderer.setSeriesShape(
                    i,
                    new Ellipse2D.Double(-size / 2, -size / 2, size, size)
            );
        }

        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelGenerator((xyDataset, series, item) -> {
            String name = xyDataset.getSeriesKey(series).toString();
            return name.length() > 16 ? name.substring(0, 16) + "..." : name;
        });
        renderer.setDefaultItemLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        plot.setRenderer(renderer);

        NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
        compactNumberAxis(xAxis, maxQuantity);

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        compactNumberAxis(yAxis, maxRevenueMillion);

        return chart.createBufferedImage(1200, 650);
    }

    private String safeText(JLabel label) {
        return label == null || label.getText() == null ? "0" : label.getText();
    }

    private long scalarLong(String sql) {
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            return rs.next() ? rs.getLong(1) : 0L;

        } catch (Exception e) {
            System.err.println("[AdminSystemPanel] scalarLong error: " + e.getMessage());
            return 0L;
        }
    }

    private double scalarDouble(String sql) {
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

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

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

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
