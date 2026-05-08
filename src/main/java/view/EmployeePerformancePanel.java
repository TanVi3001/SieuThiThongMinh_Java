package view;

import business.EmployeePerformanceService;
import model.employee.EmployeePerformance;
import common.utils.FormatUtils; // Dùng để format tiền tệ

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class EmployeePerformancePanel extends JPanel {

    private JTable kpiTable;
    private DefaultTableModel tableModel;
    private EmployeePerformanceService kpiService;

    // Các thẻ Card
    private JLabel lblTopSaleName, lblTopSaleRevenue;

    public EmployeePerformancePanel() {
        kpiService = new EmployeePerformanceService();
        initUI();
        loadData();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));

        // 1. Tạo phần Card thống kê (Top)
        JPanel pnlCards = new JPanel(new GridLayout(1, 3, 10, 10));
        pnlCards.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Card Top Sale
        JPanel cardTopSale = createCard("TOP SALE", new Color(41, 128, 185));
        lblTopSaleName = new JLabel("Đang tải...");
        lblTopSaleRevenue = new JLabel("");
        cardTopSale.add(lblTopSaleName);
        cardTopSale.add(lblTopSaleRevenue);

        // Thêm các card vào panel
        pnlCards.add(cardTopSale);
        pnlCards.add(createCard("BEST DELIVERY", new Color(39, 174, 96))); // Bạn có thể thêm Label tương tự
        pnlCards.add(createCard("NHÂN VIÊN XUẤT SẮC", new Color(243, 156, 18))); // Bạn có thể thêm Label tương tự

        add(pnlCards, BorderLayout.NORTH);

        // 2. Tạo phần Bảng dữ liệu (Center)
        String[] columns = {"Mã NV", "Tên NV", "Số đơn", "Doanh thu", "Hoàn thành (%)", "Giao hàng (%)", "Chuyên cần", "Điểm KPI"};
        tableModel = new DefaultTableModel(columns, 0);
        kpiTable = new JTable(tableModel);

        JScrollPane scrollPane = new JScrollPane(kpiTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Chi tiết hiệu suất nhân viên"));
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createCard(String title, Color bgColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(bgColor);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Arial", Font.BOLD, 14));
        lblTitle.setForeground(Color.WHITE);
        card.add(lblTitle);

        return card;
    }

    private void loadData() {
        List<EmployeePerformance> data = kpiService.getDashboardData();
        tableModel.setRowCount(0); // Xóa dữ liệu cũ

        for (EmployeePerformance ep : data) {
            tableModel.addRow(new Object[]{
                ep.getEmployeeId(),
                ep.getEmployeeName(),
                ep.getTotalOrders(),
                ep.getRevenue(), // Chỗ này có thể bọc qua FormatUtils.formatCurrency(ep.getRevenue())
                ep.getCompletionRate(),
                ep.getDeliverySuccessRate(),
                ep.getAttendanceScore(),
                String.format("%.2f", ep.getPerformanceScore())
            });
        }

        // Cập nhật thẻ Card Top Sale
        EmployeePerformance topSale = kpiService.getTopSaleEmployee(data);
        if (topSale != null) {
            lblTopSaleName.setText((String) topSale.getEmployeeName());
            lblTopSaleName.setForeground(Color.WHITE);
            // lblTopSaleRevenue.setText(FormatUtils.formatCurrency(topSale.getRevenue())); 
            lblTopSaleRevenue.setText(String.valueOf(topSale.getRevenue()));
            lblTopSaleRevenue.setForeground(Color.WHITE);
        }
    }
}
