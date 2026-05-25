package view;

import business.service.RolePermissionService;
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
    private JTable tblShiftAssignments;
    private DefaultTableModel shiftTableModel;
    private JLabel lblSelectedShiftEmployeeAvatar;
    private JLabel lblSelectedShiftEmployeeStore;
    private JLabel lblSelectedShiftEmployeeShift;
    private JLabel lblShiftSummary;
    private EmployeeShift selectedTimelineBlock;

    private static final int COL_EMPLOYEE_ID = 0;
    private static final int COL_EMPLOYEE_NAME = 1;
    private static final int COL_STORE = 2;
    private static final int COL_PHONE = 3;
    private static final int COL_EMAIL = 4;
    private static final int COL_ACCOUNT_STATUS = 5;
    private static final int COL_ONLINE_STATUS = 6;
    private static final int COL_ROLE = 7;
    private static final int COL_GENDER = 8;

    private final EmployeeSql employeeSql = EmployeeSql.getInstance();
    private final EmployeeShiftSql employeeShiftSql = new EmployeeShiftSql();
    private final ShiftSql shiftSql = ShiftSql.getInstance();
    private String currentSelectedRawId;

    public EmployeeView() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents();
        loadStores();
        loadEmployees();
        applyPermissionMatrixToButtons();
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
    }

    private boolean denyIfCannotAdd(String actionName) {
        if (RolePermissionService.canAdd()) {
            return false;
        }
        JOptionPane.showMessageDialog(this,
                "Bạn không có quyền Thêm cho thao tác: " + actionName,
                "Từ chối quyền",
                JOptionPane.WARNING_MESSAGE);
        return true;
    }

    // FILE TRUNCATED INTENTIONALLY BY PATCH SAFETY
}
