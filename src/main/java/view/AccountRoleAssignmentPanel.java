package view;

import business.service.SessionManager;
import business.sql.rbac.AccountSql;
import common.db.DatabaseConnection;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.realtime.RealtimeNotifier;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AccountRoleAssignmentPanel extends JPanel {

    private static final String ROLE_ADMIN = "R_ADMIN_ALL";
    private static final String ROLE_MANAGER = "R_STORE_MNG";
    private static final String ROLE_MANAGER_ALIAS = "R_MANAGER";
    private static final String ROLE_STAFF_SALE = "R_STAFF_SALE";
    private static final String ROLE_STAFF_WAREHOUSE = "R_STAFF_VIEW_PROD";
    private static final String ROLE_STAFF_WAREHOUSE_ALIAS = "R_STAFF_STOCK";

    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color primaryBlue = new Color(67, 97, 238);
    private final Color borderGray = new Color(230, 235, 241);
    private final Color red = new Color(239, 68, 68);

    private JLabel lblSelectedUser;
    private JLabel lblSelectedEmail;
    private JLabel lblSelectedStore;
    private JPanel pnlCurrRole;
    private JPanel listItems;
    private JTextField txtSearch;
    private JComboBox<String> cbRole;
    private JComboBox<StoreItem> cbStoreAssign;

    private final Map<String, JRadioButton> radioMap = new LinkedHashMap<>();
    private final Map<String, JPanel> roleCardMap = new LinkedHashMap<>();
    private ButtonGroup roleGroup;
    private JPanel roleCardsContainer;
    private JButton btnSaveRole;

    private String selectedAccountId = "";
    private String selectedEmployeeId = "";
    private String selectedStoreId = "";
    private String selectedOldRoleId = "";
    private String selectedOldRoleDisplay = "";

    private String currentRoleId;
    private String currentAccountId;
    private String currentStoreId;

    private final List<StoreItem> stores = new ArrayList<>();
    private static final double COL_ACCOUNT = 2.05;
    private static final double COL_STORE = 1.55;
    private static final double COL_ROLE = 1.35;
    private static final double COL_STATUS = 1.35;
    private static final double COL_ONLINE = 1.05;
    private static final double COL_LOCK = 0.85;

    private static final double[] ACCOUNT_COL_WEIGHTS = {
        COL_ACCOUNT,
        COL_STORE,
        COL_ROLE,
        COL_STATUS,
        COL_ONLINE,
        COL_LOCK
    };
    private static final int ACCOUNT_COL_GAP = 14;

    public AccountRoleAssignmentPanel() {
        initSessionContext();
        setupModernLayout();
        setupRealtimeSync();
    }

    private void initSessionContext() {
        try {
            if (SessionManager.getCurrentUser() != null) {
                currentRoleId = normalizeRoleId(SessionManager.getCurrentUser().getRoleId());
                currentAccountId = clean(SessionManager.getCurrentUser().getAccountId());
            }
        } catch (Exception ignored) {
        }

        try {
            currentStoreId = clean(SessionManager.getCurrentStoreId());
        } catch (Exception ignored) {
        }

        if (currentRoleId == null) {
            currentRoleId = ROLE_ADMIN;
        }
    }

    private void setupModernLayout() {
        removeAll();
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(bgLight);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        titlePanel.setBackground(bgLight);

        JLabel title = new JLabel("Phân Quyền Tài Khoản");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(textDark);

        JLabel subtitle = new JLabel("Gán quyền, chi nhánh và Khóa/Mở khóa tài khoản theo phân cấp Admin > Manager > Kho > Sale");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(textGray);

        titlePanel.add(title);
        titlePanel.add(subtitle);

        JLabel signedInBadge = new JLabel(
                "<html><span style='color:#A3AED0'>Đăng nhập:</span> <b>"
                + escapeHtml(roleIdToDisplay(currentRoleId))
                + "</b></html>",
                SwingConstants.CENTER
        );
        signedInBadge.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        signedInBadge.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 20),
                new EmptyBorder(5, 15, 5, 15)
        ));

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightHeader.setBackground(bgLight);
        rightHeader.add(signedInBadge);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        if (!canAccessRoleAssignment(currentRoleId)) {
            add(createNoPermissionPanel(), BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        loadStores();

        JPanel content = new JPanel(new BorderLayout(20, 0));
        content.setBackground(bgLight);

        JPanel leftCol = createAccountListColumn();
        JPanel rightCol = createAssignmentColumn();
        rightCol.setPreferredSize(new Dimension(470, 10));
        rightCol.setMinimumSize(new Dimension(450, 10));

        content.add(leftCol, BorderLayout.CENTER);
        content.add(rightCol, BorderLayout.EAST);

        add(content, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    private JPanel createNoPermissionPanel() {
        RoundedPanel panel = new RoundedPanel(20, cardWhite);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));
        JLabel label = new JLabel(
                "<html><div style='text-align:center;'><h2>Không có quyền truy cập</h2>"
                + "<p>Chức năng phân quyền chỉ dành cho Quản trị viên hoặc Quản lý cửa hàng.</p></div></html>",
                SwingConstants.CENTER
        );
        label.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        label.setForeground(textDark);
        panel.add(label);
        return panel;
    }

    private JPanel createAccountListColumn() {
        RoundedPanel container = new RoundedPanel(20, cardWhite);
        container.setLayout(new BorderLayout(0, 15));
        container.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel lblList = new JLabel("Danh sách tài khoản");
        lblList.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblList.setForeground(textDark);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        filterPanel.setBackground(cardWhite);

        txtSearch = new JTextField(17);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo tên, email hoặc chi nhánh...");
        txtSearch.setPreferredSize(new Dimension(240, 35));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 10),
                new EmptyBorder(5, 15, 5, 15)
        ));

        cbRole = new JComboBox<>(buildRoleFilterItems());
        cbRole.setPreferredSize(new Dimension(170, 35));
        cbRole.setBackground(Color.WHITE);
        cbRole.setBorder(new RoundBorder(borderGray, 10));

        filterPanel.add(txtSearch);
        filterPanel.add(cbRole);

        JPanel topSection = new JPanel(new BorderLayout(0, 15));
        topSection.setBackground(cardWhite);
        topSection.add(lblList, BorderLayout.NORTH);
        topSection.add(filterPanel, BorderLayout.CENTER);
        topSection.add(createAccountHeaderRow(), BorderLayout.SOUTH);
        container.add(topSection, BorderLayout.NORTH);

        listItems = new JPanel();
        listItems.setLayout(new BoxLayout(listItems, BoxLayout.Y_AXIS));
        listItems.setBackground(cardWhite);
        listItems.setAlignmentX(Component.LEFT_ALIGNMENT);

        initTableData();

        cbRole.addActionListener(e -> initTableData());
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                initTableData();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                initTableData();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                initTableData();
            }
        });

        JScrollPane scroll = new JScrollPane(listItems);
        scroll.setBorder(null);
        scroll.setViewportBorder(null);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(cardWhite);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        container.add(scroll, BorderLayout.CENTER);
        return container;
    }

    private JPanel createAccountHeaderRow() {
        JPanel header = new JPanel(new ColumnRatioLayout(ACCOUNT_COL_WEIGHTS, ACCOUNT_COL_GAP));
        header.setBackground(new Color(248, 249, 252));
        header.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 10),
                new EmptyBorder(10, 15, 10, 15)
        ));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        header.setPreferredSize(new Dimension(100, 42));
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(createHeaderLabel("Tài khoản"));
        header.add(createHeaderLabel("Chi nhánh"));
        header.add(createHeaderLabel("Vai trò hiện tại"));
        header.add(createHeaderLabel("Trạng thái tài khoản"));
        header.add(createHeaderLabel("Hoạt động"));
        header.add(createHeaderLabel("Khóa / Mở"));

        return header;
    }

    private JLabel createHeaderLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(textGray);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private String[] buildRoleFilterItems() {
        if (isAdmin(currentRoleId)) {
            return new String[]{"Tất cả vai trò", "Quản trị viên", "Quản lý cửa hàng", "Nhân viên kho", "Nhân viên bán hàng"};
        }
        if (isManager(currentRoleId)) {
            return new String[]{"Tất cả vai trò", "Nhân viên kho", "Nhân viên bán hàng"};
        }
        return new String[]{"Tất cả vai trò"};
    }

    private JPanel createAccountRow(AccountRowData acc) {
        JPanel row = new JPanel(new ColumnRatioLayout(ACCOUNT_COL_WEIGHTS, ACCOUNT_COL_GAP));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        row.setPreferredSize(new Dimension(100, 66));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setBackground(cardWhite);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderGray),
                new EmptyBorder(10, 15, 10, 15)
        ));

        JPanel pnlName = new JPanel();
        pnlName.setLayout(new BoxLayout(pnlName, BoxLayout.Y_AXIS));
        pnlName.setBackground(cardWhite);
        pnlName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblName = new JLabel(shortText(acc.displayName, 26), SwingConstants.LEFT);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(textDark);
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblEmail = new JLabel(shortText(acc.displayEmail, 30), SwingConstants.LEFT);
        lblEmail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEmail.setForeground(textGray);
        lblEmail.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlName.add(lblName);
        pnlName.add(Box.createRigidArea(new Dimension(0, 2)));
        pnlName.add(lblEmail);
        row.add(pnlName);

        row.add(wrapLeft(createStoreBadge(acc.storeId, acc.storeName, isAdmin(acc.roleId))));

        JPanel pnlRoleBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlRoleBadge.setBackground(cardWhite);
        pnlRoleBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlRoleBadge.add(createBadge(acc.displayRole));
        row.add(pnlRoleBadge);

        String colorHex = acc.active ? "#10B981" : "#EF4444";
        String statusText = acc.active ? "Hoạt động" : "Bị khóa";

        JLabel lblStatus = new JLabel(
                "<html><span style='color:" + colorHex + "; font-size:14px;'>●</span> "
                + "<span style='color:#2B3674;'>" + statusText + "</span></html>",
                SwingConstants.LEFT
        );
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        row.add(wrapLeft(lblStatus));

        row.add(wrapLeft(createOnlineStatusBadge(acc.onlineStatus)));

        JPanel pnlToggle = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlToggle.setBackground(cardWhite);

        if (isAdmin(acc.roleId)) {
            JLabel protectedBadge = createAdminProtectedBadge();
            protectedBadge.setToolTipText("Tài khoản Quản trị viên được bảo vệ, không thể khóa/mở tại màn này.");
            pnlToggle.add(protectedBadge);
        } else {
            ToggleSwitch toggleBtn = new ToggleSwitch(acc.active);
            boolean canToggle = canToggleAccount(acc);

            if (!canToggle) {
                toggleBtn.setEnabled(false);
                toggleBtn.setToolTipText(buildNoPermissionTooltip(acc));
            }

            toggleBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (toggleBtn.isEnabled()) {
                        toggleAccountStatus(acc, toggleBtn);
                    }
                }
            });

            pnlToggle.add(toggleBtn);
        }

        row.add(pnlToggle);

        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getX() >= pnlToggle.getX()) {
                    return;
                }
                selectAccount(acc);
            }
        });

        return row;
    }

    class ColumnRatioLayout implements LayoutManager2 {

        private final double[] weights;
        private final int gap;

        public ColumnRatioLayout(double[] weights, int gap) {
            this.weights = weights == null ? new double[0] : weights.clone();
            this.gap = Math.max(0, gap);
        }

        @Override
        public void layoutContainer(Container parent) {
            Insets insets = parent.getInsets();
            int count = Math.min(parent.getComponentCount(), weights.length);
            if (count <= 0) {
                return;
            }

            int totalGap = gap * (count - 1);
            int availableWidth = Math.max(0, parent.getWidth() - insets.left - insets.right - totalGap);
            int availableHeight = Math.max(0, parent.getHeight() - insets.top - insets.bottom);

            double totalWeight = 0.0;
            for (int i = 0; i < count; i++) {
                totalWeight += Math.max(0.01, weights[i]);
            }

            int[] widths = new int[count];
            int usedWidth = 0;
            for (int i = 0; i < count; i++) {
                widths[i] = (int) Math.floor(availableWidth * Math.max(0.01, weights[i]) / totalWeight);
                usedWidth += widths[i];
            }

            int remain = availableWidth - usedWidth;
            for (int i = 0; i < count && remain > 0; i++, remain--) {
                widths[i]++;
            }

            int x = insets.left;
            for (int i = 0; i < count; i++) {
                Component comp = parent.getComponent(i);
                comp.setBounds(x, insets.top, widths[i], availableHeight);
                x += widths[i] + gap;
            }
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            return new Dimension(1000, 66);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            return new Dimension(760, 50);
        }

        @Override
        public Dimension maximumLayoutSize(Container target) {
            return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
        }

        @Override
        public void addLayoutComponent(Component comp, Object constraints) {
        }

        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        @Override
        public float getLayoutAlignmentX(Container target) {
            return 0.0f;
        }

        @Override
        public float getLayoutAlignmentY(Container target) {
            return 0.0f;
        }

        @Override
        public void invalidateLayout(Container target) {
        }
    }

    private void addRowCell(JPanel row, Component comp, int gridx, double weightx, int fill, Insets insets) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gridx;
        gbc.gridy = 0;
        gbc.weightx = weightx;
        gbc.weighty = 1.0;
        gbc.fill = fill;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = insets;
        row.add(comp, gbc);
    }

    private JPanel wrapCenter(Component comp) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        p.setBackground(cardWhite);
        p.add(comp);
        return p;
    }

    private JPanel wrapLeft(Component comp) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setBackground(cardWhite);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(comp);
        return p;
    }

    private JPanel createAssignmentColumn() {
        RoundedPanel container = new RoundedPanel(20, cardWhite);
        container.setLayout(new BorderLayout());
        container.setBorder(new EmptyBorder(22, 24, 22, 24));
        container.setBackground(cardWhite);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(cardWhite);
        contentPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblAssign = new JLabel("Gán Vai Trò / Chi Nhánh");
        lblAssign.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblAssign.setForeground(textDark);
        lblAssign.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(lblAssign);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 18)));

        lblSelectedUser = createLabel("-", textDark, true);
        lblSelectedEmail = createLabel("-", textDark, false);
        lblSelectedStore = createLabel("-", textDark, true);

        lblSelectedUser.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblSelectedEmail.setAlignmentX(Component.LEFT_ALIGNMENT);
        lblSelectedStore.setAlignmentX(Component.LEFT_ALIGNMENT);

        pnlCurrRole = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlCurrRole.setBackground(cardWhite);
        pnlCurrRole.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel infoGrid = new JPanel(new GridBagLayout());
        infoGrid.setBackground(cardWhite);
        infoGrid.setBorder(new EmptyBorder(0, 0, 12, 0));
        infoGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 118));

        addInfoRow(infoGrid, 0, "Người dùng đã chọn", lblSelectedUser);
        addInfoRow(infoGrid, 1, "Email", lblSelectedEmail);
        addInfoRow(infoGrid, 2, "Vai trò hiện tại", pnlCurrRole);
        addInfoRow(infoGrid, 3, "Chi nhánh hiện tại", lblSelectedStore);

        contentPanel.add(infoGrid);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        cbStoreAssign = new JComboBox<>();
        cbStoreAssign.setBackground(Color.WHITE);
        cbStoreAssign.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        cbStoreAssign.setPreferredSize(new Dimension(360, 38));
        cbStoreAssign.setBorder(new RoundBorder(borderGray, 10));
        cbStoreAssign.setEnabled(false);
        cbStoreAssign.setAlignmentX(Component.LEFT_ALIGNMENT);
        reloadStoreCombo(null);

        JLabel storeLabel = createLabel("Chi nhánh mới", textGray, true);
        storeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        contentPanel.add(storeLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        contentPanel.add(cbStoreAssign);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        roleGroup = new ButtonGroup();
        radioMap.clear();
        roleCardMap.clear();

        roleCardsContainer = new JPanel();
        roleCardsContainer.setLayout(new BoxLayout(roleCardsContainer, BoxLayout.Y_AXIS));
        roleCardsContainer.setBackground(cardWhite);
        roleCardsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);
        roleCardsContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 230));

        buildRoleCards();
        renderAssignableRoleCards(null);

        contentPanel.add(roleCardsContainer);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 14)));

        JPanel summaryBox = createSummaryBox();
        summaryBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(summaryBox);

        contentPanel.add(Box.createRigidArea(new Dimension(0, 18)));

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        buttonPanel.setBackground(cardWhite);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));

        JButton btnCancel = createCustomButton("Hủy bỏ", new Color(235, 238, 244), textDark);
        btnCancel.addActionListener(e -> clearSelection());

        btnSaveRole = createCustomButton("Lưu thay đổi", primaryBlue, Color.WHITE);
        btnSaveRole.setEnabled(false);
        btnSaveRole.addActionListener(e -> saveSelectedRoleAndStore());

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSaveRole);

        contentPanel.add(buttonPanel);

        JPanel topWrapper = new JPanel(new BorderLayout());
        topWrapper.setBackground(cardWhite);
        topWrapper.add(contentPanel, BorderLayout.NORTH);

        container.add(topWrapper, BorderLayout.NORTH);

        return container;
    }

    private void addInfoRow(JPanel panel, int row, String labelText, Component value) {
        GridBagConstraints gbc = new GridBagConstraints();

        JLabel label = createLabel(labelText, textGray, false);
        label.setHorizontalAlignment(SwingConstants.LEFT);

        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 16);
        panel.add(label, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 10, 0);

        if (value instanceof JComponent) {
            ((JComponent) value).setAlignmentX(Component.LEFT_ALIGNMENT);
        }

        panel.add(value, gbc);
    }

    private void buildRoleCards() {
        String[][] activeRoles = {
            {ROLE_ADMIN, "Quản trị viên", "Toàn quyền quản lý hệ thống, nhân sự và thiết lập."},
            {ROLE_MANAGER, "Quản lý cửa hàng", "Quản lý hoạt động cửa hàng, xem báo cáo."},
            {ROLE_STAFF_WAREHOUSE, "Nhân viên kho", "Quản lý sản phẩm, lập phiếu nhập và tồn kho."},
            {ROLE_STAFF_SALE, "Nhân viên bán hàng", "Truy cập màn hình POS, tạo hóa đơn và thanh toán."}
        };
        for (String[] roleInfo : activeRoles) {
            JRadioButton rb = new JRadioButton();
            rb.setActionCommand(roleInfo[0]);
            radioMap.put(roleInfo[0], rb);
            radioMap.put(roleInfo[1], rb);
            JPanel card = createRoleCard(roleInfo[1], roleInfo[2], roleGroup, rb);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            roleCardMap.put(roleInfo[0], card);
            roleCardMap.put(roleInfo[1], card);
        }
    }

    private JPanel createSummaryBox() {
        JPanel summaryBox = new RoundedPanel(10, new Color(248, 249, 252));
        summaryBox.setLayout(new BorderLayout());
        summaryBox.setBorder(BorderFactory.createCompoundBorder(
                new DashedBorder(textGray, 1, 5),
                new EmptyBorder(14, 16, 14, 16)
        ));
        summaryBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        summaryBox.setPreferredSize(new Dimension(420, 120));
        summaryBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea txtSummary = new JTextArea(
                "Tóm tắt thay đổi:\n"
                + "Có thể đổi vai trò và chi nhánh trong cùng một lần lưu.\n"
                + "Manager chỉ được chuyển vào chi nhánh chưa có Manager chủ trì."
        );
        txtSummary.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSummary.setForeground(textDark);
        txtSummary.setBackground(new Color(248, 249, 252));
        txtSummary.setEditable(false);
        txtSummary.setFocusable(false);
        txtSummary.setOpaque(false);
        txtSummary.setLineWrap(true);
        txtSummary.setWrapStyleWord(true);
        txtSummary.setBorder(null);

        summaryBox.add(txtSummary, BorderLayout.CENTER);

        return summaryBox;
    }

    private void renderAssignableRoleCards(AccountRowData selected) {
        roleCardsContainer.removeAll();
        roleGroup.clearSelection();

        List<String> assignable = getAssignableRoleIds(currentRoleId);
        boolean canModifySelected = selected != null && canModifyTargetAccount(selected);

        if (selected == null) {
            JLabel hint = createLabel("Chọn một tài khoản bên trái để gán vai trò / chi nhánh.", textGray, false);
            hint.setBorder(new EmptyBorder(10, 0, 10, 0));
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            roleCardsContainer.add(hint);
        } else if (!canModifySelected) {
            JLabel hint = createLabel(
                    "<html>Bạn không có quyền thay đổi tài khoản này.<br>Chỉ được sửa tài khoản cấp thấp hơn và đúng phạm vi chi nhánh.</html>",
                    red,
                    false
            );
            hint.setBorder(new EmptyBorder(10, 0, 10, 0));
            hint.setAlignmentX(Component.LEFT_ALIGNMENT);
            roleCardsContainer.add(hint);
        } else {
            boolean first = true;

            for (String roleId : assignable) {
                JPanel card = roleCardMap.get(roleId);
                JRadioButton rb = radioMap.get(roleId);

                if (card == null || rb == null) {
                    continue;
                }

                if (!first) {
                    roleCardsContainer.add(Box.createRigidArea(new Dimension(0, 8)));
                }

                rb.setEnabled(canAssignRole(currentRoleId, roleId));
                roleCardsContainer.add(card);
                first = false;
            }

            JRadioButton currentRb = radioMap.get(normalizeRoleId(selected.roleId));
            if (currentRb != null && currentRb.isEnabled()) {
                currentRb.setSelected(true);
            }
        }

        boolean enableStore = selected != null
                && canModifySelected
                && isAdmin(currentRoleId)
                && !isAdmin(selected.roleId);

        if (cbStoreAssign != null) {
            cbStoreAssign.setEnabled(enableStore);
        }

        if (btnSaveRole != null) {
            btnSaveRole.setEnabled(selected != null && canModifySelected);
        }

        roleCardsContainer.revalidate();
        roleCardsContainer.repaint();
    }

    private void initTableData() {
        if (listItems == null) {
            return;
        }
        listItems.removeAll();
        List<AccountRowData> accounts = loadAccountsForCurrentScope();
        String selectedRoleFilter = cbRole != null && cbRole.getSelectedItem() != null ? cbRole.getSelectedItem().toString() : "Tất cả vai trò";
        String searchText = txtSearch != null ? txtSearch.getText().toLowerCase().trim() : "";

        List<AccountRowData> filteredAccounts = new ArrayList<>();
        for (AccountRowData acc : accounts) {
            if (acc == null || !canCurrentUserSeeAccount(acc)) {
                continue;
            }
            String displayName = fallback(acc.displayName, acc.username);
            String displayEmail = fallback(acc.displayEmail, "Chưa có email");
            String displayRole = roleIdToDisplay(acc.roleId);
            String storeDisplay = getStoreDisplayName(acc.storeId, acc.storeName, isAdmin(acc.roleId));
            boolean matchRole = "Tất cả vai trò".equals(selectedRoleFilter) || displayRole.equals(selectedRoleFilter);
            boolean matchSearch = searchText.isEmpty()
                    || displayName.toLowerCase().contains(searchText)
                    || displayEmail.toLowerCase().contains(searchText)
                    || storeDisplay.toLowerCase().contains(searchText)
                    || fallback(acc.storeId, "").toLowerCase().contains(searchText);
            if (matchRole && matchSearch) {
                acc.displayName = displayName;
                acc.displayEmail = displayEmail;
                acc.displayRole = displayRole;
                acc.storeDisplay = storeDisplay;
                filteredAccounts.add(acc);
            }
        }

        filteredAccounts.sort((a, b) -> {
            int adminCompare = Integer.compare(storeSortGroup(a), storeSortGroup(b));
            if (adminCompare != 0) {
                return adminCompare;
            }
            int storeCompare = fallback(a.storeDisplay, "").compareToIgnoreCase(fallback(b.storeDisplay, ""));
            if (storeCompare != 0) {
                return storeCompare;
            }
            int levelCompare = Integer.compare(levelOf(b.roleId), levelOf(a.roleId));
            if (levelCompare != 0) {
                return levelCompare;
            }
            int activeCompare = Boolean.compare(b.active, a.active);
            if (activeCompare != 0) {
                return activeCompare;
            }
            return fallback(a.displayName, a.username).compareToIgnoreCase(fallback(b.displayName, b.username));
        });

        String lastGroupKey = null;
        for (AccountRowData acc : filteredAccounts) {
            String groupKey = isAdmin(acc.roleId) ? "ADMIN" : fallback(acc.storeDisplay, "Chưa phân chi nhánh");
            if (!groupKey.equals(lastGroupKey)) {
                listItems.add(createStoreGroupHeader(groupKey, acc.storeId, isAdmin(acc.roleId)));
                lastGroupKey = groupKey;
            }
            listItems.add(createAccountRow(acc));
        }
        listItems.revalidate();
        listItems.repaint();
    }

    private int storeSortGroup(AccountRowData acc) {
        if (acc != null && isAdmin(acc.roleId)) {
            return -1;
        }
        return 0;
    }

    private JPanel createStoreGroupHeader(String title, String storeId, boolean adminGroup) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        panel.setPreferredSize(new Dimension(100, 36));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBackground(adminGroup ? new Color(255, 247, 237) : new Color(237, 242, 255));
        panel.setBorder(new EmptyBorder(8, 15, 8, 15));
        JLabel label = new JLabel(adminGroup ? "QUẢN TRỊ HỆ THỐNG" : title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(adminGroup ? new Color(194, 65, 12) : storeColor(storeId));
        panel.add(label, BorderLayout.WEST);
        return panel;
    }

    private List<AccountRowData> loadAccountsForCurrentScope() {
        List<AccountRowData> rows = new ArrayList<>();
        try {
            AccountSql.getInstance().syncOnlineStatusFromSessions();
        } catch (Exception ignored) {
        }
        StringBuilder sql = new StringBuilder("""
            SELECT a.account_id,
                   a.user_id,
                   a.username,
                   NVL(u.full_name, a.username) AS full_name,
                   NVL(u.email, '') AS email,
                   COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id) AS role_value,
                   NVL(a.is_deleted, 0) AS is_deleted,
                   NVL(a.status, N'Hoạt động') AS account_status,
                   CASE
                       WHEN NVL(a.is_deleted, 0) = 1 THEN 'OFFLINE'
                       WHEN EXISTS (
                           SELECT 1
                           FROM ACCOUNT_SESSIONS s
                           WHERE s.account_id = a.account_id
                             AND s.status = 'ACTIVE'
                             AND NVL(s.is_deleted, 0) = 0
                             AND s.last_heartbeat_at >= SYSTIMESTAMP - INTERVAL '30' SECOND
                       ) THEN 'ONLINE'
                       ELSE 'OFFLINE'
                   END AS online_status,
                   e.employee_id,
                   e.store_id,
                   st.store_name
            FROM ACCOUNTS a
            LEFT JOIN USERS u
                   ON a.user_id = u.user_id
            LEFT JOIN EMPLOYEES e
                   ON e.employee_id = a.user_id
                  AND NVL(e.is_deleted, 0) = 0
            LEFT JOIN STORES st
                   ON st.store_id = e.store_id
                  AND NVL(st.is_deleted, 0) = 0
            LEFT JOIN ACCOUNT_ASSIGN_ROLE aar
                   ON a.account_id = aar.account_id
                  AND NVL(aar.is_deleted, 0) = 0
            LEFT JOIN ACCOUNT_ASSIGN_ROLE_GROUP aarg
                   ON a.account_id = aarg.account_id
                  AND NVL(aarg.is_deleted, 0) = 0
            LEFT JOIN ROLE_GROUPS rg
                   ON aarg.role_group_id = rg.role_group_id
                  AND NVL(rg.is_deleted, 0) = 0
            WHERE NVL(a.is_deleted, 0) = 0
              AND (
                   COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id) = 'R_ADMIN_ALL'
                   OR e.employee_id IS NOT NULL
              )
        """);
        boolean managerScoped = isManager(currentRoleId);
        if (managerScoped) {
            sql.append("""
                  AND e.store_id = ?
                  AND COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id)
                      IN ('R_STAFF_SALE', 'R_STAFF_VIEW_PROD', 'R_STAFF_STOCK')
            """);
        }
        sql.append(" ORDER BY NVL(a.is_deleted, 0), LOWER(NVL(u.full_name, a.username)) ");
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (managerScoped) {
                ps.setString(1, currentStoreId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(mapAccountRow(rs));
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();

            String msg = ex.getMessage() == null ? "" : ex.getMessage();

            if (Thread.currentThread().isInterrupted()
                    || msg.contains("Interrupted")
                    || msg.contains("Socket read interrupted")
                    || msg.contains("Socket write interrupted")) {
                System.err.println("[AccountRoleAssignmentPanel] Bỏ qua lỗi do reload/cancel worker: " + msg);
                return rows;
            }

            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                    this,
                    "Lỗi tải danh sách tài khoản: " + msg,
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            ));
        }
        return rows;
    }

    private AccountRowData mapAccountRow(ResultSet rs) throws Exception {
        AccountRowData row = new AccountRowData();
        row.accountId = clean(rs.getString("account_id"));
        row.userId = clean(rs.getString("user_id"));
        row.employeeId = clean(rs.getString("employee_id"));
        row.username = fallback(clean(rs.getString("username")), "");
        row.displayName = fallback(clean(rs.getString("full_name")), row.username);
        row.displayEmail = fallback(clean(rs.getString("email")), "Chưa có email");
        row.roleId = normalizeRoleId(rs.getString("role_value"));
        row.displayRole = roleIdToDisplay(row.roleId);
        row.isDeleted = rs.getInt("is_deleted");
        row.accountStatus = fallback(clean(rs.getString("account_status")), "Hoạt động");
        row.active = row.isDeleted == 0 && !isLockedStatus(row.accountStatus);
        row.onlineStatus = fallback(clean(rs.getString("online_status")), "OFFLINE");
        row.storeId = clean(rs.getString("store_id"));
        row.storeName = clean(rs.getString("store_name"));
        row.storeDisplay = getStoreDisplayName(row.storeId, row.storeName, isAdmin(row.roleId));
        return row;
    }

    private void selectAccount(AccountRowData acc) {
        selectedAccountId = acc.accountId;
        selectedEmployeeId = fallback(acc.employeeId, acc.userId);
        selectedStoreId = acc.storeId;
        selectedOldRoleId = normalizeRoleId(acc.roleId);
        selectedOldRoleDisplay = acc.displayRole;
        lblSelectedUser.setText(acc.displayName);
        lblSelectedEmail.setText(shortText(acc.displayEmail, 26));
        lblSelectedStore.setText(shortText(getStoreDisplayName(acc.storeId, acc.storeName, isAdmin(acc.roleId)), 28));
        pnlCurrRole.removeAll();
        pnlCurrRole.add(createBadge(acc.displayRole));
        pnlCurrRole.revalidate();
        pnlCurrRole.repaint();
        reloadStoreCombo(acc.storeId);
        renderAssignableRoleCards(acc);
    }

    private void clearSelection() {
        selectedAccountId = "";
        selectedEmployeeId = "";
        selectedStoreId = "";
        selectedOldRoleId = "";
        selectedOldRoleDisplay = "";
        lblSelectedUser.setText("-");
        lblSelectedEmail.setText("-");
        lblSelectedStore.setText("-");
        pnlCurrRole.removeAll();
        pnlCurrRole.revalidate();
        pnlCurrRole.repaint();
        reloadStoreCombo(null);
        if (cbStoreAssign != null) {
            cbStoreAssign.setEnabled(false);
        }
        renderAssignableRoleCards(null);
    }

    private void saveSelectedRoleAndStore() {
        if (selectedAccountId == null || selectedAccountId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một tài khoản từ danh sách bên trái!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ButtonModel selectedModel = roleGroup.getSelection();
        if (selectedModel == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một vai trò mới để gán!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String newRoleId = normalizeRoleId(selectedModel.getActionCommand());
        AccountRowData latest = loadAccountById(selectedAccountId);
        if (latest == null) {
            JOptionPane.showMessageDialog(this, "Tài khoản không còn tồn tại hoặc đã bị thay đổi.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            initTableData();
            clearSelection();
            return;
        }
        if (!canModifyTargetAccount(latest)) {
            JOptionPane.showMessageDialog(this, "Bạn không có quyền phân quyền tài khoản này!", "Không đủ quyền", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!canAssignRole(currentRoleId, newRoleId)) {
            JOptionPane.showMessageDialog(this, "Bạn không được gán vai trò này!", "Không đủ quyền", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String oldRoleId = normalizeRoleId(latest.roleId);
        String newStoreId = latest.storeId;
        if (isAdmin(currentRoleId) && !isAdmin(newRoleId)) {
            StoreItem selectedStore = getSelectedStoreItem();
            if (selectedStore == null || selectedStore.storeId == null || selectedStore.storeId.isBlank()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn chi nhánh cho tài khoản này.", "Thiếu chi nhánh", JOptionPane.WARNING_MESSAGE);
                return;
            }
            newStoreId = selectedStore.storeId;
        }

        if (isManager(newRoleId) && isStoreManagedByAnotherAccount(newStoreId, selectedAccountId)) {
            JOptionPane.showMessageDialog(this, "Chi nhánh này đã có Manager chủ trì.\nVui lòng chọn chi nhánh khác chưa có Manager.", "Trùng Manager chi nhánh", JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean roleChanged = !newRoleId.equalsIgnoreCase(oldRoleId);
        boolean storeChanged = !isSameStore(fallback(latest.storeId, ""), fallback(newStoreId, ""));
        if (!roleChanged && !storeChanged) {
            JOptionPane.showMessageDialog(this, "Vai trò và chi nhánh không thay đổi nên không cần cập nhật.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        boolean success = updateAccountRoleAndStore(latest, newRoleId, newStoreId);
        if (success) {
            if (storeChanged) {
                try {
                    AccountSql.getInstance().forceLogoutAccount(selectedAccountId);
                } catch (Exception ex) {
                    System.err.println("[AccountRoleAssignmentPanel] Không thể force logout sau khi đổi chi nhánh: " + ex.getMessage());
                }
            }

            String oldValue = roleIdToDisplay(oldRoleId) + " | " + getStoreDisplayName(latest.storeId, latest.storeName, isAdmin(oldRoleId));
            String newValue = roleIdToDisplay(newRoleId) + " | " + getStoreDisplayName(newStoreId, getStoreNameById(newStoreId), isAdmin(newRoleId));
            business.service.AuditLogService.logAction(
                    "CẬP NHẬT",
                    "ACCOUNTS",
                    selectedAccountId,
                    oldValue,
                    newValue,
                    roleIdToDisplay(currentRoleId) + " thay đổi phân quyền/chi nhánh tài khoản"
            );
            business.service.AccountService.logChangeRole(
                    selectedAccountId,
                    oldRoleId,
                    newRoleId,
                    roleIdToDisplay(currentRoleId) + " thay đổi phân quyền tài khoản"
            );
            RealtimeNotifier.accountSecurityChanged("ACCOUNT_ROLE_STORE_UPDATED:" + selectedAccountId);
            RealtimeNotifier.employeesChanged("EMPLOYEE_STORE_ROLE_UPDATED:" + fallback(selectedEmployeeId, selectedAccountId));
            String successMessage = storeChanged
                    ? "Cập nhật phân quyền / chi nhánh thành công!\nTài khoản vừa đổi chi nhánh sẽ bị đăng xuất và cần đăng nhập lại để nhận dữ liệu chi nhánh mới."
                    : "Cập nhật phân quyền / chi nhánh thành công!";
            JOptionPane.showMessageDialog(this, successMessage, "Thành công", JOptionPane.INFORMATION_MESSAGE);
            clearSelection();
            initTableData();
        } else {
            JOptionPane.showMessageDialog(this, "Cập nhật thất bại. Vui lòng kiểm tra lại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean updateAccountRoleAndStore(AccountRowData latest, String newRoleId, String newStoreId) {
        String employeeId = fallback(latest.employeeId, latest.userId);
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                boolean roleOk = AccountSql.getInstance().updateAccountRole(latest.accountId, newRoleId);
                if (!roleOk) {
                    con.rollback();
                    return false;
                }
                if (employeeId != null && !employeeId.isBlank() && !isAdmin(newRoleId)) {
                    String updateEmployee = """
                        UPDATE EMPLOYEES
                        SET store_id = ?,
                            role_id = ?,
                            updated_at = CURRENT_TIMESTAMP
                        WHERE employee_id = ?
                          AND NVL(is_deleted, 0) = 0
                    """;
                    try (PreparedStatement ps = con.prepareStatement(updateEmployee)) {
                        ps.setString(1, newStoreId);
                        ps.setString(2, newRoleId);
                        ps.setString(3, employeeId);
                        ps.executeUpdate();
                    }
                }
                con.commit();
                return true;
            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private AccountRowData loadAccountById(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return null;
        }
        try {
            AccountSql.getInstance().syncOnlineStatusFromSessions();
        } catch (Exception ignored) {
        }
        String sql = """
            SELECT a.account_id,
                   a.user_id,
                   a.username,
                   NVL(u.full_name, a.username) AS full_name,
                   NVL(u.email, '') AS email,
                   COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id) AS role_value,
                   NVL(a.is_deleted, 0) AS is_deleted,
                   NVL(a.status, N'Hoạt động') AS account_status,
                   CASE
                       WHEN NVL(a.is_deleted, 0) = 1 THEN 'OFFLINE'
                       WHEN EXISTS (
                           SELECT 1
                           FROM ACCOUNT_SESSIONS s
                           WHERE s.account_id = a.account_id
                             AND s.status = 'ACTIVE'
                             AND NVL(s.is_deleted, 0) = 0
                             AND s.last_heartbeat_at >= SYSTIMESTAMP - INTERVAL '30' SECOND
                       ) THEN 'ONLINE'
                       ELSE 'OFFLINE'
                   END AS online_status,
                   e.employee_id,
                   e.store_id,
                   st.store_name
            FROM ACCOUNTS a
            LEFT JOIN USERS u
                   ON a.user_id = u.user_id
            LEFT JOIN EMPLOYEES e
                   ON e.employee_id = a.user_id
                  AND NVL(e.is_deleted, 0) = 0
            LEFT JOIN STORES st
                   ON st.store_id = e.store_id
                  AND NVL(st.is_deleted, 0) = 0
            LEFT JOIN ACCOUNT_ASSIGN_ROLE aar
                   ON a.account_id = aar.account_id
                  AND NVL(aar.is_deleted, 0) = 0
            LEFT JOIN ACCOUNT_ASSIGN_ROLE_GROUP aarg
                   ON a.account_id = aarg.account_id
                  AND NVL(aarg.is_deleted, 0) = 0
            LEFT JOIN ROLE_GROUPS rg
                   ON aarg.role_group_id = rg.role_group_id
                  AND NVL(rg.is_deleted, 0) = 0
            WHERE a.account_id = ?
              AND NVL(a.is_deleted, 0) = 0
              AND (
                   COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id) = 'R_ADMIN_ALL'
                   OR e.employee_id IS NOT NULL
              )
        """;
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, accountId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapAccountRow(rs);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    private void toggleAccountStatus(AccountRowData acc, ToggleSwitch toggleBtn) {
        AccountRowData latest = loadAccountById(acc.accountId);
        if (latest == null) {
            JOptionPane.showMessageDialog(this, "Tài khoản không còn tồn tại hoặc đã bị thay đổi.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            initTableData();
            return;
        }
        if (!canToggleAccount(latest)) {
            JOptionPane.showMessageDialog(this, "Bạn không có quyền khóa/mở tài khoản này!", "Không đủ quyền", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean nextState = !latest.active;
        String actionName = nextState ? "Mở khóa" : "Khóa";
        String confirmMsg = nextState
                ? "Bạn có chắc chắn muốn MỞ KHÓA tài khoản [" + latest.displayName + "]?"
                : "KHÓA tài khoản [" + latest.displayName + "]?\nNgười dùng sẽ bị đăng xuất khỏi hệ thống ngay lập tức.";
        int confirm = JOptionPane.showConfirmDialog(this, confirmMsg, "Xác nhận " + actionName, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        String sql = """
            UPDATE ACCOUNTS
            SET status = CASE WHEN ? = 1 THEN N'Bị khóa' ELSE N'Hoạt động' END,
                online_status = CASE WHEN ? = 1 THEN 'OFFLINE' ELSE NVL(online_status, 'OFFLINE') END,
                active_sessions = CASE WHEN ? = 1 THEN 0 ELSE NVL(active_sessions, 0) END,
                current_session_id = CASE WHEN ? = 1 THEN NULL ELSE current_session_id END,
                last_logout_at = CASE WHEN ? = 1 THEN CURRENT_TIMESTAMP ELSE last_logout_at END,
                updated_at = CURRENT_TIMESTAMP
            WHERE account_id = ?
              AND NVL(is_deleted, 0) = 0
        """;
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int lockedValue = nextState ? 0 : 1;
            ps.setInt(1, lockedValue);
            ps.setInt(2, lockedValue);
            ps.setInt(3, lockedValue);
            ps.setInt(4, lockedValue);
            ps.setInt(5, lockedValue);
            ps.setString(6, latest.accountId);
            int updated = ps.executeUpdate();
            if (updated > 0) {
                toggleBtn.setOn(nextState);
                if (!nextState) {
                    AccountSql.getInstance().forceLogoutAccount(latest.accountId);
                }
                business.service.AuditLogService.logAction(
                        "CẬP NHẬT",
                        "ACCOUNTS",
                        latest.accountId,
                        nextState ? "Bị khóa" : "Hoạt động",
                        nextState ? "Hoạt động" : "Bị khóa",
                        roleIdToDisplay(currentRoleId) + " " + actionName.toLowerCase() + " tài khoản"
                );
                RealtimeNotifier.accountSecurityChanged("ACCOUNT_LOCK_UPDATED:" + latest.accountId);
                RealtimeNotifier.employeesChanged("EMPLOYEE_ACCOUNT_LOCK_UPDATED:" + fallback(latest.employeeId, latest.userId));
                SwingUtilities.invokeLater(this::initTableData);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi cập nhật trạng thái: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadStores() {
        stores.clear();
        StringBuilder sql = new StringBuilder("""
            SELECT store_id, store_name
            FROM STORES
            WHERE NVL(is_deleted, 0) = 0
        """);
        boolean managerScoped = isManager(currentRoleId) && currentStoreId != null && !currentStoreId.isBlank();
        if (managerScoped) {
            sql.append(" AND store_id = ? ");
        }
        sql.append(" ORDER BY store_id ");
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {
            if (managerScoped) {
                ps.setString(1, currentStoreId);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    stores.add(new StoreItem(clean(rs.getString("store_id")), clean(rs.getString("store_name"))));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void reloadStoreCombo(String selectedStoreId) {
        if (cbStoreAssign == null) {
            return;
        }
        cbStoreAssign.removeAllItems();
        for (StoreItem store : stores) {
            cbStoreAssign.addItem(store);
        }
        if (selectedStoreId != null) {
            for (int i = 0; i < cbStoreAssign.getItemCount(); i++) {
                StoreItem item = cbStoreAssign.getItemAt(i);
                if (isSameStore(item.storeId, selectedStoreId)) {
                    cbStoreAssign.setSelectedIndex(i);
                    return;
                }
            }
        }
        if (cbStoreAssign.getItemCount() > 0) {
            cbStoreAssign.setSelectedIndex(0);
        }
    }

    private StoreItem getSelectedStoreItem() {
        Object selected = cbStoreAssign == null ? null : cbStoreAssign.getSelectedItem();
        if (selected instanceof StoreItem) {
            return (StoreItem) selected;
        }
        return null;
    }

    private String getStoreNameById(String storeId) {
        if (storeId == null || storeId.isBlank()) {
            return null;
        }
        for (StoreItem store : stores) {
            if (isSameStore(store.storeId, storeId)) {
                return store.storeName;
            }
        }
        return null;
    }

    private boolean isStoreManagedByAnotherAccount(String storeId, String accountId) {
        if (storeId == null || storeId.isBlank()) {
            return false;
        }
        String sql = """
            SELECT COUNT(*) AS cnt
            FROM ACCOUNTS a
            JOIN EMPLOYEES e
              ON e.employee_id = a.user_id
             AND NVL(e.is_deleted, 0) = 0
            LEFT JOIN ACCOUNT_ASSIGN_ROLE aar
              ON aar.account_id = a.account_id
             AND NVL(aar.is_deleted, 0) = 0
            LEFT JOIN ACCOUNT_ASSIGN_ROLE_GROUP aarg
              ON aarg.account_id = a.account_id
             AND NVL(aarg.is_deleted, 0) = 0
            LEFT JOIN ROLE_GROUPS rg
              ON rg.role_group_id = aarg.role_group_id
             AND NVL(rg.is_deleted, 0) = 0
            WHERE e.store_id = ?
              AND a.account_id <> ?
              AND NVL(a.is_deleted, 0) = 0
              AND COALESCE(aar.role_id, CAST(rg.group_name AS VARCHAR2(100)), aarg.role_group_id) IN ('R_STORE_MNG', 'R_MANAGER')
        """;
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, storeId);
            ps.setString(2, accountId == null ? "" : accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("cnt") > 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }

    private boolean canCurrentUserSeeAccount(AccountRowData acc) {
        if (acc == null) {
            return false;
        }
        if (isAdmin(currentRoleId)) {
            return true;
        }
        if (isManager(currentRoleId)) {
            return isSameStore(currentStoreId, acc.storeId) && levelOf(acc.roleId) < levelOf(currentRoleId);
        }
        return false;
    }

    private boolean canModifyTargetAccount(AccountRowData acc) {
        if (acc == null || !canAccessRoleAssignment(currentRoleId)) {
            return false;
        }
        if (isSameAccount(currentAccountId, acc.accountId)) {
            return false;
        }
        if (levelOf(currentRoleId) <= levelOf(acc.roleId)) {
            return false;
        }
        if (isAdmin(currentRoleId)) {
            return true;
        }
        if (isManager(currentRoleId)) {
            return isSameStore(currentStoreId, acc.storeId);
        }
        return false;
    }

    private boolean canToggleAccount(AccountRowData acc) {
        if (acc == null) {
            return false;
        }
        if (isAdmin(acc.roleId)) {
            return false;
        }
        return canModifyTargetAccount(acc);
    }

    private String buildNoPermissionTooltip(AccountRowData acc) {
        if (acc == null) {
            return "Không có quyền thao tác.";
        }
        if (isAdmin(acc.roleId)) {
            return "Tài khoản Quản trị viên được bảo vệ, không thể khóa/mở tại màn này.";
        }
        if (isSameAccount(currentAccountId, acc.accountId)) {
            return "Không thể khóa/mở chính tài khoản đang đăng nhập.";
        }
        if (levelOf(currentRoleId) <= levelOf(acc.roleId)) {
            return "Không thể thao tác tài khoản cùng cấp hoặc cao hơn.";
        }
        if (isManager(currentRoleId) && !isSameStore(currentStoreId, acc.storeId)) {
            return "Manager chỉ được thao tác tài khoản trong cùng chi nhánh.";
        }
        return "Không có quyền thao tác.";
    }

    private boolean canAccessRoleAssignment(String roleId) {
        return isAdmin(roleId) || isManager(roleId);
    }

    private boolean canAssignRole(String currentRole, String targetRole) {
        int currentLevel = levelOf(currentRole);
        int targetLevel = levelOf(targetRole);
        return currentLevel > 0 && targetLevel > 0 && targetLevel < currentLevel;
    }

    private List<String> getAssignableRoleIds(String roleId) {
        List<String> roles = new ArrayList<>();
        if (isAdmin(roleId)) {
            roles.add(ROLE_MANAGER);
            roles.add(ROLE_STAFF_WAREHOUSE);
            roles.add(ROLE_STAFF_SALE);
        } else if (isManager(roleId)) {
            roles.add(ROLE_STAFF_WAREHOUSE);
            roles.add(ROLE_STAFF_SALE);
        }
        return roles;
    }

    private int levelOf(String roleId) {
        String role = normalizeRoleId(roleId);
        if (ROLE_ADMIN.equals(role)) {
            return 4;
        }
        if (ROLE_MANAGER.equals(role)) {
            return 3;
        }
        if (ROLE_STAFF_WAREHOUSE.equals(role)) {
            return 2;
        }
        if (ROLE_STAFF_SALE.equals(role)) {
            return 1;
        }
        return 0;
    }

    private boolean isAdmin(String roleId) {
        return ROLE_ADMIN.equals(normalizeRoleId(roleId));
    }

    private boolean isManager(String roleId) {
        return ROLE_MANAGER.equals(normalizeRoleId(roleId));
    }

    private boolean isSameStore(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private boolean isSameAccount(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    private String normalizeRoleId(String roleId) {
        String role = roleId == null ? "" : roleId.trim().toUpperCase();
        if (role.isEmpty()) {
            return ROLE_STAFF_SALE;
        }
        if (ROLE_MANAGER_ALIAS.equals(role)) {
            return ROLE_MANAGER;
        }
        if (ROLE_STAFF_WAREHOUSE_ALIAS.equals(role)) {
            return ROLE_STAFF_WAREHOUSE;
        }
        if ("QUẢN TRỊ VIÊN".equalsIgnoreCase(role) || "ADMIN".equalsIgnoreCase(role)) {
            return ROLE_ADMIN;
        }
        if ("QUẢN LÝ CỬA HÀNG".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role)) {
            return ROLE_MANAGER;
        }
        if ("NHÂN VIÊN KHO".equalsIgnoreCase(role) || "WAREHOUSE".equalsIgnoreCase(role)) {
            return ROLE_STAFF_WAREHOUSE;
        }
        if ("NHÂN VIÊN BÁN HÀNG".equalsIgnoreCase(role) || "SALE".equalsIgnoreCase(role)) {
            return ROLE_STAFF_SALE;
        }
        return role;
    }

    private boolean isLockedStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        return normalized.equals("BỊ KHÓA")
                || normalized.equals("BỊ KHOÁ")
                || normalized.equals("LOCKED")
                || normalized.equals("INACTIVE")
                || normalized.equals("DISABLED")
                || normalized.equals("KHÓA")
                || normalized.equals("KHOÁ");
    }

    private String roleIdToDisplay(String roleId) {
        String role = normalizeRoleId(roleId);
        if (ROLE_ADMIN.equals(role)) {
            return "Quản trị viên";
        }
        if (ROLE_MANAGER.equals(role)) {
            return "Quản lý cửa hàng";
        }
        if (ROLE_STAFF_WAREHOUSE.equals(role)) {
            return "Nhân viên kho";
        }
        return "Nhân viên bán hàng";
    }

    private JLabel createLabel(String text, Color color) {
        return createLabel(text, color, false);
    }

    private JLabel createLabel(String text, Color color, boolean isBold) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", isBold ? Font.BOLD : Font.PLAIN, 13));
        l.setForeground(color);
        return l;
    }

    private JLabel createBadge(String role) {
        Color bg;
        Color fg;
        if ("Quản trị viên".equals(role)) {
            bg = new Color(255, 235, 238);
            fg = new Color(220, 53, 69);
        } else if ("Quản lý cửa hàng".equals(role)) {
            bg = new Color(255, 248, 225);
            fg = new Color(245, 158, 11);
        } else if ("Nhân viên kho".equals(role)) {
            bg = new Color(243, 232, 255);
            fg = new Color(147, 51, 234);
        } else {
            bg = new Color(237, 242, 255);
            fg = primaryBlue;
        }
        JLabel badge = new JLabel(role, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(fg);
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(5, 15, 5, 15));
        return badge;
    }

    private JLabel createStoreBadge(String storeId, String storeName, boolean adminAccount) {
        String text = shortText(getStoreDisplayName(storeId, storeName, adminAccount), 24);
        Color fg = adminAccount ? new Color(194, 65, 12) : storeColor(storeId);
        JLabel label = new JLabel(text, SwingConstants.LEFT);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(fg);
        return label;
    }

    private String getStoreDisplayName(String storeId, String storeName, boolean adminAccount) {
        if (adminAccount) {
            return "Hệ thống";
        }
        String id = fallback(storeId, "Chưa phân chi nhánh");
        String name = fallback(storeName, "");
        if ("Chưa phân chi nhánh".equals(id)) {
            return id;
        }
        return name.isBlank() ? id : id + " - " + name;
    }

    private String shortText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private Color storeColor(String storeId) {
        String id = fallback(storeId, "");
        int hash = Math.abs(id.hashCode());
        Color[] colors = {
            new Color(37, 99, 235),
            new Color(5, 150, 105),
            new Color(147, 51, 234),
            new Color(217, 119, 6),
            new Color(14, 116, 144),
            new Color(190, 24, 93)
        };
        return colors[hash % colors.length];
    }

    private JLabel createAdminProtectedBadge() {
        Color bg = new Color(255, 247, 237);
        Color fg = new Color(234, 88, 12);
        JLabel badge = new JLabel("Bảo vệ", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setForeground(fg);
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(5, 12, 5, 12));
        return badge;
    }

    private JLabel createOnlineStatusBadge(String onlineStatus) {
        String status = onlineStatus == null ? "OFFLINE" : onlineStatus.trim().toUpperCase();
        boolean online = status.equals("ONLINE") || status.equals("ACTIVE") || status.equals("ĐANG HOẠT ĐỘNG") || status.equals("DANG_HOAT_DONG");
        String text = online ? "Online" : "Offline";
        Color bg = online ? new Color(220, 252, 231) : new Color(254, 226, 226);
        Color fg = online ? new Color(5, 150, 105) : new Color(220, 38, 38);
        JLabel badge = new JLabel("<html><span style='font-size:13px;'>●</span> " + text + "</html>", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setForeground(fg);
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(5, 12, 5, 12));
        return badge;
    }

    private JPanel createRoleCard(String title, String desc, ButtonGroup group, JRadioButton rb) {
        JPanel card = new RoundedPanel(10, cardWhite);
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(borderGray, 10), new EmptyBorder(10, 12, 10, 12)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 66));
        rb.setBackground(cardWhite);
        group.add(rb);
        JLabel lblText = new JLabel(
                "<html><b style='color:#2B3674; font-size:12px;'>" + escapeHtml(title)
                + "</b><br><span style='color:#A3AED0; font-size:10px;'>" + escapeHtml(desc)
                + "</span></html>"
        );
        card.add(rb, BorderLayout.WEST);
        card.add(lblText, BorderLayout.CENTER);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (card.isEnabled() && rb.isEnabled()) {
                    rb.setSelected(true);
                }
            }
        });
        rb.addItemListener(e -> {
            card.setBorder(BorderFactory.createCompoundBorder(
                    new RoundBorder(rb.isSelected() ? primaryBlue : borderGray, 10),
                    new EmptyBorder(10, 12, 10, 12)
            ));
            card.revalidate();
            card.repaint();
        });
        return card;
    }

    private JButton createCustomButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setPreferredSize(new Dimension(130, 38));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void setupRealtimeSync() {
        EventBus.subscribe(AppDataChangedEvent.class, event -> {
            if (event == null || event.getType() == null) {
                return;
            }
            if (event.getType() == AppEventType.ACCOUNT_SECURITY
                    || event.getType() == AppEventType.EMPLOYEES
                    || event.getType() == AppEventType.STORE_INFO) {
                SwingUtilities.invokeLater(this::initTableData);
            }
        });
    }

    private String clean(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private String fallback(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static class AccountRowData {

        String accountId;
        String userId;
        String employeeId;
        String username;
        String displayName;
        String displayEmail;
        String roleId;
        String displayRole;
        String accountStatus;
        String onlineStatus;
        String storeId;
        String storeName;
        String storeDisplay;
        int isDeleted;
        boolean active;
    }

    private static class StoreItem {

        String storeId;
        String storeName;

        StoreItem(String storeId, String storeName) {
            this.storeId = storeId;
            this.storeName = storeName;
        }

        @Override
        public String toString() {
            String id = storeId == null ? "" : storeId;
            String name = storeName == null || storeName.isBlank() ? "Chi nhánh" : storeName;
            return id + " - " + name;
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

    class DashedBorder implements javax.swing.border.Border {

        private final Color color;
        private final int thickness;
        private final int dashLength;

        public DashedBorder(Color color, int thickness, int dashLength) {
            this.color = color;
            this.thickness = thickness;
            this.dashLength = dashLength;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            Stroke dashed = new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{dashLength}, 0);
            g2.setStroke(dashed);
            g2.drawRoundRect(x, y, width - 1, height - 1, 10, 10);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    class ToggleSwitch extends JComponent {

        private boolean on;

        public ToggleSwitch(boolean on) {
            this.on = on;
            setPreferredSize(new Dimension(46, 24));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        public void setOn(boolean on) {
            this.on = on;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (!isEnabled()) {
                g2.setColor(new Color(226, 232, 240));
                g2.fillRoundRect(0, 0, 46, 24, 24, 24);
                g2.setColor(new Color(248, 250, 252));
                g2.fillOval(on ? 24 : 2, 2, 20, 20);
            } else if (on) {
                g2.setColor(new Color(16, 185, 129));
                g2.fillRoundRect(0, 0, 46, 24, 24, 24);
                g2.setColor(Color.WHITE);
                g2.fillOval(24, 2, 20, 20);
            } else {
                g2.setColor(new Color(203, 213, 225));
                g2.fillRoundRect(0, 0, 46, 24, 24, 24);
                g2.setColor(Color.WHITE);
                g2.fillOval(2, 2, 20, 20);
            }
            g2.dispose();
        }
    }
}
