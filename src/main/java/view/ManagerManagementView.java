package view;

import business.service.ActivationTokenService;
import business.service.EmailService;
import business.sql.hr_kpi.EmployeeSql;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.employee.Employee;
import view.components.IconHelper;

public class ManagerManagementView extends JPanel {

    private static final Logger logger = Logger.getLogger(ManagerManagementView.class.getName());

    // --- BẢNG MÀU & THÔNG SỐ UI ---
    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color primaryBlue = new Color(54, 92, 245);
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color borderGray = new Color(230, 235, 241);

    private JTextField txtId, txtName, txtPhone, txtEmail;
    private JComboBox<String> cbSearch;
    private JRadioButton rdoMale, rdoFemale;
    private ButtonGroup btngGender;

    private JTable tblManagers;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch;

    private final EmployeeSql employeeSql = new EmployeeSql();

    // Auto-complete list cho Manager
    private List<String> managerNameList = new ArrayList<>();

    public ManagerManagementView() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        loadAutoCompleteData();
        initUI();
        initEvents();
        loadDataToTable();
    }

    private void loadAutoCompleteData() {
        managerNameList.clear();
        try {
            List<Employee> list = employeeSql.selectAll();
            for (Employee e : list) {
                if (("R_STORE_MNG".equals(e.getRoleId()) || "Quản lý cửa hàng".equals(e.getRole())) 
                        && e.getEmployeeName() != null && !e.getEmployeeName().isEmpty()) {
                    managerNameList.add(e.getEmployeeName());
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
        JLabel lblTitle = new JLabel("Quản Lý Cửa Hàng Trưởng");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Cấp phát và quản lý thông tin các Store Manager");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        // THÊM TOOL PANEL CHỨA THANH SEARCH
        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        toolPanel.setOpaque(false);

        cbSearch = new JComboBox<>();
        styleSearchBox(cbSearch);
        setupAutoComplete(cbSearch, managerNameList);

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

        // --- BỐ CỤC CHÍNH ---
        JPanel centerPanel = new JPanel(new BorderLayout(25, 0));
        centerPanel.setOpaque(false);

        // FORM BÊN TRÁI
        RoundedPanel formCard = new RoundedPanel(20, cardWhite);
        formCard.setPreferredSize(new Dimension(360, 0));
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        txtId = createTextField("Mã tự động...");
        txtId.setEnabled(false);
        txtName = createTextField("Nhập họ và tên...");
        txtPhone = createTextField("Nhập số điện thoại...");
        txtEmail = createTextField("Nhập email...");

        rdoMale = new JRadioButton("Nam");
        rdoFemale = new JRadioButton("Nữ");
        rdoMale.setOpaque(false);
        rdoFemale.setOpaque(false);
        rdoMale.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        rdoFemale.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btngGender = new ButtonGroup();
        btngGender.add(rdoMale);
        btngGender.add(rdoFemale);

        JPanel genderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        genderPanel.setOpaque(false);
        genderPanel.add(rdoMale);
        genderPanel.add(rdoFemale);

        int y = 0;
        formCard.add(createLabel("Mã quản lý"), addGbc(gbc, y++, 5));
        formCard.add(txtId, addGbc(gbc, y++, 15));
        formCard.add(createLabel("Họ và tên (*)"), addGbc(gbc, y++, 5));
        formCard.add(txtName, addGbc(gbc, y++, 15));
        formCard.add(createLabel("Số điện thoại (*)"), addGbc(gbc, y++, 5));
        formCard.add(txtPhone, addGbc(gbc, y++, 15));
        formCard.add(createLabel("Email kích hoạt (*)"), addGbc(gbc, y++, 5));
        formCard.add(txtEmail, addGbc(gbc, y++, 15));
        formCard.add(createLabel("Giới tính (*)"), addGbc(gbc, y++, 5));
        formCard.add(genderPanel, addGbc(gbc, y++, 25));

        // NÚT BẤM CÓ ICON
        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        btnGrid.setOpaque(false);
        btnAdd = createCustomButton("Cấp tài khoản", primaryBlue, Color.WHITE, IconHelper.add(20));
        btnUpdate = createCustomButton("Cập nhật", new Color(0, 168, 140), Color.WHITE, IconHelper.edit(20));
        btnDelete = createCustomButton("Xóa hồ sơ", new Color(220, 53, 69), Color.WHITE, IconHelper.delete(20));
        btnClear = createCustomButton("Làm mới", new Color(165, 177, 194), Color.WHITE, IconHelper.refresh(20));

        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);
        gbc.gridy = y++;
        formCard.add(btnGrid, gbc);

        // BẢNG BÊN PHẢI (Chỉ hiển thị Manager)
        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Bỏ cột chức vụ đi vì bảng này chỉ toàn là Quản lý
        tableModel = new DefaultTableModel(new Object[]{"Mã QL", "Họ và tên", "Số ĐT", "Email", "Cấp tài khoản", "Giới tính"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tblManagers = new JTable(tableModel);
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblManagers);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(formCard, BorderLayout.WEST);
        centerPanel.add(tableCard, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    // =================================================================================
    // CÁC HÀM TIỆN ÍCH UI 
    // =================================================================================
    private void styleSearchBox(JComboBox<String> cb) {
        cb.setEditable(true);
        cb.setBorder(null);
        cb.setBackground(Color.WHITE);
        ((JTextField) cb.getEditor().getEditorComponent()).setBorder(new EmptyBorder(0, 5, 0, 5));
    }

    private void setupAutoComplete(JComboBox<String> cb, List<String> items) {
        for (String i : items) {
            cb.addItem(i);
        }
        cb.setSelectedItem("");
        cb.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    btnSearch.doClick();
                }
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
        txt.setPreferredSize(new Dimension(200, 38));
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 8), new EmptyBorder(5, 10, 5, 10)
        ));
        return txt;
    }

    private JButton createCustomButton(String t, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(t);
        if (icon != null) {
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(18, 18, 1)));
        }
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setPreferredSize(new Dimension(140, 45));
        btn.setCursor(new Cursor(12));
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

    private void setupTableStyle() {
        tblManagers.setRowHeight(35);
        tblManagers.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblManagers.setShowVerticalLines(false);
        tblManagers.setSelectionBackground(new Color(212, 237, 218)); // Xanh nhạt cho Manager
        tblManagers.setSelectionForeground(new Color(25, 135, 84));
        tblManagers.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(textDark);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        for (int i = 0; i < tblManagers.getColumnModel().getColumnCount(); i++) {
            tblManagers.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);

                String accStatus = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(row), 4));

                // Bôi màu trạng thái cấp tài khoản
                if (column == 4) {
                    if ("Chưa cấp".equals(accStatus)) {
                        setForeground(new Color(220, 53, 69));
                        setFont(new Font("Segoe UI", Font.BOLD, 14));
                    } else if ("Đã cấp".equals(accStatus)) {
                        setForeground(new Color(25, 135, 84));
                        setFont(new Font("Segoe UI", Font.BOLD, 14));
                    }
                } else {
                    setForeground(isSelected ? new Color(25, 135, 84) : Color.BLACK);
                    setFont(new Font("Segoe UI", Font.PLAIN, 14));
                }
                return c;
            }
        };

        for (int i = 0; i < tblManagers.getColumnCount(); i++) {
            tblManagers.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
        }
    }

    private GridBagConstraints addGbc(GridBagConstraints gbc, int y, int b) {
        gbc.gridy = y;
        gbc.insets = new Insets(0, 0, b, 0);
        return gbc;
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

    // =================================================================================
    // LOGIC & SỰ KIỆN 
    // =================================================================================
    private void initEvents() {
        tblManagers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int row = tblManagers.getSelectedRow();
                if (row >= 0) {
                    txtId.setText(String.valueOf(tblManagers.getValueAt(row, 0)));
                    txtName.setText(String.valueOf(tblManagers.getValueAt(row, 1)));
                    txtPhone.setText(String.valueOf(tblManagers.getValueAt(row, 2)));
                    txtEmail.setText(String.valueOf(tblManagers.getValueAt(row, 3)));

                    String gender = String.valueOf(tblManagers.getValueAt(row, 5));
                    rdoMale.setSelected("Nam".equalsIgnoreCase(gender));
                    rdoFemale.setSelected("Nữ".equalsIgnoreCase(gender));
                }
            }
        });

        btnAdd.addActionListener(e -> {
            Employee emp = getManagerFromForm();
            if (emp == null) {
                return;
            }

            emp.setEmployeeId("MNG" + System.currentTimeMillis());

            if (employeeSql.insert(emp) <= 0) {
                JOptionPane.showMessageDialog(this, "Thêm hồ sơ thất bại! Vui lòng thử lại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                new ActivationTokenService().issueToken(emp.getEmployeeId());
            } catch (Exception ex) {
                logger.severe("Lỗi khi tạo mã kích hoạt cho " + emp.getEmployeeId() + ": " + ex.getMessage());
                JOptionPane.showMessageDialog(this,
                        "Hồ sơ đã thêm nhưng không thể tạo mã kích hoạt trong hệ thống.\n"
                        + "Vui lòng thử lại hoặc kiểm tra bảng ACTIVATION_TOKENS.\nLỗi: " + ex.getMessage(),
                        "Lỗi tạo mã kích hoạt",
                        JOptionPane.ERROR_MESSAGE);
                loadDataToTable();
                clearForm();
                return; 
            }

            final String finalEmail = emp.getEmail();
            final String finalName = emp.getEmployeeName();
            final String activationCode = emp.getEmployeeId(); 

            new Thread(() -> {
                boolean mailSent = EmailService.sendActivationEmail(finalEmail, finalName, activationCode);
                if (!mailSent) {
                    logger.warning("Cảnh báo: Không thể gửi mail kích hoạt đến " + finalEmail
                            + " (mã đã lưu DB, có thể gửi lại sau)");
                }
            }).start();

            JOptionPane.showMessageDialog(this,
                    "Cấp tài khoản Quản lý thành công!\nHệ thống đã tự động gửi Mã Kích Hoạt đến email: " + emp.getEmail(),
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);

            if (!managerNameList.contains(emp.getEmployeeName())) {
                managerNameList.add(emp.getEmployeeName());
                cbSearch.addItem(emp.getEmployeeName());
            }
            loadDataToTable();
            clearForm();
        });

        btnUpdate.addActionListener(e -> {
            String id = txtId.getText();
            if (id.isEmpty() || id.startsWith("Mã")) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn Quản lý trong bảng để cập nhật!");
                return;
            }
            Employee emp = getManagerFromForm();
            if (emp == null) {
                return;
            }

            emp.setEmployeeId(id);

            if (employeeSql.update(emp) > 0) {
                JOptionPane.showMessageDialog(this, "Cập nhật hồ sơ thành công!");
                loadDataToTable();
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnDelete.addActionListener(e -> {
            String id = txtId.getText();
            if (id.isEmpty() || id.startsWith("Mã")) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn Quản lý trong bảng để xóa!");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn thu hồi quyền Quản lý cửa hàng này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (employeeSql.delete(id) > 0) {
                    JOptionPane.showMessageDialog(this, "Thu hồi quyền thành công!");
                    loadDataToTable();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Thu hồi thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnClear.addActionListener(e -> clearForm());

        // BẮT SỰ KIỆN TÌM KIẾM
        btnSearch.addActionListener(e -> {
            JTextField searchEditor = (JTextField) cbSearch.getEditor().getEditorComponent();
            String keyword = searchEditor.getText().trim();
            updateTable(employeeSql.search(keyword));
        });
    }

    private void loadDataToTable() {
        updateTable(employeeSql.selectAll());
    }

    // HÀM HIỂN THỊ LÊN BẢNG SAU KHI LỌC/TÌM KIẾM
    private void updateTable(List<Employee> list) {
        tableModel.setRowCount(0);
        for (Employee emp : list) {
            // LỌC CHỈ LẤY ROLE R_STORE_MNG LÊN BẢNG DÙ KẾT QUẢ TÌM KIẾM TRẢ VỀ GÌ ĐI NỮA
            if ("R_STORE_MNG".equals(emp.getRoleId()) || "Quản lý cửa hàng".equals(emp.getRole())) {
                tableModel.addRow(new Object[]{
                    emp.getEmployeeId(), emp.getEmployeeName(), emp.getPhone(),
                    emp.getEmail(), emp.getAccountStatus(), emp.getGender()
                });
            }
        }
    }

    private Employee getManagerFromForm() {
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim();
        String gender = rdoMale.isSelected() ? "Nam" : (rdoFemale.isSelected() ? "Nữ" : "");

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || gender.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ các thông tin (*)");
            return null;
        }

        if (!email.endsWith("@gmail.com") && !email.endsWith("@gm.uit.edu.vn")) {
            JOptionPane.showMessageDialog(this,
                    "Email không hợp lệ!\nHệ thống chỉ chấp nhận đuôi @gmail.com hoặc @gm.uit.edu.vn",
                    "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
            return null;
        }

        Employee e = new Employee();
        e.setEmployeeName(name);
        e.setPhone(phone);
        e.setEmail(email);
        e.setGender(gender);

        // MẶC ĐỊNH GÁN QUYỀN QUẢN LÝ (KHÔNG CHO CHỌN)
        e.setRole("R_STORE_MNG");
        e.setRoleId("R_STORE_MNG");

        return e;
    }

    private void clearForm() {
        txtId.setText("");
        txtName.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        btngGender.clearSelection();
        tblManagers.clearSelection();
        if (cbSearch != null) {
            ((JTextField) cbSearch.getEditor().getEditorComponent()).setText("");
        }
    }
}