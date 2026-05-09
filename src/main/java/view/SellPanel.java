package view;

import business.service.PaymentService;
import business.service.SessionManager;
import business.sql.prod_inventory.ProductsSql;
import business.sql.sales_order.CustomersSql;
import business.sql.sales_order.PaymentMethodsSql;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;
import model.order.Customer;
import model.order.Order;
import model.order.OrderDetail;
import model.payment.PaymentMethod;
import model.product.Product;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class SellPanel extends JPanel {

    // =========================================================
    // CONFIG
    // =========================================================
    private static final String SEARCH_HINT = "Nhập mã SP, tên SP...";
    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0 đ");

    // =========================================================
    // UI COMPONENTS
    // =========================================================
    private JComboBox<String> cboSearch;
    private JSpinner spnQty;
    private JButton btnAdd;

    private JTable tblProducts;
    private DefaultTableModel modProducts;

    private JTable tblCart;
    private DefaultTableModel modCart;

    private JLabel lblTotal;
    private JComboBox<String> cboPaymentMethod;
    private JButton btnPay;
    private JButton btnCancel;
    private JButton btnRemove;

    // Customer
    private JTextField txtCustomerPhone;
    private JButton btnFindCustomer;
    private JLabel lblCustomerInfo;

    // 🌟 UI MỚI: Cảnh báo Realtime
    private JPanel pnlWarning;
    private JLabel lblWarningMsg;

    // =========================================================
    // DATA
    // =========================================================
    private List<Product> allProducts = new ArrayList<>();
    private Customer selectedCustomer;
    private double currentTotal = 0;
    private double finalAmountToPay = 0;

    // =========================================================
    // INIT
    // =========================================================
    public SellPanel() {
        buildUI();
        initEvents();

        loadProducts();
        loadPaymentMethods();

        // LẮNG NGHE REALTIME
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e.getType() == AppEventType.PRODUCTS
                    || e.getType() == AppEventType.INVENTORY
                    || e.getType() == AppEventType.ORDERS) {
                SwingUtilities.invokeLater(() -> {
                    loadProducts();
                    refreshCartRealtime();
                });
            }
        });
    }

    // =========================================================
    // BUILD UI
    // =========================================================
    private void buildUI() {
        setLayout(new BorderLayout(10, 10));
        setBackground(new Color(245, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // ---------------- TOP ----------------
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

        btnAdd = createButton("➕ Thêm vào giỏ", new Color(41, 128, 185));

        pnlTop.add(lblSearch);
        pnlTop.add(cboSearch);
        pnlTop.add(lblQty);
        pnlTop.add(spnQty);
        pnlTop.add(btnAdd);

        add(pnlTop, BorderLayout.NORTH);

        // ---------------- CENTER ----------------
        JPanel pnlCenter = new JPanel(new GridLayout(1, 2, 15, 0));
        pnlCenter.setOpaque(false);

        // LEFT - PRODUCT TABLE
        JPanel pnlLeft = new JPanel(new BorderLayout(0, 5));
        pnlLeft.setOpaque(false);
        JLabel lblProdTitle = new JLabel("DANH SÁCH SẢN PHẨM");
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

        // RIGHT - CART & WARNING
        JPanel pnlRight = new JPanel(new BorderLayout(0, 5));
        pnlRight.setOpaque(false);

        pnlWarning = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlWarning.setBackground(new Color(255, 243, 205));
        pnlWarning.setBorder(BorderFactory.createLineBorder(new Color(255, 238, 186)));
        lblWarningMsg = new JLabel("⚠ Sản phẩm không đủ tồn kho. Giỏ hàng đã đồng bộ theo dữ liệu mới nhất!");
        lblWarningMsg.setForeground(new Color(133, 100, 4));
        lblWarningMsg.setFont(new Font("Segoe UI", Font.BOLD, 13));
        pnlWarning.add(lblWarningMsg);
        pnlWarning.setVisible(false);

        JPanel pnlCartHeader = new JPanel(new BorderLayout());
        pnlCartHeader.setOpaque(false);
        JLabel lblCartTitle = new JLabel("GIỎ HÀNG");
        lblCartTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCartTitle.setForeground(new Color(39, 174, 96));
        pnlCartHeader.add(lblCartTitle, BorderLayout.NORTH);
        pnlCartHeader.add(pnlWarning, BorderLayout.SOUTH);

        // 🌟 Bảng Cart 7 Cột Mới
        modCart = new DefaultTableModel(new Object[]{"Mã SP", "Tên SP", "SL", "Đơn giá", "Thành tiền", "Tồn", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblCart = createTable(modCart);
        tblCart.setDefaultRenderer(Object.class, new CartTableRenderer());

        pnlRight.add(pnlCartHeader, BorderLayout.NORTH);
        pnlRight.add(new JScrollPane(tblCart), BorderLayout.CENTER);

        pnlCenter.add(pnlLeft);
        pnlCenter.add(pnlRight);
        add(pnlCenter, BorderLayout.CENTER);

        // ---------------- BOTTOM ----------------
        JPanel pnlBottom = new JPanel(new BorderLayout(10, 10));
        pnlBottom.setOpaque(false);

        JPanel pnlCustomer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        pnlCustomer.setOpaque(false);
        txtCustomerPhone = new JTextField(12);
        txtCustomerPhone.setPreferredSize(new Dimension(160, 35));
        btnFindCustomer = createButton("Tìm KH", new Color(52, 152, 219));
        lblCustomerInfo = new JLabel("Khách vãng lai (0%)");
        lblCustomerInfo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCustomerInfo.setForeground(new Color(41, 128, 185));

        pnlCustomer.add(new JLabel("SĐT khách:"));
        pnlCustomer.add(txtCustomerPhone);
        pnlCustomer.add(btnFindCustomer);
        pnlCustomer.add(lblCustomerInfo);

        JPanel pnlPayment = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        pnlPayment.setOpaque(false);
        cboPaymentMethod = new JComboBox<>();
        cboPaymentMethod.setPreferredSize(new Dimension(180, 40));
        lblTotal = new JLabel("Tổng tiền: 0 đ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTotal.setForeground(new Color(192, 57, 43));

        btnRemove = createButton("➖ Xóa món", new Color(243, 156, 18));
        btnCancel = createButton("🗑 Hủy đơn", new Color(231, 76, 60));
        btnPay = createButton("✔ Thanh toán", new Color(39, 174, 96));

        pnlPayment.add(cboPaymentMethod);
        pnlPayment.add(lblTotal);
        pnlPayment.add(btnRemove);
        pnlPayment.add(btnCancel);
        pnlPayment.add(btnPay);

        pnlBottom.add(pnlCustomer, BorderLayout.WEST);
        pnlBottom.add(pnlPayment, BorderLayout.EAST);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    // =========================================================
    // EVENTS
    // =========================================================
    private void initEvents() {
        JTextField txtEditor = (JTextField) cboSearch.getEditor().getEditorComponent();
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
                if (txtEditor.getText().trim().isEmpty()) {
                    txtEditor.setText(SEARCH_HINT);
                    txtEditor.setForeground(Color.GRAY);
                }
            }
        });

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
                    String keyword = txtEditor.getText().trim();
                    cboSearch.removeAllItems();
                    if (keyword.isEmpty() || keyword.equalsIgnoreCase(SEARCH_HINT)) {
                        return;
                    }

                    cboSearch.addItem(keyword);
                    boolean hasData = false;
                    for (Product p : allProducts) {
                        if (p.getQuantity() <= 0) {
                            continue;
                        }
                        String label = p.getProductId() + " - " + p.getProductName();
                        if (label.toLowerCase().contains(keyword.toLowerCase())) {
                            cboSearch.addItem(label);
                            hasData = true;
                        }
                    }
                    if (hasData) {
                        cboSearch.showPopup();
                    } else {
                        cboSearch.hidePopup();
                    }
                });
            }
        });

        tblProducts.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tblProducts.getSelectedRow();
                    if (row >= 0) {
                        addToCart(tblProducts.getValueAt(row, 0).toString(), (int) spnQty.getValue());
                    }
                }
            }
        });

        btnAdd.addActionListener(e -> {
            String selected = cboSearch.getSelectedItem() != null ? cboSearch.getSelectedItem().toString() : "";
            if (selected.isBlank() || selected.equalsIgnoreCase(SEARCH_HINT)) {
                int row = tblProducts.getSelectedRow();
                if (row < 0) {
                    JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm!");
                    return;
                }
                selected = tblProducts.getValueAt(row, 0).toString();
            }
            String productId = selected.contains(" - ") ? selected.split(" - ")[0].trim() : selected.trim();
            addToCart(productId, (int) spnQty.getValue());
            txtEditor.setText("");
            spnQty.setValue(1);
        });

        btnRemove.addActionListener(e -> {
            int row = tblCart.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm trong giỏ!");
                return;
            }
            modCart.removeRow(row);
            calculateTotal();
            refreshCartRealtime();
        });

        btnCancel.addActionListener(e -> clearCart());
        txtCustomerPhone.addActionListener(e -> btnFindCustomer.doClick());
        btnFindCustomer.addActionListener(e -> findCustomer());
        btnPay.addActionListener(e -> processPayment());
    }

    // =========================================================
    // CART & REALTIME LOGIC
    // =========================================================
    private void addToCart(String productId, int qtyToAdd) {
        Product product = allProducts.stream().filter(p -> p.getProductId().equalsIgnoreCase(productId)).findFirst().orElse(null);
        if (product == null) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy sản phẩm!");
            return;
        }
        if (product.getQuantity() <= 0) {
            JOptionPane.showMessageDialog(this, "Sản phẩm đã hết hàng!");
            return;
        }

        double price = product.getBasePrice() != null ? product.getBasePrice().doubleValue() : 0;

        for (int i = 0; i < modCart.getRowCount(); i++) {
            if (modCart.getValueAt(i, 0).toString().equalsIgnoreCase(productId)) {
                int oldQty = (int) modCart.getValueAt(i, 2);
                modCart.setValueAt(oldQty + qtyToAdd, i, 2);
                modCart.setValueAt(price * (oldQty + qtyToAdd), i, 4);
                modCart.setValueAt(product.getQuantity(), i, 5); // Update tồn kho
                calculateTotal();
                refreshCartRealtime();
                return;
            }
        }

        modCart.addRow(new Object[]{
            product.getProductId(), product.getProductName(), qtyToAdd, price, price * qtyToAdd, product.getQuantity(), ""
        });
        calculateTotal();
        refreshCartRealtime();
    }

    private void refreshCartRealtime() {
        boolean hasError = false;
        for (int i = 0; i < modCart.getRowCount(); i++) {
            String productId = modCart.getValueAt(i, 0).toString();
            int reqQty = (int) modCart.getValueAt(i, 2);
            int newStock = getStockFromDB(productId);
            modCart.setValueAt(newStock, i, 5);
            if (reqQty > newStock) {
                hasError = true;
            }
        }

        if (pnlWarning != null) {
            pnlWarning.setVisible(hasError);
        }
        if (btnPay != null) {
            btnPay.setEnabled(!hasError && modCart.getRowCount() > 0);
        }
        if (tblCart != null) {
            tblCart.repaint();
        }
    }

    private void clearCart() {
        modCart.setRowCount(0);
        currentTotal = 0;
        finalAmountToPay = 0;
        resetCustomerInfo();
        lblTotal.setText("Tổng tiền: 0 đ");
        if (pnlWarning != null) {
            pnlWarning.setVisible(false);
        }
        if (btnPay != null) {
            btnPay.setEnabled(false);
        }
    }

    private void calculateTotal() {
        currentTotal = 0;
        for (int i = 0; i < modCart.getRowCount(); i++) {
            currentTotal += (double) modCart.getValueAt(i, 4);
        }
        double discountRate = selectedCustomer != null ? selectedCustomer.getDiscountRate() : 0;
        double discountAmount = currentTotal * discountRate;
        finalAmountToPay = currentTotal - discountAmount;

        if (discountRate > 0) {
            lblTotal.setText(String.format("Tổng: %s | Giảm: -%s | Trả: %s", moneyFormat.format(currentTotal), moneyFormat.format(discountAmount), moneyFormat.format(finalAmountToPay)));
        } else {
            lblTotal.setText("Tổng tiền: " + moneyFormat.format(currentTotal));
        }
    }

    // =========================================================
    // DATABASE QUERY
    // =========================================================
    private void loadProducts() {
        new SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() {
                List<Product> products = ProductsSql.getInstance().searchByName("");
                java.util.Map<String, Integer> stockMap = getAllStockFromDB();
                for (Product p : products) {
                    p.setQuantity(stockMap.getOrDefault(p.getProductId(), 0));
                }
                return products;
            }

            @Override
            protected void done() {
                try {
                    allProducts = get();
                    modProducts.setRowCount(0);
                    for (Product p : allProducts) {
                        if (p.getQuantity() <= 0) {
                            continue;
                        }
                        BigDecimal price = p.getBasePrice() != null ? p.getBasePrice() : BigDecimal.ZERO;
                        modProducts.addRow(new Object[]{p.getProductId(), p.getProductName(), moneyFormat.format(price), p.getQuantity()});
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    private java.util.Map<String, Integer> getAllStockFromDB() {
        java.util.Map<String, Integer> stockMap = new java.util.HashMap<>();
        String sql = "SELECT product_id, quantity FROM INVENTORY WHERE NVL(is_deleted, 0) = 0";
        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection()) {
            if (con != null) {
                try (java.sql.PreparedStatement ps = con.prepareStatement(sql); java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        stockMap.put(rs.getString("product_id"), rs.getInt("quantity"));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return stockMap;
    }

    private int getStockFromDB(String productId) {
        String sql = "SELECT quantity FROM INVENTORY WHERE product_id = ?";
        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection()) {
            if (con != null) {
                try (java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, productId);
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return rs.getInt("quantity");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void loadPaymentMethods() {
        new SwingWorker<List<PaymentMethod>, Void>() {
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
        }.execute();
    }

    // =========================================================
    // CHECKOUT LOGIC
    // =========================================================
    private void processPayment() {
        if (modCart.getRowCount() <= 0) {
            return;
        }

        try {
            String employeeId = "EMP_DEFAULT";
            model.account.Account acc = SessionManager.getCurrentUser();
            if (acc != null) {
                employeeId = acc.getUserId();
            }

            String pmId = cboPaymentMethod.getSelectedItem() != null ? cboPaymentMethod.getSelectedItem().toString() : "PM_CASH";
            String orderId = business.sql.sales_order.OrdersSql.getInstance().generateNextOrderId();

            Order order = new Order();
            order.setOrderId(orderId);
            order.setCustomerId(selectedCustomer != null ? selectedCustomer.getCustomerId() : null);
            order.setEmployeeId(employeeId);
            order.setPaymentMethodId(pmId);
            order.setOrderDate(new java.sql.Date(System.currentTimeMillis()));
            order.setTotalAmount(finalAmountToPay);
            order.setStatus("Hoàn thành");

            List<OrderDetail> details = new ArrayList<>();
            for (int i = 0; i < modCart.getRowCount(); i++) {
                String pId = modCart.getValueAt(i, 0).toString();
                Product product = allProducts.stream().filter(p -> p.getProductId().equals(pId)).findFirst().orElse(null);
                String uId = (product != null && product.getBaseUnitId() != null) ? product.getBaseUnitId() : "U_CAI";
                details.add(new OrderDetail(orderId, pId, (int) modCart.getValueAt(i, 2), (double) modCart.getValueAt(i, 3), uId, 0));
            }

            boolean success = PaymentService.processCheckoutSecure(order, details);
            if (success) {
                JOptionPane.showMessageDialog(this, "✅ Thanh toán thành công!\nMã hóa đơn: " + orderId);
                clearCart();
                if (selectedCustomer != null) {
                    selectedCustomer = CustomersSql.getInstance().findByPhone(selectedCustomer.getPhone());
                    updateCustomerUI();
                }
                loadProducts();
                notifySystemChanged();
            }
        } catch (common.exception.ConcurrentCheckoutException ex) {
            StringBuilder errorMsg = new StringBuilder("Rất tiếc, giao dịch không thể hoàn tất do tồn kho vừa thay đổi.\nCác sản phẩm sau không đủ số lượng:\n\n");
            java.util.Map<String, Integer> failedMap = ex.getFailedProducts();
            for (int i = 0; i < modCart.getRowCount(); i++) {
                String pId = modCart.getValueAt(i, 0).toString();
                if (failedMap.containsKey(pId)) {
                    errorMsg.append(" • ").append(modCart.getValueAt(i, 1)).append("\n")
                            .append("   - Bạn yêu cầu: ").append(modCart.getValueAt(i, 2)).append("\n")
                            .append("   - Tồn kho chỉ còn: ").append(failedMap.get(pId)).append("\n\n");
                }
            }
            errorMsg.append("Giỏ hàng đã được đồng bộ lại. Vui lòng giảm số lượng để tiếp tục!");
            refreshCartRealtime();
            JOptionPane.showMessageDialog(this, errorMsg.toString(), "XUNG ĐỘT TỒN KHO", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi hệ thống: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    // CUSTOMER LOGIC
    // =========================================================
    private void findCustomer() {
        String phone = txtCustomerPhone.getText().trim();
        if (phone.isBlank()) {
            resetCustomerInfo();
            return;
        }
        selectedCustomer = CustomersSql.getInstance().findByPhone(phone);
        if (selectedCustomer != null) {
            updateCustomerUI();
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Không tìm thấy khách hàng!\nĐăng ký nhanh?", "Thông báo", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            JTextField txtName = new JTextField();
            if (JOptionPane.showConfirmDialog(this, new Object[]{"Tên khách hàng:", txtName, "SĐT:", new JLabel(phone)}, "Đăng ký", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                Customer c = new Customer();
                c.setCustomerId("CUS" + System.currentTimeMillis());
                c.setCustomerName(txtName.getText().trim());
                c.setPhone(phone);
                c.setRewardPoints(0);
                c.setMemberRank("Thường");
                if (CustomersSql.getInstance().insert(c) > 0) {
                    notifySystemChanged();
                    selectedCustomer = CustomersSql.getInstance().findByPhone(phone);
                    updateCustomerUI();
                    JOptionPane.showMessageDialog(this, "✅ Đăng ký thành công!");
                }
            }
        } else {
            resetCustomerInfo();
        }
    }

    private void updateCustomerUI() {
        if (selectedCustomer == null) {
            resetCustomerInfo();
            return;
        }
        lblCustomerInfo.setText(String.format("Hạng: %s | Giảm: %.0f%% | Tổng chi: %s", selectedCustomer.getMemberRank(), selectedCustomer.getDiscountRate() * 100, moneyFormat.format(selectedCustomer.getTotalSpending())));
        calculateTotal();
    }

    private void resetCustomerInfo() {
        selectedCustomer = null;
        txtCustomerPhone.setText("");
        lblCustomerInfo.setText("Khách vãng lai (0%)");
        calculateTotal();
    }

    private void notifySystemChanged() {
        try {
            String[] tags = {"ORDERS", "INVENTORY", "PRODUCTS", "CUSTOMERS"};
            for (String tag : tags) {
                SyncVersionDao.bumpVersion(tag);
                RealtimeClient.send(tag + "_CHANGED");
                EventBus.publish(new AppDataChangedEvent(AppEventType.valueOf(tag), "POS Updated"));
            }
        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // UI UTILS & CUSTOM RENDERER
    // =========================================================
    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        DefaultTableCellRenderer center = new DefaultTableCellRenderer();
        center.setHorizontalAlignment(JLabel.CENTER);
        table.setDefaultRenderer(Object.class, center);
        return table;
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    class CartTableRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(JLabel.CENTER);
            try {
                int qtyReq = Integer.parseInt(table.getValueAt(row, 2).toString());
                int stock = Integer.parseInt(table.getValueAt(row, 5).toString());

                if (column == 6) {
                    setFont(new Font("Segoe UI", Font.BOLD, 13));
                    if (qtyReq > stock) {
                        setText("❌ Hết hàng");
                        setForeground(new Color(220, 53, 69));
                    } else if (stock <= 5) {
                        setText("⚠ Sắp hết");
                        setForeground(new Color(211, 158, 0));
                    } else {
                        setText("✔ Hợp lệ");
                        setForeground(new Color(40, 167, 69));
                    }
                } else {
                    setForeground(Color.BLACK);
                    setFont(new Font("Segoe UI", Font.PLAIN, 14));
                }

                if (!isSelected) {
                    if (qtyReq > stock) {
                        setBackground(new Color(248, 215, 218));
                    } else if (stock <= 5) {
                        setBackground(new Color(255, 243, 205));
                    } else {
                        setBackground(Color.WHITE);
                    }
                } else {
                    setBackground(new Color(184, 218, 255));
                }
            } catch (Exception e) {
                setBackground(Color.WHITE);
                setForeground(Color.BLACK);
            }
            return c;
        }
    }
}
