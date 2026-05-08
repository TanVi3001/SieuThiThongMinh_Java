package view;

import business.service.PaymentService;
import business.service.SessionManager;
import business.sql.prod_inventory.ProductsSql;
import business.sql.sales_order.CustomersSql;
import business.sql.sales_order.OrdersSql;
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
import java.sql.Date;
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

    // Customer / Loyalty
    private JTextField txtCustomerPhone;
    private JButton btnFindCustomer;
    private JLabel lblCustomerInfo;

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

        EventBus.subscribe(AppDataChangedEvent.class, e -> {

            if (e.getType() == AppEventType.PRODUCTS
                    || e.getType() == AppEventType.INVENTORY) {

                SwingUtilities.invokeLater(this::loadProducts);
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

        // =====================================================
        // TOP
        // =====================================================
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

        // =====================================================
        // CENTER
        // =====================================================
        JPanel pnlCenter = new JPanel(new GridLayout(1, 2, 15, 0));
        pnlCenter.setOpaque(false);

        // LEFT - PRODUCT TABLE
        JPanel pnlLeft = new JPanel(new BorderLayout(0, 5));
        pnlLeft.setOpaque(false);

        JLabel lblProdTitle = new JLabel("DANH SÁCH SẢN PHẨM");
        lblProdTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        modProducts = new DefaultTableModel(
                new Object[]{"Mã SP", "Tên SP", "Giá bán", "Tồn kho"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblProducts = createTable(modProducts);

        pnlLeft.add(lblProdTitle, BorderLayout.NORTH);
        pnlLeft.add(new JScrollPane(tblProducts), BorderLayout.CENTER);

        // RIGHT - CART
        JPanel pnlRight = new JPanel(new BorderLayout(0, 5));
        pnlRight.setOpaque(false);

        JLabel lblCartTitle = new JLabel("GIỎ HÀNG");
        lblCartTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCartTitle.setForeground(new Color(39, 174, 96));

        modCart = new DefaultTableModel(
                new Object[]{"Mã SP", "Tên SP", "SL", "Đơn giá", "Thành tiền"}, 0
        ) {
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

        // =====================================================
        // BOTTOM
        // =====================================================
        JPanel pnlBottom = new JPanel(new BorderLayout(10, 10));
        pnlBottom.setOpaque(false);

        // CUSTOMER PANEL
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

        // PAYMENT PANEL
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

        JTextField txtEditor
                = (JTextField) cboSearch.getEditor().getEditorComponent();

        txtEditor.setText(SEARCH_HINT);
        txtEditor.setForeground(Color.GRAY);

        // SEARCH PLACEHOLDER
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

        // AUTOCOMPLETE
        txtEditor.addKeyListener(new KeyAdapter() {

            @Override
            public void keyReleased(KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btnAdd.doClick();
                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_UP
                        || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    return;
                }

                SwingUtilities.invokeLater(() -> {

                    String keyword = txtEditor.getText().trim();

                    cboSearch.removeAllItems();

                    if (keyword.isEmpty()
                            || keyword.equalsIgnoreCase(SEARCH_HINT)) {
                        return;
                    }

                    cboSearch.addItem(keyword);

                    boolean hasData = false;

                    for (Product p : allProducts) {

                        if (p.getQuantity() <= 0) {
                            continue;
                        }

                        String label = p.getProductId()
                                + " - "
                                + p.getProductName();

                        if (label.toLowerCase()
                                .contains(keyword.toLowerCase())) {

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

        // DOUBLE CLICK PRODUCT
        tblProducts.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getClickCount() == 2) {

                    int row = tblProducts.getSelectedRow();

                    if (row >= 0) {

                        String productId
                                = tblProducts.getValueAt(row, 0).toString();

                        addToCart(productId, (int) spnQty.getValue());
                    }
                }
            }
        });

        // ADD TO CART
        btnAdd.addActionListener(e -> {

            String selected = "";

            if (cboSearch.getSelectedItem() != null) {
                selected = cboSearch.getSelectedItem().toString();
            }

            if (selected.isBlank()
                    || selected.equalsIgnoreCase(SEARCH_HINT)) {

                int row = tblProducts.getSelectedRow();

                if (row < 0) {
                    JOptionPane.showMessageDialog(this,
                            "Vui lòng chọn sản phẩm!");
                    return;
                }

                selected = tblProducts.getValueAt(row, 0).toString();
            }

            String productId = selected.contains(" - ")
                    ? selected.split(" - ")[0].trim()
                    : selected.trim();

            addToCart(productId, (int) spnQty.getValue());

            txtEditor.setText("");
            spnQty.setValue(1);
        });

        // REMOVE
        btnRemove.addActionListener(e -> {

            int row = tblCart.getSelectedRow();

            if (row < 0) {
                JOptionPane.showMessageDialog(this,
                        "Vui lòng chọn sản phẩm trong giỏ!");
                return;
            }

            modCart.removeRow(row);
            calculateTotal();
        });

        // CANCEL
        btnCancel.addActionListener(e -> clearCart());

        // FIND CUSTOMER
        txtCustomerPhone.addActionListener(e -> btnFindCustomer.doClick());

        btnFindCustomer.addActionListener(e -> findCustomer());

        // PAYMENT
        btnPay.addActionListener(e -> processPayment());
    }

    // =========================================================
    // CUSTOMER
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

        int option = JOptionPane.showConfirmDialog(
                this,
                "Không tìm thấy khách hàng!\nĐăng ký nhanh?",
                "Thông báo",
                JOptionPane.YES_NO_OPTION
        );

        if (option == JOptionPane.YES_OPTION) {
            handleQuickRegister(phone);
        } else {
            resetCustomerInfo();
        }
    }

    private void handleQuickRegister(String phone) {

        JTextField txtName = new JTextField();

        Object[] message = {
            "Tên khách hàng:", txtName,
            "SĐT:", new JLabel(phone)
        };

        int option = JOptionPane.showConfirmDialog(
                this,
                message,
                "Đăng ký khách hàng",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        String name = txtName.getText().trim();

        if (name.isBlank()) {
            JOptionPane.showMessageDialog(this,
                    "Tên khách hàng không được để trống!");
            return;
        }

        Customer customer = new Customer();

        customer.setCustomerId("CUS" + System.currentTimeMillis());
        customer.setCustomerName(name);
        customer.setPhone(phone);
        customer.setRewardPoints(0);
        customer.setMemberRank("Thường");

        int result = CustomersSql.getInstance().insert(customer);

        if (result <= 0) {

            JOptionPane.showMessageDialog(this,
                    "Đăng ký khách hàng thất bại!");
            return;
        }

        SyncVersionDao.bumpVersion("CUSTOMERS");
        RealtimeClient.send("CUSTOMERS_CHANGED");

        selectedCustomer = CustomersSql.getInstance().findByPhone(phone);

        updateCustomerUI();

        JOptionPane.showMessageDialog(this,
                "✅ Đăng ký khách hàng thành công!");
    }

    private void updateCustomerUI() {

        if (selectedCustomer == null) {
            resetCustomerInfo();
            return;
        }

        lblCustomerInfo.setText(
                String.format(
                        "Hạng: %s | Giảm: %.0f%% | Tổng chi: %s",
                        selectedCustomer.getMemberRank(),
                        selectedCustomer.getDiscountRate() * 100,
                        moneyFormat.format(selectedCustomer.getTotalSpending())
                )
        );

        calculateTotal();
    }

    private void resetCustomerInfo() {

        selectedCustomer = null;

        txtCustomerPhone.setText("");

        lblCustomerInfo.setText("Khách vãng lai (0%)");

        calculateTotal();
    }

    // =========================================================
    // PAYMENT
    // =========================================================
    private void processPayment() {
        // 1. Kiểm tra giỏ hàng
        if (modCart.getRowCount() <= 0) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng đang trống!");
            return;
        }

        try {
            // =========================================================
            // 🌟 FIX LỆCH ID: Lấy UserId (Mã EMP...) thay vì AccountId
            // =========================================================
            String employeeId = "EMP_DEFAULT";
            model.account.Account acc = SessionManager.getCurrentUser();

            if (acc != null) {
                // Trong DB của bạn, Account.user_id chính là Employee_id (EMP...)
                employeeId = acc.getUserId();
            }

            // Log kiểm tra (Có thể xóa sau khi chạy ổn)
            System.out.println("DEBUG: Thanh toán hóa đơn bởi Mã NV: " + employeeId);

            // 2. Chuẩn bị thông tin hóa đơn (Order)
            String paymentMethodId = cboPaymentMethod.getSelectedItem() != null
                    ? cboPaymentMethod.getSelectedItem().toString()
                    : "PM_CASH";

            String orderId = business.sql.sales_order.OrdersSql.getInstance().generateNextOrderId();

            Order order = new Order();
            order.setOrderId(orderId);
            order.setCustomerId(selectedCustomer != null ? selectedCustomer.getCustomerId() : null);

            // Gán mã nhân viên chuẩn đã lấy ở trên
            order.setEmployeeId(employeeId);

            order.setPaymentMethodId(paymentMethodId);
            order.setOrderDate(new java.sql.Date(System.currentTimeMillis()));
            order.setTotalAmount(finalAmountToPay);

            // Trạng thái lưu "Hoàn thành" để khớp với SQL báo cáo
            order.setStatus("Hoàn thành");
            order.setNote("POS bán trực tiếp");

            // 3. Chuẩn bị danh sách chi tiết hóa đơn (OrderDetails)
            List<OrderDetail> details = new ArrayList<>();
            for (int i = 0; i < modCart.getRowCount(); i++) {
                String productId = modCart.getValueAt(i, 0).toString();
                int quantity = (int) modCart.getValueAt(i, 2);
                double price = (double) modCart.getValueAt(i, 3);

                // Tìm unitId từ danh sách sản phẩm đã load
                Product product = allProducts.stream()
                        .filter(p -> p.getProductId().equals(productId))
                        .findFirst()
                        .orElse(null);

                String unitId = (product != null && product.getBaseUnitId() != null)
                        ? product.getBaseUnitId() : "U_CAI";

                details.add(new OrderDetail(orderId, productId, quantity, price, unitId, 0));
            }

            // 4. Thực hiện gọi Service thanh toán (Database Transaction)
            boolean success = PaymentService.thanhToan(order, details);

            if (success) {
                // 5. Xử lý sau khi thanh toán thành công
                JOptionPane.showMessageDialog(this, "✅ Thanh toán thành công!\nMã hóa đơn: " + orderId);

                // Xóa giỏ hàng
                clearCart();

                // Cập nhật lại thông tin khách hàng (điểm thưởng/hạng) nếu có
                if (selectedCustomer != null) {
                    selectedCustomer = business.sql.sales_order.CustomersSql.getInstance()
                            .findByPhone(selectedCustomer.getPhone());
                    updateCustomerUI();
                }

                // Tải lại danh sách sản phẩm để cập nhật tồn kho trên giao diện
                loadProducts();

                // Thông báo Real-time cho các máy khác
                notifySystemChanged();
            } else {
                JOptionPane.showMessageDialog(this, "❌ Thanh toán thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi hệ thống: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================
    // CART
    // =========================================================
    private void addToCart(String productId, int qtyToAdd) {

        Product product = allProducts.stream()
                .filter(p -> p.getProductId().equalsIgnoreCase(productId))
                .findFirst()
                .orElse(null);

        if (product == null) {

            JOptionPane.showMessageDialog(this,
                    "Không tìm thấy sản phẩm!");

            return;
        }

        if (product.getQuantity() <= 0) {

            JOptionPane.showMessageDialog(this,
                    "Sản phẩm đã hết hàng!");

            return;
        }

        if (qtyToAdd > product.getQuantity()) {

            JOptionPane.showMessageDialog(this,
                    "Số lượng vượt tồn kho!");

            return;
        }

        double price = product.getBasePrice() != null
                ? product.getBasePrice().doubleValue()
                : 0;

        // EXISTED ITEM
        for (int i = 0; i < modCart.getRowCount(); i++) {

            String cartProductId
                    = modCart.getValueAt(i, 0).toString();

            if (cartProductId.equalsIgnoreCase(productId)) {

                int oldQty = (int) modCart.getValueAt(i, 2);

                int newQty = oldQty + qtyToAdd;

                if (newQty > product.getQuantity()) {

                    JOptionPane.showMessageDialog(this,
                            "Vượt quá tồn kho!");

                    return;
                }

                modCart.setValueAt(newQty, i, 2);
                modCart.setValueAt(price * newQty, i, 4);

                calculateTotal();

                return;
            }
        }

        // NEW ITEM
        modCart.addRow(new Object[]{
            product.getProductId(),
            product.getProductName(),
            qtyToAdd,
            price,
            price * qtyToAdd
        });

        calculateTotal();
    }

    private void clearCart() {

        modCart.setRowCount(0);

        currentTotal = 0;
        finalAmountToPay = 0;

        resetCustomerInfo();

        lblTotal.setText("Tổng tiền: 0 đ");
    }

    private void calculateTotal() {

        currentTotal = 0;

        for (int i = 0; i < modCart.getRowCount(); i++) {

            currentTotal += (double) modCart.getValueAt(i, 4);
        }

        double discountRate = selectedCustomer != null
                ? selectedCustomer.getDiscountRate()
                : 0;

        double discountAmount = currentTotal * discountRate;

        finalAmountToPay = currentTotal - discountAmount;

        if (discountRate > 0) {

            lblTotal.setText(
                    String.format(
                            "Tổng: %s | Giảm: -%s | Trả: %s",
                            moneyFormat.format(currentTotal),
                            moneyFormat.format(discountAmount),
                            moneyFormat.format(finalAmountToPay)
                    )
            );

        } else {

            lblTotal.setText(
                    "Tổng tiền: " + moneyFormat.format(currentTotal)
            );
        }
    }

    // =========================================================
    // LOAD DATA
    // =========================================================
    private void loadProducts() {

        new SwingWorker<List<Product>, Void>() {

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

                        if (p.getQuantity() <= 0) {
                            continue;
                        }

                        BigDecimal price = p.getBasePrice() != null
                                ? p.getBasePrice()
                                : BigDecimal.ZERO;

                        modProducts.addRow(new Object[]{
                            p.getProductId(),
                            p.getProductName(),
                            moneyFormat.format(price),
                            p.getQuantity()
                        });
                    }

                } catch (Exception ex) {

                    ex.printStackTrace();
                }
            }
        }.execute();
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

                        cboPaymentMethod.addItem(
                                pm.getPaymentMethodId()
                        );
                    }

                } catch (Exception ex) {

                    ex.printStackTrace();
                }
            }
        }.execute();
    }

    // =========================================================
    // SYSTEM EVENT
    // =========================================================
    private void notifySystemChanged() {

        try {

            String[] tags = {
                "ORDERS",
                "INVENTORY",
                "PRODUCTS",
                "CUSTOMERS"
            };

            for (String tag : tags) {

                SyncVersionDao.bumpVersion(tag);

                RealtimeClient.send(tag + "_CHANGED");

                EventBus.publish(
                        new AppDataChangedEvent(
                                AppEventType.valueOf(tag),
                                "POS Updated"
                        )
                );
            }

        } catch (Exception ignored) {
        }
    }

    // =========================================================
    // UI HELPER
    // =========================================================
    private JTable createTable(DefaultTableModel model) {

        JTable table = new JTable(model);

        table.setRowHeight(35);

        table.getTableHeader()
                .setFont(new Font("Segoe UI", Font.BOLD, 14));

        DefaultTableCellRenderer center
                = new DefaultTableCellRenderer();

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
}
