package view;

import business.service.PaymentService;
import business.service.SessionManager;
import business.sql.prod_inventory.ProductsSql;
import business.sql.sales_order.CustomersSql;
import business.sql.sales_order.OrdersSql;
import business.sql.sales_order.PaymentMethodsSql;
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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import common.events.EventBus;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;

/**
 * Panel bán hàng POS tại quầy Hỗ trợ: Tìm sản phẩm, Giỏ hàng, Loyalty (Khách
 * hàng thân thiết), Thanh toán đa phương thức
 */
public class SellPanel extends JPanel {

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0 đ");

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

    // --- Biến Loyalty ---
    private JTextField txtCustomerPhone;
    private JButton btnFindCustomer;
    private JLabel lblCustomerInfo;
    private Customer selectedCustomer = null;
    private double finalAmountToPay = 0; // Tiền sau khi đã trừ chiết khấu

    private List<Product> allProducts = new ArrayList<>();
    private double currentTotal = 0;
    private final String SEARCH_HINT = "Nhập mã SP, tên SP...";

    public SellPanel() {
        buildUI();
        initEvents();
        loadProducts();
        loadPaymentMethods();
    }

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
        JLabel lblProdTitle = new JLabel("DANH SÁCH SẢN PHẨM (Click đúp để chọn)");
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

        // --- KHU VỰC TÌM KHÁCH HÀNG (LOYALTY) ---
        JPanel pnlCustomer = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 15));
        pnlCustomer.setOpaque(false);
        txtCustomerPhone = new JTextField(12);
        txtCustomerPhone.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtCustomerPhone.setPreferredSize(new Dimension(150, 35));
        btnFindCustomer = createButton("Tìm KH", new Color(52, 152, 219));
        btnFindCustomer.setPreferredSize(new Dimension(100, 35));

        lblCustomerInfo = new JLabel("Khách vãng lai (0%)");
        lblCustomerInfo.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblCustomerInfo.setForeground(new Color(41, 128, 185));

        pnlCustomer.add(new JLabel("SĐT Khách:"));
        pnlCustomer.add(txtCustomerPhone);
        pnlCustomer.add(btnFindCustomer);
        pnlCustomer.add(lblCustomerInfo);
        pnlBottom.add(pnlCustomer, BorderLayout.WEST);

        // --- KHU VỰC THANH TOÁN ---
        JPanel pnlTotal = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        pnlTotal.setOpaque(false);

        cboPaymentMethod = new JComboBox<>();
        cboPaymentMethod.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cboPaymentMethod.setPreferredSize(new Dimension(150, 45));

        lblTotal = new JLabel("Tổng tiền: 0 đ");
        lblTotal.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTotal.setForeground(new Color(192, 57, 43));

        btnPay = createButton("✔ Thanh toán", new Color(39, 174, 96));
        btnCancel = createButton("🗑 Hủy đơn", new Color(231, 76, 60));
        btnRemove = createButton("➖ Xóa món", new Color(243, 156, 18));

        pnlTotal.add(cboPaymentMethod);
        pnlTotal.add(lblTotal);
        pnlTotal.add(btnRemove);
        pnlTotal.add(btnCancel);
        pnlTotal.add(btnPay);

        pnlBottom.add(pnlTotal, BorderLayout.EAST);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    private void initEvents() {
        // --- SỰ KIỆN TÌM KHÁCH HÀNG ---
        // Nhấn Enter tại ô SĐT sẽ tự động kích hoạt nút tìm
        txtCustomerPhone.addActionListener(e -> btnFindCustomer.doClick());

        btnFindCustomer.addActionListener(e -> {
            String phone = txtCustomerPhone.getText().trim();
            if (phone.isEmpty()) {
                resetCustomerInfo();
                return;
            }

            // Gọi Database tìm khách hàng
            selectedCustomer = CustomersSql.getInstance().findByPhone(phone);
            if (selectedCustomer != null) {
                updateCustomerUI();
            } else {
                int addConfirm = JOptionPane.showConfirmDialog(this,
                        "Không tìm thấy khách hàng này! Đăng ký ngay?",
                        "Khách hàng mới", JOptionPane.YES_NO_OPTION);
                if (addConfirm == JOptionPane.YES_OPTION) {
                    handleQuickRegister(phone);
                } else {
                    resetCustomerInfo();
                }
            }
        });

        // --- SỰ KIỆN TÌM SẢN PHẨM ---
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
                if (txtEditor.getText().isEmpty()) {
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
                    }
                });
            }
        });

        btnAdd.addActionListener(e -> {
            String selected = (cboSearch.getSelectedItem() != null) ? cboSearch.getSelectedItem().toString() : "";
            if (selected.isEmpty() || selected.equals(SEARCH_HINT)) {
                selected = txtEditor.getText();
            }

            if (selected.isEmpty() || selected.equals(SEARCH_HINT)) {
                int row = tblProducts.getSelectedRow();
                if (row >= 0) {
                    addToCartExplicit(tblProducts.getValueAt(row, 0).toString(), (int) spnQty.getValue());
                } else {
                    JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm!");
                }
                return;
            }

            String pId = selected.contains(" - ") ? selected.split(" - ")[0].trim() : selected.trim();
            addToCartExplicit(pId, (int) spnQty.getValue());
            txtEditor.setText("");
            spnQty.setValue(1);
            txtEditor.requestFocus();
        });

        btnRemove.addActionListener(e -> {
            int row = tblCart.getSelectedRow();
            if (row >= 0) {
                modCart.removeRow(row);
                calculateTotal();
            }
        });

        btnCancel.addActionListener(e -> {
            modCart.setRowCount(0);
            resetCustomerInfo();
        });

        btnPay.addActionListener(e -> processPayment());
    }

    private void processPayment() {
        if (modCart.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng đang trống!", "Chú ý", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            // 1. Chuẩn bị thông tin hóa đơn
            String empId = (SessionManager.getCurrentUser() != null)
                    ? SessionManager.getCurrentUser().getAccountId()
                    : "EMP_DEFAULT";

            String pm = (cboPaymentMethod.getSelectedItem() != null)
                    ? cboPaymentMethod.getSelectedItem().toString()
                    : "PM_CASH";

            String orderId = OrdersSql.getInstance().generateNextOrderId();

            Order order = new Order();
            order.setOrderId(orderId);
            order.setCustomerId(selectedCustomer != null ? selectedCustomer.getCustomerId() : null);
            order.setEmployeeId(empId);
            order.setPaymentMethodId(pm);
            order.setOrderDate(new java.sql.Date(System.currentTimeMillis()));
            order.setTotalAmount(finalAmountToPay);
            order.setStatus("Hoàn thành");
            order.setNote("POS Bán trực tiếp");

            // 2. Chuẩn bị danh sách chi tiết
            List<OrderDetail> details = new ArrayList<>();
            for (int i = 0; i < modCart.getRowCount(); i++) {
                String pId = modCart.getValueAt(i, 0).toString();
                int qty = (int) modCart.getValueAt(i, 2);
                double price = (double) modCart.getValueAt(i, 3);

                // Tìm UnitId an toàn bằng Stream
                String unitId = allProducts.stream()
                        .filter(p -> p.getProductId().equals(pId))
                        .findFirst()
                        .map(p -> (p.getBaseUnitId() != null && !p.getBaseUnitId().isBlank()) ? p.getBaseUnitId() : "U_CAI")
                        .orElse("U_CAI");

                details.add(new OrderDetail(orderId, pId, qty, price, unitId, 0));
            }

            // 3. Thực thi thanh toán
            boolean success = PaymentService.thanhToan(order, details);

            if (success) {
                JOptionPane.showMessageDialog(this, "✅ Thanh toán thành công!\nMã bill: " + orderId);
                modCart.setRowCount(0);

                // --- QUAN TRỌNG: CẬP NHẬT LẠI THÔNG TIN KHÁCH HÀNG (ĐỂ THẤY HẠNG MỚI) ---
                if (selectedCustomer != null) {
                    selectedCustomer = CustomersSql.getInstance().findByPhone(selectedCustomer.getPhone());
                    updateCustomerUI();
                } else {
                    resetCustomerInfo();
                }

                refreshUIState();
                notifySystemChanged();
            } else {
                JOptionPane.showMessageDialog(this, "❌ Thanh toán thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi hệ thống: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void handleQuickRegister(String phone) {
        JTextField txtNewName = new JTextField();
        Object[] message = {"Tên khách hàng (*):", txtNewName, "Số điện thoại:", new JLabel(phone)};
        int option = JOptionPane.showConfirmDialog(this, message, "Đăng ký nhanh", JOptionPane.OK_CANCEL_OPTION);

        if (option == JOptionPane.OK_OPTION && !txtNewName.getText().trim().isEmpty()) {
            Customer newCus = new Customer();
            newCus.setCustomerId("CUS" + System.currentTimeMillis());
            newCus.setCustomerName(txtNewName.getText().trim());
            newCus.setPhone(phone);

            if (CustomersSql.getInstance().insert(newCus) > 0) {
                SyncVersionDao.bumpVersion("CUSTOMERS");
                RealtimeClient.send("CUSTOMERS_CHANGED");
                selectedCustomer = CustomersSql.getInstance().findByPhone(phone);
                updateCustomerUI();
                JOptionPane.showMessageDialog(this, "✅ Đăng ký thành công!");
            }
        }
    }

    private void updateCustomerUI() {
        if (selectedCustomer != null) {
            lblCustomerInfo.setText(String.format("Hạng: %s | Giảm: %.0f%% | T.Chi: %s",
                    selectedCustomer.getMemberRank(),
                    selectedCustomer.getDiscountRate() * 100,
                    moneyFormat.format(selectedCustomer.getTotalSpending())));
        }
        calculateTotal();
    }

    private void resetCustomerInfo() {

        selectedCustomer = null;

        txtCustomerPhone.setText("");

        lblCustomerInfo.setText("Khách vãng lai (0%)");

        calculateTotal();
    }

    private void refreshUIState() {
        loadProducts(); // Cập nhật lại tồn kho trong bảng
        JTextField txtEditor = (JTextField) cboSearch.getEditor().getEditorComponent();
        txtEditor.setText(SEARCH_HINT);
        txtEditor.setForeground(Color.GRAY);
    }

    private void notifySystemChanged() {
        try {
            String[] tags = {"ORDERS", "INVENTORY", "PRODUCTS", "CUSTOMERS"};
            for (String tag : tags) {
                SyncVersionDao.bumpVersion(tag); //
                RealtimeClient.send(tag + "_CHANGED"); //
                EventBus.publish(new AppDataChangedEvent(AppEventType.valueOf(tag), "Update after POS"));
            }
        } catch (Exception ignored) {
        }
    }

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
                        if (p.getQuantity() > 0) {
                            modProducts.addRow(new Object[]{p.getProductId(), p.getProductName(),
                                moneyFormat.format(p.getBasePrice()), p.getQuantity()});
                        }
                    }
                } catch (Exception ignored) {
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
                        cboPaymentMethod.addItem(pm.getPaymentMethodId());
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void addToCartExplicit(String productId, int qtyToAdd) {
        Product product = allProducts.stream().filter(p -> p.getProductId().equalsIgnoreCase(productId)).findFirst().orElse(null);
        if (product == null || product.getQuantity() <= 0) {
            JOptionPane.showMessageDialog(this, "Sản phẩm không khả dụng!");
            return;
        }

        double price = product.getBasePrice().doubleValue();
        for (int i = 0; i < modCart.getRowCount(); i++) {
            if (modCart.getValueAt(i, 0).toString().equalsIgnoreCase(productId)) {
                int newQty = (int) modCart.getValueAt(i, 2) + qtyToAdd;
                if (newQty <= product.getQuantity()) {
                    modCart.setValueAt(newQty, i, 2);
                    modCart.setValueAt(newQty * price, i, 4);
                    calculateTotal();
                } else {
                    JOptionPane.showMessageDialog(this, "Vượt quá tồn kho!");
                }
                return;
            }
        }

        if (qtyToAdd <= product.getQuantity()) {
            modCart.addRow(new Object[]{product.getProductId(), product.getProductName(), qtyToAdd, price, price * qtyToAdd});
            calculateTotal();
        }
    }

    private void calculateTotal() {
        currentTotal = 0;
        for (int i = 0; i < modCart.getRowCount(); i++) {
            currentTotal += (double) modCart.getValueAt(i, 4);
        }
        double rate = (selectedCustomer != null) ? selectedCustomer.getDiscountRate() : 0;
        finalAmountToPay = currentTotal * (1 - rate);

        if (rate > 0) {
            lblTotal.setText(String.format("Tổng: %s | Giảm: -%s | Trả: %s",
                    moneyFormat.format(currentTotal), moneyFormat.format(currentTotal * rate), moneyFormat.format(finalAmountToPay)));
        } else {
            lblTotal.setText("Tổng tiền: " + moneyFormat.format(currentTotal));
        }
    }

    private JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setRowHeight(35);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
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
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
