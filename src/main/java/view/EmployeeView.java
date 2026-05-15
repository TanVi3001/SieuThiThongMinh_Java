package view;

import business.sql.hr_kpi.EmployeeSql;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.employee.Employee;
import view.components.IconHelper;
import business.service.ActivationTokenService;

public class EmployeeView extends JPanel {

    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color primaryBlue = new Color(54, 92, 245);
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color borderGray = new Color(230, 235, 241);

    private final Color onlineGreen = new Color(39, 174, 96);
    private final Color offlineRed = new Color(231, 76, 60);

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

    private String currentSelectedRawId = "";

    // Model column indexes
    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_PHONE = 2;
    private static final int COL_EMAIL = 3;
    private static final int COL_ACCOUNT_STATUS = 4;
    private static final int COL_ONLINE_STATUS = 5;
    private static final int COL_ROLE = 6;
    private static final int COL_GENDER = 7;
    private static final int COL_RAW_ID = 8;

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

    private void setupEmployeeTableColumnWidth() {
        if (tblEmployees == null) {
            return;
        }

        tblEmployees.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        tblEmployees.getColumnModel().getColumn(0).setPreferredWidth(95);   // Mã NV
        tblEmployees.getColumnModel().getColumn(1).setPreferredWidth(170);  // Tên
        tblEmployees.getColumnModel().getColumn(2).setPreferredWidth(110);  // SĐT
        tblEmployees.getColumnModel().getColumn(3).setPreferredWidth(260);  // Email
        tblEmployees.getColumnModel().getColumn(4).setPreferredWidth(120);  // Cấp tài khoản
        tblEmployees.getColumnModel().getColumn(5).setPreferredWidth(120);  // Hoạt động
        tblEmployees.getColumnModel().getColumn(6).setPreferredWidth(190);  // Chức vụ
        tblEmployees.getColumnModel().getColumn(7).setPreferredWidth(80);   // Giới tính
    }

    private void setupRealtimeSync() {
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e.getType() == AppEventType.EMPLOYEES
                    || e.getType() == AppEventType.ACCOUNT_SECURITY) {
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
            String currentRole = getCurrentUserRole();
            List<Employee> list = employeeSql.getAllNhanVien(currentRole);

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

        JLabel message = new JLabel(
                "Bạn không có quyền truy cập chức năng quản lý nhân viên.",
                SwingConstants.CENTER
        );
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

        JLabel lblSub = new JLabel("Quản lý thông tin, chức vụ và trạng thái làm việc");
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
        searchFieldWrapper.add(new JLabel(IconHelper.search(16)), BorderLayout.WEST);
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

        tableModel = new DefaultTableModel(
                new Object[]{
                    "Mã NV",
                    "Tên nhân viên",
                    "Số ĐT",
                    "Email",
                    "Cấp tài khoản",
                    "Hoạt động",
                    "Chức vụ",
                    "Giới tính",
                    "RawId"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        tblEmployees = new JTable(tableModel);

// Ẩn cột RawId
        tblEmployees.removeColumn(tblEmployees.getColumnModel().getColumn(8));

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

                if (row < 0) {
                    return;
                }

                int modelRow = tblEmployees.convertRowIndexToModel(row);

                String role = String.valueOf(tableModel.getValueAt(modelRow, COL_ROLE));

                if (role.contains("R_ADMIN_ALL")
                        || role.contains("R_STORE_MNG")
                        || role.contains("ADMIN")
                        || role.contains("MNG")) {
                    JOptionPane.showMessageDialog(
                            EmployeeView.this,
                            "⚠️ Bạn không có quyền thao tác trên hồ sơ cấp quản lý!"
                    );
                    tblEmployees.clearSelection();
                    clearForm();
                    return;
                }

                currentSelectedRawId = String.valueOf(tableModel.getValueAt(modelRow, COL_RAW_ID));
                txtId.setText(maskSensitiveInfo(currentSelectedRawId));

                txtName.setText(String.valueOf(tableModel.getValueAt(modelRow, COL_NAME)));
                txtPhone.setText(String.valueOf(tableModel.getValueAt(modelRow, COL_PHONE)));
                txtEmail.setText(String.valueOf(tableModel.getValueAt(modelRow, COL_EMAIL)));

                String accountStatus = String.valueOf(tableModel.getValueAt(modelRow, COL_ACCOUNT_STATUS));
                boolean isActivated = accountStatus != null
                        && accountStatus.trim().equalsIgnoreCase("Đã cấp");

                if (isActivated) {
                    txtEmail.setEnabled(false);
                    txtEmail.setToolTipText("Tài khoản đã được cấp, không thể thay đổi Email.");
                } else {
                    txtEmail.setEnabled(true);
                    txtEmail.setToolTipText(null);
                }

                ((JTextField) cbRole.getEditor().getEditorComponent()).setText(role);

                String gender = String.valueOf(tableModel.getValueAt(modelRow, COL_GENDER));
                rdoMale.setSelected("Nam".equalsIgnoreCase(gender));
                rdoFemale.setSelected("Nữ".equalsIgnoreCase(gender));
            }
        });

        btnAdd.addActionListener(e -> {
            Employee emp = getEmployeeFromForm();

            if (emp == null) {
                return;
            }

            if (isEmailDuplicate(emp.getEmail(), null)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Email này đã được sử dụng cho một nhân viên khác!",
                        "Trùng lặp dữ liệu",
                        JOptionPane.WARNING_MESSAGE
                );
                txtEmail.requestFocus();
                return;
            }

            if (isPhoneDuplicate(emp.getPhone(), null)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Số điện thoại này đã được đăng ký cho người khác!",
                        "Trùng lặp dữ liệu",
                        JOptionPane.WARNING_MESSAGE
                );
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
                String sqlToken = "SELECT token "
                        + "FROM (SELECT token FROM ACTIVATION_TOKENS "
                        + "      WHERE employee_id = ? "
                        + "      ORDER BY created_at DESC) "
                        + "WHERE ROWNUM = 1";

                try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sqlToken)) {

                    ps.setString(1, emp.getEmployeeId());

                    try (ResultSet rs = ps.executeQuery()) {
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
                            JOptionPane.showMessageDialog(
                                    this,
                                    "Thành công! Mã kích hoạt đã gửi tới mail: " + email
                            );
                        } else {
                            JOptionPane.showMessageDialog(
                                    this,
                                    "Hồ sơ đã lưu nhưng gửi mail thất bại.",
                                    "Lỗi Email",
                                    JOptionPane.WARNING_MESSAGE
                            );
                        }
                    });
                }).start();

                refreshAllData();
                clearForm();
            }
        });

        btnUpdate.addActionListener(e -> {
            String displayedId = txtId.getText();

            if (displayedId.isEmpty() || displayedId.startsWith("Mã")) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn nhân viên trong bảng để cập nhật!");
                return;
            }

            String idToUpdate = currentSelectedRawId;

            Employee emp = getEmployeeFromForm();

            if (emp == null) {
                return;
            }

            String oldEmail = "";
            String accStatus = "";

            try {
                List<Employee> list = employeeSql.selectAll();

                for (Employee ex : list) {
                    if (ex.getEmployeeId().equals(idToUpdate)) {
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
                JOptionPane.showMessageDialog(
                        this,
                        "Tài khoản của nhân viên này ĐÃ ĐƯỢC CẤP!\n"
                        + "Nghiêm cấm thay đổi Email để bảo mật.",
                        "Bảo mật tài khoản",
                        JOptionPane.ERROR_MESSAGE
                );
                txtEmail.setText(oldEmail);
                return;
            }

            if (isEmailDuplicate(emp.getEmail(), idToUpdate)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Email này đã bị trùng với một nhân viên khác!",
                        "Trùng lặp dữ liệu",
                        JOptionPane.WARNING_MESSAGE
                );
                txtEmail.requestFocus();
                return;
            }

            if (isPhoneDuplicate(emp.getPhone(), idToUpdate)) {
                JOptionPane.showMessageDialog(
                        this,
                        "Số điện thoại này đã bị trùng với một nhân viên khác!",
                        "Trùng lặp dữ liệu",
                        JOptionPane.WARNING_MESSAGE
                );
                txtPhone.requestFocus();
                return;
            }

            emp.setEmployeeId(idToUpdate);

            if (employeeSql.update(emp) > 0) {
                RealtimeClient.send("EMPLOYEES_CHANGED");

                if (emailChanged && !isActivated) {
                    try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                            "UPDATE USERS SET email = ? WHERE user_id = ?"
                    )) {

                        ps.setString(1, emp.getEmail());
                        ps.setString(2, emp.getEmployeeId());
                        ps.executeUpdate();

                    } catch (Exception ex) {
                    }

                    try {
                        new ActivationTokenService().issueToken(emp.getEmployeeId());
                    } catch (Exception ex) {
                    }

                    String actualToken = emp.getEmployeeId();
                    String sqlToken = "SELECT token "
                            + "FROM (SELECT token FROM ACTIVATION_TOKENS "
                            + "      WHERE employee_id = ? "
                            + "      ORDER BY created_at DESC) "
                            + "WHERE ROWNUM = 1";

                    try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sqlToken)) {

                        ps.setString(1, emp.getEmployeeId());

                        try (ResultSet rs = ps.executeQuery()) {
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
                        boolean ok = business.service.EmailService.sendActivationEmail(
                                emailToSend,
                                nameToSend,
                                codeToSend
                        );

                        SwingUtilities.invokeLater(() -> {
                            if (ok) {
                                JOptionPane.showMessageDialog(
                                        this,
                                        "Đã cập nhật hồ sơ và gửi lại Mã Kích Hoạt mới tới:\n"
                                        + emailToSend
                                );
                            } else {
                                JOptionPane.showMessageDialog(
                                        this,
                                        "Cập nhật thành công nhưng gửi mail thất bại!",
                                        "Lỗi",
                                        JOptionPane.WARNING_MESSAGE
                                );
                            }
                        });
                    }).start();

                } else {
                    JOptionPane.showMessageDialog(this, "Cập nhật hồ sơ thành công!");
                }

                refreshAllData();
                clearForm();

            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Cập nhật thất bại!",
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        });

        btnDelete.addActionListener(e -> {
            if (currentSelectedRawId.isEmpty()) {
                return;
            }

            if (JOptionPane.showConfirmDialog(
                    this,
                    "Xác nhận xóa hồ sơ này?",
                    "Xác nhận",
                    JOptionPane.YES_NO_OPTION
            ) == JOptionPane.YES_OPTION) {

                if (employeeSql.delete(currentSelectedRawId) > 0) {
                    RealtimeClient.send("EMPLOYEES_CHANGED");
                    refreshAllData();
                    clearForm();
                }
            }
        });

        btnClear.addActionListener(e -> clearForm());

        btnSearch.addActionListener(e -> {
            String kw = ((JTextField) cbSearch.getEditor().getEditorComponent())
                    .getText()
                    .trim()
                    .toLowerCase();

            String currentRole = getCurrentUserRole();
            List<Employee> list = employeeSql.getAllNhanVien(currentRole);

            if (kw.isEmpty()) {
                updateTable(list);
                return;
            }

            List<Employee> filtered = new ArrayList<>();

            for (Employee emp : list) {
                String id = emp.getEmployeeId() != null ? emp.getEmployeeId().toLowerCase() : "";
                String name = emp.getEmployeeName() != null ? emp.getEmployeeName().toLowerCase() : "";
                String phone = emp.getPhone() != null ? emp.getPhone().toLowerCase() : "";
                String email = emp.getEmail() != null ? emp.getEmail().toLowerCase() : "";

                if (id.contains(kw)
                        || name.contains(kw)
                        || phone.contains(kw)
                        || email.contains(kw)) {
                    filtered.add(emp);
                }
            }

            updateTable(filtered);
        });
    }

    private Employee getEmployeeFromForm() {
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim().toLowerCase();

        String gender = rdoMale.isSelected()
                ? "Nam"
                : (rdoFemale.isSelected() ? "Nữ" : "");

        JTextField roleEditor = (JTextField) cbRole.getEditor().getEditorComponent();
        String role = roleEditor.getText().trim().toUpperCase();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || gender.isEmpty() || role.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng điền đầy đủ các thông tin cá nhân và chức vụ (*)");
            return null;
        }

        if (!role.equals("R_STAFF_SALE") && !role.equals("R_STAFF_VIEW_PROD")) {
            JOptionPane.showMessageDialog(
                    this,
                    "Phân quyền không hợp lệ!\n"
                    + "Quản lý chỉ được phép cấp quyền:\n"
                    + "- R_STAFF_SALE\n"
                    + "- R_STAFF_VIEW_PROD",
                    "Cảnh báo bảo mật",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }

        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Email không hợp lệ!",
                    "Lỗi định dạng",
                    JOptionPane.ERROR_MESSAGE
            );
            txtEmail.requestFocus();
            return null;
        }

        if (!isValidPhone(phone)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Số điện thoại không hợp lệ!",
                    "Lỗi định dạng",
                    JOptionPane.ERROR_MESSAGE
            );
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

        currentSelectedRawId = "";

        btngGender.clearSelection();
        tblEmployees.clearSelection();

        ((JTextField) cbSearch.getEditor().getEditorComponent()).setText("");
        ((JTextField) cbRole.getEditor().getEditorComponent()).setText("");
    }

    private void loadDataToTable() {
        String currentRole = getCurrentUserRole();
        updateTable(employeeSql.getAllNhanVien(currentRole));
    }

    private String getCurrentUserRole() {
        try {
            model.account.Account currentUser
                    = business.service.SessionManager.getCurrentUser();

            if (currentUser == null) {
                return "";
            }

            if (currentUser.getRoleId() != null && !currentUser.getRoleId().trim().isEmpty()) {
                return currentUser.getRoleId();
            }

            if (currentUser.getRoleValue() != null && !currentUser.getRoleValue().trim().isEmpty()) {
                return currentUser.getRoleValue();
            }

            if (currentUser.getRole() != null && !currentUser.getRole().trim().isEmpty()) {
                return currentUser.getRole();
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return "";
    }

    private String safeCell(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "—";
        }
        return value.trim();
    }

//    private String formatRoleName(String roleId) {
//        if (roleId == null) {
//            return "—";
//        }
//
//        switch (roleId) {
//            case "R_ADMIN_ALL":
//                return "Admin hệ thống";
//            case "R_STORE_MNG":
//                return "Quản lý cửa hàng";
//            case "R_STAFF_SALE":
//                return "Nhân viên bán hàng";
//            case "R_STAFF_VIEW_PROD":
//                return "Nhân viên xem sản phẩm";
//            default:
//                return roleId;
//        }
//    }
    private void updateTable(List<Employee> list) {
        tableModel.setRowCount(0);

        if (list == null) {
            return;
        }

        for (Employee emp : list) {
            String employeeId = emp.getEmployeeId();

            String accountStatus = emp.getAccountStatus() != null
                    ? emp.getAccountStatus().trim()
                    : "Chưa cấp";

            String onlineStatus = emp.getOnlineStatus() != null
                    ? emp.getOnlineStatus().trim()
                    : "N/A";

            if ("ONLINE".equalsIgnoreCase(onlineStatus) && emp.getActiveSessions() > 1) {
                onlineStatus = "ONLINE (" + emp.getActiveSessions() + ")";
            }

            tableModel.addRow(new Object[]{
                maskSensitiveInfo(employeeId),
                safeCell(emp.getEmployeeName()),
                safeCell(emp.getPhone()),
                safeCell(emp.getEmail()),
                safeCell(accountStatus),
                safeCell(onlineStatus),
                safeCell(emp.getRole()),
                safeCell(emp.getGender()),
                employeeId
            });
        }
    }

    private Map<String, String> loadOnlineStatusMap() {
        Map<String, String> map = new HashMap<>();

        String sql = "SELECT user_id, NVL(online_status, 'OFFLINE') AS online_status "
                + "FROM ACCOUNTS "
                + "WHERE NVL(is_deleted, 0) = 0";

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                map.put(
                        rs.getString("user_id"),
                        rs.getString("online_status")
                );
            }

        } catch (Exception e) {
            System.err.println("Lỗi loadOnlineStatusMap: " + e.getMessage());
        }

        return map;
    }

    private String maskSensitiveInfo(String info) {
        if (info == null || info.isEmpty()) {
            return "Chưa có dữ liệu";
        }

        if (info.length() > 6) {
            String visiblePart = info.substring(0, 6);

            StringBuilder hiddenPart = new StringBuilder();
            for (int i = 6; i < info.length(); i++) {
                hiddenPart.append("*");
            }

            return visiblePart + hiddenPart;
        }

        return info;
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
        editor.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 8),
                new EmptyBorder(5, 5, 5, 5)
        ));
    }

    private void styleSearchBox(JComboBox<String> cb) {
        cb.setEditable(true);
        cb.setBorder(null);
        cb.setBackground(Color.WHITE);

        ((JTextField) cb.getEditor().getEditorComponent())
                .setBorder(new EmptyBorder(0, 5, 0, 5));
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
                new RoundBorder(borderGray, 8),
                new EmptyBorder(5, 10, 5, 10)
        ));

        return txt;
    }

    private JButton createCustomButton(String t, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(t);

        if (icon != null) {
            btn.setIcon(new ImageIcon(
                    icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH)
            ));
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
                g2.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON
                );

                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 25, 25);

                super.paint(g2, c);
                g2.dispose();
            }
        });

        return btn;
    }

    private void setupTableStyle() {
        tblEmployees.setRowHeight(38);
        tblEmployees.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tblEmployees.setShowVerticalLines(false);
        tblEmployees.setShowHorizontalLines(false);
        tblEmployees.setSelectionBackground(new Color(237, 242, 255));
        tblEmployees.setSelectionForeground(textDark);
        tblEmployees.getTableHeader().setReorderingAllowed(false);
        tblEmployees.setFillsViewportHeight(true);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(bgLight);
        headerRenderer.setForeground(Color.BLACK);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        for (int i = 0; i < tblEmployees.getColumnModel().getColumnCount(); i++) {
            tblEmployees.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        UnifiedEmployeeRenderer renderer = new UnifiedEmployeeRenderer();

        for (int i = 0; i < tblEmployees.getColumnCount(); i++) {
            tblEmployees.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        setupEmployeeTableColumnWidth();
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
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

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
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

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
        return phone != null
                && !phone.isEmpty()
                && phone.matches("^(0)(3|5|7|8|9)[0-9]{8}$");
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

    class UnifiedEmployeeRenderer extends DefaultTableCellRenderer {

        private final Color currentUserBg = new Color(225, 245, 233);
        private final Color managerBg = new Color(230, 245, 233);
        private final Color adminBg = new Color(255, 235, 238);
        private final Color zebraBg = new Color(249, 251, 253);
        private final Color normalBg = Color.WHITE;
        private final Color selectedBg = new Color(212, 230, 241);
        private final Color onlineGreen = new Color(39, 174, 96);
        private final Color offlineRed = new Color(231, 76, 60);

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            super.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
            );

            int modelRow = table.convertRowIndexToModel(row);
            int modelColumn = table.convertColumnIndexToModel(column);

            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setHorizontalAlignment(JLabel.CENTER);

            String roleDisplay = String.valueOf(table.getModel().getValueAt(modelRow, COL_ROLE));
            String employeeId = String.valueOf(table.getModel().getValueAt(modelRow, COL_RAW_ID));

            String currentUserId = "";
            try {
                model.account.Account currentUser
                        = business.service.SessionManager.getCurrentUser();

                if (currentUser != null && currentUser.getUserId() != null) {
                    currentUserId = currentUser.getUserId();
                }
            } catch (Exception ignored) {
            }

            boolean isCurrentUserRow
                    = currentUserId != null
                    && !currentUserId.trim().isEmpty()
                    && currentUserId.equalsIgnoreCase(employeeId);

            boolean isAdminRow
                    = roleDisplay.contains("R_ADMIN_ALL")
                    || roleDisplay.toLowerCase().contains("admin");

            boolean isManagerRow
                    = roleDisplay.contains("R_STORE_MNG")
                    || roleDisplay.contains("MNG")
                    || roleDisplay.toLowerCase().contains("quản lý");

            if (isSelected) {
                setBackground(selectedBg);
                setForeground(textDark);
            } else if (isCurrentUserRow) {
                setBackground(currentUserBg);
                setForeground(new Color(25, 135, 84));
            } else if (isAdminRow) {
                setBackground(adminBg);
                setForeground(new Color(220, 53, 69));
            } else if (isManagerRow) {
                setBackground(managerBg);
                setForeground(new Color(25, 135, 84));
            } else {
                setBackground(row % 2 == 0 ? normalBg : zebraBg);
                setForeground(Color.BLACK);
            }

            if (modelColumn == COL_ONLINE_STATUS) {
                String status = value == null ? "N/A" : value.toString().trim();

                setFont(new Font("Segoe UI", Font.BOLD, 13));

                if (status.toUpperCase().startsWith("ONLINE")) {
                    setText("● " + status.replace("ONLINE", "Online"));
                    setForeground(onlineGreen);
                } else if ("OFFLINE".equalsIgnoreCase(status)) {
                    setText("● Offline");
                    setForeground(offlineRed);
                } else {
                    setText("—");
                    setForeground(textGray);
                }
            } else {
                setText(value == null ? "—" : value.toString());
            }

            return this;
        }
    }
}
