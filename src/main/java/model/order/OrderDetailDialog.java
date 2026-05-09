package view;

import model.order.Order;
import model.order.Customer;
import java.awt.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.text.DecimalFormat;

public class OrderDetailDialog extends JDialog {

    private final DecimalFormat df = new DecimalFormat("#,##0 đ");
    private final Color primaryBlue = new Color(26, 35, 126);
    private final Color successGreen = new Color(46, 125, 50);
    private final Color errorRed = new Color(198, 40, 40);
    private final Color warningBg = new Color(255, 235, 238);

    public OrderDetailDialog(Frame parent, Order order, Customer customer, List<Map<String, Object>> details) {
        super(parent, "Chi Tiết Hóa Đơn: " + order.getOrderId(), true);
        setSize(850, 780);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // --- 1. HEADER HOÁ ĐƠN ---
        add(createHeaderPanel(order), BorderLayout.NORTH);

        // --- PHẦN THÂN ---
        JPanel pnlBody = new JPanel();
        pnlBody.setLayout(new BoxLayout(pnlBody, BoxLayout.Y_AXIS));
        pnlBody.setBorder(new EmptyBorder(15, 20, 15, 20));

        // --- KHỐI LÝ DO HỦY ---
        String status = (order.getStatus() != null) ? order.getStatus() : "";
        if (status.equalsIgnoreCase("Đã hủy") || status.equalsIgnoreCase("Đã huỷ")) {
            pnlBody.add(createCancellationPanel(order));
            pnlBody.add(Box.createVerticalStrut(15));
        }

        // --- THÔNG TIN KHÁCH HÀNG & ƯU ĐÃI ---
        pnlBody.add(createInfoGrid(customer, order));
        pnlBody.add(Box.createVerticalStrut(20));

        // --- BẢNG SẢN PHẨM ---
        pnlBody.add(createProductTable(details));
        pnlBody.add(Box.createVerticalStrut(20));

        // --- TỔNG KẾT ---
        pnlBody.add(createSummaryPanel(order, customer));

        add(pnlBody, BorderLayout.CENTER);

        // --- NÚT ĐÓNG ---
        add(createActionButtons(), BorderLayout.SOUTH);
    }

    private JPanel createCancellationPanel(Order order) {
        JPanel pnl = new JPanel(new BorderLayout(15, 5));
        pnl.setBackground(warningBg);
        pnl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(errorRed, 1),
                new EmptyBorder(12, 15, 12, 15)
        ));

        // 🌟 DÙNG ICON CHUẨN: OptionPane.errorIcon (Hình tròn đỏ gạch chéo) 
        // đảm bảo hiện được trên mọi máy
        JLabel lblIcon = new JLabel(UIManager.getIcon("OptionPane.errorIcon"));

        String reason = (order.getNote() != null && !order.getNote().isBlank())
                ? order.getNote()
                : "Hủy bởi Quản lý (Không có ghi chú)";

        JPanel pnlText = new JPanel(new GridLayout(2, 1, 0, 5));
        pnlText.setOpaque(false);
        JLabel lblTitle = new JLabel("HÓA ĐƠN NÀY ĐÃ BỊ HỦY");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(errorRed);
        JLabel lblReason = new JLabel("<html><b>Lý do hủy:</b> <font color='#c62828'>" + reason + "</font></html>");
        lblReason.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        pnlText.add(lblTitle);
        pnlText.add(lblReason);

        pnl.add(lblIcon, BorderLayout.WEST);
        pnl.add(pnlText, BorderLayout.CENTER);
        return pnl;
    }

    private JPanel createHeaderPanel(Order order) {
        JPanel pnl = new JPanel(new GridLayout(1, 2, 10, 10));
        pnl.setBackground(primaryBlue);
        pnl.setBorder(new EmptyBorder(15, 25, 15, 25));

        JPanel pnlLeft = new JPanel(new GridLayout(2, 1));
        pnlLeft.setOpaque(false);
        JLabel lblId = new JLabel("MÃ HÓA ĐƠN: " + order.getOrderId());
        lblId.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblId.setForeground(Color.WHITE);
        JLabel lblDate = new JLabel("Ngày tạo: " + order.getOrderDate());
        lblDate.setForeground(new Color(200, 200, 200));
        pnlLeft.add(lblId);
        pnlLeft.add(lblDate);

        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlRight.setOpaque(false);
        String statusText = (order.getStatus() != null) ? order.getStatus().toUpperCase() : "KHÔNG XÁC ĐỊNH";
        JLabel lblStatus = new JLabel(statusText);
        lblStatus.setOpaque(true);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatus.setBorder(new EmptyBorder(5, 15, 5, 15));

        if (statusText.contains("HOÀN THÀNH")) {
            lblStatus.setBackground(new Color(232, 245, 233));
            lblStatus.setForeground(successGreen);
        } else if (statusText.contains("HỦY") || statusText.contains("HUỶ")) {
            lblStatus.setBackground(new Color(255, 235, 238));
            lblStatus.setForeground(errorRed);
        } else {
            lblStatus.setBackground(new Color(255, 243, 205));
            lblStatus.setForeground(new Color(133, 100, 4));
        }

        JLabel lblPrefix = new JLabel("Trạng thái:  ");
        lblPrefix.setForeground(Color.WHITE);
        pnlRight.add(lblPrefix);
        pnlRight.add(lblStatus);

        pnl.add(pnlLeft);
        pnl.add(pnlRight);
        return pnl;
    }

    private JPanel createInfoGrid(Customer cus, Order order) {
        JPanel pnl = new JPanel(new GridLayout(1, 2, 20, 0));
        pnl.setOpaque(false);

        // --- Block Khách hàng (Giữ nguyên) ---
        JPanel pnlCus = new JPanel(new GridLayout(4, 1, 5, 5));
        pnlCus.setBorder(TitledBorder("THÔNG TIN KHÁCH HÀNG"));
        if (cus != null) {
            pnlCus.add(new JLabel("👤 Khách: " + cus.getCustomerName()));
            pnlCus.add(new JLabel("🆔 Mã KH: " + cus.getCustomerId()));
            pnlCus.add(new JLabel("🏆 Hạng: " + getRankEmoji(cus.getMemberRank()) + " " + cus.getMemberRank()));
            pnlCus.add(new JLabel("💰 Tổng chi: " + df.format(cus.getTotalSpending())));
        } else {
            pnlCus.add(new JLabel("👤 Khách vãng lai"));
            pnlCus.add(new JLabel("🆔 Mã KH: -"));
            pnlCus.add(new JLabel("🏆 Hạng: -"));
            pnlCus.add(new JLabel("💰 Tổng chi: 0 đ"));
        }

        // --- Block Ưu đãi & Nhân viên ---
        JPanel pnlPromo = new JPanel(new GridLayout(4, 1, 5, 5));
        pnlPromo.setBorder(TitledBorder("ƯU ĐÃI & NHÂN VIÊN"));
        double discountRate = (cus != null) ? cus.getDiscountRate() : 0;
        pnlPromo.add(new JLabel("🎁 Mức giảm giá: " + (int) (discountRate * 100) + "%"));
        pnlPromo.add(new JLabel("💳 Thanh toán: " + (order.getPaymentMethodId() != null ? order.getPaymentMethodId() : "Tiền mặt")));

        // 🌟 HIỂN THỊ ƯU TIÊN: Tên Nhân Viên (Mã ID)
        String rawEmpId = order.getEmployeeId();
        String finalDisplay;

        // Kiểm tra nếu ID là rác hoặc null
        if (rawEmpId == null || rawEmpId.trim().isEmpty() || rawEmpId.equalsIgnoreCase("null")) {
            finalDisplay = "Hệ thống tự động";
        } else {
            String name = getEmployeeDisplayName(rawEmpId);
            String cleanId = rawEmpId.trim();

            // Nếu tìm được tên thật, hiển thị: Lê Tấn Vĩ (ACC171...)
            // Nếu không tìm được tên (trả về ID), thì chỉ hiển thị ID để tránh lặp (ID (ID))
            if (name.equalsIgnoreCase(cleanId)) {
                finalDisplay = cleanId;
            } else {
                finalDisplay = name + " (" + cleanId + ")";
            }
        }

        pnlPromo.add(new JLabel("<html>👷 <b>Người lên đơn:</b> " + finalDisplay + "</html>"));

        pnl.add(pnlCus);
        pnl.add(pnlPromo);
        return pnl;
    }

    /**
     * Tra cứu Tên thực tế từ ID (Logic bắc cầu: Account -> Employee)
     */
    /**
     * Tra cứu Tên thực tế từ ID (Logic bắc cầu: Account -> Employee)
     */
    /**
     * Tra cứu Tên thực tế từ ID (Ưu tiên tối đa việc tìm Tên trong bảng
     * Employee)
     */
    private String getEmployeeDisplayName(String inputId) {
        // 1. Lọc sạch dữ liệu rác
        if (inputId == null || inputId.trim().isEmpty() || inputId.trim().equalsIgnoreCase("null")) {
            return "Hệ thống";
        }

        String idToSearch = inputId.trim();

        try {
            // Bước 1: Giả định idToSearch là mã Nhân viên (EMP...), tìm trực tiếp tên
            model.employee.Employee empDirect = business.sql.hr_kpi.EmployeeSql.getInstance().selectById(idToSearch);
            if (empDirect != null && empDirect.getEmployeeName() != null && !empDirect.getEmployeeName().isBlank()) {
                return empDirect.getEmployeeName();
            }

            // Bước 2: Nếu không thấy, có thể idToSearch là mã Tài khoản (ACC...). 
            // Tìm tài khoản để lấy USER_ID (Mã nhân viên liên kết)
            model.account.Account acc = business.sql.rbac.AccountSql.getInstance().selectById(idToSearch);
            if (acc != null) {
                String linkedUserId = (acc.getUserId() != null) ? acc.getUserId().trim() : "";

                // Nếu tài khoản này có link với mã nhân viên, dùng mã đó tìm Tên thật lần nữa
                if (!linkedUserId.isEmpty() && !linkedUserId.equalsIgnoreCase("null")) {
                    model.employee.Employee empLinked = business.sql.hr_kpi.EmployeeSql.getInstance().selectById(linkedUserId);
                    if (empLinked != null && empLinked.getEmployeeName() != null) {
                        return empLinked.getEmployeeName(); // Trả về tên thật (Lê Tấn Vĩ)
                    }
                }

                // Nếu tài khoản Admin thuần túy (không link nhân viên) thì mới hiện Username
                if (acc.getUsername() != null) {
                    return acc.getUsername();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Nếu không tìm thấy bất cứ tên nào, trả về ID gốc
        return idToSearch;
    }

    private JScrollPane createProductTable(List<Map<String, Object>> details) {
        String[] cols = {"Mã SP", "Tên Sản Phẩm", "SL", "Đơn Giá", "Thành Tiền"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Map<String, Object> d : details) {
            Object pId = d.get("product_id") != null ? d.get("product_id") : d.get("PRODUCT_ID");
            Object pName = d.get("product_name") != null ? d.get("product_name") : d.get("PRODUCT_NAME");
            Object qty = d.get("quantity") != null ? d.get("quantity") : d.get("QUANTITY");
            Object price = d.get("unit_price") != null ? d.get("unit_price") : d.get("UNIT_PRICE");
            Object total = d.get("line_total") != null ? d.get("line_total") : d.get("LINE_TOTAL");

            String strPrice = "0 đ";
            String strTotal = "0 đ";
            try {
                strPrice = df.format(Double.parseDouble(price.toString()));
            } catch (Exception e) {
            }
            try {
                strTotal = df.format(Double.parseDouble(total.toString()));
            } catch (Exception e) {
            }

            model.addRow(new Object[]{pId, pName != null ? pName : pId, qty, strPrice, strTotal});
        }

        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        table.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(rightRenderer);

        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);

        return new JScrollPane(table);
    }

    private JPanel createSummaryPanel(Order order, Customer cus) {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.EAST;

        double discountRate = (cus != null) ? cus.getDiscountRate() : 0;
        double totalToPay = order.getTotalAmount();
        double subTotal = (discountRate > 0 && discountRate < 1) ? totalToPay / (1 - discountRate) : totalToPay;
        double discountAmount = subTotal - totalToPay;

        addSummaryRow(pnl, gbc, 0, "Tạm tính:", df.format(subTotal), Color.BLACK, 14);
        addSummaryRow(pnl, gbc, 1, "Giảm giá thành viên:", "- " + df.format(discountAmount), successGreen, 14);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnl.add(new JSeparator(), gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        addSummaryRow(pnl, gbc, 3, "TỔNG THANH TOÁN:", df.format(totalToPay), errorRed, 22);

        return pnl;
    }

    private void addSummaryRow(JPanel pnl, GridBagConstraints gbc, int y, String label, String value, Color color, int size) {
        gbc.gridy = y;
        gbc.gridx = 0;
        JLabel lblLabel = new JLabel(label);
        lblLabel.setFont(new Font("Segoe UI", Font.PLAIN, size - 2));
        pnl.add(lblLabel, gbc);

        gbc.gridx = 1;
        JLabel lblVal = new JLabel(value);
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, size));
        lblVal.setForeground(color);
        pnl.add(lblVal, gbc);
    }

    private JPanel createActionButtons() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        pnl.setBackground(new Color(245, 245, 245));

        JButton btnClose = new JButton("Đóng cửa sổ");
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());

        pnl.add(btnClose);
        return pnl;
    }

    private TitledBorder TitledBorder(String title) {
        TitledBorder b = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), title);
        b.setTitleFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setTitleColor(primaryBlue);
        return b;
    }

    private String getRankEmoji(String rank) {
        if (rank == null) {
            return "⚪";
        }
        switch (rank.toLowerCase()) {
            case "vàng":
                return "🥇";
            case "bạc":
                return "🥈";
            case "đồng":
                return "🥉";
            case "kim cương":
                return "💎";
            default:
                return "👤";
        }
    }
}
