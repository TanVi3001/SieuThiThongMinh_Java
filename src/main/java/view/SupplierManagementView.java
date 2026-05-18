package view;

import business.sql.prod_inventory.SuppliersSql;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.product.Supplier;
import view.components.IconHelper;

public class SupplierManagementView extends JPanel {

    public enum SupplierViewMode {
        MANAGER,
        WAREHOUSE
    }

    private final Color BACKGROUND = new Color(246, 247, 251);
    private final Color CARD = Color.WHITE;
    private final Color NAVY = new Color(23, 52, 99);
    private final Color MUTED = new Color(111, 124, 149);
    private final Color BORDER = new Color(232, 237, 245);
    private final Color BLUE = new Color(67, 97, 238);
    private final Color GREEN = new Color(0, 163, 108);
    private final Color ORANGE = new Color(255, 153, 0);
    private final Color RED = new Color(220, 53, 69);
    private final Color GRAY = new Color(142, 153, 176);

    private final SupplierViewMode viewMode;

    private JTable table;
    private DefaultTableModel model;

    private JTextField txtSupplierId;
    private JTextField txtSupplierName;
    private JTextField txtPhone;
    private JTextField txtEmail;
    private JTextField txtAddress;
    private JTextField txtSearch;

    private JComboBox<String> cboSupplierAutoComplete;
    private JComboBox<String> cboAnalyticsFilter;

    private JButton btnAdd;
    private JButton btnUpdate;
    private JButton btnDelete;
    private JButton btnRefresh;
    private JButton btnSearch;

    private JLabel lblTotal;
    private JLabel lblSelected;
    private JLabel lblTotalSupplier;
    private JLabel lblActiveSupplier;
    private JLabel lblEmptySupplier;
    private JLabel lblTopSupplier;
    private JLabel lblHintText;

    private JPanel analyticsPanel;
    private JPanel crudButtonPanel;
    private SupplierBarChartPanel chartPanel;

    private boolean adjustingCombo = false;

    private List<Supplier> cachedSuppliers = new ArrayList<>();
    private List<SuppliersSql.SupplierProductStat> cachedStats = new ArrayList<>();

    private final Map<String, String> supplierDisplayToIdMap = new HashMap<>();
    private final List<String> supplierDisplayValues = new ArrayList<>();

    public SupplierManagementView() {
        this(SupplierViewMode.MANAGER);
    }

    public SupplierManagementView(boolean readOnlyMode) {
        this(readOnlyMode ? SupplierViewMode.WAREHOUSE : SupplierViewMode.MANAGER);
    }

    public SupplierManagementView(SupplierViewMode mode) {
        this.viewMode = mode == null ? SupplierViewMode.MANAGER : mode;

        setLayout(new BorderLayout(0, 18));
        setBackground(BACKGROUND);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents();
        clearForm();
        loadSuppliers();
        applyViewMode();
    }

    private void initUI() {
        add(buildHeader(), BorderLayout.NORTH);
        add(buildBody(), BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(20, 0));
        header.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Quản Lý Nhà Cung Cấp");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(NAVY);

        JLabel subtitle = new JLabel(viewMode == SupplierViewMode.WAREHOUSE
                ? "Tra cứu nhà cung cấp phục vụ nhập kho và phiếu nhập hàng"
                : "Quản lý nhà cung cấp, theo dõi số lượng sản phẩm đang được cung cấp");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(6));
        titleBox.add(subtitle);

        JPanel searchBox = new JPanel(new BorderLayout());
        searchBox.setOpaque(false);
        searchBox.setPreferredSize(new Dimension(430, 40));

        cboSupplierAutoComplete = new JComboBox<>();
        cboSupplierAutoComplete.setEditable(true);
        cboSupplierAutoComplete.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cboSupplierAutoComplete.setBackground(Color.WHITE);
        cboSupplierAutoComplete.setPreferredSize(new Dimension(430, 40));

        txtSearch = createTextField("Tìm theo mã, tên, SĐT hoặc email...");
        txtSearch.setVisible(false);

        btnSearch = createButton("Tìm", BLUE, Color.WHITE, IconHelper.search(16));
        btnSearch.setVisible(false);

        searchBox.add(cboSupplierAutoComplete, BorderLayout.CENTER);

        header.add(titleBox, BorderLayout.WEST);
        header.add(searchBox, BorderLayout.EAST);

        return header;
    }

    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setOpaque(false);

        body.add(buildFormCard(), BorderLayout.WEST);

        JPanel right = new JPanel(new BorderLayout(0, 16));
        right.setOpaque(false);

        analyticsPanel = buildAnalyticsPanel();
        right.add(analyticsPanel, BorderLayout.NORTH);
        right.add(buildTableCard(), BorderLayout.CENTER);

        body.add(right, BorderLayout.CENTER);

        return body;
    }

    private JPanel buildAnalyticsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 250));

        JPanel kpis = new JPanel(new GridLayout(1, 4, 12, 0));
        kpis.setOpaque(false);

        lblTotalSupplier = new JLabel("0");
        lblActiveSupplier = new JLabel("0");
        lblEmptySupplier = new JLabel("0");
        lblTopSupplier = new JLabel("N/A");

        kpis.add(createKpiCard("Tổng NCC", lblTotalSupplier, BLUE));
        kpis.add(createKpiCard("Có sản phẩm", lblActiveSupplier, GREEN));
        kpis.add(createKpiCard("Chưa có SP", lblEmptySupplier, ORANGE));
        kpis.add(createKpiCard("NCC nổi bật", lblTopSupplier, RED));

        JPanel chartCard = new JPanel(new BorderLayout(0, 8));
        chartCard.setBackground(CARD);
        chartCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel chartHeader = new JPanel(new BorderLayout());
        chartHeader.setOpaque(false);

        JLabel chartTitle = new JLabel("Phân tích nhà cung cấp theo số sản phẩm");
        chartTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        chartTitle.setForeground(NAVY);

        cboAnalyticsFilter = new JComboBox<>(new String[]{
            "Tất cả nhà cung cấp",
            "Có sản phẩm cung cấp",
            "Chưa có sản phẩm",
            "Top 5 nhiều sản phẩm"
        });
        cboAnalyticsFilter.setPreferredSize(new Dimension(210, 34));
        cboAnalyticsFilter.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cboAnalyticsFilter.setBackground(Color.WHITE);

        chartHeader.add(chartTitle, BorderLayout.WEST);
        chartHeader.add(cboAnalyticsFilter, BorderLayout.EAST);

        chartPanel = new SupplierBarChartPanel();

        chartCard.add(chartHeader, BorderLayout.NORTH);
        chartCard.add(chartPanel, BorderLayout.CENTER);

        panel.add(kpis, BorderLayout.NORTH);
        panel.add(chartCard, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createKpiCard(String title, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel stripe = new JPanel();
        stripe.setBackground(accent);
        stripe.setPreferredSize(new Dimension(5, 0));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(MUTED);

        value.setFont(new Font("Segoe UI", Font.BOLD, 22));
        value.setForeground(NAVY);

        text.add(lblTitle);
        text.add(Box.createVerticalStrut(6));
        text.add(value);

        card.add(stripe, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildFormCard() {
        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setBackground(CARD);
        card.setPreferredSize(new Dimension(370, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(20, 22, 20, 22)
        ));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Thông tin nhà cung cấp");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(NAVY);

        JLabel sub = new JLabel("Mã NCC được tự sinh, không cần nhập tay");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(MUTED);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(sub);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        txtSupplierId = createTextField("Tự sinh sau khi thêm mới");
        txtSupplierId.setEditable(false);
        txtSupplierId.setBackground(new Color(245, 246, 250));

        txtSupplierName = createTextField("VD: Nhà cung cấp Tổng hợp");
        txtPhone = createTextField("VD: 0900000000");
        txtEmail = createTextField("VD: supplier@example.com");
        txtAddress = createTextField("VD: TP.HCM");

        form.add(createField("Mã nhà cung cấp", txtSupplierId));
        form.add(createField("Tên nhà cung cấp (*)", txtSupplierName));
        form.add(createField("Số điện thoại", txtPhone));
        form.add(createField("Email", txtEmail));
        form.add(createField("Địa chỉ", txtAddress));

        crudButtonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        crudButtonPanel.setOpaque(false);

        btnAdd = createButton("Thêm", BLUE, Color.WHITE, IconHelper.add(16));
        btnUpdate = createButton("Cập nhật", ORANGE, Color.WHITE, IconHelper.edit(16));
        btnDelete = createButton("Xóa", RED, Color.WHITE, IconHelper.delete(16));
        btnRefresh = createButton("Làm mới", GRAY, Color.WHITE, IconHelper.refresh(16));

        crudButtonPanel.add(btnAdd);
        crudButtonPanel.add(btnUpdate);
        crudButtonPanel.add(btnDelete);
        crudButtonPanel.add(btnRefresh);

        JPanel hint = new JPanel(new BorderLayout());
        hint.setOpaque(true);
        hint.setBackground(new Color(239, 246, 255));
        hint.setBorder(new EmptyBorder(12, 12, 12, 12));

        lblHintText = new JLabel("<html>"
                + "<b>Gợi ý nghiệp vụ:</b><br>"
                + "Nhà cung cấp được chọn khi tạo phiếu nhập kho. "
                + "Biểu đồ bên phải cho biết NCC nào đang cung cấp nhiều sản phẩm nhất."
                + "</html>");
        lblHintText.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblHintText.setForeground(NAVY);

        hint.add(lblHintText, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        bottom.add(crudButtonPanel, BorderLayout.NORTH);
        bottom.add(hint, BorderLayout.CENTER);

        card.add(titleBox, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        return card;
    }

    private JPanel buildTableCard() {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(14, 14, 14, 14)
        ));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Danh sách nhà cung cấp");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(NAVY);

        lblTotal = new JLabel("Tổng: 0 nhà cung cấp");
        lblTotal.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTotal.setForeground(MUTED);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(lblTotal);

        lblSelected = new JLabel("Chưa chọn nhà cung cấp");
        lblSelected.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblSelected.setForeground(BLUE);

        top.add(titleBox, BorderLayout.WEST);
        top.add(lblSelected, BorderLayout.EAST);

        model = new DefaultTableModel(
                new Object[]{"Mã NCC", "Tên nhà cung cấp", "Số điện thoại", "Email", "Địa chỉ", "Số SP"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        setupTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(245, 246, 250)));
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        card.add(top, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);

        return card;
    }

    private JPanel createField(String labelText, JTextField field) {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setBorder(new EmptyBorder(0, 0, 12, 0));

        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(NAVY);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        box.add(label);
        box.add(Box.createVerticalStrut(6));
        box.add(field);

        return box;
    }

    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setPreferredSize(new Dimension(0, 38));
        txt.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(6, 10, 6, 10)
        ));
        return txt;
    }

    private JButton createButton(String text, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(text);

        if (icon != null) {
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH)));
            btn.setIconTextGap(7);
        }

        btn.setPreferredSize(new Dimension(120, 38));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return btn;
    }

    private void setupTable() {
        table.setRowHeight(38);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(NAVY);
        table.setGridColor(new Color(245, 246, 250));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(new Color(237, 242, 255));
        table.setSelectionForeground(NAVY);

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(245, 246, 250));
        table.getTableHeader().setForeground(Color.BLACK);
        table.getTableHeader().setReorderingAllowed(false);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(NAVY);
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(NAVY);
                }

                if (c instanceof JLabel lbl) {
                    lbl.setBorder(new EmptyBorder(0, 8, 0, 8));
                    lbl.setHorizontalAlignment(column == 0 || column == 2 || column == 5
                            ? SwingConstants.CENTER
                            : SwingConstants.LEFT);
                }

                return c;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(85);
        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(190);
        table.getColumnModel().getColumn(4).setPreferredWidth(250);
        table.getColumnModel().getColumn(5).setPreferredWidth(70);
    }

    private void initEvents() {
        btnAdd.addActionListener(e -> addSupplier());
        btnUpdate.addActionListener(e -> updateSupplier());
        btnDelete.addActionListener(e -> deleteSupplier());

        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            resetAutoCompleteCombo();
            if (cboAnalyticsFilter != null) {
                cboAnalyticsFilter.setSelectedIndex(0);
            }
            clearForm();
            loadSuppliers();
        });

        btnSearch.addActionListener(e -> applyFilter());
        txtSearch.addActionListener(e -> applyFilter());

        cboAnalyticsFilter.addActionListener(e -> applyFilter());

        setupSupplierAutoComplete();

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                fillFormFromSelectedRow();
            }
        });
    }

    private void setupSupplierAutoComplete() {
        JTextField editor = (JTextField) cboSupplierAutoComplete.getEditor().getEditorComponent();

        editor.putClientProperty(
                "JTextField.placeholderText",
                "Chọn hoặc nhập mã/tên/số điện thoại/email nhà cung cấp..."
        );

        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (adjustingCombo) {
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    String keyword = editor.getText() == null
                            ? ""
                            : editor.getText().trim();

                    refreshAutoCompleteModel(keyword);

                    editor.setText(keyword);

                    if (cboSupplierAutoComplete.getItemCount() > 0) {
                        cboSupplierAutoComplete.showPopup();
                    }

                    applyFilter();
                });
            }
        });

        cboSupplierAutoComplete.addActionListener(e -> {
            if (!adjustingCombo) {
                applyFilter();
            }
        });
    }

    private void loadSuppliers() {
        cachedSuppliers = SuppliersSql.getInstance().selectAll();
        cachedStats = SuppliersSql.getInstance().getSupplierProductStats();

        rebuildSupplierComboData();
        updateAnalytics();
        applyFilter();
    }

    private void rebuildSupplierComboData() {
        supplierDisplayValues.clear();
        supplierDisplayToIdMap.clear();

        supplierDisplayValues.add("Tất cả nhà cung cấp");

        for (Supplier s : cachedSuppliers) {
            String display = safe(s.getSupplierId()) + " - " + safe(s.getSupplierName());
            supplierDisplayValues.add(display);
            supplierDisplayToIdMap.put(display, s.getSupplierId());
        }

        resetAutoCompleteCombo();
    }

    private void resetAutoCompleteCombo() {
        adjustingCombo = true;

        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
        for (String item : supplierDisplayValues) {
            comboModel.addElement(item);
        }

        cboSupplierAutoComplete.setModel(comboModel);
        cboSupplierAutoComplete.setSelectedItem("Tất cả nhà cung cấp");

        adjustingCombo = false;
    }

    private void refreshAutoCompleteModel(String keyword) {
        adjustingCombo = true;

        DefaultComboBoxModel<String> comboModel = new DefaultComboBoxModel<>();
        String key = keyword == null ? "" : keyword.toLowerCase();

        for (String item : supplierDisplayValues) {
            if (key.isEmpty() || item.toLowerCase().contains(key)) {
                comboModel.addElement(item);
            }
        }

        cboSupplierAutoComplete.setModel(comboModel);
        cboSupplierAutoComplete.setSelectedItem(keyword);

        adjustingCombo = false;
    }

    private void updateAnalytics() {
        int totalSupplier = cachedStats.size();
        int active = 0;
        int empty = 0;

        SuppliersSql.SupplierProductStat top = null;

        for (SuppliersSql.SupplierProductStat stat : cachedStats) {
            if (stat.productCount > 0) {
                active++;
            } else {
                empty++;
            }

            if (top == null || stat.productCount > top.productCount) {
                top = stat;
            }
        }

        lblTotalSupplier.setText(String.valueOf(totalSupplier));
        lblActiveSupplier.setText(String.valueOf(active));
        lblEmptySupplier.setText(String.valueOf(empty));

        if (top == null || top.productCount == 0) {
            lblTopSupplier.setText("N/A");
        } else {
            lblTopSupplier.setText(top.supplierId + " • " + top.productCount + " SP");
        }

        chartPanel.setData(cachedStats);
    }

    private void applyFilter() {
        Map<String, SuppliersSql.SupplierProductStat> statMap = buildStatMap();
        List<Supplier> filtered = new ArrayList<>();

        String textKeyword = "";
        String comboText = "";

        if (cboSupplierAutoComplete.isVisible() && cboSupplierAutoComplete.getSelectedItem() != null) {
            comboText = cboSupplierAutoComplete.getSelectedItem().toString().trim();
        }

        boolean comboAll = comboText.isEmpty() || "Tất cả nhà cung cấp".equals(comboText);
        String selectedSupplierId = supplierDisplayToIdMap.get(comboText);
        String comboKeyword = comboAll || selectedSupplierId != null ? "" : comboText.toLowerCase();

        String analyticsFilter = cboAnalyticsFilter.getSelectedItem() == null
                ? "Tất cả nhà cung cấp"
                : cboAnalyticsFilter.getSelectedItem().toString();

        for (Supplier supplier : cachedSuppliers) {
            SuppliersSql.SupplierProductStat stat = statMap.get(supplier.getSupplierId());
            int productCount = stat == null ? 0 : stat.productCount;

            boolean matchText = textKeyword.isEmpty()
                    || safe(supplier.getSupplierId()).toLowerCase().contains(textKeyword)
                    || safe(supplier.getSupplierName()).toLowerCase().contains(textKeyword)
                    || safe(supplier.getPhoneNumber()).toLowerCase().contains(textKeyword)
                    || safe(supplier.getEmail()).toLowerCase().contains(textKeyword);

            boolean matchCombo = comboAll
                    || (selectedSupplierId != null && selectedSupplierId.equals(supplier.getSupplierId()))
                    || (!comboKeyword.isEmpty()
                    && (safe(supplier.getSupplierId()).toLowerCase().contains(comboKeyword)
                    || safe(supplier.getSupplierName()).toLowerCase().contains(comboKeyword)
                    || safe(supplier.getPhoneNumber()).toLowerCase().contains(comboKeyword)
                    || safe(supplier.getEmail()).toLowerCase().contains(comboKeyword)));

            boolean matchAnalytics = true;

            if ("Có sản phẩm cung cấp".equals(analyticsFilter)) {
                matchAnalytics = productCount > 0;
            } else if ("Chưa có sản phẩm".equals(analyticsFilter)) {
                matchAnalytics = productCount == 0;
            }

            if (matchText && matchCombo && matchAnalytics) {
                filtered.add(supplier);
            }
        }

        if ("Top 5 nhiều sản phẩm".equals(analyticsFilter)) {
            filtered.sort((a, b) -> {
                int ca = statMap.get(a.getSupplierId()) == null ? 0 : statMap.get(a.getSupplierId()).productCount;
                int cb = statMap.get(b.getSupplierId()) == null ? 0 : statMap.get(b.getSupplierId()).productCount;
                return Integer.compare(cb, ca);
            });

            if (filtered.size() > 5) {
                filtered = new ArrayList<>(filtered.subList(0, 5));
            }
        }

        fillTable(filtered, statMap);
    }

    private Map<String, SuppliersSql.SupplierProductStat> buildStatMap() {
        Map<String, SuppliersSql.SupplierProductStat> statMap = new HashMap<>();

        for (SuppliersSql.SupplierProductStat stat : cachedStats) {
            statMap.put(stat.supplierId, stat);
        }

        return statMap;
    }

    private void fillTable(List<Supplier> list, Map<String, SuppliersSql.SupplierProductStat> statMap) {
        model.setRowCount(0);

        if (list != null) {
            for (Supplier s : list) {
                SuppliersSql.SupplierProductStat stat = statMap.get(s.getSupplierId());
                int productCount = stat == null ? 0 : stat.productCount;

                model.addRow(new Object[]{
                    safe(s.getSupplierId()),
                    safe(s.getSupplierName()),
                    safe(s.getPhoneNumber()),
                    safe(s.getEmail()),
                    safe(s.getAddress()),
                    productCount
                });
            }
        }

        lblTotal.setText("Đang hiển thị: " + model.getRowCount() + " nhà cung cấp");
        lblSelected.setText("Chưa chọn nhà cung cấp");

        if (model.getRowCount() == 1) {
            table.setRowSelectionInterval(0, 0);
        }
    }

    private void fillFormFromSelectedRow() {
        int row = table.getSelectedRow();

        if (row < 0) {
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);

        txtSupplierId.setText(String.valueOf(model.getValueAt(modelRow, 0)));
        txtSupplierName.setText(String.valueOf(model.getValueAt(modelRow, 1)));
        txtPhone.setText(String.valueOf(model.getValueAt(modelRow, 2)));
        txtEmail.setText(String.valueOf(model.getValueAt(modelRow, 3)));
        txtAddress.setText(String.valueOf(model.getValueAt(modelRow, 4)));

        txtSupplierId.setEditable(false);
        txtSupplierId.setBackground(new Color(245, 246, 250));

        lblSelected.setText("Đang chọn: " + txtSupplierId.getText());
    }

    private void addSupplier() {
        if (viewMode == SupplierViewMode.WAREHOUSE) {
            return;
        }

        try {
            Supplier supplier = readFormForInsert();

            if (SuppliersSql.getInstance().existsActiveSupplierName(supplier.getSupplierName(), null)) {
                JOptionPane.showMessageDialog(this, "Tên nhà cung cấp đã tồn tại.", "Trùng dữ liệu", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String nextId = SuppliersSql.getInstance().generateNextSupplierId();
            supplier.setSupplierId(nextId);

            int result = SuppliersSql.getInstance().insert(supplier);

            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Thêm nhà cung cấp thành công.\nMã được tạo: " + nextId);
                clearForm();
                loadSuppliers();
            } else {
                JOptionPane.showMessageDialog(this, "Thêm nhà cung cấp thất bại.");
            }

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void updateSupplier() {
        if (viewMode == SupplierViewMode.WAREHOUSE) {
            return;
        }

        try {
            Supplier supplier = readFormForUpdate();

            if (SuppliersSql.getInstance().selectById(supplier.getSupplierId()) == null) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy nhà cung cấp để cập nhật.");
                return;
            }

            if (SuppliersSql.getInstance().existsActiveSupplierName(supplier.getSupplierName(), supplier.getSupplierId())) {
                JOptionPane.showMessageDialog(this, "Tên nhà cung cấp đã tồn tại ở nhà cung cấp khác.");
                return;
            }

            int result = SuppliersSql.getInstance().update(supplier);

            if (result > 0) {
                JOptionPane.showMessageDialog(this, "Cập nhật nhà cung cấp thành công.");
                clearForm();
                loadSuppliers();
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật nhà cung cấp thất bại.");
            }

        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Thiếu dữ liệu", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void deleteSupplier() {
        if (viewMode == SupplierViewMode.WAREHOUSE) {
            return;
        }

        String supplierId = txtSupplierId.getText() == null ? "" : txtSupplierId.getText().trim();

        if (supplierId.isEmpty() || "Tự sinh".equals(supplierId)) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn nhà cung cấp cần xóa.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn xóa nhà cung cấp " + supplierId + " không?\n"
                + "Hệ thống sẽ xóa mềm, không xóa vật lý khỏi database.",
                "Xác nhận xóa",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int result = SuppliersSql.getInstance().delete(supplierId);

        if (result > 0) {
            JOptionPane.showMessageDialog(this, "Xóa nhà cung cấp thành công.");
            clearForm();
            loadSuppliers();
        } else {
            JOptionPane.showMessageDialog(this, "Xóa nhà cung cấp thất bại.");
        }
    }

    private Supplier readFormForInsert() {
        String name = text(txtSupplierName);
        String phone = text(txtPhone);
        String email = text(txtEmail);
        String address = text(txtAddress);

        validateSupplierInput(name, phone, email);

        Supplier supplier = new Supplier();
        supplier.setSupplierName(name);
        supplier.setPhoneNumber(phone);
        supplier.setEmail(email);
        supplier.setAddress(address);
        supplier.setIsDeleted(0);

        return supplier;
    }

    private Supplier readFormForUpdate() {
        String id = text(txtSupplierId);
        String name = text(txtSupplierName);
        String phone = text(txtPhone);
        String email = text(txtEmail);
        String address = text(txtAddress);

        if (id.isEmpty() || "Tự sinh".equals(id)) {
            throw new IllegalArgumentException("Vui lòng chọn nhà cung cấp cần cập nhật.");
        }

        validateSupplierInput(name, phone, email);

        Supplier supplier = new Supplier();
        supplier.setSupplierId(id);
        supplier.setSupplierName(name);
        supplier.setPhoneNumber(phone);
        supplier.setEmail(email);
        supplier.setAddress(address);
        supplier.setIsDeleted(0);

        return supplier;
    }

    private void validateSupplierInput(String name, String phone, String email) {
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng nhập tên nhà cung cấp.");
        }

        if (!phone.isEmpty() && !phone.matches("^[0-9+\\-\\s]{8,20}$")) {
            throw new IllegalArgumentException("Số điện thoại nhà cung cấp không hợp lệ.");
        }

        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Email nhà cung cấp không hợp lệ.");
        }
    }

    private void clearForm() {
        txtSupplierId.setText("Tự sinh");
        txtSupplierName.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        txtAddress.setText("");

        txtSupplierId.setEditable(false);
        txtSupplierId.setBackground(new Color(245, 246, 250));

        if (table != null) {
            table.clearSelection();
        }

        lblSelected.setText("Chưa chọn nhà cung cấp");
    }

    private void applyViewMode() {
        boolean isWarehouse = viewMode == SupplierViewMode.WAREHOUSE;

        // Cả Manager và Nhân viên kho đều dùng autocomplete combobox
        cboSupplierAutoComplete.setVisible(true);

        // Không dùng ô search + nút tìm nữa
        txtSearch.setVisible(false);
        btnSearch.setVisible(false);

        if (isWarehouse) {
            // Warehouse: ẩn phân tích Power BI
            analyticsPanel.setVisible(false);

            // Ẩn CRUD, chỉ giữ Làm mới
            btnAdd.setVisible(false);
            btnUpdate.setVisible(false);
            btnDelete.setVisible(false);
            btnRefresh.setVisible(true);

            crudButtonPanel.removeAll();
            crudButtonPanel.setLayout(new GridLayout(1, 1, 10, 10));
            crudButtonPanel.add(btnRefresh);

            // Form chỉ xem
            txtSupplierId.setEditable(false);
            txtSupplierName.setEditable(false);
            txtPhone.setEditable(false);
            txtEmail.setEditable(false);
            txtAddress.setEditable(false);

            txtSupplierId.setBackground(new Color(245, 246, 250));
            txtSupplierName.setBackground(new Color(245, 246, 250));
            txtPhone.setBackground(new Color(245, 246, 250));
            txtEmail.setBackground(new Color(245, 246, 250));
            txtAddress.setBackground(new Color(245, 246, 250));

            lblHintText.setText("<html>"
                    + "<b>Gợi ý nghiệp vụ:</b><br>"
                    + "Nhân viên kho chỉ được xem và chọn nhà cung cấp. "
                    + "Việc thêm, sửa, xóa nhà cung cấp thuộc quyền quản lý."
                    + "</html>");

        } else {
            // Manager: hiện phân tích Power BI
            analyticsPanel.setVisible(true);

            // Hiện CRUD đầy đủ
            btnAdd.setVisible(true);
            btnUpdate.setVisible(true);
            btnDelete.setVisible(true);
            btnRefresh.setVisible(true);

            crudButtonPanel.removeAll();
            crudButtonPanel.setLayout(new GridLayout(2, 2, 10, 10));
            crudButtonPanel.add(btnAdd);
            crudButtonPanel.add(btnUpdate);
            crudButtonPanel.add(btnDelete);
            crudButtonPanel.add(btnRefresh);

            // Form cho phép nhập/sửa
            txtSupplierId.setEditable(false);
            txtSupplierName.setEditable(true);
            txtPhone.setEditable(true);
            txtEmail.setEditable(true);
            txtAddress.setEditable(true);

            txtSupplierId.setBackground(new Color(245, 246, 250));
            txtSupplierName.setBackground(Color.WHITE);
            txtPhone.setBackground(Color.WHITE);
            txtEmail.setBackground(Color.WHITE);
            txtAddress.setBackground(Color.WHITE);

            lblHintText.setText("<html>"
                    + "<b>Gợi ý nghiệp vụ:</b><br>"
                    + "Nhà cung cấp được chọn khi tạo phiếu nhập kho. "
                    + "Biểu đồ bên phải cho biết NCC nào đang cung cấp nhiều sản phẩm nhất."
                    + "</html>");
        }

        revalidate();
        repaint();
    }

    private String text(JTextField txt) {
        return txt.getText() == null ? "" : txt.getText().trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String shortName(String value, int max) {
        if (value == null) {
            return "";
        }

        if (value.length() <= max) {
            return value;
        }

        return value.substring(0, max - 3) + "...";
    }

    private class SupplierBarChartPanel extends JPanel {

        private List<SuppliersSql.SupplierProductStat> data = new ArrayList<>();

        SupplierBarChartPanel() {
            setBackground(Color.WHITE);
            setPreferredSize(new Dimension(0, 120));
        }

        void setData(List<SuppliersSql.SupplierProductStat> source) {
            data = new ArrayList<>();

            if (source != null) {
                for (SuppliersSql.SupplierProductStat stat : source) {
                    if (stat.productCount > 0) {
                        data.add(stat);
                    }

                    if (data.size() >= 5) {
                        break;
                    }
                }
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (data == null || data.isEmpty()) {
                g2.setColor(MUTED);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                g2.drawString("Chưa có dữ liệu sản phẩm theo nhà cung cấp.", 20, h / 2);
                g2.dispose();
                return;
            }

            int max = 1;

            for (SuppliersSql.SupplierProductStat stat : data) {
                max = Math.max(max, stat.productCount);
            }

            int left = 160;
            int right = 55;
            int top = 10;
            int rowHeight = 24;
            int barMaxWidth = Math.max(100, w - left - right);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));

            for (int i = 0; i < data.size(); i++) {
                SuppliersSql.SupplierProductStat stat = data.get(i);

                int y = top + i * rowHeight;
                int barWidth = (int) ((stat.productCount * 1.0 / max) * barMaxWidth);

                g2.setColor(NAVY);
                g2.drawString(shortName(stat.supplierName, 22), 8, y + 16);

                g2.setColor(new Color(226, 233, 255));
                g2.fillRoundRect(left, y + 4, barMaxWidth, 12, 10, 10);

                g2.setColor(BLUE);
                g2.fillRoundRect(left, y + 4, barWidth, 12, 10, 10);

                g2.setColor(NAVY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString(stat.productCount + " SP", left + barMaxWidth + 8, y + 16);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            }

            g2.dispose();
        }
    }
}
