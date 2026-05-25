package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;
import business.sql.sales_order.CustomersSql;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.order.Customer;
import view.components.IconHelper;
import business.service.AuthorizationService;
import view.components.CustomerAnalyticsPanel;

public class CustomerView extends JPanel {

    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color primaryBlue = new Color(54, 92, 245);
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(75, 85, 110);
    private final Color borderGray = new Color(230, 235, 241);

    private final Color zebraBg = new Color(246, 248, 252);
    private final Color selectedBg = new Color(219, 234, 254);

    private JTextField txtId, txtName, txtPhone, txtEmail, txtAddress;
    private JComboBox<String> cbSearch;
    private JTable tblCustomers;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch;
    private JPanel customerToolPanel;
    private JPanel tabContentPanel;
    private JPanel detailPanel;
    private JPanel overviewPanel;
    private JButton btnOverviewTab;
    private JButton btnDetailTab;
    private String currentCustomerTab = "DETAIL";
    private List<String> customerSearchList = new ArrayList<>();

    // Lưu tạm SĐT gốc để dùng khi cập nhật
    private String currentSelectedRawPhone = "";

    // Model column index
    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_PHONE = 2;
    private static final int COL_EMAIL = 3;
    private static final int COL_ADDRESS = 4;
    private static final int COL_TOTAL_SPENDING = 5;
    private static final int COL_RANK = 6;
    private static final int COL_RAW_PHONE = 7;

    public CustomerView() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        loadAutoCompleteData();
        initUI();
        initEvents();
        loadCustomerData();

        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e.getType() == AppEventType.CUSTOMERS) {
                SwingUtilities.invokeLater(this::refreshTable);
            }
        });
    }

    private void loadAutoCompleteData() {
        customerSearchList.clear();

        try {
            List<Customer> list = CustomersSql.getInstance().selectAllWithRank();

            for (Customer c : list) {
                if (c.getCustomerName() != null && !c.getCustomerName().isEmpty()) {
                    String phone = c.getPhone() != null ? maskPhone(c.getPhone()) : "N/A";
                    customerSearchList.add(c.getCustomerName() + " - " + phone);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 15));
        mainPanel.setOpaque(false);

        JPanel tabBar = buildCustomerTabBar();
        mainPanel.add(tabBar, BorderLayout.NORTH);

        tabContentPanel = new JPanel(new CardLayout());
        tabContentPanel.setOpaque(false);

        detailPanel = new JPanel(new BorderLayout(25, 0));
        detailPanel.setOpaque(false);
        detailPanel.add(createFormCard(), BorderLayout.WEST);
        detailPanel.add(createTableCard(), BorderLayout.CENTER);

        overviewPanel = new CustomerAnalyticsPanel();

        if (AuthorizationService.isStoreManager() || AuthorizationService.isAdmin()) {
            tabContentPanel.add(overviewPanel, "OVERVIEW");
            tabContentPanel.add(detailPanel, "DETAIL");
            switchCustomerTab("OVERVIEW");
        } else {
            tabContentPanel.add(detailPanel, "DETAIL");
            switchCustomerTab("DETAIL");
        }

        mainPanel.add(tabContentPanel, BorderLayout.CENTER);

        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel buildCustomerTabBar() {
        JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        wrapper.setOpaque(false);

        btnOverviewTab = createCustomButton("Tổng quan", primaryBlue, Color.WHITE, null);
        btnDetailTab = createCustomButton("Chi tiết", Color.WHITE, textDark, null);

        btnOverviewTab.setPreferredSize(new Dimension(130, 40));
        btnDetailTab.setPreferredSize(new Dimension(110, 40));

        btnOverviewTab.addActionListener(e -> switchCustomerTab("OVERVIEW"));
        btnDetailTab.addActionListener(e -> switchCustomerTab("DETAIL"));

        if (AuthorizationService.isStoreManager() || AuthorizationService.isAdmin()) {
            wrapper.add(btnOverviewTab);
        }

        wrapper.add(btnDetailTab);

        return wrapper;
    }

    private void switchCustomerTab(String tab) {
        if (tab == null) {
            tab = "DETAIL";
        }

        if ("OVERVIEW".equals(tab) && !(AuthorizationService.isStoreManager() || AuthorizationService.isAdmin())) {
            tab = "DETAIL";
        }

        currentCustomerTab = tab;

        if (customerToolPanel != null) {
            customerToolPanel.setVisible("DETAIL".equals(tab));
        }

        if (tabContentPanel != null) {
            CardLayout cl = (CardLayout) tabContentPanel.getLayout();
            cl.show(tabContentPanel, tab);
        }

        updateCustomerTabButtonStyle();

        revalidate();
        repaint();
    }

    private void updateCustomerTabButtonStyle() {
        if (btnOverviewTab != null) {
            boolean active = "OVERVIEW".equals(currentCustomerTab);

            btnOverviewTab.setBackground(active ? primaryBlue : Color.WHITE);
            btnOverviewTab.setForeground(active ? Color.WHITE : textDark);
            btnOverviewTab.repaint();
        }

        if (btnDetailTab != null) {
            boolean active = "DETAIL".equals(currentCustomerTab);

            btnDetailTab.setBackground(active ? primaryBlue : Color.WHITE);
            btnDetailTab.setForeground(active ? Color.WHITE : textDark);
            btnDetailTab.repaint();
        }
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Khách Hàng");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(textDark);

        JLabel lblSub = new JLabel(
                "Quản lý hồ sơ khách hàng toàn hệ thống; chi nhánh nào cũng có thể tra SĐT để áp hạng/voucher"
        );
        lblSub.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblSub.setForeground(textGray);

        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        customerToolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 8));
        customerToolPanel.setOpaque(false);
        cbSearch = new JComboBox<>();
        styleSearchBox(cbSearch);
        setupAutoComplete(cbSearch, customerSearchList);

        JPanel searchFieldWrapper = new JPanel(new BorderLayout(8, 0));
        searchFieldWrapper.setBackground(Color.WHITE);
        searchFieldWrapper.setPreferredSize(new Dimension(460, 45));
        searchFieldWrapper.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(new Color(203, 213, 225), 25),
                new EmptyBorder(0, 15, 0, 15)
        ));

        searchFieldWrapper.add(new JLabel(IconHelper.search(16)), BorderLayout.WEST);
        searchFieldWrapper.add(cbSearch, BorderLayout.CENTER);

        btnSearch = createCustomButton("Tìm kiếm", primaryBlue, Color.WHITE, null);
        btnSearch.setPreferredSize(new Dimension(130, 45));

        customerToolPanel.add(searchFieldWrapper);
        customerToolPanel.add(btnSearch);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(customerToolPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private RoundedPanel createFormCard() {
        RoundedPanel formCard = new RoundedPanel(22, cardWhite);
        formCard.setPreferredSize(new Dimension(365, 0));
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;

        txtId = createTextField("Mã tự động...");
        txtId.setEnabled(false);
        txtId.setDisabledTextColor(new Color(120, 130, 145));
        txtId.setBackground(new Color(245, 247, 250));

        txtName = createTextField("Nhập tên khách hàng...");
        txtPhone = createTextField("Nhập số điện thoại...");
        txtEmail = createTextField("Nhập email...");
        txtAddress = createTextField("Nhập địa chỉ...");

        int y = 0;

        formCard.add(createLabel("Mã khách hàng"), addGbc(gbc, y++, 5));
        formCard.add(txtId, addGbc(gbc, y++, 15));

        formCard.add(createLabel("Tên khách hàng (*)"), addGbc(gbc, y++, 5));
        formCard.add(txtName, addGbc(gbc, y++, 15));

        formCard.add(createLabel("Số điện thoại (*)"), addGbc(gbc, y++, 5));
        formCard.add(txtPhone, addGbc(gbc, y++, 15));

        formCard.add(createLabel("Email"), addGbc(gbc, y++, 5));
        formCard.add(txtEmail, addGbc(gbc, y++, 15));

        formCard.add(createLabel("Địa chỉ"), addGbc(gbc, y++, 5));
        formCard.add(txtAddress, addGbc(gbc, y++, 28));

        btnAdd = createCustomButton("Thêm", primaryBlue, Color.WHITE, IconHelper.add(20));
        btnUpdate = createCustomButton("Cập nhật", new Color(245, 158, 11), Color.WHITE, IconHelper.edit(20));
        btnDelete = createCustomButton("Xóa", new Color(220, 53, 69), Color.WHITE, IconHelper.delete(20));
        btnClear = createCustomButton("Làm mới", new Color(148, 163, 184), Color.WHITE, IconHelper.refresh(20));

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        btnGrid.setOpaque(false);
        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);

        formCard.add(btnGrid, addGbc(gbc, y++, 0));

        return formCard;
    }

    private RoundedPanel createTableCard() {
        RoundedPanel tableCard = new RoundedPanel(22, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        tableModel = new DefaultTableModel(
                new Object[]{
                    "Mã KH",
                    "Tên khách hàng",
                    "SĐT",
                    "Email",
                    "Địa chỉ",
                    "Tổng chi",
                    "Hạng",
                    "RawPhone"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblCustomers = new JTable(tableModel);
        tblCustomers.removeColumn(tblCustomers.getColumnModel().getColumn(COL_RAW_PHONE));

        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblCustomers);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        tableCard.add(scrollPane, BorderLayout.CENTER);

        return tableCard;
    }

    private void setupTableStyle() {
        tblCustomers.setRowHeight(42);
        tblCustomers.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tblCustomers.setShowVerticalLines(false);
        tblCustomers.setShowHorizontalLines(false);
        tblCustomers.setSelectionBackground(new Color(237, 242, 255));
        tblCustomers.setSelectionForeground(textDark);
        tblCustomers.getTableHeader().setReorderingAllowed(false);
        tblCustomers.setFillsViewportHeight(true);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(new Color(238, 242, 247));
        headerRenderer.setForeground(new Color(15, 23, 42));
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 15));
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(11, 6, 11, 6));

        for (int i = 0; i < tblCustomers.getColumnModel().getColumnCount(); i++) {
            tblCustomers.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        CustomerTableRenderer renderer = new CustomerTableRenderer();

        for (int i = 0; i < tblCustomers.getColumnCount(); i++) {
            tblCustomers.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        setupCustomerTableColumnWidth();
    }

    private void setupCustomerTableColumnWidth() {
        tblCustomers.getColumnModel().getColumn(0).setPreferredWidth(95);    // Mã KH
        tblCustomers.getColumnModel().getColumn(1).setPreferredWidth(180);   // Tên
        tblCustomers.getColumnModel().getColumn(2).setPreferredWidth(120);   // SĐT
        tblCustomers.getColumnModel().getColumn(3).setPreferredWidth(210);   // Email
        tblCustomers.getColumnModel().getColumn(4).setPreferredWidth(260);   // Địa chỉ
        tblCustomers.getColumnModel().getColumn(5).setPreferredWidth(145);   // Tổng chi
        tblCustomers.getColumnModel().getColumn(6).setPreferredWidth(130);   // Hạng
    }

    private void initEvents() {
        tblCustomers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int row = tblCustomers.getSelectedRow();

                if (row < 0) {
                    return;
                }

                int modelRow = tblCustomers.convertRowIndexToModel(row);

                txtId.setText(safeString(tableModel.getValueAt(modelRow, COL_ID)));
                txtName.setText(safeString(tableModel.getValueAt(modelRow, COL_NAME)));

                // Lưu SĐT gốc và hiển thị SĐT đã che
                currentSelectedRawPhone = safeString(tableModel.getValueAt(modelRow, COL_RAW_PHONE));
                txtPhone.setText(maskPhone(currentSelectedRawPhone));

                txtEmail.setText(safeString(tableModel.getValueAt(modelRow, COL_EMAIL)));
                txtAddress.setText(safeString(tableModel.getValueAt(modelRow, COL_ADDRESS)));

            }
        });

        btnAdd.addActionListener(e -> {
            Customer c = getCustomerFromForm(false);

            if (c == null) {
                return;
            }

            c.setCustomerId("CUS" + System.currentTimeMillis());

            try {
                if (CustomersSql.getInstance().insert(c) > 0) {
                    SyncVersionDao.bumpVersion("CUSTOMERS");
                    RealtimeClient.send("CUSTOMERS_CHANGED");

                    JOptionPane.showMessageDialog(this, "✅ Thêm khách hàng thành công!");
                    loadAutoCompleteData();
                    btnClear.doClick();
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "❌ Thêm thất bại, vui lòng kiểm tra lại!",
                            "Lỗi",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        btnUpdate.addActionListener(e -> {
            String id = txtId.getText().trim();

            if (id.isEmpty() || id.startsWith("Mã")) {
                JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn khách hàng trong bảng để cập nhật!");
                return;
            }

            Customer c = getCustomerFromForm(true);

            if (c == null) {
                return;
            }

            c.setCustomerId(id);

            try {
                if (CustomersSql.getInstance().update(c) > 0) {
                    SyncVersionDao.bumpVersion("CUSTOMERS");
                    RealtimeClient.send("CUSTOMERS_CHANGED");

                    JOptionPane.showMessageDialog(this, "✅ Cập nhật thông tin thành công!");
                    loadAutoCompleteData();
                    btnClear.doClick();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Cập nhật thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        btnDelete.addActionListener(e -> {
            String id = txtId.getText().trim();

            if (id.isEmpty() || id.startsWith("Mã")) {
                JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn khách hàng cần xóa!");
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có chắc muốn xóa khách hàng này?",
                    "Xác nhận",
                    JOptionPane.YES_NO_OPTION
            );

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    if (CustomersSql.getInstance().delete(id) > 0) {
                        SyncVersionDao.bumpVersion("CUSTOMERS");
                        RealtimeClient.send("CUSTOMERS_CHANGED");

                        JOptionPane.showMessageDialog(this, "✅ Xóa khách hàng thành công!");
                        loadAutoCompleteData();
                        btnClear.doClick();
                    } else {
                        JOptionPane.showMessageDialog(this, "❌ Xóa thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        btnClear.addActionListener(e -> {
            txtId.setText("");
            txtName.setText("");
            txtPhone.setText("");
            txtEmail.setText("");
            txtAddress.setText("");

            currentSelectedRawPhone = "";

            ((JTextField) cbSearch.getEditor().getEditorComponent()).setText("");
            tblCustomers.clearSelection();

            loadCustomerData();
            if ("OVERVIEW".equals(currentCustomerTab)
                    && (AuthorizationService.isStoreManager() || AuthorizationService.isAdmin())) {
                refreshTable();
            }
        });

        btnSearch.addActionListener(e -> {
            JTextField editor = (JTextField) cbSearch.getEditor().getEditorComponent();
            String keyword = editor.getText().trim().toLowerCase();

            if (keyword.contains(" - ")) {
                keyword = keyword.split(" - ")[0].trim();
            }

            searchAndFilterTable(keyword);
        });
    }

    private Customer getCustomerFromForm(boolean isUpdate) {
        String name = txtName.getText().trim();
        String displayedPhone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();
        String address = txtAddress.getText().trim();

        if (name.isEmpty() || displayedPhone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Tên và Số điện thoại khách hàng (*)");
            return null;
        }

        String finalPhone = displayedPhone;

        if (isUpdate && displayedPhone.contains("*")) {
            finalPhone = currentSelectedRawPhone;
        } else if (displayedPhone.contains("*")) {
            JOptionPane.showMessageDialog(
                    this,
                    "Số điện thoại không hợp lệ. Vui lòng nhập lại số đúng!"
            );
            return null;
        }

        Customer c = new Customer();
        c.setCustomerName(name);
        c.setPhone(finalPhone);
        c.setEmail(email);
        c.setAddress(address);

        return c;
    }

    private void searchAndFilterTable(String keyword) {
        tableModel.setRowCount(0);

        try {
            List<Customer> list = CustomersSql.getInstance().selectAllWithRank();

            for (Customer c : list) {
                String id = c.getCustomerId() != null ? c.getCustomerId().toLowerCase() : "";
                String name = c.getCustomerName() != null ? c.getCustomerName().toLowerCase() : "";
                String phone = c.getPhone() != null ? c.getPhone().toLowerCase() : "";
                String email = c.getEmail() != null ? c.getEmail().toLowerCase() : "";

                if (keyword.isEmpty()
                        || id.contains(keyword)
                        || name.contains(keyword)
                        || phone.contains(keyword)
                        || email.contains(keyword)) {
                    addCustomerRow(c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCustomerData() {
        tableModel.setRowCount(0);

        try {
            List<Customer> list = CustomersSql.getInstance().selectAllWithRank();

            for (Customer c : list) {
                addCustomerRow(c);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addCustomerRow(Customer c) {
        tableModel.addRow(new Object[]{
            safeCell(c.getCustomerId()),
            safeCell(c.getCustomerName()),
            maskPhone(c.getPhone()),
            safeCell(c.getEmail()),
            safeCell(c.getAddress()),
            formatCurrency(c.getTotalSpending()),
            normalizeRank(c.getMemberRank()),
            c.getPhone()
        });
    }

    public void refreshTable() {
        loadAutoCompleteData();
        loadCustomerData();

        if (AuthorizationService.isStoreManager() || AuthorizationService.isAdmin()) {
            String oldTab = currentCustomerTab == null ? "OVERVIEW" : currentCustomerTab;

            overviewPanel = new CustomerAnalyticsPanel();

            if (tabContentPanel != null) {
                tabContentPanel.removeAll();
                tabContentPanel.add(overviewPanel, "OVERVIEW");
                tabContentPanel.add(detailPanel, "DETAIL");

                switchCustomerTab(oldTab);
            }
        } else {
            switchCustomerTab("DETAIL");
        }

        revalidate();
        repaint();
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isEmpty() || phone.length() < 8) {
            return "Chưa có dữ liệu";
        }

        int len = phone.length();
        String start = phone.substring(0, 3);
        String end = phone.substring(len - 3);

        StringBuilder masked = new StringBuilder();

        for (int i = 3; i < len - 3; i++) {
            masked.append("*");
        }

        return start + masked + end;
    }

    private String formatCurrency(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');

        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(amount) + " VNĐ";
    }

    private String safeCell(String value) {
        if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("null")) {
            return "—";
        }

        return value.trim();
    }

    private String safeString(Object value) {
        if (value == null) {
            return "";
        }

        String text = value.toString();

        if (text.equals("—")) {
            return "";
        }

        return text;
    }

    private Font getEmojiFont(int style, int size) {
        String[] preferredFonts = {
            "Segoe UI Emoji",
            "Noto Color Emoji",
            "Apple Color Emoji",
            "Segoe UI Symbol",
            "Dialog"
        };

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        List<String> availableFonts = java.util.Arrays.asList(ge.getAvailableFontFamilyNames());

        for (String fontName : preferredFonts) {
            if (availableFonts.contains(fontName)) {
                return new Font(fontName, style, size);
            }
        }

        return new Font("Dialog", style, size);
    }

    private String normalizeRank(String rank) {
        if (rank == null || rank.trim().isEmpty() || rank.equalsIgnoreCase("null") || rank.equals("—")) {
            return "Thường";
        }

        return rank.trim();
    }

    private String getRankIcon(String rank) {
        String value = normalizeRank(rank).toLowerCase();

        if (value.contains("kim")) {
            return "DIAMOND";
        }

        if (value.contains("vàng") || value.contains("vang") || value.contains("gold")) {
            return "GOLD";
        }

        if (value.contains("bạc") || value.contains("bac") || value.contains("silver")) {
            return "SILVER";
        }

        if (value.contains("đồng") || value.contains("dong") || value.contains("bronze")) {
            return "BRONZE";
        }

        return "NORMAL";
    }

    private Color getRankBg(String rank) {
        String value = normalizeRank(rank).toLowerCase();

        if (value.contains("kim")) {
            return new Color(237, 233, 254);
        }

        if (value.contains("vàng") || value.contains("vang") || value.contains("gold")) {
            return new Color(254, 243, 199);
        }

        if (value.contains("bạc") || value.contains("bac") || value.contains("silver")) {
            return new Color(241, 245, 249);
        }

        if (value.contains("đồng") || value.contains("dong") || value.contains("bronze")) {
            return new Color(255, 237, 213);
        }

        return new Color(224, 242, 254);
    }

    private Color getRankFg(String rank) {
        String value = normalizeRank(rank).toLowerCase();

        if (value.contains("kim")) {
            return new Color(109, 40, 217);
        }

        if (value.contains("vàng") || value.contains("vang") || value.contains("gold")) {
            return new Color(180, 83, 9);
        }

        if (value.contains("bạc") || value.contains("bac") || value.contains("silver")) {
            return new Color(71, 85, 105);
        }

        if (value.contains("đồng") || value.contains("dong") || value.contains("bronze")) {
            return new Color(194, 65, 12);
        }

        return new Color(3, 105, 161);
    }

    private void styleSearchBox(JComboBox<String> cb) {
        cb.setEditable(true);
        cb.setBorder(null);
        cb.setBackground(Color.WHITE);
        cb.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JTextField editor = (JTextField) cb.getEditor().getEditorComponent();
        editor.setBorder(new EmptyBorder(0, 5, 0, 5));
        editor.setFont(new Font("Segoe UI", Font.BOLD, 14));
        editor.putClientProperty("JTextField.placeholderText", "Tìm theo tên, mã, SĐT, email...");
    }

    private void setupAutoComplete(JComboBox<String> comboBox, List<String> originalItems) {
        JTextField editor = (JTextField) comboBox.getEditor().getEditorComponent();

        comboBox.removeAllItems();

        for (String item : originalItems) {
            comboBox.addItem(item);
        }

        comboBox.setSelectedItem("");

        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP
                        || e.getKeyCode() == KeyEvent.VK_DOWN
                        || e.getKeyCode() == KeyEvent.VK_ENTER
                        || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    return;
                }

                SwingUtilities.invokeLater(() -> {
                    String text = editor.getText();

                    comboBox.removeAllItems();

                    if (text.isEmpty()) {
                        for (String item : originalItems) {
                            comboBox.addItem(item);
                        }

                        comboBox.hidePopup();
                    } else {
                        boolean hasSuggestion = false;

                        for (String item : originalItems) {
                            if (item.toLowerCase().contains(text.toLowerCase())) {
                                comboBox.addItem(item);
                                hasSuggestion = true;
                            }
                        }

                        if (hasSuggestion) {
                            comboBox.showPopup();
                        } else {
                            comboBox.hidePopup();
                        }
                    }

                    editor.setText(text);
                });
            }
        });
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lbl.setForeground(textDark);

        return lbl;
    }

    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setPreferredSize(new Dimension(200, 40));
        txt.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setBackground(Color.WHITE);
        txt.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(new Color(210, 218, 230), 10),
                new EmptyBorder(6, 12, 6, 12)
        ));

        return txt;
    }

    private JButton createCustomButton(String text, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(text);

        if (icon != null) {
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH)));
            btn.setIconTextGap(8);
        }

        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setPreferredSize(new Dimension(140, 45));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);

        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 25, 25);

                super.paint(g2, c);
                g2.dispose();
            }
        });

        return btn;
    }

    private GridBagConstraints addGbc(GridBagConstraints gbc, int y, int bottom) {
        gbc.gridy = y;
        gbc.insets = new Insets(0, 0, bottom, 0);
        return gbc;
    }

    class CustomerTableRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            int modelColumn = table.convertColumnIndexToModel(column);

            setOpaque(true);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setForeground(new Color(15, 23, 42));

            if (isSelected) {
                setBackground(selectedBg);
                setForeground(textDark);
            } else {
                setBackground(row % 2 == 0 ? Color.WHITE : zebraBg);
            }

            if (modelColumn == COL_ID
                    || modelColumn == COL_PHONE
                    || modelColumn == COL_TOTAL_SPENDING) {
                setHorizontalAlignment(JLabel.CENTER);
            } else if (modelColumn == COL_RANK) {
                setHorizontalAlignment(JLabel.CENTER);
                setBorder(new EmptyBorder(7, 12, 7, 12));

                String rank = normalizeRank(value == null ? "" : value.toString());
                setText(rank);
                setFont(new Font("Segoe UI", Font.BOLD, 14));
                setForeground(getRankFg(rank));
                setBackground(isSelected ? selectedBg : getRankBg(rank));

                return this;
            } else {
                setHorizontalAlignment(JLabel.LEFT);
            }

            String text = value == null ? "—" : value.toString();
            setText(makeEllipsisText(table, text, column));
            setToolTipText(text);

            return this;
        }

        private String makeEllipsisText(JTable table, String text, int viewColumn) {
            if (text == null || text.trim().isEmpty()) {
                return "—";
            }

            int width = table.getColumnModel().getColumn(viewColumn).getWidth() - 22;
            FontMetrics fm = getFontMetrics(getFont());

            if (fm.stringWidth(text) <= width) {
                return text;
            }

            String ellipsis = "...";
            int ellipsisWidth = fm.stringWidth(ellipsis);
            int n = text.length();

            while (n > 0 && fm.stringWidth(text.substring(0, n)) + ellipsisWidth > width) {
                n--;
            }

            return n <= 0 ? ellipsis : text.substring(0, n) + ellipsis;
        }
    }

    class RoundedPanel extends JPanel {

        private final int radius;
        private final Color bgColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

            g2.dispose();
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

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

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
}
