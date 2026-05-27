package view;

import business.service.InvoiceUpdateService;
import business.sql.prod_inventory.ProductUnitsSql;
import business.sql.sales_order.DeliveryManagementSql;
import model.order.Order;
import model.order.Customer;
import model.product.ProductUnit;

import java.awt.*;
import java.math.BigDecimal;
import java.sql.Connection;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.event.TableModelEvent;

import view.components.IconHelper;

public class OrderDetailDialog extends JDialog {

    private static final int COL_DETAIL_ID = 0;
    private static final int COL_PRODUCT_ID = 1;
    private static final int COL_PRODUCT_NAME = 2;
    private static final int COL_UNIT = 3;
    private static final int COL_QTY = 4;
    private static final int COL_UNIT_PRICE = 5;
    private static final int COL_TOTAL = 6;

    private final DecimalFormat df = new DecimalFormat("#,##0 đ");

    private final Color primaryBlue = new Color(26, 35, 126);
    private final Color successGreen = new Color(46, 125, 50);
    private final Color errorRed = new Color(198, 40, 40);
    private final Color warningBg = new Color(255, 235, 238);
    private final Color headerBg = new Color(26, 35, 126);
    private final Color lightBg = new Color(248, 250, 252);
    private final Color borderGray = new Color(209, 213, 219);

    private DefaultTableModel modOrderDetail;
    private JTable tableChiTiet;

    private JLabel lblSubTotalVal;
    private JLabel lblDiscountVal;
    private JLabel lblTotalPayVal;

    private double currentDiscountRate = 0;
    private boolean isCancelled = false;
    private boolean isUpdatingTotal = false;

    private final Connection conn;
    private final String orderId;
    private final Runnable onSavedCallback;

    private final Map<String, List<ProductUnit>> productUnitCache = new HashMap<>();

    public OrderDetailDialog(
            Frame parent,
            Connection conn,
            Order order,
            Customer customer,
            List<Map<String, Object>> details,
            Runnable onSavedCallback
    ) {
        super(parent, "Chi Tiết Hóa Đơn: " + order.getOrderId(), true);

        this.conn = conn;
        this.orderId = order.getOrderId();
        this.onSavedCallback = onSavedCallback;

        setSize(920, 850);
        setMinimumSize(new Dimension(880, 720));
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        if (customer != null) {
            this.currentDiscountRate = customer.getDiscountRate();
        }

        String status = order.getStatus() != null ? order.getStatus() : "";
        this.isCancelled = status.equalsIgnoreCase("Đã hủy")
                || status.equalsIgnoreCase("Đã huỷ")
                || status.equalsIgnoreCase("CANCELLED");

        add(createHeaderPanel(order), BorderLayout.NORTH);

        JPanel pnlBody = new JPanel();
        pnlBody.setLayout(new BoxLayout(pnlBody, BoxLayout.Y_AXIS));
        pnlBody.setBackground(Color.WHITE);
        pnlBody.setBorder(new EmptyBorder(15, 20, 15, 20));

        if (isCancelled) {
            pnlBody.add(createCancellationPanel(order));
            pnlBody.add(Box.createVerticalStrut(15));
        }

        pnlBody.add(createInfoGrid(customer, order));
        pnlBody.add(Box.createVerticalStrut(15));
        pnlBody.add(createDeliveryCard(order.getOrderId()));
        pnlBody.add(Box.createVerticalStrut(18));
        pnlBody.add(createUnitHelpPanel());
        pnlBody.add(Box.createVerticalStrut(10));
        pnlBody.add(createProductTable(details));
        pnlBody.add(Box.createVerticalStrut(20));
        pnlBody.add(createSummaryPanel());

        JScrollPane scrollPane = new JScrollPane(pnlBody);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

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

        String reason = order.getNote() != null && !order.getNote().isBlank()
                ? order.getNote()
                : "Hủy bởi Quản lý (Không có ghi chú)";

        JPanel pnlText = new JPanel(new GridLayout(2, 1, 0, 5));
        pnlText.setOpaque(false);

        JLabel lblTitle = new JLabel("HÓA ĐƠN NÀY ĐÃ BỊ HỦY");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblTitle.setForeground(errorRed);

        JLabel lblReason = new JLabel("<html><b><font color='#c62828'>" + reason + "</font></b></html>");
        lblReason.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        pnlText.add(lblTitle);
        pnlText.add(lblReason);

        pnl.add(lblIcon, BorderLayout.WEST);
        pnl.add(pnlText, BorderLayout.CENTER);

        return pnl;
    }

    private JPanel createHeaderPanel(Order order) {
        JPanel pnl = new JPanel(new GridLayout(1, 2, 10, 10));
        pnl.setBackground(headerBg);
        pnl.setBorder(new EmptyBorder(15, 25, 15, 25));

        JPanel pnlLeft = new JPanel(new GridLayout(2, 1));
        pnlLeft.setOpaque(false);

        JLabel lblId = new JLabel("MÃ HÓA ĐƠN: " + order.getOrderId());
        lblId.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblId.setForeground(Color.WHITE);

        JLabel lblDate = new JLabel("Ngày tạo: " + order.getOrderDate());
        lblDate.setForeground(new Color(220, 220, 220));

        pnlLeft.add(lblId);
        pnlLeft.add(lblDate);

        JPanel pnlRight = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        pnlRight.setOpaque(false);

        String statusText = order.getStatus() != null ? order.getStatus().toUpperCase() : "KHÔNG XÁC ĐỊNH";

        JLabel lblStatus = new JLabel(statusText);
        lblStatus.setOpaque(true);
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatus.setBorder(new EmptyBorder(5, 15, 5, 15));

        if (statusText.contains("HOÀN THÀNH") || statusText.contains("COMPLETED")) {
            lblStatus.setBackground(new Color(232, 245, 233));
            lblStatus.setForeground(successGreen);
        } else if (statusText.contains("HỦY") || statusText.contains("HUỶ") || statusText.contains("CANCEL")) {
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

        JPanel pnlCus = new JPanel(new GridLayout(4, 1, 5, 5));
        pnlCus.setBackground(Color.WHITE);
        pnlCus.setBorder(createTitledBorder("THÔNG TIN KHÁCH HÀNG"));

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

        JPanel pnlPromo = new JPanel(new GridLayout(4, 1, 5, 5));
        pnlPromo.setBackground(Color.WHITE);
        pnlPromo.setBorder(createTitledBorder("ƯU ĐÃI & NHÂN VIÊN"));

        pnlPromo.add(new JLabel("🎁 Mức giảm giá: " + (int) (currentDiscountRate * 100) + "%"));
        pnlPromo.add(new JLabel("💳 Thanh toán: "
                + (order.getPaymentMethodId() != null ? order.getPaymentMethodId() : "Tiền mặt")));

        String rawEmpId = order.getEmployeeId();
        String finalDisplay = rawEmpId == null
                || rawEmpId.trim().isEmpty()
                || rawEmpId.equalsIgnoreCase("null")
                ? "Hệ thống tự động"
                : rawEmpId.trim();

        pnlPromo.add(new JLabel("<html>👷 <b>Người lên đơn:</b> " + finalDisplay + "</html>"));

        pnl.add(pnlCus);
        pnl.add(pnlPromo);

        return pnl;
    }

    private JPanel createDeliveryCard(String orderId) {
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBackground(Color.WHITE);
        card.setBorder(createTitledBorder("THÔNG TIN GIAO NHẬN"));

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

        gbc.gridy = 0;
        gbc.gridx = 0;
        grid.add(new JLabel("<html><span style='color:#718096;'>Hình thức:</span> <b>" + loai + "</b></html>"), gbc);

        gbc.gridx = 1;
        grid.add(new JLabel("<html><span style='color:#718096;'>Trạng thái:</span> <b style='color:#E63946;'>" + status + "</b></html>"), gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;

        if ("Giao hàng tận nơi".equals(loai)) {
            String feeStr = "0 đ";
            try {
                if (data.get("fee") != null) {
                    feeStr = df.format(Double.parseDouble(data.get("fee")));
                }
            } catch (Exception ignored) {
            }

            String shipInfo = "📍 Địa chỉ: <b>" + data.get("address")
                    + "</b><br>📞 SĐT nhận: " + data.get("phone")
                    + " &nbsp;&nbsp;|&nbsp;&nbsp; 💰 Phí ship: <b style='color:#10B981;'>"
                    + feeStr + "</b>";

            grid.add(new JLabel("<html>" + shipInfo + "</html>"), gbc);

        } else if ("Lấy tại tủ khóa".equals(loai)) {
            grid.add(new JLabel("<html>📦 Mã tủ Locker: <b style='color:#365CF5;'>"
                    + data.get("locker")
                    + "</b> &nbsp;&nbsp;|&nbsp;&nbsp; ⏰ Giờ hẹn lấy: <b>"
                    + data.get("time")
                    + "</b></html>"), gbc);

        } else if ("Nhận tại quầy".equals(loai)) {
            grid.add(new JLabel("<html>🏪 Vị trí quầy tiếp nhận: <b style='color:#365CF5;'>"
                    + data.get("counter")
                    + "</b></html>"), gbc);
        }

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createUnitHelpPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(239, 246, 255));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(191, 219, 254)),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel lbl = new JLabel(
                isCancelled
                        ? "Hóa đơn đã hủy nên chỉ được xem chi tiết đơn vị bán."
                        : "Có thể chọn lại Đơn vị bán cho từng dòng. Khi đổi đơn vị, hệ thống tự đổi đơn giá và quy đổi tồn kho theo đơn vị gốc."
        );
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lbl.setForeground(new Color(30, 64, 175));
        panel.add(lbl, BorderLayout.CENTER);

        return panel;
    }

    private JScrollPane createProductTable(List<Map<String, Object>> details) {
        String[] cols = {"Mã CT", "Mã SP", "Tên Sản Phẩm", "Đơn vị", "SL", "Đơn Giá", "Thành Tiền"};

        modOrderDetail = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                if (isCancelled) {
                    return false;
                }
                return column == COL_UNIT || column == COL_QTY;
            }
        };

        for (Map<String, Object> d : details) {
            Object detailId = getValueIgnoreCase(d, "order_detail_id", "ORDER_DETAIL_ID");
            Object pId = getValueIgnoreCase(d, "product_id", "PRODUCT_ID");
            Object pName = getValueIgnoreCase(d, "product_name", "PRODUCT_NAME");
            Object unitId = getValueIgnoreCase(d, "unit_id", "UNIT_ID");
            Object qty = getValueIgnoreCase(d, "quantity", "QUANTITY");
            Object price = getValueIgnoreCase(d, "unit_price", "UNIT_PRICE");
            Object total = getValueIgnoreCase(d, "line_total", "LINE_TOTAL", "total_price", "TOTAL_PRICE");

            String productId = pId != null ? pId.toString() : "";
            ProductUnit selectedUnit = resolveProductUnit(productId, unitId != null ? unitId.toString() : null);

            double dPrice = parseMoneyToDouble(price);
            if (selectedUnit != null && selectedUnit.getSellingPrice() != null) {
                // Nếu DB có giá trong order_detail thì ưu tiên giữ giá lịch sử hóa đơn.
                if (dPrice <= 0) {
                    dPrice = selectedUnit.getSellingPrice().doubleValue();
                }
            }

            int iQty = 1;
            try {
                if (qty != null) {
                    iQty = Integer.parseInt(qty.toString());
                }
            } catch (Exception ignored) {
            }

            double dTotal = parseMoneyToDouble(total);
            if (dTotal <= 0) {
                dTotal = iQty * dPrice;
            }

            modOrderDetail.addRow(new Object[]{
                detailId != null ? detailId : "",
                productId,
                pName != null ? pName : productId,
                selectedUnit,
                iQty,
                dPrice,
                dTotal
            });
        }

        tableChiTiet = new JTable(modOrderDetail);
        tableChiTiet.setRowHeight(38);
        tableChiTiet.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tableChiTiet.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tableChiTiet.getTableHeader().setBackground(new Color(240, 245, 255));
        tableChiTiet.getTableHeader().setForeground(new Color(20, 30, 70));
        tableChiTiet.getTableHeader().setReorderingAllowed(false);
        tableChiTiet.setSelectionBackground(new Color(232, 245, 255));
        tableChiTiet.setSelectionForeground(Color.BLACK);
        tableChiTiet.setGridColor(new Color(226, 232, 240));
        tableChiTiet.setShowVerticalLines(true);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);

        DefaultTableCellRenderer productRenderer = new DefaultTableCellRenderer();
        productRenderer.setHorizontalAlignment(SwingConstants.LEFT);

        DefaultTableCellRenderer moneyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                if (value instanceof Number) {
                    value = df.format(value);
                }

                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column
                );

                setHorizontalAlignment(JLabel.RIGHT);
                return c;
            }
        };

        for (int i = 0; i < tableChiTiet.getColumnCount(); i++) {
            tableChiTiet.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        tableChiTiet.getColumnModel().getColumn(COL_PRODUCT_NAME).setCellRenderer(productRenderer);
        tableChiTiet.getColumnModel().getColumn(COL_UNIT).setCellRenderer(new ProductUnitRenderer());
        tableChiTiet.getColumnModel().getColumn(COL_UNIT_PRICE).setCellRenderer(moneyRenderer);
        tableChiTiet.getColumnModel().getColumn(COL_TOTAL).setCellRenderer(moneyRenderer);

        if (!isCancelled) {
            tableChiTiet.getColumnModel().getColumn(COL_QTY).setCellRenderer(new SpinnerRenderer());
            tableChiTiet.getColumnModel().getColumn(COL_QTY).setCellEditor(new SpinnerEditor());
            tableChiTiet.getColumnModel().getColumn(COL_UNIT).setCellEditor(new ProductUnitEditor());
        }

        tableChiTiet.getColumnModel().getColumn(COL_DETAIL_ID).setMinWidth(0);
        tableChiTiet.getColumnModel().getColumn(COL_DETAIL_ID).setMaxWidth(0);
        tableChiTiet.getColumnModel().getColumn(COL_DETAIL_ID).setPreferredWidth(0);

        tableChiTiet.getColumnModel().getColumn(COL_PRODUCT_ID).setPreferredWidth(80);
        tableChiTiet.getColumnModel().getColumn(COL_PRODUCT_NAME).setPreferredWidth(230);
        tableChiTiet.getColumnModel().getColumn(COL_UNIT).setPreferredWidth(115);
        tableChiTiet.getColumnModel().getColumn(COL_QTY).setPreferredWidth(55);
        tableChiTiet.getColumnModel().getColumn(COL_UNIT_PRICE).setPreferredWidth(100);
        tableChiTiet.getColumnModel().getColumn(COL_TOTAL).setPreferredWidth(120);

        modOrderDetail.addTableModelListener(e -> {
            if (isUpdatingTotal) {
                return;
            }

            if (e.getType() != TableModelEvent.UPDATE) {
                return;
            }

            int row = e.getFirstRow();
            int col = e.getColumn();

            if (row < 0 || row >= modOrderDetail.getRowCount()) {
                return;
            }

            if (col == COL_QTY || col == COL_UNIT) {
                updateRowAfterUnitOrQuantityChanged(row, col);
            }
        });

        JScrollPane sp = new JScrollPane(tableChiTiet);
        sp.setPreferredSize(new Dimension(0, 250));
        sp.setBorder(BorderFactory.createLineBorder(borderGray));
        return sp;
    }

    private void updateRowAfterUnitOrQuantityChanged(int row, int col) {
        try {
            int newQty = Integer.parseInt(modOrderDetail.getValueAt(row, COL_QTY).toString());

            if (newQty <= 0) {
                SwingUtilities.invokeLater(() -> {
                    int confirm = JOptionPane.showConfirmDialog(
                            this,
                            "Xóa sản phẩm này khỏi đơn hàng?",
                            "Xác nhận xóa",
                            JOptionPane.YES_NO_OPTION
                    );

                    if (confirm == JOptionPane.YES_OPTION) {
                        if (row >= 0 && row < modOrderDetail.getRowCount()) {
                            modOrderDetail.removeRow(row);
                        }
                    } else {
                        if (row >= 0 && row < modOrderDetail.getRowCount()) {
                            modOrderDetail.setValueAt(1, row, COL_QTY);
                        }
                    }

                    calculateOrderTotal();
                });
                return;
            }

            ProductUnit unit = getUnitFromCell(row);

            double price = parseMoneyToDouble(modOrderDetail.getValueAt(row, COL_UNIT_PRICE));

            if (col == COL_UNIT && unit != null && unit.getSellingPrice() != null) {
                price = unit.getSellingPrice().doubleValue();
            }

            isUpdatingTotal = true;
            modOrderDetail.setValueAt(price, row, COL_UNIT_PRICE);
            modOrderDetail.setValueAt(price * newQty, row, COL_TOTAL);
            isUpdatingTotal = false;

            calculateOrderTotal();

        } catch (Exception ex) {
            isUpdatingTotal = false;
            ex.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Số lượng hoặc đơn vị không hợp lệ!",
                    "Lỗi dữ liệu",
                    JOptionPane.ERROR_MESSAGE
            );
        }
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

        gbc.gridy = 0;
        gbc.gridx = 0;
        JLabel lbl1 = new JLabel("Tạm tính:");
        lbl1.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pnl.add(lbl1, gbc);

        gbc.gridx = 1;
        pnl.add(lblSubTotalVal, gbc);

        gbc.gridy = 1;
        gbc.gridx = 0;
        JLabel lbl2 = new JLabel("Giảm giá thành viên:");
        lbl2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        pnl.add(lbl2, gbc);

        gbc.gridx = 1;
        pnl.add(lblDiscountVal, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        pnl.add(new JSeparator(), gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridwidth = 1;

        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel lbl3 = new JLabel("TỔNG THANH TOÁN:");
        lbl3.setFont(new Font("Segoe UI", Font.PLAIN, 20));
        pnl.add(lbl3, gbc);

        gbc.gridx = 1;
        pnl.add(lblTotalPayVal, gbc);

        calculateOrderTotal();

        return pnl;
    }

    private void calculateOrderTotal() {
        if (modOrderDetail == null) {
            return;
        }

        double subTotal = 0;

        for (int i = 0; i < modOrderDetail.getRowCount(); i++) {
            subTotal += parseMoneyToDouble(modOrderDetail.getValueAt(i, COL_TOTAL));
        }

        double discountAmount = subTotal * currentDiscountRate;
        double totalToPay = subTotal - discountAmount;

        if (lblSubTotalVal != null) {
            lblSubTotalVal.setText(df.format(subTotal));
        }

        if (lblDiscountVal != null) {
            lblDiscountVal.setText("- " + df.format(discountAmount));
        }

        if (lblTotalPayVal != null) {
            lblTotalPayVal.setText(df.format(totalToPay));
        }
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
        btnSave.addActionListener(e -> saveInvoiceChanges(btnSave));

        if (!isCancelled) {
            pnl.add(btnSave);
        }

        pnl.add(btnClose);
        return pnl;
    }

    private void saveInvoiceChanges(JButton btnSave) {
        try {
            if (conn == null) {
                throw new IllegalStateException("Connection đang null. Hãy truyền conn khi mở OrderDetailDialog.");
            }

            if (tableChiTiet != null && tableChiTiet.isEditing()) {
                tableChiTiet.getCellEditor().stopCellEditing();
            }

            if (modOrderDetail == null || modOrderDetail.getRowCount() == 0) {
                throw new IllegalStateException("Hóa đơn phải có ít nhất 1 sản phẩm.");
            }

            List<InvoiceUpdateService.EditedOrderDetail> editedDetails = new ArrayList<>();

            for (int i = 0; i < modOrderDetail.getRowCount(); i++) {
                Object detailObj = modOrderDetail.getValueAt(i, COL_DETAIL_ID);
                Object productObj = modOrderDetail.getValueAt(i, COL_PRODUCT_ID);
                Object quantityObj = modOrderDetail.getValueAt(i, COL_QTY);
                Object unitPriceObj = modOrderDetail.getValueAt(i, COL_UNIT_PRICE);

                if (productObj == null) {
                    throw new IllegalArgumentException("Mã sản phẩm ở dòng " + (i + 1) + " đang trống.");
                }

                String orderDetailId = detailObj == null ? "" : detailObj.toString().trim();
                String productId = productObj.toString().trim();
                int quantity = Integer.parseInt(quantityObj.toString().trim());
                double unitPrice = parseMoneyToDouble(unitPriceObj);

                ProductUnit unit = getUnitFromCell(i);

                String unitId = unit != null && unit.getUnitId() != null
                        ? unit.getUnitId()
                        : null;

                int quantityBase = calculateQuantityBase(unit, quantity);

                if (quantity <= 0) {
                    throw new IllegalArgumentException("Số lượng sản phẩm phải lớn hơn 0: " + productId);
                }

                editedDetails.add(
                        new InvoiceUpdateService.EditedOrderDetail(
                                orderDetailId,
                                productId,
                                unitId,
                                quantity,
                                quantityBase,
                                unitPrice
                        )
                );
            }

            btnSave.setEnabled(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            new SwingWorker<Void, Void>() {
                private Exception error;

                @Override
                protected Void doInBackground() {
                    try {
                        InvoiceUpdateService service = new InvoiceUpdateService(conn);
                        service.saveInvoiceChanges(orderId, editedDetails);
                    } catch (Exception ex) {
                        error = ex;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    btnSave.setEnabled(true);
                    setCursor(Cursor.getDefaultCursor());

                    if (error != null) {
                        JOptionPane.showMessageDialog(
                                OrderDetailDialog.this,
                                "Lỗi khi lưu thay đổi hóa đơn:\n" + error.getMessage(),
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE
                        );
                        return;
                    }

                    JOptionPane.showMessageDialog(
                            OrderDetailDialog.this,
                            "Lưu thay đổi hóa đơn thành công!",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE
                    );

                    if (onSavedCallback != null) {
                        onSavedCallback.run();
                    }

                    dispose();
                }
            }.execute();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Số lượng hoặc đơn giá không hợp lệ!",
                    "Lỗi dữ liệu",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi khi lưu thay đổi hóa đơn:\n" + ex.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private Object getValueIgnoreCase(Map<String, Object> map, String... keys) {
        if (map == null || keys == null) {
            return null;
        }

        for (String key : keys) {
            if (map.containsKey(key)) {
                return map.get(key);
            }
        }

        return null;
    }

    private List<ProductUnit> getUnitsForProduct(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String key = productId.trim();

        if (productUnitCache.containsKey(key)) {
            return productUnitCache.get(key);
        }

        List<ProductUnit> units = ProductUnitsSql.getInstance().selectByProductId(key);

        if (units == null) {
            units = new ArrayList<>();
        }

        productUnitCache.put(key, units);
        return units;
    }

    private ProductUnit resolveProductUnit(String productId, String unitId) {
        List<ProductUnit> units = getUnitsForProduct(productId);

        if (units == null || units.isEmpty()) {
            return null;
        }

        if (unitId != null && !unitId.trim().isEmpty()) {
            for (ProductUnit u : units) {
                if (u != null && u.getUnitId() != null
                        && u.getUnitId().equalsIgnoreCase(unitId.trim())) {
                    return u;
                }
            }
        }

        for (ProductUnit u : units) {
            if (u != null && u.getIsBaseUnit() == 1) {
                return u;
            }
        }

        return units.get(0);
    }

    private ProductUnit getUnitFromCell(int row) {
        Object value = modOrderDetail.getValueAt(row, COL_UNIT);

        if (value instanceof ProductUnit) {
            return (ProductUnit) value;
        }

        String productId = String.valueOf(modOrderDetail.getValueAt(row, COL_PRODUCT_ID));
        String unitId = value == null ? null : value.toString();

        return resolveProductUnit(productId, unitId);
    }

    private int calculateQuantityBase(ProductUnit unit, int quantity) {
        if (quantity <= 0) {
            return 0;
        }

        if (unit == null || unit.getConversionRateToBase() == null) {
            return quantity;
        }

        try {
            BigDecimal base = BigDecimal.valueOf(quantity).multiply(unit.getConversionRateToBase());
            return base.setScale(0, java.math.RoundingMode.CEILING).intValueExact();
        } catch (Exception e) {
            return quantity;
        }
    }

    private String getUnitDisplayText(ProductUnit unit) {
        if (unit == null) {
            return "Đơn vị";
        }

        String name = unit.getUnitName();
        if (name == null || name.trim().isEmpty()) {
            name = unit.getUnitId();
        }

        if (unit.getSellingPrice() != null) {
            return name + " - " + df.format(unit.getSellingPrice());
        }

        return name;
    }

    private double parseMoneyToDouble(Object value) {
        if (value == null) {
            return 0;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        String s = value.toString()
                .replace("đ", "")
                .replace("₫", "")
                .replace(".", "")
                .replace(",", "")
                .trim();

        if (s.isEmpty()) {
            return 0;
        }

        return Double.parseDouble(s);
    }

    private TitledBorder createTitledBorder(String title) {
        TitledBorder b = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                title
        );
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

    class ProductUnitRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            String text;

            if (value instanceof ProductUnit) {
                ProductUnit unit = (ProductUnit) value;
                text = unit.getUnitName() != null ? unit.getUnitName() : unit.getUnitId();
            } else {
                text = value == null ? "" : value.toString();
            }

            Component c = super.getTableCellRendererComponent(
                    table, text, isSelected, hasFocus, row, column
            );

            setHorizontalAlignment(SwingConstants.CENTER);
            return c;
        }
    }

    class ProductUnitEditor extends AbstractCellEditor implements TableCellEditor {

        private JComboBox<ProductUnit> comboBox;

        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column
        ) {
            int modelRow = table.convertRowIndexToModel(row);
            String productId = String.valueOf(modOrderDetail.getValueAt(modelRow, COL_PRODUCT_ID));

            comboBox = new JComboBox<>();

            List<ProductUnit> units = getUnitsForProduct(productId);

            for (ProductUnit unit : units) {
                comboBox.addItem(unit);
            }

            comboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(
                        JList<?> list,
                        Object value,
                        int index,
                        boolean isSelected,
                        boolean cellHasFocus
                ) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                    if (value instanceof ProductUnit) {
                        setText(getUnitDisplayText((ProductUnit) value));
                    }

                    return this;
                }
            });

            if (value instanceof ProductUnit) {
                comboBox.setSelectedItem(value);
            } else if (comboBox.getItemCount() > 0) {
                comboBox.setSelectedIndex(0);
            }

            return comboBox;
        }

        @Override
        public Object getCellEditorValue() {
            return comboBox != null ? comboBox.getSelectedItem() : null;
        }
    }

    class SpinnerEditor extends DefaultCellEditor {

        private final JSpinner spinner;

        public SpinnerEditor() {
            super(new JTextField());
            setClickCountToStart(1);

            spinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
            spinner.setBorder(null);

            JComponent editor = spinner.getEditor();

            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField txt = ((JSpinner.DefaultEditor) editor).getTextField();
                txt.setHorizontalAlignment(JTextField.CENTER);
                txt.setFont(new Font("Segoe UI", Font.BOLD, 14));
            }
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column
        ) {
            try {
                spinner.setValue(Integer.parseInt(value.toString()));
            } catch (Exception e) {
                spinner.setValue(1);
            }

            return spinner;
        }

        @Override
        public Object getCellEditorValue() {
            return spinner.getValue();
        }
    }

    class SpinnerRenderer extends JSpinner implements TableCellRenderer {

        public SpinnerRenderer() {
            super(new SpinnerNumberModel(1, 0, 9999, 1));
            setBorder(null);

            JComponent editor = getEditor();

            if (editor instanceof JSpinner.DefaultEditor) {
                JTextField txt = ((JSpinner.DefaultEditor) editor).getTextField();
                txt.setHorizontalAlignment(JTextField.CENTER);
                txt.setFont(new Font("Segoe UI", Font.BOLD, 14));
            }
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            try {
                if (value != null) {
                    setValue(Integer.parseInt(value.toString()));
                }
            } catch (Exception e) {
                setValue(1);
            }

            setBackground(isSelected ? new Color(184, 218, 255) : Color.WHITE);
            return this;
        }
    }
}
