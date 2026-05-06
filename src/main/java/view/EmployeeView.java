package view;

import business.sql.hr_kpi.EmployeeSql;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.employee.Employee;
import view.components.IconHelper;
import business.service.ActivationTokenService;

public class EmployeeView extends JPanel {

    // --- BẢNG MÀU & THÔNG SỐ UI ---
    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color primaryBlue = new Color(54, 92, 245);
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color borderGray = new Color(230, 235, 241);

    private JTextField txtId, txtName, txtPhone, txtEmail;
    private JComboBox<String> cbRole, cbSearch;

    private JRadioButton rdoMale, rdoFemale;
    private ButtonGroup btngGender;
    private JTable tblEmployees;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch;

    private final EmployeeSql employeeSql = new EmployeeSql();

    // Auto-complete lists
    private List<String> employeeNameList = new ArrayList<>();
    private List<String> roleList = new ArrayList<>();

    public EmployeeView() {
        if (!business.service.AuthorizationService.canAccessEmployeeManagement()) {
            showAccessDenied();
            return;
        }

        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        loadAutoCompleteData();
        initUI();
        initEvents();
        loadDataToTable();

        // BẮT SỰ KIỆN REAL-TIME TỪ SERVER ĐỔ VỀ
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e.getType() == AppEventType.EMPLOYEES || e.getType() == AppEventType.ACCOUNT_SECURITY) {
                SwingUtilities.invokeLater(() -> {
                    loadDataToTable();
                });
            }
        });
    }

    private void loadAutoCompleteData() {
        roleList.clear();
        roleList.add("R_STAFF_SALE");
        roleList.add("R_STAFF_VIEW_PROD");

        employeeNameList.clear();
        try {
            List<Employee> list = employeeSql.selectAll();
            for (Employee e : list) {
                if (e.getEmployeeName() != null && !e.getEmployeeName().isEmpty()) {
                    employeeNameList.add(e.getEmployeeName());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAccessDenied() {
        setLayout(new BorderLayout());
        setBackground(bgLight);
        JLabel message = new JLabel("Bạn không có quyền truy cập chức năng quản lý nhân viên.", SwingConstants.CENTER);
        message.setFont(new Font("Segoe UI", Font.BOLD, 18));
        message.setForeground(new Color(220, 53, 69));
        add(message, BorderLayout.CENTER);
    }

    private void initUI() {
        // --- HEADER ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Hồ Sơ Nhân Viên");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Quản lý thông tin và chức vụ nhân sự");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        JPanel toolPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        toolPanel.setOpaque(false);

        cbSearch = new JComboBox<>();
        styleSearchBox(cbSearch);
        setupAutoComplete(cbSearch, employeeNameList);

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
        txtName = createTextField("Nhập tên...");
        txtPhone = createTextField("Nhập số điện thoại...");
        txtEmail = createTextField("Nhập email...");

        cbRole = new JComboBox<>();
        styleComboBox(cbRole, "Nhập phân quyền...");
        setupAutoComplete(cbRole, roleList);

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
        formCard.add(createLabel("Mã nhân viên"), addGbc(gbc, y++, 5));
        formCard.add(txtId, addGbc(gbc, y++, 15));
        formCard.add(createLabel("Tên nhân viên (*)"), addGbc(gbc, y++, 5));
        formCard.add(txtName, addGbc(gbc, y++, 15));
        formCard.add(createLabel("Số điện thoại (*)"), addGbc(gbc, y++, 5));
        formCard.add(txtPhone, addGbc(gbc, y++, 15));
        formCard.add(createLabel("Email (*)"), addGbc(gbc, y++, 5));
        formCard.add(txtEmail, addGbc(gbc, y++, 15));

        formCard.add(createLabel("Chức vụ (Role ID) (*)"), addGbc(gbc, y++, 5));
        formCard.add(cbRole, addGbc(gbc, y++, 15));
        formCard.add(createLabel("Giới tính (*)"), addGbc(gbc, y++, 5));
        formCard.add(genderPanel, addGbc(gbc, y++, 25));

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        btnGrid.setOpaque(false);
        btnAdd = createCustomButton("Thêm hồ sơ", primaryBlue, Color.WHITE, IconHelper.add(20));
        btnUpdate = createCustomButton("Cập nhật", new Color(0, 168, 140), Color.WHITE, IconHelper.edit(20));
        btnDelete = createCustomButton("Xóa hồ sơ", new Color(220, 53, 69), Color.WHITE, IconHelper.delete(20));
        btnClear = createCustomButton("Làm mới", new Color(165, 177, 194), Color.WHITE, IconHelper.refresh(20));

        btnGrid.add(btnAdd);
        btnGrid.add(btnUpdate);
        btnGrid.add(btnDelete);
        btnGrid.add(btnClear);
        gbc.gridy = y++;
        formCard.add(btnGrid, gbc);

        // BẢNG BÊN PHẢI
        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        tableModel = new DefaultTableModel(new Object[]{"Mã NV", "Tên nhân viên", "Số ĐT", "Email", "Cấp tài khoản", "Chức vụ", "Giới tính"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tblEmployees = new JTable(tableModel);
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblEmployees);
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
    private void styleComboBox(JComboBox<String> cb, String placeholder) {
        cb.setPreferredSize(new Dimension(280, 38));
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cb.setBackground(Color.WHITE);
        cb.setEditable(true);

        JTextField editor = (JTextField) cb.getEditor().getEditorComponent();
        editor.putClientProperty("JTextField.placeholderText", placeholder);
        editor.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 8), new EmptyBorder(5, 5, 5, 5)
        ));
    }

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
        tblEmployees.setRowHeight(35);
        tblEmployees.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tblEmployees.setShowVerticalLines(false);
        tblEmployees.setSelectionBackground(new Color(237, 242, 255));
        tblEmployees.setSelectionForeground(textDark);
        tblEmployees.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(bgLight);
        headerRenderer.setForeground(Color.BLACK);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        for (int i = 0; i < tblEmployees.getColumnModel().getColumnCount(); i++) {
            tblEmployees.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        DefaultTableCellRenderer customRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(JLabel.CENTER);

                String accStatus = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(row), 4));
                String role = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(row), 5));

                if (role.contains("ADMIN") || "Quản trị viên".equals(role)) {
                    setBackground(isSelected ? new Color(248, 215, 218) : new Color(255, 235, 238));
                    setForeground(new Color(220, 53, 69));
                    setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else if (role.contains("MNG") || "Quản lý cửa hàng".equals(role)) {
                    setBackground(isSelected ? new Color(212, 237, 218) : new Color(230, 245, 233));
                    setForeground(new Color(25, 135, 84));
                    setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else {
                    setBackground(isSelected ? new Color(237, 242, 255) : Color.WHITE);
                    setForeground(isSelected ? textDark : Color.BLACK);
                    setFont(new Font("Segoe UI", Font.PLAIN, 14));
                }

                if (column == 4) {
                    if ("Chưa cấp".equals(accStatus)) {
                        setForeground(new Color(220, 53, 69));
                        setFont(new Font("Segoe UI", Font.BOLD, 14));
                    } else if ("Đã cấp".equals(accStatus)) {
                        setForeground(new Color(25, 135, 84));
                        setFont(new Font("Segoe UI", Font.BOLD, 14));
                    }
                }
                return c;
            }
        };

        for (int i = 0; i < tblEmployees.getColumnCount(); i++) {
            tblEmployees.getColumnModel().getColumn(i).setCellRenderer(customRenderer);
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

    private int getRoleRank(String role) {
        if (role == null) {
            return 3;
        }
        if (role.contains("ADMIN") || "Quản trị viên".equals(role)) {
            return 1;
        }
        if (role.contains("MNG") || "Quản lý cửa hàng".equals(role)) {
            return 2;
        }
        return 3;
    }

    // =================================================================================
    // LOGIC & SỰ KIỆN 
    // =================================================================================
    private void initEvents() {
        tblEmployees.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int row = tblEmployees.getSelectedRow();
                if (row >= 0) {
                    String role = String.valueOf(tblEmployees.getValueAt(row, 5));

                    if ("R_ADMIN_ALL".equals(role) || "Quản trị viên".equals(role)) {
                        JOptionPane.showMessageDialog(EmployeeView.this,
                                "⚠️ Đây là tài khoản Quản trị viên cấp cao (Admin).\nBạn không có quyền xem hay thao tác trên hồ sơ này!",
                                "Cảnh báo bảo mật", JOptionPane.WARNING_MESSAGE);
                        tblEmployees.clearSelection();
                        clearForm();
                        return;
                    } else if ("R_STORE_MNG".equals(role) || "Quản lý cửa hàng".equals(role)) {
                        JOptionPane.showMessageDialog(EmployeeView.this,
                                "⚠️ Đây là hồ sơ Cửa hàng trưởng (Manager).\nBạn không thể can thiệp vào hồ sơ đồng cấp!",
                                "Cảnh báo bảo mật", JOptionPane.WARNING_MESSAGE);
                        tblEmployees.clearSelection();
                        clearForm();
                        return;
                    }

                    txtId.setText(String.valueOf(tblEmployees.getValueAt(row, 0)));
                    txtName.setText(String.valueOf(tblEmployees.getValueAt(row, 1)));
                    txtPhone.setText(String.valueOf(tblEmployees.getValueAt(row, 2)));
                    txtEmail.setText(String.valueOf(tblEmployees.getValueAt(row, 3)));

                    JTextField roleEditor = (JTextField) cbRole.getEditor().getEditorComponent();
                    roleEditor.setText(role);

                    String gender = String.valueOf(tblEmployees.getValueAt(row, 6));
                    rdoMale.setSelected("Nam".equalsIgnoreCase(gender));
                    rdoFemale.setSelected("Nữ".equalsIgnoreCase(gender));
                }
            }
        });

        btnAdd.addActionListener(e -> {
            Employee emp = getEmployeeFromForm();
            if (emp == null) {
                return;
            }

            emp.setEmployeeId("EMP" + System.currentTimeMillis());

            if (employeeSql.insert(emp) > 0) {
                // ĐỒNG BỘ REAL-TIME
                SyncVersionDao.bumpVersion("EMPLOYEES");
                RealtimeClient.send("EMPLOYEES_CHANGED");

                // === FIX: tạo token trong DB trước khi gửi mail ===
                try {
                    new ActivationTokenService().issueToken(emp.getEmployeeId());
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(this,
                            "Tạo hồ sơ nhân viên thành công nhưng KHÔNG tạo được mã kích hoạt trong hệ thống!\n"
                            + "Vui lòng thử lại hoặc kiểm tra bảng ACTIVATION_TOKENS.\nChi tiết: " + ex.getMessage(),
                            "Lỗi cấp mã kích hoạt",
                            JOptionPane.ERROR_MESSAGE);
                    return; // KHÔNG gửi mail nếu DB chưa có token
                }

                new Thread(() -> {
                    boolean mailSent = business.service.EmailService.sendActivationEmail(
                            emp.getEmail(),
                            emp.getEmployeeName(),
                            emp.getEmployeeId() // CODE chính là EMP...
                    );
                    if (!mailSent) {
                        System.err.println("Cảnh báo: Không thể gửi mail kích hoạt đến " + emp.getEmail());
                    }
                }).start();

                JOptionPane.showMessageDialog(this,
                        "Tạo hồ sơ nhân viên thành công!\nHệ thống đã tự động gửi Mã Kích Hoạt đến email: " + emp.getEmail(),
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);

                if (!employeeNameList.contains(emp.getEmployeeName())) {
                    employeeNameList.add(emp.getEmployeeName());
                }
                loadDataToTable();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Thêm hồ sơ thất bại! Vui lòng thử lại.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnUpdate.addActionListener(e -> {
            String id = txtId.getText();
            if (id.isEmpty() || id.startsWith("Mã")) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn nhân viên trong bảng để cập nhật!");
                return;
            }
            Employee emp = getEmployeeFromForm();
            if (emp == null) {
                return;
            }

            emp.setEmployeeId(id);

            if (employeeSql.update(emp) > 0) {
                // ĐỒNG BỘ REAL-TIME
                SyncVersionDao.bumpVersion("EMPLOYEES");
                RealtimeClient.send("EMPLOYEES_CHANGED");

                JOptionPane.showMessageDialog(this, "Cập nhật hồ sơ thành công!");
                loadDataToTable();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnDelete.addActionListener(e -> {
            String id = txtId.getText();
            if (id.isEmpty() || id.startsWith("Mã")) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn nhân viên trong bảng để xóa!");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa hồ sơ nhân viên này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                if (employeeSql.delete(id) > 0) {
                    // ĐỒNG BỘ REAL-TIME
                    SyncVersionDao.bumpVersion("EMPLOYEES");
                    RealtimeClient.send("EMPLOYEES_CHANGED");

                    JOptionPane.showMessageDialog(this, "Xóa hồ sơ thành công!");
                    loadDataToTable();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Xóa thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnClear.addActionListener(e -> clearForm());

        btnSearch.addActionListener(e -> {
            JTextField searchEditor = (JTextField) cbSearch.getEditor().getEditorComponent();
            String keyword = searchEditor.getText().trim();
            updateTable(employeeSql.search(keyword));
        });
    }

    private void loadDataToTable() {
        updateTable(employeeSql.selectAll());
    }

    private void updateTable(List<Employee> list) {
        tableModel.setRowCount(0);
        list.sort((e1, e2) -> Integer.compare(getRoleRank(e1.getRole()), getRoleRank(e2.getRole())));
        for (Employee emp : list) {
            tableModel.addRow(new Object[]{
                emp.getEmployeeId(), emp.getEmployeeName(), emp.getPhone(),
                emp.getEmail(), emp.getAccountStatus(), emp.getRole(), emp.getGender()
            });
        }
    }

    private Employee getEmployeeFromForm() {
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim().toLowerCase();
        String gender = rdoMale.isSelected() ? "Nam" : (rdoFemale.isSelected() ? "Nữ" : "");

        JTextField roleEditor = (JTextField) cbRole.getEditor().getEditorComponent();
        String role = roleEditor.getText().trim().toUpperCase();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || gender.isEmpty() || role.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ các thông tin cá nhân và chức vụ (*)");
            return null;
        }

        if (!role.equals("R_STAFF_SALE") && !role.equals("R_STAFF_VIEW_PROD")) {
            JOptionPane.showMessageDialog(this,
                    "Phân quyền không hợp lệ!\nQuản lý chỉ được phép cấp quyền:\n- R_STAFF_SALE\n- R_STAFF_VIEW_PROD",
                    "Cảnh báo bảo mật", JOptionPane.WARNING_MESSAGE);
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
        e.setRole(role);
        e.setRoleId(role);

        return e;
    }

    private void clearForm() {
        txtId.setText("");
        txtName.setText("");
        txtPhone.setText("");
        txtEmail.setText("");
        btngGender.clearSelection();
        tblEmployees.clearSelection();
        ((JTextField) cbSearch.getEditor().getEditorComponent()).setText("");
        ((JTextField) cbRole.getEditor().getEditorComponent()).setText("");
    }
}
