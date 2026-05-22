package view;

import business.sql.sales_order.DeliveryManagementSql;
import model.order.Order;
import model.order.Customer;
import java.awt.*;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.text.DecimalFormat;
import view.components.IconHelper;


public class OrderDetailDialog extends JDialog {

    private final DecimalFormat df = new DecimalFormat("#,##0 đ");
    private final Color primaryBlue = new Color(26, 35, 126);
    private final Color successGreen = new Color(46, 125, 50);
    private final Color errorRed = new Color(198, 40, 40);
    private final Color warningBg = new Color(255, 235, 238);

    // 🌟 BIẾN TOÀN CỤC ĐỂ DỄ DÀNG CẬP NHẬT TỔNG TIỀN
    private DefaultTableModel modOrderDetail;
    private JLabel lblSubTotalVal;
    private JLabel lblDiscountVal;
    private JLabel lblTotalPayVal;
    private double currentDiscountRate = 0;
    private boolean isCancelled = false;

    public OrderDetailDialog(Frame parent, Order order, Customer customer, List<Map<String, Object>> details) {
        super(parent, "Chi Tiết Hóa Đơn: " + order.getOrderId(), true);
        setSize(850, 850); // Tăng height lên một chút để nhét thêm thẻ Giao Hàng
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        if (customer != null) {
            this.currentDiscountRate = customer.getDiscountRate();
        }

        String status = (order.getStatus() != null) ? order.getStatus() : "";
        this.isCancelled = status.equalsIgnoreCase("Đã hủy") || status.equalsIgnoreCase("Đã huỷ");

        // --- 1. HEADER HOÁ ĐƠN ---
        add(createHeaderPanel(order), BorderLayout.NORTH);

        // --- PHẦN THÂN ---
        JPanel pnlBody = new JPanel();
        pnlBody.setLayout(new BoxLayout(pnlBody, BoxLayout.Y_AXIS));
        pnlBody.setBorder(new EmptyBorder(15, 20, 15, 20));

        // --- KHỐI LÝ DO HỦY ---
        if (isCancelled) {
            pnlBody.add(createCancellationPanel(order));
            pnlBody.add(Box.createVerticalStrut(15));
        }

        // --- THÔNG TIN KHÁCH HÀNG & ƯU ĐÃI ---
        pnlBody.add(createInfoGrid(customer, order));
        pnlBody.add(Box.createVerticalStrut(15));

        // --- 🌟 THÔNG TIN GIAO HÀNG ĐƯỢC NHÚNG VÀO ĐÂY ---
        pnlBody.add(createDeliveryCard(order.getOrderId()));
        pnlBody.add(Box.createVerticalStrut(20));

        // --- BẢNG SẢN PHẨM ---
        pnlBody.add(createProductTable(details));
        pnlBody.add(Box.createVerticalStrut(20));

        // --- TỔNG KẾT ---
        pnlBody.add(createSummaryPanel());

        // Bọc phần thân vào JScrollPane để chống vỡ khung nếu dữ liệu dài
        JScrollPane scrollPane = new JScrollPane(pnlBody);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // --- NÚT ĐÓNG / LƯU ---
        add(createActionButtons(), BorderLayout.SOUTH);
    }

    private JPanel createCancellationPanel(Order order) {
        JPanel pnl = new JPanel(new BorderLayout(15, 5));
        pnl.setBackground(warningBg);
        pnl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(errorRed, 1),
                new EmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblIcon = new JLabel(UIManager.getIcon("OptionPane.errorIcon"));
        String reason = (order.getNote() != null && !order.getNote().isBlank())
                ? order.getNote()
                : "Hủy bởi Quản lý (Không có ghi chú)";

        JPanel pnlText = new JPanel(new GridLayout(2, 1, 0, 5));
        pnlText.setOpaque(false);
        JLabel lblTitle = new JLabel("HÓA ĐƠN NÀY ĐÃ BỊ HỦY");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(errorRed);
        JLabel lblReason = new JLabel("<html><b><font color='#c62828'>" + reason + "</b></font></html>");
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

        // --- Block Khách hàng ---
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
        pnlPromo.add(new JLabel("🎁 Mức giảm giá: " + (int) (currentDiscountRate * 100) + "%"));
        pnlPromo.add(new JLabel("💳 Thanh toán: " + (order.getPaymentMethodId() != null ? order.getPaymentMethodId() : "Tiền mặt")));

        String rawEmpId = order.getEmployeeId();
        String finalDisplay = (rawEmpId == null || rawEmpId.trim().isEmpty() || rawEmpId.equalsIgnoreCase("null")) 
                ? "Hệ thống tự động" : rawEmpId.trim();

        pnlPromo.add(new JLabel("<html>👷 <b>Người lên đơn:</b> " + finalDisplay + "</html>"));

        pnl.add(pnlCus);
        pnl.add(pnlPromo);
        return pnl;
    }

    // =========================================================================
    // 🔥 HÀM MỚI ĐƯỢC THÊM VÀO ĐỂ HIỂN THỊ GIAO HÀNG
    // =========================================================================
    private JPanel createDeliveryCard(String orderId) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(TitledBorder("THÔNG TIN GIAO NHẬN"));
        
        // Gọi SQL lấy data
        Map<String, String> data = null;
        try {
            data = DeliveryManagementSql.getInstance().getDeliveryInfoByOrderId(orderId);
        } catch (Exception e) {
            System.err.println("Lỗi hoặc chưa có hàm SQL Giao hàng: " + e.getMessage());
        }

        if (data == null || data.isEmpty()) {
            JPanel emptyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            emptyPanel.setBackground(Color.WHITE);
            emptyPanel.add(new JLabel("<html><i>Đơn hàng này mua trực tiếp, không sử dụng dịch vụ giao nhận.</i></html>"));
            card.add(emptyPanel, BorderLayout.CENTER);
            return card;
        }

        JPanel grid = new JPanel(new GridBagLayout());
        grid.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 20);

        String loai = data.get("loai_giao_hang");
        String status = data.get("status") != null ? data.get("status") : "Đang chờ xử lý";

        // Dòng 1: Hình thức và Trạng thái
        gbc.gridy = 0; gbc.gridx = 0;
        grid.add(new JLabel("<html><span style='color:#718096;'>Hình thức:</span> <b>" + loai + "</b></html>"), gbc);
        
        gbc.gridx = 1;
        grid.add(new JLabel("<html><span style='color:#718096;'>Trạng thái:</span> <b style='color:#E63946;'>" + status + "</b></html>"), gbc);

        // Dòng 2: Phân loại theo Schema để hiện trường thích hợp
        gbc.gridy = 1; gbc.gridx = 0; gbc.gridwidth = 2;
        if ("Giao hàng tận nơi".equals(loai)) {
            String feeStr = (data.get("fee") != null) ? df.format(Double.parseDouble(data.get("fee"))) : "0 đ";
            String shipInfo = "📍 Địa chỉ: <b>" + data.get("address") + "</b><br>📞 SĐT nhận: " + data.get("phone") + " &nbsp;&nbsp;|&nbsp;&nbsp; 💰 Phí ship: <b style='color:#10B981;'>" + feeStr + "</b>";
            grid.add(new JLabel("<html>" + shipInfo + "</html>"), gbc);
            
        } else if ("Lấy tại tủ khóa".equals(loai)) {
            grid.add(new JLabel("<html>📦 Mã tủ Locker: <b style='color:#365CF5;'>" + data.get("locker") + "</b> &nbsp;&nbsp;|&nbsp;&nbsp; ⏰ Giờ hẹn lấy: <b>" + data.get("time") + "</b></html>"), gbc);
            
        } else if ("Nhận tại quầy".equals(loai)) {
            grid.add(new JLabel("<html>🏪 Vị trí quầy tiếp nhận: <b style='color:#365CF5;'>" + data.get("counter") + "</b></html>"), gbc);
        }

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JScrollPane createProductTable(List<Map<String, Object>> details) {
        String[] cols = {"Mã SP", "Tên Sản Phẩm", "SL", "Đơn Giá", "Thành Tiền"};
        
        // 🌟 BẢNG CHỈ CHO PHÉP SỬA CỘT SỐ LƯỢNG (Và không cho sửa nếu đơn đã hủy)
        modOrderDetail = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2 && !isCancelled; 
            }
        };

        for (Map<String, Object> d : details) {
            Object pId = d.get("product_id") != null ? d.get("product_id") : d.get("PRODUCT_ID");
            Object pName = d.get("product_name") != null ? d.get("product_name") : d.get("PRODUCT_NAME");
            Object qty = d.get("quantity") != null ? d.get("quantity") : d.get("QUANTITY");
            Object price = d.get("unit_price") != null ? d.get("unit_price") : d.get("UNIT_PRICE");
            Object total = d.get("line_total") != null ? d.get("line_total") : d.get("LINE_TOTAL");

            double dPrice = 0, dTotal = 0;
            try { dPrice = Double.parseDouble(price.toString()); } catch (Exception e) {}
            try { dTotal = Double.parseDouble(total.toString()); } catch (Exception e) {}

            modOrderDetail.addRow(new Object[]{pId, pName != null ? pName : pId, qty, dPrice, dTotal});
        }

        JTable table = new JTable(modOrderDetail);
        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));

        // 🌟 GẮN RENDERER HIỂN THỊ TIỀN TỆ CHO CỘT ĐƠN GIÁ VÀ THÀNH TIỀN
        DefaultTableCellRenderer moneyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Number) {
                    value = df.format(value);
                }
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.RIGHT);
                return c;
            }
        };
        table.getColumnModel().getColumn(3).setCellRenderer(moneyRenderer);
        table.getColumnModel().getColumn(4).setCellRenderer(moneyRenderer);

        // 🌟 GẮN NÚT TĂNG GIẢM VÀO CỘT SỐ LƯỢNG
        if (!isCancelled) {
            table.getColumnModel().getColumn(2).setCellRenderer(new SpinnerRenderer());
            table.getColumnModel().getColumn(2).setCellEditor(new SpinnerEditor());
        }

        table.getColumnModel().getColumn(0).setPreferredWidth(80);
        table.getColumnModel().getColumn(1).setPreferredWidth(250);

        // 🌟 LẮNG NGHE SỰ KIỆN KHI NGƯỜI DÙNG TĂNG GIẢM SỐ LƯỢNG
        modOrderDetail.addTableModelListener(e -> {
            if (e.getColumn() == 2 && e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                if (row < 0 || row >= modOrderDetail.getRowCount()) return;

                try {
                    int newQty = Integer.parseInt(modOrderDetail.getValueAt(row, 2).toString());

                    if (newQty <= 0) {
                        SwingUtilities.invokeLater(() -> {
                            int confirm = JOptionPane.showConfirmDialog(this, "Xóa sản phẩm này khỏi đơn hàng?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION);
                            if (confirm == JOptionPane.YES_OPTION) {
                                modOrderDetail.removeRow(row);
                            } else {
                                modOrderDetail.setValueAt(1, row, 2);
                            }
                            calculateOrderTotal();
                        });
                        return;
                    }

                    double price = Double.parseDouble(modOrderDetail.getValueAt(row, 3).toString());
                    modOrderDetail.setValueAt(price * newQty, row, 4);
                    calculateOrderTotal();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(0, 250)); // Set cứng chiều cao bảng để nhường chỗ cho các thẻ trên
        return sp;
    }

    private JPanel createSummaryPanel() {
        JPanel pnl = new JPanel(new GridBagLayout());
        pnl.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.EAST;

        lblSubTotalVal = new JLabel("0 đ");
        lblSubTotalVal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblDiscountVal = new JLabel("0 đ");
        lblDiscountVal.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblDiscountVal.setForeground(successGreen);
        lblTotalPayVal = new JLabel("0 đ");
        lblTotalPayVal.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTotalPayVal.setForeground(errorRed);

        // Hàng 1
        gbc.gridy = 0; gbc.gridx = 0;
        JLabel lbl1 = new JLabel("Tạm tính:"); lbl1.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pnl.add(lbl1, gbc);
        gbc.gridx = 1; pnl.add(lblSubTotalVal, gbc);

        // Hàng 2
        gbc.gridy = 1; gbc.gridx = 0;
        JLabel lbl2 = new JLabel("Giảm giá thành viên:"); lbl2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pnl.add(lbl2, gbc);
        gbc.gridx = 1; pnl.add(lblDiscountVal, gbc);

        // Kẻ vạch
        gbc.gridy = 2; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        pnl.add(new JSeparator(), gbc);

        // Hàng 3
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;
        gbc.gridy = 3; gbc.gridx = 0;
        JLabel lbl3 = new JLabel("TỔNG THANH TOÁN:"); lbl3.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        pnl.add(lbl3, gbc);
        gbc.gridx = 1; pnl.add(lblTotalPayVal, gbc);

        calculateOrderTotal();

        return pnl;
    }

    private void calculateOrderTotal() {
        double subTotal = 0;
        for (int i = 0; i < modOrderDetail.getRowCount(); i++) {
            subTotal += Double.parseDouble(modOrderDetail.getValueAt(i, 4).toString());
        }

        double discountAmount = subTotal * currentDiscountRate;
        double totalToPay = subTotal - discountAmount;

        if (lblSubTotalVal != null) lblSubTotalVal.setText(df.format(subTotal));
        if (lblDiscountVal != null) lblDiscountVal.setText("- " + df.format(discountAmount));
        if (lblTotalPayVal != null) lblTotalPayVal.setText(df.format(totalToPay));
    }

    private JPanel createActionButtons() {
        JPanel pnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        pnl.setBackground(new Color(245, 245, 245));

        JButton btnClose = new JButton("Đóng cửa sổ");
        btnClose.setIcon(IconHelper.close(18));
        btnClose.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnClose.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClose.addActionListener(e -> dispose());

        JButton btnSave = new JButton("Lưu Thay Đổi");
        btnSave.setIcon(IconHelper.save(20));
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setBackground(primaryBlue);
        btnSave.setForeground(Color.WHITE);
        btnSave.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSave.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Tính năng lưu thay đổi đơn hàng đang được phát triển!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });

        if (!isCancelled) {
            pnl.add(btnSave);
        }
        
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
        if (rank == null) return "⚪";
        switch (rank.toLowerCase()) {
            case "vàng": return "🥇";
            case "bạc": return "🥈";
            case "đồng": return "🥉";
            case "kim cương": return "💎";
            default: return "👤";
        }
    }

    class SpinnerEditor extends DefaultCellEditor {
        private JSpinner spinner;
        public SpinnerEditor() {
            super(new JTextField());
            setClickCountToStart(1); 
            spinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
            spinner.setBorder(null);
            JComponent editor = spinner.getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                ((JSpinner.DefaultEditor) editor).getTextField().setHorizontalAlignment(JTextField.CENTER);
                ((JSpinner.DefaultEditor) editor).getTextField().setFont(new Font("Segoe UI", Font.BOLD, 14));
            }
        }
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            spinner.setValue(Integer.parseInt(value.toString()));
            return spinner;
        }
        @Override public Object getCellEditorValue() { return spinner.getValue(); }
    }

    class SpinnerRenderer extends JSpinner implements javax.swing.table.TableCellRenderer {
        public SpinnerRenderer() {
            super(new SpinnerNumberModel(1, 0, 9999, 1));
            setBorder(null);
            JComponent editor = getEditor();
            if (editor instanceof JSpinner.DefaultEditor) {
                ((JSpinner.DefaultEditor) editor).getTextField().setHorizontalAlignment(JTextField.CENTER);
                ((JSpinner.DefaultEditor) editor).getTextField().setFont(new Font("Segoe UI", Font.BOLD, 14));
            }
        }
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value != null) setValue(Integer.parseInt(value.toString()));
            setBackground(isSelected ? new Color(184, 218, 255) : Color.WHITE);
            return this;
        }
    }
}