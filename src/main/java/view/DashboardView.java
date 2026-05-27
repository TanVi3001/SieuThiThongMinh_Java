package view;

import business.service.AuthorizationService;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.security.SecurityGuard;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import view.components.NotificationBell;
import view.components.Sidebar;
import view.components.TongQuanPanel;
import view.util.RolePermissionButtonGuard;

public class DashboardView extends JFrame {

    private static final java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger(DashboardView.class.getName());

    private Timer sessionTimer;
    private boolean isLoggingOut = false;
    private final long dashboardCreatedAt = System.currentTimeMillis();

    private JPanel mainContentPanel;

    private final Color BACKGROUND_COLOR = new Color(245, 245, 247);

    private String currentMenu = "Tổng quan";
    private volatile SwingWorker<JPanel, Void> currentPanelWorker;
    private final Map<String, JPanel> panelCache = new HashMap<>();

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

                if (e.getType() == AppEventType.ORDERS) {
                    panelCache.remove("Hóa đơn");
                    return;
                }

                if (e.getType() == AppEventType.PRODUCTS || e.getType() == AppEventType.INVENTORY) {
                    panelCache.remove("Bán hàng");
                    panelCache.remove("Quản lý sản phẩm");
                    panelCache.remove("Quản lý tồn kho");
                    return;
                }

                if ("Báo cáo & Thống kê".equals(currentMenu)
                        && AuthorizationService.canAccessReports()) {
                    panelCache.remove("Báo cáo & Thống kê");
                    showMenuPanel("Báo cáo & Thống kê", StatisticView::new);
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
            if (title != null && title.equals(currentMenu) && mainContentPanel.getComponentCount() > 0) {
                return;
            }

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

        if (AuthorizationService.canAccessDashboard()) {
            showMenuPanel("Tổng quan", TongQuanPanel::new);
        } else {
            showAccessDenied();
        }
    }

    private void handleMenuClick(String title) {
        switch (title) {
            case "Tổng quan":
                if (!AuthorizationService.canAccessDashboard()) {
                    showAccessDenied();
                    return;
                }
                showMenuPanel("Tổng quan", TongQuanPanel::new);
                break;

            case "Bán hàng":
                if (!AuthorizationService.canAccessSales()) {
                    showAccessDenied();
                    return;
                }
                showMenuPanel("Bán hàng", SellPanel::new);
                break;

            case "Quản lý sản phẩm":
                if (!AuthorizationService.canAccessProductsAndInventory()) {
                    showAccessDenied();
                    return;
                }
                showMenuPanel("Quản lý sản phẩm", ProductView::new);
                break;

            case "Quản lý tồn kho":
                if (!AuthorizationService.canManageStock()) {
                    showAccessDenied();
                    return;
                }
                showMenuPanel("Quản lý tồn kho", InventoryView::new);
                break;

            case "Quản lý nhà cung cấp":
                if (!AuthorizationService.canAccessSupplierAndCategory()) {
                    showAccessDenied();
                    return;
                }
                showMenuPanel("Quản lý nhà cung cấp", SupplierManagementView::new);
                break;

            case "Danh mục & Thuế VAT":
                if (!AuthorizationService.canAccessSupplierAndCategory()) {
                    showAccessDenied();
                    return;
                }
                showMenuPanel("Danh mục & Thuế VAT", CategoryTaxView::new);
                break;

            case "Quản lý nhân viên":
                if (!AuthorizationService.canAccessEmployees()) {
                    showAccessDenied();
                    return;
                }
                showMenuPanel("Quản lý nhân viên", EmployeeView::new);
                break;

            case "Khách hàng":
                if (!AuthorizationService.canAccessCustomers()) {
                    showAccessDenied();
                    return;
                }
                showMenuPanel("Khách hàng", CustomerView::new);
                break;

            case "Hóa đơn":
                if (!AuthorizationService.canAccessOrders()) {
                    showAccessDenied();
                    return;
                }
                showMenuPanel("Hóa đơn", OrderView::new);
                break;

            case "Báo cáo & Thống kê":
                if (!AuthorizationService.canAccessReports()) {
                    showAccessDenied();
                    return;
                }
                showMenuPanel("Báo cáo & Thống kê", StatisticView::new);
                break;

            case "Cài đặt":
                if (!AuthorizationService.canAccessSettings()) {
                    showAccessDenied();
                    return;
                }
                showMenuPanel("Cài đặt", view.components.UnifiedSettingsPanel::new);
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

    private void showMenuPanel(String cacheKey, Supplier<JPanel> panelFactory) {
        if (cacheKey == null || panelFactory == null) {
            return;
        }

        if (currentPanelWorker != null && !currentPanelWorker.isDone()) {
            currentPanelWorker.cancel(true);
        }

        JPanel cached = panelCache.get(cacheKey);
        if (cached != null) {
            showPanel(cached);
            return;
        }

        showLoadingPanel(cacheKey);

        SwingWorker<JPanel, Void> worker = new SwingWorker<>() {
            @Override
            protected JPanel doInBackground() {
                return panelFactory.get();
            }

            @Override
            protected void done() {
                if (isCancelled()) {
                    return;
                }

                try {
                    JPanel panel = get();
                    panelCache.put(cacheKey, panel);

                    if (cacheKey.equals(currentMenu)) {
                        showPanel(panel);
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showErrorPanel(cacheKey, ex);
                }
            }
        };

        currentPanelWorker = worker;
        worker.execute();
    }

    private void showLoadingPanel(String title) {
        JPanel loading = new JPanel(new BorderLayout());
        loading.setBackground(BACKGROUND_COLOR);
        loading.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        JPanel card = new JPanel();
        card.setLayout(new javax.swing.BoxLayout(card, javax.swing.BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(225, 229, 235)),
                BorderFactory.createEmptyBorder(24, 32, 24, 32)
        ));

        JLabel label = new JLabel("Đang tải " + title + "...");
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 16));
        label.setForeground(new Color(35, 45, 75));

        JLabel hint = new JLabel("Vui lòng chờ trong giây lát, hệ thống đang chuẩn bị dữ liệu.");
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        hint.setForeground(new Color(100, 110, 125));

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setStringPainted(false);
        progressBar.setPreferredSize(new Dimension(320, 10));
        progressBar.setMaximumSize(new Dimension(320, 10));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(label);
        card.add(javax.swing.Box.createVerticalStrut(10));
        card.add(progressBar);
        card.add(javax.swing.Box.createVerticalStrut(10));
        card.add(hint);

        JPanel wrapper = new JPanel(new java.awt.GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(card);

        loading.add(wrapper, BorderLayout.CENTER);
        showPanel(loading, false);
    }

    private void showErrorPanel(String title, Exception ex) {
        JPanel error = new JPanel(new BorderLayout());
        error.setBackground(BACKGROUND_COLOR);

        String message = ex == null || ex.getMessage() == null ? "Không rõ lỗi" : ex.getMessage();
        JLabel label = new JLabel(
                "<html><div style='text-align:center;'>Không thể tải " + title
                + ".<br>" + message + "</div></html>",
                JLabel.CENTER
        );
        label.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        label.setForeground(new Color(220, 53, 69));

        error.add(label, BorderLayout.CENTER);
        showPanel(error, false);
    }

    public void showPanel(JPanel childPanel) {
        showPanel(childPanel, true);
    }

    private void showPanel(JPanel childPanel, boolean applyPermissionGuard) {
        mainContentPanel.removeAll();

        childPanel.setMinimumSize(new Dimension(900, 600));
        childPanel.setBackground(BACKGROUND_COLOR);

        if (applyPermissionGuard) {
            RolePermissionButtonGuard.applyTo(childPanel);
        }

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

        if (applyPermissionGuard) {
            SwingUtilities.invokeLater(() -> RolePermissionButtonGuard.applyTo(childPanel));
        }
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
        sessionTimer = new Timer(10000, e -> {
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
                    String[] latestData
                            = business.sql.rbac.AccountSql.getInstance()
                                    .getAccountDetails(currentUser.getAccountId());

                    if (latestData == null) {
                        forceLogout = true;
                    } else {
                        String dbRoleId = latestData[4];

                        String currentRole = business.service.SessionManager.getCurrentRole();

                        if (currentRole == null || currentRole.trim().isEmpty()) {
                            currentRole = currentUser.getRoleId() != null
                                    ? currentUser.getRoleId()
                                    : currentUser.getRoleValue();
                        }

                        if (currentRole == null || currentRole.trim().isEmpty()) {
                            currentRole = currentUser.getRole();
                        }

                        if (dbRoleId == null
                                || currentRole == null
                                || !dbRoleId.trim().equalsIgnoreCase(currentRole.trim())) {
                            forceLogout = true;
                        }
                        if (!forceLogout
                                && business.service.SessionManager.isStoreScopedUser()
                                && !business.sql.rbac.AccountSql.getInstance()
                                        .isAccountStoreActive(currentUser.getAccountId())) {
                            forceLogout = true;
                        }
                    }

                } catch (Exception ex) {
                    System.err.println("[SessionCheck] Bỏ qua lỗi kiểm tra role/session: " + ex.getMessage());
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
                    "Phiên đăng nhập không còn hợp lệ.\n"
                    + "Tài khoản có thể đã bị đổi quyền, bị khóa hoặc chi nhánh đã tạm ngưng hoạt động.\n"
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
