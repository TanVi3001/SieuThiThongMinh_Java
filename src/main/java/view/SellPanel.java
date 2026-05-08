package view;

import business.service.PaymentService;
import business.service.SessionManager;
import business.sql.prod_inventory.ProductsSql;
import business.sql.sales_order.OrdersSql;
import business.sql.sales_order.PaymentMethodsSql;
import model.order.Order;
import model.order.OrderDetail;
import model.payment.PaymentMethod;
import model.product.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SellPanel extends JPanel {

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0 đ");

    // --- UI Components ---
    private JComboBox<String> cboSearch;
    private JSpinner spnQty;
    private JButton btnAdd;

    private JTable tblProducts;
    private DefaultTableModel modProducts;

    private JTable tblCart;
    private DefaultTableModel modCart;

    private JLabel lblTotal;
    private JComboBox<String> cboPaymentMethod;
    private JButton btnPay, btnCancel, btnRemove;

    // --- Data ---
    private List<Product> allProducts = new ArrayList<>();
    private double currentTotal = 0;
    private final String SEARCH_HINT = "Nhập mã SP, tên SP..."; // Chữ gợi ý

    public SellPanel() {
        buildUI();
        initEvents();
        loadProducts();
        loadPaymentMethods();
    }

    // =========================================================================
    // 1. XÂY DỰNG GIAO DIỆN
    // =========================================================================
    private void buildUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel pnlTop = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        pnlTop.setOpaque(false);

        JLabel lblSearch = new JLabel("🔍 Tìm SP:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));

        cboSearch = new JComboBox<>();
        cboSearch.setEditable(true);
        cboSearch.setPreferredSize(new Dimension(350, 35));
        cboSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JLabel lblQty = new JLabel("Số lượng:");
        lblQty.setFont(new Font("Segoe UI", Font.BOLD, 14));

        spnQty = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spnQty.setPreferredSize(new Dimension(80, 35));
        spnQty.setFont(new Font("Segoe UI", Font.BOLD, 14));

        btnAdd = createButton("➕ Thêm vào giỏ", new Color(41, 128, 185));
        btnAdd.setPreferredSize(new Dimension(150, 35));

        pnlTop.add(lblSearch);
        pnlTop.add(cboSearch);
        pnlTop.add(lblQty);
        pnlTop.add(spnQty);
        pnlTop.add(btnAdd);
        add(pnlTop, BorderLayout.NORTH);

        JPanel pnlCenter = new JPanel(new GridLayout(1, 2, 15, 0));
        pnlCenter.setOpaque(false);

        JPanel pnlLeft = new JPanel(new BorderLayout(0, 5));
        pnlLeft.setOpaque(false);
        JLabel lblProdTitle = new JLabel("DANH SÁCH SẢN PHẨM (Double-click để chọn)");
        lblProdTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        modProducts = new DefaultTableModel(new Object[]{"Mã SP", "Tên SP", "Giá bán", "Tồn kho"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblProducts = createTable(modProducts);
        pnlLeft.add(lblProdTitle, BorderLayout.NORTH);
        pnlLeft.add(new JScrollPane(tblProducts), BorderLayout.CENTER);

        JPanel pnlRight = new JPanel(new BorderLayout(0, 5));
        pnlRight.setOpaque(false);
        JLabel lblCartTitle = new JLabel("GIỎ HÀNG CỦA KHÁCH");
        lblCartTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCartTitle.setForeground(new Color(39, 174, 96));

        modCart = new DefaultTableModel(new Object[]{"Mã SP", "Tên SP", "SL", "Đơn giá", "Thành tiền"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblCart = createTable(modCart);
        pnlRight.add(lblCartTitle, BorderLayout.NORTH);
        pnlRight.add(new JScrollPane(tblCart), BorderLayout.CENTER);

        pnlCenter.add(pnlLeft);
        pnlCenter.add(pnlRight);
        add(pnlCenter, BorderLayout.CENTER);

        JPanel pnlBottom = new JPanel(new BorderLayout(10, 10));
        pnlBottom.setOpaque(false);
        pnlBottom.setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(200, 200, 200)));

        JPanel pnlTotal = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        pnlTotal.setOpaque(false);

        cboPaymentMethod = new JComboBox<>();
        cboPaymentMethod.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cboPaymentMethod.setPreferredSize(new Dimension(150, 45));

        lblTotal = new JLabel("Tổng tiền: 0 đ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTotal.setForeground(new Color(192, 57, 43));

        btnPay = createButton("✔ Thanh toán", new Color(39, 174, 96));
        btnCancel = createButton("🗑 Hủy đơn", new Color(231, 76, 60));
        btnRemove = createButton("➖ Xóa món", new Color(243, 156, 18));

        pnlTotal.add(new JLabel("Thanh toán:"));
        pnlTotal.add(cboPaymentMethod);
        pnlTotal.add(lblTotal);
        pnlTotal.add(btnRemove);
        pnlTotal.add(btnCancel);
        pnlTotal.add(btnPay);

        pnlBottom.add(pnlTotal, BorderLayout.EAST);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    // =========================================================================
    // 2. LOGIC AUTOCOMPLETE CỰC XỊN XÒ
    // =========================================================================
    private void initEvents() {
        JTextField txtEditor = (JTextField) cboSearch.getEditor().getEditorComponent();

        // 🌟 TẠO CHỮ GỢI Ý (PLACEHOLDER)
        txtEditor.setText(SEARCH_HINT);
        txtEditor.setForeground(Color.GRAY);

        txtEditor.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtEditor.getText().equals(SEARCH_HINT)) {
                    txtEditor.setText("");
                    txtEditor.setForeground(Color.BLACK);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (txtEditor.getText().isEmpty()) {
                    txtEditor.setText(SEARCH_HINT);
                    txtEditor.setForeground(Color.GRAY);
                }
            }
        });

        // 🌟 XỔ FULL SẢN PHẨM KHI BẤM NÚT TRỎ XUỐNG
        cboSearch.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent e) {
                SwingUtilities.invokeLater(() -> {
                    String currentText = txtEditor.getText();
                    if (currentText.isEmpty() || currentText.equals(SEARCH_HINT)) {
                        cboSearch.removeAllItems();
                        for (Product p : allProducts) {
                            if (p.getQuantity() > 0) {
                                cboSearch.addItem(p.getProductId() + " - " + p.getProductName());
                            }
                        }
                        if (currentText.equals(SEARCH_HINT)) {
                            txtEditor.setText("");
                            txtEditor.setForeground(Color.BLACK);
                        }
                    }
                });
            }

            @Override
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent e) {
            }
        });

        // AutoComplete mượt mà khi gõ chữ
        txtEditor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btnAdd.doClick();
                    return;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    String txt = txtEditor.getText();
                    if (txt.equals(SEARCH_HINT)) {
                        return;
                    }

                    cboSearch.removeAllItems();
                    cboSearch.addItem(txt);

                    if (!txt.isEmpty()) {
                        for (Product p : allProducts) {
                            if (p.getQuantity() > 0) {
                                String label = p.getProductId() + " - " + p.getProductName();
                                if (label.toLowerCase().contains(txt.toLowerCase())) {
                                    cboSearch.addItem(label);
                                }
                            }
                        }
                        cboSearch.showPopup();
                    } else {
                        cboSearch.hidePopup();
                    }
                });
            }
        });

        btnAdd.addActionListener(e -> {
            String selected = "";
            if (cboSearch.getSelectedItem() != null) {
                selected = cboSearch.getSelectedItem().toString();
            }
            if (selected.isEmpty() || selected.equals(SEARCH_HINT)) {
                selected = txtEditor.getText();
            }
            if (selected.equals(SEARCH_HINT)) {
                selected = "";
            }

            if (selected.isEmpty()) {
                int row = tblProducts.getSelectedRow();
                if (row >= 0) {
                    String pId = tblProducts.getValueAt(row, 0).toString();
                    addToCartExplicit(pId, (int) spnQty.getValue());
                    spnQty.setValue(1);
                    tblProducts.clearSelection();
                } else {
                    JOptionPane.showMessageDialog(this, "Vui lòng nhập tên SP hoặc click chọn SP trong bảng!");
                }
                return;
            }

            String pId = selected.contains(" - ") ? selected.split(" - ")[0].trim() : selected.trim();
            addToCartExplicit(pId, (int) spnQty.getValue());

            txtEditor.setText("");
            cboSearch.removeAllItems();
            spnQty.setValue(1);
            txtEditor.requestFocus();
        });

        tblProducts.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tblProducts.getSelectedRow();
                    if (row >= 0) {
                        addToCartExplicit(tblProducts.getValueAt(row, 0).toString(), 1);
                    }
                }
            }
        });

        btnRemove.addActionListener(e -> {
            int row = tblCart.getSelectedRow();
            if (row >= 0) {
                modCart.removeRow(row);
                calculateTotal();
            } else {
                JOptionPane.showMessageDialog(this, "Vui lòng click chọn 1 món trong giỏ hàng để xóa!", "Nhắc nhở", JOptionPane.WARNING_MESSAGE);
            }
        });

        btnCancel.addActionListener(e -> {
            modCart.setRowCount(0);
            calculateTotal();
        });

        btnPay.addActionListener(e -> processPayment());
    }

    private void loadProducts() {
        SwingWorker<List<Product>, Void> w = new SwingWorker<>() {
            @Override
            protected List<Product> doInBackground() {
                return ProductsSql.getInstance().searchByName("");
            }

            @Override
            protected void done() {
                try {
                    allProducts = get();
                    modProducts.setRowCount(0);
                    for (Product p : allProducts) {
                        if (p.getQuantity() > 0) {
                            modProducts.addRow(new Object[]{
                                p.getProductId(), p.getProductName(),
                                p.getBasePrice() != null ? moneyFormat.format(p.getBasePrice()) : "0 đ",
                                p.getQuantity()
                            });
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        w.execute();
    }

    private void loadPaymentMethods() {
        SwingWorker<List<PaymentMethod>, Void> w = new SwingWorker<>() {
            @Override
            protected List<PaymentMethod> doInBackground() {
                return PaymentMethodsSql.getInstance().selectAll();
            }

            @Override
            protected void done() {
                try {
                    cboPaymentMethod.removeAllItems();
                    for (PaymentMethod pm : get()) {
                        cboPaymentMethod.addItem(pm.getPaymentMethodId());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
        w.execute();
    }

    // =========================================================================
    // 3. LOGIC GIỎ HÀNG
    // =========================================================================
    private void addToCartExplicit(String productId, int qtyToAdd) {
        Product product = null;
        for (Product p : allProducts) {
            if (p.getProductId().equalsIgnoreCase(productId)) {
                product = p;
                break;
            }
        }

        if (product == null || product.getQuantity() <= 0) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy sản phẩm " + productId + " hoặc đã hết hàng!");
            return;
        }

        double price = product.getBasePrice() != null ? product.getBasePrice().doubleValue() : 0;

        for (int i = 0; i < modCart.getRowCount(); i++) {
            if (modCart.getValueAt(i, 0).toString().equalsIgnoreCase(productId)) {
                int currentQty = (int) modCart.getValueAt(i, 2);
                int newQty = currentQty + qtyToAdd;

                if (newQty <= product.getQuantity()) {
                    modCart.setValueAt(newQty, i, 2);
                    modCart.setValueAt(newQty * price, i, 4);
                    calculateTotal();
                } else {
                    JOptionPane.showMessageDialog(this, "Sản phẩm này chỉ còn " + product.getQuantity() + " trong kho!");
                }
                return;
            }
        }

        if (qtyToAdd <= product.getQuantity()) {
            modCart.addRow(new Object[]{
                product.getProductId(), product.getProductName(), qtyToAdd, price, price * qtyToAdd
            });
            calculateTotal();
        } else {
            JOptionPane.showMessageDialog(this, "Sản phẩm này chỉ còn " + product.getQuantity() + " trong kho!");
        }
    }

    private void calculateTotal() {
        currentTotal = 0;
        for (int i = 0; i < modCart.getRowCount(); i++) {
            currentTotal += (double) modCart.getValueAt(i, 4);
        }
        lblTotal.setText("Tổng tiền: " + moneyFormat.format(currentTotal));
    }

    // =========================================================================
    // 4. FIX BUG THANH TOÁN (ORA-01400: UNIT_ID NULL)
    // =========================================================================
    private void processPayment() {
        if (modCart.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng đang trống!", "Chú ý", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            String empId = "EMP1777277282761";
            if (SessionManager.getCurrentUser() != null && SessionManager.getCurrentUser().getAccountId() != null) {
                empId = SessionManager.getCurrentUser().getAccountId();
            }

            String pm = cboPaymentMethod.getSelectedItem() != null ? cboPaymentMethod.getSelectedItem().toString() : "PM_CASH";

            String orderId = "HD" + System.currentTimeMillis();
            try {
                orderId = OrdersSql.getInstance().generateNextOrderId();
            } catch (Exception ignored) {
            }

            Order order = new Order();
            order.setOrderId(orderId);
            order.setCustomerId(null);
            order.setEmployeeId(empId);
            order.setPaymentMethodId(pm);
            order.setOrderDate(new java.sql.Date(System.currentTimeMillis()));
            order.setTotalAmount(currentTotal);
            order.setStatus("COMPLETED");
            order.setNote("POS Bán trực tiếp");
            order.setDeleted(false);

            List<OrderDetail> details = new ArrayList<>();
            for (int i = 0; i < modCart.getRowCount(); i++) {
                String pId = modCart.getValueAt(i, 0).toString();
                int qty = (int) modCart.getValueAt(i, 2);
                double price = (double) modCart.getValueAt(i, 3);

                // 🌟 FIX LỖI ORA-01400: Luôn gán 1 giá trị Unit tồn tại thực tế nếu SP bị bỏ trống
                String unitId = "U_CAI"; // Default là U_CAI (Thường luôn tồn tại trong các DB)
                for (Product p : allProducts) {
                    if (p.getProductId().equals(pId)) {
                        if (p.getBaseUnitId() != null && !p.getBaseUnitId().trim().isEmpty()) {
                            unitId = p.getBaseUnitId();
                        } else if (p.getUnit() != null && !p.getUnit().toString().trim().isEmpty()) {
                            unitId = p.getUnit().toString();
                        }
                        break;
                    }
                }

                details.add(new OrderDetail(orderId, pId, qty, price, unitId, 0));
            }

            boolean success = PaymentService.thanhToan(order, details);

            if (success) {
                JOptionPane.showMessageDialog(this, "✅ Đã thanh toán thành công!\nMã bill: " + orderId, "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
                modCart.setRowCount(0);
                calculateTotal();
                loadProducts();

                JTextField txtEditor = (JTextField) cboSearch.getEditor().getEditorComponent();
                txtEditor.setText(SEARCH_HINT);
                txtEditor.setForeground(Color.GRAY);
                txtEditor.requestFocus();

                try {
                    common.events.EventBus.publish(new common.events.AppDataChangedEvent(common.events.AppEventType.ORDERS, "Có bill POS mới"));
                } catch (Exception ignored) {
                }

            } else {
                JOptionPane.showMessageDialog(this, "❌ Thanh toán thất bại!\nLỗi liên quan đến Database (Kiểm tra xem Unit ID có khớp không)", "Lỗi DB", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi hệ thống: " + ex.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        table.getTableHeader().setBackground(new Color(236, 240, 241));
        table.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);
        return table;
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setPreferredSize(new Dimension(140, 45));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
