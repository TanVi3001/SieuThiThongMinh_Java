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
import java.sql.Date;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import common.events.EventBus;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;

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
        // --- SỰ KIỆN TÌM VÀ ĐĂNG KÝ KHÁCH HÀNG NHANH ---
        btnFindCustomer.addActionListener(e -> {
            String phone = txtCustomerPhone.getText().trim();
            if (phone.isEmpty()) {
                selectedCustomer = null;
                lblCustomerInfo.setText("Khách vãng lai (0%)");
                calculateTotal();
                return;
            }

            selectedCustomer = CustomersSql.getInstance().findByPhone(phone);
            if (selectedCustomer != null) {
                lblCustomerInfo.setText(String.format("Hạng: %s | Giảm: %.0f%% | T.Chi: %s",
                        selectedCustomer.getMemberRank(),
                        selectedCustomer.getDiscountRate() * 100,
                        moneyFormat.format(selectedCustomer.getTotalSpending())));
                calculateTotal();
            } else {
                int addConfirm = JOptionPane.showConfirmDialog(this, "Không tìm thấy khách hàng với SĐT này! Bạn có muốn đăng ký ngay không?", "Khách hàng mới", JOptionPane.YES_NO_OPTION);
                if (addConfirm == JOptionPane.YES_OPTION) {
                    // Mở Form đăng ký nhanh
                    JTextField txtNewName = new JTextField();
                    Object[] message = {
                        "Tên khách hàng (*):", txtNewName,
                        "Số điện thoại:", new JLabel(phone)
                    };
                    int option = JOptionPane.showConfirmDialog(this, message, "Đăng ký nhanh tại quầy", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                    if (option == JOptionPane.OK_OPTION) {
                        String newName = txtNewName.getText().trim();
                        if (!newName.isEmpty()) {
                            Customer newCus = new Customer();
                            newCus.setCustomerId("CUS" + System.currentTimeMillis());
                            newCus.setCustomerName(newName);
                            newCus.setPhone(phone);
                            newCus.setRewardPoints(0);

                            if (CustomersSql.getInstance().insert(newCus) > 0) {
                                try {
                                    SyncVersionDao.bumpVersion("CUSTOMERS");
                                    RealtimeClient.send("CUSTOMERS_CHANGED");
                                } catch (Exception ignored) {
                                }

                                // Gán ngay cho khách vừa tạo
                                selectedCustomer = CustomersSql.getInstance().findByPhone(phone);
                                lblCustomerInfo.setText(String.format("Hạng: %s | Giảm: %.0f%% | T.Chi: %s",
                                        selectedCustomer.getMemberRank(),
                                        selectedCustomer.getDiscountRate() * 100,
                                        moneyFormat.format(selectedCustomer.getTotalSpending())));
                                calculateTotal();
                                JOptionPane.showMessageDialog(this, "✅ Đăng ký thành công! Đã tự động áp dụng thông tin cho đơn này.");
                            } else {
                                JOptionPane.showMessageDialog(this, "❌ Đăng ký thất bại do lỗi Database!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                            }
                        } else {
                            JOptionPane.showMessageDialog(this, "⚠️ Tên khách hàng không được để trống!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                        }
                    }
                } else {
                    selectedCustomer = null;
                    lblCustomerInfo.setText("Khách vãng lai (0%)");
                    calculateTotal();
                }
            }
        });

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

    // --- TÍNH LẠI TIỀN BILL, TRỪ CHIẾT KHẤU ĐẬM CHẤT TIẾNG VIỆT ---
    private void calculateTotal() {
        currentTotal = 0;
        for (int i = 0; i < modCart.getRowCount(); i++) {
            currentTotal += (double) modCart.getValueAt(i, 4);
        }

        double discountRate = selectedCustomer != null ? selectedCustomer.getDiscountRate() : 0.0;
        double discountAmount = currentTotal * discountRate;
        finalAmountToPay = currentTotal - discountAmount;

        if (discountRate > 0) {
            lblTotal.setText(String.format("Tổng: %s | Giảm: -%s | Trả: %s",
                    moneyFormat.format(currentTotal),
                    moneyFormat.format(discountAmount),
                    moneyFormat.format(finalAmountToPay)));
        } else {
            lblTotal.setText("Tổng tiền: " + moneyFormat.format(currentTotal));
        }
    }

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
            order.setCustomerId(selectedCustomer != null ? selectedCustomer.getCustomerId() : null);
            order.setEmployeeId(empId);
            order.setPaymentMethodId(pm);
            order.setOrderDate(new java.sql.Date(System.currentTimeMillis()));
            order.setTotalAmount(finalAmountToPay);
            order.setStatus("Hoàn thành");
            order.setNote("POS Bán trực tiếp");
            order.setDeleted(false);

            List<OrderDetail> details = new ArrayList<>();
            for (int i = 0; i < modCart.getRowCount(); i++) {
                String pId = modCart.getValueAt(i, 0).toString();
                int qty = (int) modCart.getValueAt(i, 2);
                double price = (double) modCart.getValueAt(i, 3);

                String unitId = "U_CAI";
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

                selectedCustomer = null;
                lblCustomerInfo.setText("Khách vãng lai (0%)");
                txtCustomerPhone.setText("");
                calculateTotal();

                loadProducts();

                JTextField txtEditor = (JTextField) cboSearch.getEditor().getEditorComponent();
                txtEditor.setText(SEARCH_HINT);
                txtEditor.setForeground(Color.GRAY);
                txtEditor.requestFocus();

                try {
                    EventBus.publish(new AppDataChangedEvent(AppEventType.ORDERS, "Có bill POS mới"));
                    EventBus.publish(new AppDataChangedEvent(AppEventType.INVENTORY, "Tồn kho bị giảm"));
                    EventBus.publish(new AppDataChangedEvent(AppEventType.PRODUCTS, "Cập nhật sản phẩm"));
                    EventBus.publish(new AppDataChangedEvent(AppEventType.CUSTOMERS, "Cập nhật chi tiêu khách"));

                    SyncVersionDao.bumpVersion("ORDERS");
                    SyncVersionDao.bumpVersion("INVENTORY");
                    SyncVersionDao.bumpVersion("PRODUCTS");
                    SyncVersionDao.bumpVersion("CUSTOMERS");

                    RealtimeClient.send("ORDERS_CHANGED");
                    RealtimeClient.send("INVENTORY_CHANGED");
                    RealtimeClient.send("PRODUCTS_CHANGED");
                    RealtimeClient.send("CUSTOMERS_CHANGED");
                } catch (Exception ignored) {
                }

            } else {
                JOptionPane.showMessageDialog(this, "❌ Thanh toán thất bại!", "Lỗi DB", JOptionPane.ERROR_MESSAGE);
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
