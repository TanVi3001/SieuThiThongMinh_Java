package view;

import business.service.PaymentService;
import business.service.SessionManager;
import business.sql.prod_inventory.ProductUnitsSql;
import business.sql.prod_inventory.ProductsSql;
import business.sql.sales_order.CustomersSql;
import business.sql.sales_order.PaymentMethodsSql;
import model.order.Customer;
import model.order.Order;
import model.order.OrderDetail;
import model.payment.PaymentMethod;
import model.product.Product;
import model.product.ProductUnit;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SellPanel — Màn hình bán hàng POS đầy đủ.
 */
public class SellPanel extends javax.swing.JPanel {

    // ── Search & product info ──
    private JTextField txtSearch;
    private JButton btnSearch;
    private JList<String> listProducts;
    private DefaultListModel<String> listModel;
    private List<Product> searchResults = new ArrayList<>();

    // ── Unit & quantity ──
    private JComboBox<String> cboUnit;
    private List<ProductUnit> currentUnits = new ArrayList<>();
    private JSpinner spnQty;
    private JButton btnAddToCart;

    // ── Cart table ──
    private JTable tblCart;
    private DefaultTableModel cartModel;
    // columns: STT | productId | Tên SP | unitId | Đơn vị | SL | Đơn giá | Thành tiền
    private static final int COL_IDX     = 0;
    private static final int COL_PID     = 1;
    private static final int COL_NAME    = 2;
    private static final int COL_UNITID  = 3;
    private static final int COL_UNIT    = 4;
    private static final int COL_QTY     = 5;
    private static final int COL_PRICE   = 6;
    private static final int COL_TOTAL   = 7;

    private JButton btnRemoveRow;
    private JLabel lblTotal;

    // ── Customer ──
    private JTextField txtCusSearch;
    private JButton btnFindCus;
    private JLabel lblCusInfo;
    private Customer currentCustomer = null;

    // ── Payment ──
    private JComboBox<String> cboPM;
    private List<PaymentMethod> paymentMethods = new ArrayList<>();
    private JTextField txtCash;
    private JLabel lblChange;

    // ── Confirm ──
    private JTextField txtNote;
    private JButton btnPay;
    private JButton btnReset;

    public SellPanel() {
        buildUI();
        loadPaymentMethods();
    }

    // ────────────────────────────────────────────────────────────────
    //  BUILD UI
    // ────────────────────────────────────────────────────────────────
    private void buildUI() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(new Color(236, 240, 241));

        // LEFT panel
        JPanel leftPanel = buildLeftPanel();
        leftPanel.setPreferredSize(new Dimension(280, 0));

        // CENTER/RIGHT panel
        JPanel rightPanel = buildRightPanel();

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);
    }

    private JPanel buildLeftPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(236, 240, 241));
        p.setBorder(BorderFactory.createTitledBorder("Tìm sản phẩm"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;

        // Search row
        txtSearch = new JTextField();
        txtSearch.setToolTipText("Nhập tên sản phẩm để tìm");
        btnSearch = new JButton("Tìm");

        c.gridx = 0; c.gridy = 0; c.gridwidth = 2;
        p.add(txtSearch, c);
        c.gridx = 2; c.gridwidth = 1; c.weightx = 0;
        p.add(btnSearch, c);

        // Result list
        listModel = new DefaultListModel<>();
        listProducts = new JList<>(listModel);
        listProducts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane spList = new JScrollPane(listProducts);
        spList.setPreferredSize(new Dimension(260, 140));

        c.gridx = 0; c.gridy = 1; c.gridwidth = 3; c.weightx = 1; c.fill = GridBagConstraints.BOTH; c.weighty = 0.4;
        p.add(spList, c);

        // Unit
        c.gridy = 2; c.weighty = 0; c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = 1;
        p.add(new JLabel("Đơn vị:"), c);
        c.gridx = 1; c.gridwidth = 2;
        cboUnit = new JComboBox<>();
        p.add(cboUnit, c);

        // Qty
        c.gridx = 0; c.gridy = 3; c.gridwidth = 1;
        p.add(new JLabel("Số lượng:"), c);
        c.gridx = 1; c.gridwidth = 2;
        spnQty = new JSpinner(new SpinnerNumberModel(1, 1, 9999, 1));
        p.add(spnQty, c);

        // Add button
        c.gridx = 0; c.gridy = 4; c.gridwidth = 3;
        btnAddToCart = new JButton("➕ Thêm vào giỏ");
        btnAddToCart.setBackground(new Color(52, 152, 219));
        btnAddToCart.setForeground(Color.WHITE);
        btnAddToCart.setFont(btnAddToCart.getFont().deriveFont(Font.BOLD));
        p.add(btnAddToCart, c);

        // Customer section
        c.gridy = 5;
        p.add(new JSeparator(), c);

        c.gridy = 6;
        JPanel cusTitlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cusTitlePanel.setOpaque(false);
        cusTitlePanel.add(new JLabel("<html><b>Khách hàng (tuỳ chọn)</b></html>"));
        p.add(cusTitlePanel, c);

        c.gridy = 7; c.gridwidth = 2;
        txtCusSearch = new JTextField();
        txtCusSearch.setToolTipText("Nhập tên hoặc SĐT");
        p.add(txtCusSearch, c);
        c.gridx = 2; c.gridwidth = 1; c.weightx = 0;
        btnFindCus = new JButton("Tìm");
        p.add(btnFindCus, c);

        c.gridx = 0; c.gridy = 8; c.gridwidth = 3; c.weightx = 1;
        lblCusInfo = new JLabel("<html><i>Chưa chọn khách hàng</i></html>");
        p.add(lblCusInfo, c);

        // Payment method
        c.gridy = 9;
        p.add(new JSeparator(), c);

        c.gridy = 10;
        p.add(new JLabel("<html><b>Phương thức thanh toán:</b></html>"), c);

        c.gridy = 11;
        cboPM = new JComboBox<>();
        p.add(cboPM, c);

        // Cash fields
        c.gridy = 12; c.gridwidth = 1;
        p.add(new JLabel("Tiền khách đưa:"), c);
        c.gridx = 1; c.gridwidth = 2;
        txtCash = new JTextField("0");
        p.add(txtCash, c);

        c.gridx = 0; c.gridy = 13; c.gridwidth = 3;
        lblChange = new JLabel("Tiền thừa: 0 đ");
        lblChange.setForeground(new Color(39, 174, 96));
        lblChange.setFont(lblChange.getFont().deriveFont(Font.BOLD));
        p.add(lblChange, c);

        // Note
        c.gridy = 14;
        p.add(new JLabel("Ghi chú:"), c);
        c.gridy = 15;
        txtNote = new JTextField();
        p.add(txtNote, c);

        // Buttons
        c.gridy = 16;
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 6, 0));
        btnPanel.setOpaque(false);
        btnPay = new JButton("✔ Thanh toán");
        btnPay.setBackground(new Color(39, 174, 96));
        btnPay.setForeground(Color.WHITE);
        btnPay.setFont(btnPay.getFont().deriveFont(Font.BOLD, 13f));

        btnReset = new JButton("↺ Đặt lại");
        btnReset.setBackground(new Color(231, 76, 60));
        btnReset.setForeground(Color.WHITE);
        btnPanel.add(btnPay);
        btnPanel.add(btnReset);
        p.add(btnPanel, c);

        // filler
        c.gridy = 17; c.weighty = 1;
        p.add(new JLabel(), c);

        // Wire events
        btnSearch.addActionListener(e -> doSearch());
        txtSearch.addActionListener(e -> doSearch());
        listProducts.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) onProductSelected();
            }
        });
        listProducts.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) onProductSelected();
            }
        });
        btnAddToCart.addActionListener(e -> addToCart());
        btnFindCus.addActionListener(e -> findCustomer());
        txtCusSearch.addActionListener(e -> findCustomer());
        cboPM.addActionListener(e -> updateCashVisibility());
        txtCash.addKeyListener(new KeyAdapter() {
            @Override public void keyReleased(KeyEvent e) { updateChange(); }
        });
        btnPay.addActionListener(e -> doPayment());
        btnReset.addActionListener(e -> resetForm());

        return p;
    }

    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout(4, 4));
        p.setOpaque(false);

        // Cart table - hidden columns: productId, unitId
        cartModel = new DefaultTableModel(
            new String[]{"STT", "productId", "Tên sản phẩm", "unitId", "Đơn vị", "SL", "Đơn giá", "Thành tiền"}, 0
        ) {
            @Override public boolean isCellEditable(int row, int col) {
                return col == COL_QTY; // chỉ cho sửa số lượng
            }
        };
        tblCart = new JTable(cartModel);
        tblCart.setRowHeight(26);
        // Hide productId and unitId columns
        hideColumn(COL_PID);
        hideColumn(COL_UNITID);

        // SL column edit triggers recalc
        cartModel.addTableModelListener(e -> {
            if (e.getColumn() == COL_QTY) {
                int row = e.getFirstRow();
                recalcRow(row);
                updateTotal();
            }
        });

        JScrollPane sp = new JScrollPane(tblCart);
        p.add(sp, BorderLayout.CENTER);

        // Bottom bar
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        bottom.setOpaque(false);
        btnRemoveRow = new JButton("🗑 Xoá dòng");
        btnRemoveRow.addActionListener(e -> removeSelectedRow());
        lblTotal = new JLabel("Tổng cộng: 0 đ");
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 15f));
        lblTotal.setForeground(new Color(192, 57, 43));
        bottom.add(btnRemoveRow);
        bottom.add(lblTotal);
        p.add(bottom, BorderLayout.SOUTH);

        return p;
    }

    private void hideColumn(int col) {
        tblCart.getColumnModel().getColumn(col).setMinWidth(0);
        tblCart.getColumnModel().getColumn(col).setMaxWidth(0);
        tblCart.getColumnModel().getColumn(col).setWidth(0);
    }

    // ────────────────────────────────────────────────────────────────
    //  LOGIC
    // ────────────────────────────────────────────────────────────────

    private void loadPaymentMethods() {
        SwingWorker<List<PaymentMethod>, Void> w = new SwingWorker<>() {
            @Override protected List<PaymentMethod> doInBackground() {
                return PaymentMethodsSql.getInstance().selectAll();
            }
            @Override protected void done() {
                try {
                    paymentMethods = get();
                    cboPM.removeAllItems();
                    for (PaymentMethod pm : paymentMethods) cboPM.addItem(pm.getPaymentMethodId());
                    updateCashVisibility();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        w.execute();
    }

    private void doSearch() {
        String kw = txtSearch.getText().trim();
        if (kw.isEmpty()) return;
        SwingWorker<List<Product>, Void> w = new SwingWorker<>() {
            @Override protected List<Product> doInBackground() {
                return ProductsSql.getInstance().searchByName(kw);
            }
            @Override protected void done() {
                try {
                    searchResults = get();
                    listModel.clear();
                    if (searchResults.isEmpty()) {
                        listModel.addElement("Không tìm thấy kết quả");
                    } else {
                        for (Product p : searchResults) {
                            String unit = p.getUnit() != null ? p.getUnit().toString() : "";
                            listModel.addElement(String.format("[%s] %s — %,.0f đ — Tồn: %d %s",
                                p.getProductId(), p.getProductName(),
                                p.getBasePrice() != null ? p.getBasePrice().doubleValue() : 0,
                                p.getQuantity(), unit));
                        }
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        w.execute();
    }

    private void onProductSelected() {
        int idx = listProducts.getSelectedIndex();
        if (idx < 0 || idx >= searchResults.size()) return;
        Product p = searchResults.get(idx);
        // Load units for this product
        SwingWorker<List<ProductUnit>, Void> w = new SwingWorker<>() {
            @Override protected List<ProductUnit> doInBackground() {
                return ProductUnitsSql.getInstance().selectByProductId(p.getProductId());
            }
            @Override protected void done() {
                try {
                    currentUnits = get();
                    cboUnit.removeAllItems();
                    if (currentUnits.isEmpty()) {
                        // fallback: use inventory unit
                        String u = p.getUnit() != null ? p.getUnit().toString() : "Cái";
                        cboUnit.addItem(u);
                    } else {
                        for (ProductUnit pu : currentUnits) cboUnit.addItem(pu.getUnitId());
                    }
                    spnQty.setValue(1);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        w.execute();
    }

    private void addToCart() {
        int idx = listProducts.getSelectedIndex();
        if (idx < 0 || idx >= searchResults.size()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm từ danh sách tìm kiếm.", "Chú ý", JOptionPane.WARNING_MESSAGE);
            return;
        }
        Product p = searchResults.get(idx);
        int qty = (int) spnQty.getValue();
        if (qty <= 0) { JOptionPane.showMessageDialog(this, "Số lượng phải > 0"); return; }

        String unitId = cboUnit.getSelectedItem() != null ? cboUnit.getSelectedItem().toString() : "";
        double price = p.getBasePrice() != null ? p.getBasePrice().doubleValue() : 0;
        double total = price * qty;

        // Check duplicate: same product + same unit → merge
        for (int row = 0; row < cartModel.getRowCount(); row++) {
            if (p.getProductId().equals(cartModel.getValueAt(row, COL_PID))
                    && unitId.equals(cartModel.getValueAt(row, COL_UNITID))) {
                int existQty = Integer.parseInt(cartModel.getValueAt(row, COL_QTY).toString());
                cartModel.setValueAt(existQty + qty, row, COL_QTY);
                recalcRow(row);
                updateTotal();
                return;
            }
        }

        int stt = cartModel.getRowCount() + 1;
        cartModel.addRow(new Object[]{stt, p.getProductId(), p.getProductName(), unitId, unitId, qty, price, total});
        updateTotal();
    }

    private void recalcRow(int row) {
        try {
            int qty = Integer.parseInt(cartModel.getValueAt(row, COL_QTY).toString());
            double price = Double.parseDouble(cartModel.getValueAt(row, COL_PRICE).toString());
            cartModel.setValueAt(price * qty, row, COL_TOTAL);
        } catch (Exception ignored) {}
    }

    private void removeSelectedRow() {
        int row = tblCart.getSelectedRow();
        if (row < 0) return;
        cartModel.removeRow(row);
        // renumber
        for (int i = 0; i < cartModel.getRowCount(); i++) cartModel.setValueAt(i + 1, i, COL_IDX);
        updateTotal();
    }

    private double getTotal() {
        double total = 0;
        for (int i = 0; i < cartModel.getRowCount(); i++) {
            try { total += Double.parseDouble(cartModel.getValueAt(i, COL_TOTAL).toString()); } catch (Exception ignored) {}
        }
        return total;
    }

    private void updateTotal() {
        double t = getTotal();
        lblTotal.setText(String.format("Tổng cộng: %,.0f đ", t));
        updateChange();
    }

    private void updateCashVisibility() {
        String pm = cboPM.getSelectedItem() != null ? cboPM.getSelectedItem().toString() : "";
        boolean isCash = pm.toLowerCase().contains("cash") || pm.toLowerCase().contains("tien_mat")
                || pm.toLowerCase().contains("tiền mặt") || pm.toLowerCase().contains("cash_payment");
        txtCash.setEnabled(isCash);
        lblChange.setVisible(isCash);
    }

    private void updateChange() {
        try {
            double cash = Double.parseDouble(txtCash.getText().replace(",", "").trim());
            double change = cash - getTotal();
            lblChange.setText(String.format("Tiền thừa: %,.0f đ", Math.max(0, change)));
            lblChange.setForeground(change >= 0 ? new Color(39, 174, 96) : new Color(192, 57, 43));
        } catch (NumberFormatException ignored) {
            lblChange.setText("Tiền thừa: — đ");
        }
    }

    private void findCustomer() {
        String kw = txtCusSearch.getText().trim();
        if (kw.isEmpty()) return;
        SwingWorker<List<Customer>, Void> w = new SwingWorker<>() {
            @Override protected List<Customer> doInBackground() {
                return CustomersSql.getInstance().search(kw);
            }
            @Override protected void done() {
                try {
                    List<Customer> results = get();
                    if (results.isEmpty()) {
                        currentCustomer = null;
                        lblCusInfo.setText("<html><i style='color:red'>Không tìm thấy khách hàng</i></html>");
                    } else if (results.size() == 1) {
                        currentCustomer = results.get(0);
                        showCustomerInfo();
                    } else {
                        // Multiple — let user pick
                        String[] names = results.stream()
                            .map(c -> c.getCustomerId() + " — " + c.getCustomerName() + " (" + c.getPhone() + ")")
                            .toArray(String[]::new);
                        String chosen = (String) JOptionPane.showInputDialog(SellPanel.this,
                            "Chọn khách hàng:", "Kết quả tìm kiếm",
                            JOptionPane.PLAIN_MESSAGE, null, names, names[0]);
                        if (chosen != null) {
                            String chosenId = chosen.split(" — ")[0];
                            currentCustomer = results.stream().filter(c -> c.getCustomerId().equals(chosenId)).findFirst().orElse(null);
                            if (currentCustomer != null) showCustomerInfo();
                        }
                    }
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        };
        w.execute();
    }

    private void showCustomerInfo() {
        lblCusInfo.setText(String.format("<html><b>%s</b><br/>%s | Điểm: %d</html>",
            currentCustomer.getCustomerName(),
            currentCustomer.getPhone() != null ? currentCustomer.getPhone() : "",
            currentCustomer.getRewardPoints()));
    }

    private void doPayment() {
        if (cartModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Giỏ hàng trống!", "Chú ý", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (cboPM.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn phương thức thanh toán.", "Chú ý", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Check cash enough
        String pm = cboPM.getSelectedItem().toString();
        boolean isCash = pm.toLowerCase().contains("cash") || pm.toLowerCase().contains("tien_mat")
                || pm.toLowerCase().contains("tiền mặt");
        if (isCash) {
            try {
                double cash = Double.parseDouble(txtCash.getText().replace(",", "").trim());
                if (cash < getTotal()) {
                    JOptionPane.showMessageDialog(this, "Tiền khách đưa không đủ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Số tiền khách đưa không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        // Build Order
        String staffId = SessionManager.getCurrentUser() != null
                ? SessionManager.getCurrentUser().getAccountId() : "STAFF-001";
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String customerId = currentCustomer != null ? currentCustomer.getCustomerId() : null;
        String paymentMethodId = pm;
        String note = txtNote.getText().trim();
        double total = getTotal();

        Order order = new Order(orderId, customerId, staffId, paymentMethodId,
                new Date(System.currentTimeMillis()), total, "COMPLETED", note, false);

        // Build OrderDetails
        List<OrderDetail> details = new ArrayList<>();
        for (int row = 0; row < cartModel.getRowCount(); row++) {
            String productId = cartModel.getValueAt(row, COL_PID).toString();
            String unitId    = cartModel.getValueAt(row, COL_UNITID).toString();
            int    qty       = Integer.parseInt(cartModel.getValueAt(row, COL_QTY).toString());
            double price     = Double.parseDouble(cartModel.getValueAt(row, COL_PRICE).toString());
            // orderId set here; PaymentService will convert to base unit
            details.add(new OrderDetail(orderId, productId, qty, price, unitId, 0));
        }

        // Run in background
        btnPay.setEnabled(false);
        SwingWorker<Boolean, Void> w = new SwingWorker<>() {
            @Override protected Boolean doInBackground() {
                return PaymentService.thanhToan(order, details);
            }
            @Override protected void done() {
                try {
                    boolean ok = get();
                    if (ok) {
                        // Reward points: +1 per 10,000đ
                        if (currentCustomer != null) {
                            int earned = (int)(total / 10_000);
                            if (earned > 0) {
                                int newPoints = currentCustomer.getRewardPoints() + earned;
                                CustomersSql.getInstance().updateRewardPoints(currentCustomer.getCustomerId(), newPoints);
                            }
                        }
                        JOptionPane.showMessageDialog(SellPanel.this,
                            String.format("✅ Thanh toán thành công!\nMã HĐ: %s\nTổng tiền: %,.0f đ", orderId, total),
                            "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        resetForm();
                    } else {
                        JOptionPane.showMessageDialog(SellPanel.this,
                            "❌ Thanh toán thất bại! Kiểm tra log để biết chi tiết.",
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(SellPanel.this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnPay.setEnabled(true);
                }
            }
        };
        w.execute();
    }

    private void resetForm() {
        txtSearch.setText("");
        listModel.clear();
        searchResults.clear();
        cboUnit.removeAllItems();
        spnQty.setValue(1);
        cartModel.setRowCount(0);
        lblTotal.setText("Tổng cộng: 0 đ");
        txtCusSearch.setText("");
        lblCusInfo.setText("<html><i>Chưa chọn khách hàng</i></html>");
        currentCustomer = null;
        txtCash.setText("0");
        lblChange.setText("Tiền thừa: 0 đ");
        txtNote.setText("");
    }

    // ────────────────────────────────────────────────────────────────
    //  NetBeans compatibility stubs (empty — UI is built above)
    // ────────────────────────────────────────────────────────────────
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        // intentionally empty — UI built programmatically in buildUI()
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 400, Short.MAX_VALUE));
        layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING).addGap(0, 300, Short.MAX_VALUE));
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
