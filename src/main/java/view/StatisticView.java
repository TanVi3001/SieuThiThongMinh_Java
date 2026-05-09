package view;

import business.service.StatisticService;
import com.toedter.calendar.JDateChooser;
import common.utils.FormatUtils;
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

// IMPORT EVENT ĐỂ LÀM REALTIME
import common.events.EventBus;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;

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

    private final StatisticService statisticService = new StatisticService();

    public StatisticView() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents();

        // =========================================================
        // BỘ THU TÍN HIỆU REAL-TIME (Bắt mọi Event liên quan)
        // =========================================================
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            // Mở rộng bộ lọc: Bắt tất cả các loại sự kiện có thể làm thay đổi Doanh thu/Tồn kho
            if (e.getType() == AppEventType.ORDERS
                    || e.getType() == AppEventType.INVENTORY
                    || e.getType() == AppEventType.PRODUCTS
                    || e.getType().toString().contains("ORDER")) { // Chống lọt lưới Enum

                SwingUtilities.invokeLater(() -> {
                    System.out.println("🔔 [StatisticView] Nhận tín hiệu Real-time: Bắt đầu tải lại bảng Thống Kê...");
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

        // TAB 1: DOANH THU
        modRevenue = new DefaultTableModel(new Object[]{"Ngày giao dịch", "Tổng Đơn", "Doanh Thu Thực Tế"}, 0);
        tblRevenue = createTable(modRevenue);
        alignColumn(tblRevenue, 1, SwingConstants.CENTER);
        alignColumn(tblRevenue, 2, SwingConstants.RIGHT);
        tabbedPane.addTab("Thống kê Doanh Thu", createTabPanel(tblRevenue));

        // TAB 2: HÀNG HÓA
        modProducts = new DefaultTableModel(new Object[]{"Mã SP", "Tên Sản Phẩm", "Số lượng Đã Bán", "Doanh thu mang lại", "Tồn Kho Hiện Tại"}, 0);
        tblProducts = createTable(modProducts);
        alignColumn(tblProducts, 2, SwingConstants.CENTER);
        alignColumn(tblProducts, 3, SwingConstants.RIGHT);
        alignColumn(tblProducts, 4, SwingConstants.CENTER);
        tabbedPane.addTab("Phân tích Hàng Hóa", createTabPanel(tblProducts));

        // TAB 3: NHÂN VIÊN
        modEmployees = new DefaultTableModel(new Object[]{"Mã NV", "Tên Nhân Viên", "Đơn Hoàn Thành", "Đơn Bị Hủy", "Doanh Thu Mang Về"}, 0);
        tblEmployees = createTable(modEmployees);
        alignColumn(tblEmployees, 0, SwingConstants.LEFT);
        alignColumn(tblEmployees, 2, SwingConstants.CENTER);
        alignColumn(tblEmployees, 3, SwingConstants.CENTER);
        alignColumn(tblEmployees, 4, SwingConstants.RIGHT);
        tabbedPane.addTab("Hiệu Suất Nhân Viên", createTabPanel(tblEmployees));

        add(tabbedPane, BorderLayout.CENTER);
    }

    private void initEvents() {
        btnFilter.addActionListener(e -> refreshDataWithCurrentDates(false, true));
    }

    private void loadInitialData() {
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(today);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        Date firstDayOfMonth = cal.getTime();

        dpFromDate.setDate(firstDayOfMonth);
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

        try {
            // Xóa sạch dữ liệu cũ trên Table
            modRevenue.setRowCount(0);
            modProducts.setRowCount(0);
            modEmployees.setRowCount(0);

            // Nạp Tab 1: Doanh thu
            List<Object[]> revData = statisticService.getRevenueReport(fromDate, toDate);
            for (Object[] row : revData) {
                row[2] = common.utils.FormatUtils.formatCurrency((double) row[2]);
                modRevenue.addRow(row);
            }

            // Nạp Tab 2: Hàng hóa
            List<Object[]> prodData = statisticService.getProductReport(fromDate, toDate);
            for (Object[] row : prodData) {
                row[3] = common.utils.FormatUtils.formatCurrency((double) row[3]);
                modProducts.addRow(row);
            }

            // Nạp Tab 3: Hiệu suất nhân viên
            List<Object[]> empData = statisticService.getEmployeeReport(fromDate, toDate);
            for (Object[] row : empData) {
                double revenue = (double) row[4];
                row[4] = common.utils.FormatUtils.formatCurrency(revenue);
                modEmployees.addRow(row);
            }

            // ÉP CÁC BẢNG PHẢI VẼ LẠI NGAY LẬP TỨC ĐỂ HIỂN THỊ REAL-TIME
            tblRevenue.revalidate();
            tblRevenue.repaint();
            tblProducts.revalidate();
            tblProducts.repaint();
            tblEmployees.revalidate();
            tblEmployees.repaint();

            // Cập nhật nhãn thời gian
            String timeNow = new java.text.SimpleDateFormat("HH:mm:ss").format(new Date());
            lblLastUpdate.setText("Cập nhật lần cuối: " + timeNow + (isAutoSync ? " (Đồng bộ tự động)" : " (Tải thủ công)"));
            if (isAutoSync) {
                lblLastUpdate.setForeground(new Color(0, 163, 108)); // Màu xanh nếu là real-time
            } else {
                lblLastUpdate.setForeground(textGray);
            }

            if (showSuccessPopup) {
                JOptionPane.showMessageDialog(this, "✅ Lọc dữ liệu thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            if (!isAutoSync) {
                JOptionPane.showMessageDialog(this, "Lỗi khi lọc dữ liệu: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
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
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setSelectionBackground(new Color(232, 240, 254));
        table.setSelectionForeground(textDark);
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 45));
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(new Color(248, 249, 250));
        header.setForeground(primaryBlue);
        header.setReorderingAllowed(false);

        return table;
    }

    private void alignColumn(JTable table, int column, int alignment) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(alignment);
        table.getColumnModel().getColumn(column).setCellRenderer(renderer);
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
            btn.setIconTextGap(8);
        }

        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setPreferredSize(new Dimension(130, 35));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);

        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ButtonModel model = ((AbstractButton) c).getModel();
                Color fill = bg;
                if (model.isPressed()) {
                    fill = bg.darker();
                } else if (model.isRollover()) {
                    fill = bg.brighter();
                }

                g2.setColor(fill);
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 15, 15);
                g2.dispose();
                super.paint(g, c);
            }
        });
        return btn;
    }
}
