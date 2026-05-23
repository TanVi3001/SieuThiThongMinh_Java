package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.table.*;
import view.components.IconHelper;
import java.util.ArrayList;
import java.util.List;

public class StoreManagementPanel extends JPanel {

    private static final String ACTIVE = "Hoạt động";
    private static final String INACTIVE = "Tạm ngưng";

    private final Color bg = new Color(244, 246, 250);
    private final Color white = Color.WHITE;
    private final Color navy = new Color(31, 42, 68);
    private final Color text = new Color(36, 47, 74);
    private final Color muted = new Color(143, 154, 179);
    private final Color border = new Color(226, 232, 240);
    private final Color blue = new Color(37, 99, 235);
    private final Color green = new Color(16, 185, 129);
    private final Color red = new Color(239, 68, 68);
    private final Color orange = new Color(245, 158, 11);
    private final Color grayBtn = new Color(148, 163, 184);

    private JTable tblStores;
    private DefaultTableModel tableModel;
    private JTextField txtSearch, txtMaChiNhanh, txtTenChiNhanh, txtDiaChi, txtSoDienThoai;
    private JComboBox<String> cbTrangThai;
    private JButton btnSave, btnClear, btnSoftDelete;
    private JLabel lblTotal, lblActive, lblInactive, lblHint, lblFormTitle;
    private boolean isEditMode = false;
    private boolean hasNameColumn = false;
    private boolean hasPhoneColumn = false;
    private boolean hasStatusColumn = false;

    public StoreManagementPanel() {
        setLayout(new BorderLayout(0, 22));
        setBackground(bg);
        setBorder(new EmptyBorder(22, 30, 22, 30));
        refreshStoreSchemaFlags();
        initUI();
        initEvents();
        loadStoreData("");
    }

    private void initUI() {
        add(createHeaderPanel(), BorderLayout.NORTH);
        JPanel body = new JPanel(new BorderLayout(22, 0));
        body.setOpaque(false);
        body.add(createMainPanel(), BorderLayout.CENTER);
        body.add(createFormPanel(), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Quản Lý chi nhánh");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(text);
        JLabel sub = new JLabel("Theo dõi, thêm mới, cập nhật và kiểm soát trạng thái toàn bộ chi nhánh trong hệ thống");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(muted);
        p.add(title);
        p.add(Box.createVerticalStrut(5));
        p.add(sub);
        return p;
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
        JPanel p = new JPanel(new GridLayout(1, 3, 14, 0));
        p.setOpaque(false);
        lblTotal = new JLabel("0");
        lblActive = new JLabel("0");
        lblInactive = new JLabel("0");
        p.add(statCard("Tổng chi nhánh", lblTotal, blue));
        p.add(statCard("Đang hoạt động", lblActive, green));
        p.add(statCard("Tạm ngưng", lblInactive, red));
        return p;
    }

    private JPanel statCard(String title, JLabel value, Color accent) {
        RoundedPanel card = new RoundedPanel(18, withAlpha(accent, 16));
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(15, 16, 15, 16));

        JPanel stripe = new JPanel();
        stripe.setPreferredSize(new Dimension(5, 0));
        stripe.setBackground(accent);

        RoundedPanel icon = new RoundedPanel(14, withAlpha(accent, 42));
        icon.setPreferredSize(new Dimension(46, 46));
        icon.setLayout(new BorderLayout());

        JLabel dot = new JLabel("●", SwingConstants.CENTER);
        dot.setFont(new Font("Segoe UI", Font.BOLD, 22));
        dot.setForeground(accent);
        icon.add(dot, BorderLayout.CENTER);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 13));
        t.setForeground(new Color(71, 85, 105));

        value.setFont(new Font("Segoe UI", Font.BOLD, 24));
        value.setForeground(accent);

        textPanel.add(t);
        textPanel.add(Box.createVerticalStrut(5));
        textPanel.add(value);

        JPanel center = new JPanel(new BorderLayout(12, 0));
        center.setOpaque(false);
        center.add(icon, BorderLayout.WEST);
        center.add(textPanel, BorderLayout.CENTER);

        card.add(stripe, BorderLayout.WEST);
        card.add(center, BorderLayout.CENTER);

        return wrapWithBorder(card);
    }

    private JPanel wrapWithBorder(JPanel panel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));
        wrapper.add(panel, BorderLayout.CENTER);
        return wrapper;
    }

    private Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    private JPanel createTableArea() {
        JPanel area = new JPanel(new BorderLayout(0, 14));
        area.setOpaque(false);
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setOpaque(false);
        JLabel title = new JLabel("Danh sách chi nhánh");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(text);
        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        search.setOpaque(false);
        txtSearch = field("Tìm kiếm chi nhánh...");
        txtSearch.setPreferredSize(new Dimension(285, 40));
        JButton btnSearch = button("Tìm", blue, Color.WHITE, IconHelper.search(16));
        btnSearch.setPreferredSize(new Dimension(92, 40));
        btnSearch.addActionListener(e -> loadStoreData(txtSearch.getText().trim()));
        search.add(txtSearch);
        search.add(btnSearch);
        bar.add(title, BorderLayout.WEST);
        bar.add(search, BorderLayout.EAST);

        tableModel = new DefaultTableModel(new Object[]{
            "Mã Chi Nhánh", "Tên Chi Nhánh", "Số Điện Thoại", "Địa Chỉ", "Trạng Thái"
        }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tblStores = new JTable(tableModel);
        setupTableStyle();
        JScrollPane sp = new JScrollPane(tblStores);
        sp.setBorder(BorderFactory.createLineBorder(border));
        sp.getViewport().setBackground(Color.WHITE);
        JLabel hint = new JLabel("Gợi ý: Bấm nút Thêm ở form bên phải để tạo mới, hoặc click một chi nhánh để chỉnh sửa.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(muted);
        area.add(bar, BorderLayout.NORTH);
        area.add(sp, BorderLayout.CENTER);
        area.add(hint, BorderLayout.SOUTH);
        return area;
    }

    private JPanel createFormPanel() {
        RoundedPanel card = new RoundedPanel(20, white);
        card.setPreferredSize(new Dimension(410, 0));
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(new EmptyBorder(24, 24, 22, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        lblFormTitle = new JLabel("Thông Tin Chi Nhánh");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 21));
        lblFormTitle.setForeground(text);
        lblHint = new JLabel("Bấm Thêm để nhập chi nhánh mới");
        lblHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblHint.setForeground(muted);
        header.add(lblFormTitle);
        header.add(Box.createVerticalStrut(6));
        header.add(lblHint);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        int y = 0;
        txtMaChiNhanh = field("Mã tự sinh");
        txtMaChiNhanh.setEditable(false);
        txtMaChiNhanh.setBackground(new Color(248, 250, 252));
        addField(form, g, y, "Mã chi nhánh", txtMaChiNhanh);
        y += 2;

        txtTenChiNhanh = field("Nhập tên chi nhánh...");
        addField(form, g, y, "Tên chi nhánh", txtTenChiNhanh);
        y += 2;
        txtSoDienThoai = field("Nhập số điện thoại...");
        txtSoDienThoai.setEnabled(hasPhoneColumn);
        txtSoDienThoai.setBackground(hasPhoneColumn ? Color.WHITE : new Color(248, 250, 252));
        addField(form, g, y, "Số điện thoại", txtSoDienThoai);
        y += 2;
        txtDiaChi = field("Nhập địa chỉ chi nhánh...");
        addField(form, g, y, "Địa chỉ", txtDiaChi);
        y += 2;
        g.gridy = y++;
        g.insets = new Insets(0, 0, 7, 0);
        form.add(label("Trạng thái"), g);
        cbTrangThai = new JComboBox<>(new String[]{ACTIVE, INACTIVE});
        cbTrangThai.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbTrangThai.setPreferredSize(new Dimension(0, 42));
        g.gridy = y;
        g.insets = new Insets(0, 0, 12, 0);
        form.add(cbTrangThai, g);

        JPanel formTopWrapper = new JPanel(new BorderLayout());
        formTopWrapper.setOpaque(false);
        formTopWrapper.add(form, BorderLayout.NORTH);

        JPanel actions = new JPanel(new GridLayout(2, 2, 10, 10));
        actions.setOpaque(false);
        JButton btnAddFromForm = button("Thêm", blue, Color.WHITE, IconHelper.add(18));
        btnSave = button("Cập nhật", orange, Color.WHITE, IconHelper.edit(18));
        btnSoftDelete = button("Xóa", red, Color.WHITE, IconHelper.delete(18));
        btnClear = button("Làm mới", grayBtn, Color.WHITE, IconHelper.refresh(18));
        btnAddFromForm.addActionListener(e -> prepareAddNewStore());
        actions.add(btnAddFromForm);
        actions.add(btnSave);
        actions.add(btnSoftDelete);
        actions.add(btnClear);

        card.add(header, BorderLayout.NORTH);
        card.add(formTopWrapper, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void addField(JPanel p, GridBagConstraints g, int y, String title, JTextField f) {
        g.gridy = y;
        g.insets = new Insets(0, 0, 7, 0);
        p.add(label(title), g);
        g.gridy = y + 1;
        g.insets = new Insets(0, 0, 14, 0);
        p.add(f, g);
    }

    private void setupTableStyle() {
        tblStores.setRowHeight(44);
        tblStores.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        tblStores.setShowVerticalLines(false);
        tblStores.setShowHorizontalLines(false);
        tblStores.setIntercellSpacing(new Dimension(0, 0));
        tblStores.setGridColor(new Color(245, 247, 251));
        tblStores.setSelectionBackground(new Color(219, 234, 254));
        tblStores.setSelectionForeground(text);
        tblStores.setFillsViewportHeight(true);
        tblStores.setAutoCreateRowSorter(true);

        JTableHeader tableHeader = tblStores.getTableHeader();
        tableHeader.setPreferredSize(new Dimension(0, 42));
        tableHeader.setReorderingAllowed(false);
        tableHeader.setBackground(new Color(243, 246, 250));
        tableHeader.setForeground(Color.BLACK);
        tableHeader.setFont(new Font("Segoe UI", Font.BOLD, 13));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
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
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                label.setFont(new Font("Segoe UI", Font.PLAIN, 13));

                if (isSelected) {
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
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column
            ) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value == null ? "" : value.toString();
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));

                if (isSelected) {
                    label.setBackground(new Color(219, 234, 254));
                    label.setForeground(text);
                } else if (ACTIVE.equals(status)) {
                    label.setBackground(new Color(220, 252, 231));
                    label.setForeground(new Color(0, 148, 92));
                } else {
                    label.setBackground(new Color(254, 226, 226));
                    label.setForeground(new Color(220, 38, 38));
                }
                return label;
            }
        };

        for (int i = 0; i < tblStores.getColumnCount(); i++) {
            tblStores.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
            tblStores.getColumnModel().getColumn(i).setCellRenderer(bodyRenderer);
        }

        tblStores.getColumnModel().getColumn(0).setPreferredWidth(120);
        tblStores.getColumnModel().getColumn(1).setPreferredWidth(220);
        tblStores.getColumnModel().getColumn(2).setPreferredWidth(140);
        tblStores.getColumnModel().getColumn(3).setPreferredWidth(300);
        tblStores.getColumnModel().getColumn(4).setPreferredWidth(130);
        tblStores.getColumnModel().getColumn(4).setCellRenderer(statusRenderer);
    }

    private void initEvents() {
        btnClear.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveStore());
        btnSoftDelete.addActionListener(e -> softDeleteStore());
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                loadStoreData(txtSearch.getText().trim());
            }

            public void removeUpdate(DocumentEvent e) {
                loadStoreData(txtSearch.getText().trim());
            }

            public void changedUpdate(DocumentEvent e) {
                loadStoreData(txtSearch.getText().trim());
            }
        });
        tblStores.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = tblStores.getSelectedRow();
                if (row >= 0) {
                    row = tblStores.convertRowIndexToModel(row);
                    fillFormFromTable(row);
                }
            }
        });
    }

    private JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(new Color(134, 147, 176));
        return l;
    }

    private JTextField field(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setForeground(text);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                new EmptyBorder(0, 12, 0, 12)
        ));
        f.setPreferredSize(new Dimension(0, 42));
        f.putClientProperty("JTextField.placeholderText", placeholder);
        return f;
    }

    private JButton button(String text, Color bg, Color fg, Icon icon) {
        JButton b = new JButton(text, icon);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(130, 42));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void refreshStoreSchemaFlags() {
        hasNameColumn = hasColumn("STORES", "STORE_NAME");
        hasPhoneColumn = hasColumn("STORES", "PHONE");
        hasStatusColumn = hasColumn("STORES", "STATUS");
    }

    private boolean hasColumn(String table, String column) {
        String sql = """
            SELECT COUNT(*)
            FROM USER_TAB_COLUMNS
            WHERE UPPER(TABLE_NAME) = UPPER(?)
              AND UPPER(COLUMN_NAME) = UPPER(?)
        """;
        try (Connection conn = common.db.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void loadStoreData(String keyword) {
        tableModel.setRowCount(0);
        int total = 0, active = 0, inactive = 0;

        String nameExpr = hasNameColumn ? "store_name" : "address";
        String phoneExpr = hasPhoneColumn ? "phone" : "''";
        String statusExpr = hasStatusColumn ? "NVL(status, 'Hoạt động')" : "'Hoạt động'";

        String sql = "SELECT store_id, " + nameExpr + " AS display_name, " + phoneExpr + " AS phone_no, address, " + statusExpr + " AS status "
                + "FROM stores WHERE NVL(is_deleted,0)=0 "
                + "AND (LOWER(store_id) LIKE ? OR LOWER(" + nameExpr + ") LIKE ? OR LOWER(address) LIKE ?) "
                + "ORDER BY store_id";

        try (Connection conn = common.db.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = "%" + (keyword == null ? "" : keyword.toLowerCase()) + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);
            ps.setString(3, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String status = normalizeStatus(rs.getString("status"));
                    tableModel.addRow(new Object[]{
                        rs.getString("store_id"),
                        rs.getString("display_name"),
                        rs.getString("phone_no"),
                        rs.getString("address"),
                        status
                    });
                    total++;
                    if (ACTIVE.equals(status)) {
                        active++;
                    } else {
                        inactive++;
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải danh sách chi nhánh: " + e.getMessage());
        }
        lblTotal.setText(String.valueOf(total));
        lblActive.setText(String.valueOf(active));
        lblInactive.setText(String.valueOf(inactive));
    }

    private String normalizeStatus(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return ACTIVE;
        }
        String s = raw.trim();
        if (s.equalsIgnoreCase("ACTIVE") || s.equalsIgnoreCase("HOAT_DONG") || s.equalsIgnoreCase("Hoạt động")) {
            return ACTIVE;
        }
        return INACTIVE;
    }

    private String toDbStatus(String ui) {
        return ACTIVE.equals(ui) ? ACTIVE : INACTIVE;
    }

    private void fillFormFromTable(int row) {
        isEditMode = true;
        lblFormTitle.setText("Cập Nhật Chi Nhánh");
        lblHint.setText("Đang chỉnh sửa chi nhánh đã chọn");
        txtMaChiNhanh.setText(String.valueOf(tableModel.getValueAt(row, 0)));
        txtTenChiNhanh.setText(String.valueOf(tableModel.getValueAt(row, 1)));
        txtSoDienThoai.setText(String.valueOf(tableModel.getValueAt(row, 2)));
        txtDiaChi.setText(String.valueOf(tableModel.getValueAt(row, 3)));
        cbTrangThai.setSelectedItem(String.valueOf(tableModel.getValueAt(row, 4)));
    }

    private void prepareAddNewStore() {
        clearForm();
        txtMaChiNhanh.setText(generateStoreId());
        txtTenChiNhanh.requestFocus();
        lblFormTitle.setText("Thêm Chi Nhánh Mới");
        lblHint.setText("Nhập thông tin chi nhánh rồi bấm Cập nhật để lưu");
        isEditMode = false;
    }

    private void clearForm() {
        isEditMode = false;
        tblStores.clearSelection();
        lblFormTitle.setText("Thông Tin Chi Nhánh");
        lblHint.setText("Bấm Thêm để nhập chi nhánh mới");
        txtMaChiNhanh.setText("");
        txtTenChiNhanh.setText("");
        txtSoDienThoai.setText("");
        txtDiaChi.setText("");
        cbTrangThai.setSelectedItem(ACTIVE);
    }

    private void saveStore() {
        String id = txtMaChiNhanh.getText().trim();
        String name = txtTenChiNhanh.getText().trim();
        String phone = txtSoDienThoai.getText().trim();
        String address = txtDiaChi.getText().trim();
        String status = toDbStatus(String.valueOf(cbTrangThai.getSelectedItem()));
        if (id.isEmpty()) {
            id = generateStoreId();
            txtMaChiNhanh.setText(id);
        }
        if (name.isEmpty() || address.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên chi nhánh và địa chỉ.");
            return;
        }
        try (Connection conn = common.db.DatabaseConnection.getConnection()) {
            String sql = isEditMode ? buildUpdateSql() : buildInsertSql();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                if (hasNameColumn) {
                    ps.setString(idx++, name);
                }
                ps.setString(idx++, address);
                if (hasPhoneColumn) {
                    ps.setString(idx++, phone);
                }
                if (hasStatusColumn) {
                    ps.setString(idx++, status);
                }
                ps.setString(idx, id);
                ps.executeUpdate();
                conn.commit();
            }
            String message = isEditMode ? "Cập nhật chi nhánh" : "Thêm chi nhánh";
            JOptionPane.showMessageDialog(this, isEditMode ? "Đã cập nhật chi nhánh." : "Đã thêm chi nhánh.");
            loadStoreData(txtSearch.getText().trim());
            clearForm();

            EventBus.publish(new AppDataChangedEvent(AppEventType.STORE_INFO, message));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi lưu chi nhánh: " + e.getMessage());
        }
    }

    private String buildInsertSql() {
        List<String> cols = new ArrayList<>();
        List<String> qs = new ArrayList<>();
        if (hasNameColumn) {
            cols.add("store_name");
            qs.add("?");
        }
        cols.add("address");
        qs.add("?");
        if (hasPhoneColumn) {
            cols.add("phone");
            qs.add("?");
        }
        if (hasStatusColumn) {
            cols.add("status");
            qs.add("?");
        }
        cols.add("store_id");
        qs.add("?");
        cols.add("is_deleted");
        qs.add("0");
        return "INSERT INTO stores (" + String.join(",", cols) + ") VALUES (" + String.join(",", qs) + ")";
    }

    private String buildUpdateSql() {
        List<String> sets = new ArrayList<>();
        if (hasNameColumn) {
            sets.add("store_name=?");
        }
        sets.add("address=?");
        if (hasPhoneColumn) {
            sets.add("phone=?");
        }
        if (hasStatusColumn) {
            sets.add("status=?");
        }
        return "UPDATE stores SET " + String.join(",", sets) + " WHERE store_id=? AND NVL(is_deleted,0)=0";
    }

    private void softDeleteStore() {
        String id = txtMaChiNhanh.getText().trim();
        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn chi nhánh cần xóa.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this, "Xóa chi nhánh " + id + "?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try (Connection conn = common.db.DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE stores SET is_deleted=1 WHERE store_id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
            conn.commit();
            JOptionPane.showMessageDialog(this, "Đã xóa chi nhánh.");
            loadStoreData(txtSearch.getText().trim());
            clearForm();

            EventBus.publish(new AppDataChangedEvent(AppEventType.STORE_INFO, "Xóa chi nhánh"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi xóa chi nhánh: " + e.getMessage());
        }
    }

    private String generateStoreId() {
        return "ST" + System.currentTimeMillis() % 100000;
    }

    private static class RoundedPanel extends JPanel {

        private final int radius;
        private final Color bg;

        RoundedPanel(int radius, Color bg) {
            this.radius = radius;
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
