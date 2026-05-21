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
    private final Color softGreen = new Color(236, 253, 245);
    private final Color softRed = new Color(254, 242, 242);

    private JTable tblStores;
    private DefaultTableModel tableModel;
    private JTextField txtSearch, txtMaSieuThi, txtTenSieuThi, txtDiaChi, txtSoDienThoai;
    private JComboBox<String> cbTrangThai;
    private JButton btnAddNew, btnSave, btnClear, btnSoftDelete;
    private JLabel lblTotal, lblActive, lblInactive, lblHint, lblFormTitle;
    private boolean isEditMode = false, hasNameColumn = false, hasStatusColumn = false;

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
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Quản Lý Chuỗi Siêu Thị");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(text);
        JLabel sub = new JLabel("Theo dõi, thêm mới, cập nhật và kiểm soát trạng thái toàn bộ chi nhánh trong hệ thống");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(muted);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(5));
        titleBox.add(sub);

        JButton quickAdd = button("+ Thêm Siêu Thị", green, Color.WHITE);
        quickAdd.setPreferredSize(new Dimension(155, 42));
        quickAdd.addActionListener(e -> prepareAddNewStore());

        p.add(titleBox, BorderLayout.WEST);
        p.add(quickAdd, BorderLayout.EAST);
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
        RoundedPanel card = new RoundedPanel(16, white);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(border), new EmptyBorder(14, 16, 14, 16)));
        JPanel icon = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 24));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        icon.setOpaque(false);
        icon.setPreferredSize(new Dimension(46, 46));
        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 22));
        dot.setForeground(accent);
        icon.add(dot);
        JPanel txt = new JPanel();
        txt.setOpaque(false);
        txt.setLayout(new BoxLayout(txt, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setForeground(muted);
        value.setFont(new Font("Segoe UI", Font.BOLD, 24));
        value.setForeground(text);
        txt.add(t);
        txt.add(Box.createVerticalStrut(5));
        txt.add(value);
        card.add(icon, BorderLayout.WEST);
        card.add(txt, BorderLayout.CENTER);
        return card;
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
        btnAddNew = button("+ Thêm", green, Color.WHITE);
        btnAddNew.setPreferredSize(new Dimension(92, 40));
        txtSearch = field("Tìm kiếm chi nhánh...");
        txtSearch.setPreferredSize(new Dimension(285, 40));
        JButton btnSearch = button("Tìm", blue, Color.WHITE);
        btnSearch.setPreferredSize(new Dimension(86, 40));
        btnSearch.addActionListener(e -> loadStoreData(txtSearch.getText().trim()));
        search.add(btnAddNew);
        search.add(txtSearch);
        search.add(btnSearch);
        bar.add(title, BorderLayout.WEST);
        bar.add(search, BorderLayout.EAST);

        tableModel = new DefaultTableModel(new Object[]{"Mã Cửa Hàng", "Tên Siêu Thị", "Trạng Thái"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tblStores = new JTable(tableModel);
        setupTableStyle();
        JScrollPane sp = new JScrollPane(tblStores);
        sp.setBorder(BorderFactory.createLineBorder(border));
        sp.getViewport().setBackground(Color.WHITE);
        JLabel hint = new JLabel("Gợi ý: Bấm + Thêm để tạo siêu thị mới hoặc click một chi nhánh để chỉnh sửa.");
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
        lblFormTitle = new JLabel("Thông Tin Siêu Thị");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 21));
        lblFormTitle.setForeground(text);
        lblHint = new JLabel("Bấm + Thêm để nhập chi nhánh mới");
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
        txtMaSieuThi = field("VD: ST001"); addField(form, g, y, "Mã siêu thị", txtMaSieuThi); y += 2;
        txtTenSieuThi = field("Nhập tên chi nhánh..."); addField(form, g, y, "Tên siêu thị", txtTenSieuThi); y += 2;
        txtSoDienThoai = field("Nhập số điện thoại..."); addField(form, g, y, "Số điện thoại", txtSoDienThoai); y += 2;
        txtDiaChi = field("Nhập địa chỉ chi nhánh..."); addField(form, g, y, "Địa chỉ", txtDiaChi); y += 2;
        g.gridy = y++; g.insets = new Insets(0, 0, 7, 0); form.add(label("Trạng thái"), g);
        cbTrangThai = new JComboBox<>(new String[]{ACTIVE, INACTIVE});
        cbTrangThai.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbTrangThai.setPreferredSize(new Dimension(0, 42));
        g.gridy = y; g.insets = new Insets(0, 0, 12, 0); form.add(cbTrangThai, g);

        JPanel actions = new JPanel(new GridLayout(4, 1, 0, 10));
        actions.setOpaque(false);
        JButton btnAddFromForm = button("+ Thêm Siêu Thị", green, Color.WHITE);
        btnSave = button("Lưu Thông Tin", blue, Color.WHITE);
        btnClear = button("Làm Mới", new Color(235, 238, 244), text);
        btnSoftDelete = button("Xóa mềm", red, Color.WHITE);
        btnAddFromForm.addActionListener(e -> prepareAddNewStore());
        actions.add(btnAddFromForm);
        actions.add(btnSave);
        actions.add(btnClear);
        actions.add(btnSoftDelete);
        card.add(header, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void addField(JPanel p, GridBagConstraints g, int y, String title, JTextField f) {
        g.gridy = y; g.insets = new Insets(0, 0, 7, 0); p.add(label(title), g);
        g.gridy = y + 1; g.insets = new Insets(0, 0, 14, 0); p.add(f, g);
    }

    private void setupTableStyle() {
        tblStores.setRowHeight(44);
        tblStores.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblStores.setShowVerticalLines(false);
        tblStores.setShowHorizontalLines(true);
        tblStores.setIntercellSpacing(new Dimension(0, 0));
        tblStores.setGridColor(new Color(245, 247, 251));
        tblStores.setSelectionBackground(new Color(219, 234, 254));
        tblStores.setSelectionForeground(text);
        tblStores.setFillsViewportHeight(true);
        tblStores.setAutoCreateRowSorter(true);
        tblStores.getTableHeader().setPreferredSize(new Dimension(0, 42));
        tblStores.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer header = new DefaultTableCellRenderer();
        header.setBackground(navy); header.setForeground(Color.WHITE); header.setFont(new Font("Segoe UI", Font.BOLD, 13)); header.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        DefaultTableCellRenderer body = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, col);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                l.setFont(new Font("Segoe UI", col == 0 ? Font.BOLD : Font.PLAIN, 13));
                if (!selected) { l.setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 252, 255)); l.setForeground(col == 0 ? blue : text); }
                return l;
            }
        };
        DefaultTableCellRenderer status = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, col);
                String s = value == null ? "" : value.toString();
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(new Font("Segoe UI", Font.BOLD, 12));
                l.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                if (!selected) { l.setForeground(ACTIVE.equals(s) ? green : red); l.setBackground(ACTIVE.equals(s) ? softGreen : softRed); }
                return l;
            }
        };
        for (int i = 0; i < tblStores.getColumnCount(); i++) { tblStores.getColumnModel().getColumn(i).setHeaderRenderer(header); tblStores.getColumnModel().getColumn(i).setCellRenderer(body); }
        tblStores.getColumnModel().getColumn(0).setPreferredWidth(130);
        tblStores.getColumnModel().getColumn(1).setPreferredWidth(360);
        tblStores.getColumnModel().getColumn(2).setPreferredWidth(150);
        tblStores.getColumnModel().getColumn(2).setCellRenderer(status);
    }

    private void initEvents() {
        btnAddNew.addActionListener(e -> prepareAddNewStore());
        btnClear.addActionListener(e -> clearForm());
        btnSave.addActionListener(e -> saveStore());
        btnSoftDelete.addActionListener(e -> softDeleteStore());
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { loadStoreData(txtSearch.getText().trim()); }
            public void removeUpdate(DocumentEvent e) { loadStoreData(txtSearch.getText().trim()); }
            public void changedUpdate(DocumentEvent e) { loadStoreData(txtSearch.getText().trim()); }
        });
        tblStores.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblStores.getSelectedRow();
                if (row >= 0) {
                    int modelRow = tblStores.convertRowIndexToModel(row);
                    isEditMode = true;
                    String id = String.valueOf(tableModel.getValueAt(modelRow, 0));
                    txtMaSieuThi.setText(id);
                    txtMaSieuThi.setEnabled(false);
                    lblFormTitle.setText("Thông Tin Siêu Thị");
                    lblHint.setText("Đang cập nhật chi nhánh " + id);
                    loadStoreDetailsToForm(id);
                }
            }
        });
    }

    private void prepareAddNewStore() {
        clearForm();
        isEditMode = false;
        txtMaSieuThi.setText(generateNextStoreId());
        txtMaSieuThi.setEnabled(true);
        lblFormTitle.setText("Thêm Thông Tin Siêu Thị");
        lblHint.setText("Đang nhập chi nhánh mới - bấm Lưu Thông Tin để tạo");
        txtTenSieuThi.requestFocusInWindow();
    }

    private void clearForm() {
        isEditMode = false;
        txtMaSieuThi.setEnabled(true);
        txtMaSieuThi.setText("");
        txtTenSieuThi.setText("");
        txtDiaChi.setText("");
        txtSoDienThoai.setText("");
        cbTrangThai.setSelectedItem(ACTIVE);
        tblStores.clearSelection();
        lblFormTitle.setText("Thông Tin Siêu Thị");
        lblHint.setText("Bấm + Thêm để nhập chi nhánh mới");
    }

    private void loadStoreData(String keyword) {
        tableModel.setRowCount(0); refreshStoreSchemaFlags();
        String name = nameExpr(), stat = statusExpr();
        String sql = "SELECT store_id, " + name + " display_name, " + stat + " display_status, NVL(is_deleted,0) deleted_flag FROM STORES WHERE LOWER(store_id) LIKE LOWER(?) OR LOWER(" + name + ") LIKE LOWER(?) OR LOWER(NVL(address,'')) LIKE LOWER(?) OR LOWER(NVL(phone_number,'')) LIKE LOWER(?) ORDER BY store_id";
        int total = 0, active = 0, inactive = 0;
        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            String kw = "%" + (keyword == null ? "" : keyword) + "%"; for (int i = 1; i <= 4; i++) ps.setString(i, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = safe(rs.getString("store_id"));
                    String display = safe(rs.getString("display_name"));
                    String status = normalizeStatus(rs.getString("display_status"), rs.getInt("deleted_flag"));
                    total++; if (INACTIVE.equals(status)) inactive++; else active++;
                    tableModel.addRow(new Object[]{id, display.isEmpty() ? id : display, status});
                }
            }
        } catch (Exception e) { System.err.println("Lỗi tải danh sách chi nhánh: " + e.getMessage()); }
        updateStats(total, active, inactive);
    }

    private void loadStoreDetailsToForm(String storeId) {
        refreshStoreSchemaFlags();
        String sql = "SELECT address, phone_number, NVL(is_deleted,0) deleted_flag, " + nameExpr() + " display_name, " + statusExpr() + " display_status FROM STORES WHERE store_id = ?";
        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, storeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = safe(rs.getString("display_name"));
                    txtTenSieuThi.setText(name.isEmpty() ? storeId : name);
                    txtDiaChi.setText(safe(rs.getString("address")));
                    txtSoDienThoai.setText(safe(rs.getString("phone_number")));
                    cbTrangThai.setSelectedItem(normalizeStatus(rs.getString("display_status"), rs.getInt("deleted_flag")));
                }
            }
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Không thể tải thông tin chi nhánh: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
    }

    private void saveStore() {
        String id = txtMaSieuThi.getText().trim(), name = txtTenSieuThi.getText().trim(), address = txtDiaChi.getText().trim(), phone = txtSoDienThoai.getText().trim(), status = String.valueOf(cbTrangThai.getSelectedItem());
        if (id.isEmpty() || name.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng nhập mã và tên chi nhánh!", "Cảnh báo", JOptionPane.WARNING_MESSAGE); return; }
        refreshStoreSchemaFlags();
        try (Connection con = common.db.DatabaseConnection.getConnection()) {
            if (isEditMode) updateStore(con, id, name, address, phone, status); else insertStore(con, id, name, address, phone, status);
            business.service.AuditLogService.logAction(isEditMode ? "CẬP NHẬT" : "THÊM MỚI", "STORES", id, "", "Trạng thái: " + status, "Admin cập nhật thông tin chi nhánh");
            JOptionPane.showMessageDialog(this, isEditMode ? "Đã cập nhật thông tin chi nhánh!" : "Đã thêm siêu thị mới!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            clearForm(); loadStoreData(txtSearch.getText().trim()); EventBus.publish(new AppDataChangedEvent(AppEventType.STORE_INFO, "STORE_UPDATED"));
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Lỗi khi lưu dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
    }

    private void insertStore(Connection con, String id, String name, String address, String phone, String status) throws SQLException {
        String columns = "store_id,address,phone_number,is_deleted", values = "?,?,?,?";
        if (hasNameColumn) { columns += ",store_name"; values += ",?"; }
        if (hasStatusColumn) { columns += ",status"; values += ",?"; }
        try (PreparedStatement ps = con.prepareStatement("INSERT INTO STORES (" + columns + ") VALUES (" + values + ")")) {
            int i = 1; ps.setString(i++, id); ps.setString(i++, address); ps.setString(i++, phone); ps.setInt(i++, statusFlag(status)); if (hasNameColumn) ps.setString(i++, name); if (hasStatusColumn) ps.setString(i, status); ps.executeUpdate();
        }
    }

    private void updateStore(Connection con, String id, String name, String address, String phone, String status) throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE STORES SET address=?, phone_number=?, is_deleted=?");
        if (hasNameColumn) sql.append(", store_name=?");
        if (hasStatusColumn) sql.append(", status=?");
        sql.append(" WHERE store_id=?");
        try (PreparedStatement ps = con.prepareStatement(sql.toString())) {
            int i = 1; ps.setString(i++, address); ps.setString(i++, phone); ps.setInt(i++, statusFlag(status)); if (hasNameColumn) ps.setString(i++, name); if (hasStatusColumn) ps.setString(i++, status); ps.setString(i, id); ps.executeUpdate();
        }
    }

    private void softDeleteStore() {
        String id = txtMaSieuThi.getText().trim();
        if (id.isEmpty() || !isEditMode) { JOptionPane.showMessageDialog(this, "Vui lòng chọn một chi nhánh để xóa mềm.", "Cảnh báo", JOptionPane.WARNING_MESSAGE); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn tạm ngưng chi nhánh " + id + "?", "Xác nhận xóa mềm", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        refreshStoreSchemaFlags();
        String sql = hasStatusColumn ? "UPDATE STORES SET is_deleted=1, status=? WHERE store_id=?" : "UPDATE STORES SET is_deleted=1 WHERE store_id=?";
        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (hasStatusColumn) { ps.setString(1, INACTIVE); ps.setString(2, id); } else ps.setString(1, id);
            ps.executeUpdate(); business.service.AuditLogService.logAction("XÓA MỀM", "STORES", id, "", "is_deleted=1", "Admin tạm ngưng chi nhánh"); clearForm(); loadStoreData(txtSearch.getText().trim()); EventBus.publish(new AppDataChangedEvent(AppEventType.STORE_INFO, "STORE_UPDATED"));
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Lỗi khi xóa mềm: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE); }
    }

    private String generateNextStoreId() {
        int max = 0;
        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT store_id FROM STORES"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String id = safe(rs.getString(1));
                String digits = id.replaceAll("\\D+", "");
                if (!digits.isEmpty()) max = Math.max(max, Integer.parseInt(digits));
            }
        } catch (Exception ignored) {}
        return String.format("ST%03d", max + 1);
    }

    private void refreshStoreSchemaFlags() { hasNameColumn = hasColumn("STORES", "STORE_NAME"); hasStatusColumn = hasColumn("STORES", "STATUS"); }
    private boolean hasColumn(String table, String column) { try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME=? AND COLUMN_NAME=?")) { ps.setString(1, table); ps.setString(2, column); try (ResultSet rs = ps.executeQuery()) { return rs.next() && rs.getInt(1) > 0; } } catch (Exception e) { return false; } }
    private String nameExpr() { return hasNameColumn ? "NVL(store_name,NVL(address,store_id))" : "NVL(address,store_id)"; }
    private String statusExpr() { return hasStatusColumn ? "NVL(status,CASE WHEN NVL(is_deleted,0)=1 THEN '" + INACTIVE + "' ELSE '" + ACTIVE + "' END)" : "CASE WHEN NVL(is_deleted,0)=1 THEN '" + INACTIVE + "' ELSE '" + ACTIVE + "' END"; }
    private String normalizeStatus(String status, int deleted) { return deleted == 1 ? INACTIVE : (INACTIVE.equals(status) ? INACTIVE : ACTIVE); }
    private int statusFlag(String status) { return INACTIVE.equals(status) ? 1 : 0; }
    private void updateStats(int total, int active, int inactive) { lblTotal.setText(String.valueOf(total)); lblActive.setText(String.valueOf(active)); lblInactive.setText(String.valueOf(inactive)); }
    private String safe(String value) { return value == null ? "" : value.trim(); }
    private JLabel label(String text) { JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI", Font.BOLD, 12)); l.setForeground(muted); return l; }
    private JTextField field(String placeholder) { JTextField f = new JTextField(); f.setFont(new Font("Segoe UI", Font.PLAIN, 14)); f.putClientProperty("JTextField.placeholderText", placeholder); f.setPreferredSize(new Dimension(0, 42)); f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(border), new EmptyBorder(6, 12, 6, 12))); return f; }
    private JButton button(String text, Color bg, Color fg) { JButton b = new JButton(text); b.setFont(new Font("Segoe UI", Font.BOLD, 13)); b.setBackground(bg); b.setForeground(fg); b.setCursor(new Cursor(Cursor.HAND_CURSOR)); b.setFocusPainted(false); b.setBorderPainted(false); b.setContentAreaFilled(false); b.setPreferredSize(new Dimension(130, 40)); b.setUI(new javax.swing.plaf.basic.BasicButtonUI() { @Override public void paint(Graphics g, JComponent c) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(c.getBackground()); g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10); super.paint(g2, c); g2.dispose(); } }); return b; }

    class RoundedPanel extends JPanel {
        private final int r; private final Color bgColor;
        RoundedPanel(int r, Color bgColor) { this.r = r; this.bgColor = bgColor; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(bgColor); g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r); g2.dispose(); super.paintComponent(g); }
    }
}
