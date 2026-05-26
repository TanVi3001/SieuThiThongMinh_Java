package view;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import business.sql.report.ImportSalesEfficiencySql;
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
    private final int initialTabIndex;

    private final DecimalFormat moneyFmt = new DecimalFormat("#,###");

    private JLabel lblStoreTotal;
    private JLabel lblStoreActive;
    private JLabel lblTodayRevenue;
    private JLabel lblMonthRevenue;
    private JLabel lblOrderTotal;
    private JLabel lblLowStock;
    private JLabel lblOnlineSessions;
    private JLabel lblMonthImportCost;
    private JLabel lblGrossProfit;
    private JLabel lblGrossProfitMargin;

    private JTable tblOverviewRevenueByStore;
    private JTable tblOverviewInventoryByStore;
    private JTable tblReportRevenueByStore;
    private JTable tblReportInventoryByStore;
    private JTable tblTopEmployee;
    private JTable tblLowStock;

    private JSpinner spnFilterFromDate;
    private JSpinner spnFilterToDate;
    private JLabel lblFilterInfo;

    private LocalDateTime filterFrom;
    private LocalDateTime filterTo;
    private String filterLabel = "Tháng hiện tại";

    public AdminSystemPanel() {
        this(0);
    }

    public AdminSystemPanel(int initialTabIndex) {
        this.initialTabIndex = initialTabIndex;

        setLayout(new BorderLayout());
        setBackground(bg);
        setBorder(new EmptyBorder(22, 30, 22, 30));

        resetFilterToCurrentMonth();

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

        if (initialTabIndex >= 0 && initialTabIndex < tabs.getTabCount()) {
            tabs.setSelectedIndex(initialTabIndex);
        }

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

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);

        top.add(createPageIntro(
                "Tổng quan hệ thống",
                "Theo dõi nhanh tình hình chi nhánh, doanh thu, tồn kho và phiên hoạt động"
        ), BorderLayout.NORTH);

        top.add(createTimeFilterPanel(), BorderLayout.CENTER);

        page.add(top, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);

        JPanel cards = new JPanel(new GridLayout(3, 4, 14, 14));
        cards.setOpaque(false);

        lblStoreTotal = new JLabel("0");
        lblStoreActive = new JLabel("0");
        lblTodayRevenue = new JLabel("0 đ");
        lblMonthRevenue = new JLabel("0 đ");
        lblOrderTotal = new JLabel("0");
        lblLowStock = new JLabel("0");
        lblOnlineSessions = new JLabel("0");
        lblMonthImportCost = new JLabel("0 đ");
        lblGrossProfit = new JLabel("0 đ");
        lblGrossProfitMargin = new JLabel("0%");

        cards.add(statCard("Tổng chi nhánh", lblStoreTotal, blue));
        cards.add(statCard("Đang hoạt động", lblStoreActive, green));
        cards.add(statCard("Doanh thu hôm nay", lblTodayRevenue, orange));
        cards.add(statCard("Tổng doanh thu kỳ lọc", lblMonthRevenue, orange));
        cards.add(statCard("Tiền nhập kho kỳ lọc", lblMonthImportCost, purple));
        cards.add(statCard("Lãi gộp tạm tính", lblGrossProfit, green));
        cards.add(statCard("Biên lãi gộp", lblGrossProfitMargin, blue));
        cards.add(statCard("Tổng đơn hàng", lblOrderTotal, blue));

        cards.add(statCard("Tồn kho thấp", lblLowStock, red));
        cards.add(statCard("Phiên online", lblOnlineSessions, green));
        cards.add(statCard("Trạng thái hệ thống", new JLabel("OK"), purple));
        cards.add(statCard("Hiệu quả nhập - bán", new JLabel("OK"), green));

        content.add(cards, BorderLayout.NORTH);

        JPanel tables = new JPanel(new GridLayout(1, 2, 18, 0));
        tables.setOpaque(false);

        tblOverviewRevenueByStore = table(new String[]{
            "Mã CN",
            "Chi nhánh",
            "Số đơn",
            "Doanh thu",
            "Tiền nhập",
            "Lãi gộp",
            "Biên lãi %"
        });
        tblOverviewInventoryByStore = table(new String[]{"Mã CN", "Chi nhánh", "Mặt hàng", "Tổng tồn", "Tồn thấp"});

        tables.add(cardWithTable(
                "Doanh thu theo chi nhánh",
                "Chỉ tính hóa đơn hoàn thành trong khoảng thời gian đang lọc",
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

    private JPanel createTimeFilterPanel() {
        RoundedPanel panel = new RoundedPanel(18, white);
        panel.setLayout(new BorderLayout(12, 0));
        panel.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);

        JLabel lblFrom = new JLabel("Từ:");
        lblFrom.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblFrom.setForeground(text);

        JLabel lblTo = new JLabel("Đến:");
        lblTo.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTo.setForeground(text);

        SpinnerDateModel fromModel = new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE);
        SpinnerDateModel toModel = new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE);

        spnFilterFromDate = new JSpinner(fromModel);
        spnFilterToDate = new JSpinner(toModel);

        spnFilterFromDate.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        spnFilterToDate.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        spnFilterFromDate.setPreferredSize(new Dimension(190, 36));
        spnFilterToDate.setPreferredSize(new Dimension(190, 36));

        spnFilterFromDate.setEditor(new JSpinner.DateEditor(spnFilterFromDate, "dd/MM/yyyy HH:mm"));
        spnFilterToDate.setEditor(new JSpinner.DateEditor(spnFilterToDate, "dd/MM/yyyy HH:mm"));

        syncSpinnerWithCurrentFilter();

        JButton btnApplyFilter = createPrimaryButton("Lọc", blue);
        btnApplyFilter.setPreferredSize(new Dimension(86, 38));
        btnApplyFilter.addActionListener(e -> applyDateRangeFilter());

        JButton btnResetFilter = createPrimaryButton("Tháng hiện tại", primary);
        btnResetFilter.setPreferredSize(new Dimension(135, 38));
        btnResetFilter.addActionListener(e -> {
            resetFilterToCurrentMonth();
            syncSpinnerWithCurrentFilter();
            updateFilterInfoLabel();
            reloadAll();
        });

        left.add(lblFrom);
        left.add(spnFilterFromDate);
        left.add(lblTo);
        left.add(spnFilterToDate);
        left.add(btnApplyFilter);
        left.add(btnResetFilter);

        lblFilterInfo = new JLabel();
        lblFilterInfo.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblFilterInfo.setForeground(muted);
        updateFilterInfoLabel();

        panel.add(left, BorderLayout.WEST);
        panel.add(lblFilterInfo, BorderLayout.EAST);

        return wrapWithBorder(panel);
    }

    private void syncSpinnerWithCurrentFilter() {
        if (spnFilterFromDate != null && filterFrom != null) {
            spnFilterFromDate.setValue(Date.from(filterFrom.atZone(ZoneId.systemDefault()).toInstant()));
        }

        if (spnFilterToDate != null && filterTo != null) {
            spnFilterToDate.setValue(Date.from(filterTo.atZone(ZoneId.systemDefault()).toInstant()));
        }
    }

    private void resetFilterToCurrentMonth() {
        LocalDateTime now = LocalDateTime.now();

        filterFrom = now
                .withDayOfMonth(1)
                .truncatedTo(ChronoUnit.DAYS);

        filterTo = filterFrom.plusMonths(1);
        filterLabel = "Tháng hiện tại";
    }

    private void applyDateRangeFilter() {
        Date fromDate = (Date) spnFilterFromDate.getValue();
        Date toDate = (Date) spnFilterToDate.getValue();

        LocalDateTime from = fromDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MINUTES);

        LocalDateTime to = toDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .truncatedTo(ChronoUnit.MINUTES);

        if (!to.isAfter(from)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Thời gian kết thúc phải lớn hơn thời gian bắt đầu.",
                    "Bộ lọc không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        filterFrom = from;
        filterTo = to;

        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        filterLabel = "Tùy chọn: "
                + fmt.format(Date.from(filterFrom.atZone(ZoneId.systemDefault()).toInstant()))
                + " - "
                + fmt.format(Date.from(filterTo.atZone(ZoneId.systemDefault()).toInstant()));

        updateFilterInfoLabel();
        reloadAll();
    }

    private void updateFilterInfoLabel() {
        if (lblFilterInfo == null || filterFrom == null || filterTo == null) {
            return;
        }

        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        Date fromDate = Date.from(filterFrom.atZone(ZoneId.systemDefault()).toInstant());
        Date toDate = Date.from(filterTo.atZone(ZoneId.systemDefault()).toInstant());

        lblFilterInfo.setText(filterLabel + " | " + fmt.format(fromDate) + " → " + fmt.format(toDate));
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

        tblReportRevenueByStore = table(new String[]{
            "Mã CN",
            "Chi nhánh",
            "Số đơn",
            "Doanh thu",
            "Tiền nhập",
            "Lãi gộp",
            "Biên lãi %"
        });
        tblReportInventoryByStore = table(new String[]{"Mã CN", "Chi nhánh", "Mặt hàng", "Tổng tồn", "Tồn thấp"});
        tblTopEmployee = table(new String[]{"Chi nhánh", "Mã NV", "Nhân viên", "Số đơn", "Doanh thu"});
        tblLowStock = table(new String[]{"Chi nhánh", "Mã SP", "Sản phẩm", "Tồn", "Mức cảnh báo"});

        grid.add(cardWithTable(
                "Doanh thu theo kỳ lọc toàn hệ thống",
                "Tổng hợp doanh thu theo khoảng thời gian đang lọc",
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

        reloadImportSalesEfficiencyCards();
        reloadImportSalesEfficiencyByStore(tblOverviewRevenueByStore);
        reloadImportSalesEfficiencyByStore(tblReportRevenueByStore);

        reloadInventoryByStore(tblOverviewInventoryByStore);
        reloadInventoryByStore(tblReportInventoryByStore);
        reloadTopEmployee();
        reloadLowStock();
    }

    private void reloadImportSalesEfficiencyCards() {
        ImportSalesEfficiencySql.EfficiencySummary summary
                = ImportSalesEfficiencySql.getInstance()
                        .selectSummaryForAdmin(filterFrom, filterTo);

        lblMonthRevenue.setText(money(summary.totalRevenue));
        lblMonthImportCost.setText(money(summary.totalImportCost));
        lblGrossProfit.setText(money(summary.grossProfit));
        lblGrossProfitMargin.setText(percent(summary.grossProfitMargin));

        lblGrossProfit.setForeground(summary.grossProfit < 0 ? red : green);
    }

    private void reloadImportSalesEfficiencyByStore(JTable targetTable) {
        if (targetTable == null) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) targetTable.getModel();
        model.setRowCount(0);

        List<ImportSalesEfficiencySql.EfficiencyRow> rows
                = ImportSalesEfficiencySql.getInstance()
                        .selectByStoreForAdmin(filterFrom, filterTo);

        for (ImportSalesEfficiencySql.EfficiencyRow row : rows) {
            model.addRow(new Object[]{
                row.storeId,
                row.storeName,
                row.totalOrders,
                money(row.totalRevenue),
                money(row.totalImportCost),
                money(row.grossProfit),
                percent(row.grossProfitMargin)
            });
        }
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

        // lblMonthRevenue được cập nhật theo filter trong reloadImportSalesEfficiencyCards().
        lblOrderTotal.setText(String.valueOf(countOrdersByFilter()));

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
        if (tblTopEmployee == null) {
            return;
        }

        DefaultTableModel model = (DefaultTableModel) tblTopEmployee.getModel();
        model.setRowCount(0);

        String sql = """
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
                   AND o.order_date >= ?
                   AND o.order_date < ?
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
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(filterFrom));
            ps.setTimestamp(2, Timestamp.valueOf(filterTo));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("store_name"),
                        rs.getString("employee_id"),
                        rs.getString("employee_name"),
                        rs.getLong("total_orders"),
                        money(rs.getDouble("revenue"))
                    });
                }
            }
        } catch (Exception ex) {
            System.err.println("[AdminSystemPanel] reloadTopEmployee error: " + ex.getMessage());
        }
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
            params.put("P_FINAL_REVENUE_CHART", createFinalRevenueChartImage());
            params.put("P_REVENUE_ORDER_CHART", createRevenueOrderLineChartImage());
            params.put("P_PRODUCT_BUBBLE_CHART", createProductBubbleChartImage());
            params.put("P_PRODUCT_PIE_3D_CHART", createProductRevenuePie3DChartImage());
            params.put("P_REVENUE_ORDER_DIFFERENCE_CHART", createRevenueOrderDifferenceChartImage());
            params.put("P_FILTER_LABEL", filterLabel);
            params.put("P_FILTER_FROM", Timestamp.valueOf(filterFrom));
            params.put("P_FILTER_TO", Timestamp.valueOf(filterTo));

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

    private long countOrdersByFilter() {
        String sql = """
        SELECT COUNT(*)
        FROM STORES s
        JOIN ORDERS o
            ON o.store_id = s.store_id
           AND NVL(o.is_deleted, 0) = 0
           AND o.order_date >= ?
           AND o.order_date < ?
           AND (
                UPPER(NVL(o.status, '')) = 'COMPLETED'
                OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
           )
        WHERE NVL(s.is_deleted, 0) = 0
    """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(filterFrom));
            ps.setTimestamp(2, Timestamp.valueOf(filterTo));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }

        } catch (Exception ex) {
            System.err.println("[AdminSystemPanel] countOrdersByFilter error: " + ex.getMessage());
        }

        return 0;
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

    private BufferedImage createFinalRevenueChartImage() {
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
                FROM purchase_receipts
                WHERE NVL(is_deleted, 0) = 0
                  AND created_at >= ?
                  AND created_at < ?
                GROUP BY store_id
            ) imports
                ON imports.store_id = s.store_id
            WHERE NVL(s.is_deleted, 0) = 0
            ORDER BY total_revenue DESC, gross_profit DESC, s.store_id
        """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(filterFrom));
            ps.setTimestamp(2, Timestamp.valueOf(filterTo));
            ps.setTimestamp(3, Timestamp.valueOf(filterFrom));
            ps.setTimestamp(4, Timestamp.valueOf(filterTo));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String storeName = rs.getString("store_name");
                    if (storeName == null || storeName.trim().isEmpty()) {
                        storeName = rs.getString("store_id");
                    }
                    if (storeName.length() > 18) {
                        storeName = storeName.substring(0, 18) + "...";
                    }

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
            }
        } catch (Exception ex) {
            System.err.println("[AdminSystemPanel] createFinalRevenueChartImage error: " + ex.getMessage());
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

    @SuppressWarnings("deprecation")
    private BufferedImage createProductRevenuePie3DChartImage() {
        DefaultPieDataset dataset = new DefaultPieDataset();

        /*
     * FIX REPORT CHART TRẮNG:
     * Không lọc p.is_deleted = 0 ở report lịch sử.
     * Vì ORDER_DETAILS có thể đang tham chiếu sản phẩm đã bị soft-delete do dọn trùng.
     * Report doanh thu phải đọc lịch sử bán hàng, không phụ thuộc sản phẩm còn active hay không.
         */
        String sql = """
        SELECT product_name, revenue
        FROM (
            SELECT NVL(MAX(p.product_name), od.product_id) AS product_name,
                   SUM(NVL(od.quantity, 0) * NVL(od.unit_price, 0)) AS revenue
            FROM orders o
            JOIN order_details od
                ON od.order_id = o.order_id
               AND NVL(od.is_deleted, 0) = 0
            LEFT JOIN products p
                ON p.product_id = od.product_id
            WHERE NVL(o.is_deleted, 0) = 0
              AND o.store_id IS NOT NULL
              AND o.order_date >= ?
              AND o.order_date < ?
              AND (
                    UPPER(NVL(TO_CHAR(o.status), '')) IN ('COMPLETED', 'PAID', 'DONE')
                    OR UPPER(NVL(TO_CHAR(o.status), '')) LIKE '%HOÀN THÀNH%'
                    OR UPPER(NVL(TO_CHAR(o.status), '')) LIKE '%HOAN THANH%'
                    OR UPPER(NVL(TO_CHAR(o.status), '')) LIKE '%ĐÃ THANH TOÁN%'
                    OR UPPER(NVL(TO_CHAR(o.status), '')) LIKE '%DA THANH TOAN%'
                    OR UPPER(NVL(TO_CHAR(o.status), '')) LIKE '%THANH TOÁN%'
                    OR UPPER(NVL(TO_CHAR(o.status), '')) LIKE '%THANH TOAN%'
              )
            GROUP BY od.product_id
            HAVING SUM(NVL(od.quantity, 0) * NVL(od.unit_price, 0)) > 0
            ORDER BY revenue DESC
        )
        WHERE ROWNUM <= 5
    """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(filterFrom));
            ps.setTimestamp(2, Timestamp.valueOf(filterTo));

            try (ResultSet rs = ps.executeQuery()) {
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
            }

        } catch (Exception e) {
            System.err.println("[AdminSystemPanel] createProductRevenuePie3DChartImage error: " + e.getMessage());
        }

        /*
     * Không để pie chart trắng nếu thật sự không có dữ liệu.
         */
        if (dataset.getItemCount() == 0) {
            dataset.setValue("Không có dữ liệu", 1);
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

        return chart.createBufferedImage(1200, 620);
    }

    private BufferedImage createRevenueOrderDifferenceChartImage() {
        XYSeries currentPeriodSeries = new XYSeries("Kỳ đang lọc");
        XYSeries previousPeriodSeries = new XYSeries("Kỳ liền trước");

        long durationMinutes = Math.max(1, java.time.Duration.between(filterFrom, filterTo).toMinutes());
        LocalDateTime previousFrom = filterFrom.minusMinutes(durationMinutes);
        LocalDateTime previousTo = filterFrom;

        String sql = """
        SELECT period_type,
               day_no,
               NVL(SUM(revenue), 0) AS revenue
        FROM (
            SELECT 'CURRENT' AS period_type,
                   TRUNC(o.order_date) - TRUNC(?) + 1 AS day_no,
                   o.total_amount AS revenue
            FROM stores s
            JOIN orders o
                ON o.store_id = s.store_id
               AND NVL(o.is_deleted, 0) = 0
               AND o.store_id IS NOT NULL
               AND o.order_date >= ?
               AND o.order_date < ?
               AND (
                    UPPER(NVL(o.status, '')) = 'COMPLETED'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                    OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
               )
            WHERE NVL(s.is_deleted, 0) = 0

            UNION ALL

            SELECT 'PREVIOUS' AS period_type,
                   TRUNC(o.order_date) - TRUNC(?) + 1 AS day_no,
                   o.total_amount AS revenue
            FROM stores s
            JOIN orders o
                ON o.store_id = s.store_id
               AND NVL(o.is_deleted, 0) = 0
               AND o.store_id IS NOT NULL
               AND o.order_date >= ?
               AND o.order_date < ?
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

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(filterFrom));
            ps.setTimestamp(2, Timestamp.valueOf(filterFrom));
            ps.setTimestamp(3, Timestamp.valueOf(filterTo));
            ps.setTimestamp(4, Timestamp.valueOf(previousFrom));
            ps.setTimestamp(5, Timestamp.valueOf(previousFrom));
            ps.setTimestamp(6, Timestamp.valueOf(previousTo));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String periodType = rs.getString("period_type");
                    int dayNo = rs.getInt("day_no");
                    double revenueMillion = rs.getDouble("revenue") / 1_000_000.0;

                    if ("CURRENT".equalsIgnoreCase(periodType)) {
                        currentPeriodSeries.add(dayNo, revenueMillion);
                    } else {
                        previousPeriodSeries.add(dayNo, revenueMillion);
                    }

                    maxRevenueMillion = Math.max(maxRevenueMillion, revenueMillion);
                    minDay = Math.min(minDay, dayNo);
                    maxDay = Math.max(maxDay, dayNo);
                }
            }
        } catch (Exception e) {
            System.err.println("[AdminSystemPanel] createRevenueOrderDifferenceChartImage error: " + e.getMessage());
        }

        if (maxDay == 0) {
            minDay = 1;
            maxDay = 2;
            currentPeriodSeries.add(1, 0);
            previousPeriodSeries.add(1, 0);
        }

        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(previousPeriodSeries);
        dataset.addSeries(currentPeriodSeries);

        JFreeChart chart = ChartFactory.createXYLineChart(
                null,
                "Ngày thứ trong kỳ lọc",
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
        xAxis.setRange(Math.max(1, minDay - 1), Math.max(2, maxDay + 1));

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
           AND o.order_date >= ?
           AND o.order_date < ?
           AND (
                UPPER(NVL(o.status, '')) = 'COMPLETED'
                OR UPPER(NVL(o.status, '')) LIKE '%HOÀN THÀNH%'
                OR UPPER(NVL(o.status, '')) LIKE '%HOAN THANH%'
           )
        WHERE NVL(s.is_deleted, 0) = 0
        GROUP BY s.store_id, NVL(s.store_name, s.address)
        ORDER BY revenue DESC, s.store_id
    """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(filterFrom));
            ps.setTimestamp(2, Timestamp.valueOf(filterTo));

            try (ResultSet rs = ps.executeQuery()) {
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
        revenueRenderer.setMaximumBarWidth(0.045);
        revenueRenderer.setItemMargin(0.35);

        plot.setRenderer(0, revenueRenderer);

        NumberAxis orderAxis = new NumberAxis("Số đơn");
        orderAxis.setRange(0, Math.max(1, maxOrderCount) * 1.15);
        orderAxis.setTickUnit(new NumberTickUnit(niceTickUnit(Math.max(1, maxOrderCount))));
        orderAxis.setTickLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        orderAxis.setLabelFont(new Font("Segoe UI", Font.BOLD, 14));
        plot.setRangeAxis(1, orderAxis);
        plot.setDataset(1, orderDataset);
        plot.mapDatasetToRangeAxis(1, 1);

        LineAndShapeRenderer orderRenderer = new LineAndShapeRenderer();
        orderRenderer.setSeriesPaint(0, blue);
        orderRenderer.setSeriesStroke(0, new BasicStroke(3.2f));
        orderRenderer.setSeriesShapesVisible(0, true);
        orderRenderer.setSeriesShapesFilled(0, true);
        plot.setRenderer(1, orderRenderer);

        return chart.createBufferedImage(1200, 650);
    }

    private BufferedImage createProductBubbleChartImage() {
        XYSeriesCollection dataset = new XYSeriesCollection();
        List<Double> bubbleSizes = new ArrayList<>();

        double maxQuantity = 0;
        double maxRevenueMillion = 0;

        /*
     * FIX REPORT CHART TRẮNG:
     * LEFT JOIN products và bỏ điều kiện p.is_deleted = 0.
     * Báo cáo lịch sử bán hàng phải tính theo ORDER_DETAILS, kể cả sản phẩm đã soft-delete.
         */
        String sql = """
        SELECT *
        FROM (
            SELECT NVL(MAX(p.product_name), od.product_id) AS product_name,
                   SUM(NVL(od.quantity, 0)) AS quantity_sold,
                   SUM(NVL(od.quantity, 0) * NVL(od.unit_price, 0)) AS revenue,
                   COUNT(DISTINCT o.order_id) AS bubble_size
            FROM orders o
            JOIN order_details od
                ON od.order_id = o.order_id
               AND NVL(od.is_deleted, 0) = 0
            LEFT JOIN products p
                ON p.product_id = od.product_id
            WHERE NVL(o.is_deleted, 0) = 0
              AND o.store_id IS NOT NULL
              AND o.order_date >= ?
              AND o.order_date < ?
              AND (
                    UPPER(NVL(TO_CHAR(o.status), '')) IN ('COMPLETED', 'PAID', 'DONE')
                    OR UPPER(NVL(TO_CHAR(o.status), '')) LIKE '%HOÀN THÀNH%'
                    OR UPPER(NVL(TO_CHAR(o.status), '')) LIKE '%HOAN THANH%'
                    OR UPPER(NVL(TO_CHAR(o.status), '')) LIKE '%ĐÃ THANH TOÁN%'
                    OR UPPER(NVL(TO_CHAR(o.status), '')) LIKE '%DA THANH TOAN%'
                    OR UPPER(NVL(TO_CHAR(o.status), '')) LIKE '%THANH TOÁN%'
                    OR UPPER(NVL(TO_CHAR(o.status), '')) LIKE '%THANH TOAN%'
              )
            GROUP BY od.product_id
            HAVING SUM(NVL(od.quantity, 0)) > 0
               AND SUM(NVL(od.quantity, 0) * NVL(od.unit_price, 0)) > 0
            ORDER BY revenue DESC
        )
        WHERE ROWNUM <= 10
    """;

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(filterFrom));
            ps.setTimestamp(2, Timestamp.valueOf(filterTo));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String productName = rs.getString("product_name");
                    double quantitySold = rs.getDouble("quantity_sold");
                    double revenueMillion = rs.getDouble("revenue") / 1_000_000.0;
                    double orderCount = Math.max(1.0, rs.getDouble("bubble_size"));

                    String seriesName = productName == null ? "Không rõ" : productName.trim();

                    if (seriesName.length() > 28) {
                        seriesName = seriesName.substring(0, 28) + "...";
                    }

                    XYSeries series = new XYSeries(seriesName);
                    series.add(quantitySold, revenueMillion);
                    dataset.addSeries(series);

                    bubbleSizes.add(orderCount);

                    maxQuantity = Math.max(maxQuantity, quantitySold);
                    maxRevenueMillion = Math.max(maxRevenueMillion, revenueMillion);
                }
            }

        } catch (Exception e) {
            System.err.println("[AdminSystemPanel] createProductBubbleChartImage error: " + e.getMessage());
        }

        /*
     * Không để bubble chart trắng nếu thật sự không có data.
         */
        if (dataset.getSeriesCount() == 0) {
            XYSeries empty = new XYSeries("Không có dữ liệu");
            empty.add(0, 0);
            dataset.addSeries(empty);
            bubbleSizes.add(1.0);
            maxQuantity = 10;
            maxRevenueMillion = 10;
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
        applyStrongXYGrid(plot);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true) {
            @Override
            public java.awt.Shape getItemShape(int row, int column) {
                double orderCount = row < bubbleSizes.size() ? bubbleSizes.get(row) : 1.0;
                double size = Math.max(18, Math.min(54, 18 + orderCount * 7));
                return new Ellipse2D.Double(-size / 2, -size / 2, size, size);
            }
        };

        Color[] colors = new Color[]{
            new Color(239, 68, 68, 230),
            new Color(37, 99, 235, 220),
            new Color(34, 197, 94, 220),
            new Color(245, 158, 11, 220),
            new Color(217, 70, 239, 220),
            new Color(20, 184, 166, 220),
            new Color(148, 163, 184, 220),
            new Color(234, 88, 12, 220),
            new Color(99, 102, 241, 220),
            new Color(16, 185, 129, 220)
        };

        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, colors[i % colors.length]);
            renderer.setSeriesFillPaint(i, colors[i % colors.length]);
            renderer.setSeriesOutlinePaint(i, Color.WHITE);
            renderer.setSeriesOutlineStroke(i, new BasicStroke(1.2f));
        }

        plot.setRenderer(renderer);

        NumberAxis domain = (NumberAxis) plot.getDomainAxis();
        compactNumberAxis(domain, maxQuantity <= 0 ? 10 : maxQuantity);
        domain.setTickLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        domain.setLabelFont(new Font("Segoe UI", Font.BOLD, 14));

        NumberAxis range = (NumberAxis) plot.getRangeAxis();
        compactNumberAxis(range, maxRevenueMillion <= 0 ? 10 : maxRevenueMillion);
        range.setTickLabelFont(new Font("Segoe UI", Font.BOLD, 12));
        range.setLabelFont(new Font("Segoe UI", Font.BOLD, 14));

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

    private String percent(double value) {
        return new DecimalFormat("#,##0.##").format(value) + "%";
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
