package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import view.components.IconHelper;

public class StoreManagementPanel extends JPanel {

    // --- BẢNG MÀU UI CHUẨN ---
    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color borderGray = new Color(230, 235, 241);
    private final Color primaryBlue = new Color(67, 97, 238);

    private JTable tblStores;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;

    // --- FORM COMPONENTS ---
    private JTextField txtMaSieuThi;
    private JTextField txtTenSieuThi;
    private JTextField txtDiaChi;
    private JTextField txtSoDienThoai;
    private JComboBox<String> cbTrangThai;
    private JButton btnSave, btnClear;

    private boolean isEditMode = false;

    public StoreManagementPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents();
        loadStoreData("");
    }

    private void initUI() {
        // ── 1. HEADER ────────────────────────────────────────────────────────
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Quản Lý Chuỗi Siêu Thị");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Thêm mới, cập nhật và tra cứu thông tin các chi nhánh trong hệ thống");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // ── 2. MAIN CONTENT (SPLIT LAYOUT) ───────────────────────────────────
        JPanel contentPanel = new JPanel(new BorderLayout(20, 0));
        contentPanel.setOpaque(false);

        contentPanel.add(createLeftPanel(), BorderLayout.CENTER);
        contentPanel.add(createRightPanel(), BorderLayout.EAST);

        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel() {
        RoundedPanel leftCard = new RoundedPanel(20, cardWhite);
        leftCard.setLayout(new BorderLayout(0, 15));
        leftCard.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Tool bar tìm kiếm
        JPanel toolBar = new JPanel(new BorderLayout());
        toolBar.setOpaque(false);

        JLabel lblListTitle = new JLabel("Danh sách chi nhánh");
        lblListTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblListTitle.setForeground(textDark);

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchBox.setOpaque(false);
        txtSearch = createTextField("Tra cứu tên hoặc mã...");
        txtSearch.setPreferredSize(new Dimension(250, 38));

        JButton btnSearch = createCustomButton("Tìm", primaryBlue, Color.WHITE, IconHelper.search(16));
        btnSearch.setPreferredSize(new Dimension(90, 38));

        searchBox.add(txtSearch);
        searchBox.add(btnSearch);

        toolBar.add(lblListTitle, BorderLayout.WEST);
        toolBar.add(searchBox, BorderLayout.EAST);

        // Table — cột khớp với STORES: store_id, store_name, status
        tableModel = new DefaultTableModel(new Object[]{"Mã Cửa Hàng", "Tên Siêu Thị", "Trạng Thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblStores = new JTable(tableModel);
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblStores);
        scrollPane.setBorder(BorderFactory.createLineBorder(borderGray));
        scrollPane.getViewport().setBackground(Color.WHITE);

        leftCard.add(toolBar, BorderLayout.NORTH);
        leftCard.add(scrollPane, BorderLayout.CENTER);

        return leftCard;
    }

    private JPanel createRightPanel() {
        RoundedPanel rightCard = new RoundedPanel(20, cardWhite);
        rightCard.setPreferredSize(new Dimension(380, 0));
        rightCard.setLayout(new BorderLayout());
        rightCard.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel lblFormTitle = new JLabel("Thông Tin Siêu Thị");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblFormTitle.setForeground(textDark);
        lblFormTitle.setBorder(new EmptyBorder(0, 0, 20, 0));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 15, 0);

        // Mã siêu thị (= STORE_ID)
        gbc.gridy = 0;
        formPanel.add(createFormLabel("Mã siêu thị (Tự động hoặc tự nhập):"), gbc);
        gbc.gridy = 1;
        txtMaSieuThi = createTextField("VD: ST001");
        formPanel.add(txtMaSieuThi, gbc);

        // Tên siêu thị (= STORE_NAME)
        gbc.gridy = 2;
        formPanel.add(createFormLabel("Tên siêu thị:"), gbc);
        gbc.gridy = 3;
        txtTenSieuThi = createTextField("Nhập tên chi nhánh...");
        formPanel.add(txtTenSieuThi, gbc);

        // Số điện thoại (= PHONE_NUMBER)
        gbc.gridy = 4;
        formPanel.add(createFormLabel("Số điện thoại liên hệ:"), gbc);
        gbc.gridy = 5;
        txtSoDienThoai = createTextField("Nhập số điện thoại...");
        formPanel.add(txtSoDienThoai, gbc);

        // Địa chỉ (= ADDRESS)
        gbc.gridy = 6;
        formPanel.add(createFormLabel("Địa chỉ đầy đủ:"), gbc);
        gbc.gridy = 7;
        txtDiaChi = createTextField("Nhập địa chỉ chi nhánh...");
        formPanel.add(txtDiaChi, gbc);

        // Trạng thái (= STATUS)
        gbc.gridy = 8;
        formPanel.add(createFormLabel("Trạng thái hoạt động:"), gbc);
        gbc.gridy = 9;
        cbTrangThai = new JComboBox<>(new String[]{"Hoạt động", "Tạm ngưng"});
        cbTrangThai.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbTrangThai.setPreferredSize(new Dimension(0, 38));
        formPanel.add(cbTrangThai, gbc);

        // Nút bấm
        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        btnClear = createCustomButton("Làm Mới", new Color(235, 238, 244), textDark, null);
        btnSave = createCustomButton("Lưu Thông Tin", primaryBlue, Color.WHITE, null);

        actionPanel.add(btnClear);
        actionPanel.add(btnSave);

        rightCard.add(lblFormTitle, BorderLayout.NORTH);
        rightCard.add(formPanel, BorderLayout.CENTER);
        rightCard.add(actionPanel, BorderLayout.SOUTH);

        return rightCard;
    }

    private void initEvents() {
        btnClear.addActionListener(e -> clearForm());

        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { loadStoreData(txtSearch.getText().trim()); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { loadStoreData(txtSearch.getText().trim()); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { loadStoreData(txtSearch.getText().trim()); }
        });

        tblStores.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblStores.getSelectedRow();
                if (row >= 0) {
                    isEditMode = true;
                    String ma = tableModel.getValueAt(row, 0).toString();
                    txtMaSieuThi.setText(ma);
                    txtMaSieuThi.setEnabled(false); // Không cho sửa khóa chính
                    loadStoreDetailsToForm(ma);
                }
            }
        });

        btnSave.addActionListener(e -> saveStore());
    }

    private void clearForm() {
        isEditMode = false;
        txtMaSieuThi.setText("");
        txtMaSieuThi.setEnabled(true);
        txtTenSieuThi.setText("");
        txtDiaChi.setText("");
        txtSoDienThoai.setText("");
        cbTrangThai.setSelectedIndex(0);
        tblStores.clearSelection();
    }

    // =========================================================================
    // KẾT NỐI DATABASE — dùng đúng tên bảng STORES và cột theo KhoiTaoCacBang.sql
    // =========================================================================

    /**
     * Load danh sách chi nhánh lên bảng.
     * Bảng: STORES  |  Cột: STORE_ID, STORE_NAME, STATUS
     */
    private void loadStoreData(String keyword) {
        tableModel.setRowCount(0);
        // Lọc bỏ bản ghi đã xóa mềm (IS_DELETED = 0)
        String sql = "SELECT store_id, store_name, status "
                   + "FROM STORES "
                   + "WHERE is_deleted = 0 "
                   + "  AND (LOWER(store_name) LIKE LOWER(?) OR LOWER(store_id) LIKE LOWER(?)) "
                   + "ORDER BY store_id ASC";

        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String kw = "%" + keyword + "%";
            ps.setString(1, kw);
            ps.setString(2, kw);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                        rs.getString("store_id"),
                        rs.getString("store_name"),
                        rs.getString("status")
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải danh sách chi nhánh: " + e.getMessage());
        }
    }

    /**
     * Load chi tiết 1 chi nhánh vào form khi click dòng trên bảng.
     * Bảng: STORES  |  Cột: STORE_NAME, ADDRESS, PHONE_NUMBER, STATUS
     */
    private void loadStoreDetailsToForm(String storeId) {
        String sql = "SELECT store_name, address, phone_number, status "
                   + "FROM STORES "
                   + "WHERE store_id = ? AND is_deleted = 0";
        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, storeId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtTenSieuThi.setText(rs.getString("store_name"));
                    txtDiaChi.setText(rs.getString("address"));
                    txtSoDienThoai.setText(rs.getString("phone_number"));
                    String status = rs.getString("status");
                    cbTrangThai.setSelectedItem(status != null ? status : "Hoạt động");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Lưu (INSERT hoặc UPDATE) vào bảng STORES.
     * Bảng: STORES  |  Cột: STORE_ID, STORE_NAME, ADDRESS, PHONE_NUMBER, STATUS, IS_DELETED
     */
    private void saveStore() {
        String ma      = txtMaSieuThi.getText().trim();
        String ten     = txtTenSieuThi.getText().trim();
        String diaChi  = txtDiaChi.getText().trim();
        String sdt     = txtSoDienThoai.getText().trim();
        String trangThai = cbTrangThai.getSelectedItem().toString();

        if (ma.isEmpty() || ten.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Vui lòng nhập mã và tên chi nhánh!",
                "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection con = common.db.DatabaseConnection.getConnection()) {
            if (isEditMode) {
                // ── UPDATE ──────────────────────────────────────────────────
                String sql = "UPDATE STORES "
                           + "SET store_name = ?, address = ?, phone_number = ?, status = ? "
                           + "WHERE store_id = ? AND is_deleted = 0";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, ten);
                    ps.setString(2, diaChi);
                    ps.setString(3, sdt);
                    ps.setString(4, trangThai);
                    ps.setString(5, ma);
                    ps.executeUpdate();
                }

                business.service.AuditLogService.logAction(
                    "CẬP NHẬT", "STORES", ma,
                    "Dữ liệu cũ", "Trạng thái: " + trangThai,
                    "Admin cập nhật thông tin chi nhánh"
                );
                JOptionPane.showMessageDialog(this,
                    "Đã cập nhật thông tin chi nhánh!",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);

            } else {
                // ── INSERT ──────────────────────────────────────────────────
                String sql = "INSERT INTO STORES (store_id, store_name, address, phone_number, status, is_deleted) "
                           + "VALUES (?, ?, ?, ?, ?, 0)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, ma);
                    ps.setString(2, ten);
                    ps.setString(3, diaChi);
                    ps.setString(4, sdt);
                    ps.setString(5, trangThai);
                    ps.executeUpdate();
                }

                business.service.AuditLogService.logAction(
                    "THÊM MỚI", "STORES", ma,
                    "", "Tên: " + ten + ", Trạng thái: " + trangThai,
                    "Admin mở chi nhánh mới"
                );
                JOptionPane.showMessageDialog(this,
                    "Đã mở chi nhánh mới thành công!",
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }

            clearForm();
            loadStoreData(txtSearch.getText().trim());
            EventBus.publish(new AppDataChangedEvent(AppEventType.STORE_INFO, "STORE_UPDATED"));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Lỗi khi lưu dữ liệu (Có thể trùng mã Siêu thị): " + e.getMessage(),
                "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // CÁC HÀM TIỆN ÍCH UI
    // =========================================================================
    private JLabel createFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(textGray);
        return lbl;
    }

    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setPreferredSize(new Dimension(0, 38));
        txt.setBorder(BorderFactory.createCompoundBorder(
            new RoundBorder(borderGray, 8),
            new EmptyBorder(5, 10, 5, 10)
        ));
        return txt;
    }

    private void setupTableStyle() {
        tblStores.setRowHeight(38);
        tblStores.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblStores.setShowVerticalLines(false);
        tblStores.setSelectionBackground(new Color(237, 242, 255));
        tblStores.setSelectionForeground(textDark);
        tblStores.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(textDark);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        for (int i = 0; i < tblStores.getColumnCount(); i++) {
            tblStores.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
    }

    private JButton createCustomButton(String t, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(t);
        if (icon != null) btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(18, 18, 1)));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setPreferredSize(new Dimension(130, 40));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
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

    class RoundedPanel extends JPanel {
        private int r; private Color bg;
        public RoundedPanel(int r, Color bg) { this.r = r; this.bg = bg; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
            g2.dispose();
        }
    }

    class RoundBorder implements javax.swing.border.Border {
        private Color c; private int r;
        public RoundBorder(Color c, int r) { this.c = c; this.r = r; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.c);
            g2.drawRoundRect(x, y, w - 1, h - 1, r, r);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
        @Override public boolean isBorderOpaque() { return false; }
    }
}