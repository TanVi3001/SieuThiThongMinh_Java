package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import business.service.AuthorizationService;
import business.service.ProductImportService;
import business.sql.prod_inventory.InventoryTransactionSql;
import business.sql.prod_inventory.ProductsSql;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.product.Product;
import view.components.IconHelper;
import business.sql.prod_inventory.InventoryNotificationSql;
import common.realtime.PanelRealtimeSupport;
import common.realtime.RealtimeNotifier;
import common.ui.UiAsync;

public class InventoryView extends JPanel {

    private final Color BACKGROUND = new Color(246, 247, 251);
    private final Color CARD = Color.WHITE;
    private final Color NAVY = new Color(23, 52, 99);
    private final Color MUTED = new Color(111, 124, 149);
    private final Color SOFT_MUTED = new Color(163, 174, 208);
    private final Color BORDER = new Color(232, 237, 245);
    private final Color BLUE = new Color(67, 97, 238);
    private final Color GREEN = new Color(0, 163, 108);
    private final Color ORANGE = new Color(255, 153, 0);
    private final Color RED = new Color(220, 53, 69);
    private final Color PURPLE = new Color(103, 58, 183);
    private final Color GRAY_BUTTON = new Color(142, 153, 176);

    private JTable tblInventory;
    private DefaultTableModel tableModel;

    /*
     * Đã bỏ combobox "Lọc theo kho" khỏi UI.
     * Vẫn giữ biến này để tránh phá các hàm cũ nếu nơi khác còn gọi,
     * nhưng nó không còn được add lên header nữa.
     */
    private JComboBox<String> cbStoreFilter;
    private JTextField txtSearch;

    private JButton btnInbound;
    private JButton btnAuditLog;
    private JButton btnSearch;
    private JButton btnResetSearch;
    private JButton btnImportCsv;
    private JLabel lblTotalItems;
    private JLabel lblLowStock;
    private JLabel lblOutOfStock;
    private JLabel lblTotalQuantity;

    private JPanel alertListPanel;
    private JPanel recentActivityPanel;
    private final List<Product> cachedInventory = new ArrayList<>();
    private boolean updatingStoreFilter = false;

    public InventoryView() {
        setLayout(new BorderLayout(0, 18));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents();
        subscribeRealtimeInventory();
        loadInventoryData();
        applyInventoryRolePermission();
    }

    private void initUI() {
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private void subscribeRealtimeInventory() {
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e == null || e.getType() == null) {
                return;
            }

            if (e.getType() == AppEventType.INVENTORY
                    || e.getType() == AppEventType.PRODUCTS
                    || e.getType() == AppEventType.ORDERS
                    || e.getType() == AppEventType.INVENTORY_ALERT) {

                SwingUtilities.invokeLater(this::loadInventoryData);
            }
        });
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Quản Lý Tồn Kho");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Theo dõi số lượng thực tế, cảnh báo tồn kho và điều chỉnh kho hàng theo chi nhánh hiện tại");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(SOFT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel branchInfo = new JLabel(buildCurrentBranchLabel());
        branchInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        branchInfo.setForeground(new Color(0, 120, 95));
        branchInfo.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(6));
        titleBox.add(subtitle);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(branchInfo);

        /*
         * Không add filterBox nữa. Lọc kho bị loại bỏ theo yêu cầu.
         * Dữ liệu bảng, KPI, cảnh báo, biến động kho đều lấy theo store trong session.
         */
        cbStoreFilter = new JComboBox<>();
        cbStoreFilter.addItem(buildCurrentBranchValueForHiddenFilter());

        header.add(titleBox, BorderLayout.WEST);

        return header;
    }

    private String buildCurrentBranchLabel() {
        String currentStoreId = getCurrentStoreIdOrNull();

        if (currentStoreId == null || currentStoreId.isBlank()) {
            return "Phạm vi kho: Chưa xác định chi nhánh";
        }

        return "Phạm vi kho: " + currentStoreId;
    }

    private String buildCurrentBranchValueForHiddenFilter() {
        String currentStoreId = getCurrentStoreIdOrNull();
        return currentStoreId == null ? "Chưa xác định" : currentStoreId;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);

        body.add(buildKpiSection(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(16, 0));
        center.setOpaque(false);

        JPanel leftMain = new JPanel(new BorderLayout(0, 16));
        leftMain.setOpaque(false);
        leftMain.add(buildAlertZone(), BorderLayout.NORTH);
        leftMain.add(buildInventoryTableCard(), BorderLayout.CENTER);

        JPanel rightPanel = buildRecentActivityPanel();
        rightPanel.setPreferredSize(new Dimension(310, 0));

        center.add(leftMain, BorderLayout.CENTER);
        center.add(rightPanel, BorderLayout.EAST);

        body.add(center, BorderLayout.CENTER);

        return body;
    }

    private JPanel buildKpiSection() {
        JPanel kpiPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        kpiPanel.setOpaque(false);
        kpiPanel.setPreferredSize(new Dimension(0, 104));

        lblTotalItems = new JLabel("0");
        lblLowStock = new JLabel("0");
        lblOutOfStock = new JLabel("0");
        lblTotalQuantity = new JLabel("0");

        kpiPanel.add(createKpiCard("Tổng mặt hàng", lblTotalItems, BLUE, "Tổng số mã sản phẩm đang theo dõi"));
        kpiPanel.add(createKpiCard("Sắp hết hàng (<20)", lblLowStock, ORANGE, "Cần chuẩn bị nhập bổ sung"));
        kpiPanel.add(createKpiCard("Hết sạch hàng", lblOutOfStock, RED, "Cần xử lý ngay"));
        kpiPanel.add(createKpiCard("Tổng tồn kho", lblTotalQuantity, GREEN, "Tổng số lượng hiện có"));

        return kpiPanel;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, Color accent, String desc) {
        RoundedPanel card = new RoundedPanel(18, CARD);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER, 18),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JPanel stripe = new JPanel();
        stripe.setBackground(accent);
        stripe.setPreferredSize(new Dimension(5, 0));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(SOFT_MUTED);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valueLabel.setForeground(NAVY);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblDesc = new JLabel(desc);
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDesc.setForeground(MUTED);
        lblDesc.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(lblTitle);
        content.add(Box.createVerticalStrut(8));
        content.add(valueLabel);
        content.add(Box.createVerticalStrut(4));
        content.add(lblDesc);

        card.add(stripe, BorderLayout.WEST);
        card.add(content, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildAlertZone() {
        RoundedPanel alertCard = new RoundedPanel(18, CARD);
        alertCard.setLayout(new BorderLayout(0, 12));
        alertCard.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER, 18),
                new EmptyBorder(16, 18, 16, 18)
        ));
        alertCard.setPreferredSize(new Dimension(0, 160));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Cảnh báo cần xử lý");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(NAVY);

        JLabel subtitle = new JLabel("Ưu tiên sản phẩm hết hàng hoặc sắp hết để nhập bổ sung kịp thời");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(MUTED);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(3));
        titleBox.add(subtitle);

        btnImportCsv = createButton("Nhập CSV", BLUE, Color.WHITE, IconHelper.file(18));
        btnImportCsv.setPreferredSize(new Dimension(130, 38));

        header.add(titleBox, BorderLayout.WEST);
        header.add(btnImportCsv, BorderLayout.EAST);

        alertListPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        alertListPanel.setOpaque(false);

        JScrollPane alertScroll = new JScrollPane(alertListPanel);
        alertScroll.setBorder(BorderFactory.createEmptyBorder());
        alertScroll.setOpaque(false);
        alertScroll.getViewport().setOpaque(false);
        alertScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        alertScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        alertScroll.getHorizontalScrollBar().setUnitIncrement(18);

        alertCard.add(header, BorderLayout.NORTH);
        alertCard.add(alertScroll, BorderLayout.CENTER);

        return alertCard;
    }

    private JPanel createAlertItem(Product p) {
        int qty = p.getQuantity();

        Color accent = qty <= 0 ? RED : ORANGE;
        Color bg = qty <= 0 ? new Color(255, 239, 239) : new Color(255, 248, 232);
        String status = qty <= 0 ? "HẾT HÀNG" : "SẮP HẾT";
        String qtyText = qty <= 0 ? "0" : String.valueOf(qty);

        RoundedPanel card = new RoundedPanel(14, bg);
        card.setLayout(new BorderLayout(8, 0));
        card.setPreferredSize(new Dimension(250, 72));
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel icon = new JLabel(qty <= 0 ? "🚨" : "⚠️");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        icon.setPreferredSize(new Dimension(34, 0));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel lblName = new JLabel(shortText(p.getProductName(), 24));
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(NAVY);

        JLabel lblInfo = new JLabel(status + " • Còn: " + qtyText + " " + safe(p.getUnit(), "Cái"));
        lblInfo.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblInfo.setForeground(accent);

        JLabel lblId = new JLabel(safe(p.getProductId(), ""));
        lblId.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblId.setForeground(MUTED);

        text.add(lblName);
        text.add(Box.createVerticalStrut(4));
        text.add(lblInfo);
        text.add(Box.createVerticalStrut(2));
        text.add(lblId);

        card.add(icon, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectProductInTable(p.getProductId());
            }
        });

        return card;
    }

    private JPanel buildInventoryTableCard() {
        RoundedPanel tableCard = new RoundedPanel(18, CARD);
        tableCard.setLayout(new BorderLayout(0, 12));
        tableCard.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER, 18),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JPanel actionBar = new JPanel(new BorderLayout(12, 0));
        actionBar.setOpaque(false);

        JPanel searchPanel = new JPanel(new BorderLayout(8, 0));
        searchPanel.setOpaque(false);

        txtSearch = new JTextField();
        txtSearch.setPreferredSize(new Dimension(300, 38));
        txtSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo mã SP hoặc tên sản phẩm...");
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER, 18),
                new EmptyBorder(0, 14, 0, 14)
        ));

        btnSearch = createButton("Tìm", BLUE, Color.WHITE, IconHelper.search(16));
        btnSearch.setPreferredSize(new Dimension(88, 38));

        btnResetSearch = createButton("Đặt lại", GRAY_BUTTON, Color.WHITE, IconHelper.refresh(16));
        btnResetSearch.setPreferredSize(new Dimension(105, 38));

        JPanel searchButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        searchButtons.setOpaque(false);
        searchButtons.add(btnSearch);
        searchButtons.add(btnResetSearch);

        searchPanel.add(txtSearch, BorderLayout.CENTER);
        searchPanel.add(searchButtons, BorderLayout.EAST);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        btnInbound = createButton("Nhập Kho", GREEN, Color.WHITE, IconHelper.add(18));
        btnAuditLog = createButton("Lịch sử biến động", PURPLE, Color.WHITE, IconHelper.history(18));

        btnInbound.setPreferredSize(new Dimension(130, 38));
        btnAuditLog.setPreferredSize(new Dimension(165, 38));

        buttons.add(btnInbound);
        buttons.add(btnAuditLog);

        actionBar.add(searchPanel, BorderLayout.WEST);
        actionBar.add(buttons, BorderLayout.EAST);

        tableModel = new DefaultTableModel(
                new Object[]{
                    "Ảnh",
                    "Mã SP",
                    "Tên sản phẩm",
                    "Tồn hiện tại",
                    "Mức cảnh báo",
                    "Đơn vị",
                    "Chi nhánh",
                    "Trạng thái",
                    "Cập nhật cuối"
                },
                0
        ) {
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 0) {
                    return ImageIcon.class;
                }
                return Object.class;
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblInventory = new JTable(tableModel);
        setupTableStyle();
        tblInventory.setRowHeight(60);
        tblInventory.getColumnModel().getColumn(0).setPreferredWidth(70);
        tblInventory.getColumnModel().getColumn(0).setMaxWidth(70);
        tblInventory.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel lbl = new JLabel();
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setOpaque(true);
                lbl.setBackground(sel ? t.getSelectionBackground() : Color.WHITE);
                if (v instanceof ImageIcon) {
                    lbl.setIcon((ImageIcon) v);
                } else {
                    lbl.setText("—");
                }
                return lbl;
            }
        });

        JScrollPane scrollPane = new JScrollPane(tblInventory);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        tableCard.add(actionBar, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        return tableCard;
    }

    private void setupTableStyle() {
        tblInventory.setRowHeight(42);
        tblInventory.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblInventory.setForeground(NAVY);
        tblInventory.setGridColor(new Color(245, 246, 250));
        tblInventory.setShowVerticalLines(false);
        tblInventory.setShowHorizontalLines(true);
        tblInventory.setSelectionBackground(new Color(237, 242, 255));
        tblInventory.setSelectionForeground(NAVY);

        tblInventory.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblInventory.getTableHeader().setBackground(new Color(245, 246, 250));
        tblInventory.getTableHeader().setForeground(Color.BLACK);
        tblInventory.getTableHeader().setReorderingAllowed(false);

        tblInventory.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                Component c = super.getTableCellRendererComponent(
                        table,
                        value,
                        isSelected,
                        hasFocus,
                        row,
                        column
                );

                int modelRow = table.convertRowIndexToModel(row);
                int qty = parseInt(table.getModel().getValueAt(modelRow, 3));
                String status = String.valueOf(table.getModel().getValueAt(modelRow, 7));

                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(NAVY);
                } else if (qty <= 0) {
                    c.setBackground(new Color(255, 235, 235));
                    c.setForeground(column == 6 ? RED : new Color(120, 20, 20));
                } else if (qty <= 10) {
                    c.setBackground(new Color(255, 243, 224));
                    c.setForeground(column == 6 ? new Color(230, 81, 0) : new Color(120, 70, 0));
                } else if (qty <= 30) {
                    c.setBackground(new Color(255, 253, 231));
                    c.setForeground(column == 6 ? ORANGE : NAVY);
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(NAVY);
                }

                if (c instanceof JLabel lbl) {
                    lbl.setBorder(new EmptyBorder(0, 8, 0, 8));
                    lbl.setHorizontalAlignment(
                            column == 3 || column == 4 || column == 5 || column == 6
                                    ? SwingConstants.CENTER
                                    : SwingConstants.LEFT
                    );

                    if (column == 6 || status.contains("Hết") || status.contains("Sắp")) {
                        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    } else {
                        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    }
                }

                return c;
            }
        });

        tblInventory.getColumnModel().getColumn(0).setPreferredWidth(70);
        tblInventory.getColumnModel().getColumn(1).setPreferredWidth(100);
        tblInventory.getColumnModel().getColumn(2).setPreferredWidth(260);
        tblInventory.getColumnModel().getColumn(3).setPreferredWidth(100);
        tblInventory.getColumnModel().getColumn(4).setPreferredWidth(105);
        tblInventory.getColumnModel().getColumn(5).setPreferredWidth(80);
        tblInventory.getColumnModel().getColumn(6).setPreferredWidth(100);
        tblInventory.getColumnModel().getColumn(7).setPreferredWidth(120);
        tblInventory.getColumnModel().getColumn(8).setPreferredWidth(140);
    }

    private JPanel buildRecentActivityPanel() {
        RoundedPanel card = new RoundedPanel(18, Color.WHITE);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(22, 22, 22, 22));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Biến động kho gần đây");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(NAVY);

        JLabel sub = new JLabel("Theo dõi nhanh giao dịch của chi nhánh hiện tại.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(MUTED);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(6));
        titleBox.add(sub);

        recentActivityPanel = new JPanel();
        recentActivityPanel.setOpaque(false);
        recentActivityPanel.setLayout(new BoxLayout(recentActivityPanel, BoxLayout.Y_AXIS));
        recentActivityPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleBox, BorderLayout.NORTH);
        card.add(recentActivityPanel, BorderLayout.CENTER);

        return card;
    }

    private JPanel createActivityItem(String type, String product, String time, Color color) {
        JPanel item = new JPanel(new BorderLayout(10, 0));
        item.setOpaque(false);
        item.setBorder(new EmptyBorder(10, 0, 10, 0));
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        JPanel dotWrap = new JPanel(new GridBagLayout());
        dotWrap.setOpaque(false);
        dotWrap.setPreferredSize(new Dimension(22, 58));
        dotWrap.setMinimumSize(new Dimension(22, 58));
        dotWrap.setMaximumSize(new Dimension(22, 58));

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 14));
        dot.setForeground(color);

        dotWrap.add(dot);

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
        textBox.setBorder(new EmptyBorder(0, 4, 0, 0));

        JLabel lblType = new JLabel(type);
        lblType.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblType.setForeground(NAVY);
        lblType.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblProduct = new JLabel(
                "<html><div style='width:190px;'>"
                + escapeHtml(product)
                + "</div></html>"
        );
        lblProduct.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblProduct.setForeground(MUTED);
        lblProduct.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblTime = new JLabel(time);
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTime.setForeground(SOFT_MUTED);
        lblTime.setAlignmentX(Component.LEFT_ALIGNMENT);

        textBox.add(lblType);
        textBox.add(Box.createVerticalStrut(3));
        textBox.add(lblProduct);
        textBox.add(Box.createVerticalStrut(3));
        textBox.add(lblTime);

        item.add(dotWrap, BorderLayout.WEST);
        item.add(textBox, BorderLayout.CENTER);

        return item;
    }

    private void refreshRecentActivities() {
        recentActivityPanel.removeAll();

        try {
            String storeId = currentInventoryStoreId();

            List<InventoryTransactionSql.InventoryTransactionDTO> list
                    = InventoryTransactionSql.getInstance().getRecentTransactionsByStore(storeId, 3);

            if (list == null || list.isEmpty()) {
                recentActivityPanel.add(createActivityItem(
                        "Chưa có dữ liệu",
                        "Hãy nhập kho, nhập CSV hoặc xuất/hủy để phát sinh lịch sử.",
                        storeId == null ? "Chưa xác định chi nhánh" : "Chi nhánh: " + storeId,
                        ORANGE
                ));
            } else {
                for (int i = 0; i < list.size(); i++) {
                    InventoryTransactionSql.InventoryTransactionDTO x = list.get(i);

                    boolean inbound = "INBOUND".equalsIgnoreCase(x.transactionType);

                    recentActivityPanel.add(createActivityItem(
                            inbound ? "Nhập kho" : "Xuất / Hủy",
                            safe(x.productName, x.productId) + " • SL: " + x.quantity,
                            x.receiptId == null || x.receiptId.trim().isEmpty()
                            ? "Chi nhánh: " + safe(x.storeId, storeId == null ? "—" : storeId)
                            : "Phiếu: " + x.receiptId,
                            inbound ? GREEN : RED
                    ));

                    if (i < list.size() - 1) {
                        JSeparator sep = new JSeparator();
                        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                        sep.setForeground(new Color(225, 225, 225));

                        JPanel sepWrap = new JPanel(new BorderLayout());
                        sepWrap.setOpaque(false);
                        sepWrap.setBorder(new EmptyBorder(4, 0, 4, 0));
                        sepWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 10));
                        sepWrap.add(sep, BorderLayout.CENTER);

                        recentActivityPanel.add(sepWrap);
                    }
                }
            }

        } catch (Exception e) {
            recentActivityPanel.add(createActivityItem(
                    "Chưa có bảng lịch sử",
                    "Kiểm tra INVENTORY_TRANSACTIONS trong database.",
                    "Database chưa sẵn sàng",
                    RED
            ));
        }

        recentActivityPanel.revalidate();
        recentActivityPanel.repaint();
    }

    private void initEvents() {
        if (btnImportCsv != null) {
            btnImportCsv.addActionListener(e -> handleImportCSV());
        }

        btnInbound.addActionListener(e -> openPurchaseReceiptDialog());

        btnAuditLog.addActionListener(e -> {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
            String storeId = currentInventoryStoreId();
            new InventoryHistoryDialog(owner, storeId).setVisible(true);
        });

        btnSearch.addActionListener(e -> applySearchFilter());

        btnResetSearch.addActionListener(e -> {
            txtSearch.setText("");
            fillInventoryTable(cachedInventory);
        });

        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                applySearchFilter();
            }
        });

        /*
         * Không còn cbStoreFilter trên UI nên không cần listener lọc kho nữa.
         * Tất cả dữ liệu tồn kho và biến động kho lấy theo chi nhánh hiện tại.
         */
    }

    private void applyInventoryRolePermission() {
        boolean canManageStock = AuthorizationService.canManageStock();

        if (btnImportCsv != null) {
            btnImportCsv.setVisible(canManageStock);
            btnImportCsv.setEnabled(canManageStock);
        }

        if (btnInbound != null) {
            btnInbound.setVisible(canManageStock);
            btnInbound.setEnabled(canManageStock);
        }

        if (btnAuditLog != null) {
            btnAuditLog.setVisible(canManageStock);
            btnAuditLog.setEnabled(canManageStock);
        }

        if (!canManageStock) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền truy cập Quản lý tồn kho.",
                    "Không có quyền",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void openPurchaseReceiptDialog() {
        if (!AuthorizationService.canManageStock()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền nhập kho. Chức năng này dành cho Staff Product hoặc Admin.",
                    "Không có quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int row = tblInventory.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn sản phẩm cần nhập kho!",
                    "Chưa chọn sản phẩm",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int modelRow = tblInventory.convertRowIndexToModel(row);

        String productId = String.valueOf(tableModel.getValueAt(modelRow, 1));
        String selectedRowStoreId = normalizeStoreId(String.valueOf(tableModel.getValueAt(modelRow, 6)));

        String storeIdForAction = currentInventoryStoreId();

        if (storeIdForAction == null || storeIdForAction.isBlank()) {
            storeIdForAction = selectedRowStoreId;
        }

        if (storeIdForAction == null || storeIdForAction.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không xác định được chi nhánh để nhập kho.\n"
                    + "Vui lòng đăng nhập bằng tài khoản đã được phân chi nhánh.",
                    "Thiếu chi nhánh",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);

        String finalStoreIdForAction = storeIdForAction;

        StockImportReceiptDialog dialog = new StockImportReceiptDialog(
                owner,
                productId,
                () -> {
                    try {
                        InventoryNotificationSql.getInstance()
                                .resolveByProductIdAndStore(productId, finalStoreIdForAction);
                    } catch (Exception ignored) {
                    }

                    loadInventoryData();
                    refreshRecentActivities();
                }
        );

        dialog.setVisible(true);
    }

    private void applySearchFilter() {
        String keyword = txtSearch.getText() == null
                ? ""
                : txtSearch.getText().trim().toLowerCase();

        String currentStoreId = currentInventoryStoreId();

        List<Product> result = new ArrayList<>();

        for (Product p : cachedInventory) {
            String id = safe(p.getProductId(), "").toLowerCase();
            String name = safe(p.getProductName(), "").toLowerCase();
            String storeId = safe(p.getStoreId(), "Chưa xác định");
            String normalizedProductStoreId = normalizeStoreId(storeId);

            boolean matchKeyword = keyword.isEmpty()
                    || id.contains(keyword)
                    || name.contains(keyword);

            boolean matchStore = currentStoreId == null
                    || (normalizedProductStoreId != null
                    && currentStoreId.equalsIgnoreCase(normalizedProductStoreId));

            if (matchKeyword && matchStore) {
                result.add(p);
            }
        }

        fillInventoryTable(result);
        updateKpi(result);
        refreshAlertZone(result);
    }

    private void handleStockAdjustment(boolean isInbound) {
        if (!AuthorizationService.canManageStock()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền điều chỉnh kho. Chức năng này dành cho Staff Product hoặc Admin.",
                    "Không có quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int row = tblInventory.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn một mặt hàng từ bảng để thao tác!",
                    "Nhắc nhở",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int modelRow = tblInventory.convertRowIndexToModel(row);

        String prodId = String.valueOf(tableModel.getValueAt(modelRow, 1));
        String prodName = String.valueOf(tableModel.getValueAt(modelRow, 2));
        int currentQty = parseInt(tableModel.getValueAt(modelRow, 3));

        String actionName = isInbound ? "NHẬP KHO" : "XUẤT / HỦY KHO";

        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 10));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel lblProduct = new JLabel("Sản phẩm: " + prodName + " (" + prodId + ")");
        JLabel lblCurrent = new JLabel("Tồn kho hiện tại: " + currentQty);

        JTextField txtQty = new JTextField();
        JTextField txtReason = new JTextField();

        panel.add(lblProduct);
        panel.add(lblCurrent);
        panel.add(new JLabel(isInbound ? "Nhập số lượng cần cộng thêm:" : "Nhập số lượng trừ đi:"));
        panel.add(txtQty);
        panel.add(new JLabel("Lý do / ghi chú:"));
        panel.add(txtReason);

        int result = JOptionPane.showConfirmDialog(
                this,
                panel,
                actionName,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        try {
            int adjustQty = Integer.parseInt(txtQty.getText().trim());
            String reason = txtReason.getText().trim();

            if (adjustQty <= 0) {
                JOptionPane.showMessageDialog(this, "Số lượng phải lớn hơn 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (reason.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng ghi rõ lý do để lưu vào nhật ký!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (adjustQty > currentQty) {
                JOptionPane.showMessageDialog(this, "Số lượng xuất/hủy không được lớn hơn tồn kho hiện tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean success = InventoryTransactionSql.getInstance()
                    .createOutboundTransaction(prodId, adjustQty, reason);

            if (!success) {
                JOptionPane.showMessageDialog(
                        this,
                        "Cập nhật kho thất bại. Vui lòng kiểm tra database hoặc Output Console.",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            SyncVersionDao.bumpVersion("INVENTORY");
            SyncVersionDao.bumpVersion("PRODUCTS");

            RealtimeClient.send("INVENTORY_CHANGED");
            RealtimeClient.send("PRODUCTS_CHANGED");

            JOptionPane.showMessageDialog(
                    this,
                    "Cập nhật kho thành công!\n"
                    + "Thao tác: " + actionName + "\n"
                    + "Sản phẩm: " + prodName + "\n"
                    + "Số lượng: " + adjustQty + "\n"
                    + "Lý do: " + reason,
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE
            );

            loadInventoryData();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số lượng nhập vào không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadInventoryData() {
        cachedInventory.clear();

        try {
            String currentStoreId = currentInventoryStoreId();
            List<Product> list;

            if (currentStoreId == null || currentStoreId.isBlank()) {
                /*
                 * Admin không có store trong session thì vẫn xem toàn bộ.
                 * Với Warehouse/Staff/Manager có chi nhánh thì luôn scope theo chi nhánh.
                 */
                if (!business.service.SessionManager.isAdmin()) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Không xác định được chi nhánh hiện tại. Vui lòng đăng nhập lại bằng tài khoản đã được phân chi nhánh.",
                            "Thiếu chi nhánh",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                list = ProductsSql.getInstance().selectAll();
            } else {
                list = ProductsSql.getInstance().selectAllByStore(currentStoreId);
            }

            cachedInventory.addAll(list);

            rebuildStoreFilter(cachedInventory);

            applySearchFilter();
            refreshRecentActivities();

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi khi tải dữ liệu tồn kho!\n" + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void fillInventoryTable(List<Product> list) {
        tableModel.setRowCount(0);

        if (list == null) {
            return;
        }

        for (Product p : list) {
            int qty = p.getQuantity();

            String status;
            String threshold;

            if (qty <= 0) {
                status = "Hết hàng";
                threshold = "Khẩn cấp";
            } else if (qty <= 10) {
                status = "Nguy hiểm";
                threshold = "≤ 10";
            } else if (qty <= 30) {
                status = "Sắp hết";
                threshold = "≤ 30";
            } else {
                status = "Ổn định";
                threshold = "> 30";
            }

            ImageIcon thumb = loadInventoryThumb(p.getImagePath());
            tableModel.addRow(new Object[]{
                thumb,
                safe(p.getProductId(), ""),
                safe(p.getProductName(), ""),
                qty,
                threshold,
                safe(p.getUnit(), "Cái"),
                safe(p.getStoreId(), "Chưa xác định"),
                status,
                formatLastUpdated(p.getLastUpdated())
            });
        }
    }

    private String getCurrentStoreIdOrNull() {
        try {
            return normalizeStoreId(business.service.SessionManager.getCurrentStoreId());
        } catch (Exception e) {
            return null;
        }
    }

    private void rebuildStoreFilter(List<Product> list) {
        updatingStoreFilter = true;

        try {
            if (cbStoreFilter == null) {
                cbStoreFilter = new JComboBox<>();
            }

            cbStoreFilter.removeAllItems();
            cbStoreFilter.addItem(buildCurrentBranchValueForHiddenFilter());
            cbStoreFilter.setSelectedIndex(0);
            cbStoreFilter.setEnabled(false);

        } finally {
            updatingStoreFilter = false;
        }
    }

    private void updateKpi(List<Product> list) {
        int totalItems = 0;
        int lowStock = 0;
        int outOfStock = 0;
        int totalQuantity = 0;

        if (list != null) {
            for (Product p : list) {
                int qty = p.getQuantity();

                totalItems++;
                totalQuantity += qty;

                if (qty <= 0) {
                    outOfStock++;
                } else if (qty <= 30) {
                    lowStock++;
                }
            }
        }

        lblTotalItems.setText(String.valueOf(totalItems));
        lblLowStock.setText(String.valueOf(lowStock));
        lblOutOfStock.setText(String.valueOf(outOfStock));
        lblTotalQuantity.setText(String.valueOf(totalQuantity));
    }

    private void refreshAlertZone(List<Product> list) {
        alertListPanel.removeAll();

        int count = 0;

        if (list != null) {
            for (Product p : list) {
                if (p.getQuantity() <= 30) {
                    alertListPanel.add(createAlertItem(p));
                    count++;

                    if (count >= 8) {
                        break;
                    }
                }
            }
        }

        if (count == 0) {
            JLabel empty = new JLabel("Không có cảnh báo tồn kho cần xử lý.");
            empty.setFont(new Font("Segoe UI", Font.BOLD, 13));
            empty.setForeground(MUTED);
            alertListPanel.add(empty);
        }

        alertListPanel.revalidate();
        alertListPanel.repaint();
    }

    private void handleImportCSV() {
        if (!AuthorizationService.canManageStock()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền nhập CSV kho. Chức năng này dành cho Staff Product hoặc Admin.",
                    "Không có quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String selectedStoreId = currentInventoryStoreId();

        if (selectedStoreId == null || selectedStoreId.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không xác định được chi nhánh hiện tại để nhập CSV.\n"
                    + "Vui lòng đăng nhập bằng tài khoản đã được phân chi nhánh.",
                    "Thiếu chi nhánh nhập kho",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Chọn file CSV để nhập sản phẩm/tồn kho");
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));

        File defaultFile = new File("data/products1_1m.csv");
        if (defaultFile.exists()) {
            fileChooser.setSelectedFile(defaultFile);
        }

        int result = fileChooser.showOpenDialog(this);

        if (result != JFileChooser.APPROVE_OPTION) {
            return;
        }

        File file = fileChooser.getSelectedFile();

        if (file == null || !file.exists()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không tìm thấy file CSV.",
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        JDialog dialog = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Đang nhập CSV",
                true
        );

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(22, 26, 22, 26));
        content.setBackground(Color.WHITE);

        JLabel lblStatus = new JLabel("Đang nhập dữ liệu từ file: " + file.getName());
        lblStatus.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblStatus.setForeground(NAVY);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        JProgressBar progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setPreferredSize(new Dimension(360, 28));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(lblStatus);
        content.add(Box.createVerticalStrut(16));
        content.add(progressBar);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(this);

        final ProductImportService.ImportResult[] importResult = new ProductImportService.ImportResult[1];

        CompletableFuture.runAsync(() -> {
            importResult[0] = new ProductImportService().importProductCSVWithReceipt(
                    file.getAbsolutePath(),
                    progress -> SwingUtilities.invokeLater(() -> progressBar.setValue(progress))
            );
        }).thenRun(() -> {
            SwingUtilities.invokeLater(() -> {
                dialog.dispose();

                try {
                    SyncVersionDao.bumpVersion("PRODUCTS");
                    SyncVersionDao.bumpVersion("INVENTORY");

                    RealtimeClient.send("PRODUCTS_CHANGED");
                    RealtimeClient.send("INVENTORY_CHANGED");
                } catch (Exception ignored) {
                }

                loadInventoryData();

                ProductImportService.ImportResult rs = importResult[0];

                if (rs == null) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Import CSV hoàn tất nhưng không nhận được kết quả phiếu nhập.",
                            "Thông báo",
                            JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Nhập CSV thành công!\n"
                        + "Mã phiếu nhập: " + rs.receiptId + "\n"
                        + "Số dòng thành công: " + rs.successRows + "\n"
                        + "Số dòng bỏ qua: " + rs.skippedRows + "\n"
                        + "Tổng trước thuế: " + formatMoney(rs.totalBeforeTax) + " VNĐ\n"
                        + "Tổng VAT: " + formatMoney(rs.totalTax) + " VNĐ\n"
                        + "Tổng sau thuế: " + formatMoney(rs.totalAfterTax) + " VNĐ",
                        "Import CSV hoàn tất",
                        JOptionPane.INFORMATION_MESSAGE
                );

                Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
                new PurchaseReceiptInvoiceDialog(owner, rs.receiptId).setVisible(true);
            });
        }).exceptionally(ex -> {
            SwingUtilities.invokeLater(() -> {
                dialog.dispose();

                JOptionPane.showMessageDialog(
                        this,
                        "Lỗi nhập CSV:\n" + getRootMessage(ex),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
            });
            return null;
        });

        dialog.setVisible(true);
    }

    private JButton createButton(String text, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(text);

        if (icon != null) {
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH)));
            btn.setIconTextGap(7);
        }

        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);

        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 14, 14);

                super.paint(g2, c);
                g2.dispose();
            }
        });

        return btn;
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private int parseInt(Object value) {
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    private String shortText(String text, int max) {
        if (text == null) {
            return "";
        }

        if (text.length() <= max) {
            return text;
        }

        return text.substring(0, max - 3) + "...";
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String formatMoney(BigDecimal value) {
        if (value == null) {
            return "0";
        }

        return String.format("%,.0f", value);
    }

    private String getRootMessage(Throwable ex) {
        Throwable t = ex;

        while (t.getCause() != null) {
            t = t.getCause();
        }

        return t.getMessage() == null ? t.toString() : t.getMessage();
    }

    class RoundedPanel extends JPanel {

        private final int radius;
        Color bgColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    class RoundBorder implements javax.swing.border.Border {

        private final Color color;
        private final int radius;

        public RoundBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    public void focusProduct(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return;
        }

        String targetId = productId.trim();

        SwingUtilities.invokeLater(() -> {
            try {
                if (txtSearch != null) {
                    txtSearch.setText("");
                }

                loadInventoryData();

                boolean found = selectProductInTable(targetId);

                if (!found) {
                    if (txtSearch != null) {
                        txtSearch.setText(targetId);
                        applySearchFilter();
                    }

                    if (tableModel.getRowCount() > 0) {
                        tblInventory.setRowSelectionInterval(0, 0);
                        tblInventory.scrollRectToVisible(tblInventory.getCellRect(0, 0, true));
                        tblInventory.requestFocusInWindow();
                    }
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private boolean selectProductInTable(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }

        String targetId = productId.trim();

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Object value = tableModel.getValueAt(i, 1);

            if (targetId.equalsIgnoreCase(String.valueOf(value))) {
                int viewRow = tblInventory.convertRowIndexToView(i);

                tblInventory.setRowSelectionInterval(viewRow, viewRow);
                tblInventory.scrollRectToVisible(tblInventory.getCellRect(viewRow, 1, true));
                tblInventory.requestFocusInWindow();

                return true;
            }
        }

        return false;
    }

    private String formatLastUpdated(Timestamp time) {
        if (time == null) {
            return "Chưa cập nhật";
        }

        return new SimpleDateFormat("dd/MM/yyyy HH:mm").format(time);
    }

    private String currentInventoryStoreId() {
        String currentStoreId = getCurrentStoreIdOrNull();

        if (currentStoreId != null && !currentStoreId.isBlank()) {
            return currentStoreId;
        }

        if (business.service.SessionManager.isAdmin()) {
            return null;
        }

        return normalizeStoreId(business.service.SessionManager.requireCurrentStoreId());
    }

    private String normalizeStoreId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String text = value.trim();

        if ("Tất cả chi nhánh".equalsIgnoreCase(text)
                || "Chưa xác định".equalsIgnoreCase(text)
                || "null".equalsIgnoreCase(text)) {
            return null;
        }

        if (text.contains(" - ")) {
            return text.substring(0, text.indexOf(" - ")).trim();
        }

        return text;
    }

    private ImageIcon loadInventoryThumb(String imagePath) {
        return view.components.ProductImageLoader.load(imagePath, 55, 45);
    }
}
