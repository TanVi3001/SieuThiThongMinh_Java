package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import business.sql.prod_inventory.ProductsSql;
import model.product.Product;
import javax.swing.ListSelectionModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collections;
import java.util.Objects;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import view.components.IconHelper;
import business.sql.prod_inventory.CategoriesSql;
import business.sql.prod_inventory.StoresSql;
import model.product.Category;
import model.product.Store;
import view.JDateChooser;

public class PromotionManagementPanel extends JPanel {

    private static final String STATUS_ALL = "Tất cả";
    private static final String STATUS_ACTIVE = "Đang diễn ra";
    private static final String STATUS_UPCOMING = "Sắp diễn ra";
    private static final String STATUS_ENDED = "Đã kết thúc";
    private static final String STATUS_PAUSED = "Tạm ngưng";

    private static final String BRANCH_ALL = "Tất cả chi nhánh";

    private final Color bg = new Color(244, 246, 250);
    private final Color white = Color.WHITE;
    private final Color text = new Color(36, 47, 74);
    private final Color muted = new Color(143, 154, 179);
    private final Color border = new Color(226, 232, 240);
    private final Color blue = new Color(37, 99, 235);
    private final Color green = new Color(16, 185, 129);
    private final Color red = new Color(239, 68, 68);
    private final Color orange = new Color(245, 158, 11);
    private final Color grayBtn = new Color(148, 163, 184);
    private final Color softBlue = new Color(237, 242, 255);
    private final Color softGreen = new Color(236, 253, 245);
    private final Color softOrange = new Color(255, 247, 237);
    private final Color softRed = new Color(254, 242, 242);

    private JTable tblPromos;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JComboBox<String> cbFilterStatus;
    private JLabel lblTotalPromos, lblActivePromos, lblEndedPromos, lblPausedPromos;

    private JTextField txtMaKM;
    private JTextField txtTenKM;
    private JSpinner spinGiamGia;
    private JDateChooser dtpTuNgay;
    private JDateChooser dtpDenNgay;
    private JComboBox<String> cbChiNhanh;
    private JComboBox<ProductOption> cbProductSearch;
    private DefaultComboBoxModel<ProductOption> productComboModel;

    private JTable tblPromoProducts;
    private DefaultTableModel promoProductModel;

    private java.util.List<ProductOption> allProductOptions = new java.util.ArrayList<>();
    private boolean updatingProductCombo = false;
    private Timer productSearchFilterTimer;
    private JSpinner spinMinOrderAmount;
    private JComboBox<String> cbTrangThai;
    private JButton btnSave, btnClear, btnDeactivate, btnPreview;
    private JLabel lblFormTitle;
    private JLabel lblFormHint;

    private boolean isEditMode = false;

    private static class ProductOption {

        String productId;
        String productName;

        ProductOption(String productId, String productName) {
            this.productId = productId;
            this.productName = productName;
        }

        @Override
        public String toString() {
            return productId + " - " + productName;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (!(obj instanceof ProductOption other)) {
                return false;
            }

            return Objects.equals(productId, other.productId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(productId);
        }
    }

    private static class PromotionProductOption {

        String productId;
        String productName;
        double discountPercent;

        PromotionProductOption(String productId, String productName, double discountPercent) {
            this.productId = productId;
            this.productName = productName;
            this.discountPercent = discountPercent;
        }
    }

    public PromotionManagementPanel() {
        setLayout(new BorderLayout(0, 22));
        setBackground(bg);
        setBorder(new EmptyBorder(22, 30, 22, 30));
        initUI();
        initEvents();
        loadPromoData("", STATUS_ALL);
    }

    private void initUI() {
        add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(22, 0));
        body.setOpaque(false);
        body.add(createMainPanel(), BorderLayout.CENTER);
        body.add(createRightPanel(), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Quản Lý Chiến Dịch Khuyến Mãi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(text);

        JLabel sub = new JLabel("Thiết lập mã giảm giá, cấu hình phạm vi áp dụng và đối tượng khách hàng");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(muted);

        panel.add(title);
        panel.add(Box.createVerticalStrut(5));
        panel.add(sub);
        return panel;
    }

    private JPanel createMainPanel() {
        RoundedPanel card = new RoundedPanel(20, white);
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(new EmptyBorder(22, 22, 18, 22));
        card.add(createStatsPanel(), BorderLayout.NORTH);
        card.add(createTableArea(), BorderLayout.CENTER);
        return card;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 14, 0));
        panel.setOpaque(false);

        lblTotalPromos = new JLabel("0");
        lblActivePromos = new JLabel("0");
        lblEndedPromos = new JLabel("0");
        lblPausedPromos = new JLabel("0");

        panel.add(createStatCard("Tổng chương trình", lblTotalPromos, blue));
        panel.add(createStatCard("Đang diễn ra", lblActivePromos, green));
        panel.add(createStatCard("Đã kết thúc", lblEndedPromos, orange));
        panel.add(createStatCard("Tạm ngưng", lblPausedPromos, red));
        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        RoundedPanel card = new RoundedPanel(16, white);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JPanel iconBox = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 24));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(46, 46));

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 22));
        dot.setForeground(accentColor);
        iconBox.add(dot);

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(muted);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(text);

        textBox.add(titleLabel);
        textBox.add(Box.createVerticalStrut(5));
        textBox.add(valueLabel);

        card.add(iconBox, BorderLayout.WEST);
        card.add(textBox, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTableArea() {
        JPanel area = new JPanel(new BorderLayout(0, 14));
        area.setOpaque(false);

        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setOpaque(false);

        JLabel title = new JLabel("Danh sách khuyến mãi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(text);

        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        search.setOpaque(false);

        cbFilterStatus = new JComboBox<>(new String[]{STATUS_ALL, STATUS_ACTIVE, STATUS_UPCOMING, STATUS_ENDED, STATUS_PAUSED});
        cbFilterStatus.setPreferredSize(new Dimension(160, 40));
        cbFilterStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        txtSearch = createTextField("Tra mã KM, tên CT...");
        txtSearch.setPreferredSize(new Dimension(245, 40));

        JButton btnSearch = createButton("Tìm", blue, Color.WHITE, IconHelper.search(16));
        btnSearch.setPreferredSize(new Dimension(92, 40));
        btnSearch.addActionListener(e -> doSearch());

        search.add(cbFilterStatus);
        search.add(txtSearch);
        search.add(btnSearch);
        bar.add(title, BorderLayout.WEST);
        bar.add(search, BorderLayout.EAST);

        tableModel = new DefaultTableModel(new Object[]{"Mã KM", "Tên Chương Trình", "Từ Ngày", "Đến Ngày", "Trạng Thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblPromos = new JTable(tableModel);
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblPromos);
        scrollPane.setBorder(BorderFactory.createLineBorder(border));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JLabel hint = new JLabel("Gợi ý: Bấm Thêm để tạo khuyến mãi mới, hoặc click một dòng để chỉnh sửa / tạm ngưng.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(muted);

        area.add(bar, BorderLayout.NORTH);
        area.add(scrollPane, BorderLayout.CENTER);
        area.add(hint, BorderLayout.SOUTH);
        return area;
    }

    private JPanel createRightPanel() {
        RoundedPanel card = new RoundedPanel(20, white);
        card.setPreferredSize(new Dimension(470, 0));
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(new EmptyBorder(24, 24, 22, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        lblFormTitle = new JLabel("Cấu Hình Khuyến Mãi");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 21));
        lblFormTitle.setForeground(text);

        lblFormHint = new JLabel("Bấm Thêm để nhập chương trình mới");
        lblFormHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblFormHint.setForeground(muted);

        header.add(lblFormTitle);
        header.add(Box.createVerticalStrut(6));
        header.add(lblFormHint);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        int y = 0;
        JPanel basicRow = new JPanel(new GridLayout(1, 2, 10, 0));
        basicRow.setOpaque(false);
        JPanel maPanel = fieldPanel("Mã KM", txtMaKM = createTextField("VD: TET2026"));
        JPanel discountPanel = new JPanel(new BorderLayout(0, 7));
        discountPanel.setOpaque(false);
        discountPanel.add(createFormLabel("Mức giảm (%)"), BorderLayout.NORTH);
        spinGiamGia = new JSpinner(new SpinnerNumberModel(5, 0, 100, 1));
        spinGiamGia.setFont(new Font("Segoe UI", Font.BOLD, 14));
        spinGiamGia.setPreferredSize(new Dimension(0, 40));
        discountPanel.add(spinGiamGia, BorderLayout.CENTER);
        basicRow.add(maPanel);
        basicRow.add(discountPanel);

        addSection(form, g, y++, "1. Thông tin cơ bản");
        addComponent(form, g, y++, basicRow, 14);
        addField(form, g, y, "Tên chương trình", txtTenKM = createTextField("Nhập tên sự kiện..."));
        y += 2;

        addSection(form, g, y++, "2. Thời gian áp dụng");
        JPanel timeRow = new JPanel(new GridLayout(1, 2, 10, 0));
        timeRow.setOpaque(false);

        dtpTuNgay = new JDateChooser();
        dtpTuNgay.setDateFormatString("yyyy-MM-dd");
        dtpTuNgay.setPreferredSize(new Dimension(0, 40));

        dtpDenNgay = new JDateChooser();
        dtpDenNgay.setDateFormatString("yyyy-MM-dd");
        dtpDenNgay.setPreferredSize(new Dimension(0, 40));

        timeRow.add(fieldPanel("Từ ngày", dtpTuNgay));
        timeRow.add(fieldPanel("Đến ngày", dtpDenNgay));
        addComponent(form, g, y++, timeRow, 14);

        addSection(form, g, y++, "3. Phạm vi áp dụng");

        // Lấy danh sách tất cả các chi nhánh động hiện có thông qua StoresSql
        cbChiNhanh = new JComboBox<>();
        cbChiNhanh.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbChiNhanh.setPreferredSize(new Dimension(0, 40));
        cbChiNhanh.addItem("Tất cả các chi nhánh");
        for (Store store : StoresSql.getInstance().selectAll()) {
            cbChiNhanh.addItem(store.getStoreId() + " - " + store.getAddress());
        }
        addField(form, g, y, "Áp dụng tại chi nhánh", cbChiNhanh);
        y += 2;

        // Lấy danh sách Loại sản phẩm động hiện có thông qua CategoriesSql
        spinMinOrderAmount = new JSpinner(new SpinnerNumberModel(100000, 0, 999999999, 10000));
        spinMinOrderAmount.setFont(new Font("Segoe UI", Font.BOLD, 13));
        spinMinOrderAmount.setPreferredSize(new Dimension(0, 40));
        addField(form, g, y, "Đơn tối thiểu để áp dụng", spinMinOrderAmount);
        y += 2;

        loadProductOptionsForAutocomplete();

        cbProductSearch = new JComboBox<>(productComboModel);
        cbProductSearch.setEditable(true);
        cbProductSearch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbProductSearch.setPreferredSize(new Dimension(0, 38));
        cbProductSearch.setMaximumRowCount(8);
        cbProductSearch.setPrototypeDisplayValue(new ProductOption("SP000000", "Tên sản phẩm mẫu để canh chiều rộng"));

        cbProductSearch.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus
                );

                label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                label.setBorder(new EmptyBorder(6, 8, 6, 8));
                label.setText(value == null ? "" : value.toString());
                return label;
            }
        });

        JTextField productEditor = (JTextField) cbProductSearch.getEditor().getEditorComponent();
        productEditor.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        productEditor.putClientProperty("JTextField.placeholderText", "Nhập mã hoặc tên sản phẩm...");

        productSearchFilterTimer = new Timer(180, e -> filterProductCombo());
        productSearchFilterTimer.setRepeats(false);

        productEditor.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleProductComboFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleProductComboFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                scheduleProductComboFilter();
            }
        });

        productEditor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    ProductOption opt = resolveProductFromSearchInput();

                    if (opt != null) {
                        addProductToPromotionList(opt);
                        clearProductSearchEditor();
                        e.consume();
                    }
                }
            }
        });

        JButton btnAddProduct = createButton("Thêm SP", blue, Color.WHITE, null);
        btnAddProduct.setPreferredSize(new Dimension(95, 38));
        btnAddProduct.addActionListener(e -> {
            ProductOption opt = resolveProductFromSearchInput();

            if (opt != null) {
                addProductToPromotionList(opt);
                clearProductSearchEditor();
                productEditor.requestFocusInWindow();
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Vui lòng nhập/chọn sản phẩm cần áp dụng.",
                        "Chưa chọn sản phẩm",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        });

        JPanel productSearchPanel = new JPanel(new BorderLayout(8, 0));
        productSearchPanel.setOpaque(false);
        productSearchPanel.add(cbProductSearch, BorderLayout.CENTER);
        productSearchPanel.add(btnAddProduct, BorderLayout.EAST);

        promoProductModel = new DefaultTableModel(
                new Object[]{"Mã SP", "Tên sản phẩm", "Giảm (%)"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 2;
            }
        };

        tblPromoProducts = new JTable(promoProductModel);
        tblPromoProducts.setRowHeight(28);
        tblPromoProducts.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tblPromoProducts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblPromoProducts.setShowHorizontalLines(true);
        tblPromoProducts.setShowVerticalLines(false);
        tblPromoProducts.setGridColor(border);
        tblPromoProducts.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        if (tblPromoProducts.getTableHeader() != null) {
            tblPromoProducts.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
            tblPromoProducts.getTableHeader().setBackground(new Color(243, 246, 250));
        }

        tblPromoProducts.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblPromoProducts.getColumnModel().getColumn(1).setPreferredWidth(220);
        tblPromoProducts.getColumnModel().getColumn(2).setPreferredWidth(70);

        tblPromoProducts.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    removeSelectedPromotionProduct();
                }
            }
        });

        tblPromoProducts.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    removeSelectedPromotionProduct();
                }
            }
        });

        JScrollPane selectedProductScroll = new JScrollPane(tblPromoProducts);
        selectedProductScroll.setPreferredSize(new Dimension(0, 130));
        selectedProductScroll.setMinimumSize(new Dimension(0, 110));
        selectedProductScroll.getVerticalScrollBar().setUnitIncrement(16);

        JLabel selectedHint = new JLabel("Sửa % từng dòng. Double click hoặc Delete để xoá sản phẩm.");
        selectedHint.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        selectedHint.setForeground(muted);

        JPanel productApplyPanel = new JPanel();
        productApplyPanel.setOpaque(false);
        productApplyPanel.setLayout(new BoxLayout(productApplyPanel, BoxLayout.Y_AXIS));
        productApplyPanel.add(productSearchPanel);
        productApplyPanel.add(Box.createVerticalStrut(6));
        productApplyPanel.add(selectedProductScroll);
        productApplyPanel.add(Box.createVerticalStrut(4));
        productApplyPanel.add(selectedHint);

        addField(form, g, y, "Áp dụng cho sản phẩm", productApplyPanel);
        y += 2;

        addSection(form, g, y++, "4. Trạng thái chương trình");
        cbTrangThai = combo(new String[]{STATUS_ACTIVE, STATUS_UPCOMING, STATUS_ENDED, STATUS_PAUSED});
        addComponent(form, g, y++, cbTrangThai, 12);

        JPanel actions = new JPanel(new GridLayout(2, 2, 10, 10));
        actions.setOpaque(false);
        btnClear = createButton("Làm mới", new Color(235, 238, 244), text, IconHelper.refresh(18));
        btnPreview = createButton("Xem trước", softBlue, blue, null);
        btnDeactivate = createButton("Tạm ngưng", red, Color.WHITE, IconHelper.delete(18));
        btnSave = createButton("Lưu", blue, Color.WHITE, IconHelper.edit(18));
        actions.add(btnClear);
        actions.add(btnPreview);
        actions.add(btnDeactivate);
        actions.add(btnSave);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.setOpaque(false);
        formScroll.getViewport().setOpaque(false);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        formScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        formScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

        card.add(header, BorderLayout.NORTH);
        card.add(formScroll, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel fieldPanel(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setOpaque(false);
        panel.add(createFormLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private JComboBox<String> combo(String[] values) {
        JComboBox<String> cb = new JComboBox<>(values);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cb.setPreferredSize(new Dimension(0, 40));
        return cb;
    }

    private void addSection(JPanel form, GridBagConstraints g, int y, String textValue) {
        g.gridy = y;
        g.insets = new Insets(2, 0, 8, 0);
        form.add(createSectionTitle(textValue), g);
    }

    private void addField(JPanel form, GridBagConstraints g, int y, String label, JComponent field) {
        g.gridy = y;
        g.insets = new Insets(0, 0, 7, 0);
        form.add(createFormLabel(label), g);
        g.gridy = y + 1;
        g.insets = new Insets(0, 0, 14, 0);
        form.add(field, g);
    }

    private void addComponent(JPanel form, GridBagConstraints g, int y, JComponent component, int bottom) {
        g.gridy = y;
        g.insets = new Insets(0, 0, bottom, 0);
        form.add(component, g);
    }

    private void initEvents() {
        btnClear.addActionListener(e -> clearForm());
        btnPreview.addActionListener(e -> previewPromo());
        btnDeactivate.addActionListener(e -> deactivatePromo());
        btnSave.addActionListener(e -> savePromo());

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                doSearch();
            }

            public void removeUpdate(DocumentEvent e) {
                doSearch();
            }

            public void changedUpdate(DocumentEvent e) {
                doSearch();
            }
        });
        cbFilterStatus.addActionListener(e -> doSearch());

        tblPromos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblPromos.getSelectedRow();
                if (row >= 0) {
                    int modelRow = tblPromos.convertRowIndexToModel(row);
                    isEditMode = true;
                    String ma = tableModel.getValueAt(modelRow, 0).toString();
                    txtMaKM.setText(ma);
                    txtMaKM.setEnabled(false);
                    lblFormHint.setText("Đang cập nhật khuyến mãi " + ma);
                    loadPromoDetailsToForm(ma);
                }
            }
        });
    }

    private void doSearch() {
        loadPromoData(txtSearch.getText().trim(), cbFilterStatus.getSelectedItem().toString());
    }

    private void clearForm() {
        isEditMode = false;
        txtMaKM.setText("");
        txtMaKM.setEnabled(true);
        txtTenKM.setText("");
        spinGiamGia.setValue(5);
        dtpTuNgay.setDate(null);
        dtpDenNgay.setDate(null);
        cbChiNhanh.setSelectedIndex(0);
        cbTrangThai.setSelectedIndex(0);
        tblPromos.clearSelection();
        lblFormTitle.setText("Cấu Hình Khuyến Mãi");
        lblFormHint.setText("Bấm Thêm để nhập chương trình mới");
        if (spinMinOrderAmount != null) {
            spinMinOrderAmount.setValue(100000);
        }

        if (promoProductModel != null) {
            promoProductModel.setRowCount(0);
        }

        clearProductSearchEditor();
    }

    private String normalizeDbStatus(String rawStatus) {
        if (rawStatus == null || rawStatus.trim().isEmpty()) {
            return STATUS_ACTIVE;
        }

        String status = rawStatus.trim();

        if ("Tạm ngưng / Kết thúc".equals(status)) {
            return STATUS_PAUSED;
        }

        if (STATUS_ACTIVE.equals(status)
                || STATUS_UPCOMING.equals(status)
                || STATUS_ENDED.equals(status)
                || STATUS_PAUSED.equals(status)) {
            return status;
        }

        return STATUS_ACTIVE;
    }

    private String resolvePromotionStatusByDate(String rawStatus, String startDateText, String endDateText) {
        String normalized = normalizeDbStatus(rawStatus);

        // Chỉ Tạm ngưng là trạng thái admin tự set, luôn giữ nguyên.
        if (STATUS_PAUSED.equals(normalized)) {
            return STATUS_PAUSED;
        }

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate startDate = parseLocalDate(startDateText);
        java.time.LocalDate endDate = parseLocalDate(endDateText);

        // Nếu chưa có ngày thì fallback theo DB, tránh tự đoán sai.
        if (startDate == null || endDate == null) {
            return normalized;
        }

        if (today.isBefore(startDate)) {
            return STATUS_UPCOMING;      // hôm nay nằm bên trái khoảng
        }

        if (today.isAfter(endDate)) {
            return STATUS_ENDED;         // hôm nay nằm bên phải khoảng
        }

        return STATUS_ACTIVE;            // hôm nay nằm trong khoảng, tính cả ngày đầu/cuối
    }

    private java.time.LocalDate parseLocalDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return null;
        }

        try {
            return java.time.LocalDate.parse(dateText.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private int statusPriority(String status) {
        String normalized = normalizeDbStatus(status);

        if (STATUS_ACTIVE.equals(normalized)) {
            return 1;
        }

        if (STATUS_UPCOMING.equals(normalized)) {
            return 2;
        }

        if (STATUS_ENDED.equals(normalized)) {
            return 3;
        }

        if (STATUS_PAUSED.equals(normalized)) {
            return 4;
        }

        return 5;
    }

    private void loadPromoData(String keyword, String statusFilter) {
        tableModel.setRowCount(0);
        int total = 0, active = 0, ended = 0, paused = 0;

        class PromoRow {

            String ma;
            String ten;
            int giam;
            String tuNgay;
            String denNgay;
            String trangThai;
        }

        List<PromoRow> rows = new ArrayList<>();

        /*
         * FIX ORA-12704:
         * Không dùng NVL(p.status, 'chuỗi tiếng Việt') hoặc CASE p.status WHEN '...'
         * trong SQL nữa. Một số DB đang lưu STATUS/PROMOTION_NAME bằng charset khác
         * nên so sánh literal tiếng Việt trực tiếp trong SQL dễ lỗi character set mismatch.
         * Lọc trạng thái và sắp xếp ưu tiên được làm ở Java.
         */
        String sql = "SELECT p.promotion_id AS makm, "
                + "TO_CHAR(p.promotion_name) AS tenkm, "
                + "NVL(p.discount_percent, NVL(p.discount_amount, 0)) AS phantramgiam, "
                + "TO_CHAR(c.start_date, 'YYYY-MM-DD') AS tungay, "
                + "TO_CHAR(c.end_date, 'YYYY-MM-DD') AS denngay, "
                + "TO_CHAR(p.status) AS trangthai "
                + "FROM PROMOTIONS p "
                + "LEFT JOIN PROMOTION_CAMPAIGNS c ON p.campaign_id = c.campaign_id "
                + "WHERE NVL(p.is_deleted, 0) = 0 "
                + "AND (LOWER(TO_CHAR(p.promotion_id)) LIKE LOWER(?) "
                + "OR LOWER(TO_CHAR(p.promotion_name)) LIKE LOWER(?)) "
                + "ORDER BY c.start_date DESC NULLS LAST, p.promotion_id DESC";

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String trangThai = resolvePromotionStatusByDate(
                            rs.getString("trangthai"),
                            rs.getString("tungay"),
                            rs.getString("denngay")
                    );

                    if (!STATUS_ALL.equals(statusFilter) && !statusFilter.equals(trangThai)) {
                        continue;
                    }

                    PromoRow row = new PromoRow();
                    row.ma = rs.getString("makm");
                    row.ten = rs.getString("tenkm");
                    row.giam = rs.getInt("phantramgiam");
                    row.tuNgay = rs.getString("tungay");
                    row.denNgay = rs.getString("denngay");
                    row.trangThai = trangThai;
                    rows.add(row);
                }
            }

            rows.sort((a, b) -> {
                int byStatus = Integer.compare(statusPriority(a.trangThai), statusPriority(b.trangThai));
                if (byStatus != 0) {
                    return byStatus;
                }

                String aDate = a.tuNgay == null ? "" : a.tuNgay;
                String bDate = b.tuNgay == null ? "" : b.tuNgay;
                int byDateDesc = bDate.compareTo(aDate);
                if (byDateDesc != 0) {
                    return byDateDesc;
                }

                String aId = a.ma == null ? "" : a.ma;
                String bId = b.ma == null ? "" : b.ma;
                return bId.compareTo(aId);
            });

            for (PromoRow row : rows) {
                tableModel.addRow(new Object[]{
                    row.ma,
                    row.ten,
                    row.tuNgay,
                    row.denNgay,
                    row.trangThai
                });

                total++;
                if (STATUS_ACTIVE.equals(row.trangThai)) {
                    active++;
                } else if (STATUS_ENDED.equals(row.trangThai)) {
                    ended++;
                } else if (STATUS_PAUSED.equals(row.trangThai)) {
                    paused++;
                }
            }

            lblTotalPromos.setText(String.valueOf(total));
            lblActivePromos.setText(String.valueOf(active));
            lblEndedPromos.setText(String.valueOf(ended));
            lblPausedPromos.setText(String.valueOf(paused));

        } catch (Exception e) {
            System.err.println("Lỗi tải danh sách Khuyến mãi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadPromoDetailsToForm(String maKM) {
        String sql = "SELECT p.promotion_name, "
                + "NVL(p.discount_percent, NVL(p.discount_amount, 0)) AS discount_amount, "
                + "NVL(p.min_order_amount, 100000) AS min_order_amount, "
                + "TO_CHAR(c.start_date, 'YYYY-MM-DD') AS start_date, "
                + "TO_CHAR(c.end_date, 'YYYY-MM-DD') AS end_date, "
                + "TO_CHAR(p.status) AS status "
                + "FROM PROMOTIONS p "
                + "LEFT JOIN PROMOTION_CAMPAIGNS c ON p.campaign_id = c.campaign_id "
                + "WHERE p.promotion_id = ? AND NVL(p.is_deleted, 0) = 0";

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maKM);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtTenKM.setText(rs.getString("promotion_name"));
                    spinGiamGia.setValue(rs.getInt("discount_amount"));

                    if (spinMinOrderAmount != null) {
                        spinMinOrderAmount.setValue(rs.getInt("min_order_amount"));
                    }

                    cbTrangThai.setSelectedItem(resolvePromotionStatusByDate(
                            rs.getString("status"),
                            rs.getString("start_date"),
                            rs.getString("end_date")
                    ));

                    java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");

                    try {
                        String startDateStr = rs.getString("start_date");
                        if (startDateStr != null && !startDateStr.trim().isEmpty()) {
                            dtpTuNgay.setDate(sdf.parse(startDateStr));
                        } else {
                            dtpTuNgay.setDate(null);
                        }
                    } catch (Exception ex) {
                        dtpTuNgay.setDate(null);
                        System.err.println("Lỗi phân tích Từ ngày: " + ex.getMessage());
                    }

                    try {
                        String endDateStr = rs.getString("end_date");
                        if (endDateStr != null && !endDateStr.trim().isEmpty()) {
                            dtpDenNgay.setDate(sdf.parse(endDateStr));
                        } else {
                            dtpDenNgay.setDate(null);
                        }
                    } catch (Exception ex) {
                        dtpDenNgay.setDate(null);
                        System.err.println("Lỗi phân tích Đến ngày: " + ex.getMessage());
                    }

                    loadPromotionProductsSelection(maKM);
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải chi tiết Khuyến mãi: " + e.getMessage());
        }
    }

    /**
     * Chuẩn hóa trạng thái theo ngày áp dụng. - Nếu ngày bắt đầu > hôm nay:
     * không cho lưu "Đang diễn ra", tự chuyển thành "Sắp diễn ra". - Nếu ngày
     * kết thúc < hôm nay: không cho lưu "Đang diễn ra" hoặc "Sắp diễn ra", tự
     * chuyển "Đã kết thúc". - Nếu đang trong thời gian áp dụng mà chọn "Sắp
     * diễn ra": tự chuyển "Đang diễn ra". - "Tạm ngưng" và "Đã kết thúc" là
     * trạng thái chủ động nên được giữ lại.
     */
    private String normalizePromotionStatusByDate(String requestedStatus) {
        String status = requestedStatus == null || requestedStatus.trim().isEmpty()
                ? STATUS_ACTIVE
                : requestedStatus.trim();

        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate startDate = toLocalDate(dtpTuNgay.getDate());
        java.time.LocalDate endDate = toLocalDate(dtpDenNgay.getDate());

        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ngày kết thúc không được nhỏ hơn ngày bắt đầu.",
                    "Ngày áp dụng không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );
            return status;
        }

        if (STATUS_PAUSED.equals(status)) {
            return STATUS_PAUSED;
        }

        if (startDate != null && today.isBefore(startDate)) {
            return STATUS_UPCOMING;
        }

        if (endDate != null && today.isAfter(endDate)) {
            return STATUS_ENDED;
        }

        if (startDate != null && endDate != null
                && !today.isBefore(startDate)
                && !today.isAfter(endDate)) {
            return STATUS_ACTIVE;
        }

        return status;
    }

    private java.time.LocalDate toLocalDate(java.util.Date date) {
        if (date == null) {
            return null;
        }

        return date.toInstant()
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate();
    }

    private void savePromo() {
        String ma = txtMaKM.getText().trim();
        String ten = txtTenKM.getText().trim();
        int giam = (int) spinGiamGia.getValue();
        int minOrderAmount = ((Number) spinMinOrderAmount.getValue()).intValue();

        List<PromotionProductOption> selectedProducts = getPromotionSelectedProducts();

        if (selectedProducts == null || selectedProducts.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng thêm ít nhất 1 sản phẩm áp dụng khuyến mãi.",
                    "Thiếu sản phẩm áp dụng",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (giam <= 0 || giam > 30) {
            JOptionPane.showMessageDialog(
                    this,
                    "Mức giảm mặc định nên từ 1% đến 30% để tránh lỗ.",
                    "Mức giảm không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        for (PromotionProductOption p : selectedProducts) {
            if (p.discountPercent <= 0 || p.discountPercent > 30) {
                JOptionPane.showMessageDialog(
                        this,
                        "Mức giảm của sản phẩm " + p.productId + " phải từ 1% đến 30%.",
                        "Mức giảm sản phẩm không hợp lệ",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }
        }

        if (minOrderAmount < 100000) {
            JOptionPane.showMessageDialog(
                    this,
                    "Đơn tối thiểu nên từ 100.000đ trở lên để tránh khuyến mãi quá thấp gây lỗ.",
                    "Đơn tối thiểu chưa hợp lý",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String requestedTrangThai = cbTrangThai.getSelectedItem() == null
                ? STATUS_ACTIVE
                : cbTrangThai.getSelectedItem().toString();

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String tuNgay = "";
        String denNgay = "";

        if (dtpTuNgay.getDate() != null) {
            tuNgay = sdf.format(dtpTuNgay.getDate());
        }

        if (dtpDenNgay.getDate() != null) {
            denNgay = sdf.format(dtpDenNgay.getDate());
        }

        java.time.LocalDate startDateForValidate = toLocalDate(dtpTuNgay.getDate());
        java.time.LocalDate endDateForValidate = toLocalDate(dtpDenNgay.getDate());

        if (startDateForValidate != null && endDateForValidate != null && endDateForValidate.isBefore(startDateForValidate)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ngày kết thúc không được nhỏ hơn ngày bắt đầu.",
                    "Ngày áp dụng không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (ma.isEmpty() || ten.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng nhập mã và tên Khuyến mãi!",
                    "Cảnh báo",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String trangThai = normalizePromotionStatusByDate(requestedTrangThai);

        if (!trangThai.equals(requestedTrangThai)) {
            cbTrangThai.setSelectedItem(trangThai);
            JOptionPane.showMessageDialog(
                    this,
                    "Trạng thái đã được tự động chỉnh thành: " + trangThai
                    + "\nDo ngày áp dụng hiện tại không phù hợp với trạng thái bạn chọn.",
                    "Tự động chỉnh trạng thái",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }

        String campaignId = buildCampaignId(ma);

        try (Connection con = common.db.DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);

            try {
                upsertCampaign(con, campaignId, ten, tuNgay, denNgay);

                if (isEditMode) {
                    String sql = "UPDATE PROMOTIONS "
                            + "SET promotion_name = ?, campaign_id = ?, discount_amount = ?, discount_percent = ?, "
                            + "    min_order_amount = ?, status = ? "
                            + "WHERE promotion_id = ? AND NVL(is_deleted, 0) = 0";

                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, ten);
                        ps.setString(2, campaignId);
                        ps.setInt(3, giam);
                        ps.setInt(4, giam);
                        ps.setInt(5, minOrderAmount);
                        ps.setString(6, trangThai);
                        ps.setString(7, ma);
                        ps.executeUpdate();
                    }
                } else {
                    String sql = "INSERT INTO PROMOTIONS "
                            + "(promotion_id, promotion_name, campaign_id, status, discount_amount, discount_percent, min_order_amount, is_deleted) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, 0)";

                    try (PreparedStatement ps = con.prepareStatement(sql)) {
                        ps.setString(1, ma);
                        ps.setString(2, ten);
                        ps.setString(3, campaignId);
                        ps.setString(4, trangThai);
                        ps.setInt(5, giam);
                        ps.setInt(6, giam);
                        ps.setInt(7, minOrderAmount);
                        ps.executeUpdate();
                    }
                }

                savePromotionProducts(con, ma, selectedProducts);
                con.commit();

            } catch (Exception ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }

            JOptionPane.showMessageDialog(
                    this,
                    isEditMode ? "Cập nhật Khuyến mãi thành công!" : "Đã tạo Khuyến mãi mới thành công!",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE
            );

            business.service.AuditLogService.logAction(
                    isEditMode ? "CẬP NHẬT" : "THÊM MỚI",
                    "PROMOTIONS",
                    ma,
                    "",
                    "Tên CT: " + ten + " (-" + giam + "%)",
                    "Cập nhật chiến dịch KM"
            );

            clearForm();
            doSearch();
            EventBus.publish(new AppDataChangedEvent(AppEventType.STORE_INFO, "PROMO_UPDATED"));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi lưu Khuyến mãi: " + e.getMessage(),
                    "Lỗi DB",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void upsertCampaign(Connection con, String campaignId, String ten, String tuNgay, String denNgay) throws Exception {
        String sql = "MERGE INTO PROMOTION_CAMPAIGNS c "
                + "USING (SELECT ? AS campaign_id FROM dual) src "
                + "ON (c.campaign_id = src.campaign_id) "
                + "WHEN MATCHED THEN UPDATE SET c.campaign_name = ?, c.start_date = TO_DATE(NULLIF(?, ''), 'YYYY-MM-DD'), c.end_date = TO_DATE(NULLIF(?, ''), 'YYYY-MM-DD') "
                + "WHEN NOT MATCHED THEN INSERT (campaign_id, campaign_name, start_date, end_date, is_deleted) "
                + "VALUES (?, ?, TO_DATE(NULLIF(?, ''), 'YYYY-MM-DD'), TO_DATE(NULLIF(?, ''), 'YYYY-MM-DD'), 0)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, campaignId);
            ps.setString(2, ten);
            ps.setString(3, tuNgay);
            ps.setString(4, denNgay);
            ps.setString(5, campaignId);
            ps.setString(6, ten);
            ps.setString(7, tuNgay);
            ps.setString(8, denNgay);
            ps.executeUpdate();
        }
    }

    private void savePromotionProducts(
            Connection con,
            String promotionId,
            List<PromotionProductOption> selectedProducts
    ) throws Exception {
        String deleteSql = "DELETE FROM PROMOTION_PRODUCTS WHERE promotion_id = ?";

        try (PreparedStatement ps = con.prepareStatement(deleteSql)) {
            ps.setString(1, promotionId);
            ps.executeUpdate();
        }

        String insertSql = """
            INSERT INTO PROMOTION_PRODUCTS (
                promotion_id,
                product_id,
                discount_percent,
                is_deleted
            )
            VALUES (?, ?, ?, 0)
        """;

        try (PreparedStatement ps = con.prepareStatement(insertSql)) {
            for (PromotionProductOption p : selectedProducts) {
                ps.setString(1, promotionId);
                ps.setString(2, p.productId);
                ps.setDouble(3, p.discountPercent);
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    private void loadPromotionProductsSelection(String promotionId) {
        if (promoProductModel == null) {
            return;
        }

        promoProductModel.setRowCount(0);

        String sql = """
            SELECT
                pp.product_id,
                NVL(p.product_name, pp.product_id) AS product_name,
                NVL(pp.discount_percent, 0) AS discount_percent
            FROM PROMOTION_PRODUCTS pp
            LEFT JOIN PRODUCTS p
                ON p.product_id = pp.product_id
               AND NVL(p.is_deleted, 0) = 0
            WHERE pp.promotion_id = ?
              AND NVL(pp.is_deleted, 0) = 0
            ORDER BY pp.product_id
        """;

        try (
                Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, promotionId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    promoProductModel.addRow(new Object[]{
                        rs.getString("product_id"),
                        rs.getString("product_name"),
                        rs.getDouble("discount_percent")
                    });
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getSelectedProductPreviewText() {
        List<PromotionProductOption> selectedProducts = getPromotionSelectedProducts();

        if (selectedProducts == null || selectedProducts.isEmpty()) {
            return "-";
        }

        if (selectedProducts.size() <= 3) {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < selectedProducts.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }

                PromotionProductOption p = selectedProducts.get(i);
                sb.append(p.productId)
                        .append(" - ")
                        .append(new java.text.DecimalFormat("#,##0.##").format(p.discountPercent))
                        .append("%");
            }

            return sb.toString();
        }

        return selectedProducts.size() + " sản phẩm";
    }

    private void loadProductOptionsForAutocomplete() {
        allProductOptions = new java.util.ArrayList<>();
        productComboModel = new DefaultComboBoxModel<>();

        java.util.Map<String, ProductOption> uniqueById = new java.util.LinkedHashMap<>();

        try {
            for (Product p : ProductsSql.getInstance().selectAll()) {
                if (p == null || p.getProductId() == null || p.getProductName() == null) {
                    continue;
                }

                String productId = p.getProductId().trim();
                String productName = p.getProductName().trim();

                if (productId.isEmpty() || productName.isEmpty()) {
                    continue;
                }

                // ProductsSql.selectAll() có thể trả trùng sản phẩm theo chi nhánh/tồn kho.
                // Dedupe theo PRODUCT_ID để autocomplete không bị lặp.
                uniqueById.putIfAbsent(productId, new ProductOption(productId, productName));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        allProductOptions.addAll(uniqueById.values());
        allProductOptions.sort(java.util.Comparator.comparing(o -> o.productId));

        refillProductComboWithDefaultItems();
    }

    private void refillProductComboWithDefaultItems() {
        if (productComboModel == null) {
            return;
        }

        productComboModel.removeAllElements();
        int limit = Math.min(25, allProductOptions.size());
        for (int i = 0; i < limit; i++) {
            productComboModel.addElement(allProductOptions.get(i));
        }
    }

    private void scheduleProductComboFilter() {
        if (updatingProductCombo) {
            return;
        }

        if (productSearchFilterTimer != null) {
            productSearchFilterTimer.restart();
        } else {
            SwingUtilities.invokeLater(this::filterProductCombo);
        }
    }

    private void filterProductCombo() {
        if (cbProductSearch == null || productComboModel == null) {
            return;
        }

        if (updatingProductCombo) {
            return;
        }

        JTextField editor = (JTextField) cbProductSearch.getEditor().getEditorComponent();
        String keyword = editor.getText();
        String normalizedKeyword = normalizeSearchText(keyword);

        updatingProductCombo = true;

        try {
            productComboModel.removeAllElements();

            int count = 0;
            for (ProductOption opt : allProductOptions) {
                String haystack = normalizeSearchText(opt.productId + " " + opt.productName);

                if (normalizedKeyword.isEmpty() || haystack.contains(normalizedKeyword)) {
                    productComboModel.addElement(opt);
                    count++;
                }

                // Giới hạn kết quả để combo không lag khi danh sách nhiều sản phẩm.
                if (count >= 25) {
                    break;
                }
            }

            editor.setText(keyword);
            editor.setCaretPosition(editor.getText().length());

        } finally {
            updatingProductCombo = false;
        }

        if (cbProductSearch.isShowing()) {
            if (!normalizedKeyword.isEmpty() && productComboModel.getSize() > 0) {
                cbProductSearch.showPopup();
            } else {
                cbProductSearch.hidePopup();
            }
        }
    }

    private ProductOption resolveProductFromSearchInput() {
        if (cbProductSearch == null) {
            return null;
        }

        Object selected = cbProductSearch.getSelectedItem();
        if (selected instanceof ProductOption opt) {
            return opt;
        }

        JTextField editor = (JTextField) cbProductSearch.getEditor().getEditorComponent();
        String input = editor.getText();
        String normalizedInput = normalizeSearchText(input);

        if (normalizedInput.isEmpty()) {
            return null;
        }

        for (ProductOption opt : allProductOptions) {
            if (normalizeSearchText(opt.productId).equals(normalizedInput)
                    || normalizeSearchText(opt.toString()).equals(normalizedInput)) {
                return opt;
            }
        }

        if (productComboModel != null && productComboModel.getSize() == 1) {
            return productComboModel.getElementAt(0);
        }

        return null;
    }

    private String normalizeSearchText(String value) {
        if (value == null) {
            return "";
        }

        String s = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD);
        s = s.replaceAll("\\p{M}", "");
        s = s.replace('đ', 'd').replace('Đ', 'D');
        return s.toLowerCase().trim();
    }

    private void addProductToPromotionList(ProductOption opt) {
        if (opt == null || opt.productId == null || opt.productId.trim().isEmpty()) {
            return;
        }

        if (promoProductModel == null || tblPromoProducts == null) {
            return;
        }

        for (int i = 0; i < promoProductModel.getRowCount(); i++) {
            String existedId = String.valueOf(promoProductModel.getValueAt(i, 0));

            if (opt.productId.equals(existedId)) {
                tblPromoProducts.setRowSelectionInterval(i, i);
                tblPromoProducts.scrollRectToVisible(tblPromoProducts.getCellRect(i, 0, true));
                return;
            }
        }

        int defaultDiscount = ((Number) spinGiamGia.getValue()).intValue();

        promoProductModel.addRow(new Object[]{
            opt.productId,
            opt.productName,
            defaultDiscount
        });

        int last = promoProductModel.getRowCount() - 1;
        tblPromoProducts.setRowSelectionInterval(last, last);
        tblPromoProducts.scrollRectToVisible(tblPromoProducts.getCellRect(last, 0, true));
    }

    private void removeSelectedPromotionProduct() {
        if (tblPromoProducts == null || promoProductModel == null) {
            return;
        }

        int row = tblPromoProducts.getSelectedRow();

        if (row >= 0) {
            int modelRow = tblPromoProducts.convertRowIndexToModel(row);

            if (modelRow >= 0 && modelRow < promoProductModel.getRowCount()) {
                promoProductModel.removeRow(modelRow);
            }
        }
    }

    private List<PromotionProductOption> getPromotionSelectedProducts() {
        List<PromotionProductOption> result = new ArrayList<>();

        if (promoProductModel == null) {
            return result;
        }

        if (tblPromoProducts != null && tblPromoProducts.isEditing()) {
            try {
                tblPromoProducts.getCellEditor().stopCellEditing();
            } catch (Exception ignored) {
            }
        }

        for (int i = 0; i < promoProductModel.getRowCount(); i++) {
            String productId = String.valueOf(promoProductModel.getValueAt(i, 0)).trim();
            String productName = String.valueOf(promoProductModel.getValueAt(i, 1)).trim();

            double discount = 0.0;

            try {
                discount = Double.parseDouble(String.valueOf(promoProductModel.getValueAt(i, 2)).trim());
            } catch (Exception ignored) {
            }

            if (!productId.isEmpty()) {
                result.add(new PromotionProductOption(productId, productName, discount));
            }
        }

        return result;
    }

    private void clearProductSearchEditor() {
        if (cbProductSearch == null) {
            return;
        }

        updatingProductCombo = true;

        try {
            cbProductSearch.setSelectedItem(null);
            JTextField editor = (JTextField) cbProductSearch.getEditor().getEditorComponent();
            editor.setText("");
            refillProductComboWithDefaultItems();
            cbProductSearch.hidePopup();
        } catch (Exception ignored) {
        } finally {
            updatingProductCombo = false;
        }
    }

    private String buildCampaignId(String promotionId) {
        String id = "CAMP_" + promotionId;
        return id.length() <= 50 ? id : promotionId.substring(0, Math.min(50, promotionId.length()));
    }

    private void deactivatePromo() {
        String ma = txtMaKM.getText().trim();
        if (ma.isEmpty() || !isEditMode) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một khuyến mãi trong bảng trước khi tạm ngưng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn tạm ngưng khuyến mãi này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "UPDATE PROMOTIONS SET status = ? WHERE promotion_id = ? AND NVL(is_deleted, 0) = 0";
        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, STATUS_PAUSED);
            ps.setString(2, ma);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Đã tạm ngưng khuyến mãi!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            doSearch();
            EventBus.publish(new AppDataChangedEvent(AppEventType.STORE_INFO, "PROMO_UPDATED"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi tạm ngưng khuyến mãi: " + e.getMessage(), "Lỗi DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void previewPromo() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        String strTuNgay = (dtpTuNgay.getDate() != null) ? sdf.format(dtpTuNgay.getDate()) : "-";
        String strDenNgay = (dtpDenNgay.getDate() != null) ? sdf.format(dtpDenNgay.getDate()) : "-";

        String message = "Mã khuyến mãi: " + valueOrDash(txtMaKM.getText()) + "\n"
                + "Tên chương trình: " + valueOrDash(txtTenKM.getText()) + "\n"
                + "Mức giảm: " + spinGiamGia.getValue() + "%\n"
                + "Thời gian: " + strTuNgay + " đến " + strDenNgay + "\n"
                + "Chi nhánh: " + cbChiNhanh.getSelectedItem() + "\n"
                + "Sản phẩm áp dụng: " + getSelectedProductPreviewText() + "\n"
                + "Đơn tối thiểu: " + spinMinOrderAmount.getValue() + " đ\n"
                + "Trạng thái: " + cbTrangThai.getSelectedItem();

        JOptionPane.showMessageDialog(this, message, "Xem trước khuyến mãi", JOptionPane.INFORMATION_MESSAGE);
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private JLabel createSectionTitle(String value) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(blue);
        return label;
    }

    private JLabel createFormLabel(String value) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(muted);
        return label;
    }

    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setPreferredSize(new Dimension(0, 40));
        txt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(border), new EmptyBorder(6, 12, 6, 12)));
        return txt;
    }

    private void setupTableStyle() {
        tblPromos.setRowHeight(44);
        tblPromos.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblPromos.setShowVerticalLines(false);
        tblPromos.setShowHorizontalLines(false);
        tblPromos.setIntercellSpacing(new Dimension(0, 0));
        tblPromos.setGridColor(new Color(245, 247, 251));
        tblPromos.setSelectionBackground(new Color(219, 234, 254));
        tblPromos.setSelectionForeground(text);
        tblPromos.setFillsViewportHeight(true);
        tblPromos.setAutoCreateRowSorter(true);

        JTableHeader header = tblPromos.getTableHeader();
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);
        header.setBackground(new Color(243, 246, 250));
        header.setForeground(Color.BLACK);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                label.setOpaque(true);
                label.setBackground(new Color(243, 246, 250));
                label.setForeground(Color.BLACK);
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return label;
            }
        };

        DefaultTableCellRenderer bodyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                if (selected) {
                    label.setBackground(new Color(219, 234, 254));
                    label.setForeground(text);
                } else {
                    label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    if (column == 0) {
                        label.setForeground(blue);
                        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    } else {
                        label.setForeground(text);
                    }
                }
                return label;
            }
        };

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                String status = value == null ? "" : value.toString();
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                if (selected) {
                    label.setBackground(new Color(219, 234, 254));
                    label.setForeground(text);
                } else if (STATUS_ACTIVE.equals(status)) {
                    label.setBackground(softGreen);
                    label.setForeground(green);
                } else if (STATUS_UPCOMING.equals(status)) {
                    label.setBackground(softBlue);
                    label.setForeground(blue);
                } else if (STATUS_ENDED.equals(status)) {
                    label.setBackground(softOrange);
                    label.setForeground(orange);
                } else {
                    label.setBackground(softRed);
                    label.setForeground(red);
                }
                return label;
            }
        };

        for (int i = 0; i < tblPromos.getColumnCount(); i++) {
            tblPromos.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
            tblPromos.getColumnModel().getColumn(i).setCellRenderer(bodyRenderer);
        }
        tblPromos.getColumnModel().getColumn(0).setPreferredWidth(100);
        tblPromos.getColumnModel().getColumn(1).setPreferredWidth(310);
        tblPromos.getColumnModel().getColumn(2).setPreferredWidth(120);
        tblPromos.getColumnModel().getColumn(3).setPreferredWidth(120);
        tblPromos.getColumnModel().getColumn(4).setPreferredWidth(160);
        tblPromos.getColumnModel().getColumn(4).setCellRenderer(statusRenderer);
    }

    private JButton createButton(String value, Color bgColor, Color fgColor, Icon icon) {
        JButton button = new JButton(value);
        button.setIcon(icon);
        button.setIconTextGap(8);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(fgColor);
        button.setBackground(bgColor);
        button.setPreferredSize(new Dimension(130, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);
                g2.dispose();
                super.paint(g, c);
            }
        });
        return button;
    }

    class RoundedPanel extends JPanel {

        private final int radius;
        private final Color backgroundColor;

        RoundedPanel(int radius, Color backgroundColor) {
            this.radius = radius;
            this.backgroundColor = backgroundColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
