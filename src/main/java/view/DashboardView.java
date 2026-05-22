package view;

import business.service.AuthorizationService;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.security.SecurityGuard;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import view.components.NotificationBell;
import view.components.Sidebar;
import view.components.TongQuanPanel;

public class DashboardView extends JFrame {

    private static final java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger(DashboardView.class.getName());

    private Timer sessionTimer;
    private boolean isLoggingOut = false;
    private final long dashboardCreatedAt = System.currentTimeMillis();

    private JPanel mainContentPanel;

    private final Color BACKGROUND_COLOR = new Color(245, 245, 247);

    private String currentMenu = "Tổng quan";

    public DashboardView() {
        setupUI();
        startSessionCheck();

        SecurityGuard.attach(mainContentPanel);

        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            SwingUtilities.invokeLater(() -> {
                if (e.getType() == AppEventType.ORDERS && "Tổng quan".equals(currentMenu)) {
                    refreshTongQuanIfVisible();
                    return;
                }

                if ("Báo cáo & Thống kê".equals(currentMenu)
                        && AuthorizationService.canAccessReports()) {
                    showPanel(new StatisticView());
                }
            });
        });
    }

    private void setupUI() {
        model.account.Account u = business.service.LoginService.getCurrentUser();

        String portalTitle = AuthorizationService.currentPortalTitle();

        if (u != null && u.getUsername() != null) {
            this.setTitle(portalTitle + " | Chào, " + u.getUsername().trim());
        } else {
            this.setTitle(portalTitle);
        }

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleCloseApp();
            }
        });

        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        getContentPane().setLayout(new BorderLayout());

        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(BACKGROUND_COLOR);
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        String roleForSidebar = business.service.SessionManager.getCurrentRole();

        if (roleForSidebar == null || roleForSidebar.trim().isEmpty()) {
            try {
                roleForSidebar = common.auth.UserSession.getInstance().getUserRole();
            } catch (Exception ignored) {
                roleForSidebar = "";
            }
        }

        Sidebar sidebar = new Sidebar(roleForSidebar);
        sidebar.setMenuClickListener(title -> {
            currentMenu = title;
            handleMenuClick(title);
        });

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        topBar.setBackground(Color.WHITE);
        topBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)));

        NotificationBell bell;

        if (AuthorizationService.isWarehouseStaff()) {
            bell = new NotificationBell(NotificationBell.Audience.WAREHOUSE);
        } else if (AuthorizationService.isStoreManager()) {
            bell = new NotificationBell(NotificationBell.Audience.MANAGER);
        } else {
            bell = new NotificationBell(NotificationBell.Audience.ALL);
        }

        topBar.add(bell);

        getContentPane().add(topBar, BorderLayout.NORTH);
        getContentPane().add(sidebar, BorderLayout.WEST);
        getContentPane().add(mainContentPanel, BorderLayout.CENTER);

        showPanel(new TongQuanPanel());
    }

    private void handleMenuClick(String title) {
        switch (title) {
            case "Tổng quan":
                showPanel(new TongQuanPanel());
                break;

            case "Bán hàng":
                if (!AuthorizationService.canAccessSales()) {
                    showAccessDenied();
                    return;
                }
                showPanel(new SellPanel());
                break;

            case "Quản lý sản phẩm":
                if (!AuthorizationService.canAccessProductsAndInventory()) {
                    showAccessDenied();
                    return;
                }
                showPanel(new ProductView());
                break;

            case "Quản lý tồn kho":
                if (!AuthorizationService.canAccessProductsAndInventory()) {
                    showAccessDenied();
                    return;
                }
                showPanel(new InventoryView());
                break;

            case "Quản lý nhà cung cấp":
                if (!AuthorizationService.canAccessSupplierAndCategory()) {
                    showAccessDenied();
                    return;
                }
                showPanel(new SupplierManagementView());
                break;

            case "Danh mục & Thuế VAT":
                if (!AuthorizationService.canAccessSupplierAndCategory()) {
                    showAccessDenied();
                    return;
                }
                showPanel(new CategoryTaxView());
                break;

            case "Quản lý nhân viên":
                if (!AuthorizationService.canAccessEmployees()) {
                    showAccessDenied();
                    return;
                }
                showPanel(new EmployeeView());
                break;

            case "Khách hàng":
                if (!AuthorizationService.canAccessCustomers()) {
                    showAccessDenied();
                    return;
                }
                showPanel(new CustomerView());
                break;

            case "Hóa đơn":
                if (!AuthorizationService.canAccessOrders()) {
                    showAccessDenied();
                    return;
                }
                showPanel(new OrderView());
                break;

            case "Báo cáo & Thống kê":
                if (!AuthorizationService.canAccessReports()) {
                    showAccessDenied();
                    return;
                }
                showPanel(new StatisticView());
                break;

            case "Cài đặt":
                showPanel(new view.components.UnifiedSettingsPanel());
                break;

            case "Đăng xuất":
                handleLogout();
                break;

            default:
                JOptionPane.showMessageDialog(
                        this,
                        "Chức năng chưa được hỗ trợ!",
                        "Thông báo",
                        JOptionPane.INFORMATION_MESSAGE
                );
                break;
        }
    }

    private void showAccessDenied() {
        JOptionPane.showMessageDialog(
                this,
                "Bạn không có quyền truy cập chức năng này!",
                "Từ chối",
                JOptionPane.WARNING_MESSAGE
        );
    }

    private void refreshTongQuanIfVisible() {
        System.out.println("Cập nhật Real-time: Refresh Dashboard...");

        if (mainContentPanel == null || mainContentPanel.getComponentCount() <= 0) {
            return;
        }

        Component c = mainContentPanel.getComponent(0);

        if (!(c instanceof JScrollPane scroll)) {
            return;
        }

        Component innerPanel = scroll.getViewport().getView();

        if (innerPanel instanceof TongQuanPanel tongQuanPanel) {
            tongQuanPanel.loadRealData();
        }
    }

    public void showPanel(JPanel childPanel) {
        mainContentPanel.removeAll();

        childPanel.setMinimumSize(new Dimension(900, 600));
        childPanel.setBackground(BACKGROUND_COLOR);

        JScrollPane scrollPane = new JScrollPane(childPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().putClientProperty("ScrollBar.showButtons", false);

        mainContentPanel.add(scrollPane, BorderLayout.CENTER);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn đăng xuất?",
                "Xác nhận đăng xuất",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            model.account.Account currentUser
                    = business.service.SessionManager.getCurrentUser();

            String sessionId
                    = business.service.SessionManager.getCurrentSessionId();

            if (currentUser != null && currentUser.getAccountId() != null) {
                business.service.HeartbeatService.stop();

                business.service.AccountService.onLogoutOrCloseApp(
                        currentUser.getAccountId(),
                        sessionId
                );
            }

            business.service.SessionManager.clear();

            try {
                common.auth.UserSession.getInstance().clear();
            } catch (Exception ignored) {
            }

            SecurityGuard.setProcessingLogout(false);

        } catch (Exception ex) {
            System.err.println("[Logout] Lỗi logout: " + ex.getMessage());
        }

        dispose();

        LoginView login = new LoginView();
        login.setLocationRelativeTo(null);
        login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        login.setVisible(true);
    }

    private void startSessionCheck() {
        sessionTimer = new Timer(5000, e -> {
            if (SecurityGuard.isProcessingLogout() || isLoggingOut) {
                ((Timer) e.getSource()).stop();
                return;
            }

            if (System.currentTimeMillis() - dashboardCreatedAt < 10000) {
                return;
            }

            new Thread(() -> {
                if (SecurityGuard.isProcessingLogout() || isLoggingOut) {
                    return;
                }

                model.account.Account currentUser
                        = business.service.SessionManager.getCurrentUser();

                String sessionId
                        = business.service.SessionManager.getCurrentSessionId();

                if (currentUser == null
                        || currentUser.getAccountId() == null
                        || sessionId == null
                        || sessionId.trim().isEmpty()) {
                    return;
                }

                boolean forceLogout = false;

                try {
                    if (!isMultiSessionRole(currentUser)) {
                        boolean currentSessionValid
                                = business.sql.rbac.AccountSql.getInstance()
                                        .isCurrentSessionValid(
                                                currentUser.getAccountId(),
                                                sessionId
                                        );

                        if (!currentSessionValid) {
                            forceLogout = true;
                        }
                    }

                    String[] latestData
                            = business.sql.rbac.AccountSql.getInstance()
                                    .getAccountDetails(currentUser.getAccountId());

                    if (latestData != null) {
                        String dbRoleId = latestData[4];

                        String currentRole = currentUser.getRoleId() != null
                                ? currentUser.getRoleId()
                                : currentUser.getRoleValue();

                        if (dbRoleId != null
                                && currentRole != null
                                && !dbRoleId.equalsIgnoreCase(currentRole)) {
                            forceLogout = true;
                        }
                    }

                } catch (Exception ex) {
                    System.err.println("[SessionCheck] Bỏ qua lỗi kiểm tra session: " + ex.getMessage());
                    return;
                }

                if (forceLogout) {
                    SwingUtilities.invokeLater(this::forceLogoutToLogin);
                }

            }, "dashboard-session-check-thread").start();
        });

        sessionTimer.start();
    }

    private boolean isMultiSessionRole(model.account.Account acc) {
        if (acc == null) {
            return false;
        }

        String role = "";

        if (acc.getRoleId() != null && !acc.getRoleId().trim().isEmpty()) {
            role = acc.getRoleId();
        } else if (acc.getRoleValue() != null && !acc.getRoleValue().trim().isEmpty()) {
            role = acc.getRoleValue();
        } else if (acc.getRole() != null && !acc.getRole().trim().isEmpty()) {
            role = acc.getRole();
        }

        return "R_ADMIN_ALL".equalsIgnoreCase(role)
                || "R_STORE_MNG".equalsIgnoreCase(role);
    }

    private void forceLogoutToLogin() {
        if (isLoggingOut || SecurityGuard.isProcessingLogout()) {
            return;
        }

        isLoggingOut = true;
        SecurityGuard.setProcessingLogout(true);

        if (sessionTimer != null) {
            sessionTimer.stop();
        }

        try {
            JOptionPane.showMessageDialog(
                    this,
                    "Phiên đăng nhập đã hết hạn hoặc tài khoản đã được đăng nhập ở thiết bị khác.\n"
                    + "Vui lòng đăng nhập lại.",
                    "Thông báo bảo mật",
                    JOptionPane.WARNING_MESSAGE
            );

            business.service.HeartbeatService.stop();
            business.service.SessionManager.clear();

            try {
                common.auth.UserSession.getInstance().clear();
            } catch (Exception ignored) {
            }

            dispose();

            LoginView login = new LoginView();
            login.setLocationRelativeTo(null);
            login.setVisible(true);

        } catch (Exception ex) {
            System.err.println("[DashboardView] Force logout error: " + ex.getMessage());
        }
    }

    private void handleCloseApp() {
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Bạn có chắc muốn thoát ứng dụng?",
                "Xác nhận thoát",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            model.account.Account currentUser
                    = business.service.SessionManager.getCurrentUser();

            String sessionId
                    = business.service.SessionManager.getCurrentSessionId();

            if (currentUser != null
                    && currentUser.getAccountId() != null
                    && sessionId != null) {

                if (business.service.HeartbeatService.markLogoutOnce()) {
                    business.service.HeartbeatService.stop();

                    business.service.AccountService.onLogoutOrCloseApp(
                            currentUser.getAccountId(),
                            sessionId
                    );
                }
            }

        } catch (Exception ex) {
            System.err.println("[CloseApp] Không thể cập nhật session: " + ex.getMessage());
        }

        dispose();
        System.exit(0);
    }

    public static void main(String args[]) {
        System.setProperty("sun.java2d.uiScale", "1.5");

        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> {
            new DashboardView().setVisible(true);
        });
    }
}
