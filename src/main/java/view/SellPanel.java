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

// 🌟 Import 2 class KPI của bạn
import model.account.kpi.KpiCriteria;
import model.account.kpi.KpiEvaluation;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
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
    // COLOR PALETTE & CONSTANTS
    // =========================================================
    private final Color BG_LIGHT = new Color(245, 246, 250);
    private final Color CARD_WHITE = Color.WHITE;
    private final Color PRIMARY_BLUE = new Color(41, 128, 185);
    private final Color SUCCESS_GREEN = new Color(39, 174, 96);
    private final Color DANGER_RED = new Color(231, 76, 60);
    private final Color WARNING_YELLOW = new Color(241, 196, 15);
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color TEXT_GRAY = new Color(127, 140, 141);
    private final Color BORDER_GRAY = new Color(223, 228, 234);

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0 đ");
    private static final String SEARCH_HINT = "Nhập mã, tên SP hoặc scan mã vạch...";

    // =========================================================
    // UI COMPONENTS
    // =========================================================
    private JComboBox<String> cboSearch;
    private JSpinner spnQtyAdd;
    private ModernButton btnAdd;

    private JTable tblProducts;
    private DefaultTableModel modProducts;

    private JTable tblCart;
    private DefaultTableModel modCart;
    private JPanel pnlWarning;
    private JLabel lblWarningMsg;

    private JTextField txtCustomerPhone;
    private ModernButton btnFindCustomer;
    private JLabel lblCusName, lblCusRank, lblCusTotalSpend;
    private JPanel pnlRankBadge;

    private JComboBox<String> cboPaymentMethod;
    private JLabel lblSubTotal, lblDiscount, lblTotalPay;
    private ModernButton btnPay, btnCancel, btnRemove;

    private ToggleButton chkAutoDiscount, chkRealtimeSync, chkPrintBill;

    private JLabel lblKpiItems, lblKpiTotal, lblKpiStatus;

    // =========================================================
    // DATA
    // =========================================================
    private List<Product> allProducts = new ArrayList<>();
    private Customer selectedCustomer;
    private double currentTotal = 0;
    private double finalAmountToPay = 0;
    private int totalItems = 0;

    // Đối tượng đánh giá KPI
    private KpiEvaluation currentKpiEval = new KpiEvaluation();

    // Flag chống lỗi vòng lặp (StackOverflow) khi cập nhật JTable
    private boolean isUpdatingCart = false;

    // =========================================================
    // INIT
    // =========================================================
    public SellPanel() {
        setLayout(new BorderLayout(15, 15));
        setBackground(BG_LIGHT);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        buildUI();
        initEvents();
        loadProducts();
        loadPaymentMethods();

        // REALTIME SUBSCRIPTION
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e.getType() == AppEventType.PRODUCTS
                    || e.getType() == AppEventType.INVENTORY
                    || e.getType() == AppEventType.ORDERS) {
                SwingUtilities.invokeLater(() -> {
                    if (chkRealtimeSync.isSelected()) {
                        loadProducts();
                        refreshCartRealtime();
                    }
                });
            }
        });
    }

    // =========================================================
    // BUILD UI
    // =========================================================
    private void buildUI() {
        add(buildTopBar(), BorderLayout.NORTH);

        JPanel pnlCenter = new JPanel(new GridLayout(1, 2, 15, 0));
        pnlCenter.setOpaque(false);
        pnlCenter.add(buildProductPanel());
        pnlCenter.add(buildCartPanel());
        add(pnlCenter, BorderLayout.CENTER);

        JPanel pnlBottom = new JPanel(new BorderLayout(15, 0));
        pnlBottom.setOpaque(false);
        pnlBottom.add(buildCustomerAndSettingsPanel(), BorderLayout.WEST);
        pnlBottom.add(buildPaymentPanel(), BorderLayout.EAST);
        add(pnlBottom, BorderLayout.SOUTH);
    }

    private JPanel buildTopBar() {
        ModernCardPanel pnl = new ModernCardPanel(10);
        pnl.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 12));
        pnl.setPreferredSize(new Dimension(0, 65));

        JLabel lblIcon = new JLabel("🔍");
        cboSearch = new JComboBox<>();
        cboSearch.setEditable(true);
        cboSearch.setPreferredSize(new Dimension(400, 40));
        cboSearch.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        spnQtyAdd = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spnQtyAdd.setPreferredSize(new Dimension(70, 40));

        btnAdd = new ModernButton("Thêm vào giỏ", PRIMARY_BLUE, Color.WHITE);
        btnAdd.setPreferredSize(new Dimension(130, 40));

        pnl.add(lblIcon);
        pnl.add(cboSearch);
        pnl.add(new JLabel(" SL: "));
        pnl.add(spnQtyAdd);
        pnl.add(btnAdd);

        return pnl;
    }

    private JPanel buildProductPanel() {
        ModernCardPanel pnl = new ModernCardPanel(10);
        pnl.setLayout(new BorderLayout(0, 10));
        pnl.setBorder(new EmptyBorder(15, 15, 15, 15));

        JLabel lblTitle = new JLabel("📦 DANH SÁCH SẢN PHẨM");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(TEXT_DARK);
        pnl.add(lblTitle, BorderLayout.NORTH);

        modProducts = new DefaultTableModel(new Object[]{"Mã SP", "Tên SP", "Giá bán", "Kho"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tblProducts = createTable(modProducts);
        tblProducts.getColumnModel().getColumn(1).setPreferredWidth(250);

        pnl.add(new JScrollPane(tblProducts), BorderLayout.CENTER);
        return pnl;
    }

    private JPanel buildCartPanel() {
        ModernCardPanel pnl = new ModernCardPanel(10);
        pnl.setLayout(new BorderLayout(0, 10));
        pnl.setBorder(new EmptyBorder(15, 15, 15, 15));

        // Header & Actions
        JPanel pnlHeaderTop = new JPanel(new BorderLayout());
        pnlHeaderTop.setOpaque(false);
        JLabel lblTitle = new JLabel("🛒 GIỎ HÀNG HIỆN TẠI");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(SUCCESS_GREEN);

        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlActions.setOpaque(false);
        btnRemove = new ModernButton("➖ Xóa món", new Color(149, 165, 166), Color.WHITE);
        btnRemove.setPreferredSize(new Dimension(110, 32));
        btnCancel = new ModernButton("🗑 Hủy đơn", DANGER_RED, Color.WHITE);
        btnCancel.setPreferredSize(new Dimension(110, 32));
        pnlActions.add(btnRemove);
        pnlActions.add(btnCancel);

        pnlHeaderTop.add(lblTitle, BorderLayout.WEST);
        pnlHeaderTop.add(pnlActions, BorderLayout.EAST);

        // Cảnh báo
        pnlWarning = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlWarning.setBackground(new Color(253, 237, 236));
        lblWarningMsg = new JLabel("⚠ Lỗi: Có sản phẩm vượt tồn kho!");
        lblWarningMsg.setForeground(DANGER_RED);
        lblWarningMsg.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pnlWarning.add(lblWarningMsg);
        pnlWarning.setVisible(false);

        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setOpaque(false);
        pnlHeader.add(pnlHeaderTop, BorderLayout.NORTH);
        pnlHeader.add(pnlWarning, BorderLayout.SOUTH);
        pnl.add(pnlHeader, BorderLayout.NORTH);

        // Bảng Cart
        modCart = new DefaultTableModel(new Object[]{"Mã SP", "Sản phẩm", "Số lượng", "Đơn giá", "Thành tiền", "Tồn", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2;
            }
        };
        tblCart = createTable(modCart);
        tblCart.getColumnModel().getColumn(1).setPreferredWidth(220);
        tblCart.getColumnModel().getColumn(2).setPreferredWidth(100);

        tblCart.getColumnModel().getColumn(2).setCellRenderer(new QuantitySpinnerRenderer());
        tblCart.getColumnModel().getColumn(2).setCellEditor(new QuantitySpinnerEditor());

        CartTableRenderer cartRenderer = new CartTableRenderer();
        for (int i = 0; i < tblCart.getColumnCount(); i++) {
            if (i != 2) {
                tblCart.getColumnModel().getColumn(i).setCellRenderer(cartRenderer);
            }
        }

        pnl.add(new JScrollPane(tblCart), BorderLayout.CENTER);
        return pnl;
    }

    private JPanel buildCustomerAndSettingsPanel() {
        JPanel pnlCombine = new JPanel(new BorderLayout(0, 15));
        pnlCombine.setOpaque(false);
        pnlCombine.setPreferredSize(new Dimension(450, 0));

        // --- 1. Customer Panel ---
        ModernCardPanel pnlCust = new ModernCardPanel(10);
        pnlCust.setLayout(new GridBagLayout());
        pnlCust.setBorder(new EmptyBorder(10, 15, 10, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        txtCustomerPhone = new JTextField();
        txtCustomerPhone.setPreferredSize(new Dimension(140, 35));
        btnFindCustomer = new ModernButton("Tìm KH", PRIMARY_BLUE, Color.WHITE);
        btnFindCustomer.setPreferredSize(new Dimension(80, 35));

        lblCusName = new JLabel("Khách vãng lai");
        lblCusName.setFont(new Font("Segoe UI", Font.BOLD, 14));

        lblCusRank = new JLabel("Rank: Thường");
        pnlRankBadge = new JPanel(new BorderLayout());
        pnlRankBadge.setOpaque(false);
        pnlRankBadge.add(lblCusRank, BorderLayout.WEST);

        lblCusTotalSpend = new JLabel("Chi tiêu: 0 đ");

        gbc.gridx = 0;
        gbc.gridy = 0;
        pnlCust.add(new JLabel("SĐT:"), gbc);
        gbc.gridx = 1;
        pnlCust.add(txtCustomerPhone, gbc);
        gbc.gridx = 2;
        pnlCust.add(btnFindCustomer, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        pnlCust.add(new JSeparator(), gbc);

        gbc.gridy = 2;
        gbc.gridwidth = 1;
        pnlCust.add(lblCusName, gbc);
        gbc.gridx = 1;
        pnlCust.add(pnlRankBadge, gbc);
        gbc.gridx = 2;
        pnlCust.add(lblCusTotalSpend, gbc);

        pnlCombine.add(pnlCust, BorderLayout.NORTH);

        // --- 2. KPI & Settings ---
        ModernCardPanel pnlKpiSet = new ModernCardPanel(10);
        pnlKpiSet.setLayout(new GridLayout(1, 2, 10, 0));
        pnlKpiSet.setBorder(new EmptyBorder(10, 15, 10, 15));

        JPanel pKpi = new JPanel(new GridLayout(4, 1));
        pKpi.setOpaque(false);
        pKpi.add(new JLabel("📊 KPI NHÂN VIÊN"));
        lblKpiItems = new JLabel("Tổng SP bán: 0");
        lblKpiTotal = new JLabel("Doanh thu: 0 đ");
        lblKpiStatus = new JLabel("Đánh giá: CHƯA ĐẠT");
        lblKpiStatus.setForeground(TEXT_GRAY);
        pKpi.add(lblKpiItems);
        pKpi.add(lblKpiTotal);
        pKpi.add(lblKpiStatus);

        JPanel pSet = new JPanel(new GridLayout(4, 1));
        pSet.setOpaque(false);
        pSet.add(new JLabel("⚙ CẤU HÌNH"));
        chkAutoDiscount = new ToggleButton("Tự áp mã giảm giá");
        chkAutoDiscount.setSelected(true);
        chkRealtimeSync = new ToggleButton("Đồng bộ kho realtime");
        chkRealtimeSync.setSelected(true);
        chkPrintBill = new ToggleButton("In hóa đơn khi thanh toán");
        pSet.add(chkAutoDiscount);
        pSet.add(chkRealtimeSync);
        pSet.add(chkPrintBill);

        pnlKpiSet.add(pKpi);
        pnlKpiSet.add(pSet);

        pnlCombine.add(pnlKpiSet, BorderLayout.CENTER);
        return pnlCombine;
    }

    private JPanel buildPaymentPanel() {
        ModernCardPanel pnl = new ModernCardPanel(10);
        pnl.setLayout(new GridBagLayout());
        pnl.setBorder(new EmptyBorder(15, 25, 15, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 10, 8, 10);

        cboPaymentMethod = new JComboBox<>();
        cboPaymentMethod.setPreferredSize(new Dimension(220, 40));
        cboPaymentMethod.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        lblSubTotal = new JLabel("0 đ", SwingConstants.RIGHT);
        lblSubTotal.setFont(new Font("Segoe UI", Font.BOLD, 15));

        lblDiscount = new JLabel("0 đ", SwingConstants.RIGHT);
        lblDiscount.setForeground(SUCCESS_GREEN);
        lblDiscount.setFont(new Font("Segoe UI", Font.BOLD, 15));

        lblTotalPay = new JLabel("0 đ", SwingConstants.RIGHT);
        lblTotalPay.setFont(new Font("Segoe UI", Font.BOLD, 32)); // TO KHỔNG LỒ
        lblTotalPay.setForeground(DANGER_RED);

        btnPay = new ModernButton("✔ THANH TOÁN", SUCCESS_GREEN, Color.WHITE);
        btnPay.setPreferredSize(new Dimension(0, 60)); // Nút thanh toán cao
        btnPay.setFont(new Font("Segoe UI", Font.BOLD, 20));
        btnPay.setEnabled(false);

        // Canh chỉnh dạng Hóa đơn
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.4;
        pnl.add(new JLabel("Phương thức TT:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        pnl.add(cboPaymentMethod, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        pnl.add(new JLabel("Tổng tạm tính:"), gbc);
        gbc.gridx = 1;
        pnl.add(lblSubTotal, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        pnl.add(new JLabel("Giảm giá thẻ:"), gbc);
        gbc.gridx = 1;
        pnl.add(lblDiscount, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        pnl.add(new JSeparator(), gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        JLabel lblT = new JLabel("TỔNG TIỀN:");
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 18));
        pnl.add(lblT, gbc);
        gbc.gridx = 1;
        pnl.add(lblTotalPay, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 10, 0, 10);
        pnl.add(btnPay, gbc);

        return pnl;
    }

    // =========================================================
    // LOGIC & EVENTS
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
                if (txtEditor.getText().isBlank()) {
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
                    String kw = txtEditor.getText().trim();
                    cboSearch.removeAllItems();
                    if (kw.isBlank() || kw.equals(SEARCH_HINT)) {
                        return;
                    }
                    cboSearch.addItem(kw);
                    boolean has = false;
                    for (Product p : allProducts) {
                        if (p.getQuantity() > 0 && (p.getProductId() + " " + p.getProductName()).toLowerCase().contains(kw.toLowerCase())) {
                            cboSearch.addItem(p.getProductId() + " - " + p.getProductName());
                            has = true;
                        }
                    }
                    if (has) {
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
                if (e.getClickCount() == 2 && tblProducts.getSelectedRow() >= 0) {
                    addToCart(tblProducts.getValueAt(tblProducts.getSelectedRow(), 0).toString(), (int) spnQtyAdd.getValue());
                }
            }
        });

        btnAdd.addActionListener(e -> {
            String sel = cboSearch.getSelectedItem() != null ? cboSearch.getSelectedItem().toString() : "";
            if (sel.isBlank() || sel.equals(SEARCH_HINT)) {
                if (tblProducts.getSelectedRow() < 0) {
                    JOptionPane.showMessageDialog(this, "Chọn SP cần thêm!");
                    return;
                }
                sel = tblProducts.getValueAt(tblProducts.getSelectedRow(), 0).toString();
            }
            addToCart(sel.split(" - ")[0].trim(), (int) spnQtyAdd.getValue());
            txtEditor.setText("");
            spnQtyAdd.setValue(1);
        });

        // 🌟 XÓA ĐƯỢC NHIỀU DÒNG CÙNG LÚC
        btnRemove.addActionListener(e -> {
            int[] selectedRows = tblCart.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn các sản phẩm trong giỏ để xóa!");
                return;
            }
            isUpdatingCart = true;
            for (int i = selectedRows.length - 1; i >= 0; i--) {
                modCart.removeRow(selectedRows[i]);
            }
            isUpdatingCart = false;
            calculateTotal();
            refreshCartRealtime();
        });

        btnCancel.addActionListener(e -> clearCart());
        txtCustomerPhone.addActionListener(e -> btnFindCustomer.doClick());
        btnFindCustomer.addActionListener(e -> findCustomer());
        btnPay.addActionListener(e -> processPayment());
        chkAutoDiscount.addActionListener(e -> calculateTotal());

        // CẬP NHẬT SPINNER TRONG GIỎ
        modCart.addTableModelListener(e -> {
            if (isUpdatingCart) {
                return;
            }

            if (e.getColumn() == 2 && e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int r = e.getFirstRow();
                if (r < 0 || r >= modCart.getRowCount()) {
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    isUpdatingCart = true;
                    try {
                        int qty = Integer.parseInt(modCart.getValueAt(r, 2).toString());
                        if (qty <= 0) {
                            if (JOptionPane.showConfirmDialog(this, "Xóa sản phẩm này?", "Xác nhận", JOptionPane.YES_NO_OPTION) == 0) {
                                modCart.removeRow(r);
                            } else {
                                modCart.setValueAt(1, r, 2);
                                double price = Double.parseDouble(modCart.getValueAt(r, 3).toString());
                                modCart.setValueAt(price * 1, r, 4);
                            }
                        } else {
                            double price = Double.parseDouble(modCart.getValueAt(r, 3).toString());
                            modCart.setValueAt(price * qty, r, 4);
                        }
                        calculateTotal();
                        refreshCartRealtime();
                    } catch (Exception ex) {
                    } finally {
                        isUpdatingCart = false;
                    }
                });
            }
        });
    }

    private void addToCart(String pId, int qty) {
        Product p = allProducts.stream().filter(x -> x.getProductId().equals(pId)).findFirst().orElse(null);
        if (p == null || p.getQuantity() <= 0) {
            return;
        }
        double price = p.getBasePrice().doubleValue();

        for (int i = 0; i < modCart.getRowCount(); i++) {
            if (modCart.getValueAt(i, 0).equals(pId)) {
                modCart.setValueAt(Integer.parseInt(modCart.getValueAt(i, 2).toString()) + qty, i, 2);
                return;
            }
        }

        isUpdatingCart = true;
        modCart.addRow(new Object[]{pId, p.getProductName(), qty, price, price * qty, p.getQuantity(), ""});
        isUpdatingCart = false;

        calculateTotal();
        refreshCartRealtime();
    }

    private void refreshCartRealtime() {
        boolean err = false;
        totalItems = 0;

        isUpdatingCart = true;
        for (int i = 0; i < modCart.getRowCount(); i++) {
            int q = Integer.parseInt(modCart.getValueAt(i, 2).toString());
            int stock = getStockFromDB(modCart.getValueAt(i, 0).toString());
            modCart.setValueAt(stock, i, 5);
            totalItems += q;
            if (q > stock) {
                err = true;
            }
        }
        isUpdatingCart = false;

        pnlWarning.setVisible(err);
        btnPay.setEnabled(!err && modCart.getRowCount() > 0);
        updateKpiMiniPanel();
        tblCart.repaint();
    }

    private void clearCart() {
        isUpdatingCart = true;
        modCart.setRowCount(0);
        isUpdatingCart = false;
        calculateTotal();
        refreshCartRealtime();
    }

    private void calculateTotal() {
        double sub = 0;
        for (int i = 0; i < modCart.getRowCount(); i++) {
            sub += Double.parseDouble(modCart.getValueAt(i, 4).toString());
        }

        double rate = (chkAutoDiscount.isSelected() && selectedCustomer != null) ? selectedCustomer.getDiscountRate() : 0;
        double disc = sub * rate;
        finalAmountToPay = sub - disc;

        lblSubTotal.setText(moneyFormat.format(sub));
        lblDiscount.setText("- " + moneyFormat.format(disc));
        lblTotalPay.setText(moneyFormat.format(finalAmountToPay));

        updateKpiMiniPanel();
    }

    private void updateKpiMiniPanel() {
        currentKpiEval.setActualValue(finalAmountToPay);
        currentKpiEval.setAchievedScore((finalAmountToPay >= 500000) ? 100 : (finalAmountToPay >= 100000) ? 50 : 0);

        lblKpiItems.setText("Tổng SP bán: " + totalItems);
        lblKpiTotal.setText("Doanh thu tạm: " + moneyFormat.format(finalAmountToPay));

        if (currentKpiEval.getAchievedScore() >= 100) {
            lblKpiStatus.setText("Đánh giá: TỐT");
            lblKpiStatus.setForeground(SUCCESS_GREEN);
        } else if (currentKpiEval.getAchievedScore() >= 50) {
            lblKpiStatus.setText("Đánh giá: ĐẠT");
            lblKpiStatus.setForeground(PRIMARY_BLUE);
        } else {
            lblKpiStatus.setText("Đánh giá: CHƯA ĐẠT");
            lblKpiStatus.setForeground(TEXT_GRAY);
        }
    }

    // =========================================================
    // CUSTOMER RULE & LOGIC
    // =========================================================
    private void findCustomer() {
        String p = txtCustomerPhone.getText().trim();
        if (p.isBlank()) {
            selectedCustomer = null;
            updateCustomerUI();
            return;
        }
        selectedCustomer = CustomersSql.getInstance().findByPhone(p);
        if (selectedCustomer != null) {
            updateCustomerUI();
            return;
        }

        if (JOptionPane.showConfirmDialog(this, "Không thấy KH! Đăng ký?", "Đăng ký", JOptionPane.YES_NO_OPTION) == 0) {
            JTextField txtName = new JTextField();
            if (JOptionPane.showConfirmDialog(this, new Object[]{"Tên:", txtName, "SĐT:", new JLabel(p)}, "Đăng ký", 2) == 0) {
                Customer c = new Customer();
                c.setCustomerId("CUS" + System.currentTimeMillis());
                c.setCustomerName(txtName.getText());
                c.setPhone(p);
                c.setMemberRank("Thường");
                c.setTotalSpending(0);

                if (CustomersSql.getInstance().insert(c) > 0) {
                    selectedCustomer = c;
                    updateCustomerUI();
                    notifySystemChanged();
                }
            }
        } else {
            selectedCustomer = null;
            updateCustomerUI();
        }
    }

    private void updateCustomerUI() {
        if (selectedCustomer == null) {
            lblCusName.setText("Khách vãng lai");
            lblCusRank.setText("Rank: Thường");
            lblCusRank.setForeground(TEXT_GRAY);
            lblCusTotalSpend.setText("Chi tiêu: 0 đ");
        } else {
            lblCusName.setText(selectedCustomer.getCustomerName());

            double spend = selectedCustomer.getTotalSpending();
            String rank = selectedCustomer.getMemberRank();
            double rate = selectedCustomer.getDiscountRate();

            if (rank == null) {
                rank = "Thường";
            }

            Color c = TEXT_GRAY;
            String rankDisplay = rank;

            if (rank.equalsIgnoreCase("Kim Cương")) {
                c = new Color(155, 89, 182);
                rankDisplay = "Kim Cương 💎";
            } else if (rank.equalsIgnoreCase("Vàng")) {
                c = WARNING_YELLOW;
                rankDisplay = "Vàng 🥇";
            } else if (rank.equalsIgnoreCase("Bạc")) {
                c = new Color(189, 195, 199);
                rankDisplay = "Bạc 🥈";
            } else if (rank.equalsIgnoreCase("Đồng")) {
                c = new Color(205, 127, 50);
                rankDisplay = "Đồng";
            }

            lblCusRank.setText("Rank: " + rankDisplay + " (" + (int) (rate * 100) + "%)");
            lblCusRank.setForeground(c);
            lblCusTotalSpend.setText("Chi tiêu: " + moneyFormat.format(spend));
        }
        calculateTotal();
    }

    // =========================================================
    // DB & PAYMENT LOGIC
    // =========================================================
    private void loadProducts() {
        new SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() {
                List<Product> prds = ProductsSql.getInstance().searchByName("");
                java.util.Map<String, Integer> stockMap = new java.util.HashMap<>();
                try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement("SELECT product_id, quantity FROM INVENTORY WHERE NVL(is_deleted, 0) = 0"); java.sql.ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        stockMap.put(rs.getString("product_id"), rs.getInt("quantity"));
                    }
                } catch (Exception e) {
                }
                for (Product p : prds) {
                    p.setQuantity(stockMap.getOrDefault(p.getProductId(), 0));
                }
                return prds;
            }

            @Override
            protected void done() {
                try {
                    allProducts = get();
                    modProducts.setRowCount(0);
                    for (Product p : allProducts) {
                        if (p.getQuantity() > 0) {
                            modProducts.addRow(new Object[]{p.getProductId(), p.getProductName(), moneyFormat.format(p.getBasePrice()), p.getQuantity()});
                        }
                    }
                } catch (Exception ex) {
                }
            }
        }.execute();
    }

    private int getStockFromDB(String pId) {
        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement("SELECT quantity FROM INVENTORY WHERE product_id = ?")) {
            ps.setString(1, pId);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (Exception e) {
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
                    for (PaymentMethod p : get()) {
                        cboPaymentMethod.addItem(p.getPaymentMethodId());
                    }
                } catch (Exception e) {
                }
            }
        }.execute();
    }

    private void processPayment() {
        if (modCart.getRowCount() <= 0) {
            return;
        }
        try {
            String emp = "EMP_DEFAULT";
            model.account.Account a = SessionManager.getCurrentUser();
            if (a != null) {
                emp = (a.getUserId() != null && !a.getUserId().isBlank()) ? a.getUserId() : a.getAccountId();
            }

            String pId = cboPaymentMethod.getSelectedItem() != null ? cboPaymentMethod.getSelectedItem().toString() : "PM_CASH";
            String oId = business.sql.sales_order.OrdersSql.getInstance().generateNextOrderId();

            Order o = new Order();
            o.setOrderId(oId);
            o.setCustomerId(selectedCustomer != null ? selectedCustomer.getCustomerId() : null);
            o.setEmployeeId(emp);
            o.setPaymentMethodId(pId);
            o.setOrderDate(new java.sql.Date(System.currentTimeMillis()));
            o.setTotalAmount(finalAmountToPay);
            o.setStatus("Hoàn thành");

            List<OrderDetail> dt = new ArrayList<>();
            for (int i = 0; i < modCart.getRowCount(); i++) {
                String id = modCart.getValueAt(i, 0).toString();
                Product p = allProducts.stream().filter(x -> x.getProductId().equals(id)).findFirst().orElse(null);
                dt.add(new OrderDetail(oId, id, Integer.parseInt(modCart.getValueAt(i, 2).toString()), Double.parseDouble(modCart.getValueAt(i, 3).toString()), (p != null && p.getBaseUnitId() != null) ? p.getBaseUnitId() : "U_CAI", 0));
            }

            if (PaymentService.processCheckoutSecure(o, dt)) {
                JOptionPane.showMessageDialog(this, "✅ Thanh toán thành công! Hóa đơn: " + oId);
                if (chkPrintBill.isSelected()) {
                    JOptionPane.showMessageDialog(this, "Đang gửi lệnh in hóa đơn...");
                }
                clearCart();
                if (selectedCustomer != null) {
                    selectedCustomer = CustomersSql.getInstance().findByPhone(selectedCustomer.getPhone());
                    updateCustomerUI();
                }
                loadProducts();
                notifySystemChanged();
            }
        } catch (common.exception.ConcurrentCheckoutException ex) {
            StringBuilder sb = new StringBuilder("Lỗi: Tồn kho thay đổi. Vui lòng giảm SL món sau:\n");
            for (int i = 0; i < modCart.getRowCount(); i++) {
                String id = modCart.getValueAt(i, 0).toString();
                if (ex.getFailedProducts().containsKey(id)) {
                    sb.append("- ").append(modCart.getValueAt(i, 1)).append(" (Yêu cầu: ").append(modCart.getValueAt(i, 2)).append(", Kho: ").append(ex.getFailedProducts().get(id)).append(")\n");
                }
            }
            refreshCartRealtime();
            JOptionPane.showMessageDialog(this, sb.toString(), "XUNG ĐỘT", 0);
        } catch (Exception ex) {
        }
    }

    private void notifySystemChanged() {
        try {
            String[] t = {"ORDERS", "INVENTORY", "PRODUCTS", "CUSTOMERS"};
            for (String x : t) {
                SyncVersionDao.bumpVersion(x);
                RealtimeClient.send(x + "_CHANGED");
                EventBus.publish(new AppDataChangedEvent(AppEventType.valueOf(x), ""));
            }
        } catch (Exception e) {
        }
    }

    // =========================================================
    // CUSTOM UI CLASSES (SỬA LỖI MẤT NGOẶC Ở ĐÂY)
    // =========================================================
    private JTable createTable(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(40);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.getTableHeader().setBackground(new Color(236, 240, 241));
        t.getTableHeader().setForeground(TEXT_DARK);
        t.setShowVerticalLines(false);
        t.setGridColor(BORDER_GRAY);
        t.setSelectionBackground(new Color(212, 230, 241));
        t.setSelectionForeground(TEXT_DARK);
        DefaultTableCellRenderer c = new DefaultTableCellRenderer();
        c.setHorizontalAlignment(JLabel.CENTER);
        t.setDefaultRenderer(Object.class, c);
        return t;
    }

    class ModernCardPanel extends JPanel {

        private int rad;

        public ModernCardPanel(int r) {
            this.rad = r;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_WHITE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), rad, rad);
            g2.setColor(BORDER_GRAY);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, rad, rad);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class ModernButton extends JButton {

        public ModernButton(String text, Color bg, Color fg) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setBackground(bg);
            setForeground(fg);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color color = getBackground();
            if (!isEnabled()) {
                color = new Color(180, 180, 180);
            } else if (getModel().isPressed()) {
                color = getBackground().darker();
            } else if (getModel().isRollover()) {
                color = getBackground().brighter();
            }

            // Shadow
            g2.setColor(new Color(0, 0, 0, 25));
            g2.fillRoundRect(3, 4, getWidth() - 6, getHeight() - 4, 16, 16);
            // Background
            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    class ToggleButton extends JCheckBox {

        public ToggleButton(String text) {
            super(text);
            setOpaque(false);
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setIcon(new ToggleIcon());
            setSelectedIcon(new ToggleIcon());
        }

        class ToggleIcon implements Icon {

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                AbstractButton b = (AbstractButton) c;
                int w = 36, h = 18;
                if (b.isSelected()) {
                    g2.setColor(SUCCESS_GREEN);
                    g2.fillRoundRect(x, y, w, h, h, h);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(x + w - h + 2, y + 2, h - 4, h - 4);
                } else {
                    g2.setColor(new Color(200, 200, 200));
                    g2.fillRoundRect(x, y, w, h, h, h);
                    g2.setColor(Color.WHITE);
                    g2.fillOval(x + 2, y + 2, h - 4, h - 4);
                }
                g2.dispose();
            }

            @Override
            public int getIconWidth() {
                return 40;
            }

            @Override
            public int getIconHeight() {
                return 18;
            }
        }
    }

    // 🌟 SPINNER EDITOR VÀ RENDERER
    class QuantitySpinnerEditor extends DefaultCellEditor {

        JSpinner s;

        public QuantitySpinnerEditor() {
            super(new JTextField());
            setClickCountToStart(1);
            s = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
            s.setBorder(null);
            ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setHorizontalAlignment(0);
            ((JSpinner.DefaultEditor) s.getEditor()).getTextField().setFont(new Font("Segoe UI", Font.BOLD, 14));
            s.addChangeListener(e -> stopCellEditing());
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) {
            this.s.setValue(Integer.parseInt(v.toString()));
            return this.s;
        }

        @Override
        public Object getCellEditorValue() {
            return s.getValue();
        }
    }

    class QuantitySpinnerRenderer extends JSpinner implements javax.swing.table.TableCellRenderer {

        public QuantitySpinnerRenderer() {
            super(new SpinnerNumberModel(1, 0, 9999, 1));
            setBorder(null);
            ((JSpinner.DefaultEditor) getEditor()).getTextField().setHorizontalAlignment(0);
            ((JSpinner.DefaultEditor) getEditor()).getTextField().setFont(new Font("Segoe UI", Font.BOLD, 14));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            if (v != null) {
                setValue(Integer.parseInt(v.toString()));
            }
            try {
                int q = Integer.parseInt(t.getValueAt(r, 2).toString());
                int st = Integer.parseInt(t.getValueAt(r, 5).toString());
                Color b = Color.WHITE;
                if (!s) {
                    if (q > st) {
                        b = new Color(253, 237, 236);
                    } else if (st <= 5) {
                        b = new Color(254, 249, 231);
                    }
                } else {
                    b = new Color(212, 230, 241);
                }
                setBackground(b);
                ((JSpinner.DefaultEditor) getEditor()).getTextField().setBackground(b);
            } catch (Exception e) {
            }
            return this;
        }
    }

    class CartTableRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            if ((c == 3 || c == 4) && v instanceof Number) {
                v = moneyFormat.format(v);
            }
            Component cp = super.getTableCellRendererComponent(t, v, s, f, r, c);
            if (c == 3 || c == 4) {
                setHorizontalAlignment(JLabel.RIGHT);
            } else if (c == 1) {
                setHorizontalAlignment(JLabel.LEFT);
            } else {
                setHorizontalAlignment(JLabel.CENTER);
            }

            try {
                int q = Integer.parseInt(t.getValueAt(r, 2).toString());
                int st = Integer.parseInt(t.getValueAt(r, 5).toString());
                if (c == 6) {
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                    if (q > st) {
                        setText("🔴 Vượt tồn");
                        setForeground(DANGER_RED);
                    } else if (st <= 5) {
                        setText("🟡 Sắp hết");
                        setForeground(WARNING_YELLOW);
                    } else {
                        setText("🟢 Hợp lệ");
                        setForeground(SUCCESS_GREEN);
                    }
                } else {
                    setForeground(TEXT_DARK);
                    setFont(new Font("Segoe UI", Font.PLAIN, 14));
                }

                if (!s) {
                    if (q > st) {
                        setBackground(new Color(253, 237, 236));
                    } else if (st <= 5) {
                        setBackground(new Color(254, 249, 231));
                    } else {
                        setBackground(Color.WHITE);
                    }
                } else {
                    setBackground(new Color(212, 230, 241));
                }
            } catch (Exception e) {
            }
            return cp;
        }
    }
}
