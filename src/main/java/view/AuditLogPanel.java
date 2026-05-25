package view;

import common.events.AppDataChangedEvent;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import view.components.IconHelper;

public class AuditLogPanel extends JPanel {

    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(100, 116, 139);
    private final Color borderGray = new Color(230, 235, 241);
    private final Color primaryBlue = new Color(37, 99, 235);
    private final Color successGreen = new Color(16, 185, 129);
    private final Color dangerRed = new Color(239, 68, 68);
    private final Color warningOrange = new Color(245, 158, 11);
    private final Color purple = new Color(124, 58, 237);

    private final Color softBlue = new Color(237, 242, 255);
    private final Color softGreen = new Color(236, 253, 245);
    private final Color softRed = new Color(254, 242, 242);
    private final Color softOrange = new Color(255, 247, 237);
    private final Color softPurple = new Color(245, 243, 255);

    private JTable tblLogs;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JComboBox<String> cbActionType;
    private JComboBox<String> cbEntity;
    private JComboBox<String> cbStore;
    private JButton btnFilter;
    private JButton btnRefresh;
    private JButton btnExport;

    private final Map<String, StoreColor> storeColorMap = new LinkedHashMap<>();

    public AuditLogPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents();
        loadStoreFilter();
        loadRealData("Tất cả Hành động", "Tất cả Đối tượng", "Tất cả Chi nhánh", "");

        /*
         * Không dùng real-time cho Audit Log vì bảng log lớn sẽ rất lag.
         * Muốn xem dữ liệu mới thì bấm nút "Cập nhật".
         */
    }

    private void initUI() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Nhật Ký Hoạt Động Hệ Thống (Audit Trail)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);

        JLabel lblSub = new JLabel("Lưu thao tác hệ thống theo tài khoản, module và store_id thực hiện. Không tự reload để tránh lag.");
        lblSub.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSub.setForeground(textGray);

        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        RoundedPanel filterCard = new RoundedPanel(15, cardWhite);
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 15));
        filterCard.setBorder(new EmptyBorder(5, 10, 5, 10));

        txtSearch = createTextField("Tìm tài khoản, IP, store_id, nội dung...");
        txtSearch.setPreferredSize(new Dimension(260, 40));

        cbActionType = new JComboBox<>(new String[]{
            "Tất cả Hành động",
            "ĐĂNG NHẬP",
            "ĐĂNG XUẤT",
            "THÊM MỚI",
            "CẬP NHẬT",
            "XÓA",
            "XUẤT FILE",
            "NHẬP FILE",
            "IN PHIẾU",
            "HỆ THỐNG"
        });
        cbActionType.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cbActionType.setPreferredSize(new Dimension(160, 40));

        cbEntity = new JComboBox<>(new String[]{
            "Tất cả Đối tượng",
            "ACCOUNTS",
            "USERS",
            "ROLES",
            "EMPLOYEES",
            "EMPLOYEE_SHIFT_ASSIGNMENTS",
            "PRODUCTS",
            "INVENTORY",
            "PURCHASE_RECEIPTS",
            "ORDERS",
            "CUSTOMERS",
            "SUPPLIERS",
            "PROMOTIONS",
            "STORES",
            "SYSTEM_CONFIG",
            "HỆ THỐNG"
        });
        cbEntity.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cbEntity.setPreferredSize(new Dimension(190, 40));

        cbStore = new JComboBox<>(new String[]{"Tất cả Chi nhánh"});
        cbStore.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cbStore.setPreferredSize(new Dimension(170, 40));

        btnFilter = createCustomButton("Lọc dữ liệu", new Color(54, 92, 245), Color.WHITE, IconHelper.search(18));
        btnRefresh = createCustomButton("Cập nhật", new Color(23, 162, 184), Color.WHITE, IconHelper.refresh(18));
        btnExport = createCustomButton("Xuất Excel", new Color(165, 177, 194), Color.WHITE, IconHelper.barChart(18));

        filterCard.add(new JLabel("Tìm kiếm: "));
        filterCard.add(txtSearch);
        filterCard.add(new JLabel("Hành động: "));
        filterCard.add(cbActionType);
        filterCard.add(new JLabel("Đối tượng: "));
        filterCard.add(cbEntity);
        filterCard.add(new JLabel("Store ID: "));
        filterCard.add(cbStore);
        filterCard.add(btnFilter);
        filterCard.add(btnRefresh);
        filterCard.add(btnExport);

        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        tableModel = new DefaultTableModel(new Object[]{
            "Thời gian",
            "Store ID",
            "Module",
            "Tài khoản",
            "IP Address",
            "Hành động",
            "Đối tượng",
            "Mã Đối tượng",
            "Chi tiết thay đổi",
            "FullDetails"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tblLogs = new JTable(tableModel);
        tblLogs.removeColumn(tblLogs.getColumnModel().getColumn(9));

        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblLogs);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);

        tableCard.add(scrollPane, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setOpaque(false);
        centerPanel.add(filterCard, BorderLayout.NORTH);
        centerPanel.add(tableCard, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private void initEvents() {
        btnFilter.addActionListener(e -> applyFilter());
        txtSearch.addActionListener(e -> applyFilter());

        /*
         * Combo box không tự query nữa để tránh lag khi chọn.
         * Chọn xong bấm "Lọc dữ liệu".
         */
        btnRefresh.addActionListener(e -> resetAndRefresh());

        tblLogs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showLogDetailDialog();
                }
            }
        });

        btnExport.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "Tính năng Xuất Excel đang được xây dựng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void resetAndRefresh() {
        txtSearch.setText("");
        cbActionType.setSelectedIndex(0);
        cbEntity.setSelectedIndex(0);
        cbStore.setSelectedIndex(0);
        loadStoreFilter();
        applyFilter();
    }

    private void showLogDetailDialog() {
        int row = tblLogs.getSelectedRow();
        if (row < 0) {
            return;
        }

        int modelRow = tblLogs.convertRowIndexToModel(row);

        String time = textAt(modelRow, 0);
        String storeId = textAt(modelRow, 1);
        String module = textAt(modelRow, 2);
        String user = textAt(modelRow, 3);
        String ip = textAt(modelRow, 4);
        String action = textAt(modelRow, 5);
        String entity = textAt(modelRow, 6);
        String entityId = textAt(modelRow, 7);
        String fullDetails = textAt(modelRow, 9);

        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setPreferredSize(new Dimension(720, 500));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        StoreColor storeColor = getStoreColor(storeId);

        StringBuilder html = new StringBuilder("<html><body style='font-family:Segoe UI; font-size:11pt;'>");
        html.append("<h2 style='color:#365CF5; margin-bottom:5px;'>Thông Tin Chi Tiết Giao Dịch</h2>");
        html.append("<hr color='#E2E8F0'>");
        html.append("<table width='100%' cellpadding='5'>");
        html.append("<tr><td width='150'><b>⌚ Thời gian:</b></td><td>").append(escapeHtml(time)).append("</td></tr>");
        html.append("<tr><td><b>🏬 Store ID:</b></td><td><span style='background:#")
                .append(hex(storeColor.bg))
                .append("; color:#")
                .append(hex(storeColor.fg))
                .append("; font-weight:bold;'> ")
                .append(escapeHtml(storeId))
                .append(" </span></td></tr>");
        html.append("<tr><td><b>🧩 Module:</b></td><td>").append(escapeHtml(module)).append("</td></tr>");
        html.append("<tr><td><b>👤 Tài khoản:</b></td><td><span style='color:#E63946; font-weight:bold;'>").append(escapeHtml(user)).append("</span></td></tr>");
        html.append("<tr><td><b>🌐 Địa chỉ IP:</b></td><td>").append(escapeHtml(ip)).append("</td></tr>");
        html.append("<tr><td><b>⚡ Hành động:</b></td><td>").append(escapeHtml(action)).append("</td></tr>");
        html.append("<tr><td><b>📦 Đối tượng:</b></td><td>").append(escapeHtml(entity)).append(" (Mã: ").append(escapeHtml(entityId)).append(")</td></tr>");
        html.append("</table><br><b>📝 Nội dung thay đổi chi tiết:</b>");
        html.append("</body></html>");

        JLabel lblInfo = new JLabel(html.toString());

        JTextArea txtDetails = new JTextArea(fullDetails);
        txtDetails.setFont(new Font("Consolas", Font.PLAIN, 15));
        txtDetails.setLineWrap(true);
        txtDetails.setWrapStyleWord(true);
        txtDetails.setEditable(false);
        txtDetails.setBackground(new Color(248, 249, 252));
        txtDetails.setBorder(new EmptyBorder(15, 15, 15, 15));

        JScrollPane sp = new JScrollPane(txtDetails);
        sp.setBorder(BorderFactory.createLineBorder(borderGray));

        panel.add(lblInfo, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, panel, "Truy vết hệ thống (Audit Trail)", JOptionPane.PLAIN_MESSAGE);
    }

    private void applyFilter() {
        String action = String.valueOf(cbActionType.getSelectedItem());
        String entity = String.valueOf(cbEntity.getSelectedItem());
        String store = String.valueOf(cbStore.getSelectedItem());
        String keyword = txtSearch.getText();
        loadRealData(action, entity, store, keyword);
    }

    private void loadStoreFilter() {
        if (cbStore == null) {
            return;
        }

        Object selected = cbStore.getSelectedItem();

        cbStore.removeAllItems();
        cbStore.addItem("Tất cả Chi nhánh");

        ensureAuditColumns();

        String sql = ""
                + "SELECT DISTINCT NVL(store_id, 'CENTRAL') AS store_id "
                + "FROM AUDIT_LOG "
                + "WHERE NVL(is_deleted, 0) = 0 "
                + "ORDER BY store_id";

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String storeId = normalizeStoreId(rs.getString("store_id"));
                if (storeId == null) {
                    storeId = "CENTRAL";
                }

                cbStore.addItem(storeId);
                getStoreColor(storeId);
            }

        } catch (Exception e) {
            cbStore.addItem("CENTRAL");
        }

        if (selected != null) {
            cbStore.setSelectedItem(selected);
        }

        if (cbStore.getSelectedItem() == null) {
            cbStore.setSelectedIndex(0);
        }
    }

    private void loadRealData(String actionFilter, String entityFilter, String storeFilter, String searchKeyword) {
        int selectedRow = tblLogs.getSelectedRow();
        tableModel.setRowCount(0);

        ensureAuditColumns();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ")
                .append("TO_CHAR(a.CREATED_AT, 'DD/MM/YYYY HH24:MI:SS') AS log_time, ")
                .append("NVL(acc.username, a.account_id) AS username, ")
                .append("NVL(a.IP_ADDRESS, 'Localhost') AS ip_address, ")
                .append("a.ACTION_TYPE, ")
                .append("a.ENTITY_TYPE, ")
                .append("a.ENTITY_ID, ")
                .append("a.NEW_VALUE, ")
                .append("a.OLD_VALUE, ")
                .append("a.REASON, ")
                .append("NVL(a.STORE_ID, 'CENTRAL') AS store_id, ")
                .append("NVL(a.MODULE_NAME, 'Hệ thống') AS module_name, ")
                .append("(CASE ")
                .append("  WHEN a.ENTITY_TYPE = 'ACCOUNTS' THEN (SELECT u.full_name FROM ACCOUNTS ac JOIN USERS u ON ac.user_id = u.user_id WHERE ac.account_id = a.ENTITY_ID FETCH FIRST 1 ROWS ONLY) ")
                .append("  WHEN a.ENTITY_TYPE = 'EMPLOYEES' THEN (SELECT employee_name FROM EMPLOYEES WHERE employee_id = a.ENTITY_ID FETCH FIRST 1 ROWS ONLY) ")
                .append("  WHEN a.ENTITY_TYPE = 'PRODUCTS' THEN (SELECT product_name FROM PRODUCTS WHERE product_id = a.ENTITY_ID FETCH FIRST 1 ROWS ONLY) ")
                .append("  WHEN a.ENTITY_TYPE = 'CUSTOMERS' THEN (SELECT customer_name FROM CUSTOMERS WHERE customer_id = a.ENTITY_ID FETCH FIRST 1 ROWS ONLY) ")
                .append("  WHEN a.ENTITY_TYPE = 'ROLES' THEN (SELECT role_name FROM ROLES WHERE role_id = a.ENTITY_ID FETCH FIRST 1 ROWS ONLY) ")
                .append("  ELSE CAST(a.ENTITY_ID AS NVARCHAR2(100)) ")
                .append("END) AS target_name ")
                .append("FROM AUDIT_LOG a ")
                .append("LEFT JOIN ACCOUNTS acc ON a.ACCOUNT_ID = acc.ACCOUNT_ID ")
                .append("WHERE NVL(a.IS_DELETED, 0) = 0 ");

        java.util.List<Object> params = new ArrayList<>();

        if (actionFilter != null && !"Tất cả Hành động".equals(actionFilter)) {
            sql.append(" AND UPPER(a.ACTION_TYPE) = UPPER(?) ");
            params.add(actionFilter);
        }

        if (entityFilter != null && !"Tất cả Đối tượng".equals(entityFilter)) {
            sql.append(" AND UPPER(a.ENTITY_TYPE) = UPPER(?) ");
            params.add(entityFilter);
        }

        String selectedStoreId = normalizeStoreId(storeFilter);
        if (selectedStoreId != null && !"Tất cả Chi nhánh".equalsIgnoreCase(storeFilter)) {
            sql.append(" AND UPPER(NVL(a.STORE_ID, 'CENTRAL')) = UPPER(?) ");
            params.add(selectedStoreId);
        }

        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            sql.append(" AND (")
                    .append("LOWER(NVL(acc.username, a.account_id)) LIKE LOWER(?) ")
                    .append("OR LOWER(NVL(a.IP_ADDRESS, 'Localhost')) LIKE LOWER(?) ")
                    .append("OR LOWER(NVL(a.STORE_ID, 'CENTRAL')) LIKE LOWER(?) ")
                    .append("OR LOWER(NVL(a.MODULE_NAME, '')) LIKE LOWER(?) ")
                    .append("OR LOWER(NVL(a.REASON, '')) LIKE LOWER(?) ")
                    .append("OR LOWER(NVL(a.NEW_VALUE, '')) LIKE LOWER(?) ")
                    .append("OR LOWER(NVL(a.OLD_VALUE, '')) LIKE LOWER(?) ")
                    .append(") ");

            String like = "%" + searchKeyword.trim() + "%";
            for (int i = 0; i < 7; i++) {
                params.add(like);
            }
        }

        sql.append(" ORDER BY a.CREATED_AT DESC FETCH FIRST 500 ROWS ONLY");

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String time = rs.getString("log_time");

                    String user = rs.getString("username");
                    if (user == null || user.trim().isEmpty()) {
                        user = "Hệ thống (SYSTEM)";
                    }

                    String ip = safe(rs.getString("IP_ADDRESS"), "Localhost");
                    String action = safe(rs.getString("ACTION_TYPE"), "");
                    String entity = safe(rs.getString("ENTITY_TYPE"), "");
                    String entityId = safe(rs.getString("ENTITY_ID"), "");
                    String reason = safe(rs.getString("REASON"), "Hệ thống ghi nhận tự động");
                    String targetName = safe(rs.getString("target_name"), entityId);
                    String storeId = safe(normalizeStoreId(rs.getString("store_id")), "CENTRAL");
                    String moduleName = safe(rs.getString("module_name"), resolveModuleName(entity));

                    String newValue = translateReadable(rs.getString("NEW_VALUE"));
                    String oldValue = translateReadable(rs.getString("OLD_VALUE"));

                    String details;
                    String fullDetails;

                    if ("ROLES".equalsIgnoreCase(entity) && "CẬP NHẬT".equalsIgnoreCase(action)) {
                        details = "Chỉnh quyền [" + targetName + "]: " + safe(newValue, "");
                        fullDetails = ""
                                + "🏬 Store ID:\n   " + storeId + "\n\n"
                                + "🧩 Module:\n   " + moduleName + "\n\n"
                                + "🔹 Chức vụ bị tác động:\n   " + targetName + " (" + entityId + ")\n\n"
                                + "🔹 Chi tiết thay đổi:\n   " + safe(newValue, "") + "\n\n"
                                + "🔹 Quyền hạn trước đó:\n   " + safe(oldValue, "") + "\n\n"
                                + "💡 Ghi chú:\n   " + reason;

                    } else if ("CẬP NHẬT".equalsIgnoreCase(action) && oldValue != null && newValue != null) {
                        details = "Cập nhật [" + targetName + "]: " + oldValue + " sang " + newValue;
                        fullDetails = ""
                                + "🏬 Store ID:\n   " + storeId + "\n\n"
                                + "🧩 Module:\n   " + moduleName + "\n\n"
                                + "🔹 Đối tượng tác động:\n   " + targetName + " (" + entityId + ")\n\n"
                                + "🔹 Dữ liệu ban đầu:\n   " + oldValue + "\n\n"
                                + "🔹 Dữ liệu sau khi cập nhật:\n   " + newValue + "\n\n"
                                + "💡 Lý do / Ghi chú:\n   " + reason;

                    } else if (newValue != null && !newValue.isEmpty()) {
                        details = action + " [" + targetName + "]: " + newValue;
                        fullDetails = ""
                                + "🏬 Store ID:\n   " + storeId + "\n\n"
                                + "🧩 Module:\n   " + moduleName + "\n\n"
                                + "🔹 Đối tượng tác động:\n   " + targetName + " (" + entityId + ")\n\n"
                                + "🔹 Dữ liệu mới:\n   " + newValue + "\n\n"
                                + "💡 Lý do / Ghi chú:\n   " + reason;

                    } else if (oldValue != null && !oldValue.isEmpty()) {
                        details = action + " [" + targetName + "]: " + oldValue;
                        fullDetails = ""
                                + "🏬 Store ID:\n   " + storeId + "\n\n"
                                + "🧩 Module:\n   " + moduleName + "\n\n"
                                + "🔹 Đối tượng tác động:\n   " + targetName + " (" + entityId + ")\n\n"
                                + "🔹 Dữ liệu cũ:\n   " + oldValue + "\n\n"
                                + "💡 Lý do / Ghi chú:\n   " + reason;
                    } else {
                        details = action + " trên [" + targetName + "]";
                        fullDetails = ""
                                + "🏬 Store ID:\n   " + storeId + "\n\n"
                                + "🧩 Module:\n   " + moduleName + "\n\n"
                                + "🔹 Đối tượng tác động:\n   " + targetName + " (" + entityId + ")\n\n"
                                + "💡 Lý do / Ghi chú:\n   " + reason;
                    }

                    getStoreColor(storeId);

                    tableModel.addRow(new Object[]{
                        time,
                        storeId,
                        moduleName,
                        user,
                        ip,
                        action,
                        entity,
                        entityId,
                        details,
                        fullDetails
                    });
                }
            }

            if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
                tblLogs.setRowSelectionInterval(selectedRow, selectedRow);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupTableStyle() {
        tblLogs.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tblLogs.setRowHeight(44);
        tblLogs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblLogs.setShowVerticalLines(false);
        tblLogs.setShowHorizontalLines(false);
        tblLogs.setIntercellSpacing(new Dimension(0, 0));
        tblLogs.setSelectionBackground(new Color(219, 234, 254));
        tblLogs.setSelectionForeground(textDark);
        tblLogs.setFillsViewportHeight(true);
        tblLogs.setAutoCreateRowSorter(true);

        JTableHeader header = tblLogs.getTableHeader();
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);
        header.setBackground(new Color(243, 246, 250));
        header.setForeground(Color.BLACK);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));

        int[] widths = {155, 135, 160, 135, 115, 120, 125, 150, 560};
        for (int i = 0; i < widths.length; i++) {
            tblLogs.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

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
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                label.setFont(new Font("Segoe UI", column == 0 || column == 2 ? Font.BOLD : Font.PLAIN, 13));

                if (column == 8) {
                    label.setHorizontalAlignment(SwingConstants.LEFT);
                } else {
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                }

                if (selected) {
                    label.setBackground(new Color(219, 234, 254));
                    label.setForeground(textDark);
                } else {
                    label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));

                    if (column == 0) {
                        label.setForeground(primaryBlue);
                    } else if (column == 2) {
                        label.setForeground(purple);
                    } else {
                        label.setForeground(Color.BLACK);
                    }
                }

                return label;
            }
        };

        DefaultTableCellRenderer storeRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);

                String storeId = value == null ? "CENTRAL" : value.toString();
                StoreColor color = getStoreColor(storeId);

                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(new Font("Segoe UI", Font.BOLD, 12));
                label.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createEmptyBorder(6, 8, 6, 8),
                        BorderFactory.createLineBorder(selected ? new Color(96, 165, 250) : color.border)
                ));

                if (selected) {
                    label.setBackground(new Color(219, 234, 254));
                    label.setForeground(textDark);
                } else {
                    label.setBackground(color.bg);
                    label.setForeground(color.fg);
                }

                label.setText(storeId);
                label.setToolTipText(storeId);

                return label;
            }
        };

        DefaultTableCellRenderer actionRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);

                String action = value == null ? "" : value.toString().toUpperCase();

                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));

                if (selected) {
                    label.setBackground(new Color(219, 234, 254));
                    label.setForeground(textDark);
                } else if (action.contains("THÊM") || action.contains("NHẬP")) {
                    label.setBackground(softGreen);
                    label.setForeground(successGreen);
                } else if (action.contains("CẬP NHẬT") || action.contains("UPDATE")) {
                    label.setBackground(softBlue);
                    label.setForeground(primaryBlue);
                } else if (action.contains("XÓA") || action.contains("DELETE")) {
                    label.setBackground(softRed);
                    label.setForeground(dangerRed);
                } else if (action.contains("XUẤT") || action.contains("IN")) {
                    label.setBackground(softOrange);
                    label.setForeground(warningOrange);
                } else if (action.contains("ĐĂNG")) {
                    label.setBackground(softPurple);
                    label.setForeground(purple);
                } else {
                    label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    label.setForeground(textDark);
                }

                return label;
            }
        };

        for (int i = 0; i < tblLogs.getColumnCount(); i++) {
            tblLogs.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
            tblLogs.getColumnModel().getColumn(i).setCellRenderer(bodyRenderer);
        }

        tblLogs.getColumnModel().getColumn(1).setCellRenderer(storeRenderer);
        tblLogs.getColumnModel().getColumn(5).setCellRenderer(actionRenderer);
    }

    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(borderGray, 8), new EmptyBorder(5, 10, 5, 10)));
        return txt;
    }

    private JButton createCustomButton(String t, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(t);
        if (icon != null) {
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH)));
        }
        btn.setIconTextGap(8);
        btn.setHorizontalAlignment(SwingConstants.CENTER);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setPreferredSize(new Dimension(130, 40));
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
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);
                super.paint(g2, c);
                g2.dispose();
            }
        });
        return btn;
    }

    private void ensureAuditColumns() {
        try (Connection con = common.db.DatabaseConnection.getConnection()) {
            addColumnIfMissing(con, "STORE_ID", "VARCHAR2(30)");
            addColumnIfMissing(con, "MODULE_NAME", "NVARCHAR2(100)");
        } catch (Exception e) {
            System.err.println("Không thể kiểm tra cột AUDIT_LOG: " + e.getMessage());
        }
    }

    private void addColumnIfMissing(Connection con, String columnName, String columnType) {
        String checkSql = ""
                + "SELECT COUNT(*) "
                + "FROM USER_TAB_COLUMNS "
                + "WHERE TABLE_NAME = 'AUDIT_LOG' "
                + "  AND COLUMN_NAME = ?";

        try (PreparedStatement ps = con.prepareStatement(checkSql)) {
            ps.setString(1, columnName.toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return;
                }
            }

            try (Statement st = con.createStatement()) {
                st.executeUpdate("ALTER TABLE AUDIT_LOG ADD (" + columnName + " " + columnType + ")");
            }

        } catch (Exception e) {
            System.err.println("Bỏ qua thêm cột " + columnName + ": " + e.getMessage());
        }
    }

    private String translateReadable(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("R_ADMIN_ALL", "Quản trị viên")
                .replace("R_STORE_MNG", "Quản lý cửa hàng")
                .replace("R_STAFF_SALE", "Nhân viên bán hàng")
                .replace("R_STAFF_STOCK", "Nhân viên kho")
                .replace("R_STAFF_VIEW_PROD", "Nhân viên kho");
    }

    private String resolveModuleName(String entityType) {
        if (entityType == null) {
            return "Hệ thống";
        }

        String e = entityType.toUpperCase();

        if (e.contains("ACCOUNT") || e.contains("ROLE") || e.contains("USER")) {
            return "Tài khoản & phân quyền";
        }
        if (e.contains("EMPLOYEE") || e.contains("SHIFT")) {
            return "Nhân viên & phân ca";
        }
        if (e.contains("PRODUCT") || e.contains("INVENTORY") || e.contains("PURCHASE") || e.contains("SUPPLIER")) {
            return "Kho hàng";
        }
        if (e.contains("ORDER") || e.contains("INVOICE") || e.contains("SALE")) {
            return "Bán hàng";
        }
        if (e.contains("CUSTOMER")) {
            return "Khách hàng";
        }
        if (e.contains("PROMOTION") || e.contains("VOUCHER")) {
            return "Khuyến mãi";
        }
        if (e.contains("STORE")) {
            return "Chi nhánh";
        }

        return "Hệ thống";
    }

    private StoreColor getStoreColor(String storeId) {
        String key = normalizeStoreId(storeId);
        if (key == null) {
            key = "CENTRAL";
        }

        StoreColor cached = storeColorMap.get(key.toUpperCase());
        if (cached != null) {
            return cached;
        }

        StoreColor color;
        if ("CENTRAL".equalsIgnoreCase(key)) {
            color = new StoreColor(new Color(243, 232, 255), new Color(109, 40, 217), new Color(196, 181, 253));
        } else {
            Color[] bg = {
                new Color(219, 234, 254),
                new Color(220, 252, 231),
                new Color(255, 237, 213),
                new Color(254, 226, 226),
                new Color(224, 242, 254),
                new Color(237, 233, 254),
                new Color(204, 251, 241),
                new Color(254, 249, 195)
            };

            Color[] fg = {
                new Color(30, 64, 175),
                new Color(22, 101, 52),
                new Color(154, 52, 18),
                new Color(153, 27, 27),
                new Color(3, 105, 161),
                new Color(91, 33, 182),
                new Color(15, 118, 110),
                new Color(133, 77, 14)
            };

            Color[] border = {
                new Color(147, 197, 253),
                new Color(134, 239, 172),
                new Color(253, 186, 116),
                new Color(252, 165, 165),
                new Color(125, 211, 252),
                new Color(196, 181, 253),
                new Color(94, 234, 212),
                new Color(253, 224, 71)
            };

            int idx = Math.abs(key.toUpperCase().hashCode()) % bg.length;
            color = new StoreColor(bg[idx], fg[idx], border[idx]);
        }

        storeColorMap.put(key.toUpperCase(), color);
        return color;
    }

    private String normalizeStoreId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String text = value.trim();

        if ("null".equalsIgnoreCase(text)
                || "Tất cả Chi nhánh".equalsIgnoreCase(text)
                || "Tất cả chi nhánh".equalsIgnoreCase(text)
                || "Chưa xác định".equalsIgnoreCase(text)) {
            return null;
        }

        if (text.contains(" - ")) {
            return text.substring(0, text.indexOf(" - ")).trim();
        }

        return text;
    }

    private String textAt(int modelRow, int col) {
        Object value = tableModel.getValueAt(modelRow, col);
        return value == null ? "" : value.toString();
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
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

    private String hex(Color color) {
        return String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    static class StoreColor {

        Color bg;
        Color fg;
        Color border;

        StoreColor(Color bg, Color fg, Color border) {
            this.bg = bg;
            this.fg = fg;
            this.border = border;
        }
    }

    class RoundedPanel extends JPanel {

        private int r;
        private Color bg;

        public RoundedPanel(int r, Color bg) {
            this.r = r;
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
            g2.dispose();
        }
    }

    class RoundBorder implements javax.swing.border.Border {

        private Color c;
        private int r;

        public RoundBorder(Color c, int r) {
            this.c = c;
            this.r = r;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.c);
            g2.drawRoundRect(x, y, w - 1, h - 1, r, r);
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
