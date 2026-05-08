package view;

import business.EmployeePerformanceService;
import model.employee.EmployeePerformance;
import common.utils.FormatUtils;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.List;
import java.util.Date;

public class EmployeePerformanceTab extends JPanel {

    private JTable tblKPI;
    private DefaultTableModel model;
    private EmployeePerformanceService service = new EmployeePerformanceService();

    // Màu sắc theo style ManagerManagementView của bạn
    private final Color textDark = new Color(43, 54, 116);
    private final Color primaryBlue = new Color(54, 92, 245);

    public EmployeePerformanceTab() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        initTable();
    }

    private void initTable() {
        // Cấu trúc cột đúng theo ảnh bạn chụp
        String[] cols = {"Mã NV", "Tên Nhân Viên", "Đơn Hoàn Thành", "Đơn Bị Hủy", "Doanh Thu Mang Về", "Điểm KPI"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tblKPI = new JTable(model);
        setupTableStyle();

        JScrollPane sp = new JScrollPane(tblKPI);
        sp.setBorder(BorderFactory.createEmptyBorder());
        add(sp, BorderLayout.CENTER);
    }

    private void setupTableStyle() {
        tblKPI.setRowHeight(40);
        tblKPI.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblKPI.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        tblKPI.getTableHeader().setBackground(new Color(244, 246, 250));
        tblKPI.getTableHeader().setForeground(textDark);
        tblKPI.setSelectionBackground(new Color(232, 240, 254));

        // Căn giữa dữ liệu các cột số lượng
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        tblKPI.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tblKPI.getColumnModel().getColumn(2).setCellRenderer(centerRenderer);
        tblKPI.getColumnModel().getColumn(3).setCellRenderer(centerRenderer);
    }

    // Hàm này để lớp cha (StatisticView) gọi khi bấm nút "Lọc Dữ Liệu"
    public void refreshData(Date from, Date to) {
        model.setRowCount(0);
        // Lưu ý: Bạn cần cập nhật Service để nhận 2 tham số ngày này
        // List<EmployeePerformance> data = service.getKPIByDate(from, to); 

        // Demo đổ dữ liệu (Sau khi bạn nối Service xong)
        /*
        for (EmployeePerformance ep : data) {
            model.addRow(new Object[]{
                ep.getEmployeeId(),
                ep.getEmployeeName(),
                ep.getTotalOrders(),
                ep.getTotalDeliveries(), // Đang dùng làm cột hủy
                FormatUtils.formatCurrency(ep.getRevenue()),
                String.format("%.2f", ep.getPerformanceScore())
            });
        }
         */
    }
}
