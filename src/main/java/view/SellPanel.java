package view;

import business.service.PaymentService;
import business.service.SessionManager;
import business.sql.prod_inventory.ProductsSql;
import business.sql.sales_order.CustomersSql;
import business.sql.sales_order.PaymentMethodsSql;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.exception.InventoryChangedException;
import view.components.IconHelper;

import model.order.Customer;
import model.order.Order;
import model.order.OrderDetail;
import model.payment.PaymentMethod;
import model.product.Product;
import model.account.kpi.KpiEvaluation;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SellPanel extends JPanel {

    // =========================================================
    // COLOR PALETTE & CONSTANTS
    // =========================================================
    private final Color BG_LIGHT = new Color(245, 246, 250);
    private final Color CARD_WHITE = Color.WHITE;
    private final Color PRIMARY_BLUE = new Color(41, 98, 255);
    private final Color SUCCESS_GREEN = new Color(39, 174, 96);
    private final Color DANGER_RED = new Color(231, 76, 60);
    private final Color WARNING_YELLOW = new Color(241, 196, 15);
    private final Color TEXT_DARK = new Color(44, 62, 80);
    private final Color TEXT_GRAY = new Color(127, 140, 141);
    private final Color BORDER_GRAY = new Color(223, 228, 234);

    private final DecimalFormat moneyFormat = new DecimalFormat("#,##0 đ");
    // Đổi Hint hiển thị đẹp hơn
    private static final String SEARCH_HINT = "🔍 Gõ mã hoặc tên SP vào đây để tìm nè...";
    private volatile boolean paymentProcessing = false;
    private volatile boolean paymentJustSucceeded = false;

    // =========================================================
    // UI COMPONENTS
    // =========================================================
    private JComboBox<String> cboSearchProduct;
    private DefaultComboBoxModel<String> searchComboModel;
    private JTextField searchEditor;
    private JSpinner spnQtyAdd;
    private RoundedButton btnAdd;
    private RoundedButton btnRefreshProducts;
    private ModernCardPanel pnlPayment;

    private JTable tblProducts;
    private DefaultTableModel modProducts;
    private JPanel pnlProductPreview;
    private JLabel lblPreviewImage;
    private JLabel lblPreviewName;
    private JLabel lblPreviewPrice;
    private JLabel lblPreviewStock;
    private JLabel lblPreviewCategory;

    private JTable tblCart;
    private DefaultTableModel modCart;
    private JPanel pnlWarning;
    private JLabel lblWarningMsg;

    private JTextField txtCustomerPhone;
    private RoundedButton btnFindCustomer;
    private JLabel lblCusName, lblCusRank, lblCusTotalSpend;

    private JComboBox<String> cboPaymentMethod;
    private JComboBox<String> cboKhuyenMai;
    private double discountPercentage = 0.0;
    private JLabel lblSubTotal, lblDiscount, lblTotalPay;
    private RoundedButton btnPay, btnCancel, btnRemove;

    private ToggleButton chkPrintBill;
    private JLabel lblEmployeeName, lblEmployeeRole;
    private JLabel lblProductCount, lblCartCount, lblCartEmptyHint;
    private JPanel pnlProductsBody;
    private JPanel pnlCartBody;
    private CardLayout productsCardLayout;
    private CardLayout cartCardLayout;
    private int hoverRow = -1;
    private boolean updatingSearchSuggestions = false;

    // =========================================================
    // DATA
    // =========================================================
    private List<Product> allProducts = new ArrayList<>();
    private Customer selectedCustomer;
    private double finalAmountToPay = 0;
    private double currentSubTotalAmount = 0.0;
    private double currentMemberDiscountAmount = 0.0;
    private double currentProgramDiscountAmount = 0.0;
    private final java.util.Map<String, PromoRule> promoRuleMap = new java.util.LinkedHashMap<>();
    private final java.util.Map<String, Double> currentLineProgramDiscountPercent = new java.util.HashMap<>();
    private final java.util.Map<String, Double> currentLineProgramDiscountAmount = new java.util.HashMap<>();

    private static class PromoRule {

        String promotionId;
        String promotionName;
        double discountPercent;
        double minOrderAmount;
        java.util.Set<String> productIds = new java.util.HashSet<>();

        String comboLabel() {
            return promotionId + " | " + promotionName
                    + " | " + new java.text.DecimalFormat("#,##0.##").format(discountPercent) + "%"
                    + " | Tối thiểu " + new java.text.DecimalFormat("#,##0").format(minOrderAmount) + "đ";
        }
    }

    private double lastPaidMemberDiscountAmount = 0.0;
    private double lastPaidProgramDiscountAmount = 0.0;
    private String productSearchKeyword = "";

    private KpiEvaluation currentKpiEval = new KpiEvaluation();
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
                    loadProducts();

                    // Nếu vừa thanh toán xong hoặc đang xử lý thanh toán thì không validate giỏ cũ nữa.
                    // Tránh hiện cảnh báo "vượt tồn" trong lúc DB đã trừ kho nhưng UI chưa clear cart.
                    if (!paymentProcessing && !paymentJustSucceeded && modCart.getRowCount() > 0) {
                        validateCartAgainstDatabase();
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

        JPanel dashboard = new JPanel(new GridBagLayout());
        dashboard.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridy = 0;
        gbc.weighty = 0.58;
        gbc.insets = new Insets(12, 0, 12, 0);
        dashboard.add(buildMainRow(), gbc);

        gbc.gridy = 1;
        gbc.weighty = 0.42;
        gbc.insets = new Insets(0, 0, 0, 0);
        dashboard.add(buildBottomRow(), gbc);

        add(dashboard, BorderLayout.CENTER);
    }

    private JPanel buildTopBar() {
        ModernCardPanel pnl = new ModernCardPanel(16);
        pnl.setLayout(new BorderLayout(18, 0));
        pnl.setPreferredSize(new Dimension(0, 78));
        pnl.setBorder(new EmptyBorder(10, 18, 10, 18));

        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);

        JLabel title = new JLabel("Bán hàng POS");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_DARK);

        JLabel subtitle = new JLabel("Tìm sản phẩm, quét hàng và thanh toán nhanh");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_GRAY);

        JPanel titleWrap = new JPanel();
        titleWrap.setOpaque(false);
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.add(title);
        titleWrap.add(Box.createVerticalStrut(2));
        titleWrap.add(subtitle);

        left.add(titleWrap, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);

        lblEmployeeName = new JLabel(getCurrentEmployeeDisplayName());
        lblEmployeeName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblEmployeeName.setForeground(TEXT_DARK);

        lblEmployeeRole = new JLabel(getCurrentEmployeeRoleText());
        lblEmployeeRole.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblEmployeeRole.setForeground(TEXT_GRAY);

        JPanel employeeText = new JPanel();
        employeeText.setOpaque(false);
        employeeText.setLayout(new BoxLayout(employeeText, BoxLayout.Y_AXIS));
        employeeText.add(lblEmployeeRole);
        employeeText.add(Box.createVerticalStrut(2));
        employeeText.add(lblEmployeeName);

        JLabel avatar = new JLabel("NV");
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setPreferredSize(new Dimension(42, 42));
        avatar.setForeground(Color.WHITE);
        avatar.setFont(new Font("Segoe UI", Font.BOLD, 13));
        avatar.setOpaque(true);
        avatar.setBackground(PRIMARY_BLUE);

        right.add(employeeText);
        right.add(avatar);

        pnl.add(left, BorderLayout.CENTER);
        pnl.add(right, BorderLayout.EAST);

        return pnl;
    }

    private JPanel buildMainRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.add(buildProductPanel());
        row.add(buildCartPanel());
        return row;
    }

    private JPanel buildBottomRow() {
        JPanel row = new JPanel(new GridLayout(1, 2, 14, 0));
        row.setOpaque(false);
        row.add(buildCustomerAndSettingsPanel());
        row.add(buildPaymentPanel());
        return row;
    }

    private JPanel buildCustomerAndSettingsPanel() {
        JPanel pnlCombine = new JPanel(new BorderLayout());
        pnlCombine.setOpaque(false);
        pnlCombine.add(buildCustomerCard(), BorderLayout.CENTER);
        return pnlCombine;
    }

    private JPanel buildProductPanel() {
        ModernCardPanel pnl = new ModernCardPanel(16);
        pnl.setLayout(new BorderLayout(0, 10));
        pnl.setBorder(new EmptyBorder(14, 14, 14, 14));
        addCardHoverEffect(pnl);

        JPanel header = createSectionHeader("Danh sách sản phẩm", "Stock xanh, cập nhật realtime", IconHelper.product(18));
        lblProductCount = new JLabel("0 sản phẩm khả dụng");
        lblProductCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblProductCount.setForeground(TEXT_GRAY);
        header.add(lblProductCount, BorderLayout.EAST);
        pnl.add(header, BorderLayout.NORTH);

        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);

        searchComboModel = new DefaultComboBoxModel<>();
        cboSearchProduct = new JComboBox<>(searchComboModel);
        cboSearchProduct.setEditable(true);
        cboSearchProduct.setMaximumRowCount(8);
        cboSearchProduct.setPreferredSize(new Dimension(250, 36));
        cboSearchProduct.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cboSearchProduct.setToolTipText("Ê, gõ mã hoặc tên sản phẩm vào ô này để tìm kiếm nha!");

        searchEditor = (JTextField) cboSearchProduct.getEditor().getEditorComponent();
        searchEditor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchEditor.putClientProperty("JTextField.placeholderText", SEARCH_HINT);
        searchEditor.setToolTipText("Ê, gõ mã hoặc tên sản phẩm vào ô này để tìm kiếm nha!");
        // Bỏ Icon kính lúp ở đây

        btnRefreshProducts = new RoundedButton("Làm mới");
        styleButton(btnRefreshProducts, new Color(127, 140, 141));
        btnRefreshProducts.setPreferredSize(new Dimension(110, 34));
        btnRefreshProducts.setIcon(IconHelper.refresh(14));

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightActions.setOpaque(false);
        // Bỏ Add nút Tìm ở đây
        rightActions.add(btnRefreshProducts);

        toolbar.add(cboSearchProduct, BorderLayout.CENTER);
        toolbar.add(rightActions, BorderLayout.EAST);

        pnl.add(toolbar, BorderLayout.NORTH);

        modProducts = new DefaultTableModel(new Object[]{"Mã SP", "Tên SP", "Giá bán", "Kho"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tblProducts = createTable(modProducts);
        tblProducts.getColumnModel().getColumn(1).setPreferredWidth(250);

        productsCardLayout = new CardLayout();
        pnlProductsBody = new JPanel(productsCardLayout);
        pnlProductsBody.setOpaque(false);
        pnlProductsBody.add(createLoadingPanel("Đang tải danh sách sản phẩm..."), "loading");
        pnlProductsBody.add(wrapTable(tblProducts), "table");
        productsCardLayout.show(pnlProductsBody, "loading");

        JPanel centerWrap = new JPanel(new BorderLayout(0, 10));
        centerWrap.setOpaque(false);
        centerWrap.add(pnlProductsBody, BorderLayout.CENTER);
        centerWrap.add(buildProductPreviewPanel(), BorderLayout.SOUTH);

        pnl.add(centerWrap, BorderLayout.CENTER);
        return pnl;
    }

    private void updateProductPreviewByProductId(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            clearProductPreview();
            return;
        }

        Product p = allProducts.stream()
                .filter(x -> x.getProductId() != null && x.getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (p == null) {
            clearProductPreview();
            return;
        }

        lblPreviewName.setText(p.getProductName() != null ? p.getProductName() : productId);
        lblPreviewPrice.setText("Giá bán: " + moneyFormat.format(p.getBasePrice()));
        lblPreviewStock.setText("Kho: " + p.getQuantity());
        lblPreviewCategory.setText("Loại: " + (p.getCategoryId() == null ? "—" : p.getCategoryId()));

        lblPreviewImage.setIcon(null);
        lblPreviewImage.setText("Ảnh");

        try {
            ImageIcon icon = loadProductImageIcon(p.getImagePath(), 88, 72);

            if (icon != null) {
                lblPreviewImage.setIcon(icon);
                lblPreviewImage.setText("");
            } else {
                lblPreviewImage.setText("Chưa có ảnh");
            }
        } catch (Exception e) {
            lblPreviewImage.setIcon(null);
            lblPreviewImage.setText("Không tải được ảnh");
        }
    }

    private void clearProductPreview() {
        if (lblPreviewImage != null) {
            lblPreviewImage.setIcon(null);
            lblPreviewImage.setText("Ảnh");
        }

        if (lblPreviewName != null) {
            lblPreviewName.setText("Chọn sản phẩm để xem ảnh");
        }

        if (lblPreviewPrice != null) {
            lblPreviewPrice.setText("Giá bán: —");
        }

        if (lblPreviewStock != null) {
            lblPreviewStock.setText("Kho: —");
        }

        if (lblPreviewCategory != null) {
            lblPreviewCategory.setText("Loại: —");
        }
    }

    private JPanel buildCartPanel() {
        ModernCardPanel pnl = new ModernCardPanel(16);
        pnl.setLayout(new BorderLayout(0, 10));
        pnl.setBorder(new EmptyBorder(14, 14, 14, 14));
        addCardHoverEffect(pnl);

        JPanel header = createSectionHeader("Giỏ hàng hiện tại", "Stepper số lượng và trạng thái", IconHelper.bill(18));
        lblCartCount = new JLabel("0 dòng");
        lblCartCount.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblCartCount.setForeground(TEXT_GRAY);
        header.add(lblCartCount, BorderLayout.EAST);

        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlActions.setOpaque(false);

        // Spinner và nút Thêm đã nằm ở preview sản phẩm bên trái.
// KHÔNG tạo lại spnQtyAdd ở đây, nếu không preview chỉnh 7 nhưng add vẫn lấy 1.
        if (spnQtyAdd == null) {
            spnQtyAdd = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        }

        btnAdd = new RoundedButton("Thêm");
        styleButton(btnAdd, PRIMARY_BLUE);
        btnAdd.setVisible(false);
        btnRemove = new RoundedButton("Xóa");
        styleButton(btnRemove, new Color(149, 165, 166));
        btnRemove.setPreferredSize(new Dimension(80, 34));

        btnCancel = new RoundedButton("Hủy");
        styleButton(btnCancel, DANGER_RED);
        btnCancel.setPreferredSize(new Dimension(80, 34));

        pnlActions.add(btnRemove);
        pnlActions.add(btnCancel);
        JPanel headerWrap = new JPanel(new BorderLayout(0, 8));
        headerWrap.setOpaque(false);
        headerWrap.add(header, BorderLayout.NORTH);
        headerWrap.add(pnlActions, BorderLayout.CENTER);

        pnlWarning = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pnlWarning.setBackground(new Color(253, 237, 236));
        lblWarningMsg = new JLabel("⚠ Có sản phẩm vượt tồn kho hoặc sắp hết hàng");
        lblWarningMsg.setForeground(DANGER_RED);
        lblWarningMsg.setFont(new Font("Segoe UI", Font.BOLD, 12));
        pnlWarning.add(lblWarningMsg);
        pnlWarning.setVisible(false);

        headerWrap.add(pnlWarning, BorderLayout.SOUTH);
        pnl.add(headerWrap, BorderLayout.NORTH);

        modCart = new DefaultTableModel(new Object[]{"Mã SP", "Sản phẩm", "Số lượng", "Đơn giá", "Thành tiền", "Tồn", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col == 2;
            }
        };
        tblCart = new JTable(modCart);
        tblCart.setRowHeight(52);
        tblCart.getColumnModel().getColumn(1).setPreferredWidth(220);
        tblCart.getColumnModel().getColumn(2).setPreferredWidth(130);
        tblCart.getColumnModel().getColumn(2).setMinWidth(110);
        tblCart.getColumnModel().getColumn(2).setMaxWidth(150);

        tblCart.setSurrendersFocusOnKeystroke(true);
        tblCart.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        tblCart.getColumnModel().getColumn(2).setCellRenderer(new QuantitySpinnerRenderer());
        tblCart.getColumnModel().getColumn(2).setCellEditor(new QuantitySpinnerEditor());

        CartTableRenderer cartRenderer = new CartTableRenderer();
        for (int i = 0; i < tblCart.getColumnCount(); i++) {
            if (i != 2) {
                tblCart.getColumnModel().getColumn(i).setCellRenderer(cartRenderer);
            }
        }

        cartCardLayout = new CardLayout();
        pnlCartBody = new JPanel(cartCardLayout);
        pnlCartBody.setOpaque(false);
        pnlCartBody.add(createEmptyCartPanel(), "empty");
        pnlCartBody.add(wrapTable(tblCart), "table");
        cartCardLayout.show(pnlCartBody, "empty");

        pnl.add(pnlCartBody, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(6, 4, 0, 4));
        lblCartEmptyHint = new JLabel("Tổng cộng: 0 đ");
        lblCartEmptyHint.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblCartEmptyHint.setForeground(TEXT_DARK);
        footer.add(lblCartEmptyHint, BorderLayout.WEST);
        pnl.add(footer, BorderLayout.SOUTH);
        return pnl;
    }

    private JPanel buildPaymentPanel() {
        pnlPayment = new ModernCardPanel(16);
        pnlPayment.setLayout(new GridBagLayout());
        pnlPayment.setBorder(new EmptyBorder(14, 18, 14, 18));
        addCardHoverEffect(pnlPayment);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 8, 6, 8);

        // Khởi tạo ComboBox Khuyến Mãi
        cboKhuyenMai = new JComboBox<>();
        cboKhuyenMai.setPreferredSize(new Dimension(220, 36));
        cboKhuyenMai.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loadActivePromotions();
        cboKhuyenMai.addActionListener(e -> {
            String selected = cboKhuyenMai.getSelectedItem() != null
                    ? cboKhuyenMai.getSelectedItem().toString()
                    : "";
            discountPercentage = getPromoRateFromString(selected);
            calculateTotal(); // Tính lại tiền ngay khi chọn
        });

        cboPaymentMethod = new JComboBox<>();
        cboPaymentMethod.setPreferredSize(new Dimension(220, 36));
        cboPaymentMethod.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        lblSubTotal = new JLabel("0 đ", SwingConstants.RIGHT);
        lblSubTotal.setFont(new Font("Segoe UI", Font.BOLD, 15));

        lblDiscount = new JLabel("0 đ", SwingConstants.RIGHT);
        lblDiscount.setForeground(SUCCESS_GREEN);
        lblDiscount.setFont(new Font("Segoe UI", Font.BOLD, 15));

        lblTotalPay = new JLabel("0 đ", SwingConstants.RIGHT);
        lblTotalPay.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTotalPay.setForeground(DANGER_RED);

        btnPay = new RoundedButton("THANH TOÁN");
        styleButton(btnPay, SUCCESS_GREEN);
        btnPay.setIcon(IconHelper.bill(20));
        btnPay.setHorizontalTextPosition(SwingConstants.RIGHT);
        btnPay.setIconTextGap(10);
        btnPay.setPreferredSize(new Dimension(0, 50));
        btnPay.setFont(new Font("Segoe UI", Font.BOLD, 18));
        btnPay.setEnabled(false);

        chkPrintBill = new ToggleButton("In hóa đơn");

        JPanel header = createSectionHeader("Thanh toán", "Tổng tiền nổi bật", IconHelper.bill(18));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 12, 0);
        pnlPayment.add(header, gbc);

        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridwidth = 1;

        // Thêm dòng Khuyến mãi
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.4;
        pnlPayment.add(new JLabel("Mã Voucher:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.6;
        pnlPayment.add(cboKhuyenMai, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        pnlPayment.add(new JLabel("Phương thức TT:"), gbc);
        gbc.gridx = 1;
        pnlPayment.add(cboPaymentMethod, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        pnlPayment.add(new JLabel("Tổng tạm tính:"), gbc);
        gbc.gridx = 1;
        pnlPayment.add(lblSubTotal, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        pnlPayment.add(new JLabel("Tổng giảm giá:"), gbc); // Đổi tên nhãn
        gbc.gridx = 1;
        pnlPayment.add(lblDiscount, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        pnlPayment.add(new JSeparator(), gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        JLabel lblT = new JLabel("TỔNG TIỀN:");
        lblT.setFont(new Font("Segoe UI", Font.BOLD, 18));
        pnlPayment.add(lblT, gbc);
        gbc.gridx = 1;
        pnlPayment.add(lblTotalPay, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(10, 8, 0, 8);

        JPanel actionRow = new JPanel(new BorderLayout(10, 0));
        actionRow.setOpaque(false);
        actionRow.add(chkPrintBill, BorderLayout.WEST);
        actionRow.add(btnPay, BorderLayout.CENTER);
        pnlPayment.add(actionRow, gbc);

        return pnlPayment;
    }

    private JPanel buildCustomerCard() {
        ModernCardPanel card = new ModernCardPanel(16);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        addCardHoverEffect(card);

        card.add(createSectionHeader("Khách hàng", "Tra bằng SĐT", IconHelper.customer(18)), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(12, 0));
        body.setOpaque(false);

        CircleAvatarPanel avatar = new CircleAvatarPanel(52, new Color(230, 240, 255));
        avatar.setLayout(new GridBagLayout());
        avatar.add(new JLabel(IconHelper.customer(24)));

        lblCusName = new JLabel("Khách vãng lai");
        lblCusName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblCusName.setForeground(TEXT_DARK);

        lblCusRank = new JLabel("Rank: Thường");
        lblCusRank.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCusRank.setForeground(TEXT_GRAY);

        lblCusTotalSpend = new JLabel("Chi tiêu: 0 đ");
        lblCusTotalSpend.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblCusTotalSpend.setForeground(TEXT_GRAY);

        txtCustomerPhone = new JTextField();
        txtCustomerPhone.setPreferredSize(new Dimension(160, 34));
        txtCustomerPhone.putClientProperty("JTextField.placeholderText", "Nhập SĐT khách hàng");
        // Bỏ Icon Kính lúp ở đây

        btnFindCustomer = new RoundedButton("Tìm");
        styleButton(btnFindCustomer, PRIMARY_BLUE);
        btnFindCustomer.setPreferredSize(new Dimension(75, 34));

        JPanel infoWrap = new JPanel();
        infoWrap.setOpaque(false);
        infoWrap.setLayout(new BoxLayout(infoWrap, BoxLayout.Y_AXIS));
        infoWrap.add(lblCusName);
        infoWrap.add(Box.createVerticalStrut(4));
        infoWrap.add(lblCusRank);
        infoWrap.add(Box.createVerticalStrut(4));
        infoWrap.add(lblCusTotalSpend);

        JPanel topRow = new JPanel(new BorderLayout(10, 0));
        topRow.setOpaque(false);
        topRow.add(avatar, BorderLayout.WEST);
        topRow.add(infoWrap, BorderLayout.CENTER);

        // GridBagLayout ép thẳng nút
        JPanel phoneRow = new JPanel(new GridBagLayout());
        phoneRow.setOpaque(false);

        GridBagConstraints gbcPhone = new GridBagConstraints();
        gbcPhone.insets = new Insets(0, 4, 0, 4);
        gbcPhone.anchor = GridBagConstraints.CENTER;

        JLabel phoneLabel = new JLabel("SĐT:");
        phoneLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        gbcPhone.gridx = 0;
        phoneRow.add(phoneLabel, gbcPhone);
        gbcPhone.gridx = 1;
        phoneRow.add(txtCustomerPhone, gbcPhone);
        gbcPhone.gridx = 2;
        phoneRow.add(btnFindCustomer, gbcPhone);

        body.add(topRow, BorderLayout.CENTER);
        body.add(phoneRow, BorderLayout.EAST);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private void styleButton(RoundedButton btn, Color bg) {
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
    }

    private void addCardHoverEffect(ModernCardPanel pnl) {
        pnl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                pnl.setBackground(new Color(245, 245, 247));
                pnl.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                pnl.setBackground(CARD_WHITE);
                pnl.repaint();
            }
        });
    }

    private ImageIcon loadProductImageIcon(String imageNameOrPath, int width, int height) {
        if (imageNameOrPath == null || imageNameOrPath.trim().isEmpty()) {
            return null;
        }

        String path = imageNameOrPath.trim().replace("\\", "/");

        try {
            java.net.URL url = getClass().getClassLoader().getResource("view/image/" + path);

            if (url != null) {
                Image img = new ImageIcon(url)
                        .getImage()
                        .getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }

            java.io.File file = new java.io.File(path);

            if (!file.exists()) {
                file = new java.io.File("src/main/resources/view/image/" + path);
            }

            if (file.exists()) {
                Image img = new ImageIcon(file.getAbsolutePath())
                        .getImage()
                        .getScaledInstance(width, height, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private JPanel buildProductPreviewPanel() {
        pnlProductPreview = new JPanel(new BorderLayout(12, 0));
        pnlProductPreview.setOpaque(true);
        pnlProductPreview.setBackground(new Color(248, 250, 252));
        pnlProductPreview.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_GRAY),
                new EmptyBorder(10, 12, 10, 12)
        ));
        pnlProductPreview.setPreferredSize(new Dimension(0, 112));

        lblPreviewImage = new JLabel("Ảnh", SwingConstants.CENTER);
        lblPreviewImage.setPreferredSize(new Dimension(105, 88));
        lblPreviewImage.setOpaque(true);
        lblPreviewImage.setBackground(Color.WHITE);
        lblPreviewImage.setForeground(TEXT_GRAY);
        lblPreviewImage.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblPreviewImage.setBorder(BorderFactory.createLineBorder(new Color(225, 230, 235)));

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        lblPreviewName = new JLabel("Chọn sản phẩm để xem ảnh");
        lblPreviewName.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblPreviewName.setForeground(TEXT_DARK);

        lblPreviewPrice = new JLabel("Giá bán: —");
        lblPreviewPrice.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblPreviewPrice.setForeground(PRIMARY_BLUE);

        lblPreviewStock = new JLabel("Kho: —");
        lblPreviewStock.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPreviewStock.setForeground(TEXT_GRAY);

        lblPreviewCategory = new JLabel("Loại: —");
        lblPreviewCategory.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblPreviewCategory.setForeground(TEXT_GRAY);

        info.add(lblPreviewName);
        info.add(Box.createVerticalStrut(6));
        info.add(lblPreviewPrice);
        info.add(Box.createVerticalStrut(3));
        info.add(lblPreviewStock);
        info.add(Box.createVerticalStrut(3));
        info.add(lblPreviewCategory);

        JPanel actionPanel = new JPanel(new GridBagLayout());
        actionPanel.setOpaque(false);
        actionPanel.setPreferredSize(new Dimension(210, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblQty = new JLabel("SL:");
        lblQty.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblQty.setForeground(TEXT_DARK);

        // Dùng lại spinner global để addSelectedProductToCart() vẫn lấy đúng số lượng
        if (spnQtyAdd == null) {
            spnQtyAdd = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        }
        spnQtyAdd.setPreferredSize(new Dimension(70, 34));

        RoundedButton btnAddPreview = new RoundedButton("Thêm vào giỏ");
        styleButton(btnAddPreview, PRIMARY_BLUE);
        btnAddPreview.setPreferredSize(new Dimension(130, 36));
        btnAddPreview.addActionListener(e -> {
            try {
                spnQtyAdd.commitEdit();
            } catch (Exception ignored) {
            }
            addSelectedProductToCart();
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        actionPanel.add(lblQty, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        actionPanel.add(spnQtyAdd, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        actionPanel.add(btnAddPreview, gbc);

        pnlProductPreview.add(lblPreviewImage, BorderLayout.WEST);
        pnlProductPreview.add(info, BorderLayout.CENTER);
        pnlProductPreview.add(actionPanel, BorderLayout.EAST);

        return pnlProductPreview;
    }

    private JPanel createSectionHeader(String title, String subtitle, ImageIcon icon) {
        JPanel header = new JPanel(new BorderLayout(10, 4));
        header.setOpaque(false);
        JPanel left = new JPanel(new BorderLayout(10, 0));
        left.setOpaque(false);
        JLabel iconLabel = new JLabel();
        if (icon != null) {
            iconLabel.setIcon(icon);
        }
        left.add(iconLabel, BorderLayout.WEST);

        JPanel textWrap = new JPanel();
        textWrap.setOpaque(false);
        textWrap.setLayout(new BoxLayout(textWrap, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(TEXT_DARK);
        JLabel lblSub = new JLabel(subtitle);
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSub.setForeground(TEXT_GRAY);

        textWrap.add(lblTitle);
        textWrap.add(Box.createVerticalStrut(2));
        textWrap.add(lblSub);
        left.add(textWrap, BorderLayout.CENTER);
        header.add(left, BorderLayout.WEST);
        return header;
    }

    private JPanel createLoadingPanel(String message) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 8, 0);
        JLabel icon = new JLabel("");
        icon.setFont(new Font("Segoe UI", Font.PLAIN, 26));
        panel.add(icon, gbc);
        gbc.gridy = 1;
        JLabel lbl = new JLabel(message);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(TEXT_GRAY);
        panel.add(lbl, gbc);
        gbc.gridy = 2;
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setPreferredSize(new Dimension(180, 8));
        panel.add(bar, gbc);
        return panel;
    }

    private JPanel createEmptyCartPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel icon = new JLabel();
        icon.setIcon(IconHelper.getIcon("shopping-cart.png", 48, 48));
        if (icon.getIcon() == null) {
            icon.setText("🛒");
        }
        panel.add(icon, gbc);
        gbc.gridy = 1;
        JLabel title = new JLabel("Giỏ hàng đang trống");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(title, gbc);
        gbc.gridy = 2;
        JLabel sub = new JLabel("Chọn sản phẩm để bắt đầu thanh toán");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_GRAY);
        panel.add(sub, gbc);
        return panel;
    }

    private JScrollPane wrapTable(JTable table) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(BORDER_GRAY));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private String getCurrentEmployeeDisplayName() {
        model.account.Account user = SessionManager.getCurrentUser();
        if (user == null) {
            return "Nhân viên bán hàng";
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getAccountId() != null ? user.getAccountId() : "Nhân viên bán hàng";
    }

    private String getCurrentEmployeeRoleText() {
        model.account.Account user = SessionManager.getCurrentUser();
        if (user == null || user.getRoleId() == null || user.getRoleId().isBlank()) {
            return "Đang đăng nhập";
        }
        return "Nhân viên - " + user.getRoleId();
    }

    // =========================================================
    // LOGIC & EVENTS
    // =========================================================
    private void initEvents() {
        if (searchEditor != null) {
            searchEditor.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    handleSearchChanged();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    handleSearchChanged();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    handleSearchChanged();
                }
            });
            searchEditor.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        addSelectedProductToCart();
                    }
                }
            });
        }

        btnRefreshProducts.addActionListener(e -> resetProductSearch());

        cboSearchProduct.addActionListener(e -> {
            if (!updatingSearchSuggestions && cboSearchProduct.getSelectedItem() != null) {
                String selected = cboSearchProduct.getSelectedItem().toString();
                updatingSearchSuggestions = true;
                if (searchEditor != null) {
                    searchEditor.setText(selected);
                }
                updatingSearchSuggestions = false;
                filterProductsBySearch(selected);
            }
        });

        tblProducts.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tblProducts.getSelectedRow() >= 0) {
                    int viewRow = tblProducts.getSelectedRow();
                    int modelRow = tblProducts.convertRowIndexToModel(viewRow);
                    addToCart(modProducts.getValueAt(modelRow, 0).toString(), getQtyToAdd());
                    spnQtyAdd.setValue(1);
                }
            }
        });

        tblProducts.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }

            int viewRow = tblProducts.getSelectedRow();

            if (viewRow < 0) {
                clearProductPreview();
                return;
            }

            int modelRow = tblProducts.convertRowIndexToModel(viewRow);

            if (modelRow < 0 || modelRow >= modProducts.getRowCount()) {
                clearProductPreview();
                return;
            }

            String productId = String.valueOf(modProducts.getValueAt(modelRow, 0));
            updateProductPreviewByProductId(productId);
        });

        if (btnAdd != null) {
            if (btnAdd != null) {
                btnAdd.addActionListener(e -> {
                    try {
                        if (spnQtyAdd != null) {
                            spnQtyAdd.commitEdit();
                        }
                    } catch (Exception ignored) {
                    }

                    addSelectedProductToCart();
                });
            }
        }
        btnRemove.addActionListener(e -> {
            int[] selectedRows = tblCart.getSelectedRows();
            if (selectedRows.length == 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn sản phẩm trong giỏ để xóa!");
                return;
            }
            int[] modelRows = Arrays.stream(selectedRows)
                    .map(tblCart::convertRowIndexToModel)
                    .sorted()
                    .toArray();
            isUpdatingCart = true;
            for (int i = modelRows.length - 1; i >= 0; i--) {
                if (modelRows[i] >= 0 && modelRows[i] < modCart.getRowCount()) {
                    modCart.removeRow(modelRows[i]);
                }
            }
            isUpdatingCart = false;
            calculateTotal();
            validateCartAgainstDatabase();
        });

        btnCancel.addActionListener(e -> clearCart());
        txtCustomerPhone.addActionListener(e -> btnFindCustomer.doClick());
        btnFindCustomer.addActionListener(e -> findCustomer());
        btnPay.addActionListener(e -> processPaymentAction());

        modCart.addTableModelListener(e -> {
            if (isUpdatingCart) {
                return;
            }
            if (e.getColumn() != 2 || e.getType() != javax.swing.event.TableModelEvent.UPDATE) {
                return;
            }
            final int r = e.getFirstRow();

            SwingUtilities.invokeLater(() -> {
                if (r < 0 || r >= modCart.getRowCount()) {
                    return;
                }
                isUpdatingCart = true;
                try {
                    Object qtyObj = modCart.getValueAt(r, 2);
                    if (qtyObj == null) {
                        return;
                    }
                    int qty = Integer.parseInt(qtyObj.toString());
                    if (qty <= 0) {
                        if (r >= 0 && r < modCart.getRowCount()) {
                            modCart.removeRow(r);
                        }
                    } else {
                        double price = Double.parseDouble(modCart.getValueAt(r, 3).toString());
                        modCart.setValueAt(price * qty, r, 4);
                    }
                    calculateTotal();
                    validateCartAgainstDatabase();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    isUpdatingCart = false;
                }
            });
        });

        // 🚀 KÍCH HOẠT DRAG & DROP
        setupDragAndDrop();
    }

    private void setupDragAndDrop() {
        tblProducts.setDragEnabled(true);
        tblProducts.setTransferHandler(new TransferHandler("String") {
            @Override
            protected Transferable createTransferable(JComponent c) {
                JTable table = (JTable) c;
                int[] selectedRows = table.getSelectedRows();
                if (selectedRows.length > 0) {
                    StringBuilder productIds = new StringBuilder();
                    for (int i = 0; i < selectedRows.length; i++) {
                        int modelRow = table.convertRowIndexToModel(selectedRows[i]);
                        String pId = ((DefaultTableModel) table.getModel()).getValueAt(modelRow, 0).toString();
                        productIds.append(pId);
                        if (i < selectedRows.length - 1) {
                            productIds.append(",");
                        }
                    }
                    return new StringSelection(productIds.toString());
                }
                return null;
            }

            @Override
            public int getSourceActions(JComponent c) {
                return COPY;
            }
        });

        TransferHandler dropHandler = new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.stringFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    String transferData = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
                    String[] productIds = transferData.split(",");
                    SwingUtilities.invokeLater(() -> {
                        for (String pId : productIds) {
                            addToCart(pId.trim(), getQtyToAdd());
                        }
                        spnQtyAdd.setValue(1);
                    });
                    return true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        };

        tblCart.setDropMode(DropMode.INSERT_ROWS);
        tblCart.setTransferHandler(dropHandler);
        pnlCartBody.setTransferHandler(dropHandler);
    }

    private void addSelectedProductToCart() {
        int qty = getQtyToAdd();

        int[] selectedRows = tblProducts.getSelectedRows();

        if (selectedRows.length > 0) {
            for (int i = 0; i < selectedRows.length; i++) {
                int modelRow = tblProducts.convertRowIndexToModel(selectedRows[i]);

                if (modelRow < 0 || modelRow >= modProducts.getRowCount()) {
                    continue;
                }

                String pId = modProducts.getValueAt(modelRow, 0).toString();
                addToCart(pId, qty);
            }

            spnQtyAdd.setValue(1);
            tblProducts.clearSelection();
            clearProductPreview();
            return;
        }

        String kw = getSearchText();

        if (!kw.isBlank() && !kw.equals(SEARCH_HINT)) {
            Product p = allProducts.stream()
                    .filter(x -> x.getProductId().equalsIgnoreCase(kw)
                    || formatProductSearchLabel(x).equalsIgnoreCase(kw))
                    .findFirst()
                    .orElse(null);

            if (p != null) {
                addToCart(p.getProductId(), qty);
                spnQtyAdd.setValue(1);
                clearProductPreview();
                return;
            }
        }

        JOptionPane.showMessageDialog(
                this,
                "Vui lòng chọn Sản phẩm từ danh sách bên trái trước!"
        );
    }

    private void addToCart(String pId, int qty) {
        Product p = allProducts.stream().filter(x -> x.getProductId().equals(pId)).findFirst().orElse(null);
        if (p == null || p.getQuantity() <= 0) {
            return;
        }

        double price = p.getBasePrice().doubleValue();

        for (int i = 0; i < modCart.getRowCount(); i++) {
            if (modCart.getValueAt(i, 0).equals(pId)) {
                int currentQty = Integer.parseInt(modCart.getValueAt(i, 2).toString());
                int newQty = currentQty + qty;

                if (newQty > p.getQuantity()) {
                    JOptionPane.showMessageDialog(this, "Số lượng vượt tồn kho!\nTồn hiện tại: " + p.getQuantity(), "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                modCart.setValueAt(newQty, i, 2);
                return;
            }
        }

        isUpdatingCart = true;
        modCart.addRow(new Object[]{pId, p.getProductName(), qty, price, price * qty, p.getQuantity(), ""});
        isUpdatingCart = false;

        calculateTotal();
        validateCartAgainstDatabase();
    }

    private void handleSearchChanged() {
        if (updatingSearchSuggestions) {
            return;
        }
        String keyword = getSearchText();
        productSearchKeyword = keyword;
        applyProductFilter(keyword);
        refreshSearchSuggestions(keyword);
    }

    private void filterProductsBySearch(String keyword) {
        productSearchKeyword = keyword;
        applyProductFilter(keyword);
    }

    private void resetProductSearch() {
        productSearchKeyword = "";
        clearSearchText();
        refreshSearchSuggestions("");
        applyProductFilter("");
        if (tblProducts != null) {
            tblProducts.clearSelection();
        }
    }

    private String getSearchText() {
        return searchEditor != null ? searchEditor.getText().trim() : "";
    }

    private void clearSearchText() {
        updatingSearchSuggestions = true;
        if (searchEditor != null) {
            searchEditor.setText("");
        }
        if (cboSearchProduct != null) {
            cboSearchProduct.setSelectedItem(null);
        }
        updatingSearchSuggestions = false;
    }

    private void refreshSearchSuggestions(String keyword) {
        if (searchComboModel == null || cboSearchProduct == null) {
            return;
        }
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();

        updatingSearchSuggestions = true;
        try {
            searchComboModel.removeAllElements();
            int matches = 0;
            for (Product p : allProducts) {
                if (p.getQuantity() <= 0) {
                    continue;
                }
                String label = formatProductSearchLabel(p);
                if (normalized.isBlank() || label.toLowerCase().contains(normalized)) {
                    searchComboModel.addElement(label);
                    matches++;
                }
            }
            if (searchEditor != null) {
                searchEditor.setText(keyword == null ? "" : keyword);
            }
            if (!normalized.isBlank() && matches > 0) {
                cboSearchProduct.showPopup();
            } else {
                cboSearchProduct.hidePopup();
            }
        } finally {
            updatingSearchSuggestions = false;
        }
    }

    private String formatProductSearchLabel(Product p) {
        return p.getProductId() + " - " + p.getProductName();
    }

    private void applyProductFilter(String keyword) {
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        modProducts.setRowCount(0);

        for (Product p : allProducts) {
            if (p.getQuantity() <= 0) {
                continue;
            }
            String haystack = (p.getProductId() + " " + p.getProductName()).toLowerCase();
            if (normalized.isBlank() || haystack.contains(normalized)) {
                modProducts.addRow(new Object[]{p.getProductId(), p.getProductName(), moneyFormat.format(p.getBasePrice()), p.getQuantity()});
            }
        }

        if (lblProductCount != null) {
            lblProductCount.setText(normalized.isBlank() ? allProducts.size() + " sản phẩm khả dụng" : modProducts.getRowCount() + " kết quả phù hợp");
        }
        if (productsCardLayout != null && pnlProductsBody != null) {
            productsCardLayout.show(pnlProductsBody, "table");
        }
    }

    private int getQtyToAdd() {
        int qty = 1;

        try {
            if (spnQtyAdd != null) {
                spnQtyAdd.commitEdit();
                Object value = spnQtyAdd.getValue();

                if (value instanceof Number number) {
                    qty = number.intValue();
                } else {
                    qty = Integer.parseInt(String.valueOf(value));
                }
            }
        } catch (Exception ignored) {
            qty = 1;
        }

        if (qty <= 0) {
            qty = 1;
        }

        return qty;
    }

    private void validateCartAgainstDatabase() {
        if (paymentProcessing || paymentJustSucceeded) {
            return;
        }
        boolean hasError = false;

        isUpdatingCart = true;
        for (int i = 0; i < modCart.getRowCount(); i++) {
            int q = Integer.parseInt(modCart.getValueAt(i, 2).toString());
            int stock = getStockFromDB(modCart.getValueAt(i, 0).toString());
            modCart.setValueAt(stock, i, 5);
            if (q > stock) {
                hasError = true;
            }
        }
        isUpdatingCart = false;

        if (hasError) {
            pnlWarning.setBackground(new Color(253, 237, 236));
            lblWarningMsg.setForeground(DANGER_RED);
            lblWarningMsg.setText("Lỗi: Có sản phẩm vượt tồn kho hoặc đã bị thay đổi!");
            pnlWarning.setVisible(true);
            btnPay.setEnabled(false);
        } else {
            pnlWarning.setVisible(false);
            btnPay.setEnabled(modCart.getRowCount() > 0);
        }

        if (lblCartCount != null) {
            lblCartCount.setText(modCart.getRowCount() + " dòng");
        }
        if (cartCardLayout != null && pnlCartBody != null) {
            cartCardLayout.show(pnlCartBody, modCart.getRowCount() > 0 ? "table" : "empty");
        }
        updateKpiMiniPanel();
        tblCart.repaint();
    }

    private void clearCart() {
        isUpdatingCart = true;
        modCart.setRowCount(0);
        isUpdatingCart = false;
        calculateTotal();
        validateCartAgainstDatabase();
    }

    private double getSelectedCustomerDiscountRate() {
        if (selectedCustomer == null) {
            return 0.0;
        }

        /*
         * BUG CŨ:
         * Ở đây từng gọi lại chính getSelectedCustomerDiscountRate(), gây đệ quy sai.
         * Kết quả là giảm giá thành viên không được tính ổn định, report nhận MEMBER_DISCOUNT_AMOUNT = 0.
         *
         * FIX:
         * 1) Ưu tiên rate từ Customer.getDiscountRate().
         * 2) Nếu model/DB chưa đủ dữ liệu thì fallback theo rank.
         * 3) Nếu rank trống thì fallback theo totalSpending.
         */
        try {
            double rate = selectedCustomer.getDiscountRate();
            if (rate > 0) {
                return rate;
            }
        } catch (Exception ignored) {
        }

        String rank = null;
        try {
            rank = selectedCustomer.getMemberRank();
        } catch (Exception ignored) {
        }

        if (rank != null && !rank.trim().isEmpty()) {
            String normalizedRank = normalizeVietnamese(rank).toLowerCase().trim();

            if (normalizedRank.equals("kim cuong") || normalizedRank.equals("kim cương")) {
                return 0.12;
            }
            if (normalizedRank.equals("vang") || normalizedRank.equals("vàng")) {
                return 0.08;
            }
            if (normalizedRank.equals("bac") || normalizedRank.equals("bạc")) {
                return 0.05;
            }
            if (normalizedRank.equals("dong") || normalizedRank.equals("đồng")) {
                return 0.02;
            }
        }

        try {
            double spend = selectedCustomer.getTotalSpending();

            if (spend >= 80_000_000) {
                return 0.12;
            }
            if (spend >= 40_000_000) {
                return 0.08;
            }
            if (spend >= 15_000_000) {
                return 0.05;
            }
            if (spend >= 5_000_000) {
                return 0.02;
            }
        } catch (Exception ignored) {
        }

        return 0.0;
    }

    private String normalizeVietnamese(String input) {
        if (input == null) {
            return "";
        }
        String s = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD);
        s = s.replaceAll("\\p{M}", "");
        s = s.replace('đ', 'd').replace('Đ', 'D');
        return s;
    }

    private void calculateTotal() {
        if (lblSubTotal == null || lblTotalPay == null) {
            return;
        }

        double subTotal = 0.0;
        double memberDiscount = 0.0;
        double programDiscount = 0.0;

        currentLineProgramDiscountPercent.clear();
        currentLineProgramDiscountAmount.clear();

        // 1. Thành tiền gốc
        for (int i = 0; i < modCart.getRowCount(); i++) {
            double lineTotal = parseMoneyObject(modCart.getValueAt(i, 4));
            subTotal += lineTotal;

            String productId = String.valueOf(modCart.getValueAt(i, 0));
            currentLineProgramDiscountPercent.put(productId, 0.0);
            currentLineProgramDiscountAmount.put(productId, 0.0);
        }

        // 2. Giảm giá thành viên: giảm cấp hóa đơn, không đưa xuống từng dòng sản phẩm.
        if (selectedCustomer != null && subTotal > 0) {
            double memberRate = getSelectedCustomerDiscountRate();

            if (memberRate > 0) {
                memberDiscount = subTotal * memberRate;
            }
        }

        // 3. Giảm giá chương trình: chỉ áp dụng cho product_id nằm trong PROMOTION_PRODUCTS.
        PromoRule selectedRule = getSelectedPromoRule();

        if (selectedRule != null && selectedRule.discountPercent > 0 && modCart.getRowCount() > 0) {
            double eligibleSubTotal = 0.0;

            for (int i = 0; i < modCart.getRowCount(); i++) {
                String productId = String.valueOf(modCart.getValueAt(i, 0));
                double lineTotal = parseMoneyObject(modCart.getValueAt(i, 4));

                if (selectedRule.productIds != null && selectedRule.productIds.contains(productId)) {
                    eligibleSubTotal += lineTotal;
                }
            }

            if (eligibleSubTotal >= selectedRule.minOrderAmount) {
                for (int i = 0; i < modCart.getRowCount(); i++) {
                    String productId = String.valueOf(modCart.getValueAt(i, 0));
                    double lineTotal = parseMoneyObject(modCart.getValueAt(i, 4));

                    if (selectedRule.productIds != null && selectedRule.productIds.contains(productId)) {
                        double lineDiscount = roundMoney(lineTotal * selectedRule.discountPercent / 100.0);

                        currentLineProgramDiscountPercent.put(productId, selectedRule.discountPercent);
                        currentLineProgramDiscountAmount.put(productId, lineDiscount);

                        programDiscount += lineDiscount;
                    } else {
                        currentLineProgramDiscountPercent.put(productId, 0.0);
                        currentLineProgramDiscountAmount.put(productId, 0.0);
                    }
                }
            } else {
                currentLineProgramDiscountPercent.clear();
                currentLineProgramDiscountAmount.clear();

                for (int i = 0; i < modCart.getRowCount(); i++) {
                    String productId = String.valueOf(modCart.getValueAt(i, 0));
                    currentLineProgramDiscountPercent.put(productId, 0.0);
                    currentLineProgramDiscountAmount.put(productId, 0.0);
                }

                final double finalEligibleSubTotal = eligibleSubTotal;
                final double finalMinOrderAmount = selectedRule.minOrderAmount;

                JOptionPane.showMessageDialog(
                        this,
                        "Mã khuyến mãi cần tổng tiền nhóm sản phẩm áp dụng tối thiểu "
                        + moneyFormat.format(finalMinOrderAmount)
                        + ".\nHiện tại nhóm sản phẩm áp dụng chỉ có "
                        + moneyFormat.format(finalEligibleSubTotal)
                        + ".",
                        "Chưa đủ điều kiện khuyến mãi",
                        JOptionPane.WARNING_MESSAGE
                );

                if (cboKhuyenMai != null && cboKhuyenMai.getSelectedIndex() > 0) {
                    cboKhuyenMai.setSelectedIndex(0);
                }

                programDiscount = 0.0;
            }
        }

        // 4. Chặn tổng giảm vượt quá thành tiền.
        double totalDiscount = memberDiscount + programDiscount;

        if (totalDiscount > subTotal) {
            double overflow = totalDiscount - subTotal;

            if (programDiscount >= overflow) {
                programDiscount -= overflow;
            } else {
                overflow -= programDiscount;
                programDiscount = 0.0;
                memberDiscount = Math.max(0.0, memberDiscount - overflow);
            }

            totalDiscount = memberDiscount + programDiscount;
        }

        currentSubTotalAmount = roundMoney(subTotal);
        currentMemberDiscountAmount = roundMoney(memberDiscount);
        currentProgramDiscountAmount = roundMoney(programDiscount);
        finalAmountToPay = roundMoney(subTotal - totalDiscount);

        lblSubTotal.setText(moneyFormat.format(currentSubTotalAmount));

        if (lblDiscount != null) {
            lblDiscount.setText(moneyFormat.format(currentMemberDiscountAmount + currentProgramDiscountAmount));
        }

        lblTotalPay.setText(moneyFormat.format(finalAmountToPay));

        if (lblCartEmptyHint != null) {
            lblCartEmptyHint.setText("Tổng cộng: " + moneyFormat.format(finalAmountToPay));
        }

        if (btnPay != null) {
            btnPay.setEnabled(modCart.getRowCount() > 0);
        }
    }

    private double parseMoneyObject(Object value) {
        if (value == null) {
            return 0.0;
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        try {
            String s = value.toString().trim();
            if (s.isEmpty()) {
                return 0.0;
            }

            // Các ô tiền trong model thường là Number. Nhánh này xử lý thêm nếu lỡ là String.
            // Ví dụ:
            // - "180.000 đ"  -> 180000
            // - "180,000 đ"  -> 180000
            // - "180000.0"   -> 180000.0
            // Không được xóa dấu "." một cách mù quáng vì sẽ biến "180000.0" thành "1800000".
            s = s.replace("đ", "")
                    .replace("VND", "")
                    .trim();

            boolean hasComma = s.contains(",");
            boolean hasDot = s.contains(".");

            if (hasComma && hasDot) {
                // Dạng có cả dấu phẩy và dấu chấm: ưu tiên xem dấu cuối là dấu thập phân.
                int lastComma = s.lastIndexOf(',');
                int lastDot = s.lastIndexOf('.');

                if (lastDot > lastComma) {
                    // 1,234,567.89
                    s = s.replace(",", "");
                } else {
                    // 1.234.567,89
                    s = s.replace(".", "").replace(",", ".");
                }
            } else if (hasComma) {
                // Nếu sau dấu phẩy là 3 số thì coi là phân cách hàng nghìn.
                int lastComma = s.lastIndexOf(',');
                int digitsAfter = s.length() - lastComma - 1;
                if (digitsAfter == 3) {
                    s = s.replace(",", "");
                } else {
                    s = s.replace(",", ".");
                }
            } else if (hasDot) {
                // Nếu sau dấu chấm là 3 số thì coi là phân cách hàng nghìn.
                // Nếu là .0, .00 thì giữ lại để parse double.
                int lastDot = s.lastIndexOf('.');
                int digitsAfter = s.length() - lastDot - 1;
                if (digitsAfter == 3) {
                    s = s.replace(".", "");
                }
            }

            return Double.parseDouble(s);
        } catch (Exception ex) {
            return 0.0;
        }
    }

    private double roundMoney(double value) {
        return Math.round(value);
    }

    private void updateKpiMiniPanel() {
        currentKpiEval.setActualValue(finalAmountToPay);
        currentKpiEval.setAchievedScore((finalAmountToPay >= 500000) ? 100 : (finalAmountToPay >= 100000) ? 50 : 0);
    }

    private void findCustomer() {
        String p = txtCustomerPhone.getText().trim();
        if (p.isBlank()) {
            selectedCustomer = null;
            updateCustomerUI();
            return;
        }
        selectedCustomer = CustomersSql.getInstance().findByPhone(p);
        if (selectedCustomer != null) {
            txtCustomerPhone.setText("");
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
                    txtCustomerPhone.setText("");
                    updateCustomerUI();
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
            lblCusName.setText(selectedCustomer.getCustomerName() != null ? selectedCustomer.getCustomerName() : "Khách hàng");
            double spend = selectedCustomer.getTotalSpending();
            String rank = selectedCustomer.getMemberRank();
            double rate = getSelectedCustomerDiscountRate();

            if (rank == null || rank.trim().isEmpty()) {
                rank = "Thường";
            }
            rank = rank.trim();

            Color c = TEXT_GRAY;
            String rankDisplay = rank;

            if (rank.equalsIgnoreCase("Kim Cương")) {
                c = new Color(155, 89, 182);
                rankDisplay = "Kim Cương";
            } else if (rank.equalsIgnoreCase("Vàng")) {
                c = WARNING_YELLOW;
                rankDisplay = "Vàng";
            } else if (rank.equalsIgnoreCase("Bạc")) {
                c = new Color(189, 195, 199);
                rankDisplay = "Bạc";
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

    private void loadProducts() {
        if (productsCardLayout != null && pnlProductsBody != null) {
            productsCardLayout.show(pnlProductsBody, "loading");
        }

        new SwingWorker<List<Product>, Void>() {
            @Override
            protected List<Product> doInBackground() {
                String storeId = requireCurrentStoreForSale();
                return ProductsSql.getInstance().selectAllByStore(storeId);
            }

            @Override
            protected void done() {
                try {
                    allProducts = get();

                    refreshSearchSuggestions(productSearchKeyword);
                    applyProductFilter(productSearchKeyword);

                    // Reset preview sau khi reload danh sách sản phẩm
                    clearProductPreview();

                } catch (Exception ex) {
                    ex.printStackTrace();

                    JOptionPane.showMessageDialog(
                            SellPanel.this,
                            "Không thể tải sản phẩm theo chi nhánh hiện tại:\n" + ex.getMessage(),
                            "Lỗi tải sản phẩm",
                            JOptionPane.ERROR_MESSAGE
                    );

                    if (productsCardLayout != null && pnlProductsBody != null) {
                        productsCardLayout.show(pnlProductsBody, "table");
                    }

                    clearProductPreview();
                }
            }
        }.execute();
    }

    private String requireCurrentStoreForSale() {
        String storeId = SessionManager.getCurrentStoreId();

        if (storeId == null || storeId.trim().isEmpty()) {
            throw new IllegalStateException("Tài khoản bán hàng chưa được phân chi nhánh.");
        }

        return storeId.trim();
    }

    private int getStockFromDB(String pId) {
        if (pId == null || pId.trim().isEmpty()) {
            return 0;
        }

        try {
            String storeId = requireCurrentStoreForSale();

            Product p = ProductsSql.getInstance().findByIdInStore(pId.trim(), storeId);

            if (p == null) {
                return 0;
            }

            return Math.max(0, p.getQuantity());

        } catch (Exception e) {
            System.err.println("[SellPanel] getStockFromDB error: " + e.getMessage());
            return 0;
        }
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

    private void loadActivePromotions() {
        if (cboKhuyenMai == null) {
            return;
        }

        promoRuleMap.clear();
        cboKhuyenMai.removeAllItems();
        cboKhuyenMai.addItem("Không áp dụng mã giảm giá");

        new SwingWorker<java.util.List<PromoRule>, Void>() {
            @Override
            protected java.util.List<PromoRule> doInBackground() {
                java.util.Map<String, PromoRule> temp = new java.util.LinkedHashMap<>();

                String sql = """
                    SELECT
                        p.promotion_id,
                        p.promotion_name,
                        NVL(p.discount_percent, NVL(p.discount_amount, 0)) AS discount_percent,
                        NVL(p.min_order_amount, 100000) AS min_order_amount,
                        pp.product_id
                    FROM PROMOTIONS p
                    JOIN PROMOTION_CAMPAIGNS c
                        ON p.campaign_id = c.campaign_id
                    JOIN PROMOTION_PRODUCTS pp
                        ON pp.promotion_id = p.promotion_id
                       AND NVL(pp.is_deleted, 0) = 0
                    WHERE NVL(p.is_deleted, 0) = 0
                      AND NVL(p.status, N'Đang diễn ra') = N'Đang diễn ra'
                      AND (
                            c.start_date IS NULL
                            OR TRUNC(SYSDATE) >= TRUNC(c.start_date)
                          )
                      AND (
                            c.end_date IS NULL
                            OR TRUNC(SYSDATE) <= TRUNC(c.end_date)
                          )
                    ORDER BY p.promotion_id, pp.product_id
                """;

                try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql); java.sql.ResultSet rs = ps.executeQuery()) {

                    while (rs.next()) {
                        String promoId = rs.getString("promotion_id");

                        PromoRule rule = temp.get(promoId);
                        if (rule == null) {
                            rule = new PromoRule();
                            rule.promotionId = promoId;
                            rule.promotionName = rs.getString("promotion_name");
                            rule.discountPercent = rs.getDouble("discount_percent");
                            rule.minOrderAmount = rs.getDouble("min_order_amount");
                            temp.put(promoId, rule);
                        }

                        String productId = rs.getString("product_id");
                        if (productId != null && !productId.trim().isEmpty()) {
                            rule.productIds.add(productId.trim());
                        }
                    }

                } catch (Exception e) {
                    System.err.println("Lỗi tải khuyến mãi theo sản phẩm: " + e.getMessage());
                    e.printStackTrace();
                }

                return new java.util.ArrayList<>(temp.values());
            }

            @Override
            protected void done() {
                try {
                    java.util.List<PromoRule> rules = get();

                    promoRuleMap.clear();
                    cboKhuyenMai.removeAllItems();
                    cboKhuyenMai.addItem("Không áp dụng mã giảm giá");

                    for (PromoRule rule : rules) {
                        promoRuleMap.put(rule.promotionId, rule);
                        cboKhuyenMai.addItem(rule.comboLabel());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }

    private void processPaymentAction() {
        if (paymentProcessing) {
            return;
        }

        if (modCart.getRowCount() <= 0) {
            return;
        }

        try {
            validateCartAgainstDatabase();

            if (!btnPay.isEnabled()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Giỏ hàng có sản phẩm không hợp lệ hoặc vượt tồn kho. Vui lòng kiểm tra lại.",
                        "Không thể thanh toán",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể kiểm tra tồn kho trước thanh toán:\n" + ex.getMessage(),
                    "Lỗi kiểm tra tồn kho",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        // Chốt lại tiền ngay trước khi thanh toán để đảm bảo đã tính đúng
        // giảm giá thành viên + giảm giá chương trình theo khách/voucher hiện tại.
        calculateTotal();

        paymentProcessing = true;
        paymentJustSucceeded = false;

        btnPay.setEnabled(false);
        btnPay.setText("ĐANG XỬ LÝ...");
        btnPay.setBackground(Color.GRAY);
        btnCancel.setEnabled(false);
        btnRemove.setEnabled(false);

        String emp = "EMP_DEFAULT";
        model.account.Account a = SessionManager.getCurrentUser();

        if (a != null) {
            emp = (a.getUserId() != null && !a.getUserId().isBlank())
                    ? a.getUserId()
                    : a.getAccountId();
        }

        String pId = cboPaymentMethod.getSelectedItem() != null
                ? cboPaymentMethod.getSelectedItem().toString()
                : "PM_CASH";

        String oId = "HD" + System.nanoTime();

        // Lưu lại số giảm giá riêng cho report trước khi clear cart/reset khách hàng
        lastPaidMemberDiscountAmount = currentMemberDiscountAmount;
        lastPaidProgramDiscountAmount = currentProgramDiscountAmount;

        Order o = new Order();
        o.setOrderId(oId);
        o.setCustomerId(selectedCustomer != null ? selectedCustomer.getCustomerId() : null);
        o.setEmployeeId(emp);
        o.setPaymentMethodId(pId);
        o.setOrderDate(new java.sql.Date(System.currentTimeMillis()));
        o.setTotalAmount(finalAmountToPay);
        o.setStoreId(requireCurrentStoreForSale());

        List<OrderDetail> dt = new ArrayList<>();

        for (int i = 0; i < modCart.getRowCount(); i++) {
            String id = modCart.getValueAt(i, 0).toString();
            int qty = Integer.parseInt(modCart.getValueAt(i, 2).toString());
            double unitPrice = Double.parseDouble(modCart.getValueAt(i, 3).toString());

            Product p = allProducts.stream()
                    .filter(x -> x.getProductId().equals(id))
                    .findFirst()
                    .orElse(null);

            dt.add(new OrderDetail(
                    oId,
                    id,
                    qty,
                    unitPrice,
                    (p != null && p.getBaseUnitId() != null) ? p.getBaseUnitId() : "U_CAI",
                    qty
            ));
        }

        new SwingWorker<Boolean, Void>() {
            private Exception error;

            @Override
            protected Boolean doInBackground() {
                try {
                    return PaymentService.thanhToan(o, dt);
                } catch (Exception ex) {
                    error = ex;
                    return false;
                }
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();

                    if (success) {
                        paymentJustSucceeded = true;
                        persistDiscountBreakdownAfterPayment(o.getOrderId());
                        handlePaymentSuccess(o.getOrderId());
                    } else {
                        if (error != null) {
                            handleGeneralError(error);
                        } else {
                            JOptionPane.showMessageDialog(
                                    SellPanel.this,
                                    "Thanh toán thất bại. Có thể tồn kho đã thay đổi, vui lòng làm mới giỏ hàng.",
                                    "Thanh toán thất bại",
                                    JOptionPane.WARNING_MESSAGE
                            );
                        }

                        loadProducts();
                        validateCartAgainstDatabase();
                    }

                } catch (Exception ex) {
                    handleGeneralError(ex);

                } finally {
                    paymentProcessing = false;
                    resetPaymentUI();

                    Timer t = new Timer(700, ev -> paymentJustSucceeded = false);
                    t.setRepeats(false);
                    t.start();
                }
            }
        }.execute();
    }

    private void handlePaymentSuccess(String orderId) {
        boolean shouldPrint = chkPrintBill != null && chkPrintBill.isSelected();

        // Clear UI trước để tránh giỏ hàng cũ bị validate lại khi realtime vừa bắn về
        clearCart();
        resetCustomerAfterPayment();

        // Reload danh sách sản phẩm sau khi cart đã trống
        loadProducts();

        JOptionPane.showMessageDialog(
                this,
                "✅ Thanh toán thành công! Hóa đơn: " + orderId,
                "Thanh toán thành công",
                JOptionPane.INFORMATION_MESSAGE
        );

        if (shouldPrint) {
            openInvoiceReportAfterPayment(orderId);
        }
    }

    private PromoRule getSelectedPromoRule() {
        if (cboKhuyenMai == null || cboKhuyenMai.getSelectedItem() == null) {
            return null;
        }

        String label = cboKhuyenMai.getSelectedItem().toString();

        if (label.equalsIgnoreCase("Không áp dụng mã giảm giá")) {
            return null;
        }

        String promoId = label.split("\\|")[0].trim();

        return promoRuleMap.get(promoId);
    }

    private void persistDiscountBreakdownAfterPayment(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            return;
        }

        PromoRule selectedRule = getSelectedPromoRule();
        String promotionId = selectedRule == null ? null : selectedRule.promotionId;

        String sqlUpdateOrder = """
            UPDATE ORDERS
            SET promotion_id = ?,
                member_discount_amount = ?,
                program_discount_amount = ?
            WHERE order_id = ?
        """;

        String sqlUpdateDetail = """
            UPDATE ORDER_DETAILS
            SET promotion_id = ?,
                program_discount_percent = ?,
                program_discount_amount = ?,
                line_net_total = (NVL(quantity_base, quantity) * unit_price) - ?
            WHERE order_id = ?
              AND product_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try (java.sql.PreparedStatement ps = con.prepareStatement(sqlUpdateOrder)) {
                ps.setString(1, promotionId);
                ps.setDouble(2, currentMemberDiscountAmount);
                ps.setDouble(3, currentProgramDiscountAmount);
                ps.setString(4, orderId);
                ps.executeUpdate();
            }

            try (java.sql.PreparedStatement ps = con.prepareStatement(sqlUpdateDetail)) {
                for (int i = 0; i < modCart.getRowCount(); i++) {
                    String productId = String.valueOf(modCart.getValueAt(i, 0));

                    double percent = currentLineProgramDiscountPercent.getOrDefault(productId, 0.0);
                    double amount = currentLineProgramDiscountAmount.getOrDefault(productId, 0.0);

                    ps.setString(1, percent > 0 ? promotionId : null);
                    ps.setDouble(2, percent);
                    ps.setDouble(3, amount);
                    ps.setDouble(4, amount);
                    ps.setString(5, orderId);
                    ps.setString(6, productId);
                    ps.addBatch();
                }

                ps.executeBatch();
            }

            con.commit();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Thanh toán đã thành công nhưng lưu chi tiết khuyến mãi bị lỗi:\n" + e.getMessage(),
                    "Cảnh báo lưu khuyến mãi",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void openInvoiceReportAfterPayment(String orderId) {
        if (orderId == null || orderId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Thanh toán đã thành công nhưng không tìm thấy mã hóa đơn để in.",
                    "Không thể mở hóa đơn",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        try {
            java.util.HashMap<String, Object> params = new java.util.HashMap<>();

            params.put("ORDER_ID", orderId.trim());

            // Bắt buộc truyền 2 số này để report không tự dồn hết về giảm chương trình
            params.put(
                    "MEMBER_DISCOUNT_AMOUNT",
                    java.math.BigDecimal.valueOf(roundMoney(lastPaidMemberDiscountAmount))
            );

            params.put(
                    "PROGRAM_DISCOUNT_AMOUNT",
                    java.math.BigDecimal.valueOf(roundMoney(lastPaidProgramDiscountAmount))
            );

            try {
                String storeId = SessionManager.getCurrentStoreId();
                if (storeId != null && !storeId.trim().isEmpty()) {
                    params.put("STORE_ID", storeId.trim());
                }
            } catch (Exception ignored) {
            }

            common.report.ReportViewer.showReport(
                    "/reports/SalesInvoiceReport.jrxml",
                    params
            );

        } catch (Exception ex) {
            ex.printStackTrace();

            JOptionPane.showMessageDialog(
                    this,
                    "Thanh toán đã thành công nhưng không mở được report hóa đơn:\n" + ex.getMessage(),
                    "Lỗi mở hóa đơn",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void handleGeneralError(Exception ex) {
        JOptionPane.showMessageDialog(this, "Lỗi hệ thống: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private void resetPaymentUI() {
        btnPay.setText("THANH TOÁN");
        btnPay.setBackground(SUCCESS_GREEN);

        boolean hasCart = modCart.getRowCount() > 0;
        btnPay.setEnabled(hasCart && !paymentProcessing);
        btnCancel.setEnabled(true);
        btnRemove.setEnabled(true);
    }

    private void resetCustomerAfterPayment() {
        selectedCustomer = null;
        if (txtCustomerPhone != null) {
            txtCustomerPhone.setText("");
        }
        // Reset Khuyến mãi về dòng đầu tiên ("Không áp dụng")
        if (cboKhuyenMai != null && cboKhuyenMai.getItemCount() > 0) {
            cboKhuyenMai.setSelectedIndex(0);
        }
        updateCustomerUI();
    }

    // =========================================================
    // CUSTOM UI CLASSES 
    // =========================================================
    private JTable createTable(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(50);
        t.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        t.setShowHorizontalLines(false);
        t.setShowVerticalLines(false);
        t.setIntercellSpacing(new Dimension(0, 1));
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        t.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.getTableHeader().setBackground(new Color(239, 242, 247));
        t.getTableHeader().setForeground(TEXT_DARK);
        t.getTableHeader().setPreferredSize(new Dimension(0, 42));
        t.setSelectionBackground(new Color(212, 230, 241));
        t.setSelectionForeground(TEXT_DARK);
        DefaultTableCellRenderer c = new DefaultTableCellRenderer();
        c.setHorizontalAlignment(JLabel.CENTER);
        t.setDefaultRenderer(Object.class, c);

        t.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = t.rowAtPoint(e.getPoint());
                if (row != hoverRow) {
                    hoverRow = row;
                    t.repaint();
                }
            }
        });
        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                if (hoverRow != -1) {
                    hoverRow = -1;
                    t.repaint();
                }
            }
        });
        return t;
    }

    class ModernCardPanel extends JPanel {

        private int rad;

        public ModernCardPanel(int r) {
            this.rad = r;
            setOpaque(false);
            setBackground(CARD_WHITE);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), rad, rad);
            g2.setColor(BORDER_GRAY);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, rad, rad);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class CircleAvatarPanel extends JPanel {

        private final int size;
        private final Color fill;

        CircleAvatarPanel(int size, Color fill) {
            this.size = size;
            this.fill = fill;
            setOpaque(false);
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fill);
            g2.fillOval(0, 0, size - 1, size - 1);
            g2.setColor(new Color(208, 220, 235));
            g2.drawOval(0, 0, size - 1, size - 1);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class RoundedButton extends JButton {

        public RoundedButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setVerticalAlignment(SwingConstants.CENTER);

            // 🐛 FIX Ở ĐÂY: Thêm chữ "set" vào và đưa vào trong ngoặc tròn
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color color = getBackground();
            if (!isEnabled()) {
                color = new Color(180, 180, 180);
            } else if (getModel().isPressed()) {
                color = getBackground().darker();
            } else if (getModel().isRollover()) {
                color = getBackground().brighter();
            }

            g2.setColor(color);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);

            super.paintComponent(g);
            g2.dispose();
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
                if (!b.isEnabled()) {
                    g2.setColor(new Color(190, 190, 190));
                    g2.fillRoundRect(x, y, w, h, h, h);
                    g2.setColor(new Color(245, 245, 245));
                    g2.fillOval(x + 2, y + 2, h - 4, h - 4);
                } else if (b.isSelected()) {
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

    class QuantitySpinnerEditor extends DefaultCellEditor {

        JSpinner s;

        public QuantitySpinnerEditor() {
            super(new JTextField());
            setClickCountToStart(1);
            s = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
            s.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 228, 236), 1, true), new EmptyBorder(2, 8, 2, 8)));
            JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) s.getEditor();
            editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
            editor.getTextField().setFont(new Font("Segoe UI", Font.BOLD, 16));
            editor.getTextField().addActionListener(e -> stopCellEditing());
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object v, boolean sel, int r, int c) {
            s.setValue(Integer.parseInt(v.toString()));
            return s;
        }

        @Override
        public Object getCellEditorValue() {
            try {
                ((JSpinner.DefaultEditor) s.getEditor()).commitEdit();
            } catch (Exception ex) {
            }
            return s.getValue();
        }

        @Override
        public boolean stopCellEditing() {
            try {
                ((JSpinner.DefaultEditor) s.getEditor()).commitEdit();
                int row = tblCart.getEditingRow();
                if (row >= 0 && row < modCart.getRowCount()) {
                    int qty = (Integer) s.getValue();
                    if (qty <= 0) {
                        SwingUtilities.invokeLater(() -> {
                            if (row >= 0 && row < modCart.getRowCount()) {
                                modCart.removeRow(row);
                                calculateTotal();
                                validateCartAgainstDatabase();
                            }
                        });
                        return super.stopCellEditing();
                    }
                    double price = Double.parseDouble(modCart.getValueAt(row, 3).toString());
                    modCart.setValueAt(price * qty, row, 4);
                    calculateTotal();
                    validateCartAgainstDatabase();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return super.stopCellEditing();
        }
    }

    class QuantitySpinnerRenderer implements javax.swing.table.TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean selected, boolean focused, int r, int c) {
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(1, 0, 9999, 1));
            spinner.setEnabled(false);
            spinner.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(220, 228, 236), 1, true), new EmptyBorder(2, 8, 2, 8)));
            JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
            editor.getTextField().setFont(new Font("Segoe UI", Font.BOLD, 16));
            editor.getTextField().setHorizontalAlignment(SwingConstants.CENTER);
            editor.getTextField().setBorder(BorderFactory.createEmptyBorder());
            editor.getTextField().setForeground(TEXT_DARK);
            editor.getTextField().setBackground(Color.WHITE);

            int q = 0, st = 0;
            try {
                q = Integer.parseInt(t.getValueAt(r, 2).toString());
                st = Integer.parseInt(t.getValueAt(r, 5).toString());
            } catch (Exception ex) {
            }
            spinner.setValue(q);

            Color bg = selected ? new Color(230, 240, 255) : (r == hoverRow ? new Color(244, 248, 255) : (r % 2 == 0 ? Color.WHITE : new Color(249, 251, 253)));
            if (q > st) {
                bg = new Color(253, 237, 236);
            } else if (st <= 5) {
                bg = new Color(254, 249, 231);
            }

            spinner.setBackground(bg);
            editor.getTextField().setBackground(bg);
            return spinner;
        }
    }

    class CartTableRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
            if ((c == 3 || c == 4) && v instanceof Number) {
                v = moneyFormat.format(v);
            }
            Component cp = super.getTableCellRendererComponent(t, v, s, f, r, c);
            Color rowBg = (r == hoverRow) ? new Color(244, 248, 255) : (r % 2 == 0 ? Color.WHITE : new Color(249, 251, 253));

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
                    setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                    setOpaque(true);
                    if (q > st) {
                        setText("Vượt tồn");
                        setForeground(DANGER_RED);
                        setBackground(new Color(251, 226, 226));
                    } else if (st <= 5) {
                        setText("Sắp hết");
                        setForeground(new Color(153, 120, 0));
                        setBackground(new Color(255, 241, 198));
                    } else {
                        setText("Hợp lệ");
                        setForeground(SUCCESS_GREEN);
                        setBackground(new Color(224, 245, 233));
                    }
                } else {
                    setForeground(TEXT_DARK);
                    setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    setOpaque(true);
                    setBackground(q > st ? new Color(253, 237, 236) : (st <= 5 ? new Color(254, 249, 231) : rowBg));
                }

                if (c != 6 && s) {
                    setBackground(new Color(230, 240, 255));
                }
            } catch (Exception e) {
            }
            return cp;
        }
    }

    private double getPromoRateFromString(String promoString) {
        PromoRule rule = getSelectedPromoRule();

        if (rule != null) {
            return rule.discountPercent;
        }

        if (promoString == null || promoString.isEmpty() || promoString.equals("Không áp dụng mã giảm giá")) {
            return 0.0;
        }

        try {
            if (promoString.contains("|")) {
                String[] parts = promoString.split("\\|");
                for (String part : parts) {
                    String p = part.trim();
                    if (p.endsWith("%")) {
                        return Double.parseDouble(p.replace("%", "").trim());
                    }
                }
            }

            String[] parts = promoString.split("Giảm ");
            if (parts.length > 1) {
                String percentStr = parts[1].replace("%)", "").replace("%", "").trim();
                return Double.parseDouble(percentStr);
            }
        } catch (Exception e) {
            return 0.0;
        }

        return 0.0;
    }

    private boolean isEligibleForPromotion(Product p, String selectedPromo) {
        if (p == null) {
            return false;
        }

        PromoRule rule = getSelectedPromoRule();

        return rule != null
                && p.getProductId() != null
                && rule.productIds.contains(p.getProductId());
    }
}
