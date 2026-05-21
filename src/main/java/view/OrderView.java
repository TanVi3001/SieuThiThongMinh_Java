package view;

import business.service.SessionManager;
import business.sql.sales_order.CustomersSql;
import business.sql.sales_order.OrderDetailsSql;
import business.sql.sales_order.OrdersSql;
import com.toedter.calendar.JDateChooser;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.report.ReportViewer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import model.order.Customer;
import model.order.Order;

public class OrderView extends javax.swing.JPanel {

    private static final String STATUS_ALL = "Tất cả";
    private static final String SALES_INVOICE_REPORT = "/reports/SalesInvoiceReport.jrxml";

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.##");
    private JDateChooser dcFromDate;
    private JDateChooser dcToDate;
    private javax.swing.JButton btnFilterDate;
    private javax.swing.JButton btnResetFilter;
    private String userRole;
    private String empId;

    public OrderView() {
        if (SessionManager.getCurrentUser() != null) {
            this.userRole = SessionManager.getCurrentUser().getRoleId();
            this.empId = SessionManager.getCurrentEmployeeId();
            if (this.empId == null || this.empId.isBlank()) {
                this.empId = SessionManager.getCurrentUser().getUserId();
            }
            if (this.empId == null || this.empId.isBlank()) {
                this.empId = SessionManager.getCurrentUser().getAccountId();
            }
        } else {
            this.userRole = "R_ADMIN_ALL";
            this.empId = "EMP_TEST";
        }

        initComponents();
        initDateFilter();
        setupModernUI();
        initTableModel();
        initStatusFilter();
        loadDataToTable();

        EventBus.subscribe(AppDataChangedEvent.class, event -> {
            if (event.getType() == AppEventType.ORDERS) {
                System.out.println("Đã nhận tín hiệu Real-time: " + event.getMessage());
                loadDataToTable();
            }
        });

        this.revalidate();
        this.repaint();
    }

    private boolean isStoreScopedUser() {
        return SessionManager.isStoreManager()
                || "R_STAFF_SALE".equalsIgnoreCase(userRole)
                || "R_STAFF_VIEW_PROD".equalsIgnoreCase(userRole);
    }

    private String getCurrentStoreIdOrWarn() {
        String storeId = SessionManager.getCurrentStoreId();

        if (isStoreScopedUser() && (storeId == null || storeId.trim().isEmpty())) {
            JOptionPane.showMessageDialog(
                    this,
                    "Tài khoản hiện tại chưa được phân chi nhánh. Không thể tải hóa đơn.",
                    "Thiếu chi nhánh",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        return storeId != null ? storeId.trim() : null;
    }

    private List<Order> loadOrdersByCurrentScope() {
        String storeId = getCurrentStoreIdOrWarn();

        if (isStoreScopedUser() && storeId == null) {
            return java.util.Collections.emptyList();
        }

        if ("R_STAFF_SALE".equalsIgnoreCase(userRole)) {
            return OrdersSql.getInstance().selectAllByStoreAndEmployee(storeId, empId);
        }

        if (SessionManager.isStoreManager() || "R_STAFF_VIEW_PROD".equalsIgnoreCase(userRole)) {
            return OrdersSql.getInstance().selectAllByStoreId(storeId);
        }

        return OrdersSql.getInstance().selectAll();
    }

    private List<Order> loadOrdersByStatusCurrentScope(String status) {
        String storeId = getCurrentStoreIdOrWarn();

        if (isStoreScopedUser() && storeId == null) {
            return java.util.Collections.emptyList();
        }

        if (status == null || status.equals(STATUS_ALL)) {
            return loadOrdersByCurrentScope();
        }

        if ("R_STAFF_SALE".equalsIgnoreCase(userRole)) {
            return OrdersSql.getInstance().selectByConditionStoreAndEmployee(status, storeId, empId);
        }

        if (SessionManager.isStoreManager() || "R_STAFF_VIEW_PROD".equalsIgnoreCase(userRole)) {
            return OrdersSql.getInstance().selectByConditionAndStore(status, storeId);
        }

        return OrdersSql.getInstance().selectByCondition(status);
    }

    private List<Order> loadOrdersByDateCurrentScope(java.sql.Date fromDate, java.sql.Date toDate) {
        String storeId = getCurrentStoreIdOrWarn();

        if (isStoreScopedUser() && storeId == null) {
            return java.util.Collections.emptyList();
        }

        if ("R_STAFF_SALE".equalsIgnoreCase(userRole)) {
            return OrdersSql.getInstance().findByDateRangeStoreAndEmployee(fromDate, toDate, storeId, empId);
        }

        if (SessionManager.isStoreManager() || "R_STAFF_VIEW_PROD".equalsIgnoreCase(userRole)) {
            return OrdersSql.getInstance().findByDateRangeAndStore(fromDate, toDate, storeId);
        }

        return OrdersSql.getInstance().findByDateRange(fromDate, toDate);
    }

    private Order selectOrderByIdCurrentScope(String orderId) {
        String storeId = getCurrentStoreIdOrWarn();

        if (isStoreScopedUser()) {
            if (storeId == null) {
                return null;
            }

            if ("R_STAFF_SALE".equalsIgnoreCase(userRole)) {
                return OrdersSql.getInstance().selectByIdInStoreAndEmployee(orderId, storeId, empId);
            }

            return OrdersSql.getInstance().selectByIdInStore(orderId, storeId);
        }

        return OrdersSql.getInstance().selectById(orderId);
    }

    private void setupModernUI() {
        setBackground(new Color(245, 247, 250));
        pnTop.setBackground(Color.WHITE);
        pnButton.setBackground(Color.WHITE);

        pnTop.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        pnButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        jTable1.setRowHeight(44);
        jTable1.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        jTable1.setShowGrid(false);
        jTable1.setIntercellSpacing(new Dimension(0, 0));
        jTable1.setSelectionBackground(new Color(220, 235, 255));
        jTable1.setSelectionForeground(Color.BLACK);
        jTable1.setFocusable(false);

        jTable1.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        jTable1.getTableHeader().setBackground(new Color(245, 247, 250));
        jTable1.getTableHeader().setForeground(new Color(60, 60, 60));
        jTable1.getTableHeader().setPreferredSize(new Dimension(0, 46));
        jTable1.getTableHeader().setReorderingAllowed(false);

        jTable1.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 249, 250));
                }

                c.setForeground(new Color(40, 40, 40));
                ((javax.swing.JLabel) c).setHorizontalAlignment(javax.swing.SwingConstants.CENTER);

                if (column == 4) {
                    String status = value != null ? value.toString() : "";
                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    if (status.equalsIgnoreCase("Hoàn thành") || status.equalsIgnoreCase("COMPLETED")) {
                        c.setForeground(new Color(16, 185, 129));
                    } else if (status.equalsIgnoreCase("Đang xử lý") || status.equalsIgnoreCase("PENDING")) {
                        c.setForeground(new Color(245, 158, 11));
                    } else if (status.equalsIgnoreCase("Đã hủy") || status.equalsIgnoreCase("Đã huỷ") || status.equalsIgnoreCase("CANCELLED")) {
                        c.setForeground(new Color(239, 68, 68));
                    }
                }

                if (column == 3) {
                    c.setForeground(new Color(59, 130, 246));
                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                }

                if (column == 0) {
                    c.setForeground(new Color(79, 70, 229));
                    c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                }

                if (column == 1) {
                    c.setForeground(new Color(5, 150, 105));
                }

                if (column == 2) {
                    c.setForeground(new Color(120, 113, 108));
                }

                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        });

        tbOrder.setBorder(BorderFactory.createEmptyBorder());

        jTable1.getColumnModel().getColumn(0).setPreferredWidth(120);
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(150);
        jTable1.getColumnModel().getColumn(2).setPreferredWidth(120);
        jTable1.getColumnModel().getColumn(3).setPreferredWidth(140);
        jTable1.getColumnModel().getColumn(4).setPreferredWidth(140);

        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                if (evt.getClickCount() == 2) {
                    btnDetailActionPerformed(null);
                }
            }
        });

        cbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbStatus.setPreferredSize(new Dimension(160, 36));

        Status.setFont(new Font("Segoe UI", Font.BOLD, 13));
        Status.setForeground(new Color(70, 70, 70));

        styleButton(btnDetail, new Color(59, 130, 246));
        styleButton(btnUpdate, new Color(16, 185, 129));
        styleButton(btnIssueAnInvoice, new Color(239, 68, 68));

        JTableHeader header = jTable1.getTableHeader();
        header.setDefaultRenderer(new TableCellRenderer() {
            private final DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                c.setForeground(Color.WHITE);

                switch (column) {
                    case 0 ->
                        c.setBackground(new Color(59, 130, 246));
                    case 1 ->
                        c.setBackground(new Color(16, 185, 129));
                    case 2 ->
                        c.setBackground(new Color(245, 158, 11));
                    case 3 ->
                        c.setBackground(new Color(139, 92, 246));
                    case 4 ->
                        c.setBackground(new Color(239, 68, 68));
                    default ->
                        c.setBackground(new Color(59, 130, 246));
                }

                ((javax.swing.JLabel) c).setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
                return c;
            }
        });
    }

    private void initDateFilter() {
        dcFromDate = new JDateChooser();
        dcToDate = new JDateChooser();

        Calendar cal = Calendar.getInstance();
        cal.set(2020, Calendar.JANUARY, 1);
        dcFromDate.setDate(cal.getTime());
        dcToDate.setDate(new Date());

        dcFromDate.setDateFormatString("dd/MM/yyyy");
        dcToDate.setDateFormatString("dd/MM/yyyy");

        JTextField fromEditor = (JTextField) dcFromDate.getDateEditor().getUiComponent();
        fromEditor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fromEditor.setMargin(new java.awt.Insets(2, 5, 2, 5));

        JTextField toEditor = (JTextField) dcToDate.getDateEditor().getUiComponent();
        toEditor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        toEditor.setMargin(new java.awt.Insets(2, 5, 2, 5));

        dcFromDate.setPreferredSize(new Dimension(200, 38));
        dcFromDate.setMinimumSize(new Dimension(200, 38));
        dcToDate.setPreferredSize(new Dimension(200, 38));
        dcToDate.setMinimumSize(new Dimension(200, 38));
        dcFromDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dcToDate.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        btnFilterDate = new javax.swing.JButton("Lọc");
        btnResetFilter = new javax.swing.JButton("Đặt lại");
        styleButton(btnFilterDate, new Color(59, 130, 246));
        styleButton(btnResetFilter, new Color(107, 114, 128));
        btnFilterDate.setPreferredSize(new Dimension(100, 38));
        btnResetFilter.setPreferredSize(new Dimension(110, 38));

        btnFilterDate.addActionListener(e -> filterByDate());
        btnResetFilter.addActionListener(e -> {
            Calendar resetCal = Calendar.getInstance();
            resetCal.set(2020, Calendar.JANUARY, 1);
            dcFromDate.setDate(resetCal.getTime());
            dcToDate.setDate(new Date());
            cbStatus.setSelectedItem(STATUS_ALL);
            loadDataToTable();
        });

        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 8, 5, 8);
        gbc.gridy = 0;
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.fill = java.awt.GridBagConstraints.NONE;

        gbc.gridx = 2;
        pnTop.add(new javax.swing.JLabel("Từ ngày"), gbc);
        gbc.gridx = 3;
        pnTop.add(dcFromDate, gbc);
        gbc.gridx = 4;
        pnTop.add(new javax.swing.JLabel("Đến ngày"), gbc);
        gbc.gridx = 5;
        pnTop.add(dcToDate, gbc);
        gbc.gridx = 6;
        pnTop.add(btnFilterDate, gbc);
        gbc.gridx = 7;
        pnTop.add(btnResetFilter, gbc);
    }

    private void styleButton(javax.swing.JButton button, Color bg) {
        button.setBackground(bg);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(170, 38));
    }

    private void filterByDate() {
        Date from = dcFromDate.getDate();
        Date to = dcToDate.getDate();

        if (from == null || to == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc!");
            return;
        }

        if (from.after(to)) {
            JOptionPane.showMessageDialog(this,
                    "Lỗi: 'Từ ngày' không được lớn hơn 'Đến ngày'!",
                    "Sai ngày tháng",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            java.sql.Date sqlFrom = new java.sql.Date(from.getTime());
            java.sql.Date sqlTo = new java.sql.Date(to.getTime());
            fillTable(loadOrdersByDateCurrentScope(sqlFrom, sqlTo));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi lọc theo ngày: " + ex.getMessage());
        }
    }

    private void initTableModel() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[]{"Mã đơn", "Khách hàng", "Ngày", "Tổng tiền", "Trạng thái"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        jTable1.setModel(model);
        jTable1.setAutoCreateRowSorter(true);
    }

    private void initStatusFilter() {
        cbStatus.setModel(new javax.swing.DefaultComboBoxModel<>(
                new String[]{STATUS_ALL, "Đang xử lý", "Hoàn thành", "Đã hủy"}
        ));
        cbStatus.setSelectedItem(STATUS_ALL);
    }

    private void loadDataToTable() {
        new SwingWorker<List<Order>, Void>() {
            @Override
            protected List<Order> doInBackground() {
                return loadOrdersByCurrentScope();
            }

            @Override
            protected void done() {
                try {
                    fillTable(get());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private void fillTable(List<Order> list) {
        if (list == null) {
            return;
        }
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        for (Order o : list) {
            model.addRow(new Object[]{
                o.getOrderId(),
                o.getCustomerId() != null ? o.getCustomerId() : "Khách vãng lai",
                o.getOrderDate(),
                moneyFormat.format(o.getTotalAmount()) + " đ",
                normalizeDisplayStatus(o.getStatus())
            });
        }
    }

    private String normalizeDisplayStatus(String status) {
        if (status == null) {
            return "";
        }
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return "Hoàn thành";
        }
        if ("PENDING".equalsIgnoreCase(status)) {
            return "Đang xử lý";
        }
        if ("CANCELLED".equalsIgnoreCase(status)) {
            return "Đã hủy";
        }
        return status;
    }

    private int getSelectedModelRow() {
        int viewRow = jTable1.getSelectedRow();
        if (viewRow < 0) {
            return -1;
        }
        return jTable1.convertRowIndexToModel(viewRow);
    }

    private String getSelectedOrderId() {
        int modelRow = getSelectedModelRow();
        if (modelRow < 0) {
            return null;
        }
        return String.valueOf(jTable1.getModel().getValueAt(modelRow, 0));
    }

    private void showOrderDetailsDialog(String orderId) {
        try {
            Order order = selectOrderByIdCurrentScope(orderId);
            if (order == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy hóa đơn hoặc bạn không có quyền xem hóa đơn này!");
                return;
            }

            Customer customer = null;
            if (order.getCustomerId() != null && !order.getCustomerId().equalsIgnoreCase("Khách vãng lai")) {
                customer = CustomersSql.getInstance().selectById(order.getCustomerId());
            }

            List<Map<String, Object>> details;

            if (isStoreScopedUser()) {
                String storeId = getCurrentStoreIdOrWarn();

                if (storeId == null) {
                    return;
                }

                details = OrderDetailsSql.getInstance().selectDetailRowsByOrderIdAndStore(orderId, storeId);
            } else {
                details = OrderDetailsSql.getInstance().selectDetailRowsByOrderId(orderId);
            }

            if (details == null || details.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy chi tiết sản phẩm cho hóa đơn này!", "Trống", JOptionPane.WARNING_MESSAGE);
                return;
            }

            java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(this);
            OrderDetailDialog dialog = new OrderDetailDialog((java.awt.Frame) win, order, customer, details);
            dialog.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi khi tải chi tiết hóa đơn: " + e.getMessage());
        }
    }

    private void exportInvoice(String orderId) throws IOException {
        Order order = selectOrderByIdCurrentScope(orderId);
        if (order == null) {
            throw new IOException("Khong tim thay hoa don hoac khong co quyen xem: " + orderId);
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chon noi luu hoa don (PDF)");
        chooser.setSelectedFile(new java.io.File("HoaDon_" + orderId + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        List<Map<String, Object>> details;

        if (isStoreScopedUser()) {
            String storeId = getCurrentStoreIdOrWarn();

            if (storeId == null) {
                throw new IOException("Không xác định được chi nhánh hiện tại.");
            }

            details = OrderDetailsSql.getInstance().selectDetailRowsByOrderIdAndStore(orderId, storeId);
        } else {
            details = OrderDetailsSql.getInstance().selectDetailRowsByOrderId(orderId);
        }

        try {
            com.itextpdf.kernel.pdf.PdfWriter writer = new com.itextpdf.kernel.pdf.PdfWriter(chooser.getSelectedFile().getAbsolutePath());
            com.itextpdf.kernel.pdf.PdfDocument pdf = new com.itextpdf.kernel.pdf.PdfDocument(writer);
            com.itextpdf.layout.Document document = new com.itextpdf.layout.Document(pdf);

            document.add(new com.itextpdf.layout.element.Paragraph("HÓA ĐƠN BÁN HÀNG")
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER)
                    .setBold().setFontSize(20));

            document.add(new com.itextpdf.layout.element.Paragraph("Mã đơn: " + order.getOrderId()));
            document.add(new com.itextpdf.layout.element.Paragraph("Khách hàng: " + order.getCustomerId()));
            document.add(new com.itextpdf.layout.element.Paragraph("Nhân viên: " + order.getEmployeeId()));
            document.add(new com.itextpdf.layout.element.Paragraph("Chi nhánh: " + (order.getStoreId() != null ? order.getStoreId() : "")));
            document.add(new com.itextpdf.layout.element.Paragraph("Ngày: " + order.getOrderDate()));
            document.add(new com.itextpdf.layout.element.Paragraph("Trạng thái: " + normalizeDisplayStatus(order.getStatus())));
            document.add(new com.itextpdf.layout.element.Paragraph("Tổng tiền: " + moneyFormat.format(order.getTotalAmount()) + " VNĐ").setBold());
            document.add(new com.itextpdf.layout.element.Paragraph("\n"));

            com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(5);
            table.addHeaderCell("Mã SP");
            table.addHeaderCell("Tên sản phẩm");
            table.addHeaderCell("SL");
            table.addHeaderCell("Đơn giá");
            table.addHeaderCell("Thành tiền");

            for (Map<String, Object> detail : details) {
                table.addCell(String.valueOf(detail.get("product_id")));
                table.addCell(detail.get("product_name") != null ? detail.get("product_name").toString() : String.valueOf(detail.get("product_id")));
                table.addCell(String.valueOf(detail.get("quantity")));
                table.addCell(moneyFormat.format(detail.get("unit_price")));
                table.addCell(moneyFormat.format(detail.get("line_total")));
            }
            document.add(table);
            document.close();
            JOptionPane.showMessageDialog(this, "Đã xuất hoá đơn PDF thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            throw new IOException("Lỗi khi tạo PDF: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        pnTop = new javax.swing.JPanel();
        Status = new javax.swing.JLabel();
        cbStatus = new javax.swing.JComboBox<>();
        tbOrder = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        pnButton = new javax.swing.JPanel();
        btnDetail = new javax.swing.JButton();
        btnUpdate = new javax.swing.JButton();
        btnIssueAnInvoice = new javax.swing.JButton();

        setLayout(new java.awt.BorderLayout());

        pnTop.setBackground(new java.awt.Color(236, 240, 241));
        pnTop.setPreferredSize(new java.awt.Dimension(342, 60));
        pnTop.setLayout(new java.awt.GridBagLayout());

        Status.setFont(new java.awt.Font("Segoe UI", 1, 12));
        Status.setText("Trạng thái");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 184, 0, 0);
        pnTop.add(Status, gridBagConstraints);

        cbStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Tất cả", "Đang xử lý", "Hoàn thành", "Đã hủy"}));
        cbStatus.addActionListener(this::cbStatusActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 12, 5, 172);
        pnTop.add(cbStatus, gridBagConstraints);

        add(pnTop, java.awt.BorderLayout.PAGE_START);

        jTable1.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{{null, null, null, null, null}},
                new String[]{"Mã đơn", "Khách hàng", "Ngày", "Tổng tiền", "Trạng thái"}
        ));
        tbOrder.setViewportView(jTable1);

        add(tbOrder, java.awt.BorderLayout.CENTER);

        pnButton.setBackground(new java.awt.Color(236, 240, 241));
        pnButton.setPreferredSize(new java.awt.Dimension(358, 70));
        pnButton.setLayout(new java.awt.GridBagLayout());

        btnDetail.setFont(new java.awt.Font("Segoe UI", 1, 12));
        btnDetail.setText("Xem chi tiết");
        btnDetail.addActionListener(this::btnDetailActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 44, 12, 0);
        pnButton.add(btnDetail, gridBagConstraints);

        btnUpdate.setFont(new java.awt.Font("Segoe UI", 1, 12));
        btnUpdate.setText("Cập nhật trạng thái");
        btnUpdate.addActionListener(this::btnUpdateActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 6, 12, 0);
        pnButton.add(btnUpdate, gridBagConstraints);

        btnIssueAnInvoice.setFont(new java.awt.Font("Segoe UI", 1, 12));
        btnIssueAnInvoice.setForeground(new java.awt.Color(204, 0, 0));
        btnIssueAnInvoice.setText("Xuất hóa đơn");
        btnIssueAnInvoice.addActionListener(this::btnIssueAnInvoiceActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 6, 12, 59);
        pnButton.add(btnIssueAnInvoice, gridBagConstraints);

        add(pnButton, java.awt.BorderLayout.PAGE_END);

        if ("R_STAFF_SALE".equalsIgnoreCase(userRole)) {
            btnUpdate.setVisible(false);
        }
    }

    private boolean requirePassword(String actionName) {
        javax.swing.JPasswordField pf = new javax.swing.JPasswordField();
        Object[] message = {
            "Thao tác [" + actionName + "] yêu cầu xác thực bảo mật.",
            "Vui lòng nhập mật khẩu tài khoản của bạn để tiếp tục:",
            pf
        };

        int option = JOptionPane.showConfirmDialog(
                this, message, "Xác thực bảo mật",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE
        );

        if (option != JOptionPane.OK_OPTION) {
            return false;
        }

        String enteredPass = new String(pf.getPassword());
        if (enteredPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Mật khẩu không được để trống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        try {
            model.account.Account user = SessionManager.getCurrentUser();
            if (user == null || user.getUsername() == null) {
                JOptionPane.showMessageDialog(this, "Không xác định được tài khoản hiện tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return false;
            }

            String hashFromDb = null;
            try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement("SELECT password FROM ACCOUNTS WHERE username = ? AND NVL(is_deleted, 0) = 0")) {
                ps.setString(1, user.getUsername());
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        hashFromDb = rs.getString("password");
                    }
                }
            }

            if (common.utils.PasswordUtils.checkPassword(enteredPass, hashFromDb)) {
                return true;
            }

            JOptionPane.showMessageDialog(this, "Mật khẩu không chính xác!", "Từ chối truy cập", JOptionPane.ERROR_MESSAGE);
            return false;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi hệ thống khi xác thực: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {
        int row = getSelectedModelRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đơn hàng!");
            return;
        }

        String orderId = jTable1.getModel().getValueAt(row, 0).toString();
        String currentStatus = jTable1.getModel().getValueAt(row, 4).toString();

        if (currentStatus.equals("Đã hủy")) {
            JOptionPane.showMessageDialog(this, "Đơn hàng này đã bị hủy, không thể thay đổi trạng thái!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String[] statuses = {"Đang xử lý", "Hoàn thành", "Đã hủy"};
        String newStatus = (String) JOptionPane.showInputDialog(
                this,
                "Chọn trạng thái mới",
                "Cập nhật trạng thái",
                JOptionPane.QUESTION_MESSAGE,
                null,
                statuses,
                currentStatus
        );

        if (newStatus == null || newStatus.equals(currentStatus)) {
            return;
        }

        try {
            if (newStatus.equals("Đã hủy")) {
                if (!requirePassword("Hủy hóa đơn")) {
                    return;
                }
                String reason = JOptionPane.showInputDialog(this, "Xác thực thành công!\nNhập lý do hủy đơn (Bắt buộc):");
                if (reason == null || reason.trim().isEmpty()) {
                    return;
                }
                boolean success = business.service.PaymentService.cancelOrder(orderId, this.empId, reason);
                if (success) {
                    JOptionPane.showMessageDialog(this, "✅ Đã hủy đơn hàng và hoàn lại tồn kho!");
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Không thể hủy đơn hàng. Vui lòng kiểm tra lại!");
                }
            } else {
                int result;
                if (isStoreScopedUser()) {
                    String storeId = getCurrentStoreIdOrWarn();
                    if (storeId == null) {
                        return;
                    }
                    result = OrdersSql.getInstance().updateStatusInStore(orderId, newStatus, storeId);
                } else {
                    result = OrdersSql.getInstance().updateStatus(orderId, newStatus);
                }

                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "Cập nhật trạng thái thành công!");
                } else {
                    JOptionPane.showMessageDialog(this, "Cập nhật thất bại hoặc bạn không có quyền thao tác hóa đơn này!");
                }
            }

            try {
                common.sync.SyncVersionDao.bumpVersion("ORDERS");
                common.realtime.RealtimeClient.send("ORDERS_CHANGED");
            } catch (Exception ignored) {
            }

            EventBus.publish(new AppDataChangedEvent(AppEventType.ORDERS, "Cập nhật trạng thái bill"));
            loadDataToTable();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage());
        }
    }

    private void cbStatusActionPerformed(java.awt.event.ActionEvent evt) {
        Object selectedObj = cbStatus.getSelectedItem();
        if (selectedObj == null) {
            return;
        }
        String selected = selectedObj.toString();
        try {
            fillTable(loadOrdersByStatusCurrentScope(selected));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi lọc hóa đơn: " + ex.getMessage());
        }
    }

    private void btnDetailActionPerformed(java.awt.event.ActionEvent evt) {
        String orderId = getSelectedOrderId();
        if (orderId == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn đơn hàng muốn xem chi tiết!");
            return;
        }
        showOrderDetailsDialog(orderId);
    }

    private void btnIssueAnInvoiceActionPerformed(java.awt.event.ActionEvent evt) {
        String orderId = getSelectedOrderId();
        if (orderId == null || orderId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn một hóa đơn trong bảng để xuất!", "Chưa chọn hóa đơn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            Order order = selectOrderByIdCurrentScope(orderId.trim());
            if (order == null) {
                JOptionPane.showMessageDialog(this, "Bạn không có quyền xuất hóa đơn này!", "Không có quyền", JOptionPane.WARNING_MESSAGE);
                return;
            }
            openSalesInvoiceReport(orderId.trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi xuất hóa đơn: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSalesInvoiceReport(String orderId) {
        HashMap<String, Object> params = new HashMap<>();
        params.put("ORDER_ID", orderId);

        String storeId = getCurrentStoreIdOrWarn();

        if (isStoreScopedUser()) {
            params.put("STORE_ID", storeId);
        } else {
            params.put("STORE_ID", null);
        }

        ReportViewer.showReport(SALES_INVOICE_REPORT, params);
    }

    private javax.swing.JLabel Status;
    private javax.swing.JButton btnDetail;
    private javax.swing.JButton btnIssueAnInvoice;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JComboBox<String> cbStatus;
    private javax.swing.JTable jTable1;
    private javax.swing.JPanel pnButton;
    private javax.swing.JPanel pnTop;
    private javax.swing.JScrollPane tbOrder;
}
