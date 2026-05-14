/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package view;

import business.sql.hr_kpi.EmployeeSql;
import business.sql.hr_kpi.KpiEvaluationSql;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.account.kpi.KpiEvaluation;
import model.employee.Employee;

/**
 * Panel hiển thị hiệu suất nhân viên với chi tiết KPI
 * @author Admin
 */
public class EmployeePerformancePanel extends javax.swing.JPanel {

    private DefaultTableModel employeeTableModel;
    private DefaultTableModel kpiHistoryTableModel;
    private final EmployeeSql employeeSql = new EmployeeSql();
    private final KpiEvaluationSql kpiEvaluationSql = KpiEvaluationSql.getInstance();
    private String selectedEmployeeId = null;

    /**
     * Creates new form EmployeePerformancePanel
     */
    public EmployeePerformancePanel() {
        initComponents();
        initEmployeeTable();
        initKpiHistoryTable();
        initEvents();
        loadEmployeesToTable();
    }

    /**
     * Khởi tạo bảng danh sách nhân viên
     */
    private void initEmployeeTable() {
        employeeTableModel = new DefaultTableModel(
                new Object[]{"Mã NV", "Tên Nhân Viên", "Chức Vụ", "Email"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblEmployees.setModel(employeeTableModel);
        tblEmployees.setRowHeight(45);
        tblEmployees.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblEmployees.getTableHeader().setReorderingAllowed(false);
        
        // Style cho header
        tblEmployees.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblEmployees.getTableHeader().setBackground(new Color(52, 152, 219));
        tblEmployees.getTableHeader().setForeground(Color.WHITE);
        
        // Style cho cells
        tblEmployees.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblEmployees.setGridColor(new Color(236, 240, 241));
        tblEmployees.setShowVerticalLines(false);
        tblEmployees.setShowHorizontalLines(true);
        
        // Căn giữa các cột
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblEmployees.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblEmployees.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
    }

    /**
     * Khởi tạo bảng lịch sử KPI với phong cách hiện đại
     */
    private void initKpiHistoryTable() {
        kpiHistoryTableModel = new DefaultTableModel(
                new Object[]{"Tháng Đánh Giá", "Mục Tiêu", "Thực Tế", "Điểm KPI", "Trạng Thái", "Ghi Chú"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblKpiHistory.setModel(kpiHistoryTableModel);
        tblKpiHistory.setRowHeight(40);
        tblKpiHistory.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblKpiHistory.getTableHeader().setReorderingAllowed(false);
        
        // Style hiện đại cho header
        tblKpiHistory.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblKpiHistory.getTableHeader().setBackground(new Color(41, 128, 185));
        tblKpiHistory.getTableHeader().setForeground(Color.WHITE);
        tblKpiHistory.getTableHeader().setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        
        // Style cho cells
        tblKpiHistory.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblKpiHistory.setGridColor(new Color(236, 240, 241));
        tblKpiHistory.setShowVerticalLines(false);
        tblKpiHistory.setShowHorizontalLines(true);
        
        // Căn giữa các cột số
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tblKpiHistory.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblKpiHistory.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
        tblKpiHistory.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        
        // Đặt độ rộng cột
        tblKpiHistory.getColumnModel().getColumn(0).setPreferredWidth(100);
        tblKpiHistory.getColumnModel().getColumn(1).setPreferredWidth(80);
        tblKpiHistory.getColumnModel().getColumn(2).setPreferredWidth(80);
        tblKpiHistory.getColumnModel().getColumn(3).setPreferredWidth(70);
        tblKpiHistory.getColumnModel().getColumn(4).setPreferredWidth(90);
        tblKpiHistory.getColumnModel().getColumn(5).setPreferredWidth(200);
    }

    /**
     * Khởi tạo sự kiện
     */
    private void initEvents() {
        // Sự kiện chọn nhân viên từ bảng
        tblEmployees.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = tblEmployees.getSelectedRow();
                    if (selectedRow >= 0) {
                        selectedEmployeeId = employeeTableModel.getValueAt(selectedRow, 0).toString();
                        String employeeName = employeeTableModel.getValueAt(selectedRow, 1).toString();
                        
                        // Cập nhật tiêu đề
                        lblSelectedEmployee.setText("📊 Chi tiết KPI: " + employeeName);
                        
                        // Load dữ liệu KPI trong background thread
                        loadKpiHistoryAsync(selectedEmployeeId);
                    }
                }
            }
        });
    }

    /**
     * Tải danh sách nhân viên vào bảng
     */
    private void loadEmployeesToTable() {
        employeeTableModel.setRowCount(0);
        ArrayList<Employee> list = employeeSql.selectAll();
        for (Employee e : list) {
            employeeTableModel.addRow(new Object[]{
                e.getEmployeeId(),
                e.getEmployeeName(),
                e.getRole(),
                e.getEmail()
            });
        }
    }

    /**
     * Tải lịch sử KPI của nhân viên (bất đồng bộ)
     */
    private void loadKpiHistoryAsync(String employeeId) {
        Thread.ofVirtual().start(() -> {
            ArrayList<KpiEvaluation> kpiList = kpiEvaluationSql.findByEmployeeId(employeeId);
            
            // Cập nhật UI trên EDT
            javax.swing.SwingUtilities.invokeLater(() -> {
                kpiHistoryTableModel.setRowCount(0);
                
                if (kpiList.isEmpty()) {
                    kpiHistoryTableModel.addRow(new Object[]{
                        "Không có dữ liệu", "-", "-", "-", "N/A", "Chưa có đánh giá KPI"
                    });
                    // Xóa biểu đồ cũ và thêm label thông báo
                    pnChart.removeAll();
                    javax.swing.JLabel lblNoData = new javax.swing.JLabel(
                        "<html><div style='text-align: center; padding: 50px;'>" +
                        "<h3>📭 Chưa có dữ liệu KPI</h3>" +
                        "<p>Nhân viên này chưa được đánh giá KPI trong hệ thống.</p>" +
                        "</div></html>",
                        SwingConstants.CENTER
                    );
                    lblNoData.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    pnChart.add(lblNoData);
                    pnChart.revalidate();
                    pnChart.repaint();
                    return;
                }
                
                // Thêm dữ liệu vào bảng
                for (KpiEvaluation kpi : kpiList) {
                    String status = determineStatus(kpi.getAchievedScore());
                    Color statusColor = getStatusColor(status);
                    
                    kpiHistoryTableModel.addRow(new Object[]{
                        kpi.getEvaluationPeriod(),
                        formatNumber(kpi.getActualValue()),
                        formatNumber(kpi.getActualValue()),
                        String.format("%.1f", kpi.getAchievedScore()),
                        createStatusLabel(status, statusColor),
                        kpi.getManagerNote() != null ? kpi.getManagerNote() : "-"
                    });
                }
                
                // Vẽ biểu đồ
                drawKpiChart(kpiList);
            });
        });
    }

    /**
     * Xác định trạng thái dựa trên điểm KPI
     */
    private String determineStatus(double score) {
        if (score >= 90) return "Xuất Sắc";
        if (score >= 80) return "Tốt";
        if (score >= 70) return "Đạt";
        if (score >= 60) return "Cần Cải Thiện";
        return "Không Đạt";
    }

    /**
     * Lấy màu cho trạng thái
     */
    private Color getStatusColor(String status) {
        return switch (status) {
            case "Xuất Sắc" -> new Color(39, 174, 96);
            case "Tốt" -> new Color(46, 204, 113);
            case "Đạt" -> new Color(241, 196, 15);
            case "Cần Cải Thiện" -> new Color(230, 126, 34);
            default -> new Color(231, 76, 60);
        };
    }

    /**
     * Tạo label hiển thị trạng thái với màu nền
     */
    private javax.swing.JLabel createStatusLabel(String text, Color bgColor) {
        javax.swing.JLabel label = new javax.swing.JLabel(text, SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(bgColor);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        return label;
    }

    /**
     * Định dạng số
     */
    private String formatNumber(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }

    /**
     * Vẽ biểu đồ KPI sử dụng JFreeChart (Option B)
     */
    private void drawKpiChart(ArrayList<KpiEvaluation> kpiList) {
        try {
            // Đảo ngược danh sách để hiển thị theo thứ tự thời gian tăng dần
            ArrayList<KpiEvaluation> reversedList = new ArrayList<>(kpiList);
            java.util.Collections.reverse(reversedList);
            
            // Tạo dataset
            org.jfree.data.category.DefaultDataset dataset = createDataset(reversedList);
            
            // Tạo biểu đồ line chart
            org.jfree.chart.JFreeChart chart = org.jfree.chart.ChartFactory.createLineChart(
                "📈 Biểu Đồ Điểm KPI Theo Tháng",
                "Tháng Đánh Giá",
                "Điểm KPI",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                true,
                true,
                false
            );
            
            // Customize chart style
            chart.setBackgroundPaint(Color.WHITE);
            chart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 14));
            chart.getTitle().setPaint(new Color(52, 73, 94));
            
            org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
            plot.setBackgroundPaint(Color.WHITE);
            plot.setRangeGridlinePaint(new Color(236, 240, 241));
            plot.setDomainGridlinePaint(new Color(236, 240, 241));
            
            // Customize line color
            org.jfree.chart.renderer.category.LineAndShapeRenderer renderer = 
                (org.jfree.chart.renderer.category.LineAndShapeRenderer) plot.getRenderer();
            renderer.setSeriesPaint(0, new Color(52, 152, 219));
            renderer.setSeriesStroke(0, new java.awt.BasicStroke(3.0f));
            renderer.setSeriesShapesVisible(0, true);
            renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
            
            // Customize axis fonts
            plot.getDomainAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
            plot.getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
            plot.getRangeAxis().setLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
            plot.getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 11));
            plot.getRangeAxis().setRange(0, 100);
            
            // Tạo ChartPanel và hiển thị
            org.jfree.chart.ChartPanel chartPanel = new org.jfree.chart.ChartPanel(chart);
            chartPanel.setPreferredSize(new java.awt.Dimension(pnChart.getWidth(), pnChart.getHeight()));
            chartPanel.setMinimumSize(new java.awt.Dimension(300, 200));
            chartPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            
            // Xóa nội dung cũ và thêm chart mới
            pnChart.removeAll();
            pnChart.setLayout(new java.awt.BorderLayout());
            pnChart.add(chartPanel, BorderLayout.CENTER);
            pnChart.revalidate();
            pnChart.repaint();
            
        } catch (Exception ex) {
            ex.printStackTrace();
            pnChart.removeAll();
            javax.swing.JLabel lblError = new javax.swing.JLabel(
                "<html><div style='text-align: center; padding: 20px; color: red;'>" +
                "⚠️ Không thể hiển thị biểu đồ: " + ex.getMessage() +
                "</div></html>",
                SwingConstants.CENTER
            );
            pnChart.add(lblError);
            pnChart.revalidate();
            pnChart.repaint();
        }
    }

    /**
     * Tạo dataset cho biểu đồ
     */
    private org.jfree.data.category.DefaultDataset createDataset(ArrayList<KpiEvaluation> kpiList) {
        org.jfree.data.category.DefaultDataset dataset = new org.jfree.data.category.DefaultDataset();
        
        for (KpiEvaluation kpi : kpiList) {
            dataset.addValue(kpi.getAchievedScore(), "Điểm KPI", kpi.getEvaluationPeriod());
        }
        
        return dataset;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnMain = new javax.swing.JPanel();
        pnLeft = new javax.swing.JPanel();
        pnTitle = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblEmployees = new javax.swing.JTable();
        pnRight = new javax.swing.JPanel();
        pnSelectedEmployee = new javax.swing.JPanel();
        lblSelectedEmployee = new javax.swing.JLabel();
        jSplitPane1 = new javax.swing.JSplitPane();
        pnKpiTable = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblKpiHistory = new javax.swing.JTable();
        pnChart = new javax.swing.JPanel();

        setBackground(new java.awt.Color(236, 240, 241));
        setLayout(new java.awt.BorderLayout());

        pnMain.setBackground(new java.awt.Color(236, 240, 241));
        pnMain.setLayout(new java.awt.BorderLayout());

        pnLeft.setBackground(new java.awt.Color(255, 255, 255));
        pnLeft.setPreferredSize(new java.awt.Dimension(350, 0));
        pnLeft.setLayout(new java.awt.BorderLayout());

        pnTitle.setBackground(new java.awt.Color(52, 152, 219));
        pnTitle.setPreferredSize(new java.awt.Dimension(350, 60));
        pnTitle.setLayout(new java.awt.BorderLayout());

        lblTitle.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblTitle.setForeground(new java.awt.Color(255, 255, 255));
        lblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitle.setIcon(new javax.swing.ImageIcon(getClass().getResource("/view/image/team-Photoroom.png"))); // NOI18N
        lblTitle.setText("DANH SÁCH NHÂN VIÊN");
        lblTitle.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        pnTitle.add(lblTitle, java.awt.BorderLayout.CENTER);

        pnLeft.add(pnTitle, java.awt.BorderLayout.NORTH);

        tblEmployees.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        jScrollPane1.setViewportView(tblEmployees);

        pnLeft.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        pnMain.add(pnLeft, java.awt.BorderLayout.LINE_START);

        pnRight.setBackground(new java.awt.Color(236, 240, 241));
        pnRight.setLayout(new java.awt.BorderLayout());

        pnSelectedEmployee.setBackground(new java.awt.Color(255, 255, 255));
        pnSelectedEmployee.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(new java.awt.Color(52, 152, 219), 2),
            javax.swing.BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        pnSelectedEmployee.setPreferredSize(new java.awt.Dimension(0, 60));

        lblSelectedEmployee.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        lblSelectedEmployee.setForeground(new java.awt.Color(52, 73, 94));
        lblSelectedEmployee.setText("📊 Chi tiết KPI: Chọn nhân viên để xem");
        pnSelectedEmployee.add(lblSelectedEmployee);

        pnRight.add(pnSelectedEmployee, java.awt.BorderLayout.NORTH);

        jSplitPane1.setDividerLocation(300);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        pnKpiTable.setBackground(new java.awt.Color(255, 255, 255));
        pnKpiTable.setBorder(javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createLineBorder(new java.awt.Color(52, 152, 219)),
            "📋 Lịch Sử Đánh Giá KPI",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new java.awt.Font("Segoe UI", 1, 13),
            new java.awt.Color(52, 73, 94)
        ));
        pnKpiTable.setLayout(new java.awt.BorderLayout());

        tblKpiHistory.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        jScrollPane2.setViewportView(tblKpiHistory);

        pnKpiTable.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jSplitPane1.setTopComponent(pnKpiTable);

        pnChart.setBackground(new java.awt.Color(255, 255, 255));
        pnChart.setBorder(javax.swing.BorderFactory.createTitledBorder(
            javax.swing.BorderFactory.createLineBorder(new java.awt.Color(52, 152, 219)),
            "📈 Biểu Đồ Phân Tích KPI",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new java.awt.Font("Segoe UI", 1, 13),
            new java.awt.Color(52, 73, 94)
        ));
        pnChart.setLayout(new java.awt.BorderLayout());
        pnChart.setPreferredSize(new java.awt.Dimension(0, 250));
        jSplitPane1.setBottomComponent(pnChart);

        pnRight.add(jSplitPane1, java.awt.BorderLayout.CENTER);

        pnMain.add(pnRight, java.awt.BorderLayout.CENTER);

        add(pnMain, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel lblSelectedEmployee;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JPanel pnChart;
    private javax.swing.JPanel pnKpiTable;
    private javax.swing.JPanel pnLeft;
    private javax.swing.JPanel pnMain;
    private javax.swing.JPanel pnRight;
    private javax.swing.JPanel pnSelectedEmployee;
    private javax.swing.JPanel pnTitle;
    private javax.swing.JTable tblEmployees;
    private javax.swing.JTable tblKpiHistory;
    // End of variables declaration//GEN-END:variables
}
