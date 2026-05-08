/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JPanel.java to edit this template
 */
package view;

import business.sql.sales_order.OrderDetailsSql;
import business.sql.sales_order.OrdersSql;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import model.order.Order;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import javax.swing.BorderFactory;
import javax.swing.table.DefaultTableCellRenderer;
import com.toedter.calendar.JDateChooser;
import java.util.Date;
import java.text.SimpleDateFormat;
import com.toedter.calendar.JDateChooser;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.JTextField;

/**
 *
 * @author Admin
 */
public class OrderView extends javax.swing.JPanel {

    private static final String STATUS_ALL = "Tất cả";
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0.##");
    private JDateChooser dcFromDate;
    private JDateChooser dcToDate;
    private javax.swing.JButton btnFilterDate;
    private javax.swing.JButton btnResetFilter;

    /**
     * Creates new form OrderView
     */
    public OrderView() {
        initComponents();

        initDateFilter();

        setupModernUI();

        initTableModel();
        initStatusFilter();

        loadDataToTable();

        this.revalidate();
        this.repaint();
    }

    private void setupModernUI() {

        // =========================
        // PANEL BACKGROUND
        // =========================
        setBackground(new Color(245, 247, 250));

        pnTop.setBackground(Color.WHITE);
        pnButton.setBackground(Color.WHITE);

        pnTop.setBorder(
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        );

        pnButton.setBorder(
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        );

        // =========================
        // TABLE STYLE
        // =========================
        jTable1.setRowHeight(44);
        jTable1.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        jTable1.setShowGrid(false);
        jTable1.setIntercellSpacing(new Dimension(0, 0));

        jTable1.setSelectionBackground(new Color(220, 235, 255));
        jTable1.setSelectionForeground(Color.BLACK);

        jTable1.setFocusable(false);

        // Header
        jTable1.getTableHeader().setFont(
                new Font("Segoe UI", Font.BOLD, 13)
        );

        jTable1.getTableHeader().setBackground(
                new Color(245, 247, 250)
        );

        jTable1.getTableHeader().setForeground(
                new Color(60, 60, 60)
        );

        jTable1.getTableHeader().setPreferredSize(
                new Dimension(0, 46)
        );

        jTable1.getTableHeader().setReorderingAllowed(false);

        // Zebra rows
        jTable1.setDefaultRenderer(
                Object.class,
                new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {

                Component c
                        = super.getTableCellRendererComponent(
                                table,
                                value,
                                isSelected,
                                hasFocus,
                                row,
                                column
                        );

                // Zebra row
                if (!isSelected) {

                    if (row % 2 == 0) {

                        c.setBackground(Color.WHITE);

                    } else {

                        c.setBackground(
                                new Color(248, 249, 250)
                        );
                    }
                }

                c.setForeground(new Color(40, 40, 40));

                // CENTER TEXT
                ((javax.swing.JLabel) c).setHorizontalAlignment(
                        javax.swing.SwingConstants.CENTER
                );

                // =========================
                // STATUS COLUMN
                // =========================
                if (column == 4) {

                    String status = value.toString();

                    c.setFont(
                            new Font(
                                    "Segoe UI",
                                    Font.BOLD,
                                    13
                            )
                    );

                    if (status.equals("Hoàn thành")) {

                        c.setForeground(
                                new Color(16, 185, 129)
                        );

                    } else if (status.equals("Đang xử lý")) {

                        c.setForeground(
                                new Color(245, 158, 11)
                        );

                    } else if (status.equals("Đã hủy")) {

                        c.setForeground(
                                new Color(239, 68, 68)
                        );
                    }
                }

                // =========================
                // MONEY COLUMN
                // =========================
                if (column == 3) {

                    c.setForeground(
                            new Color(59, 130, 246)
                    );

                    c.setFont(
                            new Font(
                                    "Segoe UI",
                                    Font.BOLD,
                                    13
                            )
                    );
                }

                // =========================
                // ORDER ID
                // =========================
                if (column == 0) {

                    c.setForeground(
                            new Color(79, 70, 229)
                    );

                    c.setFont(
                            new Font(
                                    "Segoe UI",
                                    Font.BOLD,
                                    13
                            )
                    );
                }

                // =========================
                // CUSTOMER
                // =========================
                if (column == 1) {

                    c.setForeground(
                            new Color(5, 150, 105)
                    );
                }

                // =========================
                // DATE
                // =========================
                if (column == 2) {

                    c.setForeground(
                            new Color(120, 113, 108)
                    );
                }

                setBorder(
                        BorderFactory.createEmptyBorder(
                                0, 10, 0, 10
                        )
                );

                return c;
            }
        });

        // =========================
        // SCROLLPANE
        // =========================
        tbOrder.setBorder(BorderFactory.createEmptyBorder());

        // =========================
        // COLUMN WIDTH
        // =========================
        jTable1.getColumnModel().getColumn(0).setPreferredWidth(120);
        jTable1.getColumnModel().getColumn(1).setPreferredWidth(150);
        jTable1.getColumnModel().getColumn(2).setPreferredWidth(120);
        jTable1.getColumnModel().getColumn(3).setPreferredWidth(140);
        jTable1.getColumnModel().getColumn(4).setPreferredWidth(140);

        // =========================
        // COMBOBOX
        // =========================
        cbStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbStatus.setPreferredSize(new Dimension(160, 36));

        // =========================
        // LABEL
        // =========================
        Status.setFont(new Font("Segoe UI", Font.BOLD, 13));
        Status.setForeground(new Color(70, 70, 70));

        // =========================
        // BUTTON STYLE
        // =========================
        styleButton(btnDetail,
                new Color(59, 130, 246));

        styleButton(btnUpdate,
                new Color(16, 185, 129));

        styleButton(btnIssueAnInvoice,
                new Color(239, 68, 68));
        jTable1.getColumnModel()
                .getColumn(4)
                .setCellRenderer(
                        new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table,
                            Object value,
                            boolean isSelected,
                            boolean hasFocus,
                            int row,
                            int column
                    ) {

                        Component c
                                = super.getTableCellRendererComponent(
                                        table,
                                        value,
                                        isSelected,
                                        hasFocus,
                                        row,
                                        column
                                );

                        String status
                                = value.toString();

                        setHorizontalAlignment(CENTER);

                        setOpaque(true);
                        setForeground(Color.WHITE);

                        if (status.equals("Hoàn thành")) {

                            c.setBackground(
                                    new Color(16, 185, 129)
                            );

                        } else if (status.equals("Đang xử lý")) {

                            c.setBackground(
                                    new Color(245, 158, 11)
                            );

                        } else if (status.equals("Đã hủy")) {

                            c.setBackground(
                                    new Color(239, 68, 68)
                            );
                        }

                        return c;
                    }
                });
        jTable1.getColumnModel()
                .getColumn(4)
                .setCellRenderer(
                        new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table,
                            Object value,
                            boolean isSelected,
                            boolean hasFocus,
                            int row,
                            int column
                    ) {

                        Component c
                                = super.getTableCellRendererComponent(
                                        table,
                                        value,
                                        isSelected,
                                        hasFocus,
                                        row,
                                        column
                                );

                        String status = value.toString();

                        setHorizontalAlignment(CENTER);

                        setOpaque(true);

                        if (isSelected) {

                            c.setBackground(
                                    table.getSelectionBackground()
                            );

                            c.setForeground(Color.BLACK);

                            return c;
                        }

                        setForeground(Color.WHITE);

                        if (status.equals("Hoàn thành")) {

                            c.setBackground(
                                    new Color(16, 185, 129)
                            );

                        } else if (status.equals("Đang xử lý")) {

                            c.setBackground(
                                    new Color(245, 158, 11)
                            );

                        } else if (status.equals("Đã hủy")) {

                            c.setBackground(
                                    new Color(239, 68, 68)
                            );
                        }

                        return c;
                    }
                });
        JTableHeader header = jTable1.getTableHeader();

        header.setDefaultRenderer(new TableCellRenderer() {

            private final DefaultTableCellRenderer renderer
                    = new DefaultTableCellRenderer();

            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {

                Component c = renderer.getTableCellRendererComponent(
                        table,
                        value,
                        isSelected,
                        hasFocus,
                        row,
                        column
                );

                c.setFont(new Font("Segoe UI", Font.BOLD, 13));
                c.setForeground(Color.WHITE);

                // Màu từng cột
                switch (column) {

                    case 0 ->
                        c.setBackground(
                                new Color(59, 130, 246)); // Mã đơn

                    case 1 ->
                        c.setBackground(
                                new Color(16, 185, 129)); // Khách hàng

                    case 2 ->
                        c.setBackground(
                                new Color(245, 158, 11)); // Ngày

                    case 3 ->
                        c.setBackground(
                                new Color(139, 92, 246)); // Tổng tiền

                    case 4 ->
                        c.setBackground(
                                new Color(239, 68, 68)); // Trạng thái
                }

                ((javax.swing.JLabel) c).setHorizontalAlignment(
                        javax.swing.SwingConstants.CENTER
                );

                return c;
            }
        });
    }

    private void initDateFilter() {
        dcFromDate = new JDateChooser();
        dcToDate = new JDateChooser();

        dcFromDate.setDateFormatString("dd/MM/yyyy");
        dcToDate.setDateFormatString("dd/MM/yyyy");

        // 1. Tinh chỉnh Font và kích thước bên trong Editor (JTextField)
        JTextField fromEditor = (JTextField) dcFromDate.getDateEditor().getUiComponent();
        fromEditor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fromEditor.setMargin(new java.awt.Insets(2, 5, 2, 5)); // Thêm khoảng cách cho chữ dễ nhìn

        JTextField toEditor = (JTextField) dcToDate.getDateEditor().getUiComponent();
        toEditor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        toEditor.setMargin(new java.awt.Insets(2, 5, 2, 5));

        // 2. Set kích thước tổng thể cho nguyên cục JDateChooser
        // Thay vì 400x36 như cũ (rất dài và hẹp), chuyển sang 200x38 (Vừa đủ hiển thị DD/MM/YYYY)
        dcFromDate.setPreferredSize(new Dimension(200, 38));
        dcFromDate.setMinimumSize(new Dimension(200, 38));

        dcToDate.setPreferredSize(new Dimension(200, 38));
        dcToDate.setMinimumSize(new Dimension(200, 38));

        // Font cho cái nút bấm xổ lịch xuống (nếu có)
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
            dcFromDate.setDate(null);
            dcToDate.setDate(null);
            cbStatus.setSelectedItem(STATUS_ALL);
            loadDataToTable();
        });

        // 3. Cấu hình GridBagConstraints cực kỳ quan trọng
        java.awt.GridBagConstraints gbc = new java.awt.GridBagConstraints();
        gbc.insets = new java.awt.Insets(5, 8, 5, 8);
        gbc.gridy = 0;

        // Căn lề trái cho các component, không cho nó tự động co giãn bậy bạ
        gbc.anchor = java.awt.GridBagConstraints.WEST;
        gbc.fill = java.awt.GridBagConstraints.NONE; // KHÔNG cho GridBag tự ép size

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
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn đầy đủ ngày bắt đầu và ngày kết thúc!"
            );
            return;
        }

        try {

            List<Order> allOrders
                    = OrdersSql.getInstance().selectAll();

            java.util.ArrayList<Order> filtered
                    = new java.util.ArrayList<>();

            for (Order o : allOrders) {

                java.util.Date orderDate
                        = java.sql.Date.valueOf(
                                o.getOrderDate().toString()
                        );

                if (!orderDate.before(from)
                        && !orderDate.after(to)) {

                    filtered.add(o);
                }
            }

            fillTable(filtered);

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi lọc theo ngày: " + ex.getMessage()
            );
        }
    }

    // ==========================================
    // Khởi tạo model bảng đúng cột theo DB
    // ==========================================
    private void initTableModel() {
        DefaultTableModel model = new DefaultTableModel(
                // ĐÃ XÓA "NHÂN VIÊN"
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
                new String[]{STATUS_ALL, "Đang xử lý", "Hoàn thành", "Đã huỷ"}
        ));
        cbStatus.setSelectedItem(STATUS_ALL);
    }

    // ==========================================
    // Load dữ liệu đơn hàng từ DB lên bảng (Đã ép Phân quyền)
    // ==========================================
    private void loadDataToTable() {
        try {
            // Lấy thông tin người đang đăng nhập (Tùy project của ông lưu ở đâu, tui ví dụ SessionManager)
            String role = business.service.SessionManager.getCurrentUser().getRoleId();
            String empId = business.service.SessionManager.getCurrentUser().getAccountId();

            // GỌI HÀM CÓ THAM SỐ THAY VÌ HÀM KHÔNG THAM SỐ CŨ
            List<Order> list = OrdersSql.getInstance().selectAll(role, empId);
            fillTable(list);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách đơn hàng: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // 2. Sửa hàm fillTable
    private void fillTable(List<Order> list) {
        DefaultTableModel model = (DefaultTableModel) jTable1.getModel();
        model.setRowCount(0);
        for (Order o : list) {
            model.addRow(new Object[]{
                o.getOrderId(),
                o.getCustomerId(),
                o.getOrderDate(),
                moneyFormat.format(o.getTotalAmount()) + " đ",
                getVietnameseStatus(o.getStatus())
            });
        }
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
        List<Map<String, Object>> details = OrderDetailsSql.getInstance().selectDetailRowsByOrderId(orderId);

        // Chặn đầu tiên: Lỡ câu SQL chạy ra rỗng thì báo luôn cho dễ debug
        if (details == null || details.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy chi tiết cho hóa đơn này!\n(Vui lòng kiểm tra lại câu query lấy Order Details)", "Trống", JOptionPane.WARNING_MESSAGE);
            return;
        }

        DefaultTableModel detailModel = new DefaultTableModel(
                new Object[]{"Mã SP", "Tên sản phẩm", "Số lượng", "Đơn giá", "Thành tiền"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Map<String, Object> detail : details) {
            // FIX BẪY 1: Đề phòng Oracle trả về key IN HOA
            Object pId = detail.get("product_id") != null ? detail.get("product_id") : detail.get("PRODUCT_ID");
            Object pName = detail.get("product_name") != null ? detail.get("product_name") : detail.get("PRODUCT_NAME");
            Object qty = detail.get("quantity") != null ? detail.get("quantity") : detail.get("QUANTITY");
            Object price = detail.get("unit_price") != null ? detail.get("unit_price") : detail.get("UNIT_PRICE");
            Object total = detail.get("line_total") != null ? detail.get("line_total") : detail.get("LINE_TOTAL");

            // FIX BẪY 2: Xử lý an toàn cho tiền tệ để tránh lỗi Crash ngầm
            String formattedPrice = "0";
            String formattedTotal = "0";
            try {
                if (price != null) {
                    formattedPrice = moneyFormat.format(price);
                }
                if (total != null) {
                    formattedTotal = moneyFormat.format(total);
                }
            } catch (Exception e) {
                formattedPrice = String.valueOf(price); // Nếu format lỗi thì in chay ra luôn
                formattedTotal = String.valueOf(total);
            }

            detailModel.addRow(new Object[]{
                pId,
                pName != null ? pName : pId, // Nếu không có tên thì hiện đỡ mã SP
                qty,
                formattedPrice,
                formattedTotal
            });
        }

        JTable detailTable = new JTable(detailModel);
        detailTable.setAutoCreateRowSorter(true);
        detailTable.setRowHeight(30); // Cho dòng cao lên dễ nhìn
        detailTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        // FIX BẪY 3: Ép kích thước khung nhìn cho ScrollPane
        JScrollPane scrollPane = new JScrollPane(detailTable);
        scrollPane.setPreferredSize(new Dimension(600, 300));

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "Chi tiết hóa đơn " + orderId,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void exportInvoice(String orderId) throws IOException {
        Order order = OrdersSql.getInstance().selectById(orderId);
        if (order == null) {
            throw new IOException("Khong tim thay hoa don: " + orderId);
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chon noi luu hoa don (PDF)");
        chooser.setSelectedFile(new java.io.File("HoaDon_" + orderId + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        List<Map<String, Object>> details = OrderDetailsSql.getInstance().selectDetailRowsByOrderId(orderId);

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
            document.add(new com.itextpdf.layout.element.Paragraph("Ngày: " + order.getOrderDate()));
            document.add(new com.itextpdf.layout.element.Paragraph("Trạng thái: " + order.getStatus()));
            document.add(new com.itextpdf.layout.element.Paragraph("Tổng tiền: " + moneyFormat.format(order.getTotalAmount()) + " VNĐ").setBold());
            document.add(new com.itextpdf.layout.element.Paragraph("\n"));

            com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(5);
            table.addHeaderCell("Mã SP");
            table.addHeaderCell("Tên sản phẩm");
            table.addHeaderCell("SL");
            table.addHeaderCell("Đơn giá");
            table.addHeaderCell("Thành tiền");

            for (Map<String, Object> detail : details) {
                table.addCell(detail.get("product_id").toString());
                table.addCell(detail.get("product_name") != null ? detail.get("product_name").toString() : detail.get("product_id").toString());
                table.addCell(detail.get("quantity").toString());
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

    // ==========================================
    // Hàm điều hướng Panel (Thống nhất với Dashboard)
    // ==========================================
    private void showPanel(javax.swing.JPanel panel) {
        java.awt.Window win = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (win instanceof javax.swing.JFrame frame) {
            frame.setContentPane(panel);
            frame.revalidate();
            frame.repaint();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
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

        Status.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        Status.setText("Trạng thái");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(8, 184, 0, 0);
        pnTop.add(Status, gridBagConstraints);

        cbStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Tất cả", "PROCESSING", "COMPLETED", "CANCELLED"}));
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
                new Object[][]{
                    {null, null, null, null, null},
                    {null, null, null, null, null},
                    {null, null, null, null, null},
                    {null, null, null, null, null}
                },
                new String[]{
                    "Mã đơn", "Khách hàng", "Ngày", "Tổng tiền", "Trạng thái"
                }
        ));
        tbOrder.setViewportView(jTable1);

        add(tbOrder, java.awt.BorderLayout.CENTER);

        pnButton.setBackground(new java.awt.Color(236, 240, 241));
        pnButton.setPreferredSize(new java.awt.Dimension(358, 70));
        pnButton.setLayout(new java.awt.GridBagLayout());

        btnDetail.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnDetail.setText("Xem chi tiết");
        btnDetail.addActionListener(this::btnDetailActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 44, 12, 0);
        pnButton.add(btnDetail, gridBagConstraints);

        btnUpdate.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnUpdate.setText("Cập nhật trạng thái");
        btnUpdate.addActionListener(this::btnUpdateActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(5, 6, 12, 0);
        pnButton.add(btnUpdate, gridBagConstraints);

        btnIssueAnInvoice.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
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
        // Lấy quyền của người đang đăng nhập (Ví dụ dùng SessionManager)
        String currentUserRole = business.service.SessionManager.getCurrentUser().getRoleId();

        // Nếu là Sale thì ẩn nút Cập nhật trạng thái đi
        if ("R_STAFF_SALE".equalsIgnoreCase(currentUserRole)) {
            btnUpdate.setVisible(false); // Nút cập nhật trạng thái
        }
    }// </editor-fold>                        

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {

        int row = getSelectedModelRow();

        if (row < 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn một đơn hàng!"
            );

            return;
        }

        String orderId
                = jTable1.getModel()
                        .getValueAt(row, 0)
                        .toString();

        String currentStatus
                = jTable1.getModel()
                        .getValueAt(row, 4)
                        .toString();

        String[] statuses = {
            "Đang xử lý",
            "Hoàn thành",
            "Đã hủy"
        };

        String newStatus
                = (String) JOptionPane.showInputDialog(
                        this,
                        "Chọn trạng thái mới",
                        "Cập nhật trạng thái",
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        statuses,
                        currentStatus
                );

        if (newStatus == null) {
            return;
        }

        try {

            String dbStatus
                    = mapStatusToDb(newStatus);

            // =========================
            // HỦY ĐƠN
            // =========================
            if (dbStatus.equals("CANCELLED")) {

                String reason
                        = JOptionPane.showInputDialog(
                                this,
                                "Nhập lý do hủy đơn:"
                        );

                if (reason == null
                        || reason.trim().isEmpty()) {

                    reason = "Quản lý hủy đơn";
                }

                boolean success
                        = business.service.PaymentService
                                .cancelOrder(orderId, reason);

                if (success) {

                    JOptionPane.showMessageDialog(
                            this,
                            "Đã hủy đơn hàng!"
                    );

                } else {

                    JOptionPane.showMessageDialog(
                            this,
                            "Không thể hủy đơn hàng!"
                    );
                }

            } else {

                int result
                        = OrdersSql.getInstance()
                                .updateStatus(
                                        orderId,
                                        dbStatus
                                );

                if (result > 0) {

                    JOptionPane.showMessageDialog(
                            this,
                            "Cập nhật trạng thái thành công!"
                    );

                } else {

                    JOptionPane.showMessageDialog(
                            this,
                            "Cập nhật thất bại!"
                    );
                }
            }

            loadDataToTable();

        } catch (Exception ex) {

            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi: " + ex.getMessage()
            );
        }
    }

    private void cbStatusActionPerformed(java.awt.event.ActionEvent evt) {

        String selected
                = cbStatus.getSelectedItem().toString();

        try {

            if (selected.equals(STATUS_ALL)) {

                loadDataToTable();
                return;
            }

            String dbStatus = switch (selected) {

                case "Đang xử lý" ->
                    "PROCESSING";

                case "Hoàn thành" ->
                    "COMPLETED";

                case "Đã hủy" ->
                    "CANCELLED";

                default ->
                    "";
            };

            fillTable(
                    OrdersSql.getInstance()
                            .selectByCondition(dbStatus)
            );

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi lọc hóa đơn: " + ex.getMessage()
            );
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

        // 1. Kiểm tra kỹ càng cả trường hợp null VÀ trường hợp chuỗi rỗng
        if (orderId == null || orderId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "⚠️ Vui lòng chọn một hóa đơn trong bảng để xem chi tiết!",
                    "Chưa chọn hóa đơn",
                    JOptionPane.WARNING_MESSAGE // Thêm icon tam giác vàng cảnh báo
            );
            return;
        }

        // 2. Gọi hàm show pop-up chi tiết (Nhớ trim() để cắt khoảng trắng thừa nếu có)
        showOrderDetailsDialog(orderId.trim());
    }

    private String getVietnameseStatus(String status) {

        return switch (status) {

            case "PROCESSING" ->
                "Đang xử lý";

            case "COMPLETED" ->
                "Hoàn thành";

            case "CANCELLED" ->
                "Đã hủy";

            default ->
                status;
        };
    }

    private String mapStatusToDb(String status) {

        return switch (status) {

            case "Đang xử lý" ->
                "PROCESSING";

            case "Hoàn thành" ->
                "COMPLETED";

            case "Đã hủy" ->
                "CANCELLED";

            default ->
                status;
        };
    }

    // Variables declaration - do not modify                     
    private javax.swing.JLabel Status;
    private javax.swing.JButton btnDetail;
    private javax.swing.JButton btnIssueAnInvoice;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JComboBox<String> cbStatus;
    private javax.swing.JTable jTable1;
    private javax.swing.JPanel pnButton;
    private javax.swing.JPanel pnTop;
    private javax.swing.JScrollPane tbOrder;
    // End of variables declaration                   
}
