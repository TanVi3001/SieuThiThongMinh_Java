package view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import view.components.IconHelper;

public class CategoryTaxView extends JPanel {

    // --- BẢNG MÀU & THÔNG SỐ UI ---
    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color primaryBlue = new Color(54, 92, 245);
    private final Color successGreen = new Color(34, 197, 94); // Xanh lá cho kho
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color borderGray = new Color(230, 235, 241);

    private JTextField txtId, txtName, txtDesc;
    private JComboBox<String> cbVat;
    private JTextField txtSearch;

    private JTable tblCategories;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch;

    public CategoryTaxView() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        loadDummyData(); // Data mẫu để bạn xem trước UI
    }

    private void initUI() {
        // ── 1. HEADER ────────────────────────────────────────────────────────
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Danh Mục & Thuế VAT");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Quản lý nhóm sản phẩm và định mức thuế GTGT áp dụng");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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

        // ── 2. BỐ CỤC CHÍNH (FORM + TABLE) ───────────────────────────────────
        JPanel centerPanel = new JPanel(new BorderLayout(25, 0));
        centerPanel.setOpaque(false);

        // --- FORM BÊN TRÁI ---
        RoundedPanel formCard = new RoundedPanel(20, cardWhite);
        formCard.setPreferredSize(new Dimension(380, 0));
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0; gbc.gridx = 0;

        txtId = createTextField("Mã tự động...");
        txtId.setEnabled(false);
        txtName = createTextField("Nhập tên danh mục...");
        txtDesc = createTextField("Nhập mô tả chi tiết...");
        
        cbVat = new JComboBox<>(new String[]{"0%", "5%", "8%", "10%"});
        cbVat.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbVat.setBackground(Color.WHITE);
        cbVat.setPreferredSize(new Dimension(200, 40));

        int y = 0;
        formCard.add(createLabel("Mã danh mục"), addGbc(gbc, y++, 5));
        formCard.add(txtId, addGbc(gbc, y++, 15));
        formCard.add(createLabel("Tên danh mục (*)"), addGbc(gbc, y++, 5));
        formCard.add(txtName, addGbc(gbc, y++, 15));
        formCard.add(createLabel("Mức Thuế VAT (*)"), addGbc(gbc, y++, 5));
        formCard.add(cbVat, addGbc(gbc, y++, 15));
        formCard.add(createLabel("Mô tả / Ghi chú"), addGbc(gbc, y++, 5));
        formCard.add(txtDesc, addGbc(gbc, y++, 25));

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        btnGrid.setOpaque(false);
        btnAdd = createCustomButton("Thêm mới", successGreen, Color.WHITE, IconHelper.add(18));
        btnUpdate = createCustomButton("Cập nhật", new Color(0, 168, 140), Color.WHITE, IconHelper.edit(18));
        btnDelete = createCustomButton("Xóa", new Color(239, 68, 68), Color.WHITE, IconHelper.delete(18));
        btnClear = createCustomButton("Làm mới", new Color(148, 163, 184), Color.WHITE, IconHelper.refresh(18));

        btnGrid.add(btnAdd); btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete); btnGrid.add(btnClear);
        gbc.gridy = y++;
        formCard.add(btnGrid, gbc);

        // --- BẢNG BÊN PHẢI ---
        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        tableModel = new DefaultTableModel(new Object[]{"Mã DM", "Tên Danh Mục", "Thuế VAT", "Mô tả", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
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

        // Event click row table
        tblCategories.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int row = tblCategories.getSelectedRow();
                if (row >= 0) {
                    txtId.setText(tblCategories.getValueAt(row, 0).toString());
                    txtName.setText(tblCategories.getValueAt(row, 1).toString());
                    cbVat.setSelectedItem(tblCategories.getValueAt(row, 2).toString());
                    txtDesc.setText(tblCategories.getValueAt(row, 3) != null ? tblCategories.getValueAt(row, 3).toString() : "");
                }
            }
        });
    }

    private void loadDummyData() {
        tableModel.addRow(new Object[]{"CAT001", "Thực phẩm khô", "8%", "Gạo, mì, đường, gia vị", "Hoạt động"});
        tableModel.addRow(new Object[]{"CAT002", "Đồ uống & Giải khát", "10%", "Nước suối, nước ngọt, bia", "Hoạt động"});
        tableModel.addRow(new Object[]{"CAT003", "Hóa mỹ phẩm", "10%", "Dầu gội, sữa tắm, bột giặt", "Hoạt động"});
        tableModel.addRow(new Object[]{"CAT004", "Thực phẩm tươi sống", "5%", "Thịt, cá, rau củ quả", "Hoạt động"});
    }

    // --- UI HELPERS ---
    private JLabel createLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(textDark);
        return lbl;
    }

    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setPreferredSize(new Dimension(200, 40));
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 8), new EmptyBorder(5, 10, 5, 10)));
        return txt;
    }

    private JButton createCustomButton(String t, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(t);
        if (icon != null) btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(18, 18, 1)));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg); btn.setBackground(bg);
        btn.setCursor(new Cursor(12)); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 15, 15);
                super.paint(g2, c); g2.dispose();
            }
        });
        return btn;
    }

    private void setupTableStyle() {
        tblCategories.setRowHeight(38);
        tblCategories.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblCategories.setShowVerticalLines(false);
        tblCategories.setSelectionBackground(new Color(237, 242, 255));
        tblCategories.setSelectionForeground(textDark);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(bgLight);
        headerRenderer.setForeground(Color.BLACK);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        for (int i = 0; i < tblCategories.getColumnCount(); i++) {
            tblCategories.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        // Cột VAT bôi đỏ nhẹ, in đậm
        DefaultTableCellRenderer vatRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setFont(new Font("Segoe UI", Font.BOLD, 14));
                setForeground(new Color(220, 38, 38)); // Đỏ
                if(isSelected) setBackground(new Color(237, 242, 255));
                else setBackground(Color.WHITE);
                return c;
            }
        };
        tblCategories.getColumnModel().getColumn(2).setCellRenderer(vatRenderer);
    }

    private GridBagConstraints addGbc(GridBagConstraints gbc, int y, int b) {
        gbc.gridy = y; gbc.insets = new Insets(0, 0, b, 0); return gbc;
    }

    class RoundedPanel extends JPanel {
        private int r; private Color bg;
        public RoundedPanel(int r, Color bg) { this.r = r; this.bg = bg; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg); g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r); g2.dispose();
        }
    }

    class RoundBorder implements javax.swing.border.Border {
        private Color c; private int r;
        public RoundBorder(Color c, int r) { this.c = c; this.r = r; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.c); g2.drawRoundRect(x, y, w - 1, h - 1, r, r); g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
        @Override public boolean isBorderOpaque() { return false; }
    }
}