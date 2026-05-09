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

        roleList.add("R_STAFF_SALE");
        roleList.add("R_STAFF_VIEW_PROD");

        initUI();
        initEvents();

        refreshAllData();
        setupRealtimeSync();
    }

    private void setupRealtimeSync() {
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e.getType() == AppEventType.EMPLOYEES || e.getType() == AppEventType.ACCOUNT_SECURITY) {
                System.out.println("🛡️ [EmployeeView] Detecting data changes, refreshing UI...");
                refreshAllData(); 
            }
        });
    }

    private void refreshAllData() {
        SwingUtilities.invokeLater(() -> {
            loadDataToTable();
            loadAutoCompleteData();
        });
    }

    private void loadAutoCompleteData() {
        if (cbSearch == null) {
            return;
        }

        employeeNameList.clear();
        cbSearch.removeAllItems();
        cbSearch.addItem(""); 

        try {
            List<Employee> list = employeeSql.selectAll();
            for (Employee e : list) {
                if (e.getEmployeeName() != null && !e.getEmployeeName().isEmpty()) {
                    employeeNameList.add(e.getEmployeeName());
                    cbSearch.addItem(e.getEmployeeName());
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

        JPanel centerPanel = new JPanel(new BorderLayout(25, 0));
        centerPanel.setOpaque(false);

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
        for (String r : roleList) {
            cbRole.addItem(r);
        }

        rdoMale = new JRadioButton("Nam");
        rdoFemale = new JRadioButton("Nữ");
        rdoMale.setOpaque(false);
        rdoFemale.setOpaque(false);
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

        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 🔥 THÊM CỘT ẨN "RawPhone" CHỨA SĐT GỐC VÀO CỘT SỐ 9
        tableModel = new DefaultTableModel(new Object[]{"Mã NV", "Tên nhân viên", "Số ĐT", "Email", "Cấp tài khoản", "Chức vụ", "Giới tính", "RawEmail", "RawId", "RawPhone"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tblEmployees = new JTable(tableModel);
        
        // Ẩn 3 cột chứa dữ liệu gốc
        tblEmployees.removeColumn(tblEmployees.getColumnModel().getColumn(9)); // Giấu RawPhone
        tblEmployees.removeColumn(tblEmployees.getColumnModel().getColumn(8)); // Giấu RawId
        tblEmployees.removeColumn(tblEmployees.getColumnModel().getColumn(7)); // Giấu RawEmail
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblEmployees);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(formCard, BorderLayout.WEST);
        centerPanel.add(tableCard, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void initEvents() {
        tblEmployees.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int row = tblEmployees.getSelectedRow();
                int modelRow = tblEmployees.convertRowIndexToModel(row);
                if (row >= 0) {
                    String role = String.valueOf(tableModel.getValueAt(modelRow, 5));
                    if (role.contains("ADMIN") || role.contains("MNG") || role.contains("Quản")) {
                        JOptionPane.showMessageDialog(EmployeeView.this, "⚠️ Bạn không có quyền thao tác trên hồ sơ cấp quản lý!");
                        tblEmployees.clearSelection();
                        clearForm();
                        return;
                    }
                    
                    // LẤY LẠI DỮ LIỆU GỐC TỪ CÁC CỘT ẨN
                    txtId.setText(String.valueOf(tableModel.getValueAt(modelRow, 8))); // Mã NV gốc
                    txtName.setText(String.valueOf(tableModel.getValueAt(modelRow, 1)));
                    txtPhone.setText(String.valueOf(tableModel.getValueAt(modelRow, 9))); // SĐT gốc
                    txtEmail.setText(String.valueOf(tableModel.getValueAt(modelRow, 7))); // Email gốc

                    String status = String.valueOf(tableModel.getValueAt(modelRow, 4));
                    boolean isActivated = status != null && status.trim().equalsIgnoreCase("Đã cấp");

                    if (isActivated) {
                        txtEmail.setEnabled(false); 
                        txtEmail.setToolTipText("Tài khoản đã được cấp, không thể thay đổi Email.");
                    } else {
                        txtEmail.setEnabled(true); 
                        txtEmail.setToolTipText(null);
                    }

                    JTextField roleEditor = (JTextField) cbRole.getEditor().getEditorComponent();
                    roleEditor.setText(role);

                    String gender = String.valueOf(tableModel.getValueAt(modelRow, 6));
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

            if (isEmailDuplicate(emp.getEmail(), null)) {
                JOptionPane.showMessageDialog(this, "Email này đã được sử dụng cho một nhân viên khác!\nVui lòng nhập email khác.", "Trùng lặp dữ liệu", JOptionPane.WARNING_MESSAGE);
                txtEmail.requestFocus();
                return;
            }
            if (isPhoneDuplicate(emp.getPhone(), null)) {
                JOptionPane.showMessageDialog(this, "Số điện thoại này đã được đăng ký cho người khác!\nVui lòng nhập số khác.", "Trùng lặp dữ liệu", JOptionPane.WARNING_MESSAGE);
                txtPhone.requestFocus();
                return;
            }

            emp.setEmployeeId("EMP" + System.currentTimeMillis());

            if (employeeSql.insert(emp) > 0) {
                SyncVersionDao.bumpVersion("EMPLOYEES");
                RealtimeClient.send("EMPLOYEES_CHANGED");

                try {
                    new ActivationTokenService().issueToken(emp.getEmployeeId());
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                String actualToken = emp.getEmployeeId();
                String sqlToken = "SELECT token FROM (SELECT token FROM ACTIVATION_TOKENS WHERE employee_id = ? ORDER BY created_at DESC) WHERE ROWNUM = 1";
                try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sqlToken)) {
                    ps.setString(1, emp.getEmployeeId());
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            actualToken = rs.getString("token");
                        }
                    }
                } catch (Exception ex) {
                }

                final String email = emp.getEmail();
                final String name = emp.getEmployeeName();
                final String code = actualToken; 

                new Thread(() -> {
                    boolean ok = business.service.EmailService.sendActivationEmail(email, name, code);
                    SwingUtilities.invokeLater(() -> {
                        if (ok) {
                            JOptionPane.showMessageDialog(this, "Thành công! Mã kích hoạt đã gửi tới mail: " + email);
                        } else {
                            JOptionPane.showMessageDialog(this, "Hồ sơ đã lưu nhưng gửi mail thất bại. Hãy check SMTP!", "Lỗi Email", JOptionPane.WARNING_MESSAGE);
                        }
                    });
                }).start();

                refreshAllData();
                clearForm();
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

            String oldEmail = "";
            String accStatus = "";
            try {
                List<Employee> list = employeeSql.selectAll();
                for (Employee ex : list) {
                    if (ex.getEmployeeId().equals(id)) {
                        oldEmail = ex.getEmail() != null ? ex.getEmail() : "";
                        accStatus = ex.getAccountStatus() != null ? ex.getAccountStatus().trim() : "";
                        break;
                    }
                }
            } catch (Exception ex) {
            }

            boolean isActivated = accStatus.equalsIgnoreCase("Đã cấp");
            boolean emailChanged = !oldEmail.equalsIgnoreCase(emp.getEmail());

            if (emailChanged && isActivated) {
                JOptionPane.showMessageDialog(this, "Tài khoản của nhân viên này ĐÃ ĐƯỢC CẤP!\nNghiêm cấm thay đổi Email để bảo mật.", "Bảo mật tài khoản", JOptionPane.ERROR_MESSAGE);
                txtEmail.setText(oldEmail);
                return;
            }

            if (isEmailDuplicate(emp.getEmail(), id)) {
                JOptionPane.showMessageDialog(this, "Email này đã bị trùng với một nhân viên khác!\nVui lòng nhập email khác.", "Trùng lặp dữ liệu", JOptionPane.WARNING_MESSAGE);
                txtEmail.requestFocus();
                return;
            }
            if (isPhoneDuplicate(emp.getPhone(), id)) {
                JOptionPane.showMessageDialog(this, "Số điện thoại này đã bị trùng với một nhân viên khác!\nVui lòng nhập số khác.", "Trùng lặp dữ liệu", JOptionPane.WARNING_MESSAGE);
                txtPhone.requestFocus();
                return;
            }

            emp.setEmployeeId(id);
            if (employeeSql.update(emp) > 0) {
                RealtimeClient.send("EMPLOYEES_CHANGED");

                if (emailChanged && !isActivated) {

                    try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement("UPDATE ACCOUNTS SET email = ? WHERE user_id = ?")) {
                        ps.setString(1, emp.getEmail());
                        ps.setString(2, emp.getEmployeeId());
                        ps.executeUpdate();
                    } catch (Exception ex) {
                        System.err.println("Lỗi đồng bộ email sang Accounts: " + ex.getMessage());
                    }

                    try {
                        new ActivationTokenService().issueToken(emp.getEmployeeId());
                    } catch (Exception ex) {
                    }

                    String actualToken = emp.getEmployeeId();
                    String sqlToken = "SELECT token FROM (SELECT token FROM ACTIVATION_TOKENS WHERE employee_id = ? ORDER BY created_at DESC) WHERE ROWNUM = 1";
                    try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sqlToken)) {
                        ps.setString(1, emp.getEmployeeId());
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                actualToken = rs.getString("token");
                            }
                        }
                    } catch (Exception ex) {
                    }

                    final String emailToSend = emp.getEmail();
                    final String nameToSend = emp.getEmployeeName();
                    final String codeToSend = actualToken;

                    new Thread(() -> {
                        boolean ok = business.service.EmailService.sendActivationEmail(emailToSend, nameToSend, codeToSend);
                        SwingUtilities.invokeLater(() -> {
                            if (ok) {
                                JOptionPane.showMessageDialog(this, "Đã cập nhật hồ sơ và gửi lại Mã Kích Hoạt mới tới:\n" + emailToSend);
                            } else {
                                JOptionPane.showMessageDialog(this, "Cập nhật thành công nhưng gửi mail thất bại!\nHãy kiểm tra lại SMTP hoặc kết nối mạng.", "Lỗi", JOptionPane.WARNING_MESSAGE);
                            }
                        });
                    }).start();
                } else {
                    JOptionPane.showMessageDialog(this, "Cập nhật hồ sơ thành công!");
                }

                refreshAllData();
                clearForm();
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật thất bại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnDelete.addActionListener(e -> {
            String id = txtId.getText();
            if (id.isEmpty()) {
                return;
            }
            if (JOptionPane.showConfirmDialog(this, "Xác nhận xóa hồ sơ này?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                if (employeeSql.delete(id) > 0) {
                    RealtimeClient.send("EMPLOYEES_CHANGED");
                    refreshAllData();
                    clearForm();
                }
            }
        });

        btnClear.addActionListener(e -> clearForm());

        btnSearch.addActionListener(e -> {
            String kw = ((JTextField) cbSearch.getEditor().getEditorComponent()).getText().trim();
            updateTable(employeeSql.search(kw));
        });
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

        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(this,
                    "Email không hợp lệ!\n- Vui lòng kiểm tra lại khoảng trắng, ký tự đặc biệt.",
                    "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
            txtEmail.requestFocus();
            return null;
        }

        if (!isValidPhone(phone)) {
            JOptionPane.showMessageDialog(this,
                    "Số điện thoại không hợp lệ!\n- Phải đủ 10 chữ số.\n- Bắt đầu bằng các đầu số hợp lệ (03, 05, 07, 08, 09).",
                    "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
            txtPhone.requestFocus();
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
        txtEmail.setEnabled(true); 
        txtEmail.setToolTipText(null);

        btngGender.clearSelection();
        tblEmployees.clearSelection();
        ((JTextField) cbSearch.getEditor().getEditorComponent()).setText("");
        ((JTextField) cbRole.getEditor().getEditorComponent()).setText("");
    }

    private void loadDataToTable() {
        updateTable(employeeSql.selectAll());
    }

    private void updateTable(List<Employee> list) {
        tableModel.setRowCount(0);
        list.sort((e1, e2) -> Integer.compare(getRoleRank(e1.getRole()), getRoleRank(e2.getRole())));
        for (Employee emp : list) {
            tableModel.addRow(new Object[]{
                maskSensitiveInfo(emp.getEmployeeId()), 
                emp.getEmployeeName(), 
                maskPhone(emp.getPhone()), // 🔥 HIỂN THỊ SĐT ĐÃ CHE LÊN BẢNG
                maskSensitiveInfo(emp.getEmail()), 
                emp.getAccountStatus(), 
                emp.getRole(), 
                emp.getGender(),
                emp.getEmail(), // CỘT 7: EMAIL GỐC
                emp.getEmployeeId(), // CỘT 8: MÃ GỐC 
                emp.getPhone() // CỘT 9: SĐT GỐC ĐỂ DÙNG KHI CLICK ĐÚP
            });
        }
    }

    // =========================================================================
    // HÀM TIỆN ÍCH: LÀM MỜ THÔNG TIN NHẠY CẢM
    // =========================================================================
    private String maskSensitiveInfo(String info) {
        if (info == null || info.isEmpty() || info.equals("Chưa có email")) {
            return "Chưa có dữ liệu";
        }
        
        int atIndex = info.indexOf("@");
        if (atIndex > 0) { // Nếu là email
            String localPart = info.substring(0, atIndex);
            String domainPart = info.substring(atIndex);
            if (localPart.length() > 3) {
                return localPart.substring(0, 3) + "***" + domainPart;
            } else {
                return "***" + domainPart;
            }
        } else if (info.length() > 6) { // Nếu là Mã NV
            String visiblePart = info.substring(0, 6);
            StringBuilder hiddenPart = new StringBuilder();
            for (int i = 6; i < info.length(); i++) {
                hiddenPart.append("*");
            }
            return visiblePart + hiddenPart.toString();
        }
        return info; 
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.isEmpty() || phone.length() < 8) {
            return "Chưa có dữ liệu";
        }
        // VD: 0987654321 -> 098****321
        int len = phone.length();
        String start = phone.substring(0, 3);
        String end = phone.substring(len - 3);
        StringBuilder masked = new StringBuilder();
        for (int i = 3; i < len - 3; i++) {
            masked.append("*");
        }
        return start + masked.toString() + end;
    }

    private int getRoleRank(String role) {
        if (role == null) {
            return 3;
        }
        if (role.contains("ADMIN")) {
            return 1;
        }
        if (role.contains("MNG")) {
            return 2;
        }
        return 3;
    }

    private void styleComboBox(JComboBox<String> cb, String placeholder) {
        cb.setPreferredSize(new Dimension(280, 38));
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cb.setBackground(Color.WHITE);
        cb.setEditable(true);
        JTextField editor = (JTextField) cb.getEditor().getEditorComponent();
        editor.putClientProperty("JTextField.placeholderText", placeholder);
        editor.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(borderGray, 8), new EmptyBorder(5, 5, 5, 5)));
    }

    private void styleSearchBox(JComboBox<String> cb) {
        cb.setEditable(true);
        cb.setBorder(null);
        cb.setBackground(Color.WHITE);
        ((JTextField) cb.getEditor().getEditorComponent()).setBorder(new EmptyBorder(0, 5, 0, 5));
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
        txt.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(borderGray, 8), new EmptyBorder(5, 10, 5, 10)));
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
                String role = String.valueOf(table.getModel().getValueAt(table.convertRowIndexToModel(row), 5));
                if (role.contains("ADMIN")) {
                    setBackground(isSelected ? new Color(248, 215, 218) : new Color(255, 235, 238));
                    setForeground(new Color(220, 53, 69));
                } else if (role.contains("MNG")) {
                    setBackground(isSelected ? new Color(212, 237, 218) : new Color(230, 245, 233));
                    setForeground(new Color(25, 135, 84));
                } else {
                    setBackground(isSelected ? new Color(237, 242, 255) : Color.WHITE);
                    setForeground(isSelected ? textDark : Color.BLACK);
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
            g2.setStroke(new BasicStroke(1.2f));
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

    private boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        if (!email.matches(emailRegex)) {
            return false;
        }
        return email.endsWith("@gmail.com") || email.endsWith("@gm.uit.edu.vn");
    }

    private boolean isEmailDuplicate(String email, String excludeEmpId) {
        return employeeSql.existsByEmailGlobal(email, excludeEmpId);
    }

    private boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        return phone.matches("^(0)(3|5|7|8|9)[0-9]{8}$");
    }

    private boolean isPhoneDuplicate(String phone, String excludeEmpId) {
        try {
            List<Employee> list = employeeSql.selectAll();
            for (Employee e : list) {
                if (e.getPhone() != null && e.getPhone().equals(phone)) {
                    if (excludeEmpId != null && e.getEmployeeId().equals(excludeEmpId)) {
                        continue;
                    }
                    return true;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }
}