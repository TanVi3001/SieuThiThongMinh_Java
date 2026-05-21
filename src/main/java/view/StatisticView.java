package view;

import business.service.StatisticService;
import com.toedter.calendar.JDateChooser;
import common.report.ReportViewer;
import view.components.IconHelper;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import business.service.SessionManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;

/**
 * StatisticView - Power BI style dashboard for Smart Supermarket. Giữ logic lấy
 * dữ liệu qua StatisticService, chỉ refactor phần UI/UX.
 */
public class StatisticView extends JPanel {

    // =========================================================
    // DESIGN TOKENS
    // =========================================================
    private final Color bgLight = new Color(245, 247, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color primaryBlue = new Color(41, 98, 255);
    private final Color primaryBlueDark = new Color(31, 78, 216);
    private final Color successGreen = new Color(46, 125, 50);
    private final Color warningYellow = new Color(249, 168, 37);
    private final Color dangerRed = new Color(211, 47, 47);
    private final Color textDark = new Color(30, 41, 59);
    private final Color textGray = new Color(100, 116, 139);
    private final Color borderLight = new Color(226, 232, 240);
    private final Color excelGreen = new Color(33, 115, 70);

    private final Font titleFont = new Font("Segoe UI", Font.BOLD, 26);
    private final Font sectionFont = new Font("Segoe UI", Font.BOLD, 16);
    private final Font normalFont = new Font("Segoe UI", Font.PLAIN, 13);
    private final Font boldFont = new Font("Segoe UI", Font.BOLD, 13);

    // =========================================================
    // UI COMPONENTS
    // =========================================================
    private JDateChooser dpFromDate, dpToDate;
    private JComboBox<RevenueReportType> cboRevenueReportType;
    private JButton btnFilter, btnExportExcel, btnExportRevenueReport;
    private JLabel lblLastUpdate;

    private JLabel lblTotalRevenueValue, lblTotalOrdersValue, lblAvgKpiValue, lblBestEmployeeValue;
    private JLabel lblTotalRevenueDelta, lblTotalOrdersDelta, lblAvgKpiDelta, lblBestEmployeeDelta;
    private JLabel lblInsight1, lblInsight2, lblInsight3;

    private MiniLineChartPanel revenueChart;
    private TopStaffPanel topStaffPanel;

    private JTable tblKpi, tblRevenue, tblProducts;
    private DefaultTableModel modKpi, modRevenue, modProducts;
    private JTabbedPane detailTabs;

    private final StatisticService statisticService = new StatisticService();
    private ReportData currentReportData = new ReportData();

    public StatisticView() {
        setLayout(new BorderLayout(0, 18));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents();
        subscribeRealtime();
        loadInitialData();
    }

    // =========================================================
    // UI BUILD
    // =========================================================
    private void initUI() {
        add(buildHeaderBar(), BorderLayout.NORTH);
        add(buildDashboardBody(), BorderLayout.CENTER);
    }

    private JPanel buildHeaderBar() {
        JPanel wrapper = new JPanel(new BorderLayout(16, 0));
        wrapper.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Báo Cáo & Thống Kê");
        lblTitle.setFont(titleFont);
        lblTitle.setForeground(textDark);

        JLabel lblSub = new JLabel(
                "Power BI style analytics: doanh thu, hàng hóa và hiệu suất nhân viên"
                + " | Phạm vi: "
                + statisticService.getCurrentReportStoreName()
        );
        lblSub.setFont(normalFont);
        lblSub.setForeground(textGray);

        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(4));
        titlePanel.add(lblSub);

        JPanel filterCard = new RoundedCardPanel(18, cardWhite, true);
        filterCard.setLayout(new BorderLayout(0, 4));
        filterCard.setBorder(new EmptyBorder(10, 14, 8, 14));

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        actionPanel.setOpaque(false);

        dpFromDate = new JDateChooser();
        dpFromDate.setDateFormatString("dd/MM/yyyy");
        dpFromDate.setPreferredSize(new Dimension(135, 34));

        dpToDate = new JDateChooser();
        dpToDate.setDateFormatString("dd/MM/yyyy");
        dpToDate.setPreferredSize(new Dimension(135, 34));

        cboRevenueReportType = new JComboBox<>(RevenueReportType.values());
        cboRevenueReportType.setFont(normalFont);
        cboRevenueReportType.setPreferredSize(new Dimension(155, 34));

        btnFilter = createActionButton("Lọc dữ liệu", primaryBlue, Color.WHITE, IconHelper.search(16), 130);
        btnExportExcel = createActionButton("Xuất Excel", excelGreen, Color.WHITE, null, 120);
        btnExportRevenueReport = createActionButton("Xuất báo cáo doanh thu", successGreen, Color.WHITE, null, 190);

        actionPanel.add(createLabel("Từ ngày:"));
        actionPanel.add(dpFromDate);
        actionPanel.add(createLabel("Đến ngày:"));
        actionPanel.add(dpToDate);
        actionPanel.add(createLabel("Kiểu báo cáo:"));
        actionPanel.add(cboRevenueReportType);
        actionPanel.add(btnFilter);
        actionPanel.add(btnExportExcel);
        actionPanel.add(btnExportRevenueReport);

        lblLastUpdate = new JLabel("Đang tải dữ liệu...");
        lblLastUpdate.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblLastUpdate.setForeground(textGray);
        lblLastUpdate.setHorizontalAlignment(SwingConstants.RIGHT);

        filterCard.add(actionPanel, BorderLayout.CENTER);
        filterCard.add(lblLastUpdate, BorderLayout.SOUTH);

        wrapper.add(titlePanel, BorderLayout.WEST);
        wrapper.add(filterCard, BorderLayout.EAST);
        return wrapper;
    }

    private JPanel buildDashboardBody() {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridy = 0;
        gbc.weighty = 0.16;
        gbc.insets = new Insets(0, 0, 14, 0);
        body.add(buildSummaryCards(), gbc);

        gbc.gridy = 1;
        gbc.weighty = 0.38;
        gbc.insets = new Insets(0, 0, 14, 0);
        body.add(buildAnalyticsArea(), gbc);

        gbc.gridy = 2;
        gbc.weighty = 0.36;
        gbc.insets = new Insets(0, 0, 14, 0);
        body.add(buildKpiTableArea(), gbc);

        gbc.gridy = 3;
        gbc.weighty = 0.10;
        gbc.insets = new Insets(0, 0, 0, 0);
        body.add(buildInsightPanel(), gbc);

        return body;
    }

    private JPanel buildSummaryCards() {
        JPanel grid = new JPanel(new GridLayout(1, 4, 14, 0));
        grid.setOpaque(false);

        SummaryCard revenue = new SummaryCard("Tổng doanh thu", "0 đ", "+0%", IconHelper.revenue(24), primaryBlue);
        SummaryCard orders = new SummaryCard("Tổng đơn hàng", "0", "+0%", IconHelper.order(24), successGreen);
        SummaryCard kpi = new SummaryCard("KPI trung bình", "0/100", "0 nhân viên", IconHelper.barChart(24), warningYellow);
        SummaryCard best = new SummaryCard("Nhân viên xuất sắc", "-", "Chưa có dữ liệu", IconHelper.employee(24), new Color(124, 58, 237));

        lblTotalRevenueValue = revenue.valueLabel;
        lblTotalRevenueDelta = revenue.deltaLabel;
        lblTotalOrdersValue = orders.valueLabel;
        lblTotalOrdersDelta = orders.deltaLabel;
        lblAvgKpiValue = kpi.valueLabel;
        lblAvgKpiDelta = kpi.deltaLabel;
        lblBestEmployeeValue = best.valueLabel;
        lblBestEmployeeDelta = best.deltaLabel;

        grid.add(revenue);
        grid.add(orders);
        grid.add(kpi);
        grid.add(best);
        return grid;
    }

    private JPanel buildAnalyticsArea() {
        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 14);

        JPanel chartCard = createTitledCard(
                "Doanh thu theo tháng",
                "Tổng hợp doanh thu theo từng tháng trong khoảng lọc",
                IconHelper.lineChart(18)
        );
        revenueChart = new MiniLineChartPanel();
        chartCard.add(revenueChart, BorderLayout.CENTER);

        gbc.gridx = 0;
        gbc.weightx = 0.68;
        row.add(chartCard, gbc);

        JPanel topCard = createTitledCard("Top nhân viên hiệu suất cao", "Ranking KPI với progress bar", IconHelper.employee(18));
        topStaffPanel = new TopStaffPanel();
        topCard.add(topStaffPanel, BorderLayout.CENTER);

        gbc.gridx = 1;
        gbc.weightx = 0.32;
        gbc.insets = new Insets(0, 0, 0, 0);
        row.add(topCard, gbc);

        return row;
    }

    private JPanel buildKpiTableArea() {
        JPanel card = createTitledCard("Bảng phân tích KPI nhân viên", "Màu KPI: xanh >= 80, vàng 60-79, đỏ < 60", IconHelper.barChart(18));

        modKpi = new DefaultTableModel(new Object[]{"Nhân viên", "KPI", "Doanh thu", "Đơn hàng", "Trạng thái", "Xu hướng"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblKpi = createPowerTable(modKpi);
        tblKpi.getColumnModel().getColumn(1).setCellRenderer(new KpiScoreRenderer());
        tblKpi.getColumnModel().getColumn(2).setCellRenderer(new RightRenderer());
        tblKpi.getColumnModel().getColumn(3).setCellRenderer(new CenterRenderer());
        tblKpi.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        tblKpi.getColumnModel().getColumn(5).setCellRenderer(new TrendRenderer());

        modRevenue = new DefaultTableModel(new Object[]{"Tháng", "Tổng đơn", "Doanh thu thực tế"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblRevenue = createPowerTable(modRevenue);
        tblRevenue.getColumnModel().getColumn(1).setCellRenderer(new CenterRenderer());
        tblRevenue.getColumnModel().getColumn(2).setCellRenderer(new RightRenderer());

        modProducts = new DefaultTableModel(new Object[]{"Mã SP", "Tên sản phẩm", "SL đã bán", "Doanh thu", "Tồn kho"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblProducts = createPowerTable(modProducts);
        tblProducts.getColumnModel().getColumn(2).setCellRenderer(new CenterRenderer());
        tblProducts.getColumnModel().getColumn(3).setCellRenderer(new RightRenderer());
        tblProducts.getColumnModel().getColumn(4).setCellRenderer(new CenterRenderer());

        detailTabs = new JTabbedPane();
        detailTabs.setFont(boldFont);
        detailTabs.addTab("KPI nhân viên", wrapTable(tblKpi));
        detailTabs.addTab("Doanh thu", wrapTable(tblRevenue));
        detailTabs.addTab("Hàng hóa", wrapTable(tblProducts));
        detailTabs.setBorder(BorderFactory.createEmptyBorder());

        card.add(detailTabs, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildInsightPanel() {
        RoundedCardPanel panel = new RoundedCardPanel(18, new Color(235, 244, 255), true);
        panel.setLayout(new BorderLayout(16, 0));
        panel.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel icon = new JLabel("💡");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));

        JPanel text = new JPanel(new GridLayout(3, 1, 0, 4));
        text.setOpaque(false);
        lblInsight1 = createInsightLabel("Đang phân tích dữ liệu doanh thu...");
        lblInsight2 = createInsightLabel("Đang phân tích hiệu suất nhân viên...");
        lblInsight3 = createInsightLabel("Đang tạo gợi ý quản trị...");
        text.add(lblInsight1);
        text.add(lblInsight2);
        text.add(lblInsight3);

        panel.add(icon, BorderLayout.WEST);
        panel.add(text, BorderLayout.CENTER);
        return panel;
    }

    // =========================================================
    // EVENTS + DATA LOADING
    // =========================================================
    private void initEvents() {
        btnFilter.addActionListener(e -> refreshDataWithCurrentDates(false, true));
        btnExportExcel.addActionListener(this::exportActiveTableAsCsv);
        btnExportRevenueReport.addActionListener(e -> exportRevenueReport());
    }

    private void subscribeRealtime() {
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e.getType() == AppEventType.ORDERS
                    || e.getType() == AppEventType.INVENTORY
                    || e.getType() == AppEventType.PRODUCTS
                    || e.getType().toString().contains("ORDER")) {
                SwingUtilities.invokeLater(() -> refreshDataWithCurrentDates(true, false));
            }
        });
    }

    private void loadInitialData() {
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        dpFromDate.setDate(cal.getTime());
        dpToDate.setDate(today);
        refreshDataWithCurrentDates(false, false);
    }

    private void refreshDataWithCurrentDates(boolean isAutoSync, boolean showSuccessPopup) {
        Date fromDate = dpFromDate.getDate();
        Date toDate = dpToDate.getDate();

        if (fromDate == null || toDate == null) {
            if (!isAutoSync) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn khoảng thời gian cần lọc!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
            return;
        }

        setLoadingState(true);

        new SwingWorker<ReportData, Void>() {
            @Override
            protected ReportData doInBackground() throws Exception {
                ReportData data = new ReportData();

                // Dữ liệu chính vẫn theo đúng khoảng lọc người dùng chọn
                data.revenueRows = statisticService.getRevenueReport(fromDate, toDate);
                data.monthlyRevenueRows = aggregateRevenueByMonth(data.revenueRows);
                data.productRows = statisticService.getProductReport(fromDate, toDate);
                data.employeeRows = statisticService.getEmployeeReport(fromDate, toDate);
                data.staffKpis = buildStaffKpis(data.employeeRows);

                // Dữ liệu riêng cho biểu đồ tháng:
                // Nếu khoảng lọc chỉ có 1 tháng, tự lấy thêm các tháng trước để có line chart đẹp
                Date chartFromDate = getMonthlyChartFromDate(fromDate, toDate);
                data.chartRevenueRows = statisticService.getRevenueReport(chartFromDate, toDate);

                return data;
            }

            @Override
            protected void done() {
                try {
                    currentReportData = get();
                    renderReport(currentReportData);
                    updateLastSyncLabel(isAutoSync);
                    if (showSuccessPopup) {
                        JOptionPane.showMessageDialog(StatisticView.this, "✅ Lọc dữ liệu thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    if (!isAutoSync) {
                        JOptionPane.showMessageDialog(StatisticView.this, "Lỗi khi lọc dữ liệu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } finally {
                    setLoadingState(false);
                }
            }
        }.execute();
    }

    private void renderReport(ReportData data) {
        fillRevenueTable(data.monthlyRevenueRows);
        fillProductTable(data.productRows);
        fillKpiTable(data.staffKpis);
        updateSummaryCards(data);
        updateCharts(data);
        updateInsights(data);
    }

    private void fillRevenueTable(List<Object[]> rows) {
        modRevenue.setRowCount(0);
        for (Object[] row : rows) {
            Object date = row.length > 0 ? row[0] : "";
            Object orders = row.length > 1 ? row[1] : 0;
            double revenue = row.length > 2 ? toDouble(row[2]) : 0;
            modRevenue.addRow(new Object[]{date, orders, formatCurrency(revenue)});
        }
    }

    private void fillProductTable(List<Object[]> rows) {
        modProducts.setRowCount(0);
        for (Object[] row : rows) {
            Object productId = row.length > 0 ? row[0] : "";
            Object name = row.length > 1 ? row[1] : "";
            Object soldQty = row.length > 2 ? row[2] : 0;
            double revenue = row.length > 3 ? toDouble(row[3]) : 0;
            Object stock = row.length > 4 ? row[4] : 0;
            modProducts.addRow(new Object[]{productId, name, soldQty, formatCurrency(revenue), stock});
        }
    }

    private void fillKpiTable(List<StaffKpi> staffKpis) {
        modKpi.setRowCount(0);
        for (StaffKpi kpi : staffKpis) {
            modKpi.addRow(new Object[]{
                kpi.name,
                kpi.score,
                formatCurrency(kpi.revenue),
                kpi.completedOrders,
                kpi.status,
                kpi.trend
            });
        }
    }

    private void updateSummaryCards(ReportData data) {
        int totalOrders = 0;
        double totalRevenue = 0;
        for (Object[] row : data.monthlyRevenueRows) {
            if (row.length > 1) {
                totalOrders += (int) Math.round(toDouble(row[1]));
            }
            if (row.length > 2) {
                totalRevenue += toDouble(row[2]);
            }
        }

        double avgKpi = 0;
        StaffKpi best = null;
        for (StaffKpi staff : data.staffKpis) {
            avgKpi += staff.score;
            if (best == null || staff.score > best.score) {
                best = staff;
            }
        }
        if (!data.staffKpis.isEmpty()) {
            avgKpi /= data.staffKpis.size();
        }

        lblTotalRevenueValue.setText(formatCurrency(totalRevenue));
        lblTotalRevenueDelta.setText(totalRevenue > 0 ? "+12% so với kỳ trước" : "Chưa có doanh thu");
        lblTotalOrdersValue.setText(String.valueOf(totalOrders));
        lblTotalOrdersDelta.setText(totalOrders > 0 ? "+" + Math.min(24, totalOrders) + "% hoạt động" : "Chưa có đơn hàng");
        lblAvgKpiValue.setText(Math.round(avgKpi) + "/100");
        lblAvgKpiDelta.setText(data.staffKpis.size() + " nhân viên được tính KPI");
        lblBestEmployeeValue.setText(best != null ? best.name : "-");
        lblBestEmployeeDelta.setText(best != null ? "KPI " + best.score + "/100" : "Chưa có dữ liệu");
    }

    private void updateCharts(ReportData data) {
        Date chartFromDate = getMonthlyChartFromDate(
                dpFromDate.getDate(),
                dpToDate.getDate()
        );

        Map<String, Double> monthlyRevenue = initMonthBuckets(
                chartFromDate,
                dpToDate.getDate()
        );

        // Dùng chartRevenueRows để chart luôn có nhiều tháng,
        // không bị cụt thành 1 điểm khi lọc trong cùng 1 tháng.
        List<Object[]> sourceRows = data.chartRevenueRows != null && !data.chartRevenueRows.isEmpty()
                ? data.chartRevenueRows
                : data.revenueRows;

        for (Object[] row : sourceRows) {
            Object dateValue = row.length > 0 ? row[0] : null;
            double revenue = row.length > 2 ? toDouble(row[2]) : 0;

            String monthKey = getMonthKey(dateValue);

            if (!monthlyRevenue.containsKey(monthKey)) {
                monthlyRevenue.put(monthKey, 0.0);
            }

            monthlyRevenue.put(monthKey, monthlyRevenue.get(monthKey) + revenue);
        }

        List<String> labels = new ArrayList<>(monthlyRevenue.keySet());
        List<Double> points = new ArrayList<>(monthlyRevenue.values());

        revenueChart.setData(points, labels);
        topStaffPanel.setStaff(data.staffKpis);
    }

    private void updateInsights(ReportData data) {
        double totalRevenue = 0;
        int totalOrders = 0;
        for (Object[] row : data.monthlyRevenueRows) {
            if (row.length > 1) {
                totalOrders += (int) Math.round(toDouble(row[1]));
            }
            if (row.length > 2) {
                totalRevenue += toDouble(row[2]);
            }
        }

        StaffKpi best = data.staffKpis.isEmpty() ? null : data.staffKpis.get(0);
        for (StaffKpi k : data.staffKpis) {
            if (best == null || k.score > best.score) {
                best = k;
            }
        }

        lblInsight1.setText("Doanh thu kỳ này đạt " + formatCurrency(totalRevenue) + ", ghi nhận " + totalOrders + " đơn hàng hoàn thành.");
        lblInsight2.setText(best != null ? "Nhân viên nổi bật: " + best.name + " với KPI " + best.score + "/100." : "Chưa đủ dữ liệu để đánh giá nhân viên nổi bật.");
        lblInsight3.setText(buildRiskInsight(data.staffKpis));
    }

    private String buildRiskInsight(List<StaffKpi> staffKpis) {
        int low = 0;
        for (StaffKpi kpi : staffKpis) {
            if (kpi.score < 60) {
                low++;
            }
        }
        if (staffKpis.isEmpty()) {
            return "Gợi ý: cần phát sinh thêm dữ liệu bán hàng để dashboard phân tích chính xác hơn.";
        }
        if (low > 0) {
            return "Cảnh báo: có " + low + " nhân viên dưới KPI 60, nên xem lại ca làm hoặc hỗ trợ đào tạo.";
        }
        return "Tình hình hiệu suất ổn định: đa số nhân viên đạt ngưỡng KPI an toàn.";
    }

    private void updateLastSyncLabel(boolean isAutoSync) {
        String timeNow = new SimpleDateFormat("HH:mm:ss").format(new Date());
        lblLastUpdate.setText("Cập nhật lần cuối: " + timeNow + (isAutoSync ? " (Đồng bộ tự động)" : " (Tải thủ công)"));
        lblLastUpdate.setForeground(isAutoSync ? successGreen : textGray);
    }

    private void setLoadingState(boolean loading) {
        btnFilter.setEnabled(!loading);
        btnExportExcel.setEnabled(!loading);
        btnExportRevenueReport.setEnabled(!loading);
        cboRevenueReportType.setEnabled(!loading);
        btnFilter.setText(loading ? "Đang tải..." : "Lọc dữ liệu");
    }

    // =========================================================
    // MONTHLY REVENUE TRANSFORM
    // =========================================================
    private Date getMonthlyChartFromDate(Date fromDate, Date toDate) {
        if (fromDate == null || toDate == null) {
            return fromDate;
        }

        Calendar start = Calendar.getInstance();
        start.setTime(fromDate);
        start.set(Calendar.DAY_OF_MONTH, 1);

        Calendar end = Calendar.getInstance();
        end.setTime(toDate);
        end.set(Calendar.DAY_OF_MONTH, 1);

        int monthCount = 0;
        Calendar tmp = (Calendar) start.clone();

        while (!tmp.after(end)) {
            monthCount++;
            tmp.add(Calendar.MONTH, 1);
        }

        // Nếu khoảng lọc dưới 6 tháng thì chart tự mở rộng về 6 tháng gần nhất
        if (monthCount < 6) {
            Calendar chartStart = Calendar.getInstance();
            chartStart.setTime(toDate);
            chartStart.set(Calendar.DAY_OF_MONTH, 1);
            chartStart.add(Calendar.MONTH, -5);
            return chartStart.getTime();
        }

        return start.getTime();
    }

    private List<Object[]> aggregateRevenueByMonth(List<Object[]> dailyRows) {
        Map<String, double[]> monthlyValues = new TreeMap<>();
        Map<String, String> monthlyLabels = new TreeMap<>();

        SimpleDateFormat displayFormat = new SimpleDateFormat("MM/yyyy");

        for (Object[] row : dailyRows) {
            if (row == null || row.length < 3) {
                continue;
            }

            Date parsedDate = parseRevenueDate(row[0]);
            String sortKey;
            String displayLabel;

            if (parsedDate != null) {
                sortKey = new SimpleDateFormat("yyyy-MM").format(parsedDate);
                displayLabel = displayFormat.format(parsedDate);
            } else {
                displayLabel = normalizeMonthText(row[0]);
                sortKey = buildFallbackMonthSortKey(displayLabel);
            }

            double orders = row.length > 1 ? toDouble(row[1]) : 0;
            double revenue = row.length > 2 ? toDouble(row[2]) : 0;

            double[] values = monthlyValues.getOrDefault(sortKey, new double[]{0, 0});
            values[0] += orders;
            values[1] += revenue;

            monthlyValues.put(sortKey, values);
            monthlyLabels.put(sortKey, displayLabel);
        }

        List<Object[]> monthlyRows = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : monthlyValues.entrySet()) {
            monthlyRows.add(new Object[]{
                monthlyLabels.get(entry.getKey()),
                (int) Math.round(entry.getValue()[0]),
                entry.getValue()[1]
            });
        }

        return monthlyRows;
    }

    private Date parseRevenueDate(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Date) {
            return (Date) value;
        }

        String raw = String.valueOf(value).trim();
        String[] patterns = {
            "dd/MM/yyyy",
            "d/M/yyyy",
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "MM/yyyy",
            "M/yyyy"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                sdf.setLenient(false);
                return sdf.parse(raw);
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String normalizeMonthText(Object value) {
        if (value == null) {
            return "Không rõ";
        }

        String raw = String.valueOf(value).trim();
        if (raw.matches("\\d{1,2}/\\d{4}")) {
            String[] parts = raw.split("/");
            int month = Integer.parseInt(parts[0]);
            return String.format("%02d/%s", month, parts[1]);
        }

        return raw;
    }

    private String buildFallbackMonthSortKey(String monthLabel) {
        try {
            if (monthLabel.matches("\\d{2}/\\d{4}")) {
                String[] parts = monthLabel.split("/");
                return parts[1] + "-" + parts[0];
            }
        } catch (Exception ignored) {
        }

        return monthLabel;
    }

    // =========================================================
    // KPI DATA TRANSFORM
    // =========================================================
    private List<StaffKpi> buildStaffKpis(List<Object[]> employeeRows) {
        List<StaffKpi> result = new ArrayList<>();
        for (Object[] row : employeeRows) {
            String name = row.length > 1 ? String.valueOf(row[1]) : "Nhân viên";
            int completed = row.length > 2 ? (int) Math.round(toDouble(row[2])) : 0;
            int cancelled = row.length > 3 ? (int) Math.round(toDouble(row[3])) : 0;
            double revenue = row.length > 4 ? toDouble(row[4]) : 0;

            int score = calculateKpiScore(completed, cancelled, revenue);
            String status = score >= 80 ? "Tốt" : score >= 60 ? "Đạt" : "Cần cải thiện";
            String trend = score >= 80 ? "Tăng" : score >= 60 ? "Ổn định" : "Giảm";
            result.add(new StaffKpi(name, score, revenue, completed, status, trend));
        }
        result.sort((a, b) -> Integer.compare(b.score, a.score));
        return result;
    }

    private int calculateKpiScore(int completed, int cancelled, double revenue) {
        int total = completed + cancelled;
        double completionScore = total == 0 ? 0 : ((double) completed / total) * 70;
        double revenueScore = Math.min(30, revenue / 5_000_000d * 30);
        return (int) Math.max(0, Math.min(100, Math.round(completionScore + revenueScore)));
    }

    // =========================================================
    // EXPORT / PRINT
    // =========================================================
    private void exportActiveTableAsCsv(ActionEvent e) {
        JTable table = getActiveTable();
        if (table == null) {
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Xuất dữ liệu Excel/CSV");
        chooser.setSelectedFile(new File("bao_cao_thong_ke.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(chooser.getSelectedFile(), false))) {
            for (int c = 0; c < table.getColumnCount(); c++) {
                out.print(escapeCsv(table.getColumnName(c)));
                if (c < table.getColumnCount() - 1) {
                    out.print(",");
                }
            }
            out.println();

            for (int r = 0; r < table.getRowCount(); r++) {
                for (int c = 0; c < table.getColumnCount(); c++) {
                    out.print(escapeCsv(String.valueOf(table.getValueAt(r, c))));
                    if (c < table.getColumnCount() - 1) {
                        out.print(",");
                    }
                }
                out.println();
            }
            JOptionPane.showMessageDialog(this, "✅ Đã xuất file: " + chooser.getSelectedFile().getAbsolutePath());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi xuất Excel/CSV: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void exportRevenueReport() {
        try {
            Date startDate = getSelectedStartDate();
            Date endDate = getSelectedEndDate();
            Date endDatePlusOne = addDays(endDate, 1);
            RevenueReportType reportType = getSelectedReportType();

            String scopedStoreId = statisticService.getCurrentReportStoreId();
            boolean scoped = scopedStoreId != null && !scopedStoreId.trim().isEmpty();

            HashMap<String, Object> params = new HashMap<>();

            params.put("START_DATE", new java.sql.Date(startDate.getTime()));
            params.put("END_DATE", new java.sql.Date(endDate.getTime()));
            params.put("END_DATE_PLUS_ONE", new java.sql.Date(endDatePlusOne.getTime()));
            params.put("REPORT_TYPE", reportType.queryValue);
            params.put("REPORT_TYPE_LABEL", reportType.label);
            params.put("PERIOD_HEADER", reportType.periodHeader);

            // Thêm scope chi nhánh cho Jasper.
            // Admin: STORE_ID = null => báo cáo toàn hệ thống.
            // Manager/Staff: STORE_ID = currentStoreId => báo cáo chi nhánh hiện tại.
            params.put("STORE_ID", scoped ? scopedStoreId.trim() : null);
            params.put("STORE_NAME", statisticService.getCurrentReportStoreName());
            params.put("IS_ADMIN_REPORT", !scoped);

            ReportViewer.showReport("src/main/resources/reports/RevenueReport.jrxml", params);

        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Khoảng ngày không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi xuất báo cáo doanh thu: " + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private Date getSelectedStartDate() {
        Date selected = dpFromDate.getDate();
        if (selected == null) {
            throw new IllegalArgumentException("Vui lòng chọn Từ ngày trước khi xuất báo cáo.");
        }
        return atStartOfDay(selected);
    }

    private Date getSelectedEndDate() {
        Date selected = dpToDate.getDate();
        if (selected == null) {
            throw new IllegalArgumentException("Vui lòng chọn Đến ngày trước khi xuất báo cáo.");
        }

        Date startDate = getSelectedStartDate();
        Date endDate = atStartOfDay(selected);
        if (startDate.after(endDate)) {
            throw new IllegalArgumentException("Ngày bắt đầu không thể sau ngày kết thúc.");
        }
        return endDate;
    }

    private RevenueReportType getSelectedReportType() {
        Object selected = cboRevenueReportType.getSelectedItem();
        return selected instanceof RevenueReportType ? (RevenueReportType) selected : RevenueReportType.DAY;
    }

    private Date atStartOfDay(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    private Date addDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days);
        return cal.getTime();
    }

    private JTable getActiveTable() {
        int index = detailTabs != null ? detailTabs.getSelectedIndex() : 0;
        if (index == 1) {
            return tblRevenue;
        }
        if (index == 2) {
            return tblProducts;
        }
        return tblKpi;
    }

    private String escapeCsv(String s) {
        if (s == null) {
            return "";
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    // =========================================================
    // UI HELPERS
    // =========================================================
    private JPanel createTitledCard(String title, String subtitle, ImageIcon icon) {
        RoundedCardPanel card = new RoundedCardPanel(18, cardWhite, true);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(14, 16, 16, 16));
        card.add(createSectionHeader(title, subtitle, icon), BorderLayout.NORTH);
        return card;
    }

    private JPanel createSectionHeader(String title, String subtitle, ImageIcon icon) {
        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);

        JLabel iconLabel = new JLabel(icon);
        iconLabel.setPreferredSize(new Dimension(28, 28));

        JPanel textWrap = new JPanel();
        textWrap.setOpaque(false);
        textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(sectionFont);
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(textGray);
        textWrap.add(lblTitle);
        textWrap.add(Box.createVerticalStrut(2));
        textWrap.add(lblSub);

        header.add(iconLabel, BorderLayout.WEST);
        header.add(textWrap, BorderLayout.CENTER);
        return header;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(boldFont);
        lbl.setForeground(textDark);
        return lbl;
    }

    private JLabel createInsightLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(textDark);
        return lbl;
    }

    private JButton createActionButton(String text, Color bg, Color fg, ImageIcon icon, int width) {
        JButton btn = new RoundedButton(text, bg, fg);
        if (icon != null) {
            btn.setIcon(icon);
            btn.setIconTextGap(8);
        }
        btn.setFont(boldFont);
        btn.setPreferredSize(new Dimension(width, 38));
        return btn;
    }

    private JTable createPowerTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table.setRowHeight(42);
        table.setFont(normalFont);
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(textDark);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 42));
        header.setFont(boldFont);
        header.setBackground(primaryBlueDark);
        header.setForeground(Color.WHITE);
        header.setReorderingAllowed(false);

        table.setDefaultRenderer(Object.class, new ZebraRenderer());
        return table;
    }

    private JScrollPane wrapTable(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(borderLight));
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private String formatCurrency(double value) {
        return String.format("%,.0f đ", value).replace(',', '.');
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString().replace("đ", "").replace(".", "").replace(",", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Double> initMonthBuckets(Date fromDate, Date toDate) {
        Map<String, Double> result = new LinkedHashMap<>();

        if (fromDate == null || toDate == null) {
            return result;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(fromDate);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        Calendar end = Calendar.getInstance();
        end.setTime(toDate);
        end.set(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat fmt = new SimpleDateFormat("MM/yyyy");

        while (!cal.after(end)) {
            result.put(fmt.format(cal.getTime()), 0.0);
            cal.add(Calendar.MONTH, 1);
        }

        return result;
    }

    private String getMonthKey(Object dateValue) {
        if (dateValue == null) {
            return "Không rõ";
        }

        if (dateValue instanceof Date) {
            return new SimpleDateFormat("MM/yyyy").format((Date) dateValue);
        }

        String raw = dateValue.toString().trim();

        String[] patterns = {
            "yyyy-MM-dd",
            "yyyy-MM-dd HH:mm:ss",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "MM/yyyy"
        };

        for (String pattern : patterns) {
            try {
                Date parsed = new SimpleDateFormat(pattern).parse(raw);
                return new SimpleDateFormat("MM/yyyy").format(parsed);
            } catch (Exception ignored) {
            }
        }

        if (raw.length() >= 7 && raw.charAt(4) == '-') {
            return raw.substring(5, 7) + "/" + raw.substring(0, 4);
        }

        return raw;
    }

    // =========================================================
    // RENDERERS
    // =========================================================
    class ZebraRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setForeground(textDark);
            setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252)));
            return c;
        }
    }

    class CenterRenderer extends ZebraRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            return c;
        }
    }

    class RightRenderer extends ZebraRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.RIGHT);
            return c;
        }
    }

    class KpiScoreRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            int score = (int) Math.round(toDouble(value));
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, score + "/100", isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(boldFont);
            label.setOpaque(true);
            if (score >= 80) {
                label.setForeground(successGreen);
                label.setBackground(new Color(220, 252, 231));
            } else if (score >= 60) {
                label.setForeground(new Color(146, 64, 14));
                label.setBackground(new Color(254, 243, 199));
            } else {
                label.setForeground(dangerRed);
                label.setBackground(new Color(254, 226, 226));
            }
            if (isSelected) {
                label.setBackground(new Color(219, 234, 254));
            }
            return label;
        }
    }

    class StatusRenderer extends KpiBadgeRenderer {

        @Override
        protected Color getBadgeBg(String value) {
            if (value.contains("Tốt")) {
                return new Color(220, 252, 231);
            }
            if (value.contains("Đạt")) {
                return new Color(254, 243, 199);
            }
            return new Color(254, 226, 226);
        }

        @Override
        protected Color getBadgeFg(String value) {
            if (value.contains("Tốt")) {
                return successGreen;
            }
            if (value.contains("Đạt")) {
                return new Color(146, 64, 14);
            }
            return dangerRed;
        }
    }

    class TrendRenderer extends KpiBadgeRenderer {

        @Override
        protected Color getBadgeBg(String value) {
            if (value.contains("Tăng")) {
                return new Color(220, 252, 231);
            }
            if (value.contains("Ổn")) {
                return new Color(219, 234, 254);
            }
            return new Color(254, 226, 226);
        }

        @Override
        protected Color getBadgeFg(String value) {
            if (value.contains("Tăng")) {
                return successGreen;
            }
            if (value.contains("Ổn")) {
                return primaryBlue;
            }
            return dangerRed;
        }
    }

    abstract class KpiBadgeRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String text = value == null ? "" : value.toString();
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(boldFont);
            label.setForeground(getBadgeFg(text));
            label.setBackground(isSelected ? new Color(219, 234, 254) : getBadgeBg(text));
            label.setOpaque(true);
            label.setBorder(new EmptyBorder(6, 10, 6, 10));
            return label;
        }

        protected abstract Color getBadgeBg(String value);

        protected abstract Color getBadgeFg(String value);
    }

    // =========================================================
    // CUSTOM COMPONENTS
    // =========================================================
    class RoundedCardPanel extends JPanel {

        private final int radius;
        private final Color bg;
        private final boolean shadow;

        RoundedCardPanel(int radius, Color bg, boolean shadow) {
            this.radius = radius;
            this.bg = bg;
            this.shadow = shadow;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (shadow) {
                g2.setColor(new Color(15, 23, 42, 18));
                g2.fillRoundRect(3, 4, getWidth() - 6, getHeight() - 6, radius, radius);
            }
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth() - 4, getHeight() - 4, radius, radius);
            g2.setColor(borderLight);
            g2.drawRoundRect(0, 0, getWidth() - 5, getHeight() - 5, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class RoundedButton extends JButton {

        private final Color bg;

        RoundedButton(String text, Color bg, Color fg) {
            super(text);
            this.bg = bg;
            setForeground(fg);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = !isEnabled() ? new Color(148, 163, 184) : (getModel().isPressed() ? bg.darker() : (getModel().isRollover() ? bg.brighter() : bg));
            g2.setColor(fill);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class SummaryCard extends RoundedCardPanel {

        JLabel valueLabel;
        JLabel deltaLabel;

        SummaryCard(String title, String value, String delta, ImageIcon icon, Color accent) {
            super(18, cardWhite, true);
            setLayout(new BorderLayout(12, 0));
            setBorder(new EmptyBorder(16, 16, 16, 16));

            JLabel iconLabel = new JLabel(icon);
            iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
            iconLabel.setOpaque(true);
            iconLabel.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 28));
            iconLabel.setPreferredSize(new Dimension(48, 48));

            JPanel textPanel = new JPanel();
            textPanel.setOpaque(false);
            textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

            JLabel titleLabel = new JLabel(title);
            titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
            titleLabel.setForeground(textGray);

            valueLabel = new JLabel(value);
            valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
            valueLabel.setForeground(textDark);

            deltaLabel = new JLabel(delta);
            deltaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            deltaLabel.setForeground(accent);

            textPanel.add(titleLabel);
            textPanel.add(Box.createVerticalStrut(4));
            textPanel.add(valueLabel);
            textPanel.add(Box.createVerticalStrut(3));
            textPanel.add(deltaLabel);

            add(iconLabel, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);
        }
    }

    class MiniLineChartPanel extends JPanel {

        private List<Double> points = new ArrayList<>();
        private List<String> labels = new ArrayList<>();

        MiniLineChartPanel() {
            setOpaque(false);
            setBorder(new EmptyBorder(10, 10, 10, 10));
        }

        void setData(List<Double> points, List<String> labels) {
            this.points = points != null ? points : new ArrayList<>();
            this.labels = labels != null ? labels : new ArrayList<>();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            int left = 65;
            int right = 35;
            int top = 24;
            int bottom = 54;

            int chartW = Math.max(1, w - left - right);
            int chartH = Math.max(1, h - top - bottom);

            // Grid ngang
            g2.setColor(new Color(241, 245, 249));
            for (int i = 0; i <= 4; i++) {
                int y = top + i * chartH / 4;
                g2.drawLine(left, y, left + chartW, y);
            }

            if (points.isEmpty()) {
                g2.setColor(textGray);
                g2.setFont(boldFont);

                String msg = "Chưa có dữ liệu doanh thu trong khoảng lọc";
                int msgX = left + chartW / 2 - g2.getFontMetrics().stringWidth(msg) / 2;
                int msgY = top + chartH / 2;

                g2.drawString(msg, msgX, msgY);
                g2.dispose();
                return;
            }

            double max = 0;
            for (double p : points) {
                max = Math.max(max, p);
            }

            if (max <= 0) {
                max = 1;
            }

            Path2D path = new Path2D.Double();

            for (int i = 0; i < points.size(); i++) {
                double x = left + (points.size() == 1
                        ? chartW / 2.0
                        : i * chartW / (double) (points.size() - 1));

                double y = top + chartH - (points.get(i) / max) * chartH;

                if (i == 0) {
                    path.moveTo(x, y);
                } else {
                    path.lineTo(x, y);
                }
            }

            // Fill nhẹ dưới line cho giống dashboard
            Path2D area = new Path2D.Double(path);
            double lastX = left + (points.size() == 1
                    ? chartW / 2.0
                    : (points.size() - 1) * chartW / (double) (points.size() - 1));
            double firstX = left + (points.size() == 1 ? chartW / 2.0 : 0);

            area.lineTo(lastX, top + chartH);
            area.lineTo(firstX, top + chartH);
            area.closePath();

            g2.setColor(new Color(primaryBlue.getRed(), primaryBlue.getGreen(), primaryBlue.getBlue(), 28));
            g2.fill(area);

            // Vẽ line
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(primaryBlue);
            g2.draw(path);

            // Vẽ điểm
            g2.setStroke(new BasicStroke(1.4f));
            for (int i = 0; i < points.size(); i++) {
                int x = (int) (left + (points.size() == 1
                        ? chartW / 2.0
                        : i * chartW / (double) (points.size() - 1)));

                int y = (int) (top + chartH - (points.get(i) / max) * chartH);

                g2.setColor(Color.WHITE);
                g2.fillOval(x - 5, y - 5, 10, 10);

                g2.setColor(primaryBlue);
                g2.drawOval(x - 5, y - 5, 10, 10);
            }

            // Label trục Y
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(textGray);
            g2.drawString("0", 12, top + chartH + 4);
            g2.drawString(formatShortCurrency(max), 12, top + 4);

            // Label trục X: vẽ theo tháng, không cắt cụt 05/2026 thành 05/20
            drawMonthLabels(g2, left, chartW, h);

            g2.dispose();
        }

        private void drawMonthLabels(Graphics2D g2, int left, int chartW, int h) {
            if (labels.isEmpty()) {
                return;
            }

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(textGray);

            FontMetrics fm = g2.getFontMetrics();
            int n = labels.size();

            if (n == 1) {
                String label = shortenMonthLabel(String.valueOf(labels.get(0)));
                int x = left + chartW / 2 - fm.stringWidth(label) / 2;
                g2.drawString(label, x, h - 16);
                return;
            }

            // Nếu nhiều tháng quá thì giảm số label để không bị đè chữ
            int step = 1;
            if (n > 8) {
                step = 2;
            }
            if (n > 14) {
                step = 3;
            }

            for (int i = 0; i < n; i++) {
                boolean shouldDraw = i == 0 || i == n - 1 || i % step == 0;
                if (!shouldDraw) {
                    continue;
                }

                String label = shortenMonthLabel(String.valueOf(labels.get(i)));

                int x = (int) (left + i * chartW / (double) (n - 1));
                int textW = fm.stringWidth(label);

                if (i == 0) {
                    g2.drawString(label, x, h - 16);
                } else if (i == n - 1) {
                    g2.drawString(label, x - textW, h - 16);
                } else {
                    g2.drawString(label, x - textW / 2, h - 16);
                }
            }
        }

        private String shortenMonthLabel(String label) {
            if (label == null) {
                return "";
            }

            label = label.trim();

            // 05/2026 -> 05/26
            if (label.length() == 7 && label.contains("/")) {
                return label.substring(0, 3) + label.substring(5);
            }

            return label;
        }

        private String formatShortCurrency(double value) {
            if (value >= 1_000_000_000) {
                return String.format("%.1f tỷ", value / 1_000_000_000d);
            }

            if (value >= 1_000_000) {
                return String.format("%.1f tr", value / 1_000_000d);
            }

            if (value >= 1_000) {
                return String.format("%.0f k", value / 1_000d);
            }

            return String.format("%.0f", value);
        }
    }

    class TopStaffPanel extends JPanel {

        private List<StaffKpi> staff = new ArrayList<>();

        TopStaffPanel() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(6, 0, 0, 0));
        }

        void setStaff(List<StaffKpi> staff) {
            this.staff = staff != null ? staff : new ArrayList<>();
            rebuild();
        }

        private void rebuild() {
            removeAll();
            if (staff.isEmpty()) {
                add(Box.createVerticalGlue());
                JLabel empty = new JLabel("Chưa có dữ liệu nhân viên");
                empty.setAlignmentX(Component.CENTER_ALIGNMENT);
                empty.setForeground(textGray);
                empty.setFont(boldFont);
                add(empty);
                add(Box.createVerticalGlue());
            } else {
                int limit = Math.min(5, staff.size());
                for (int i = 0; i < limit; i++) {
                    add(createStaffRow(i + 1, staff.get(i)));
                    add(Box.createVerticalStrut(10));
                }
            }
            revalidate();
            repaint();
        }

        private JPanel createStaffRow(int rank, StaffKpi kpi) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

            JLabel medal = new JLabel(rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : String.valueOf(rank));
            medal.setPreferredSize(new Dimension(30, 38));
            medal.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            medal.setHorizontalAlignment(SwingConstants.CENTER);

            JPanel center = new JPanel();
            center.setOpaque(false);
            center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
            JLabel name = new JLabel(kpi.name);
            name.setFont(boldFont);
            name.setForeground(textDark);
            JProgressBar bar = new JProgressBar(0, 100);
            bar.setValue(kpi.score);
            bar.setStringPainted(true);
            bar.setString(kpi.score + "/100");
            bar.setForeground(kpi.score >= 80 ? successGreen : kpi.score >= 60 ? warningYellow : dangerRed);
            bar.setBackground(new Color(226, 232, 240));
            bar.setPreferredSize(new Dimension(180, 16));
            center.add(name);
            center.add(Box.createVerticalStrut(5));
            center.add(bar);

            row.add(medal, BorderLayout.WEST);
            row.add(center, BorderLayout.CENTER);
            return row;
        }
    }

    // =========================================================
    // DATA HOLDERS
    // =========================================================
    static class ReportData {

        List<Object[]> revenueRows = new ArrayList<>();
        List<Object[]> chartRevenueRows = new ArrayList<>(); // Dữ liệu riêng cho chart theo tháng
        List<Object[]> monthlyRevenueRows = new ArrayList<>();
        List<Object[]> productRows = new ArrayList<>();
        List<Object[]> employeeRows = new ArrayList<>();
        List<StaffKpi> staffKpis = new ArrayList<>();
    }

    static class StaffKpi {

        String name;
        int score;
        double revenue;
        int completedOrders;
        String status;
        String trend;

        StaffKpi(String name, int score, double revenue, int completedOrders, String status, String trend) {
            this.name = name;
            this.score = score;
            this.revenue = revenue;
            this.completedOrders = completedOrders;
            this.status = status;
            this.trend = trend;
        }
    }

    enum RevenueReportType {
        DAY("Theo ngày", "DAY", "Ngày"),
        MONTH("Theo tháng", "MONTH", "Tháng"),
        YEAR("Theo năm", "YEAR", "Năm"),
        RANGE("Khoảng thời gian", "RANGE", "Kỳ báo cáo");

        private final String label;
        private final String queryValue;
        private final String periodHeader;

        RevenueReportType(String label, String queryValue, String periodHeader) {
            this.label = label;
            this.queryValue = queryValue;
            this.periodHeader = periodHeader;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
