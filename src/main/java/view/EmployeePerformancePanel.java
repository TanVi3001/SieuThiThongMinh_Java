package view;

import business.EmployeePerformanceService;
import business.sql.hr_kpi.KpiEvaluationSql;
import common.utils.FormatUtils;
import model.account.kpi.KpiEvaluation;
import model.employee.EmployeePerformance;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

// Import thêm thư viện để xuất Excel
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import view.components.IconHelper;

public class EmployeePerformancePanel extends JPanel {

    private static final String DEFAULT_POWER_BI_URL = System.getProperty("powerbi.report.url", "");

    private static final Color COLOR_BG = new Color(243, 245, 250);
    private static final Color COLOR_WHITE = Color.WHITE;
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_TEXT_DARK = new Color(30, 41, 59);
    private static final Color COLOR_TEXT_GRAY = new Color(100, 116, 139);
    private static final Color COLOR_BLUE = new Color(59, 130, 246);
    private static final Color COLOR_GREEN = new Color(34, 197, 94);
    private static final Color COLOR_ORANGE = new Color(249, 115, 22);
    private static final Color COLOR_RED = new Color(239, 68, 68);

    private final EmployeePerformanceService kpiService;
    private final KpiEvaluationSql kpiEvaluationSql;

    private JTable employeeTable;
    private JTable historyTable;
    private DefaultTableModel employeeTableModel;
    private DefaultTableModel historyTableModel;
    private JPanel chartHostPanel;
    private JPanel chartCardPanel;
    private JPanel powerBiCardPanel;
    private JFXPanel powerBiJfxPanel;
    private WebEngine powerBiEngine;
    private JLabel powerBiStatusLabel;
    private JToggleButton btnJFreeChartView;
    private JToggleButton btnPowerBiView;
    private JTextField txtPowerBiUrl;
    private boolean powerBiInitialized;

    private JLabel lblTopSaleName;
    private JLabel lblTopSaleRevenue;
    private JLabel lblBestDeliveryName;
    private JLabel lblTopKpiName;
    private JLabel lblSelectedEmployee;
    private JLabel lblSelectedEmployeeId;
    private JLabel lblSelectedSummary;

    private List<EmployeePerformance> currentDashboardData = new ArrayList<>();
    private String selectedEmployeeId;
    private List<KpiEvaluation> currentEmployeeHistory = new ArrayList<>();
    private String currentEmployeeName = "";

    public EmployeePerformancePanel() {
        this.kpiService = new EmployeePerformanceService();
        this.kpiEvaluationSql = KpiEvaluationSql.getInstance();
        initUI();
        loadData();
    }

    private void initUI() {
        setLayout(new BorderLayout(12, 12));
        setBackground(COLOR_BG);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel topContainer = new JPanel(new BorderLayout(0, 10));
        topContainer.setOpaque(false);
        topContainer.add(createToolbarPanel(), BorderLayout.NORTH);
        topContainer.add(createCardsPanel(), BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                createEmployeeListPanel(),
                createDetailPanel());
        mainSplit.setResizeWeight(0.34);
        mainSplit.setDividerSize(8);
        mainSplit.setBorder(null);
        add(mainSplit, BorderLayout.CENTER);
    }

    private JPanel createToolbarPanel() {
        JPanel panel = new ModernCardPanel(18);
        panel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));

        JButton btnImportKpi = createToolbarButton("Nhập KPI từ File", new Color(59, 130, 246));
        btnImportKpi.addActionListener(e -> openImportKpiDialog());

        JButton btnRefresh = createToolbarButton("Làm mới", new Color(34, 197, 94));
        btnRefresh.addActionListener(e -> loadData());

        // Nút Xuất Excel ở local panel
        JButton btnExport = createToolbarButton("Xuất Excel", new Color(245, 158, 11));
        btnExport.addActionListener(e -> exportToExcel());

        panel.add(btnImportKpi);
        panel.add(btnRefresh);
        panel.add(btnExport);
        return panel;
    }

    private JPanel createCardsPanel() {
        JPanel cards = new JPanel(new GridLayout(1, 3, 10, 10));
        cards.setOpaque(false);

        ModernCardPanel cardTopSale = createMetricCard("TOP SALE", new Color(41, 128, 185));
        lblTopSaleName = createMetricValueLabel("Đang tải...");
        lblTopSaleRevenue = createSecondaryMetricLabel("");
        cardTopSale.add(lblTopSaleName);
        cardTopSale.add(lblTopSaleRevenue);

        ModernCardPanel cardDelivery = createMetricCard("BEST DELIVERY", new Color(39, 174, 96));
        lblBestDeliveryName = createMetricValueLabel("Đang tải...");
        cardDelivery.add(lblBestDeliveryName);

        ModernCardPanel cardTopKpi = createMetricCard("NHÂN VIÊN XUẤT SẮC", new Color(243, 156, 18));
        lblTopKpiName = createMetricValueLabel("Đang tải...");
        cardTopKpi.add(lblTopKpiName);

        cards.add(cardTopSale);
        cards.add(cardDelivery);
        cards.add(cardTopKpi);
        return cards;
    }

    private ModernCardPanel createMetricCard(String title, Color bgColor) {
        ModernCardPanel card = new ModernCardPanel(20);
        card.setBackground(bgColor);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(Color.WHITE);
        card.add(lblTitle);
        card.add(Box.createVerticalStrut(10));
        return card;
    }

    private JLabel createMetricValueLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        label.setForeground(Color.WHITE);
        return label;
    }

    private JLabel createSecondaryMetricLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        label.setForeground(Color.WHITE);
        return label;
    }

    private JButton createToolbarButton(String text, Color background) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(background);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private JPanel createEmployeeListPanel() {
        ModernCardPanel card = new ModernCardPanel(22);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Danh sách nhân viên");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(COLOR_TEXT_DARK);
        JLabel subtitle = new JLabel("Chọn một nhân viên để xem KPI chi tiết");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(COLOR_TEXT_GRAY);
        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);

        String[] columns = {"Mã NV", "Tên NV", "Số đơn", "Doanh thu", "Hoàn thành (%)", "Giao hàng (%)", "Chuyên cần", "Điểm KPI"};
        employeeTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        employeeTable = new JTable(employeeTableModel);
        setupModernTable(employeeTable);
        employeeTable.getSelectionModel().addListSelectionListener(this::onEmployeeSelectionChanged);
        employeeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    loadSelectedEmployeeHistory();
                }
            }
        });

        setColumnWidths(employeeTable, new int[]{120, 160, 90, 110, 110, 110, 90, 90});

        JScrollPane scroll = new JScrollPane(employeeTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(COLOR_WHITE);

        card.add(header, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel createDetailPanel() {
        ModernCardPanel card = new ModernCardPanel(22);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel infoPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        infoPanel.setOpaque(false);

        lblSelectedEmployee = createInfoChip("Chọn một nhân viên");
        lblSelectedEmployeeId = createInfoChip("Mã NV: -");
        lblSelectedSummary = createInfoChip("KPI history sẽ hiển thị ở đây");

        infoPanel.add(lblSelectedEmployee);
        infoPanel.add(lblSelectedEmployeeId);
        infoPanel.add(lblSelectedSummary);

        JPanel historyPanel = createHistoryPanel();
        JPanel chartPanel = createChartPanel();

        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, historyPanel, chartPanel);
        verticalSplit.setResizeWeight(0.60);
        verticalSplit.setDividerSize(8);
        verticalSplit.setBorder(null);

        card.add(infoPanel, BorderLayout.NORTH);
        card.add(verticalSplit, BorderLayout.CENTER);
        return card;
    }

    private JLabel createInfoChip(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(COLOR_TEXT_DARK);
        label.setBorder(new EmptyBorder(8, 12, 8, 12));
        label.setOpaque(true);
        label.setBackground(new Color(248, 250, 252));
        return label;
    }

    private JPanel createHistoryPanel() {
        ModernCardPanel panel = new ModernCardPanel(18);
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JLabel title = new JLabel("Lịch sử KPI theo tháng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(COLOR_TEXT_DARK);

        historyTableModel = new DefaultTableModel(
                new Object[]{"Kỳ đánh giá", "Chỉ tiêu", "Mục tiêu", "Thực tế", "Điểm KPI", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        historyTable = new JTable(historyTableModel);
        setupModernTable(historyTable);
        historyTable.setRowHeight(38);
        setColumnWidths(historyTable, new int[]{110, 180, 90, 90, 90, 90});

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value == null ? "" : value.toString().trim();
                setHorizontalAlignment(JLabel.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 11));
                setText(status);
                setOpaque(true);
                if (status.equalsIgnoreCase("Đạt")) {
                    setBackground(isSelected ? new Color(187, 247, 208) : new Color(220, 252, 231));
                    setForeground(new Color(22, 101, 52));
                } else if (status.equalsIgnoreCase("Chưa đạt")) {
                    setBackground(isSelected ? new Color(254, 202, 202) : new Color(254, 226, 226));
                    setForeground(new Color(153, 27, 27));
                } else {
                    setBackground(isSelected ? new Color(226, 232, 240) : new Color(241, 245, 249));
                    setForeground(COLOR_TEXT_DARK);
                }
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(isSelected ? new Color(148, 163, 184) : getBackground().darker(), 1),
                        new EmptyBorder(4, 10, 4, 10)));
                return this;
            }
        };
        historyTable.getColumnModel().getColumn(5).setCellRenderer(statusRenderer);

        historyTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                double score = parseDouble(value);
                setHorizontalAlignment(JLabel.CENTER);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                setOpaque(true);
                if (score >= 8.0) {
                    setBackground(isSelected ? new Color(187, 247, 208) : new Color(220, 252, 231));
                    setForeground(new Color(22, 101, 52));
                } else if (score >= 6.0) {
                    setBackground(isSelected ? new Color(254, 240, 138) : new Color(254, 249, 195));
                    setForeground(new Color(161, 98, 7));
                } else {
                    setBackground(isSelected ? new Color(254, 202, 202) : new Color(254, 226, 226));
                    setForeground(new Color(153, 27, 27));
                }
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(isSelected ? new Color(148, 163, 184) : getBackground().darker(), 1),
                        new EmptyBorder(4, 10, 4, 10)));
                return this;
            }
        });

        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(COLOR_WHITE);

        panel.add(title, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createChartPanel() {
        ModernCardPanel panel = new ModernCardPanel(18);
        panel.setLayout(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel header = new JPanel(new BorderLayout(10, 8));
        header.setOpaque(false);

        JPanel titleBox = new JPanel(new GridLayout(2, 1));
        titleBox.setOpaque(false);
        JLabel title = new JLabel("Phân tích KPI theo tháng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(COLOR_TEXT_DARK);
        JLabel subtitle = new JLabel("Chuyển đổi giữa biểu đồ nội bộ và Power BI ngay trong Manager");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subtitle.setForeground(COLOR_TEXT_GRAY);
        titleBox.add(title);
        titleBox.add(subtitle);

        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        togglePanel.setOpaque(false);
        btnJFreeChartView = new JToggleButton("JFreeChart", true);
        btnPowerBiView = new JToggleButton("Power BI");
        btnJFreeChartView.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnPowerBiView.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnJFreeChartView.setFocusPainted(false);
        btnPowerBiView.setFocusPainted(false);
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(btnJFreeChartView);
        modeGroup.add(btnPowerBiView);
        btnJFreeChartView.addActionListener(e -> switchVisualizationMode(false));
        btnPowerBiView.addActionListener(e -> switchVisualizationMode(true));
        togglePanel.add(btnJFreeChartView);
        togglePanel.add(btnPowerBiView);

        header.add(titleBox, BorderLayout.WEST);
        header.add(togglePanel, BorderLayout.EAST);

        JPanel powerBiToolbar = new JPanel(new BorderLayout(8, 0));
        powerBiToolbar.setOpaque(false);
        txtPowerBiUrl = new JTextField(DEFAULT_POWER_BI_URL);
        txtPowerBiUrl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        txtPowerBiUrl.putClientProperty("JTextField.placeholderText", "Dán Power BI embed URL hoặc report URL ở đây");
        txtPowerBiUrl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(6, 10, 6, 10)));

        JButton btnLoadPowerBi = new JButton("Tải Power BI");
        btnLoadPowerBi.setIcon(IconHelper.lineChart(20));
        btnLoadPowerBi.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btnLoadPowerBi.setBackground(COLOR_BLUE);
        btnLoadPowerBi.setForeground(Color.WHITE);
        btnLoadPowerBi.setFocusPainted(false);
        btnLoadPowerBi.addActionListener(e -> {
            btnPowerBiView.setSelected(true);
            switchVisualizationMode(true);
        });

        powerBiToolbar.add(txtPowerBiUrl, BorderLayout.CENTER);
        powerBiToolbar.add(btnLoadPowerBi, BorderLayout.EAST);

        chartCardPanel = new JPanel(new BorderLayout());
        chartCardPanel.setOpaque(false);
        chartCardPanel.add(createEmptyChartPanel("Chọn một nhân viên để xem biểu đồ KPI"), BorderLayout.CENTER);

        powerBiCardPanel = createPowerBiPanel();

        chartHostPanel = new JPanel(new CardLayout());
        chartHostPanel.setOpaque(false);
        chartHostPanel.add(chartCardPanel, "CHART");
        chartHostPanel.add(powerBiCardPanel, "POWERBI");

        JPanel chartWrapper = new JPanel(new BorderLayout(0, 8));
        chartWrapper.setOpaque(false);
        chartWrapper.add(powerBiToolbar, BorderLayout.NORTH);
        chartWrapper.add(chartHostPanel, BorderLayout.CENTER);

        panel.add(header, BorderLayout.NORTH);
        panel.add(chartWrapper, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPowerBiPanel() {
        ModernCardPanel panel = new ModernCardPanel(18);
        panel.setLayout(new BorderLayout());

        powerBiStatusLabel = new JLabel("Dán Power BI report URL để hiển thị dashboard ngay trong Manager.", SwingConstants.CENTER);
        powerBiStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        powerBiStatusLabel.setForeground(COLOR_TEXT_GRAY);
        powerBiStatusLabel.setBorder(new EmptyBorder(10, 10, 10, 10));

        powerBiJfxPanel = new JFXPanel();
        panel.add(powerBiStatusLabel, BorderLayout.NORTH);
        panel.add(powerBiJfxPanel, BorderLayout.CENTER);

        Platform.runLater(() -> {
            WebView webView = new WebView();
            webView.setContextMenuEnabled(false);
            powerBiEngine = webView.getEngine();
            BorderPane root = new BorderPane(webView);
            powerBiJfxPanel.setScene(new Scene(root));
        });

        return panel;
    }

    private void switchVisualizationMode(boolean usePowerBi) {
        CardLayout layout = (CardLayout) chartHostPanel.getLayout();
        layout.show(chartHostPanel, usePowerBi ? "POWERBI" : "CHART");
        if (usePowerBi) {
            reloadPowerBiReport();
        } else {
            showJFreeChart(currentEmployeeHistory);
        }
    }

    private void refreshVisualization(List<KpiEvaluation> history) {
        if (btnPowerBiView != null && btnPowerBiView.isSelected()) {
            reloadPowerBiReport();
        } else {
            showJFreeChart(history);
        }
    }

    private void showJFreeChart(List<KpiEvaluation> history) {
        chartCardPanel.removeAll();
        if (history == null || history.isEmpty()) {
            chartCardPanel.add(createEmptyChartPanel("Chưa có dữ liệu để vẽ biểu đồ"), BorderLayout.CENTER);
            chartCardPanel.revalidate();
            chartCardPanel.repaint();
            return;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, ScoreBucket> buckets = new HashMap<>();
        for (KpiEvaluation item : history) {
            String period = safeText(item.getEvaluationPeriod());
            ScoreBucket bucket = buckets.computeIfAbsent(period, key -> new ScoreBucket());
            bucket.totalScore += item.getAchievedScore();
            bucket.count += 1;
        }

        List<String> orderedPeriods = new ArrayList<>(buckets.keySet());
        orderedPeriods.sort(this::comparePeriodsDescending);

        for (String period : orderedPeriods) {
            ScoreBucket bucket = buckets.get(period);
            double averageScore = bucket.count == 0 ? 0 : bucket.totalScore / bucket.count;
            dataset.addValue(averageScore, "Điểm KPI", period);
        }

        JFreeChart chart = ChartFactory.createLineChart(
                null,
                "Tháng",
                "Điểm KPI",
                dataset,
                PlotOrientation.VERTICAL,
                false,
                true,
                false);

        chart.setBackgroundPaint(COLOR_WHITE);
        chart.setBorderVisible(false);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(COLOR_WHITE);
        plot.setOutlineVisible(false);
        plot.setRangeGridlinePaint(COLOR_BORDER);

        LineAndShapeRenderer renderer = new LineAndShapeRenderer() {
            @Override
            public Shape getItemShape(int row, int column) {
                return new Ellipse2D.Double(-4, -4, 8, 8);
            }
        };
        renderer.setSeriesPaint(0, COLOR_BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShapesFilled(0, true);
        renderer.setDefaultToolTipGenerator(new StandardCategoryToolTipGenerator("{1}: {2}", new DecimalFormat("#,##0.00")));
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator("{2}", new DecimalFormat("#,##0.00")));
        renderer.setDefaultItemLabelPaint(COLOR_TEXT_DARK);
        renderer.setDefaultItemLabelFont(new Font("Segoe UI", Font.BOLD, 10));
        plot.setRenderer(renderer);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        domainAxis.setAxisLineVisible(false);
        domainAxis.setTickMarksVisible(false);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setUpperMargin(0.15);
        rangeAxis.setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 10));
        rangeAxis.setAxisLineVisible(false);
        rangeAxis.setTickMarksVisible(false);

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPopupMenu(null);
        chartPanel.setBackground(COLOR_WHITE);
        chartPanel.setMouseWheelEnabled(true);

        chartCardPanel.add(chartPanel, BorderLayout.CENTER);
        chartCardPanel.revalidate();
        chartCardPanel.repaint();
    }

    private void reloadPowerBiReport() {
        String baseUrl = txtPowerBiUrl != null ? txtPowerBiUrl.getText().trim() : "";
        if (baseUrl.isEmpty()) {
            updatePowerBiStatus("Dán Power BI report URL để hiển thị dashboard ngay trong Manager.");
            return;
        }

        Map<String, String> queryParams = new LinkedHashMap<>();
        if (selectedEmployeeId != null && !selectedEmployeeId.isBlank()) {
            queryParams.put("employeeId", selectedEmployeeId);
            queryParams.put("employee_id", selectedEmployeeId);
            queryParams.put("filter", "EMPLOYEES/employee_id eq '" + selectedEmployeeId.replace("'", "''") + "'");
        }
        if (currentEmployeeName != null && !currentEmployeeName.isBlank()) {
            queryParams.put("employeeName", currentEmployeeName);
            queryParams.put("employee_name", currentEmployeeName);
        }

        String reportUrl = buildPowerBiUrl(baseUrl, queryParams);
        updatePowerBiStatus("Đang tải Power BI report...");
        Platform.runLater(() -> {
            if (powerBiEngine == null) {
                return;
            }
            powerBiEngine.load(reportUrl);
        });
    }

    private void updatePowerBiStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            if (powerBiStatusLabel != null) {
                powerBiStatusLabel.setText(message);
            }
        });
    }

    private String buildPowerBiUrl(String baseUrl, Map<String, String> queryParams) {
        StringBuilder builder = new StringBuilder(baseUrl);
        String separator = baseUrl.contains("?") ? "&" : "?";
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            builder.append(separator)
                    .append(entry.getKey())
                    .append("=")
                    .append(encodeUrlValue(entry.getValue()));
            separator = "&";
        }
        return builder.toString();
    }

    private String encodeUrlValue(String value) {
        try {
            return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8.toString());
        } catch (Exception ex) {
            return value;
        }
    }

    private void setupModernTable(JTable table) {
        table.setRowHeight(36);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(241, 245, 249));
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setSelectionBackground(new Color(239, 246, 255));
        table.setSelectionForeground(COLOR_TEXT_DARK);
        table.setBackground(COLOR_WHITE);
        table.setFillsViewportHeight(true);
        table.setAutoCreateRowSorter(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 12));
        header.setBackground(new Color(248, 250, 252));
        header.setForeground(COLOR_TEXT_GRAY);
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 12));
        headerRenderer.setForeground(COLOR_TEXT_GRAY);
        headerRenderer.setBackground(new Color(248, 250, 252));
        headerRenderer.setBorder(new EmptyBorder(8, 8, 8, 8));
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 10, 0, 10));
                setFont(new Font("Segoe UI", Font.PLAIN, 12));
                setHorizontalAlignment(column == 1 ? JLabel.LEFT : JLabel.CENTER);
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? COLOR_WHITE : new Color(250, 252, 255));
                    setForeground(COLOR_TEXT_DARK);
                }
                return this;
            }
        });
    }

    private void setColumnWidths(JTable table, int[] widths) {
        for (int i = 0; i < widths.length && i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
    }

    private void openImportKpiDialog() {
        Window ownerWindow = SwingUtilities.getWindowAncestor(this);
        Frame ownerFrame = ownerWindow instanceof Frame ? (Frame) ownerWindow : null;
        ImportKpiDialog dialog = new ImportKpiDialog(ownerFrame);
        dialog.setVisible(true);
        loadData();
    }

    // XUẤT EXCEL CHUẨN KẾT NỐI VỚI DỮ LIỆU NHÂN VIÊN
    private void exportToExcel() {
        if (employeeTableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn vị trí lưu file Excel");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel Files", "xlsx"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().endsWith(".xlsx")) {
                file = new File(file.getAbsolutePath() + ".xlsx");
            }

            try (Workbook workbook = new XSSFWorkbook()) {
                // Sheet 1: Danh sách tổng quan hiệu suất
                Sheet sheet1 = workbook.createSheet("Tổng quan KPI");
                Row headerRow1 = sheet1.createRow(0);
                for (int i = 0; i < employeeTableModel.getColumnCount(); i++) {
                    headerRow1.createCell(i).setCellValue(employeeTableModel.getColumnName(i));
                }
                for (int i = 0; i < employeeTableModel.getRowCount(); i++) {
                    Row row = sheet1.createRow(i + 1);
                    for (int j = 0; j < employeeTableModel.getColumnCount(); j++) {
                        Object value = employeeTableModel.getValueAt(i, j);
                        row.createCell(j).setCellValue(value != null ? value.toString() : "");
                    }
                }

                // Sheet 2: Nếu có chọn nhân viên, xuất luôn chi tiết KPI của người đó
                if (selectedEmployeeId != null && !selectedEmployeeId.isBlank() && historyTableModel.getRowCount() > 0) {
                    Sheet sheet2 = workbook.createSheet("Chi tiết - " + currentEmployeeName);
                    Row headerRow2 = sheet2.createRow(0);
                    for (int i = 0; i < historyTableModel.getColumnCount(); i++) {
                        headerRow2.createCell(i).setCellValue(historyTableModel.getColumnName(i));
                    }
                    for (int i = 0; i < historyTableModel.getRowCount(); i++) {
                        Row row = sheet2.createRow(i + 1);
                        for (int j = 0; j < historyTableModel.getColumnCount(); j++) {
                            Object value = historyTableModel.getValueAt(i, j);
                            row.createCell(j).setCellValue(value != null ? value.toString() : "");
                        }
                    }
                }

                try (FileOutputStream out = new FileOutputStream(file)) {
                    workbook.write(out);
                }
                JOptionPane.showMessageDialog(this, "Xuất dữ liệu Excel thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                showError("Lỗi khi xuất file Excel: " + ex.getMessage(), ex);
            }
        }
    }

    protected void loadData() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        SwingWorker<List<EmployeePerformance>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<EmployeePerformance> doInBackground() {
                return kpiService.getDashboardData();
            }

            @Override
            protected void done() {
                try {
                    List<EmployeePerformance> rawData = get();
                    currentDashboardData = new ArrayList<>();

                    // BỘ LỌC CHẶT CHẼ HƠN ẨN ADMIN / QUẢN LÝ
                    for (EmployeePerformance ep : rawData) {
                        String empId = ep.getEmployeeId() != null ? ep.getEmployeeId().toUpperCase() : "";
                        String empName = ep.getEmployeeName() != null ? ep.getEmployeeName().toUpperCase() : "";

                        boolean isAdminOrManager = empId.contains("ADMIN") || empId.contains("MNG") || empId.contains("MANAGER") || empId.contains("SYS")
                                || empName.contains("ADMIN") || empName.contains("QUẢN LÝ") || empName.contains("MANAGER");

                        if (!isAdminOrManager) {
                            currentDashboardData.add(ep);
                        }
                    }

                    populateEmployeeTable(currentDashboardData);
                    updateSummaryCards(currentDashboardData);

                    if (!currentDashboardData.isEmpty() && employeeTable.getSelectedRow() < 0) {
                        employeeTable.setRowSelectionInterval(0, 0);
                        loadSelectedEmployeeHistory();
                    } else if (currentDashboardData.isEmpty()) {
                        clearEmployeeDetail("Không có dữ liệu KPI", "");
                    }
                } catch (Exception ex) {
                    showError("Không thể tải dữ liệu hiệu suất nhân viên.", ex);
                } finally {
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        };
        worker.execute();
    }

    private void populateEmployeeTable(List<EmployeePerformance> data) {
        employeeTableModel.setRowCount(0);
        if (data == null) {
            return;
        }

        for (EmployeePerformance ep : data) {
            employeeTableModel.addRow(new Object[]{
                ep.getEmployeeId(),
                ep.getEmployeeName(),
                ep.getTotalOrders(),
                FormatUtils.formatCurrency(ep.getRevenue()),
                String.format(Locale.ROOT, "%.2f", ep.getCompletionRate()),
                String.format(Locale.ROOT, "%.2f", ep.getDeliverySuccessRate()),
                String.format(Locale.ROOT, "%.2f", ep.getAttendanceScore()),
                String.format(Locale.ROOT, "%.2f", ep.getPerformanceScore())
            });
        }
    }

    private void updateSummaryCards(List<EmployeePerformance> data) {
        if (data == null || data.isEmpty()) {
            lblTopSaleName.setText("Đang tải...");
            lblTopSaleRevenue.setText("");
            lblBestDeliveryName.setText("Đang tải...");
            lblTopKpiName.setText("Đang tải...");
            return;
        }

        EmployeePerformance topSale = kpiService.getTopSaleEmployee(data);
        if (topSale != null) {
            lblTopSaleName.setText(topSale.getEmployeeName());
            lblTopSaleRevenue.setText(FormatUtils.formatCurrency(topSale.getRevenue()));
        }

        EmployeePerformance bestDelivery = kpiService.getBestDeliveryEmployee(data);
        if (bestDelivery != null) {
            lblBestDeliveryName.setText(bestDelivery.getEmployeeName() + " (" + String.format(Locale.ROOT, "%.1f", bestDelivery.getDeliverySuccessRate()) + "%)");
        }

        EmployeePerformance topKpi = kpiService.getTopKpiEmployee(data);
        if (topKpi != null) {
            lblTopKpiName.setText(topKpi.getEmployeeName() + " (" + String.format(Locale.ROOT, "%.2f", topKpi.getPerformanceScore()) + ")");
        }
    }

    private void onEmployeeSelectionChanged(ListSelectionEvent event) {
        if (event.getValueIsAdjusting()) {
            return;
        }
        loadSelectedEmployeeHistory();
    }

    // LINK DATA VỚI EMPLOYEE (Đã tích hợp sẵn và chuẩn xác qua kpiEvaluationSql)
    private void loadSelectedEmployeeHistory() {
        int viewRow = employeeTable.getSelectedRow();
        if (viewRow < 0) {
            return;
        }

        int modelRow = employeeTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= employeeTableModel.getRowCount()) {
            return;
        }

        String employeeId = String.valueOf(employeeTableModel.getValueAt(modelRow, 0));
        String employeeName = String.valueOf(employeeTableModel.getValueAt(modelRow, 1));
        selectedEmployeeId = employeeId;

        lblSelectedEmployee.setText(employeeName);
        lblSelectedEmployeeId.setText("Mã NV: " + employeeId);
        lblSelectedSummary.setText("Đang tải KPI lịch sử...");

        loadKpiHistoryAsync(employeeId, employeeName);
    }

    private void loadKpiHistoryAsync(String employeeId, String employeeName) {
        SwingWorker<List<KpiEvaluation>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<KpiEvaluation> doInBackground() {
                return kpiEvaluationSql.findByEmployeeId(employeeId); // Dữ liệu đã được link theo Mã NV
            }

            @Override
            protected void done() {
                try {
                    List<KpiEvaluation> history = get();
                    currentEmployeeHistory = history != null ? history : new ArrayList<>();
                    currentEmployeeName = employeeName;
                    populateHistoryTable(history);
                    updateHistorySummary(employeeName, history);
                    refreshVisualization(history);
                } catch (Exception ex) {
                    showError("Không thể tải lịch sử KPI của nhân viên " + employeeName + ".", ex);
                }
            }
        };
        worker.execute();
    }

    private void populateHistoryTable(List<KpiEvaluation> history) {
        historyTableModel.setRowCount(0);
        if (history == null || history.isEmpty()) {
            historyTableModel.addRow(new Object[]{"-", "Chưa có dữ liệu KPI", "-", "-", "-", "Chưa có dữ liệu"});
            return;
        }

        DecimalFormat numberFormat = new DecimalFormat("#,##0.##");
        for (KpiEvaluation item : history) {
            String status = item.getEvaluationStatus();
            if (status == null || status.isBlank()) {
                status = item.getActualValue() >= item.getMinimumTarget() ? "Đạt" : "Chưa đạt";
            }
            historyTableModel.addRow(new Object[]{
                safeText(item.getEvaluationPeriod()),
                safeText(item.getCriteriaName()),
                numberFormat.format(item.getMinimumTarget()),
                numberFormat.format(item.getActualValue()),
                numberFormat.format(item.getAchievedScore()),
                status
            });
        }
    }

    private void updateHistorySummary(String employeeName, List<KpiEvaluation> history) {
        if (history == null || history.isEmpty()) {
            lblSelectedSummary.setText(employeeName + " chưa có dữ liệu KPI lịch sử");
            return;
        }

        double averageScore = history.stream()
                .mapToDouble(KpiEvaluation::getAchievedScore)
                .average()
                .orElse(0);
        long passedCount = history.stream()
                .filter(item -> "Đạt".equalsIgnoreCase(safeText(item.getEvaluationStatus())))
                .count();

        lblSelectedSummary.setText("TB điểm KPI: " + String.format(Locale.ROOT, "%.2f", averageScore)
                + " | Số kỳ đạt: " + passedCount + "/" + history.size());
    }

    private JPanel createEmptyChartPanel(String message) {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(false);
        JLabel label = new JLabel(message);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(COLOR_TEXT_GRAY);
        empty.add(label);
        return empty;
    }

    private void clearEmployeeDetail(String employeeName, String employeeId) {
        lblSelectedEmployee.setText(employeeName);
        lblSelectedEmployeeId.setText(employeeId == null || employeeId.isBlank() ? "Mã NV: -" : "Mã NV: " + employeeId);
        lblSelectedSummary.setText("Không có dữ liệu KPI");
        historyTableModel.setRowCount(0);
        chartHostPanel.removeAll();
        chartHostPanel.add(createEmptyChartPanel("Không có dữ liệu KPI"), BorderLayout.CENTER);
        chartHostPanel.revalidate();
        chartHostPanel.repaint();
    }

    private void showError(String message, Exception ex) {
        ex.printStackTrace();
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE));
    }

    private String safeText(String text) {
        return text == null || text.isBlank() ? "-" : text.trim();
    }

    private int comparePeriodsDescending(String left, String right) {
        YearMonth leftMonth = parseYearMonth(left);
        YearMonth rightMonth = parseYearMonth(right);
        if (leftMonth != null && rightMonth != null) {
            return rightMonth.compareTo(leftMonth);
        }
        if (leftMonth != null) {
            return -1;
        }
        if (rightMonth != null) {
            return 1;
        }
        return right.compareToIgnoreCase(left);
    }

    private YearMonth parseYearMonth(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }

        DateTimeFormatter[] formatters = new DateTimeFormatter[]{
            DateTimeFormatter.ofPattern("yyyy-MM"),
            DateTimeFormatter.ofPattern("MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy/MM"),
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return YearMonth.parse(normalized, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private void loadDataAsyncIfNeeded() {
        loadData();
    }

    private static final class ScoreBucket {

        double totalScore;
        int count;
    }

    private double parseDouble(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Double.parseDouble(value.toString().replace(",", ""));
        } catch (Exception ex) {
            return 0;
        }
    }

    private static class ModernCardPanel extends JPanel {

        private final int arc;

        ModernCardPanel(int arc) {
            this.arc = arc;
            setOpaque(false);
            setBackground(COLOR_WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(COLOR_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
        }
    }
}
