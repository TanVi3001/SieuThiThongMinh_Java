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
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
import business.service.RolePermissionService;

public class EmployeeView extends JPanel {

    // ==================== COLOR PALETTE ====================
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

    // ==================== TAB HỒ SƠ NHÂN VIÊN ====================
    private JTextField txtId, txtName, txtPhone, txtEmail;
    private JComboBox<String> cbRole, cbSearch;
    private JComboBox<String> cbStoreForm;
    private List<Store> listStores = new ArrayList<>();
    private JRadioButton rdoMale, rdoFemale;
    private ButtonGroup btngGender;
    private JTable tblEmployees;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear, btnSearch;

    // ==================== TAB PHÂN CA ====================
    private JComboBox<EmployeeOption> cbShiftEmployee;
    private JComboBox<ShiftOption> cbWorkShift;
    private JComboBox<String> cbAssignmentStatus;
    private JTextField txtWorkFromDate;
    private JTextField txtWorkToDate;
    private JTextField txtShiftFilterFromDate;
    private JTextField txtShiftFilterToDate;
    private JTextArea txtAssignmentNote;
    private ShiftTimelinePanel shiftTimelinePanel;
    private JLabel lblSelectedShiftEmployeeName;
    private JLabel lblSelectedShiftEmployeeId;
    private JLabel lblSelectedShiftEmployeeType;
    private JTextField txtShiftKeyword;
    private JComboBox<String> cbShiftEmployeeTypeFilter;
    private JComboBox<ShiftOption> cbShiftFilter;
    private JComboBox<String> cbShiftStatusFilter;
    private JButton btnAddAssignment;
    private JButton btnUpdateAssignment;
    private JButton btnCancelAssignment;
    private JButton btnDeleteAssignment;
    private JButton btnClearAssignment;
    private JButton btnApplyShiftFilter;
    private JButton btnResetShiftFilter;

    // ==================== BUSINESS LAYER ====================
    private final EmployeeSql employeeSql = new EmployeeSql();
    private final ShiftSql shiftSql = new ShiftSql();
    private final EmployeeShiftSql employeeShiftSql = new EmployeeShiftSql();
    private List<String> employeeNameList = new ArrayList<>();
    private List<String> roleList = new ArrayList<>();
    private String currentSelectedRawId = "";
    private List<Employee> assignableEmployees = new ArrayList<>();
    private List<Shift> shiftList = new ArrayList<>();
    private List<EmployeeShift> currentShiftAssignments = new ArrayList<>();
    private String selectedAssignmentId = "";
    private TimelineBlock selectedTimelineBlock;
    private final SimpleDateFormat shiftTimeFormat = new SimpleDateFormat("HH:mm");
    private static final DateTimeFormatter UI_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // Model column indexes
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
        view.util.RolePermissionButtonGuard.applyTo(this);
        applyPermissionMatrixToButtons();

        refreshAllData();
        setupRealtimeSync();
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

            loadDataToTable();
            loadAutoCompleteData();
            loadShiftComboboxData();
            loadShiftAssignments();
        });
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

    // ==================== UI INITIALIZATION ====================
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

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setOpaque(false);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setForeground(textDark);
        tabbedPane.addTab("Hồ sơ nhân viên", createEmployeeProfileTab());
        tabbedPane.addTab("Phân ca", createShiftAssignmentTab());
        tabbedPane.setUI(new UnderlineTabbedPaneUI(primaryOrange, borderGray));

        add(tabbedPane, BorderLayout.CENTER);
    }

    // ==================== TAB 1: HỒ SƠ NHÂN VIÊN ====================
    private JPanel createEmployeeProfileTab() {
        JPanel profilePanel = new JPanel(new BorderLayout(0, 18));
        profilePanel.setOpaque(false);

        // Tool panel (Search box)
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

        // Center panel (Form + Table)
        JPanel centerPanel = new JPanel(new BorderLayout(25, 0));
        centerPanel.setOpaque(false);

        centerPanel.add(createEmployeeFormPanel(), BorderLayout.WEST);
        centerPanel.add(createEmployeeListPanel(), BorderLayout.CENTER);

        profilePanel.add(centerPanel, BorderLayout.CENTER);

        return profilePanel;
    }

    private JPanel createEmployeeFormPanel() {
        RoundedPanel formCard = new RoundedPanel(20, cardWhite);
        formCard.setPreferredSize(new Dimension(360, 560));
        formCard.setLayout(new GridBagLayout());
        formCard.setBorder(new EmptyBorder(25, 25, 25, 25));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.NORTH;

        txtId = createTextField("Mã tự động...");
        txtId.setEnabled(false);

        txtName = createTextField("Nhập tên...");

        cbStoreForm = new JComboBox<>();
        cbStoreForm.setPreferredSize(new Dimension(280, 38));
        cbStoreForm.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbStoreForm.setBackground(Color.WHITE);

        txtPhone = createTextField("Nhập số điện thoại...");
        txtEmail = createTextField("Nhập email...");

        cbRole = new JComboBox<>();
        cbRole.setPreferredSize(new Dimension(280, 38));
        cbRole.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbRole.setBackground(Color.WHITE);
        for (String r : roleList) {
            cbRole.addItem(r);
        }
        cbRole.setSelectedIndex(-1);

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

        formCard.add(createLabel("Chi nhánh làm việc (*)"), addGbc(gbc, y++, 5));
        formCard.add(cbStoreForm, addGbc(gbc, y++, 15));

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

        GridBagConstraints formGlueGbc = new GridBagConstraints();
        formGlueGbc.gridx = 0;
        formGlueGbc.gridy = y;
        formGlueGbc.weighty = 1.0;
        formGlueGbc.fill = GridBagConstraints.VERTICAL;
        formCard.add(Box.createVerticalGlue(), formGlueGbc);

        return formCard;
    }

    private void applyPermissionMatrixToButtons() {
        if (btnAdd != null) {
            btnAdd.setEnabled(RolePermissionService.canAdd());
        }
        if (btnAddAssignment != null) {
            btnAddAssignment.setEnabled(RolePermissionService.canAdd());
        }
        if (btnUpdate != null) {
            btnUpdate.setEnabled(RolePermissionService.canEdit());
        }
        if (btnUpdateAssignment != null) {
            btnUpdateAssignment.setEnabled(RolePermissionService.canEdit());
        }
        if (btnDelete != null) {
            btnDelete.setEnabled(RolePermissionService.canDelete());
        }
        if (btnDeleteAssignment != null) {
            btnDeleteAssignment.setEnabled(RolePermissionService.canDelete());
        }
        if (btnCancelAssignment != null) {
            btnCancelAssignment.setEnabled(RolePermissionService.canEdit());
        }
    }

    private JPanel createEmployeeListPanel() {
        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        tableModel = new DefaultTableModel(
                new Object[]{
                    "Mã NV",
                    "Tên nhân viên",
                    "Chi nhánh",
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
        tblEmployees.removeColumn(tblEmployees.getColumnModel().getColumn(9));

        setupEmployeeTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblEmployees);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);

        tableCard.add(scrollPane, BorderLayout.CENTER);

        return tableCard;
    }

    private void setupEmployeeTableStyle() {
        tblEmployees.setRowHeight(34);
        tblEmployees.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblEmployees.setShowVerticalLines(false);
        tblEmployees.setShowHorizontalLines(false);
        tblEmployees.setSelectionBackground(new Color(237, 242, 255));
        tblEmployees.setSelectionForeground(textDark);
        tblEmployees.getTableHeader().setReorderingAllowed(false);
        tblEmployees.setFillsViewportHeight(true);
        tblEmployees.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(bgLight);
        headerRenderer.setForeground(Color.BLACK);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerRenderer.setHorizontalAlignment(JLabel.CENTER);
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(7, 5, 7, 5));

        for (int i = 0; i < tblEmployees.getColumnModel().getColumnCount(); i++) {
            tblEmployees.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }

        UnifiedEmployeeRenderer renderer = new UnifiedEmployeeRenderer();
        for (int i = 0; i < tblEmployees.getColumnCount(); i++) {
            tblEmployees.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        setupEmployeeTableColumnWidth();
    }

    private void setupEmployeeTableColumnWidth() {
        if (tblEmployees == null) {
            return;
        }

        tblEmployees.getColumnModel().getColumn(0).setPreferredWidth(95);
        tblEmployees.getColumnModel().getColumn(1).setPreferredWidth(160);
        tblEmployees.getColumnModel().getColumn(2).setPreferredWidth(155);
        tblEmployees.getColumnModel().getColumn(3).setPreferredWidth(115);
        tblEmployees.getColumnModel().getColumn(4).setPreferredWidth(230);
        tblEmployees.getColumnModel().getColumn(5).setPreferredWidth(120);
        tblEmployees.getColumnModel().getColumn(6).setPreferredWidth(120);
        tblEmployees.getColumnModel().getColumn(7).setPreferredWidth(150);
        tblEmployees.getColumnModel().getColumn(8).setPreferredWidth(80);
    }

    // ==================== TAB 2: PHÂN CA ====================
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
        filterCard.setLayout(new BorderLayout(10, 8));
        filterCard.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel title = new JLabel("Bộ lọc phân ca");
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(textDark);

        JLabel hint = new JLabel("Lọc theo khoảng ngày, loại nhân viên, ca làm việc và trạng thái");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        hint.setForeground(new Color(107, 119, 140));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(hint);
        top.add(titleBox, BorderLayout.WEST);

        txtShiftKeyword = createTextField("Tìm theo tên hoặc mã nhân viên...");
        txtShiftKeyword.setPreferredSize(new Dimension(190, 34));

        txtShiftFilterFromDate = createDateField(LocalDate.now());
        txtShiftFilterFromDate.setPreferredSize(new Dimension(150, 34));

        txtShiftFilterToDate = createDateField(LocalDate.now());
        txtShiftFilterToDate.setPreferredSize(new Dimension(150, 34));

        cbShiftEmployeeTypeFilter = new JComboBox<>(new String[]{
            "Tất cả", "Nhân viên kho", "Nhân viên sale - thu ngân"
        });
        stylePlainCombo(cbShiftEmployeeTypeFilter);
        cbShiftEmployeeTypeFilter.setPreferredSize(new Dimension(165, 34));

        cbShiftFilter = new JComboBox<>();
        stylePlainCombo(cbShiftFilter);
        cbShiftFilter.setPreferredSize(new Dimension(165, 34));

        cbShiftStatusFilter = new JComboBox<>(new String[]{
            "Tất cả trạng thái", "ASSIGNED", "COMPLETED", "CANCELED"
        });
        stylePlainCombo(cbShiftStatusFilter);
        cbShiftStatusFilter.setPreferredSize(new Dimension(165, 34));

        JPanel fields = new JPanel(new GridLayout(2, 3, 10, 7));
        fields.setOpaque(false);
        fields.add(createNotionFilterField("Tìm nhân viên", txtShiftKeyword));
        fields.add(createNotionFilterField("Từ ngày", txtShiftFilterFromDate));
        fields.add(createNotionFilterField("Đến ngày", txtShiftFilterToDate));
        fields.add(createNotionFilterField("Loại nhân viên", cbShiftEmployeeTypeFilter));
        fields.add(createNotionFilterField("Ca làm việc", cbShiftFilter));
        fields.add(createNotionFilterField("Trạng thái", cbShiftStatusFilter));

        btnApplyShiftFilter = createCustomButton("Lọc", primaryOrange, Color.WHITE, null);
        btnResetShiftFilter = createCustomButton("Đặt lại", new Color(235, 239, 245), textDark, null);
        btnApplyShiftFilter.setPreferredSize(new Dimension(86, 36));
        btnResetShiftFilter.setPreferredSize(new Dimension(86, 36));

        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 7));
        buttonPanel.setOpaque(false);
        buttonPanel.add(btnApplyShiftFilter);
        buttonPanel.add(btnResetShiftFilter);

        JPanel center = new JPanel(new BorderLayout(10, 0));
        center.setOpaque(false);
        center.add(fields, BorderLayout.CENTER);
        center.add(buttonPanel, BorderLayout.EAST);

        filterCard.add(top, BorderLayout.NORTH);
        filterCard.add(center, BorderLayout.CENTER);

        return filterCard;
    }

    private JPanel createNotionFilterField(String label, JComponent field) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.setOpaque(true);
        wrapper.setBackground(new Color(255, 251, 247));
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(new Color(255, 210, 180), 10),
                new EmptyBorder(7, 10, 7, 10)
        ));

        JLabel lbl = createLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(new Color(179, 82, 24));

        wrapper.add(lbl, BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);
        return wrapper;
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

        txtWorkFromDate = createDateField(LocalDate.now());
        txtWorkFromDate.setPreferredSize(new Dimension(280, 42));

        txtWorkToDate = createDateField(LocalDate.now());
        txtWorkToDate.setPreferredSize(new Dimension(280, 42));

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
        btnDeleteAssignment = createCustomButton("Xóa lịch", new Color(185, 28, 28), Color.WHITE, null);
        btnClearAssignment = createCustomButton("Làm mới", new Color(148, 163, 184), Color.WHITE, null);

        JPanel buttons = new JPanel(new GridLayout(3, 2, 10, 10));
        buttons.setOpaque(false);
        buttons.add(btnAddAssignment);
        buttons.add(btnUpdateAssignment);
        buttons.add(btnCancelAssignment);
        buttons.add(btnDeleteAssignment);
        buttons.add(btnClearAssignment);
        buttons.add(Box.createHorizontalStrut(1));

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
        card.add(createLabel("Từ ngày (*)"), addGbc(gbc, y++, 5));
        card.add(txtWorkFromDate, addGbc(gbc, y++, 12));
        card.add(createLabel("Đến ngày (*)"), addGbc(gbc, y++, 5));
        card.add(txtWorkToDate, addGbc(gbc, y++, 12));
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
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Lịch phân ca dạng timeline");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(textDark);

        JLabel hint = new JLabel("Kéo timeline bằng chuột để xem nhiều ngày, bấm block để xem chi tiết, bấm ô trống để tạo ca");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(new Color(107, 119, 140));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(hint);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        legend.setOpaque(false);
        legend.add(createLegendDot("Ca sáng", new Color(46, 204, 113)));
        legend.add(createLegendDot("Ca chiều", new Color(52, 152, 219)));
        legend.add(createLegendDot("Ca tối", new Color(155, 89, 182)));
        legend.add(createLegendDot("Full time", new Color(96, 125, 139)));
        legend.add(createLegendDot("Đã hủy", offlineRed));

        header.add(titleBox, BorderLayout.WEST);
        header.add(legend, BorderLayout.EAST);

        shiftTimelinePanel = new ShiftTimelinePanel();
        shiftTimelinePanel.setAssignments(currentShiftAssignments);

        JScrollPane scrollPane = new JScrollPane(shiftTimelinePanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);

        card.add(header, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);
        return card;
    }

    private JComponent createLegendDot(String text, Color color) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        panel.setOpaque(false);

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 14));
        dot.setForeground(color);

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(new Color(107, 119, 140));

        panel.add(dot);
        panel.add(label);
        return panel;
    }

    // ==================== EVENT HANDLERS ====================
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

                if (role.contains("R_ADMIN_ALL") || role.contains("R_STORE_MNG")
                        || role.contains("ADMIN") || role.contains("MNG")) {
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

                cbRole.setSelectedItem(role);

                String gender = String.valueOf(tableModel.getValueAt(modelRow, COL_GENDER));
                rdoMale.setSelected("Nam".equalsIgnoreCase(gender));
                rdoFemale.setSelected("Nữ".equalsIgnoreCase(gender));
            }
        });

        btnAdd.addActionListener(e -> handleAddEmployee());
        btnUpdate.addActionListener(e -> handleUpdateEmployee());
        btnDelete.addActionListener(e -> handleDeleteEmployee());
        btnClear.addActionListener(e -> clearForm());
        btnSearch.addActionListener(e -> handleSearchEmployee());

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
            txtShiftFilterFromDate.setText(formatUiDate(LocalDate.now()));
            txtShiftFilterToDate.setText(formatUiDate(LocalDate.now()));
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
        btnDeleteAssignment.addActionListener(e -> deleteShiftAssignment());
        btnClearAssignment.addActionListener(e -> {
            clearShiftForm();
            loadShiftComboboxData();
            loadShiftAssignments();
        });

        if (shiftTimelinePanel != null) {
            shiftTimelinePanel.setSelectionCallback(block -> {
                selectedTimelineBlock = block;
                if (block != null && block.getPrimary() != null) {
                    fillShiftForm(block);
                    showShiftDetailDialog(block);
                }
            });
        }
    }

    // ==================== EMPLOYEE CRUD OPERATIONS ====================
    private void handleAddEmployee() {
        if (!RolePermissionService.canAdd()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền Thêm nhân viên!",
                    "Từ chối quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        Employee emp = getEmployeeFromForm();
        if (emp == null) {
            return;
        }

        if (SessionManager.isStoreManager()) {
            emp.setStoreId(getCurrentStoreId());
        }

        if (isEmailDuplicate(emp.getEmail(), null)) {
            JOptionPane.showMessageDialog(this, "Email này đã được sử dụng cho một nhân viên khác!",
                    "Trùng lặp dữ liệu", JOptionPane.WARNING_MESSAGE);
            txtEmail.requestFocus();
            return;
        }

        if (isPhoneDuplicate(emp.getPhone(), null)) {
            JOptionPane.showMessageDialog(this, "Số điện thoại này đã được đăng ký cho người khác!",
                    "Trùng lặp dữ liệu", JOptionPane.WARNING_MESSAGE);
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
            String sqlToken = "SELECT token FROM (SELECT token FROM ACTIVATION_TOKENS "
                    + "WHERE employee_id = ? ORDER BY created_at DESC) WHERE ROWNUM = 1";

            try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sqlToken)) {
                ps.setString(1, emp.getEmployeeId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        actualToken = rs.getString("token");
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
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
                        JOptionPane.showMessageDialog(this, "Hồ sơ đã lưu nhưng gửi mail thất bại.",
                                "Lỗi Email", JOptionPane.WARNING_MESSAGE);
                    }
                });
            }).start();

            refreshAllData();
            clearForm();
        }
    }

    private void handleUpdateEmployee() {
        if (!RolePermissionService.canEdit()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền Sửa nhân viên!",
                    "Từ chối quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        String displayedId = txtId.getText();
        if (displayedId == null || displayedId.trim().isEmpty() || displayedId.startsWith("Mã")) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn nhân viên trong bảng để cập nhật!");
            return;
        }

        String idToUpdate = currentSelectedRawId;
        if (idToUpdate == null || idToUpdate.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy mã nhân viên gốc để cập nhật!");
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
                if (ex.getEmployeeId() != null && ex.getEmployeeId().equals(idToUpdate)) {
                    oldEmail = ex.getEmail() != null ? ex.getEmail() : "";
                    accStatus = ex.getAccountStatus() != null ? ex.getAccountStatus().trim() : "";
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        boolean isActivated = accStatus.equalsIgnoreCase("Đã cấp");
        boolean emailChanged = !oldEmail.equalsIgnoreCase(emp.getEmail());

        if (emailChanged && isActivated) {
            JOptionPane.showMessageDialog(this,
                    "Tài khoản của nhân viên này ĐÃ ĐƯỢC CẤP!\nNghiêm cấm thay đổi Email để bảo mật.",
                    "Bảo mật tài khoản", JOptionPane.ERROR_MESSAGE);
            txtEmail.setText(oldEmail);
            return;
        }

        if (isEmailDuplicate(emp.getEmail(), idToUpdate)) {
            JOptionPane.showMessageDialog(this, "Email này đã bị trùng với một nhân viên khác!",
                    "Trùng lặp dữ liệu", JOptionPane.WARNING_MESSAGE);
            txtEmail.requestFocus();
            return;
        }

        if (isPhoneDuplicate(emp.getPhone(), idToUpdate)) {
            JOptionPane.showMessageDialog(this, "Số điện thoại này đã bị trùng với một nhân viên khác!",
                    "Trùng lặp dữ liệu", JOptionPane.WARNING_MESSAGE);
            txtPhone.requestFocus();
            return;
        }

        String oldRole = getEmployeeRoleById(idToUpdate);
        emp.setEmployeeId(idToUpdate);

        if (SessionManager.isStoreManager()) {
            emp.setStoreId(getCurrentStoreId());
        }

        String newRole = emp.getRoleId();
        String oldRoleSafe = oldRole == null ? "" : oldRole.trim();
        String newRoleSafe = newRole == null ? "" : newRole.trim();
        boolean roleChanged = !oldRoleSafe.equalsIgnoreCase(newRoleSafe);

        int updateRows;
        if (SessionManager.isStoreManager()) {
            updateRows = employeeSql.updateInStore(emp, getCurrentStoreId());
        } else {
            updateRows = employeeSql.update(emp);
        }

        if (updateRows <= 0) {
            JOptionPane.showMessageDialog(this,
                    "Cập nhật thất bại hoặc bạn không có quyền thao tác nhân viên ngoài chi nhánh!",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (roleChanged && isActivated) {
            boolean roleSynced = business.sql.rbac.AccountSql.getInstance()
                    .updateAccountRoleByEmployeeId(idToUpdate, newRoleSafe);

            if (!roleSynced) {
                JOptionPane.showMessageDialog(this,
                        "Hồ sơ nhân viên đã cập nhật nhưng đồng bộ quyền tài khoản thất bại.\n"
                        + "Vui lòng kiểm tra lại bảng ACCOUNT_ASSIGN_ROLE.",
                        "Lỗi đồng bộ quyền", JOptionPane.WARNING_MESSAGE);
                refreshAllData();
                return;
            }

            touchAccountSecurityByEmployeeId(idToUpdate);
            business.service.AccountService.notifyAccountSecurityChanged("EMPLOYEE_ROLE_UPDATED");
        } else {
            RealtimeClient.send("EMPLOYEES_CHANGED");
        }

        if (emailChanged && !isActivated) {
            try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement("UPDATE USERS SET email = ? WHERE user_id = ?")) {
                ps.setString(1, emp.getEmail());
                ps.setString(2, emp.getEmployeeId());
                ps.executeUpdate();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            try {
                new ActivationTokenService().issueToken(emp.getEmployeeId());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            String actualToken = emp.getEmployeeId();
            String sqlToken = "SELECT token FROM (SELECT token FROM ACTIVATION_TOKENS "
                    + "WHERE employee_id = ? ORDER BY created_at DESC) WHERE ROWNUM = 1";

            try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sqlToken)) {
                ps.setString(1, emp.getEmployeeId());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        actualToken = rs.getString("token");
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            final String emailToSend = emp.getEmail();
            final String nameToSend = emp.getEmployeeName();
            final String codeToSend = actualToken;

            new Thread(() -> {
                boolean ok = business.service.EmailService.sendActivationEmail(emailToSend, nameToSend, codeToSend);
                SwingUtilities.invokeLater(() -> {
                    if (ok) {
                        JOptionPane.showMessageDialog(this,
                                "Đã cập nhật hồ sơ và gửi lại Mã Kích Hoạt mới tới:\n" + emailToSend);
                    } else {
                        JOptionPane.showMessageDialog(this, "Cập nhật thành công nhưng gửi mail thất bại!",
                                "Lỗi", JOptionPane.WARNING_MESSAGE);
                    }
                });
            }).start();
        } else {
            JOptionPane.showMessageDialog(this, "Cập nhật hồ sơ thành công!");
        }

        refreshAllData();
        clearForm();
    }

    private void handleDeleteEmployee() {
        if (!RolePermissionService.canDelete()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền Xóa nhân viên!",
                    "Từ chối quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (currentSelectedRawId == null || currentSelectedRawId.trim().isEmpty()) {
            return;
        }

        if (JOptionPane.showConfirmDialog(this, "Xác nhận xóa hồ sơ này?", "Xác nhận",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

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
                JOptionPane.showMessageDialog(this,
                        "Xóa thất bại hoặc bạn không có quyền xóa nhân viên ngoài chi nhánh!",
                        "Không có quyền", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void handleSearchEmployee() {
        String kw = ((JTextField) cbSearch.getEditor().getEditorComponent()).getText().trim().toLowerCase();
        String currentRole = getCurrentUserRole();
        String storeId = getCurrentStoreId();

        List<Employee> list;
        if (SessionManager.isStoreManager()) {
            if (storeId == null || storeId.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Tài khoản quản lý chưa được phân chi nhánh. Vui lòng liên hệ Admin.",
                        "Chưa phân chi nhánh", JOptionPane.WARNING_MESSAGE);
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
            String roleId = emp.getRoleId() != null ? emp.getRoleId().toLowerCase() : "";
            String storeName = emp.getStoreName() != null ? emp.getStoreName().toLowerCase() : "";

            if (id.contains(kw) || name.contains(kw) || phone.contains(kw)
                    || email.contains(kw) || role.contains(kw) || roleId.contains(kw) || storeName.contains(kw)) {
                filtered.add(emp);
            }
        }

        updateTable(filtered);
    }

    // ==================== SHIFT ASSIGNMENT OPERATIONS ====================
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
        Date filterFromDate = parseOptionalSqlDate(
                txtShiftFilterFromDate != null ? txtShiftFilterFromDate.getText().trim() : ""
        );
        Date filterToDate = parseOptionalSqlDate(
                txtShiftFilterToDate != null ? txtShiftFilterToDate.getText().trim() : ""
        );

        if (filterFromDate != null && filterToDate != null && filterFromDate.after(filterToDate)) {
            JOptionPane.showMessageDialog(this, "Từ ngày không được lớn hơn Đến ngày.",
                    "Khoảng ngày không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String employeeTypeFilter = getEmployeeTypeFilterCode();
        ShiftOption filterShift = cbShiftFilter != null ? (ShiftOption) cbShiftFilter.getSelectedItem() : null;
        String shiftId = filterShift != null && !filterShift.all ? filterShift.shift.getShiftId() : "";
        String status = "";
        if (cbShiftStatusFilter != null && cbShiftStatusFilter.getSelectedIndex() > 0) {
            status = String.valueOf(cbShiftStatusFilter.getSelectedItem());
        }

        currentShiftAssignments = employeeShiftSql.selectAssignments(
                txtShiftKeyword != null ? txtShiftKeyword.getText().trim() : "",
                filterFromDate,
                filterToDate,
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

        if (shiftTimelinePanel != null) {
            LocalDate displayFrom = filterFromDate != null ? filterFromDate.toLocalDate() : LocalDate.now();
            LocalDate displayTo = filterToDate != null ? filterToDate.toLocalDate() : displayFrom;
            shiftTimelinePanel.setDisplayRange(displayFrom, displayTo);
            shiftTimelinePanel.setAssignments(currentShiftAssignments);
        }
    }

    private void saveShiftAssignment(boolean update) {
        if (!update && !RolePermissionService.canAdd()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền Thêm phân ca!",
                    "Từ chối quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        if (update) {
            updateSelectedShiftBlockFromForm();
            return;
        }

        List<EmployeeShift> items = getShiftAssignmentsFromFormRange();
        if (items.isEmpty()) {
            return;
        }

        int success = 0;
        int duplicate = 0;
        int failed = 0;

        for (EmployeeShift item : items) {
            if (employeeShiftSql.existsDuplicate(item.getEmployeeId(), item.getShiftId(),
                    item.getWorkDate(), null)) {
                duplicate++;
                continue;
            }

            int rows = employeeShiftSql.insert(item);
            if (rows > 0) {
                success++;
            } else {
                failed++;
            }
        }

        JOptionPane.showMessageDialog(this,
                "Kết quả phân ca:\n"
                + "- Thêm thành công: " + success + "\n"
                + "- Bỏ qua do trùng: " + duplicate + "\n"
                + "- Thất bại: " + failed,
                "Hoàn tất phân ca", JOptionPane.INFORMATION_MESSAGE);

        clearShiftForm();
        loadShiftAssignments();
    }

    private void updateSelectedShiftBlockFromForm() {
        if (!RolePermissionService.canEdit()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền Sửa phân ca!",
                    "Từ chối quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (selectedTimelineBlock == null && (selectedAssignmentId == null || selectedAssignmentId.isBlank())) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn một lịch phân ca trên timeline để cập nhật.",
                    "Chưa chọn lịch",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        EmployeeOption employeeOption = cbShiftEmployee != null ? (EmployeeOption) cbShiftEmployee.getSelectedItem() : null;
        ShiftOption shiftOption = cbWorkShift != null ? (ShiftOption) cbWorkShift.getSelectedItem() : null;

        Date fromDate = parseRequiredSqlDate(txtWorkFromDate != null ? txtWorkFromDate.getText().trim() : "");
        Date toDate = parseRequiredSqlDate(txtWorkToDate != null ? txtWorkToDate.getText().trim() : "");

        if (employeeOption == null || employeeOption.employee == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn nhân viên cần cập nhật lịch.");
            return;
        }

        if (shiftOption == null || shiftOption.shift == null || shiftOption.all) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ca làm việc.");
            return;
        }

        if (fromDate == null || toDate == null) {
            return;
        }

        if (fromDate.after(toDate)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Từ ngày không được lớn hơn Đến ngày.",
                    "Khoảng ngày không hợp lệ",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        List<String> oldIds = new ArrayList<>();
        if (selectedTimelineBlock != null && !selectedTimelineBlock.getAssignmentIds().isEmpty()) {
            oldIds.addAll(selectedTimelineBlock.getAssignmentIds());
        } else if (selectedAssignmentId != null && !selectedAssignmentId.isBlank()) {
            oldIds.add(selectedAssignmentId);
        }

        if (oldIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không tìm thấy mã phân ca cũ để cập nhật.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                oldIds.size() > 1
                ? "Block này gồm " + oldIds.size() + " ngày. Cập nhật sẽ thay thế toàn bộ block cũ bằng lịch mới.\nBạn có muốn tiếp tục?"
                : "Cập nhật lịch phân ca đang chọn?",
                "Xác nhận cập nhật lịch",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int deleted = oldIds.size() == 1
                ? employeeShiftSql.delete(oldIds.get(0))
                : employeeShiftSql.deleteMany(oldIds);

        if (deleted <= 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể cập nhật vì xóa mềm lịch cũ thất bại.",
                    "Lỗi cập nhật",
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }

        LocalDate start = fromDate.toLocalDate();
        LocalDate end = toDate.toLocalDate();

        int success = 0;
        int duplicate = 0;
        int failed = 0;
        int sequence = 0;

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            EmployeeShift newItem = new EmployeeShift();
            newItem.setAssignmentId("PC" + System.currentTimeMillis() + "_UPD_" + (++sequence));
            newItem.setEmployeeId(employeeOption.employee.getEmployeeId());
            newItem.setShiftId(shiftOption.shift.getShiftId());
            newItem.setWorkDate(Date.valueOf(d));
            newItem.setStatus(String.valueOf(cbAssignmentStatus.getSelectedItem()));
            newItem.setNote(txtAssignmentNote == null ? "" : txtAssignmentNote.getText().trim());

            if (employeeShiftSql.existsDuplicate(newItem.getEmployeeId(), newItem.getShiftId(), newItem.getWorkDate(), null)) {
                duplicate++;
                continue;
            }

            int rows = employeeShiftSql.insert(newItem);
            if (rows > 0) {
                success++;
            } else {
                failed++;
            }
        }

        JOptionPane.showMessageDialog(
                this,
                "Kết quả cập nhật lịch:\n"
                + "- Đã xóa lịch cũ: " + deleted + "\n"
                + "- Tạo lịch mới thành công: " + success + "\n"
                + "- Bỏ qua do trùng: " + duplicate + "\n"
                + "- Thất bại: " + failed,
                "Cập nhật hoàn tất",
                JOptionPane.INFORMATION_MESSAGE
        );

        clearShiftForm();
        loadShiftAssignments();
    }

    private void cancelShiftAssignment() {
        if (!RolePermissionService.canEdit()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền Hủy/Sửa phân ca!",
                    "Từ chối quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (selectedAssignmentId == null || selectedAssignmentId.isBlank()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một phân ca trong bảng để hủy.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Xác nhận hủy phân ca đang chọn?",
                "Hủy phân ca", JOptionPane.YES_NO_OPTION);
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

    private void deleteShiftAssignment() {
        if (!RolePermissionService.canDelete()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền Xóa lịch phân ca!",
                    "Từ chối quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (selectedTimelineBlock == null && (selectedAssignmentId == null || selectedAssignmentId.isBlank())) {
            JOptionPane.showMessageDialog(
                    this,
                    "Vui lòng chọn lịch phân ca cần xóa trên timeline.",
                    "Chưa chọn lịch",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        List<String> idsToDelete = new ArrayList<>();

        if (selectedTimelineBlock != null && !selectedTimelineBlock.getAssignmentIds().isEmpty()) {
            idsToDelete.addAll(selectedTimelineBlock.getAssignmentIds());
        } else if (selectedAssignmentId != null && !selectedAssignmentId.isBlank()) {
            idsToDelete.add(selectedAssignmentId);
        }

        String message;
        if (idsToDelete.size() > 1) {
            message = "Block này gồm " + idsToDelete.size() + " ngày liên tiếp.\n"
                    + "Bạn có chắc muốn xóa toàn bộ lịch trong block này khỏi gantt không?\n\n"
                    + "Lưu ý: Xóa lịch sẽ ẩn khỏi timeline, khác với Hủy phân ca.";
        } else {
            message = "Bạn có chắc muốn xóa lịch phân ca đang chọn khỏi gantt không?\n\n"
                    + "Lưu ý: Xóa lịch sẽ ẩn khỏi timeline, khác với Hủy phân ca.";
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                message,
                "Xóa lịch phân ca",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        int deleted;
        if (idsToDelete.size() == 1) {
            deleted = employeeShiftSql.delete(idsToDelete.get(0));
        } else {
            deleted = employeeShiftSql.deleteMany(idsToDelete);
        }

        if (deleted > 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Đã xóa " + deleted + " lịch phân ca khỏi gantt.",
                    "Xóa lịch thành công",
                    JOptionPane.INFORMATION_MESSAGE
            );
            clearShiftForm();
            loadShiftAssignments();
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Xóa lịch thất bại. Vui lòng kiểm tra database.",
                    "Lỗi database",
                    JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private EmployeeShift getShiftAssignmentFromForm() {
        EmployeeOption employeeOption = cbShiftEmployee != null ? (EmployeeOption) cbShiftEmployee.getSelectedItem() : null;
        ShiftOption shiftOption = cbWorkShift != null ? (ShiftOption) cbWorkShift.getSelectedItem() : null;
        Date workDate = parseRequiredSqlDate(txtWorkFromDate != null ? txtWorkFromDate.getText().trim() : "");

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

    private List<EmployeeShift> getShiftAssignmentsFromFormRange() {
        List<EmployeeShift> result = new ArrayList<>();

        EmployeeOption employeeOption = cbShiftEmployee != null ? (EmployeeOption) cbShiftEmployee.getSelectedItem() : null;
        ShiftOption shiftOption = cbWorkShift != null ? (ShiftOption) cbWorkShift.getSelectedItem() : null;
        Date fromDate = parseRequiredSqlDate(txtWorkFromDate != null ? txtWorkFromDate.getText().trim() : "");
        Date toDate = parseRequiredSqlDate(txtWorkToDate != null ? txtWorkToDate.getText().trim() : "");

        if (employeeOption == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn nhân viên cần phân ca.");
            return result;
        }
        if (shiftOption == null || shiftOption.all) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ca làm việc.");
            return result;
        }
        if (fromDate == null || toDate == null) {
            return result;
        }

        if (fromDate.after(toDate)) {
            JOptionPane.showMessageDialog(this, "Từ ngày không được lớn hơn Đến ngày.",
                    "Khoảng ngày không hợp lệ", JOptionPane.WARNING_MESSAGE);
            return result;
        }

        LocalDate start = fromDate.toLocalDate();
        LocalDate end = toDate.toLocalDate();
        int sequence = 0;

        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            EmployeeShift item = new EmployeeShift();
            item.setAssignmentId("PC" + System.currentTimeMillis() + "_" + (++sequence));
            item.setEmployeeId(employeeOption.employee.getEmployeeId());
            item.setShiftId(shiftOption.shift.getShiftId());
            item.setWorkDate(Date.valueOf(d));
            item.setStatus(String.valueOf(cbAssignmentStatus.getSelectedItem()));
            item.setNote(txtAssignmentNote == null ? "" : txtAssignmentNote.getText().trim());
            result.add(item);
        }

        return result;
    }

    private void showShiftDetailDialog(EmployeeShift item) {
        if (item == null) {
            return;
        }

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Chi tiết ca làm việc", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(560, 430);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(22, 24, 20, 24));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JLabel title = new JLabel(item.getShiftName() == null ? "Ca làm việc" : item.getShiftName());
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(textDark);

        JLabel sub = new JLabel((item.getEmployeeName() == null ? "—" : item.getEmployeeName())
                + " • " + (item.getWorkDate() == null ? "—" : formatUiDate(item.getWorkDate().toLocalDate())));
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(new Color(107, 119, 140));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 3));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(sub);

        JLabel badge = new JLabel(normalizeShiftStatusForView(item.getStatus()), SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setForeground(Color.WHITE);
        badge.setBackground(getShiftStatusColor(item.getStatus()));
        badge.setBorder(new EmptyBorder(8, 14, 8, 14));

        header.add(titleBox, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);

        String[][] rows = new String[][]{
            {"Mã phân ca", safeDetail(item.getAssignmentId())},
            {"Nhân viên", safeDetail(item.getEmployeeName())},
            {"Loại nhân viên", safeDetail(item.getEmployeeType())},
            {"Ngày làm việc", item.getWorkDate() == null ? "—" : formatUiDate(item.getWorkDate().toLocalDate())},
            {"Ca làm việc", safeDetail(item.getShiftName())},
            {"Thời gian", safeDetail(item.getStartTimeText()) + " - " + safeDetail(item.getEndTimeText())},
            {"Trạng thái", normalizeShiftStatusForView(item.getStatus())},
            {"Ghi chú", safeDetail(item.getNote())}
        };

        JTable detailTable = new JTable(new DefaultTableModel(rows, new String[]{"Thông tin", "Giá trị"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        detailTable.setRowHeight(34);
        detailTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailTable.setShowHorizontalLines(true);
        detailTable.setShowVerticalLines(false);
        detailTable.setGridColor(new Color(226, 232, 240));
        detailTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        detailTable.getTableHeader().setBackground(new Color(248, 250, 252));
        detailTable.getTableHeader().setForeground(textDark);
        detailTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        detailTable.getColumnModel().getColumn(1).setPreferredWidth(330);

        detailTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setFont(new Font("Segoe UI", column == 0 ? Font.BOLD : Font.PLAIN, 13));
                setForeground(column == 0 ? new Color(43, 54, 116) : textDark);
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 251, 253));
                return this;
            }
        });

        JScrollPane tableScroll = new JScrollPane(detailTable);
        tableScroll.setBorder(new RoundBorder(new Color(210, 218, 230), 12));
        tableScroll.getViewport().setBackground(Color.WHITE);

        JButton btnEdit = createCustomButton("Đưa lên form", primaryBlue, Color.WHITE, null);
        JButton btnClose = createCustomButton("Đóng", new Color(235, 239, 245), textDark, null);
        btnEdit.setPreferredSize(new Dimension(130, 40));
        btnClose.setPreferredSize(new Dimension(100, 40));

        btnEdit.addActionListener(e -> {
            fillShiftForm(item);
            dialog.dispose();
        });
        btnClose.addActionListener(e -> dialog.dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.add(btnEdit);
        footer.add(btnClose);

        root.add(header, BorderLayout.NORTH);
        root.add(tableScroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private void fillShiftForm(EmployeeShift item) {
        if (item == null) {
            return;
        }

        selectedTimelineBlock = null;
        selectedAssignmentId = item.getAssignmentId();
        selectEmployeeOption(item.getEmployeeId());
        selectShiftOption(cbWorkShift, item.getShiftId());

        String d = item.getWorkDate() == null ? "" : formatUiDate(item.getWorkDate().toLocalDate());
        txtWorkFromDate.setText(d);
        txtWorkToDate.setText(d);

        cbAssignmentStatus.setSelectedItem(item.getStatus());
        txtAssignmentNote.setText(item.getNote() == null ? "" : item.getNote());
        updateSelectedShiftEmployeeCard();
    }

    private void fillShiftForm(TimelineBlock block) {
        if (block == null || block.getPrimary() == null) {
            return;
        }

        selectedTimelineBlock = block;

        EmployeeShift item = block.getPrimary();
        selectedAssignmentId = item.getAssignmentId();

        selectEmployeeOption(item.getEmployeeId());
        selectShiftOption(cbWorkShift, item.getShiftId());

        txtWorkFromDate.setText(block.startDate == null ? "" : formatUiDate(block.startDate));
        txtWorkToDate.setText(block.endDate == null ? "" : formatUiDate(block.endDate));

        cbAssignmentStatus.setSelectedItem(item.getStatus());
        txtAssignmentNote.setText(item.getNote() == null ? "" : item.getNote());
        updateSelectedShiftEmployeeCard();

        if (shiftTimelinePanel != null) {
            shiftTimelinePanel.setSelectedBlock(block);
        }
    }

    private void clearShiftForm() {
        selectedAssignmentId = "";
        selectedTimelineBlock = null;
        if (cbShiftEmployee != null) {
            cbShiftEmployee.setSelectedIndex(cbShiftEmployee.getItemCount() > 0 ? 0 : -1);
        }
        if (cbWorkShift != null) {
            cbWorkShift.setSelectedIndex(cbWorkShift.getItemCount() > 0 ? 0 : -1);
        }
        if (txtWorkFromDate != null) {
            txtWorkFromDate.setText(formatUiDate(LocalDate.now()));
        }
        if (txtWorkToDate != null) {
            txtWorkToDate.setText(formatUiDate(LocalDate.now()));
        }
        if (cbAssignmentStatus != null) {
            cbAssignmentStatus.setSelectedItem("ASSIGNED");
        }
        if (txtAssignmentNote != null) {
            txtAssignmentNote.setText("");
        }
        if (shiftTimelinePanel != null) {
            shiftTimelinePanel.clearSelectedAssignment();
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

    private void prepareNewShiftFromTimelineCell(String employeeId, LocalDate date, int clickedHour) {
        if (employeeId == null || date == null) {
            return;
        }

        selectedAssignmentId = "";
        selectEmployeeOption(employeeId);
        if (txtWorkFromDate != null) {
            txtWorkFromDate.setText(formatUiDate(date));
        }
        if (txtWorkToDate != null) {
            txtWorkToDate.setText(formatUiDate(date));
        }
        if (cbAssignmentStatus != null) {
            cbAssignmentStatus.setSelectedItem("ASSIGNED");
        }
        if (txtAssignmentNote != null && txtAssignmentNote.getText().trim().isEmpty()) {
            txtAssignmentNote.setText("");
        }
        selectBestShiftByClickedHour(clickedHour);
        updateSelectedShiftEmployeeCard();
        if (shiftTimelinePanel != null) {
            shiftTimelinePanel.clearSelectedAssignment();
        }

        if (cbWorkShift != null) {
            cbWorkShift.requestFocusInWindow();
        }
    }

    private void showNewShiftDialogFromTimelineCell(String employeeId, LocalDate date, int clickedHour) {
        if (!RolePermissionService.canAdd()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Bạn không có quyền Thêm phân ca!",
                    "Từ chối quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (employeeId == null || date == null) {
            return;
        }

        // Vẫn fill nhẹ vào form bên trái để người dùng thấy context đang chọn.
        prepareNewShiftFromTimelineCell(employeeId, date, clickedHour);

        JDialog dialog = new JDialog(
                SwingUtilities.getWindowAncestor(this),
                "Thêm lịch làm việc",
                Dialog.ModalityType.APPLICATION_MODAL
        );
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(760, 680);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(22, 24, 22, 24));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JLabel title = new JLabel("Thêm lịch làm việc mới");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(textDark);

        JLabel sub = new JLabel("Tạo phân ca trực tiếp từ ô timeline vừa chọn.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(new Color(107, 119, 140));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 4));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(sub);
        header.add(titleBox, BorderLayout.WEST);

        JComboBox<EmployeeOption> dlgEmployee = new JComboBox<>();
        for (Employee emp : assignableEmployees) {
            if (emp != null && isShiftAssignableRole(emp.getRoleId())) {
                dlgEmployee.addItem(new EmployeeOption(emp));
            }
        }
        stylePlainCombo(dlgEmployee);
        selectEmployeeOptionInCombo(dlgEmployee, employeeId);

        JComboBox<ShiftOption> dlgShift = new JComboBox<>();
        for (Shift shift : shiftList) {
            dlgShift.addItem(new ShiftOption(shift, false));
        }
        stylePlainCombo(dlgShift);
        selectBestShiftByClickedHour(dlgShift, clickedHour);

        JTextField dlgFromDate = createDateField(date);
        JTextField dlgToDate = createDateField(date);
        dlgFromDate.setPreferredSize(new Dimension(240, 40));
        dlgToDate.setPreferredSize(new Dimension(240, 40));

        JComboBox<String> dlgStatus = new JComboBox<>(new String[]{"ASSIGNED", "COMPLETED", "CANCELED"});
        stylePlainCombo(dlgStatus);
        dlgStatus.setSelectedItem("ASSIGNED");

        JTextArea dlgNote = new JTextArea(4, 18);
        dlgNote.setLineWrap(true);
        dlgNote.setWrapStyleWord(true);
        dlgNote.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dlgNote.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane noteScroll = new JScrollPane(dlgNote);
        noteScroll.setBorder(new RoundBorder(new Color(210, 218, 230), 10));
        noteScroll.getViewport().setBackground(Color.WHITE);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        int y = 0;

        form.add(createDialogField("Nhân viên (*)", dlgEmployee), addGbc(gbc, y++, 10));
        form.add(createDialogField("Ca làm việc (*)", dlgShift), addGbc(gbc, y++, 10));

        JPanel dateRow = new JPanel(new GridLayout(1, 2, 12, 0));
        dateRow.setOpaque(false);
        dateRow.add(createDialogField("Từ ngày (*)", dlgFromDate));
        dateRow.add(createDialogField("Đến ngày (*)", dlgToDate));
        form.add(dateRow, addGbc(gbc, y++, 10));

        form.add(createDialogField("Trạng thái (*)", dlgStatus), addGbc(gbc, y++, 10));
        form.add(createDialogField("Ghi chú", noteScroll), addGbc(gbc, y++, 0));

        JButton btnSave = createCustomButton("Thêm lịch", primaryOrange, Color.WHITE, null);
        JButton btnCancel = createCustomButton("Đóng", new Color(235, 239, 245), textDark, null);
        btnSave.setPreferredSize(new Dimension(130, 42));
        btnCancel.setPreferredSize(new Dimension(100, 42));

        btnSave.addActionListener(e -> {
            if (!RolePermissionService.canAdd()) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Bạn không có quyền Thêm phân ca!",
                        "Từ chối quyền",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            EmployeeOption employeeOption = (EmployeeOption) dlgEmployee.getSelectedItem();
            ShiftOption shiftOption = (ShiftOption) dlgShift.getSelectedItem();

            if (employeeOption == null || employeeOption.employee == null) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn nhân viên.");
                return;
            }
            if (shiftOption == null || shiftOption.shift == null || shiftOption.all) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn ca làm việc.");
                return;
            }

            LocalDate from = parseUiDate(dlgFromDate.getText());
            LocalDate to = parseUiDate(dlgToDate.getText());

            if (from == null || to == null) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Ngày không hợp lệ. Vui lòng nhập theo định dạng dd/MM/yyyy.",
                        "Lỗi định dạng ngày",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            if (from.isAfter(to)) {
                JOptionPane.showMessageDialog(
                        dialog,
                        "Từ ngày không được lớn hơn Đến ngày.",
                        "Khoảng ngày không hợp lệ",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            int success = 0;
            int duplicate = 0;
            int failed = 0;
            int sequence = 0;

            for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
                EmployeeShift item = new EmployeeShift();
                item.setAssignmentId("PC" + System.currentTimeMillis() + "_DLG_" + (++sequence));
                item.setEmployeeId(employeeOption.employee.getEmployeeId());
                item.setShiftId(shiftOption.shift.getShiftId());
                item.setWorkDate(Date.valueOf(d));
                item.setStatus(String.valueOf(dlgStatus.getSelectedItem()));
                item.setNote(dlgNote.getText() == null ? "" : dlgNote.getText().trim());

                if (employeeShiftSql.existsDuplicate(item.getEmployeeId(), item.getShiftId(), item.getWorkDate(), null)) {
                    duplicate++;
                    continue;
                }

                int rows = employeeShiftSql.insert(item);
                if (rows > 0) {
                    success++;
                } else {
                    failed++;
                }
            }

            JOptionPane.showMessageDialog(
                    dialog,
                    "Kết quả thêm lịch:\n"
                    + "- Thêm thành công: " + success + "\n"
                    + "- Bỏ qua do trùng: " + duplicate + "\n"
                    + "- Thất bại: " + failed,
                    "Hoàn tất",
                    JOptionPane.INFORMATION_MESSAGE
            );

            if (success > 0) {
                // Đồng bộ lại form bên trái theo đúng thông tin vừa thêm nhanh.
                selectEmployeeOption(employeeOption.employee.getEmployeeId());
                selectShiftOption(cbWorkShift, shiftOption.shift.getShiftId());
                txtWorkFromDate.setText(formatUiDate(from));
                txtWorkToDate.setText(formatUiDate(to));
                cbAssignmentStatus.setSelectedItem(String.valueOf(dlgStatus.getSelectedItem()));
                txtAssignmentNote.setText(dlgNote.getText() == null ? "" : dlgNote.getText().trim());
                selectedAssignmentId = "";
                selectedTimelineBlock = null;
                updateSelectedShiftEmployeeCard();

                dialog.dispose();
                loadShiftAssignments();
            }
        });

        btnCancel.addActionListener(e -> dialog.dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.add(btnSave);
        footer.add(btnCancel);

        root.add(header, BorderLayout.NORTH);
        root.add(form, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private JPanel createDialogField(String label, JComponent field) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 6));
        wrapper.setOpaque(false);

        JLabel lbl = createLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(textDark);

        wrapper.add(lbl, BorderLayout.NORTH);
        wrapper.add(field, BorderLayout.CENTER);
        return wrapper;
    }

    private void selectEmployeeOptionInCombo(JComboBox<EmployeeOption> combo, String employeeId) {
        if (combo == null || employeeId == null) {
            return;
        }
        for (int i = 0; i < combo.getItemCount(); i++) {
            EmployeeOption option = combo.getItemAt(i);
            if (option != null && option.employee != null
                    && employeeId.equals(option.employee.getEmployeeId())) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private void selectBestShiftByClickedHour(JComboBox<ShiftOption> combo, int clickedHour) {
        if (combo == null || combo.getItemCount() == 0) {
            return;
        }

        int targetMinutes = Math.max(0, Math.min(23, clickedHour)) * 60;
        int fallbackIndex = -1;

        for (int i = 0; i < combo.getItemCount(); i++) {
            ShiftOption option = combo.getItemAt(i);
            if (option == null || option.all || option.shift == null) {
                continue;
            }

            if (fallbackIndex < 0) {
                fallbackIndex = i;
            }

            String shiftId = option.shift.getShiftId() == null ? "" : option.shift.getShiftId().toUpperCase();
            String shiftName = option.shift.getShiftName() == null ? "" : option.shift.getShiftName().toLowerCase();
            if (shiftId.contains("FULL") || shiftName.contains("full")) {
                continue;
            }

            int start = minutesOf(option.shift.getStartTime());
            int end = minutesOf(option.shift.getEndTime());

            boolean match;
            if (start <= end) {
                match = targetMinutes >= start && targetMinutes < end;
            } else {
                match = targetMinutes >= start || targetMinutes < end;
            }

            if (match) {
                combo.setSelectedIndex(i);
                return;
            }
        }

        if (fallbackIndex >= 0) {
            combo.setSelectedIndex(fallbackIndex);
        }
    }

    private void selectBestShiftByClickedHour(int clickedHour) {
        if (cbWorkShift == null || cbWorkShift.getItemCount() == 0) {
            return;
        }

        int targetMinutes = Math.max(0, Math.min(23, clickedHour)) * 60;
        int fallbackIndex = -1;

        for (int i = 0; i < cbWorkShift.getItemCount(); i++) {
            ShiftOption option = cbWorkShift.getItemAt(i);
            if (option == null || option.all || option.shift == null) {
                continue;
            }

            if (fallbackIndex < 0) {
                fallbackIndex = i;
            }

            String shiftId = option.shift.getShiftId() == null ? "" : option.shift.getShiftId().toUpperCase();
            String shiftName = option.shift.getShiftName() == null ? "" : option.shift.getShiftName().toLowerCase();
            if (shiftId.contains("FULL") || shiftName.contains("full")) {
                continue;
            }

            int start = minutesOf(option.shift.getStartTime());
            int end = minutesOf(option.shift.getEndTime());

            boolean match;
            if (start <= end) {
                match = targetMinutes >= start && targetMinutes < end;
            } else {
                match = targetMinutes >= start || targetMinutes < end;
            }

            if (match) {
                cbWorkShift.setSelectedIndex(i);
                return;
            }
        }

        if (fallbackIndex >= 0) {
            cbWorkShift.setSelectedIndex(fallbackIndex);
        }
    }

    // ==================== UTILITY METHODS ====================
    private void applyStoreScopeForManager() {
        if (!SessionManager.isStoreManager()) {
            if (cbStoreForm != null) {
                cbStoreForm.setEnabled(true);
            }
            return;
        }

        String storeId = getCurrentStoreId();
        if (storeId == null || storeId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Tài khoản quản lý chưa được phân chi nhánh. Vui lòng liên hệ Admin.",
                    "Chưa phân chi nhánh", JOptionPane.WARNING_MESSAGE);
            if (cbStoreForm != null) {
                cbStoreForm.setEnabled(false);
            }
            return;
        }

        selectStoreComboByStoreId(storeId);
        if (cbStoreForm != null) {
            cbStoreForm.setEnabled(false);
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

        if (cbStoreForm != null) {
            if (SessionManager.isStoreManager()) {
                selectStoreComboByStoreId(getCurrentStoreId());
                cbStoreForm.setEnabled(false);
            } else {
                cbStoreForm.setSelectedIndex(-1);
                cbStoreForm.setEnabled(true);
            }
        }

        if (cbRole != null) {
            cbRole.setSelectedIndex(-1);
        }
    }

    private void loadDataToTable() {
        String currentRole = getCurrentUserRole();
        String storeId = getCurrentStoreId();

        if (SessionManager.isStoreManager()) {
            if (storeId == null || storeId.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Tài khoản quản lý chưa được phân chi nhánh. Vui lòng liên hệ Admin.",
                        "Chưa phân chi nhánh", JOptionPane.WARNING_MESSAGE);
                updateTable(new ArrayList<>());
                return;
            }
            updateTable(employeeSql.getAllNhanVien(currentRole, storeId));
        } else {
            updateTable(employeeSql.getAllNhanVien(currentRole, null));
        }
    }

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

    private Employee getEmployeeFromForm() {
        String name = txtName.getText().trim();
        String phone = txtPhone.getText().trim();
        String email = txtEmail.getText().trim().toLowerCase();
        String gender = rdoMale.isSelected() ? "Nam" : (rdoFemale.isSelected() ? "Nữ" : "");

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

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty() || gender.isEmpty()
                || role.isEmpty() || storeId == null || storeId.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng điền đầy đủ thông tin cá nhân, chức vụ và chi nhánh (*)");
            return null;
        }

        if (!role.equals("R_STAFF_SALE") && !role.equals("R_STAFF_VIEW_PROD")) {
            JOptionPane.showMessageDialog(this,
                    "Phân quyền không hợp lệ!\nQuản lý chỉ được phép cấp quyền:\n- R_STAFF_SALE\n- R_STAFF_VIEW_PROD",
                    "Cảnh báo bảo mật", JOptionPane.WARNING_MESSAGE);
            return null;
        }

        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(this, "Email không hợp lệ!", "Lỗi định dạng", JOptionPane.ERROR_MESSAGE);
            txtEmail.requestFocus();
            return null;
        }

        if (!isValidPhone(phone)) {
            JOptionPane.showMessageDialog(this, "Số điện thoại không hợp lệ!",
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

    private Date parseRequiredSqlDate(String value) {
        LocalDate parsed = parseUiDate(value);
        if (parsed == null) {
            JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập ngày theo định dạng dd/MM/yyyy. Ví dụ: 25/05/2026.",
                    "Lỗi định dạng ngày", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return Date.valueOf(parsed);
    }

    private Date parseOptionalSqlDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        LocalDate parsed = parseUiDate(value);
        if (parsed == null) {
            JOptionPane.showMessageDialog(this,
                    "Ngày lọc không hợp lệ. Vui lòng nhập theo định dạng dd/MM/yyyy. Ví dụ: 25/05/2026.",
                    "Lỗi định dạng ngày", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return Date.valueOf(parsed);
    }

    private String formatUiDate(LocalDate date) {
        return date == null ? "" : UI_DATE_FORMAT.format(date);
    }

    private LocalDate parseUiDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String text = value.trim();
        try {
            return LocalDate.parse(text, UI_DATE_FORMAT);
        } catch (Exception ignored) {
        }

        try {
            return LocalDate.parse(text);
        } catch (Exception ignored) {
        }

        return null;
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

    private int minutesOf(java.util.Date value) {
        if (value == null) {
            return 0;
        }

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(value);
        return cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE);
    }

    private String safeCell(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "—";
        }
        return value.trim();
    }

    private void showShiftDetailDialog(TimelineBlock block) {
        if (block == null || block.getPrimary() == null) {
            return;
        }

        EmployeeShift item = block.getPrimary();
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Chi tiết ca làm việc", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setSize(760, 620);
        dialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(22, 24, 20, 24));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);

        JLabel title = new JLabel(item.getShiftName() == null ? "Ca làm việc" : item.getShiftName());
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(textDark);

        String dateRange = formatUiDate(block.startDate)
                + (block.startDate.equals(block.endDate) ? "" : " - " + formatUiDate(block.endDate));
        JLabel sub = new JLabel((item.getEmployeeName() == null ? "—" : item.getEmployeeName()) + " • " + dateRange);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(new Color(107, 119, 140));

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 3));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(sub);

        JLabel badge = new JLabel(normalizeShiftStatusForView(item.getStatus()), SwingConstants.CENTER);
        badge.setOpaque(true);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setForeground(Color.WHITE);
        badge.setBackground(getShiftStatusColor(item.getStatus()));
        badge.setBorder(new EmptyBorder(8, 14, 8, 14));

        header.add(titleBox, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);

        String ids = block.getAssignmentIdsText();
        String[][] rows = new String[][]{
            {"Mã phân ca", ids},
            {"Nhân viên", safeDetail(item.getEmployeeName())},
            {"Loại nhân viên", safeDetail(item.getEmployeeType())},
            {"Từ ngày", formatUiDate(block.startDate)},
            {"Đến ngày", formatUiDate(block.endDate)},
            {"Số ngày được gom", String.valueOf(block.getDayCount())},
            {"Ca làm việc", safeDetail(item.getShiftName())},
            {"Thời gian", safeDetail(item.getStartTimeText()) + " - " + safeDetail(item.getEndTimeText())},
            {"Trạng thái", normalizeShiftStatusForView(item.getStatus())},
            {"Ghi chú", safeDetail(item.getNote())}
        };

        JTable detailTable = new JTable(new DefaultTableModel(rows, new String[]{"Thông tin", "Giá trị"}) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        });
        detailTable.setRowHeight(34);
        detailTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        detailTable.setShowHorizontalLines(true);
        detailTable.setShowVerticalLines(false);
        detailTable.setGridColor(new Color(226, 232, 240));
        detailTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        detailTable.getTableHeader().setBackground(new Color(248, 250, 252));
        detailTable.getTableHeader().setForeground(textDark);
        detailTable.getColumnModel().getColumn(0).setPreferredWidth(170);
        detailTable.getColumnModel().getColumn(1).setPreferredWidth(380);

        detailTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                setFont(new Font("Segoe UI", column == 0 ? Font.BOLD : Font.PLAIN, 13));
                setForeground(column == 0 ? new Color(43, 54, 116) : textDark);
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(249, 251, 253));
                return this;
            }
        });

        JScrollPane tableScroll = new JScrollPane(detailTable);
        tableScroll.setBorder(new RoundBorder(new Color(210, 218, 230), 12));
        tableScroll.getViewport().setBackground(Color.WHITE);

        JButton btnEdit = createCustomButton("Đưa lên form", primaryBlue, Color.WHITE, null);
        JButton btnDelete = createCustomButton("Xóa lịch", new Color(185, 28, 28), Color.WHITE, null);
        JButton btnClose = createCustomButton("Đóng", new Color(235, 239, 245), textDark, null);
        btnEdit.setPreferredSize(new Dimension(130, 40));
        btnDelete.setPreferredSize(new Dimension(110, 40));
        btnClose.setPreferredSize(new Dimension(100, 40));

        btnEdit.addActionListener(e -> {
            selectedTimelineBlock = block;
            fillShiftForm(block);
            dialog.dispose();
        });
        btnDelete.addActionListener(e -> {
            selectedTimelineBlock = block;
            fillShiftForm(block);
            dialog.dispose();
            deleteShiftAssignment();
        });
        btnClose.addActionListener(e -> dialog.dispose());

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.add(btnEdit);
        footer.add(btnDelete);
        footer.add(btnClose);

        root.add(header, BorderLayout.NORTH);
        root.add(tableScroll, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setVisible(true);
    }

    private String safeDetail(String value) {
        return value == null || value.trim().isEmpty() ? "—" : value.trim();
    }

    private String normalizeAccountStatus(String status) {
        if (status == null) {
            return "Chưa cấp";
        }

        String value = status.trim();
        if (value.isEmpty() || value.equals("—") || value.equalsIgnoreCase("N/A")
                || value.equalsIgnoreCase("null")) {
            return "Chưa cấp";
        }
        return value;
    }

    private String normalizeShiftStatusForView(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "Đã phân";
        }
        if ("ASSIGNED".equalsIgnoreCase(status)) {
            return "Đã phân";
        }
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return "Hoàn thành";
        }
        if ("CANCELED".equalsIgnoreCase(status)) {
            return "Đã hủy";
        }
        return status;
    }

    private Color getShiftStatusColor(String status) {
        if ("CANCELED".equalsIgnoreCase(status)) {
            return offlineRed;
        }
        if ("COMPLETED".equalsIgnoreCase(status)) {
            return primaryBlue;
        }
        return successGreen;
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

    private String getCurrentUserRole() {
        try {
            model.account.Account currentUser = business.service.SessionManager.getCurrentUser();
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

    private void touchAccountSecurityByEmployeeId(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return;
        }

        String sql = "UPDATE ACCOUNTS SET UPDATED_AT = CURRENT_TIMESTAMP "
                + "WHERE USER_ID = ? AND NVL(IS_DELETED, 0) = 0";

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, employeeId.trim());
            ps.executeUpdate();
        } catch (Exception e) {
            System.err.println("[EmployeeView] touchAccountSecurityByEmployeeId error: " + e.getMessage());
        }
    }

    private String getEmployeeRoleById(String employeeId) {
        if (employeeId == null || employeeId.trim().isEmpty()) {
            return null;
        }

        String sql = "SELECT role_id FROM EMPLOYEES WHERE employee_id = ? AND NVL(is_deleted, 0) = 0";

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, employeeId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role_id");
                }
            }
        } catch (Exception e) {
            System.err.println("[EmployeeView] getEmployeeRoleById error: " + e.getMessage());
        }
        return null;
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
        return phone != null && !phone.isEmpty() && phone.matches("^(0)(3|5|7|8|9)[0-9]{8}$");
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

    private String formatShiftTime(java.util.Date value) {
        return value == null ? "--:--" : shiftTimeFormat.format(value);
    }

    // ==================== UI COMPONENT CREATORS ====================
    private void stylePlainCombo(JComboBox<?> combo) {
        combo.setPreferredSize(new Dimension(180, 38));
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.setBackground(Color.WHITE);
        combo.setBorder(new RoundBorder(borderGray, 8));
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
        txt.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 8),
                new EmptyBorder(5, 10, 5, 10)
        ));
        return txt;
    }

    private JButton createCustomButton(String t, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(t);

        if (icon != null) {
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(18, 18, Image.SCALE_SMOOTH)));
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

    private GridBagConstraints addGbc(GridBagConstraints gbc, int y, int b) {
        gbc.gridy = y;
        gbc.insets = new Insets(0, 0, b, 0);
        return gbc;
    }

    private JTextField createDateField(LocalDate initialDate) {
        CalendarDateField field = new CalendarDateField();
        field.setText(formatUiDate(initialDate));
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setBackground(Color.WHITE);
        field.setForeground(textDark);
        field.setCaretColor(textDark);
        field.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(new Color(185, 196, 213), 8),
                new EmptyBorder(0, 10, 0, 34)
        ));
        return field;
    }

    private void showCalendarPopup(JTextField target) {
        LocalDate base = parseUiDate(target.getText());
        if (base == null) {
            base = LocalDate.now();
        }

        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(new RoundBorder(new Color(185, 196, 213), 10));
        popup.add(createCalendarPanel(target, popup, YearMonth.from(base)));
        popup.show(target, 0, target.getHeight() + 2);
    }

    private JPanel createCalendarPanel(JTextField target, JPopupMenu popup, YearMonth month) {
        JPanel root = new JPanel(new BorderLayout(0, 8));
        root.setBackground(Color.WHITE);
        root.setPreferredSize(new Dimension(270, 255));
        root.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);

        JButton prev = new JButton("‹");
        JButton next = new JButton("›");
        styleCalendarButton(prev);
        styleCalendarButton(next);
        prev.setPreferredSize(new Dimension(34, 30));
        next.setPreferredSize(new Dimension(34, 30));

        JLabel title = new JLabel("Tháng " + month.getMonthValue() + " / " + month.getYear(), SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 13));
        title.setForeground(textDark);

        header.add(prev, BorderLayout.WEST);
        header.add(title, BorderLayout.CENTER);
        header.add(next, BorderLayout.EAST);

        JPanel grid = new JPanel(new GridLayout(7, 7, 4, 4));
        grid.setOpaque(false);

        String[] names = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
        for (String n : names) {
            JLabel l = new JLabel(n, SwingConstants.CENTER);
            l.setFont(new Font("Segoe UI", Font.BOLD, 11));
            l.setForeground(n.equals("CN") ? offlineRed : new Color(84, 101, 130));
            grid.add(l);
        }

        int firstDay = month.atDay(1).getDayOfWeek().getValue();
        int daysInMonth = month.lengthOfMonth();
        int cellsBefore = firstDay - 1;
        int day = 1;

        for (int cell = 0; cell < 42; cell++) {
            if (cell < cellsBefore || day > daysInMonth) {
                JLabel blank = new JLabel("");
                grid.add(blank);
                continue;
            }

            LocalDate date = month.atDay(day++);
            JButton btn = new JButton(String.valueOf(date.getDayOfMonth()));
            styleCalendarDayButton(btn);

            if (date.equals(LocalDate.now())) {
                btn.setForeground(primaryOrange);
                btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
                btn.setBorder(new RoundBorder(primaryOrange, 8));
                btn.setBorderPainted(true);
            }

            btn.addActionListener(e -> {
                target.setText(formatUiDate(date));
                popup.setVisible(false);
            });
            grid.add(btn);
        }

        prev.addActionListener(e -> {
            popup.removeAll();
            popup.add(createCalendarPanel(target, popup, month.minusMonths(1)));
            popup.pack();
            popup.revalidate();
            popup.repaint();
        });

        next.addActionListener(e -> {
            popup.removeAll();
            popup.add(createCalendarPanel(target, popup, month.plusMonths(1)));
            popup.pack();
            popup.revalidate();
            popup.repaint();
        });

        root.add(header, BorderLayout.NORTH);
        root.add(grid, BorderLayout.CENTER);
        return root;
    }

    private void styleCalendarDayButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBackground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(textDark);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(0, 0, 0, 0));
    }

    private void styleCalendarButton(JButton btn) {
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 17));
        btn.setForeground(textDark);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setMargin(new Insets(0, 0, 0, 0));
    }

    // ==================== INNER CLASSES ====================
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

    class UnifiedEmployeeRenderer extends DefaultTableCellRenderer {

        private final Color currentUserBg = new Color(225, 245, 233);
        private final Color managerBg = new Color(230, 245, 233);
        private final Color zebraBg = new Color(249, 251, 253);
        private final Color normalBg = Color.WHITE;
        private final Color selectedBg = new Color(212, 230, 241);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            int modelRow = table.convertRowIndexToModel(row);
            int modelColumn = table.convertColumnIndexToModel(column);

            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setHorizontalAlignment(JLabel.CENTER);

            String roleDisplay = String.valueOf(table.getModel().getValueAt(modelRow, COL_ROLE));
            String employeeId = String.valueOf(table.getModel().getValueAt(modelRow, COL_RAW_ID));

            String currentUserId = "";
            try {
                model.account.Account currentUser = business.service.SessionManager.getCurrentUser();
                if (currentUser != null && currentUser.getUserId() != null) {
                    currentUserId = currentUser.getUserId();
                }
            } catch (Exception ignored) {
            }

            boolean isCurrentUserRow = currentUserId != null && !currentUserId.trim().isEmpty()
                    && currentUserId.equalsIgnoreCase(employeeId);
            boolean isManagerRow = roleDisplay.contains("R_STORE_MNG") || roleDisplay.contains("MNG")
                    || roleDisplay.toLowerCase().contains("quản lý");

            if (isSelected) {
                setBackground(selectedBg);
                setForeground(textDark);
            } else if (isCurrentUserRow && isManagerRow) {
                setBackground(currentUserBg);
                setForeground(new Color(0, 120, 70));
            } else if (isManagerRow) {
                setBackground(managerBg);
                setForeground(new Color(25, 135, 84));
            } else {
                setBackground(row % 2 == 0 ? normalBg : zebraBg);
                setForeground(Color.BLACK);
            }

            if (modelColumn == COL_ONLINE_STATUS) {
                String status = value == null ? "—" : value.toString().trim();
                setFont(new Font("Segoe UI", Font.BOLD, 12));
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

    private class CalendarDateField extends JTextField {

        private final int iconWidth = 30;

        CalendarDateField() {
            setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getX() >= getWidth() - iconWidth) {
                        showCalendarPopup(CalendarDateField.this);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = getWidth() - iconWidth;
            int y = 6;
            int w = 18;
            int h = 18;
            int cx = x + 5;

            g2.setColor(new Color(245, 247, 250));
            g2.fillRoundRect(x, 1, iconWidth - 2, getHeight() - 3, 8, 8);
            g2.setColor(new Color(178, 188, 204));
            g2.drawLine(x, 7, x, getHeight() - 7);

            g2.setColor(new Color(95, 111, 135));
            g2.drawRoundRect(cx, y + 2, w, h - 2, 4, 4);
            g2.fillRect(cx, y + 5, w + 1, 4);
            g2.setColor(Color.WHITE);
            g2.drawLine(cx + 4, y + 9, cx + w - 4, y + 9);
            g2.setColor(new Color(95, 111, 135));
            g2.fillRect(cx + 4, y, 3, 5);
            g2.fillRect(cx + w - 7, y, 3, 5);
            g2.dispose();
        }
    }

    private static class TimelineCell {

        private final String employeeId;
        private final LocalDate date;
        private final int hour;

        private TimelineCell(String employeeId, LocalDate date, int hour) {
            this.employeeId = employeeId;
            this.date = date;
            this.hour = hour;
        }
    }

    private class TimelineBlock {

        private final List<EmployeeShift> sourceAssignments = new ArrayList<>();
        private EmployeeShift primary;
        private String employeeId;
        private String shiftId;
        private String status;
        private String startTimeText;
        private String endTimeText;
        private LocalDate startDate;
        private LocalDate endDate;
        private int startMinutes;
        private int endMinutes;
        private boolean fullTime;

        TimelineBlock(EmployeeShift item) {
            this.primary = item;
            this.employeeId = item.getEmployeeId();
            this.shiftId = item.getShiftId();
            this.status = item.getStatus() == null ? "ASSIGNED" : item.getStatus();
            this.startTimeText = item.getStartTimeText();
            this.endTimeText = item.getEndTimeText();
            this.startDate = item.getWorkDate().toLocalDate();
            this.endDate = this.startDate;
            this.startMinutes = parseMinutesForBlock(item.getStartTimeText(), 0);
            this.endMinutes = parseMinutesForBlock(item.getEndTimeText(), 24 * 60);
            this.fullTime = isFullTimeShiftForMerge(item);
            this.sourceAssignments.add(item);
        }

        boolean canMerge(EmployeeShift next) {
            if (next == null || next.getWorkDate() == null) {
                return false;
            }
            String nextStatus = next.getStatus() == null ? "ASSIGNED" : next.getStatus();
            return safeEquals(employeeId, next.getEmployeeId())
                    && safeEquals(shiftId, next.getShiftId())
                    && safeEquals(status, nextStatus)
                    && safeEquals(startTimeText, next.getStartTimeText())
                    && safeEquals(endTimeText, next.getEndTimeText())
                    && next.getWorkDate().toLocalDate().equals(endDate.plusDays(1));
        }

        void merge(EmployeeShift next) {
            sourceAssignments.add(next);
            endDate = next.getWorkDate().toLocalDate();
        }

        EmployeeShift getPrimary() {
            return primary;
        }

        int getDayCount() {
            return sourceAssignments.size();
        }

        List<String> getAssignmentIds() {
            List<String> ids = new ArrayList<>();
            for (EmployeeShift item : sourceAssignments) {
                if (item.getAssignmentId() != null && !item.getAssignmentId().isBlank()) {
                    ids.add(item.getAssignmentId());
                }
            }
            return ids;
        }

        String getAssignmentIdsText() {
            List<String> ids = getAssignmentIds();
            if (ids.isEmpty()) {
                return "—";
            }
            if (ids.size() == 1) {
                return ids.get(0);
            }
            String joined = String.join(", ", ids);
            if (joined.length() > 95) {
                return joined.substring(0, 95) + "...";
            }
            return joined;
        }
    }

    private int parseMinutesForBlock(String time, int fallback) {
        if (time == null || time.trim().isEmpty()) {
            return fallback;
        }
        try {
            String[] parts = time.trim().split(":");
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return Math.max(0, Math.min(24 * 60, h * 60 + m));
        } catch (Exception ex) {
            return fallback;
        }
    }

    private boolean isFullTimeShiftForMerge(EmployeeShift item) {
        if (item == null) {
            return false;
        }
        String id = item.getShiftId() == null ? "" : item.getShiftId().toUpperCase();
        String name = item.getShiftName() == null ? "" : item.getShiftName().toLowerCase();
        return id.contains("FULL") || name.contains("full") || name.contains("toàn thời gian");
    }

    private boolean safeEquals(String a, String b) {
        if (a == null) {
            return b == null;
        }
        return a.equalsIgnoreCase(b == null ? "" : b);
    }

    class ShiftTimelinePanel extends JPanel {

        private final int nameColWidth = 230;
        private final int dayWidth = 250;
        private final int headerHeight = 70;
        private final int rowHeight = 68;
        private final int blockHeight = 42;

        private List<EmployeeShift> assignments = new ArrayList<>();
        private List<LocalDate> visibleDates = new ArrayList<>();
        private List<String> visibleEmployeeKeys = new ArrayList<>();
        private Map<String, String> employeeDisplayNames = new HashMap<>();
        private Map<String, String> employeeTypes = new HashMap<>();
        private Map<Rectangle, TimelineBlock> hitBoxes = new HashMap<>();
        private List<TimelineBlock> timelineBlocks = new ArrayList<>();
        private TimelineBlock selectedBlock;
        private EmployeeShift selectedAssignment;
        private java.util.function.Consumer<TimelineBlock> selectionCallback;
        private Point dragStartPoint;
        private Point dragStartViewPosition;
        private boolean draggingTimeline = false;
        private LocalDate forcedMinDate;
        private LocalDate forcedMaxDate;

        ShiftTimelinePanel() {
            setBackground(Color.WHITE);
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.PLAIN, 12));
            setAssignments(new ArrayList<>());

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    dragStartPoint = e.getPoint();
                    draggingTimeline = false;
                    JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, ShiftTimelinePanel.this);
                    if (viewport != null) {
                        dragStartViewPosition = viewport.getViewPosition();
                    }
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (draggingTimeline) {
                        return;
                    }

                    for (Map.Entry<Rectangle, TimelineBlock> entry : hitBoxes.entrySet()) {
                        if (entry.getKey().contains(e.getPoint())) {
                            selectedBlock = entry.getValue();
                            selectedAssignment = selectedBlock == null ? null : selectedBlock.getPrimary();
                            if (selectionCallback != null) {
                                selectionCallback.accept(selectedBlock);
                            }
                            repaint();
                            return;
                        }
                    }

                    TimelineCell cell = locateEmptyTimelineCell(e.getPoint());
                    if (cell != null) {
                        showNewShiftDialogFromTimelineCell(cell.employeeId, cell.date, cell.hour);
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    JViewport viewport = (JViewport) SwingUtilities.getAncestorOfClass(JViewport.class, ShiftTimelinePanel.this);
                    if (viewport == null || dragStartPoint == null || dragStartViewPosition == null) {
                        return;
                    }

                    int dx = e.getX() - dragStartPoint.x;
                    int dy = e.getY() - dragStartPoint.y;
                    if (Math.abs(dx) > 4 || Math.abs(dy) > 4) {
                        draggingTimeline = true;
                    }

                    Point newPos = new Point(
                            dragStartViewPosition.x - dx,
                            dragStartViewPosition.y - dy
                    );

                    newPos.x = Math.max(0, Math.min(newPos.x, getWidth() - viewport.getWidth()));
                    newPos.y = Math.max(0, Math.min(newPos.y, getHeight() - viewport.getHeight()));
                    viewport.setViewPosition(newPos);
                }
            });
        }

        private TimelineCell locateEmptyTimelineCell(Point point) {
            if (point == null || point.y < headerHeight || point.x < nameColWidth) {
                return null;
            }

            int row = (point.y - headerHeight) / rowHeight;
            int dateIndex = (point.x - nameColWidth) / dayWidth;
            if (row < 0 || row >= visibleEmployeeKeys.size() || dateIndex < 0 || dateIndex >= visibleDates.size()) {
                return null;
            }

            int localX = point.x - (nameColWidth + dateIndex * dayWidth);
            int hour = (int) Math.floor((localX / (double) dayWidth) * 24.0);
            hour = Math.max(0, Math.min(23, hour));
            return new TimelineCell(visibleEmployeeKeys.get(row), visibleDates.get(dateIndex), hour);
        }

        void setSelectionCallback(java.util.function.Consumer<TimelineBlock> selectionCallback) {
            this.selectionCallback = selectionCallback;
        }

        void setDisplayRange(LocalDate fromDate, LocalDate toDate) {
            if (fromDate == null && toDate == null) {
                forcedMinDate = null;
                forcedMaxDate = null;
                return;
            }

            if (fromDate == null) {
                fromDate = toDate;
            }
            if (toDate == null) {
                toDate = fromDate;
            }

            if (fromDate.isAfter(toDate)) {
                LocalDate tmp = fromDate;
                fromDate = toDate;
                toDate = tmp;
            }

            forcedMinDate = fromDate;
            forcedMaxDate = toDate;
        }

        void clearSelectedAssignment() {
            selectedAssignment = null;
            selectedBlock = null;
            repaint();
        }

        void setSelectedBlock(TimelineBlock block) {
            selectedBlock = block;
            selectedAssignment = block == null ? null : block.getPrimary();
            repaint();
        }

        void setAssignments(List<EmployeeShift> data) {
            assignments = data == null ? new ArrayList<>() : new ArrayList<>(data);
            buildTimelineIndex();
            timelineBlocks = buildTimelineBlocks(assignments);
            revalidate();
            repaint();
        }

        private void buildTimelineIndex() {
            visibleDates.clear();
            visibleEmployeeKeys.clear();
            employeeDisplayNames.clear();
            employeeTypes.clear();

            LocalDate minDate = null;
            LocalDate maxDate = null;

            for (EmployeeShift item : assignments) {
                if (item.getWorkDate() == null) {
                    continue;
                }

                LocalDate d = item.getWorkDate().toLocalDate();
                if (minDate == null || d.isBefore(minDate)) {
                    minDate = d;
                }
                if (maxDate == null || d.isAfter(maxDate)) {
                    maxDate = d;
                }

                String employeeId = item.getEmployeeId() == null ? "UNKNOWN" : item.getEmployeeId();
                if (!visibleEmployeeKeys.contains(employeeId)) {
                    visibleEmployeeKeys.add(employeeId);
                    employeeDisplayNames.put(employeeId, item.getEmployeeName() == null ? employeeId : item.getEmployeeName());
                    employeeTypes.put(employeeId, item.getEmployeeType() == null ? "" : item.getEmployeeType());
                }
            }

            for (Employee emp : assignableEmployees) {
                if (emp == null || emp.getEmployeeId() == null || emp.getEmployeeId().trim().isEmpty()) {
                    continue;
                }
                if (!isShiftAssignableRole(emp.getRoleId())) {
                    continue;
                }
                String employeeId = emp.getEmployeeId();
                if (!visibleEmployeeKeys.contains(employeeId)) {
                    visibleEmployeeKeys.add(employeeId);
                    employeeDisplayNames.put(employeeId, emp.getEmployeeName() == null ? employeeId : emp.getEmployeeName());
                    employeeTypes.put(employeeId, getEmployeeTypeLabel(emp.getRoleId()));
                }
            }

            if (forcedMinDate != null || forcedMaxDate != null) {
                LocalDate forcedFrom = forcedMinDate != null ? forcedMinDate : forcedMaxDate;
                LocalDate forcedTo = forcedMaxDate != null ? forcedMaxDate : forcedFrom;

                if (minDate == null || forcedFrom.isBefore(minDate)) {
                    minDate = forcedFrom;
                }
                if (maxDate == null || forcedTo.isAfter(maxDate)) {
                    maxDate = forcedTo;
                }
            }

            if (minDate == null || maxDate == null) {
                LocalDate today = LocalDate.now();
                minDate = today;
                maxDate = today;
            }

            for (LocalDate d = minDate; !d.isAfter(maxDate); d = d.plusDays(1)) {
                visibleDates.add(d);
            }

            int width = nameColWidth + Math.max(visibleDates.size(), 1) * dayWidth + 60;
            int height = headerHeight + Math.max(visibleEmployeeKeys.size(), 4) * rowHeight + 80;
            setPreferredSize(new Dimension(width, height));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            hitBoxes.clear();

            paintHeader(g2);
            paintRows(g2);
            paintAssignments(g2);
            paintEmptyState(g2);

            g2.dispose();
        }

        private void paintHeader(Graphics2D g2) {
            g2.setColor(new Color(248, 250, 252));
            g2.fillRoundRect(0, 0, getWidth() - 1, headerHeight, 18, 18);
            g2.setStroke(new BasicStroke(2.4f));
            g2.setColor(new Color(111, 127, 150));
            g2.drawLine(0, headerHeight - 1, getWidth(), headerHeight - 1);

            g2.setColor(textDark);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.drawString("Nhân viên", 22, 30);
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(new Color(107, 119, 140));
            g2.drawString("Lịch làm việc theo ngày / giờ", 22, 54);

            for (int i = 0; i < visibleDates.size(); i++) {
                int x = nameColWidth + i * dayWidth;
                LocalDate d = visibleDates.get(i);

                g2.setStroke(new BasicStroke(2.1f));
                g2.setColor(new Color(111, 127, 150));
                g2.drawLine(x, 0, x, headerHeight + Math.max(visibleEmployeeKeys.size(), 4) * rowHeight);

                g2.setColor(textDark);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                g2.drawString(d.toString(), x + 12, 28);

                g2.setColor(new Color(107, 119, 140));
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.drawString("00h", x + 12, 54);
                g2.drawString("08h", x + dayWidth / 3 - 8, 54);
                g2.drawString("16h", x + dayWidth * 2 / 3 - 8, 54);
                g2.drawString("24h", x + dayWidth - 38, 54);
            }
        }

        private void paintRows(Graphics2D g2) {
            int rowCount = Math.max(visibleEmployeeKeys.size(), 4);
            for (int row = 0; row < rowCount; row++) {
                int y = headerHeight + row * rowHeight;

                g2.setColor(row % 2 == 0 ? Color.WHITE : new Color(249, 251, 253));
                g2.fillRect(0, y, getWidth(), rowHeight);

                g2.setStroke(new BasicStroke(2.1f));
                g2.setColor(new Color(111, 127, 150));
                g2.drawLine(0, y + rowHeight - 1, getWidth(), y + rowHeight - 1);

                if (row < visibleEmployeeKeys.size()) {
                    String employeeId = visibleEmployeeKeys.get(row);
                    String name = employeeDisplayNames.getOrDefault(employeeId, employeeId);
                    String type = employeeTypes.getOrDefault(employeeId, "");

                    g2.setColor(textDark);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    g2.drawString(name, 20, y + 32);

                    g2.setColor(new Color(107, 119, 140));
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                    g2.drawString(type, 20, y + 52);
                }

                for (int i = 0; i < visibleDates.size(); i++) {
                    int x = nameColWidth + i * dayWidth;
                    g2.setStroke(new BasicStroke(1.7f));
                    g2.setColor(new Color(136, 151, 174));
                    g2.drawLine(x + dayWidth / 3, y + 10, x + dayWidth / 3, y + rowHeight - 10);
                    g2.drawLine(x + dayWidth * 2 / 3, y + 10, x + dayWidth * 2 / 3, y + rowHeight - 10);
                }
            }
        }

        private void paintAssignments(Graphics2D g2) {
            for (TimelineBlock block : timelineBlocks) {
                EmployeeShift item = block.getPrimary();
                if (item == null || block.startDate == null || block.endDate == null) {
                    continue;
                }

                int row = visibleEmployeeKeys.indexOf(item.getEmployeeId());
                int startDateIndex = visibleDates.indexOf(block.startDate);
                int endDateIndex = visibleDates.indexOf(block.endDate);
                if (row < 0 || startDateIndex < 0) {
                    continue;
                }
                if (endDateIndex < 0) {
                    endDateIndex = startDateIndex;
                }

                int startDayX = nameColWidth + startDateIndex * dayWidth;
                int endDayX = nameColWidth + endDateIndex * dayWidth;
                int rowY = headerHeight + row * rowHeight;

                boolean overnight = block.startMinutes > block.endMinutes;
                boolean fullTime = block.fullTime;

                int blockX;
                int blockW;
                if (fullTime && block.getDayCount() > 1) {
                    blockX = startDayX + 8;
                    blockW = Math.max(44, (endDayX + dayWidth - 8) - blockX);
                } else if (fullTime) {
                    blockX = startDayX + 8;
                    blockW = dayWidth - 16;
                } else if (overnight) {
                    blockX = startDayX + minuteToX(block.startMinutes);
                    blockW = Math.max(34, (dayWidth - 12) - minuteToX(block.startMinutes));
                } else {
                    blockX = startDayX + minuteToX(block.startMinutes);
                    blockW = Math.max(34, minuteToX(block.endMinutes) - minuteToX(block.startMinutes));
                }

                int blockY = rowY + (rowHeight - blockHeight) / 2;
                Rectangle rect = new Rectangle(blockX, blockY, blockW, blockHeight);
                hitBoxes.put(rect, block);

                Color color = getShiftColor(item);
                boolean selected = selectedBlock != null && selectedBlock == block;
                if (!selected && selectedAssignment != null && selectedAssignment.getAssignmentId() != null) {
                    for (String id : block.getAssignmentIds()) {
                        if (selectedAssignment.getAssignmentId().equals(id)) {
                            selected = true;
                            break;
                        }
                    }
                }

                if (selected) {
                    g2.setColor(new Color(255, 193, 7));
                    g2.fillRoundRect(rect.x - 4, rect.y - 4, rect.width + 8, rect.height + 8, 18, 18);
                }

                g2.setColor(new Color(0, 0, 0, 34));
                g2.fillRoundRect(rect.x + 3, rect.y + 4, rect.width, rect.height, 14, 14);
                g2.setColor(color);
                g2.fillRoundRect(rect.x, rect.y, rect.width, rect.height, 14, 14);
                g2.setStroke(new BasicStroke(3.2f));
                g2.setColor(new Color(28, 39, 56, 125));
                g2.drawRoundRect(rect.x, rect.y, rect.width, rect.height, 14, 14);
                g2.setStroke(new BasicStroke(1.2f));
                g2.setColor(new Color(255, 255, 255, 165));
                g2.drawRoundRect(rect.x + 2, rect.y + 2, rect.width - 4, rect.height - 4, 12, 12);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                String label = item.getShiftName() == null ? item.getShiftId() : item.getShiftName();
                String time = safeTime(item.getStartTimeText()) + " - " + safeTime(item.getEndTimeText());
                if (block.getDayCount() > 1) {
                    time = formatUiDate(block.startDate) + " - " + formatUiDate(block.endDate) + " • " + time;
                }
                drawClippedString(g2, label, rect.x + 10, rect.y + 18, rect.width - 18);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                drawClippedString(g2, time, rect.x + 10, rect.y + 33, rect.width - 18);

                if (overnight && !fullTime) {
                    g2.setColor(new Color(107, 119, 140));
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                    int noteX = Math.min(startDayX + dayWidth - 70, rect.x + rect.width + 4);
                    g2.drawString("qua ngày", noteX, rect.y + 24);
                }
            }
        }

        private List<TimelineBlock> buildTimelineBlocks(List<EmployeeShift> source) {
            List<EmployeeShift> sorted = new ArrayList<>();
            if (source != null) {
                for (EmployeeShift item : source) {
                    if (item != null && item.getWorkDate() != null) {
                        sorted.add(item);
                    }
                }
            }

            sorted.sort((a, b) -> {
                int c = String.valueOf(a.getEmployeeId()).compareToIgnoreCase(String.valueOf(b.getEmployeeId()));
                if (c != 0) {
                    return c;
                }
                c = String.valueOf(a.getShiftId()).compareToIgnoreCase(String.valueOf(b.getShiftId()));
                if (c != 0) {
                    return c;
                }
                c = String.valueOf(a.getStatus()).compareToIgnoreCase(String.valueOf(b.getStatus()));
                if (c != 0) {
                    return c;
                }
                c = String.valueOf(a.getStartTimeText()).compareToIgnoreCase(String.valueOf(b.getStartTimeText()));
                if (c != 0) {
                    return c;
                }
                c = String.valueOf(a.getEndTimeText()).compareToIgnoreCase(String.valueOf(b.getEndTimeText()));
                if (c != 0) {
                    return c;
                }
                return a.getWorkDate().compareTo(b.getWorkDate());
            });

            List<TimelineBlock> blocks = new ArrayList<>();
            TimelineBlock current = null;
            for (EmployeeShift item : sorted) {
                if (current != null && current.canMerge(item)) {
                    current.merge(item);
                } else {
                    current = new TimelineBlock(item);
                    blocks.add(current);
                }
            }
            return blocks;
        }

        private void paintEmptyState(Graphics2D g2) {
            if (!assignments.isEmpty()) {
                return;
            }

            int cx = Math.max(nameColWidth + 180, getWidth() / 2);
            int cy = Math.max(headerHeight + 120, getHeight() / 2);

            g2.setColor(textDark);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 18));
            String line1 = "Chưa có phân ca trong khoảng ngày này";
            g2.drawString(line1, cx - g2.getFontMetrics().stringWidth(line1) / 2, cy);

            g2.setColor(new Color(107, 119, 140));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            String line2 = "Bấm vào ô trống trên timeline để thêm lịch trực tiếp.";
            g2.drawString(line2, cx - g2.getFontMetrics().stringWidth(line2) / 2, cy + 26);
        }

        private int minuteToX(int minutes) {
            int clamped = Math.max(0, Math.min(24 * 60, minutes));
            return 8 + (int) Math.round((dayWidth - 16) * (clamped / (double) (24 * 60)));
        }

        private int parseMinutes(String time, int fallback) {
            if (time == null || time.trim().isEmpty()) {
                return fallback;
            }

            try {
                String[] parts = time.trim().split(":");
                int h = Integer.parseInt(parts[0]);
                int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                if (h == 24) {
                    return 24 * 60;
                }
                return Math.max(0, Math.min(24 * 60, h * 60 + m));
            } catch (Exception ignored) {
                return fallback;
            }
        }

        private boolean isFullTimeShift(EmployeeShift item) {
            String id = item.getShiftId() == null ? "" : item.getShiftId().toUpperCase();
            String name = item.getShiftName() == null ? "" : item.getShiftName().toLowerCase();
            return id.contains("FULL") || name.contains("full");
        }

        private Color getShiftColor(EmployeeShift item) {
            String status = item.getStatus() == null ? "" : item.getStatus();
            if ("CANCELED".equalsIgnoreCase(status)) {
                return offlineRed;
            }

            String id = item.getShiftId() == null ? "" : item.getShiftId().toUpperCase();
            String name = item.getShiftName() == null ? "" : item.getShiftName().toLowerCase();

            if (id.contains("FULL") || name.contains("full")) {
                return new Color(72, 99, 112);
            }
            if (id.contains("MORNING") || name.contains("sáng") || name.contains("sang")) {
                return new Color(22, 163, 74);
            }
            if (id.contains("AFTERNOON") || name.contains("chiều") || name.contains("chieu")) {
                return new Color(37, 99, 235);
            }
            if (id.contains("NIGHT") || name.contains("tối") || name.contains("toi")) {
                return new Color(124, 58, 237);
            }
            return primaryOrange;
        }

        private String safeTime(String value) {
            return value == null || value.isBlank() ? "--:--" : value;
        }

        private void drawClippedString(Graphics2D g2, String text, int x, int y, int maxWidth) {
            if (text == null) {
                text = "";
            }
            FontMetrics fm = g2.getFontMetrics();
            String value = text;
            while (fm.stringWidth(value) > maxWidth && value.length() > 3) {
                value = value.substring(0, value.length() - 4) + "...";
            }
            g2.drawString(value, x, y);
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
