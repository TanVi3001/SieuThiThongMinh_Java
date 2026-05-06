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
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import model.order.Customer;
import view.components.IconHelper;
import business.sql.sales_order.CustomersSql;

public class CustomerView extends JPanel {

    // ==========================================
    // CẤU HÌNH MÀU SẮC CHUẨN MODERN UI
    // ==========================================
    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color primaryBlue = new Color(67, 97, 238);
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color borderGray = new Color(230, 235, 241);

    // ==========================================
    // KHAI BÁO CÁC THÀNH PHẦN GIAO DIỆN
    // ==========================================
    private JTextField txtId, txtName, txtPhone, txtEmail, txtAddress;
    private JComboBox<String> cbSearch;
    private JTable tblCustomers;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch;

    // Danh sách gợi ý tự động cho ô tìm kiếm
    private List<String> customerSearchList = new ArrayList<>();

    // ==========================================
    // KHỞI TẠO VIEW
    // ==========================================
    public CustomerView() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        loadAutoCompleteData();
        initUI();
        initEvents();
        loadCustomerData();

        // BẮT SỰ KIỆN REAL-TIME TỪ SERVER ĐỔ VỀ
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e.getType() == AppEventType.CUSTOMERS) {
                SwingUtilities.invokeLater(() -> {
                    refreshTable();
                });
            }
        });
    }

    private void loadAutoCompleteData() {
        customerSearchList.clear();
        try {
            List<Customer> list = CustomersSql.getInstance().getCustomersWithOrders();
            for (Customer c : list) {
                if (c.getCustomerName() != null && !c.getCustomerName().isEmpty()) {
                    String phone = c.getPhone() != null ? c.getPhone() : "N/A";
                    customerSearchList.add(c.getCustomerName() + " - " + phone);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);

        JLabel lblTitle = new JLabel("Khách Hàng");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);

        JLabel lblSub = new JLabel("Quản lý thông tin liên hệ và địa chỉ khách hàng");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);

        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        toolPanel.setOpaque(false);

        cbSearch = new JComboBox<>();
        styleSearchBox(cbSearch);
        setupAutoComplete(cbSearch, customerSearchList);

        JPanel searchFieldWrapper = new JPanel(new BorderLayout(5, 0));
        searchFieldWrapper.setBackground(Color.WHITE);
        searchFieldWrapper.setPreferredSize(new Dimension(450, 45));
        searchFieldWrapper.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(new Color(220, 225, 235), 25),
                new EmptyBorder(0, 15, 0, 15)
        ));

        JLabel searchIconLabel = new JLabel(IconHelper.search(16));
        searchFieldWrapper.add(searchIconLabel, BorderLayout.WEST);
        searchFieldWrapper.add(cbSearch, BorderLayout.CENTER);

        btnSearch = createCustomButton("Tìm kiếm", primaryBlue, Color.WHITE, null);
        toolPanel.add(searchFieldWrapper);
        toolPanel.add(btnSearch);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(toolPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- CENTER ---
        JPanel centerPanel = new JPanel(new BorderLayout(20, 0));
        centerPanel.setOpaque(false);

        // FORM TRÁI
        RoundedPanel formCard = new RoundedPanel(20, cardWhite);
        formCard.setPreferredSize(new Dimension(350, 0));
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(20, 25, 20, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;

        txtId = createTextField("Mã tự động...");
        txtId.setEnabled(false);
        txtName = createTextField("Nhập tên khách hàng...");
        txtPhone = createTextField("Nhập số điện thoại...");
        txtEmail = createTextField("Nhập email...");
        txtAddress = createTextField("Nhập địa chỉ...");

        int y = 0;
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 5, 0);
        formCard.add(createLabel("Mã khách hàng"), gbc);
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 15, 0);
        formCard.add(txtId, gbc);

        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 5, 0);
        formCard.add(createLabel("Tên khách hàng (*)"), gbc);
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 15, 0);
        formCard.add(txtName, gbc);

        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 5, 0);
        formCard.add(createLabel("Số điện thoại (*)"), gbc);
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 15, 0);
        formCard.add(txtPhone, gbc);

        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 5, 0);
        formCard.add(createLabel("Email"), gbc);
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 15, 0);
        formCard.add(txtEmail, gbc);

        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 5, 0);
        formCard.add(createLabel("Địa chỉ"), gbc);
        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 30, 0);
        formCard.add(txtAddress, gbc);

        btnAdd = createCustomButton("Thêm", primaryBlue, Color.WHITE, IconHelper.add(20));
        btnUpdate = createCustomButton("Cập nhật", new Color(255, 153, 0), Color.BLACK, IconHelper.edit(20));
        btnDelete = createCustomButton("Xóa", new Color(220, 53, 69), Color.WHITE, IconHelper.delete(20));
        btnClear = createCustomButton("Làm mới", new Color(165, 177, 194), Color.WHITE, IconHelper.refresh(20));

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        btnGrid.setOpaque(false);
        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);

        gbc.gridy = y++;
        gbc.insets = new Insets(0, 0, 0, 0);
        formCard.add(btnGrid, gbc);
        centerPanel.add(formCard, BorderLayout.WEST);

        // BẢNG PHẢI
        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        tableModel = new DefaultTableModel(
                new Object[]{"Mã KH", "Tên khách hàng", "Số điện thoại", "Email", "Địa chỉ"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblCustomers = new JTable(tableModel);
        tblCustomers.setRowHeight(35);
        tblCustomers.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblCustomers.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        tblCustomers.getTableHeader().setBackground(bgLight);
        tblCustomers.getTableHeader().setReorderingAllowed(false);
        tblCustomers.setShowVerticalLines(false);
        tblCustomers.setSelectionBackground(new Color(237, 242, 255));
        tblCustomers.setSelectionForeground(textDark);

        JScrollPane scrollPane = new JScrollPane(tblCustomers);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(tableCard, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    // ==========================================
    // SỰ KIỆN CHO CÁC NÚT VÀ BẢNG
    // ==========================================
    private void initEvents() {
        tblCustomers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int row = tblCustomers.getSelectedRow();
                if (row >= 0) {
                    txtId.setText(tblCustomers.getValueAt(row, 0).toString());
                    txtName.setText(tblCustomers.getValueAt(row, 1).toString());
                    txtPhone.setText(tblCustomers.getValueAt(row, 2).toString());
                    txtEmail.setText(tblCustomers.getValueAt(row, 3).toString());
                    txtAddress.setText(tblCustomers.getValueAt(row, 4).toString());
                }
            }
        });

        // ACTION CHO NÚT THÊM
        btnAdd.addActionListener(e -> {
            Customer c = getCustomerFromForm();
            if (c == null) {
                return;
            }
            c.setCustomerId("CUS" + System.currentTimeMillis()); // Generate Fake ID (Bên Oracle thường dùng Sequence)

            try {
                if (CustomersSql.getInstance().insert(c) > 0) { // Tuỳ theo cấu trúc hàm Insert của nhóm ông
                    SyncVersionDao.bumpVersion("CUSTOMERS");
                    RealtimeClient.send("CUSTOMERS_CHANGED");

                    JOptionPane.showMessageDialog(this, "✅ Thêm khách hàng thành công!");
                    loadCustomerData();
                    btnClear.doClick();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Thêm thất bại, vui lòng kiểm tra lại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // ACTION CHO NÚT CẬP NHẬT
        btnUpdate.addActionListener(e -> {
            String id = txtId.getText().trim();
            if (id.isEmpty() || id.startsWith("Mã")) {
                JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn khách hàng trong bảng để cập nhật!");
                return;
            }
            Customer c = getCustomerFromForm();
            if (c == null) {
                return;
            }
            c.setCustomerId(id);

            try {
                if (CustomersSql.getInstance().update(c) > 0) {
                    SyncVersionDao.bumpVersion("CUSTOMERS");
                    RealtimeClient.send("CUSTOMERS_CHANGED");

                    JOptionPane.showMessageDialog(this, "✅ Cập nhật thông tin thành công!");
                    loadCustomerData();
                    btnClear.doClick();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Cập nhật thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        // ACTION CHO NÚT XÓA
        btnDelete.addActionListener(e -> {
            String id = txtId.getText().trim();
            if (id.isEmpty() || id.startsWith("Mã")) {
                JOptionPane.showMessageDialog(this, "⚠️ Vui lòng chọn khách hàng cần xóa!");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa khách hàng này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    if (CustomersSql.getInstance().delete(id) > 0) {
                        SyncVersionDao.bumpVersion("CUSTOMERS");
                        RealtimeClient.send("CUSTOMERS_CHANGED");

                        JOptionPane.showMessageDialog(this, "✅ Xóa khách hàng thành công!");
                        loadCustomerData();
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
            ((JTextField) cbSearch.getEditor().getEditorComponent()).setText("");
            tblCustomers.clearSelection();
            loadCustomerData();
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

    private Customer getCustomerFromForm() {
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();
        String address = txtAddress.getText().trim();

        if (name.isEmpty() || phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Tên và Số điện thoại khách hàng (*)");
            return null;
        }

        Customer c = new Customer();
        c.setCustomerName(name);
        c.setPhone(phone);
        c.setEmail(email);
        c.setAddress(address);
        return c;
    }

    private void searchAndFilterTable(String keyword) {
        tableModel.setRowCount(0);
        try {
            List<Customer> list = CustomersSql.getInstance().getCustomersWithOrders();
            for (Customer c : list) {
                String name = c.getCustomerName() != null ? c.getCustomerName().toLowerCase() : "";
                String phone = c.getPhone() != null ? c.getPhone() : "";

                if (keyword.isEmpty() || name.contains(keyword) || phone.contains(keyword)) {
                    String id = c.getCustomerId() != null ? c.getCustomerId() : "";
                    String cName = c.getCustomerName() != null ? c.getCustomerName() : "";
                    String email = c.getEmail() != null ? c.getEmail() : "";
                    String address = c.getAddress() != null ? c.getAddress() : "";
                    tableModel.addRow(new Object[]{id, cName, phone, email, address});
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadCustomerData() {
        tableModel.setRowCount(0);
        try {
            List<Customer> list = CustomersSql.getInstance().getCustomersWithOrders();
            for (Customer c : list) {
                String id = c.getCustomerId() != null ? c.getCustomerId() : "";
                String name = c.getCustomerName() != null ? c.getCustomerName() : "";
                String phone = c.getPhone() != null ? c.getPhone() : "";
                String email = c.getEmail() != null ? c.getEmail() : "";
                String address = c.getAddress() != null ? c.getAddress() : "";
                tableModel.addRow(new Object[]{id, name, phone, email, address});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void refreshTable() {
        loadCustomerData();
    }

    private void styleSearchBox(JComboBox<String> cb) {
        cb.setEditable(true);
        cb.setBorder(null);
        cb.setBackground(Color.WHITE);
        ((JTextField) cb.getEditor().getEditorComponent()).setBorder(new EmptyBorder(0, 5, 0, 5));
    }

    private void setupAutoComplete(JComboBox<String> comboBox, List<String> originalItems) {
        JTextField editor = (JTextField) comboBox.getEditor().getEditorComponent();
        for (String item : originalItems) {
            comboBox.addItem(item);
        }
        comboBox.setSelectedItem("");

        editor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN
                        || e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
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
                new RoundBorder(borderGray, 8), new EmptyBorder(5, 10, 5, 10)
        ));
        return txt;
    }

    private JButton createCustomButton(String text, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(text);
        if (icon != null) {
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH)));
            btn.setIconTextGap(8);
        }
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setPreferredSize(new Dimension(130, 45));
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
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 25, 25);
                super.paint(g2, c);
                g2.dispose();
            }
        });
        return btn;
    }

    class RoundedPanel extends JPanel {

        private int radius;
        private Color bgColor;

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
