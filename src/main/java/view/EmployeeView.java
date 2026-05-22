package view;

import business.service.SessionManager;
import business.sql.hr_kpi.EmployeeSql;
import business.sql.hr_kpi.EmployeeShiftSql;
import business.sql.hr_kpi.ShiftSql;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;
import java.awt.*;
import java.awt.event.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.employee.Employee;
import model.employee.EmployeeShift;
import model.employee.Shift;
import view.components.IconHelper;
import business.service.ActivationTokenService;
import model.product.Store;
import business.sql.prod_inventory.StoresSql;

public class EmployeeView extends JPanel {

    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color primaryBlue = new Color(54, 92, 245);
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color borderGray = new Color(230, 235, 241);

    private final Color onlineGreen = new Color(39, 174, 96);
    private final Color offlineRed = new Color(231, 76, 60);
    private final Color primaryOrange = new Color(255, 112, 28);
    private final Color successGreen = new Color(22, 163, 74);

    private JTextField txtId, txtName, txtPhone, txtEmail;
    private JComboBox<String> cbRole, cbSearch;

    private JComboBox<String> cbStoreForm;
    private java.util.List<Store> listStores = new java.util.ArrayList<>();

    private JRadioButton rdoMale, rdoFemale;
    private ButtonGroup btngGender;
    private JTable tblEmployees;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch;

    private final EmployeeSql employeeSql = new EmployeeSql();
    private final ShiftSql shiftSql = new ShiftSql();
    private final EmployeeShiftSql employeeShiftSql = new EmployeeShiftSql();
    private List<String> employeeNameList = new ArrayList<>();
    private List<String> roleList = new ArrayList<>();

    private String currentSelectedRawId = "";

    private JComboBox<EmployeeOption> cbShiftEmployee;
    private JComboBox<ShiftOption> cbWorkShift;
    private JComboBox<String> cbAssignmentStatus;
    private JTextField txtWorkDate;
    private JTextArea txtAssignmentNote;
    private JLabel lblSelectedShiftEmployeeName;
    private JLabel lblSelectedShiftEmployeeId;
    private JLabel lblSelectedShiftEmployeeType;
    private JTable tblShiftAssignments;
    private DefaultTableModel shiftTableModel;
    private JTextField txtShiftKeyword;
    private JTextField txtShiftFilterDate;
    private JComboBox<String> cbShiftEmployeeTypeFilter;
    private JComboBox<ShiftOption> cbShiftFilter;
    private JComboBox<String> cbShiftStatusFilter;
    private JButton btnAddAssignment;
    private JButton btnUpdateAssignment;
    private JButton btnCancelAssignment;
    private JButton btnClearAssignment;
    private JButton btnApplyShiftFilter;
    private JButton btnResetShiftFilter;
    private List<Employee> assignableEmployees = new ArrayList<>();
    private List<Shift> shiftList = new ArrayList<>();
    private List<EmployeeShift> currentShiftAssignments = new ArrayList<>();
    private String selectedAssignmentId = "";
    private final SimpleDateFormat shiftTimeFormat = new SimpleDateFormat("HH:mm");

    // Model column indexes
    // Bảng hiện có thêm cột Chi nhánh ở vị trí 2,
    // nên toàn bộ index phía sau phải dịch sang phải 1 cột.
    private static final int COL_ID = 0;
    private static final int COL_NAME = 1;
    private static final int COL_STORE = 2;
    private static final int COL_PHONE = 3;
    private static final int COL_EMAIL = 4;
    private static final int COL_ACCOUNT_STATUS = 5;
    private static final int COL_ONLINE_STATUS = 6;
    private static final int COL_ROLE = 7;
    private static final int COL_GENDER = 8;
    private static final int COL_RAW_ID = 9;

    public EmployeeView() {
        if (!business.service.AuthorizationService.canAccessEmployees()) {
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
        tblEmployees.getColumnModel().getColumn(1).setPreferredWidth(160);  // Tên
        tblEmployees.getColumnModel().getColumn(2).setPreferredWidth(155);  // Chi nhánh
        tblEmployees.getColumnModel().getColumn(3).setPreferredWidth(115);  // SĐT
        tblEmployees.getColumnModel().getColumn(4).setPreferredWidth(230);  // Email
        tblEmployees.getColumnModel().getColumn(5).setPreferredWidth(120);  // Cấp tài khoản
        tblEmployees.getColumnModel().getColumn(6).setPreferredWidth(120);  // Hoạt động
        tblEmployees.getColumnModel().getColumn(7).setPreferredWidth(150);  // Chức vụ
        tblEmployees.getColumnModel().getColumn(8).setPreferredWidth(80);   // Giới tính
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
            // --- THÊM ĐOẠN NÀY ĐỂ TẢI DANH SÁCH CHI NHÁNH TỪ DATABASE VÀO COMBOBOX ---
            if (cbStoreForm != null) {
                cbStoreForm.removeAllItems();
                listStores = StoresSql.getInstance().selectAll();

                for (Store s : listStores) {
                    String storeLabel = s.getStoreName();

                    if (storeLabel == null || storeLabel.trim().isEmpty()) {
                        storeLabel = s.getAddress();
                    }

                    if (storeLabel == null || storeLabel.trim().isEmpty()) {
                        storeLabel = s.getStoreId();
                    }

                    cbStoreForm.addItem(storeLabel + " (" + s.getStoreId() + ")");
                }

                cbStoreForm.setSelectedIndex(-1);
                applyStoreScopeForManager();
            }
            // -----------------------------------------------------------------------

            loadDataToTable();
            loadAutoCompleteData();
            loadShiftComboboxData();
            loadShiftAssignments();
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
            String storeId = getCurrentStoreId();

            List<Employee> list;

            if (SessionManager.isStoreManager()) {
                list = employeeSql.getAllNhanVien(currentRole, storeId);
            } else {
                list = employeeSql.getAllNhanVien(currentRole, null);
            }

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

    private void selectStoreComboByStoreId(String storeId) {
        if (cbStoreForm == null || storeId == null || storeId.trim().isEmpty()) {
            return;
        }

        for (int i = 0; i < cbStoreForm.getItemCount(); i++) {
            String item = String.valueOf(cbStoreForm.getItemAt(i));

            if (item.contains("(" + storeId + ")") || item.endsWith(storeId + ")")) {
                cbStoreForm.setSelectedIndex(i);
                return;
            }
        }
    }

    private void applyStoreScopeForManager() {
        if (!SessionManager.isStoreManager()) {
            if (cbStoreForm != null) {
                cbStoreForm.setEnabled(true);
            }
            return;
        }

        String storeId = getCurrentStoreId();

        if (storeId == null || storeId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Tài khoản quản lý chưa được phân chi nhánh. Vui lòng liên hệ Admin.",
                    "Chưa phân chi nhánh",
                    JOptionPane.WARNING_MESSAGE
            );

            if (cbStoreForm != null) {
                cbStoreForm.setEnabled(false);
            }
            return;
        }

        selectStoreComboByStoreId(storeId);

        // Manager không được đổi chi nhánh
        if (cbStoreForm != null) {
            cbStoreForm.setEnabled(false);
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

        headerPanel.add(titlePanel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        JPanel profilePanel = new JPanel(new BorderLayout(0, 18));
        profilePanel.setOpaque(false);

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

        profilePanel.add(toolPanel, BorderLayout.NORTH);

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

        // --- THÊM KHỞI TẠO COMBOBOX CHI NHÁNH TẠI ĐÂY ---
        cbStoreForm = new JComboBox<>();
        cbStoreForm.setPreferredSize(new Dimension(280, 38));
        cbStoreForm.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbStoreForm.setBackground(Color.WHITE);
        // -----------------------------------------------

        txtPhone = createTextField("Nhập số điện thoại...");
        txtEmail = createTextField("Nhập email...");

        // --- ĐÃ SỬA: KHỞI TẠO COMBOBOX CHỨC VỤ LIỀN MẠCH (BƯỚC 1) ---
        cbRole = new JComboBox<>();
        cbRole.setPreferredSize(new Dimension(280, 38));
        cbRole.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbRole.setBackground(Color.WHITE);
        for (String r : roleList) {
            cbRole.addItem(r);
        }
        cbRole.setSelectedIndex(-1); // Đặt trạng thái ban đầu là trống
        // -------------------------------------------------------------

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

        // --- THÊM HIỂN THỊ COMBOBOX CHI NHÁNH LÊN FORM ---
        formCard.add(createLabel("Chi nhánh làm việc (*)"), addGbc(gbc, y++, 5));
        formCard.add(cbStoreForm, addGbc(gbc, y++, 15));
        // -------------------------------------------------

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
                    "Chi nhánh", // <-- ĐÃ BỔ SUNG CỘT CHI NHÁNH VÀO BẢNG
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

        // Ẩn cột RawId (Bây giờ RawId đã bị đẩy xuống vị trí số 9 do thêm cột Chi nhánh)
        tblEmployees.removeColumn(tblEmployees.getColumnModel().getColumn(9));

        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblEmployees);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        tableCard.add(scrollPane, BorderLayout.CENTER);

        centerPanel.add(formCard, BorderLayout.WEST);
        centerPanel.add(tableCard, BorderLayout.CENTER);

        profilePanel.add(centerPanel, BorderLayout.CENTER);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setOpaque(false);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setForeground(textDark);
        tabbedPane.addTab("Hồ sơ nhân viên", profilePanel);
        tabbedPane.addTab("Phân ca", createShiftAssignmentTab());
        tabbedPane.setUI(new UnderlineTabbedPaneUI(primaryOrange, borderGray));

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createShiftAssignmentTab() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(10, 0, 0, 0));

        root.add(createShiftFilterPanel(), BorderLayout.NORTH);

        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 0, 0, 14);
        gbc.weighty = 1.0;

        gbc.gridx = 0;
        gbc.weightx = 0.32;
        body.add(createShiftFormPanel(), gbc);

        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weightx = 0.68;
        body.add(createShiftListPanel(), gbc);

        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private JPanel createShiftFilterPanel() {
        RoundedPanel filterCard = new RoundedPanel(18, cardWhite);
        filterCard.setLayout(new GridBagLayout());
        filterCard.setBorder(new EmptyBorder(18, 18, 18, 18));

        txtShiftKeyword = createTextField("Tìm theo tên hoặc mã nhân viên...");
        txtShiftKeyword.setPreferredSize(new Dimension(220, 38));

        txtShiftFilterDate = createTextField("yyyy-MM-dd");
        txtShiftFilterDate.setPreferredSize(new Dimension(140, 38));

        cbShiftEmployeeTypeFilter = new JComboBox<>(new String[]{
            "Tất cả", "Nhân viên kho", "Nhân viên sale - thu ngân"
        });
        stylePlainCombo(cbShiftEmployeeTypeFilter);

        cbShiftFilter = new JComboBox<>();
        stylePlainCombo(cbShiftFilter);

        cbShiftStatusFilter = new JComboBox<>(new String[]{
            "Tất cả trạng thái", "ASSIGNED", "COMPLETED", "CANCELED"
        });
        stylePlainCombo(cbShiftStatusFilter);

        btnApplyShiftFilter = createCustomButton("Lọc", primaryOrange, Color.WHITE, null);
        btnResetShiftFilter = createCustomButton("Đặt lại", new Color(235, 239, 245), textDark, null);
        btnApplyShiftFilter.setPreferredSize(new Dimension(86, 38));
        btnResetShiftFilter.setPreferredSize(new Dimension(96, 38));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 6, 12);
        gbc.weightx = 1.0;

        addFilterField(filterCard, gbc, 0, "Tìm nhân viên", txtShiftKeyword);
        addFilterField(filterCard, gbc, 1, "Ngày làm việc", txtShiftFilterDate);
        addFilterField(filterCard, gbc, 2, "Loại nhân viên", cbShiftEmployeeTypeFilter);
        addFilterField(filterCard, gbc, 3, "Ca làm việc", cbShiftFilter);
        addFilterField(filterCard, gbc, 4, "Trạng thái", cbShiftStatusFilter);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 20));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnApplyShiftFilter);
        buttonPanel.add(btnResetShiftFilter);
        gbc.gridx = 5;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        filterCard.add(buttonPanel, gbc);

        return filterCard;
    }

    private void addFilterField(JPanel parent, GridBagConstraints gbc, int x, String label, JComponent field) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.setOpaque(false);
        wrapper.add(createLabel(label), BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);

        gbc.gridx = x;
        parent.add(wrapper, gbc);
    }

    private JPanel createShiftFormPanel() {
        RoundedPanel card = new RoundedPanel(18, cardWhite);
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("Tạo / Cập nhật phân ca");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(textDark);

        lblSelectedShiftEmployeeName = new JLabel("Chưa chọn nhân viên");
        lblSelectedShiftEmployeeName.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblSelectedShiftEmployeeName.setForeground(textDark);
        lblSelectedShiftEmployeeId = new JLabel("");
        lblSelectedShiftEmployeeId.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSelectedShiftEmployeeId.setForeground(new Color(107, 119, 140));
        lblSelectedShiftEmployeeId.setVisible(false);
        lblSelectedShiftEmployeeType = new JLabel("Loại nhân viên: —");
        lblSelectedShiftEmployeeType.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblSelectedShiftEmployeeType.setForeground(new Color(107, 119, 140));

        JPanel selectedPanel = new JPanel(new GridLayout(2, 1, 0, 3));
        selectedPanel.setOpaque(false);
        selectedPanel.add(lblSelectedShiftEmployeeName);
        selectedPanel.add(lblSelectedShiftEmployeeType);

        RoundedPanel selectedCard = new RoundedPanel(16, new Color(248, 250, 252));
        selectedCard.setLayout(new BorderLayout(10, 0));
        selectedCard.setBorder(new EmptyBorder(14, 14, 14, 14));
        selectedCard.add(selectedPanel, BorderLayout.CENTER);

        cbShiftEmployee = new JComboBox<>();
        stylePlainCombo(cbShiftEmployee);

        cbWorkShift = new JComboBox<>();
        stylePlainCombo(cbWorkShift);

        txtWorkDate = createTextField("yyyy-MM-dd");
        txtWorkDate.setText(LocalDate.now().toString());

        cbAssignmentStatus = new JComboBox<>(new String[]{"ASSIGNED", "COMPLETED", "CANCELED"});
        stylePlainCombo(cbAssignmentStatus);

        txtAssignmentNote = new JTextArea(4, 18);
        txtAssignmentNote.setLineWrap(true);
        txtAssignmentNote.setWrapStyleWord(true);
        txtAssignmentNote.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtAssignmentNote.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane noteScroll = new JScrollPane(txtAssignmentNote);
        noteScroll.setBorder(new RoundBorder(borderGray, 8));

        btnAddAssignment = createCustomButton("Thêm phân ca", primaryOrange, Color.WHITE, null);
        btnUpdateAssignment = createCustomButton("Cập nhật", primaryBlue, Color.WHITE, null);
        btnCancelAssignment = createCustomButton("Hủy phân ca", offlineRed, Color.WHITE, null);
        btnClearAssignment = createCustomButton("Làm mới", new Color(148, 163, 184), Color.WHITE, null);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 10, 10));
        buttons.setOpaque(false);
        buttons.add(btnAddAssignment);
        buttons.add(btnUpdateAssignment);
        buttons.add(btnCancelAssignment);
        buttons.add(btnClearAssignment);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        int y = 0;

        card.add(title, addGbc(gbc, y++, 14));
        card.add(selectedCard, addGbc(gbc, y++, 16));
        card.add(createLabel("Nhân viên (*)"), addGbc(gbc, y++, 5));
        card.add(cbShiftEmployee, addGbc(gbc, y++, 12));
        card.add(createLabel("Ca làm việc (*)"), addGbc(gbc, y++, 5));
        card.add(cbWorkShift, addGbc(gbc, y++, 12));
        card.add(createLabel("Ngày làm việc (*)"), addGbc(gbc, y++, 5));
        card.add(txtWorkDate, addGbc(gbc, y++, 12));
        card.add(createLabel("Trạng thái (*)"), addGbc(gbc, y++, 5));
        card.add(cbAssignmentStatus, addGbc(gbc, y++, 12));
        card.add(createLabel("Ghi chú"), addGbc(gbc, y++, 5));
        card.add(noteScroll, addGbc(gbc, y++, 16));
        card.add(buttons, addGbc(gbc, y++, 0));

        gbc.gridy = y;
        gbc.weighty = 1.0;
        card.add(Box.createVerticalGlue(), gbc);
        return card;
    }

    private JPanel createShiftListPanel() {
        RoundedPanel card = new RoundedPanel(18, cardWhite);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        header.setOpaque(false);
        JLabel title = new JLabel("Danh sách phân ca");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(textDark);
        header.add(title);

        shiftTableModel = new DefaultTableModel(new Object[]{
            "Mã phân ca", "Nhân viên", "Loại nhân viên", "Ngày làm việc",
            "Tên ca", "Giờ bắt đầu", "Giờ kết thúc", "Trạng thái", "Ghi chú"
        }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tblShiftAssignments = new JTable(shiftTableModel) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getRowCount() == 0) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setColor(textDark);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    String line1 = "Chưa có phân ca nào";
                    int x1 = (getWidth() - g2.getFontMetrics().stringWidth(line1)) / 2;
                    int y1 = Math.max(70, getHeight() / 2 - 8);
                    g2.drawString(line1, x1, y1);
                    g2.setColor(new Color(107, 119, 140));
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    String line2 = "Chọn nhân viên và thêm ca làm việc để bắt đầu.";
                    int x2 = (getWidth() - g2.getFontMetrics().stringWidth(line2)) / 2;
                    g2.drawString(line2, x2, y1 + 24);
                    g2.dispose();
                }
            }
        };
        setupShiftTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblShiftAssignments);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        card.add(header, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private void stylePlainCombo(JComboBox<?> combo) {
        combo.setPreferredSize(new Dimension(180, 38));
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(Color.WHITE);
        combo.setBorder(new RoundBorder(borderGray, 8));
    }

    private void setupShiftTableStyle() {
        tblShiftAssignments.setRowHeight(44);
        tblShiftAssignments.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblShiftAssignments.setShowVerticalLines(true);
        tblShiftAssignments.setShowHorizontalLines(true);
        tblShiftAssignments.setGridColor(new Color(235, 239, 245));
        tblShiftAssignments.setSelectionBackground(new Color(237, 242, 255));
        tblShiftAssignments.setSelectionForeground(textDark);
        tblShiftAssignments.getTableHeader().setReorderingAllowed(false);
        tblShiftAssignments.setFillsViewportHeight(true);
        tblShiftAssignments.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(new Color(248, 250, 252));
        headerRenderer.setForeground(textDark);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(10, 6, 10, 6));

        for (int i = 0; i < tblShiftAssignments.getColumnModel().getColumnCount(); i++) {
            tblShiftAssignments.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
            tblShiftAssignments.getColumnModel().getColumn(i).setCellRenderer(new ShiftAssignmentRenderer());
        }

        int[] widths = {105, 170, 160, 120, 100, 100, 100, 110, 220};
        for (int i = 0; i < widths.length; i++) {
            tblShiftAssignments.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
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

                String storeNameInTable = String.valueOf(tableModel.getValueAt(modelRow, COL_STORE));

                if (SessionManager.isStoreManager()) {
                    selectStoreComboByStoreId(getCurrentStoreId());
                    cbStoreForm.setEnabled(false);
                } else {
                    cbStoreForm.setSelectedIndex(-1);

                    for (int i = 0; i < cbStoreForm.getItemCount(); i++) {
                        String item = String.valueOf(cbStoreForm.getItemAt(i));
                        if (item.contains(storeNameInTable)) {
                            cbStoreForm.setSelectedIndex(i);
                            break;
                        }
                    }

                    cbStoreForm.setEnabled(true);
                }

                txtPhone.setText(String.valueOf(tableModel.getValueAt(modelRow, COL_PHONE)));
                txtEmail.setText(String.valueOf(tableModel.getValueAt(modelRow, COL_EMAIL)));

                String accountStatus = normalizeAccountStatus(
                        String.valueOf(tableModel.getValueAt(modelRow, COL_ACCOUNT_STATUS))
                );
                boolean isActivated = accountStatus.trim().equalsIgnoreCase("Đã cấp");

                if (isActivated) {
                    txtEmail.setEnabled(false);
                    txtEmail.setToolTipText("Tài khoản đã được cấp, không thể thay đổi Email.");
                } else {
                    txtEmail.setEnabled(true);
                    txtEmail.setToolTipText(null);
                }

                // --- ĐÃ SỬA THEO BƯỚC 3: Dùng setSelectedItem thay cho setText ---
                cbRole.setSelectedItem(role);
                // -----------------------------------------------------------------

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

            if (SessionManager.isStoreManager()) {
                emp.setStoreId(getCurrentStoreId());
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

            int updateRows;

            if (SessionManager.isStoreManager()) {
                emp.setStoreId(getCurrentStoreId());
                updateRows = employeeSql.updateInStore(emp, getCurrentStoreId());
            } else {
                updateRows = employeeSql.update(emp);
            }

            if (updateRows > 0) {
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
                        "Cập nhật thất bại hoặc bạn không có quyền thao tác nhân viên ngoài chi nhánh!",
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

                int deleteRows;

                if (SessionManager.isStoreManager()) {
                    deleteRows = employeeSql.deleteInStore(currentSelectedRawId, getCurrentStoreId());
                } else {
                    deleteRows = employeeSql.delete(currentSelectedRawId);
                }

                if (deleteRows > 0) {
                    RealtimeClient.send("EMPLOYEES_CHANGED");
                    refreshAllData();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "Xóa thất bại hoặc bạn không có quyền xóa nhân viên ngoài chi nhánh!",
                            "Không có quyền",
                            JOptionPane.WARNING_MESSAGE
                    );
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
            String storeId = getCurrentStoreId();

            List<Employee> list;

            if (SessionManager.isStoreManager()) {
                if (storeId == null || storeId.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Tài khoản quản lý chưa được phân chi nhánh. Vui lòng liên hệ Admin.",
                            "Chưa phân chi nhánh",
                            JOptionPane.WARNING_MESSAGE
                    );
                    updateTable(new ArrayList<>());
                    return;
                }

                list = employeeSql.getAllNhanVien(currentRole, storeId);
            } else {
                list = employeeSql.getAllNhanVien(currentRole, null);
            }

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
                String role = emp.getRole() != null ? emp.getRole().toLowerCase() : "";
                String storeName = emp.getStoreName() != null ? emp.getStoreName().toLowerCase() : "";

                if (id.contains(kw)
                        || name.contains(kw)
                        || phone.contains(kw)
                        || email.contains(kw)
                        || role.contains(kw)
                        || storeName.contains(kw)) {
                    filtered.add(emp);
                }
            }

            updateTable(filtered);
        });

        initShiftEvents();
    }

    private void initShiftEvents() {
        if (cbShiftEmployee == null) {
            return;
        }

        cbShiftEmployee.addActionListener(e -> updateSelectedShiftEmployeeCard());
        btnApplyShiftFilter.addActionListener(e -> loadShiftAssignments());
        btnResetShiftFilter.addActionListener(e -> {
            txtShiftKeyword.setText("");
            txtShiftFilterDate.setText("");
            cbShiftEmployeeTypeFilter.setSelectedIndex(0);
            cbShiftStatusFilter.setSelectedIndex(0);
            if (cbShiftFilter.getItemCount() > 0) {
                cbShiftFilter.setSelectedIndex(0);
            }
            loadShiftAssignments();
        });

        btnAddAssignment.addActionListener(e -> saveShiftAssignment(false));
        btnUpdateAssignment.addActionListener(e -> saveShiftAssignment(true));
        btnCancelAssignment.addActionListener(e -> cancelShiftAssignment());
        btnClearAssignment.addActionListener(e -> {
            clearShiftForm();
            loadShiftComboboxData();
            loadShiftAssignments();
        });

        tblShiftAssignments.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = tblShiftAssignments.getSelectedRow();
                if (row < 0) {
                    return;
                }
                int modelRow = tblShiftAssignments.convertRowIndexToModel(row);
                if (modelRow >= 0 && modelRow < currentShiftAssignments.size()) {
                    fillShiftForm(currentShiftAssignments.get(modelRow));
                }
            }
        });
    }

    private void loadShiftComboboxData() {
        if (cbShiftEmployee == null || cbWorkShift == null) {
            return;
        }

        Object selectedEmployee = cbShiftEmployee.getSelectedItem();
        Object selectedWorkShift = cbWorkShift.getSelectedItem();
        Object selectedFilterShift = cbShiftFilter.getSelectedItem();

        String currentRole = getCurrentUserRole();
        String storeId = getCurrentStoreId();

        if (SessionManager.isStoreManager()) {
            assignableEmployees = employeeSql.getAllNhanVien(currentRole, storeId);
        } else {
            assignableEmployees = employeeSql.getAllNhanVien(currentRole, null);
        }
        cbShiftEmployee.removeAllItems();
        for (Employee emp : assignableEmployees) {
            if (isShiftAssignableRole(emp.getRoleId())) {
                cbShiftEmployee.addItem(new EmployeeOption(emp));
            }
        }

        shiftList = shiftSql.selectAll();
        cbWorkShift.removeAllItems();
        cbShiftFilter.removeAllItems();
        cbShiftFilter.addItem(new ShiftOption(null, true));
        for (Shift shift : shiftList) {
            ShiftOption option = new ShiftOption(shift, false);
            cbWorkShift.addItem(option);
            cbShiftFilter.addItem(option);
        }

        restoreEmployeeSelection(selectedEmployee);
        restoreShiftSelection(cbWorkShift, selectedWorkShift);
        restoreShiftSelection(cbShiftFilter, selectedFilterShift);
        if (cbShiftEmployee.getSelectedIndex() < 0 && cbShiftEmployee.getItemCount() > 0) {
            cbShiftEmployee.setSelectedIndex(0);
        }
        updateSelectedShiftEmployeeCard();
    }

    private void loadShiftAssignments() {
        if (shiftTableModel == null) {
            return;
        }

        Date filterDate = parseOptionalSqlDate(txtShiftFilterDate != null ? txtShiftFilterDate.getText().trim() : "");
        String employeeTypeFilter = getEmployeeTypeFilterCode();
        ShiftOption filterShift = cbShiftFilter != null ? (ShiftOption) cbShiftFilter.getSelectedItem() : null;
        String shiftId = filterShift != null && !filterShift.all ? filterShift.shift.getShiftId() : "";
        String status = "";
        if (cbShiftStatusFilter != null && cbShiftStatusFilter.getSelectedIndex() > 0) {
            status = String.valueOf(cbShiftStatusFilter.getSelectedItem());
        }

        currentShiftAssignments = employeeShiftSql.selectAssignments(
                txtShiftKeyword != null ? txtShiftKeyword.getText().trim() : "",
                filterDate,
                employeeTypeFilter,
                shiftId,
                status
        );

        if (SessionManager.isStoreManager()) {
            String storeId = getCurrentStoreId();

            List<String> allowedEmployeeIds = new ArrayList<>();
            List<Employee> employeesInStore = employeeSql.getAllNhanVien(getCurrentUserRole(), storeId);

            for (Employee emp : employeesInStore) {
                allowedEmployeeIds.add(emp.getEmployeeId());
            }

            currentShiftAssignments.removeIf(item -> !allowedEmployeeIds.contains(item.getEmployeeId()));
        }
        shiftTableModel.setRowCount(0);
        for (EmployeeShift item : currentShiftAssignments) {
            shiftTableModel.addRow(new Object[]{
                item.getAssignmentId(),
                item.getEmployeeName(),
                item.getEmployeeType(),
                item.getWorkDate(),
                item.getShiftName(),
                item.getStartTimeText(),
                item.getEndTimeText(),
                item.getStatus(),
                item.getNote() == null ? "" : item.getNote()
            });
        }
        if (tblShiftAssignments != null) {
            tblShiftAssignments.repaint();
        }
    }

    private void saveShiftAssignment(boolean update) {
        EmployeeShift item = getShiftAssignmentFromForm();
        if (item == null) {
            return;
        }

        if (update) {
            if (selectedAssignmentId == null || selectedAssignmentId.isBlank()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một phân ca trong bảng để cập nhật.");
                return;
            }
            item.setAssignmentId(selectedAssignmentId);
        } else {
            item.setAssignmentId("PC" + System.currentTimeMillis());
        }

        if (employeeShiftSql.existsDuplicate(item.getEmployeeId(), item.getShiftId(), item.getWorkDate(),
                update ? item.getAssignmentId() : null)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Nhân viên này đã có phân ca cùng ngày và cùng ca làm việc.",
                    "Trùng phân ca",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        int rows = update ? employeeShiftSql.update(item) : employeeShiftSql.insert(item);
        if (rows > 0) {
            JOptionPane.showMessageDialog(this, update ? "Cập nhật phân ca thành công." : "Thêm phân ca thành công.");
            clearShiftForm();
            loadShiftAssignments();
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    update ? "Cập nhật phân ca thất bại. Vui lòng kiểm tra database." : "Thêm phân ca thất bại. Vui lòng kiểm tra database.",
                    "Lỗi database",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private void cancelShiftAssignment() {
        if (selectedAssignmentId == null || selectedAssignmentId.isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phân ca trong bảng để hủy.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Xác nhận hủy phân ca đang chọn?",
                "Hủy phân ca",
                JOptionPane.YES_NO_OPTION
        );
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        if (employeeShiftSql.cancel(selectedAssignmentId) > 0) {
            JOptionPane.showMessageDialog(this, "Đã chuyển phân ca sang trạng thái CANCELED.");
            clearShiftForm();
            loadShiftAssignments();
        } else {
            JOptionPane.showMessageDialog(this, "Hủy phân ca thất bại.", "Lỗi database", JOptionPane.ERROR_MESSAGE);
        }
    }

    private EmployeeShift getShiftAssignmentFromForm() {
        EmployeeOption employeeOption = cbShiftEmployee != null ? (EmployeeOption) cbShiftEmployee.getSelectedItem() : null;
        ShiftOption shiftOption = cbWorkShift != null ? (ShiftOption) cbWorkShift.getSelectedItem() : null;
        Date workDate = parseRequiredSqlDate(txtWorkDate != null ? txtWorkDate.getText().trim() : "");

        if (employeeOption == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn nhân viên cần phân ca.");
            return null;
        }
        if (shiftOption == null || shiftOption.all) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ca làm việc.");
            return null;
        }
        if (workDate == null) {
            return null;
        }

        EmployeeShift item = new EmployeeShift();
        item.setEmployeeId(employeeOption.employee.getEmployeeId());
        item.setShiftId(shiftOption.shift.getShiftId());
        item.setWorkDate(workDate);
        item.setStatus(String.valueOf(cbAssignmentStatus.getSelectedItem()));
        item.setNote(txtAssignmentNote.getText().trim());
        return item;
    }

    private void fillShiftForm(EmployeeShift item) {
        selectedAssignmentId = item.getAssignmentId();
        selectEmployeeOption(item.getEmployeeId());
        selectShiftOption(cbWorkShift, item.getShiftId());
        txtWorkDate.setText(item.getWorkDate() == null ? "" : item.getWorkDate().toString());
        cbAssignmentStatus.setSelectedItem(item.getStatus());
        txtAssignmentNote.setText(item.getNote() == null ? "" : item.getNote());
        updateSelectedShiftEmployeeCard();
    }

    private void clearShiftForm() {
        selectedAssignmentId = "";
        if (cbShiftEmployee != null) {
            cbShiftEmployee.setSelectedIndex(cbShiftEmployee.getItemCount() > 0 ? 0 : -1);
        }
        if (cbWorkShift != null) {
            cbWorkShift.setSelectedIndex(cbWorkShift.getItemCount() > 0 ? 0 : -1);
        }
        if (txtWorkDate != null) {
            txtWorkDate.setText(LocalDate.now().toString());
        }
        if (cbAssignmentStatus != null) {
            cbAssignmentStatus.setSelectedItem("ASSIGNED");
        }
        if (txtAssignmentNote != null) {
            txtAssignmentNote.setText("");
        }
        if (tblShiftAssignments != null) {
            tblShiftAssignments.clearSelection();
        }
        updateSelectedShiftEmployeeCard();
    }

    private void updateSelectedShiftEmployeeCard() {
        if (lblSelectedShiftEmployeeName == null) {
            return;
        }
        EmployeeOption option = cbShiftEmployee != null ? (EmployeeOption) cbShiftEmployee.getSelectedItem() : null;
        if (option == null) {
            lblSelectedShiftEmployeeName.setText("Chưa chọn nhân viên");
            lblSelectedShiftEmployeeId.setText("");
            lblSelectedShiftEmployeeType.setText("Loại nhân viên: —");
            return;
        }

        Employee emp = option.employee;
        lblSelectedShiftEmployeeName.setText(emp.getEmployeeName());
        lblSelectedShiftEmployeeId.setText("");
        lblSelectedShiftEmployeeType.setText("Loại nhân viên: " + getEmployeeTypeLabel(emp.getRoleId()));
    }

    private Date parseRequiredSqlDate(String value) {
        if (value == null || value.isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập ngày làm việc theo định dạng yyyy-MM-dd.");
            return null;
        }
        try {
            return Date.valueOf(LocalDate.parse(value.trim()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ngày làm việc không hợp lệ. Vui lòng nhập theo định dạng yyyy-MM-dd.",
                    "Lỗi định dạng",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }
    }

    private Date parseOptionalSqlDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Date.valueOf(LocalDate.parse(value.trim()));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                    this,
                    "Ngày lọc không hợp lệ. Vui lòng nhập theo định dạng yyyy-MM-dd.",
                    "Lỗi định dạng",
                    JOptionPane.WARNING_MESSAGE
            );
            return null;
        }
    }

    private boolean isShiftAssignableRole(String roleId) {
        return "R_STAFF_SALE".equalsIgnoreCase(roleId)
                || "R_STAFF_VIEW_PROD".equalsIgnoreCase(roleId)
                || "R_STAFF_STOCK".equalsIgnoreCase(roleId);
    }

    private String getEmployeeTypeLabel(String roleId) {
        if ("R_STAFF_VIEW_PROD".equalsIgnoreCase(roleId) || "R_STAFF_STOCK".equalsIgnoreCase(roleId)) {
            return "Nhân viên kho";
        }
        return "Nhân viên sale / Thu ngân";
    }

    private String getEmployeeTypeFilterCode() {
        if (cbShiftEmployeeTypeFilter == null) {
            return "ALL";
        }
        int index = cbShiftEmployeeTypeFilter.getSelectedIndex();
        if (index == 1) {
            return "WAREHOUSE";
        }
        if (index == 2) {
            return "SALE";
        }
        return "ALL";
    }

    private void restoreEmployeeSelection(Object previous) {
        if (previous instanceof EmployeeOption option) {
            selectEmployeeOption(option.employee.getEmployeeId());
        }
    }

    private void restoreShiftSelection(JComboBox<ShiftOption> combo, Object previous) {
        if (previous instanceof ShiftOption option && option.shift != null) {
            selectShiftOption(combo, option.shift.getShiftId());
        }
    }

    private void selectEmployeeOption(String employeeId) {
        if (employeeId == null || cbShiftEmployee == null) {
            return;
        }
        for (int i = 0; i < cbShiftEmployee.getItemCount(); i++) {
            EmployeeOption option = cbShiftEmployee.getItemAt(i);
            if (employeeId.equals(option.employee.getEmployeeId())) {
                cbShiftEmployee.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectShiftOption(JComboBox<ShiftOption> combo, String shiftId) {
        if (shiftId == null || combo == null) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            ShiftOption option = combo.getItemAt(i);
            if (!option.all && option.shift != null && shiftId.equals(option.shift.getShiftId())) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private String formatShiftTime(java.util.Date value) {
        return value == null ? "--:--" : shiftTimeFormat.format(value);
    }

    private Employee getEmployeeFromForm() {
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim().toLowerCase();

        String gender = rdoMale.isSelected()
                ? "Nam"
                : (rdoFemale.isSelected() ? "Nữ" : "");

        String role = "";
        if (cbRole.getSelectedIndex() >= 0) {
            role = cbRole.getSelectedItem().toString().trim().toUpperCase();
        }

        String storeId = "";

        if (SessionManager.isStoreManager()) {
            storeId = getCurrentStoreId();
        } else {
            if (cbStoreForm.getSelectedIndex() >= 0 && cbStoreForm.getSelectedItem() != null) {
                storeId = extractStoreIdFromComboText(cbStoreForm.getSelectedItem().toString());
            }
        }

        if (name.isEmpty()
                || phone.isEmpty()
                || email.isEmpty()
                || gender.isEmpty()
                || role.isEmpty()
                || storeId == null
                || storeId.trim().isEmpty()) {

            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng điền đầy đủ thông tin cá nhân, chức vụ và chi nhánh (*)"
            );
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

        // Quan trọng: Manager luôn bị ép store_id theo session
        e.setStoreId(storeId.trim());

        return e;
    }

    private String extractStoreIdFromComboText(String selectedStore) {
        if (selectedStore == null || selectedStore.trim().isEmpty()) {
            return "";
        }

        int open = selectedStore.lastIndexOf("(");
        int close = selectedStore.lastIndexOf(")");

        if (open >= 0 && close > open) {
            return selectedStore.substring(open + 1, close).trim();
        }

        return selectedStore.trim();
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

        // --- BỔ SUNG: Xóa lựa chọn Chi nhánh ---
        if (cbStoreForm != null) {
            if (SessionManager.isStoreManager()) {
                selectStoreComboByStoreId(getCurrentStoreId());
                cbStoreForm.setEnabled(false);
            } else {
                cbStoreForm.setSelectedIndex(-1);
                cbStoreForm.setEnabled(true);
            }
        }

        // --- ĐÃ SỬA THEO BƯỚC 4: Reset ComboBox Chức vụ liền mạch ---
        if (cbRole != null) {
            cbRole.setSelectedIndex(-1);
        }
        // -----------------------------------------------------------
    }

    private void loadDataToTable() {
        String currentRole = getCurrentUserRole();
        String storeId = getCurrentStoreId();

        if (SessionManager.isStoreManager()) {
            if (storeId == null || storeId.trim().isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Tài khoản quản lý chưa được phân chi nhánh. Vui lòng liên hệ Admin.",
                        "Chưa phân chi nhánh",
                        JOptionPane.WARNING_MESSAGE
                );
                updateTable(new ArrayList<>());
                return;
            }

            updateTable(employeeSql.getAllNhanVien(currentRole, storeId));
        } else {
            updateTable(employeeSql.getAllNhanVien(currentRole, null));
        }
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

    private String getCurrentStoreId() {
        try {
            return SessionManager.getCurrentStoreId();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    private String safeCell(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "—";
        }
        return value.trim();
    }

    private String normalizeAccountStatus(String status) {
        if (status == null) {
            return "Chưa cấp";
        }

        String value = status.trim();

        if (value.isEmpty()
                || value.equals("—")
                || value.equalsIgnoreCase("N/A")
                || value.equalsIgnoreCase("null")) {
            return "Chưa cấp";
        }

        return value;
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
            String storeName = safeCell(emp.getStoreName());
            if ("—".equals(storeName) || storeName.trim().isEmpty()) {
                storeName = "Chưa phân";
            }

            tableModel.addRow(new Object[]{
                maskSensitiveInfo(emp.getEmployeeId()),
                safeCell(emp.getEmployeeName()),
                storeName,
                safeCell(emp.getPhone()),
                safeCell(emp.getEmail()),
                normalizeAccountStatus(emp.getAccountStatus()),
                formatOnlineStatus(emp),
                safeCell(emp.getRoleId()),
                safeCell(emp.getGender()),
                emp.getEmployeeId()
            });
        }
    }

    private String formatOnlineStatus(Employee emp) {
        if (emp == null) {
            return "—";
        }

        String status = emp.getOnlineStatus();

        if (status == null || status.trim().isEmpty()) {
            return "—";
        }

        status = status.trim();

        if ("ONLINE".equalsIgnoreCase(status)) {
            int count = emp.getActiveSessions();

            if (count > 1) {
                return "Online (" + count + ")";
            }

            return "Online";
        }

        if ("OFFLINE".equalsIgnoreCase(status)) {
            return "Offline";
        }

        if ("N/A".equalsIgnoreCase(status)) {
            return "—";
        }

        return status;
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
            } else if (isCurrentUserRow && isManagerRow) {
                // Manager hiện tại đang đăng nhập
                setBackground(currentUserBg);
                setForeground(new Color(0, 120, 70));
            } else if (isManagerRow) {
                // Các dòng Manager khác
                setBackground(managerBg);
                setForeground(new Color(25, 135, 84));
            } else {
                // Admin và Staff giữ màu bình thường
                setBackground(row % 2 == 0 ? normalBg : zebraBg);
                setForeground(Color.BLACK);
            }

            if (modelColumn == COL_ONLINE_STATUS) {
                String status = value == null ? "—" : value.toString().trim();

                setFont(new Font("Segoe UI", Font.BOLD, 13));

                String normalized = status.toLowerCase();

                if (normalized.startsWith("online")) {
                    setText("● " + status);
                    setForeground(onlineGreen);
                } else if (normalized.startsWith("offline")) {
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

    class ShiftAssignmentRenderer extends DefaultTableCellRenderer {

        private final Color zebraBg = new Color(249, 251, 253);
        private final Color selectedBg = new Color(237, 242, 255);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            int modelColumn = table.convertColumnIndexToModel(column);
            setOpaque(true);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
            setHorizontalAlignment(modelColumn == 8 ? JLabel.LEFT : JLabel.CENTER);
            setBackground(isSelected ? selectedBg : (row % 2 == 0 ? Color.WHITE : zebraBg));
            setForeground(textDark);
            setText(value == null ? "" : value.toString());

            if (modelColumn == 7) {
                String status = value == null ? "" : value.toString();
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                if ("ASSIGNED".equalsIgnoreCase(status)) {
                    setForeground(successGreen);
                } else if ("COMPLETED".equalsIgnoreCase(status)) {
                    setForeground(primaryBlue);
                } else if ("CANCELED".equalsIgnoreCase(status)) {
                    setForeground(offlineRed);
                }
            }

            return this;
        }
    }

    class EmployeeOption {

        private final Employee employee;

        EmployeeOption(Employee employee) {
            this.employee = employee;
        }

        @Override
        public String toString() {
            return employee.getEmployeeName();
        }
    }

    class ShiftOption {

        private final Shift shift;
        private final boolean all;

        ShiftOption(Shift shift, boolean all) {
            this.shift = shift;
            this.all = all;
        }

        @Override
        public String toString() {
            if (all) {
                return "Tất cả ca";
            }
            return shift.getShiftName() + " (" + formatShiftTime(shift.getStartTime())
                    + " - " + formatShiftTime(shift.getEndTime()) + ")";
        }
    }

    static class UnderlineTabbedPaneUI extends javax.swing.plaf.basic.BasicTabbedPaneUI {

        private final Color activeColor;
        private final Color lineColor;

        UnderlineTabbedPaneUI(Color activeColor, Color lineColor) {
            this.activeColor = activeColor;
            this.lineColor = lineColor;
        }

        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets = new Insets(0, 0, 0, 0);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
            tabInsets = new Insets(10, 18, 12, 18);
        }

        @Override
        protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                int x, int y, int w, int h, boolean isSelected) {
            g.setColor(Color.WHITE);
            g.fillRect(x, y, w, h);
        }

        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
            g.setColor(lineColor);
            g.drawLine(0, 0, tabPane.getWidth(), 0);
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        }

        @Override
        protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                int x, int y, int w, int h, boolean isSelected) {
            if (isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(activeColor);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 14, y + h - 4, x + w - 14, y + h - 4);
                g2.dispose();
            }
        }
    }
}
