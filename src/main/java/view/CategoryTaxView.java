package view;

import common.db.DatabaseConnection;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import view.components.IconHelper;

public class CategoryTaxView extends JPanel {

    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color primaryBlue = new Color(54, 92, 245);
    private final Color successGreen = new Color(34, 197, 94);
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color borderGray = new Color(230, 235, 241);

    private JTextField txtId, txtName, txtDesc;
    private JComboBox<String> cbVat;
    private JTextField txtSearch;

    private JTable tblCategories;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch;

    private boolean hasVatRateColumn = false;
    private boolean hasStatusColumn = false;

    public CategoryTaxView() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        refreshCategorySchemaFlags();
        initUI();
        initEvents();
        loadCategoriesFromDb("");
    }

    private void refreshCategorySchemaFlags() {
        hasVatRateColumn = hasColumn("CATEGORIES", "VAT_RATE");
        hasStatusColumn = hasColumn("CATEGORIES", "STATUS");
    }

    private boolean hasColumn(String tableName, String columnName) {
        String sql = """
            SELECT COUNT(*)
            FROM USER_TAB_COLUMNS
            WHERE TABLE_NAME = ?
              AND COLUMN_NAME = ?
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tableName.toUpperCase());
            ps.setString(2, columnName.toUpperCase());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            System.err.println("[CategoryTaxView] hasColumn error: " + e.getMessage());
            return false;
        }
    }

    private void initUI() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Danh Mục & Thuế VAT");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);

        JLabel lblSub = new JLabel("Quản lý nhóm sản phẩm và định mức thuế GTGT áp dụng");
        lblSub.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSub.setForeground(textGray);

        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        toolPanel.setOpaque(false);

        txtSearch = createTextField("Tìm kiếm danh mục...");
        txtSearch.setPreferredSize(new Dimension(300, 42));

        btnSearch = createCustomButton("Tìm kiếm", primaryBlue, Color.WHITE, IconHelper.search(16));

        toolPanel.add(txtSearch);
        toolPanel.add(btnSearch);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(toolPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(25, 0));
        centerPanel.setOpaque(false);

        RoundedPanel formCard = new RoundedPanel(20, cardWhite);
        formCard.setPreferredSize(new Dimension(380, 0));
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        txtId = createTextField("Mã tự động...");
        txtId.setEnabled(false);

        txtName = createTextField("Nhập tên danh mục...");
        txtDesc = createTextField("Nhập mô tả chi tiết...");

        cbVat = new JComboBox<>(new String[]{"Chưa set", "0%", "5%", "8%", "10%"});
        cbVat.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cbVat.setBackground(Color.WHITE);
        cbVat.setForeground(textDark);
        cbVat.setPreferredSize(new Dimension(200, 40));

        int y = 0;

        formCard.add(createLabel("Mã danh mục"), addGbc(gbc, y++, 5));
        formCard.add(txtId, addGbc(gbc, y++, 15));

        formCard.add(createLabel("Tên danh mục (*)"), addGbc(gbc, y++, 5));
        formCard.add(txtName, addGbc(gbc, y++, 15));

        formCard.add(createLabel("Mức Thuế VAT"), addGbc(gbc, y++, 5));
        formCard.add(cbVat, addGbc(gbc, y++, 15));

        formCard.add(createLabel("Mô tả / Ghi chú"), addGbc(gbc, y++, 5));
        formCard.add(txtDesc, addGbc(gbc, y++, 25));

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        btnGrid.setOpaque(false);

        btnAdd = createCustomButton("Thêm mới", successGreen, Color.WHITE, IconHelper.add(18));
        btnUpdate = createCustomButton("Cập nhật", new Color(0, 168, 140), Color.WHITE, IconHelper.edit(18));
        btnDelete = createCustomButton("Xóa", new Color(239, 68, 68), Color.WHITE, IconHelper.delete(18));
        btnClear = createCustomButton("Làm mới", new Color(148, 163, 184), Color.WHITE, IconHelper.refresh(18));

        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);

        gbc.gridy = y++;
        formCard.add(btnGrid, gbc);

        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        tableModel = new DefaultTableModel(
                new Object[]{"Mã DM", "Tên Danh Mục", "Thuế VAT", "Mô tả", "Trạng thái"},
                0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tblCategories = new JTable(tableModel);
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblCategories);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        tableCard.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(formCard, BorderLayout.WEST);
        centerPanel.add(tableCard, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private void initEvents() {
        btnSearch.addActionListener(e -> loadCategoriesFromDb(txtSearch.getText().trim()));

        txtSearch.addActionListener(e -> loadCategoriesFromDb(txtSearch.getText().trim()));

        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() != KeyEvent.VK_ENTER) {
                    loadCategoriesFromDb(txtSearch.getText().trim());
                }
            }
        });

        btnAdd.addActionListener(e -> addCategory());
        btnUpdate.addActionListener(e -> updateCategory());
        btnDelete.addActionListener(e -> deleteCategory());
        btnClear.addActionListener(e -> {
            clearForm();
            loadCategoriesFromDb("");
        });

        tblCategories.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                fillFormFromSelectedRow();
            }
        });
    }

    private void loadCategoriesFromDb(String keyword) {
        tableModel.setRowCount(0);

        refreshCategorySchemaFlags();

        String vatExpr = hasVatRateColumn
                ? "TO_CHAR(vat_rate)"
                : "NULL";

        String statusExpr = hasStatusColumn
                ? "TO_CHAR(status)"
                : "NULL";

        String sql = """
            SELECT category_id,
                   category_name,
                   description,
                   %s AS vat_rate_text,
                   %s AS status_text
            FROM CATEGORIES
            WHERE NVL(is_deleted, 0) = 0
        """.formatted(vatExpr, statusExpr);

        boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();

        if (hasKeyword) {
            sql += """
                 AND (
                        LOWER(category_id) LIKE ?
                     OR LOWER(category_name) LIKE ?
                     OR LOWER(NVL(description, '')) LIKE ?
                 )
            """;
        }

        sql += " ORDER BY category_id";

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            if (hasKeyword) {
                String kw = "%" + keyword.trim().toLowerCase() + "%";
                ps.setString(1, kw);
                ps.setString(2, kw);
                ps.setString(3, kw);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String vatText = formatVat(rs.getString("vat_rate_text"));
                    String statusText = rs.getString("status_text");

                    if (statusText == null || statusText.trim().isEmpty()) {
                        statusText = "Hoạt động";
                    }

                    tableModel.addRow(new Object[]{
                        rs.getString("category_id"),
                        rs.getString("category_name"),
                        vatText,
                        rs.getString("description"),
                        statusText
                    });
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi tải danh mục:\n" + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void addCategory() {
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        Integer vatValue = parseVatValue(cbVat.getSelectedItem());

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên danh mục.");
            txtName.requestFocus();
            return;
        }

        String newId = generateNextCategoryId();

        String sql;

        if (hasVatRateColumn && hasStatusColumn) {
            sql = """
                INSERT INTO CATEGORIES (
                    category_id, category_name, description, vat_rate, status, is_deleted
                )
                VALUES (?, ?, ?, ?, N'Hoạt động', 0)
            """;
        } else if (hasVatRateColumn) {
            sql = """
                INSERT INTO CATEGORIES (
                    category_id, category_name, description, vat_rate, is_deleted
                )
                VALUES (?, ?, ?, ?, 0)
            """;
        } else if (hasStatusColumn) {
            sql = """
                INSERT INTO CATEGORIES (
                    category_id, category_name, description, status, is_deleted
                )
                VALUES (?, ?, ?, N'Hoạt động', 0)
            """;
        } else {
            sql = """
                INSERT INTO CATEGORIES (
                    category_id, category_name, description, is_deleted
                )
                VALUES (?, ?, ?, 0)
            """;
        }

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newId);
            ps.setString(2, name);
            ps.setString(3, desc.isEmpty() ? null : desc);

            if (hasVatRateColumn) {
                if (vatValue == null) {
                    ps.setNull(4, Types.NUMERIC);
                } else {
                    ps.setInt(4, vatValue);
                }
            }

            ps.executeUpdate();

            publishCategoryChanged("CATEGORY_ADDED:" + newId);

            JOptionPane.showMessageDialog(this, "Đã thêm danh mục " + newId + " thành công.");

            clearForm();
            loadCategoriesFromDb(txtSearch.getText().trim());

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi thêm danh mục:\n" + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void updateCategory() {
        String id = txtId.getText().trim();
        String name = txtName.getText().trim();
        String desc = txtDesc.getText().trim();
        Integer vatValue = parseVatValue(cbVat.getSelectedItem());

        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn danh mục cần cập nhật.");
            return;
        }

        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập tên danh mục.");
            txtName.requestFocus();
            return;
        }

        String sql;

        if (hasVatRateColumn && hasStatusColumn) {
            sql = """
                UPDATE CATEGORIES
                SET category_name = ?,
                    description = ?,
                    vat_rate = ?,
                    status = N'Hoạt động',
                    is_deleted = 0
                WHERE category_id = ?
            """;
        } else if (hasVatRateColumn) {
            sql = """
                UPDATE CATEGORIES
                SET category_name = ?,
                    description = ?,
                    vat_rate = ?,
                    is_deleted = 0
                WHERE category_id = ?
            """;
        } else if (hasStatusColumn) {
            sql = """
                UPDATE CATEGORIES
                SET category_name = ?,
                    description = ?,
                    status = N'Hoạt động',
                    is_deleted = 0
                WHERE category_id = ?
            """;
        } else {
            sql = """
                UPDATE CATEGORIES
                SET category_name = ?,
                    description = ?,
                    is_deleted = 0
                WHERE category_id = ?
            """;
        }

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, desc.isEmpty() ? null : desc);

            if (hasVatRateColumn) {
                if (vatValue == null) {
                    ps.setNull(3, Types.NUMERIC);
                } else {
                    ps.setInt(3, vatValue);
                }
                ps.setString(4, id);
            } else {
                ps.setString(3, id);
            }

            int updated = ps.executeUpdate();

            if (updated <= 0) {
                JOptionPane.showMessageDialog(this, "Không tìm thấy danh mục cần cập nhật.");
                return;
            }

            publishCategoryChanged("CATEGORY_UPDATED:" + id);

            JOptionPane.showMessageDialog(this, "Đã cập nhật danh mục thành công.");

            clearForm();
            loadCategoriesFromDb(txtSearch.getText().trim());

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi cập nhật danh mục:\n" + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void deleteCategory() {
        String id = txtId.getText().trim();

        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn danh mục cần xóa.");
            return;
        }

        if (isCategoryUsedByProducts(id)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Danh mục này đang có sản phẩm sử dụng nên không nên xóa.\n"
                    + "Hãy chuyển sản phẩm sang danh mục khác trước.",
                    "Không thể xóa",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Xóa danh mục " + id + "?",
                "Xác nhận",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = """
            UPDATE CATEGORIES
            SET is_deleted = 1
            WHERE category_id = ?
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();

            publishCategoryChanged("CATEGORY_DELETED:" + id);

            JOptionPane.showMessageDialog(this, "Đã xóa danh mục.");

            clearForm();
            loadCategoriesFromDb(txtSearch.getText().trim());

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    this,
                    "Lỗi xóa danh mục:\n" + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private boolean isCategoryUsedByProducts(String categoryId) {
        String sql = """
            SELECT COUNT(*)
            FROM PRODUCTS
            WHERE category_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, categoryId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            return true;
        }
    }

    private String generateNextCategoryId() {
        String sql = """
            SELECT NVL(MAX(TO_NUMBER(REGEXP_SUBSTR(category_id, '[0-9]+'))), 0) + 1
            FROM CATEGORIES
            WHERE REGEXP_LIKE(category_id, '^CAT[0-9]+$')
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return "CAT" + String.format("%03d", rs.getInt(1));
            }

        } catch (Exception e) {
            System.err.println("[CategoryTaxView] generateNextCategoryId error: " + e.getMessage());
        }

        return "CAT001";
    }

    private void fillFormFromSelectedRow() {
        int row = tblCategories.getSelectedRow();

        if (row < 0) {
            return;
        }

        int modelRow = tblCategories.convertRowIndexToModel(row);

        txtId.setText(valueAt(modelRow, 0));
        txtName.setText(valueAt(modelRow, 1));
        cbVat.setSelectedItem(valueAt(modelRow, 2));
        txtDesc.setText(valueAt(modelRow, 3));
    }

    private String valueAt(int row, int col) {
        Object value = tableModel.getValueAt(row, col);
        return value == null ? "" : value.toString();
    }

    private void clearForm() {
        txtId.setText("");
        txtName.setText("");
        txtDesc.setText("");
        cbVat.setSelectedItem("Chưa set");
        tblCategories.clearSelection();
    }

    private Integer parseVatValue(Object selected) {
        if (selected == null) {
            return null;
        }

        String s = selected.toString().trim();

        if (s.isEmpty() || s.equalsIgnoreCase("Chưa set")) {
            return null;
        }

        s = s.replace("%", "").trim();

        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return null;
        }
    }

    private String formatVat(String vatText) {
        if (vatText == null || vatText.trim().isEmpty()) {
            return "Chưa set";
        }

        try {
            double value = Double.parseDouble(vatText.trim());

            if (value == Math.floor(value)) {
                return ((int) value) + "%";
            }

            return value + "%";

        } catch (Exception e) {
            return vatText.trim() + "%";
        }
    }

    private void publishCategoryChanged(String message) {
        try {
            EventBus.publish(new AppDataChangedEvent(AppEventType.PRODUCT_CHANGED, message));
            EventBus.publish(new AppDataChangedEvent(AppEventType.PRODUCTS, message));
        } catch (Exception ignored) {
        }

        try {
            common.realtime.RealtimeNotifier.productsChanged(message);
        } catch (Exception ignored) {
        }
    }

    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(textDark);
        return lbl;
    }

    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setPreferredSize(new Dimension(200, 40));
        txt.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txt.setForeground(textDark);
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 8),
                new EmptyBorder(5, 10, 5, 10)
        ));
        return txt;
    }

    private JButton createCustomButton(String text, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(text);

        if (icon != null) {
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH)));
        }

        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setIconTextGap(8);

        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 15, 15);

                super.paint(g2, c);
                g2.dispose();
            }
        });

        return btn;
    }

    private void setupTableStyle() {
        tblCategories.setRowHeight(44);
        tblCategories.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tblCategories.setShowVerticalLines(false);
        tblCategories.setShowHorizontalLines(false);
        tblCategories.setIntercellSpacing(new Dimension(0, 0));
        tblCategories.setSelectionBackground(new Color(237, 242, 255));
        tblCategories.setSelectionForeground(textDark);

        /*
         * Header căn trái và in đậm cho dễ nhìn.
         */
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.LEFT);
        headerRenderer.setBackground(bgLight);
        headerRenderer.setForeground(textDark);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 8));

        for (int i = 0; i < tblCategories.getColumnCount(); i++) {
            tblCategories.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        /*
         * Renderer chung: căn trái, chữ đậm.
         */
        DefaultTableCellRenderer leftBoldRenderer = new DefaultTableCellRenderer() {
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

                setHorizontalAlignment(SwingConstants.LEFT);
                setFont(new Font("Segoe UI", Font.BOLD, 14));
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));

                if (isSelected) {
                    setBackground(new Color(237, 242, 255));
                    setForeground(textDark);
                } else {
                    setBackground(Color.WHITE);
                    setForeground(textDark);
                }

                return c;
            }
        };

        for (int i = 0; i < tblCategories.getColumnCount(); i++) {
            tblCategories.getColumnModel().getColumn(i).setCellRenderer(leftBoldRenderer);
        }

        /*
         * Cột Mã DM có hình/icon danh mục.
         * Nếu CategoryTableRenderer lỗi thì fallback về renderer căn trái in đậm.
         */
        try {
            tblCategories.getColumnModel()
                    .getColumn(0)
                    .setCellRenderer(new view.components.CategoryTableRenderer(24));
        } catch (Exception ignored) {
            tblCategories.getColumnModel().getColumn(0).setCellRenderer(leftBoldRenderer);
        }

        /*
         * Cột Thuế VAT: căn trái, chữ đậm, màu rõ.
         */
        DefaultTableCellRenderer vatRenderer = new DefaultTableCellRenderer() {
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

                setHorizontalAlignment(SwingConstants.LEFT);
                setFont(new Font("Segoe UI", Font.BOLD, 14));
                setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 8));

                String text = value == null ? "" : value.toString();

                if ("Chưa set".equalsIgnoreCase(text)) {
                    setForeground(new Color(180, 83, 9));
                } else {
                    setForeground(new Color(185, 28, 28));
                }

                if (isSelected) {
                    setBackground(new Color(237, 242, 255));
                } else {
                    setBackground(Color.WHITE);
                }

                return c;
            }
        };

        tblCategories.getColumnModel().getColumn(2).setCellRenderer(vatRenderer);

        /*
         * Set width để bảng nhìn cân lại khi căn trái.
         */
        tblCategories.getColumnModel().getColumn(0).setPreferredWidth(120);
        tblCategories.getColumnModel().getColumn(1).setPreferredWidth(240);
        tblCategories.getColumnModel().getColumn(2).setPreferredWidth(110);
        tblCategories.getColumnModel().getColumn(3).setPreferredWidth(390);
        tblCategories.getColumnModel().getColumn(4).setPreferredWidth(140);
    }

    private GridBagConstraints addGbc(GridBagConstraints gbc, int y, int bottom) {
        gbc.gridy = y;
        gbc.insets = new Insets(0, 0, bottom, 0);
        return gbc;
    }

    class RoundedPanel extends JPanel {

        private int radius;
        private Color bg;

        public RoundedPanel(int radius, Color bg) {
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

    class RoundBorder implements javax.swing.border.Border {

        private Color color;
        private int radius;

        public RoundBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
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
 