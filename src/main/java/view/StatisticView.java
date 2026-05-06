package view;

import business.sql.sales_order.StatisticSql;
import com.toedter.calendar.JDateChooser;
import view.components.IconHelper;

import java.awt.*;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

public class StatisticView extends JPanel {

    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color primaryBlue = new Color(67, 97, 238);
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color excelGreen = new Color(33, 115, 70);
    private final Color pdfRed = new Color(210, 33, 40);

    private JDateChooser dpFromDate, dpToDate;
    private JButton btnFilter, btnExportExcel, btnExportPDF;
    private JTabbedPane tabbedPane;
    private JLabel lblLastUpdate;

    private JTable tblRevenue, tblProducts, tblEmployees;
    private DefaultTableModel modRevenue, modProducts, modEmployees;

    public StatisticView() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents();

        // Đăng ký sự kiện Real-time
        common.events.EventBus.subscribe(common.events.AppDataChangedEvent.class, e -> {
            if (e.getType() == common.events.AppEventType.ORDERS
                    || e.getType() == common.events.AppEventType.INVENTORY
                    || e.getType() == common.events.AppEventType.PRODUCTS) {

                SwingUtilities.invokeLater(() -> {
                    // Cập nhật ngầm (true), không hiện thông báo
                    refreshDataWithCurrentDates(true, false);
                });
            }
        });

        loadInitialData();
    }

    private void initUI() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Báo Cáo & Thống Kê");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Phân tích doanh thu, hàng hóa và hiệu suất nhân viên");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        dpFromDate = new JDateChooser();
        dpFromDate.setDateFormatString("dd/MM/yyyy");
        dpFromDate.setPreferredSize(new Dimension(140, 35));
        dpToDate = new JDateChooser();
        dpToDate.setDateFormatString("dd/MM/yyyy");
        dpToDate.setPreferredSize(new Dimension(140, 35));

        btnFilter = createCustomButton("Lọc Dữ Liệu", primaryBlue, Color.WHITE, IconHelper.search(18));
        btnExportExcel = createCustomButton("Xuất Excel", excelGreen, Color.WHITE, null);
        btnExportPDF = createCustomButton("Xuất PDF", pdfRed, Color.WHITE, null);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        actionPanel.setOpaque(false);
        actionPanel.add(createLabel("Từ ngày:"));
        actionPanel.add(dpFromDate);
        actionPanel.add(createLabel("Đến ngày:"));
        actionPanel.add(dpToDate);
        actionPanel.add(btnFilter);
        actionPanel.add(btnExportExcel);
        actionPanel.add(btnExportPDF);

        lblLastUpdate = new JLabel("Đang tải dữ liệu...");
        lblLastUpdate.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblLastUpdate.setForeground(new Color(100, 116, 139));
        lblLastUpdate.setHorizontalAlignment(SwingConstants.RIGHT);
        lblLastUpdate.setBorder(new EmptyBorder(5, 0, 0, 15));

        JPanel filterWrapper = new JPanel(new BorderLayout());
        filterWrapper.setOpaque(false);
        filterWrapper.add(actionPanel, BorderLayout.CENTER);
        filterWrapper.add(lblLastUpdate, BorderLayout.SOUTH);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(filterWrapper, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.addChangeListener(e -> {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                tabbedPane.setForegroundAt(i, i == tabbedPane.getSelectedIndex() ? primaryBlue : textGray);
            }
        });

        modRevenue = new DefaultTableModel(new Object[]{"Ngày giao dịch", "Tổng Đơn", "Doanh Thu Thực Tế"}, 0);
        tblRevenue = createTable(modRevenue);
        tabbedPane.addTab("Thống kê Doanh Thu", createTabPanel(tblRevenue));

        modProducts = new DefaultTableModel(new Object[]{"Mã SP", "Tên Sản Phẩm", "Số lượng Đã Bán", "Doanh thu mang lại", "Tồn Kho Hiện Tại"}, 0);
        tblProducts = createTable(modProducts);
        tabbedPane.addTab("Phân tích Hàng Hóa", createTabPanel(tblProducts));

        modEmployees = new DefaultTableModel(new Object[]{"Mã NV", "Tên Nhân Viên", "Đơn Hoàn Thành", "Đơn Bị Hủy", "Doanh Thu Mang Về"}, 0);
        tblEmployees = createTable(modEmployees);
        tabbedPane.addTab("Hiệu Suất Nhân Viên", createTabPanel(tblEmployees));

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initEvents() {
        btnFilter.addActionListener(e -> {
            // Khi nhấn nút: Manual sync (false), và HIỆN thông báo (true)
            refreshDataWithCurrentDates(false, true);
        });
    }

    private void loadInitialData() {
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date firstDayOfMonth = cal.getTime();

        dpFromDate.setDate(firstDayOfMonth);
        dpToDate.setDate(today);

        // Lần đầu tải: Không phải auto (false), KHÔNG hiện thông báo (false)
        refreshDataWithCurrentDates(false, false);
    }

    // Cải tiến hàm refresh để kiểm soát việc hiện Popup
    private void refreshDataWithCurrentDates(boolean isAutoSync, boolean showSuccessPopup) {
        Date fromDate = dpFromDate.getDate();
        Date toDate = dpToDate.getDate();

        if (fromDate == null || toDate == null) {
            if (!isAutoSync) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn đầy đủ ngày!");
            }
            return;
        }

        if (fromDate.after(toDate)) {
            if (!isAutoSync) {
                JOptionPane.showMessageDialog(this, "'Từ ngày' phải nhỏ hơn 'Đến ngày'!");
            }
            return;
        }

        modRevenue.setRowCount(0);
        modProducts.setRowCount(0);
        modEmployees.setRowCount(0);

        List<Object[]> revData = StatisticSql.getInstance().getRevenueReport(fromDate, toDate);
        for (Object[] row : revData) {
            modRevenue.addRow(row);
        }

        List<Object[]> prodData = StatisticSql.getInstance().getProductReport(fromDate, toDate);
        for (Object[] row : prodData) {
            modProducts.addRow(row);
        }

        List<Object[]> empData = StatisticSql.getInstance().getEmployeeReport(fromDate, toDate);
        for (Object[] row : empData) {
            modEmployees.addRow(row);
        }

        String timeNow = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
        if (isAutoSync) {
            lblLastUpdate.setText("Cập nhật lần cuối: " + timeNow + " (Đồng bộ tự động thời gian thực)");
            lblLastUpdate.setForeground(new Color(0, 163, 108));
        } else {
            lblLastUpdate.setText("Cập nhật lần cuối: " + timeNow + " (Tải thủ công)");
            lblLastUpdate.setForeground(new Color(100, 116, 139));
            // Chỉ hiện popup nếu tham số showSuccessPopup là true
            if (showSuccessPopup) {
                JOptionPane.showMessageDialog(this, "✅ Đã tải dữ liệu thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(bgLight);
        header.setForeground(primaryBlue);
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(237, 242, 255));
        table.setSelectionForeground(textDark);
        return table;
    }

    private JPanel createTabPanel(JTable table) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(Color.WHITE);
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(textDark);
        return lbl;
    }

    private JButton createCustomButton(String text, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(text);
        if (icon != null) {
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH)));
        }
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setPreferredSize(new Dimension(130, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 20, 20);
                super.paint(g2, c);
                g2.dispose();
            }
        });
        return btn;
    }
}
