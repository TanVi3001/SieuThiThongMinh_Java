package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import view.components.AdminSidebar;

public class AdminDashboardView extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AdminDashboardView.class.getName());

    private JPanel mainContentPanel;
    private AdminSidebar adminSidebar;
    private final Color bgAdmin = new Color(240, 242, 245);
    private String currentMenu = "Quản lý chi nhánh";
    private volatile SwingWorker<JPanel, Void> currentPanelWorker;
    private final Map<String, JPanel> panelCache = new HashMap<>();

    public AdminDashboardView() {
        initComponents();
        setupAdminUI();
        setupRealtimeSync();

        showMenuPanel("Quản lý chi nhánh", view.AdminSystemPanel::new);

        SwingUtilities.invokeLater(() -> setExtendedState(JFrame.MAXIMIZED_BOTH));
    }

    private void setupAdminUI() {
        this.setTitle("SMART SUPERMARKET - CENTRAL ADMIN PORTAL");
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.setResizable(true);
        this.setMinimumSize(new Dimension(1100, 700));

        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            }

            @Override
            public void windowActivated(java.awt.event.WindowEvent e) {
                setExtendedState(JFrame.MAXIMIZED_BOTH);
            }

            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleCloseApp();
            }
        });

        adminSidebar = new AdminSidebar();

        adminSidebar.setMenuClickListener(title -> {
            if (title != null && title.equals(currentMenu) && mainContentPanel.getComponentCount() > 0) {
                return;
            }

            currentMenu = title;

            switch (title) {
                case "Quản lý chi nhánh":
                    showMenuPanel("Quản lý chi nhánh", view.AdminSystemPanel::new);
                    break;
                case "Quản lý khuyến mãi":
                    showMenuPanel("Quản lý khuyến mãi", view.PromotionManagementPanel::new);
                    break;
                case "Quản lý cửa hàng trưởng":
                    showMenuPanel("Quản lý cửa hàng trưởng", view.ManagerManagementView::new);
                    break;
                case "Quản lý tài khoản":
                    showMenuPanel("Quản lý tài khoản", view.AccountRoleAssignmentPanel::new);
                    break;
                case "Quản lý phân quyền":
                    showMenuPanel("Quản lý phân quyền", view.RoleManagementPanel::new);
                    break;
                case "Lịch sử truy cập":
                    showMenuPanel("Lịch sử truy cập", view.LoginManagementPanel::new);
                    break;
                case "Nhật ký hệ thống":
                    showMenuPanel("Nhật ký hệ thống", AuditLogPanel::new);
                    break;
                case "Cài đặt":
                    showMenuPanel("Cài đặt", view.components.UnifiedSettingsPanel::new);
                    break;
                case "Đăng xuất":
                    handleLogout();
                    break;
                default:
                    break;
            }
        });

        this.getContentPane().removeAll();
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(adminSidebar, BorderLayout.WEST);

        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(bgAdmin);
        this.getContentPane().add(mainContentPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
    }

    private void setupRealtimeSync() {
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e == null || e.getType() == null) {
                return;
            }

            SwingUtilities.invokeLater(() -> refreshCurrentAdminPanel(e.getType()));
        });
    }

    private void refreshCurrentAdminPanel(AppEventType type) {
        if (currentMenu == null) {
            return;
        }

        switch (currentMenu) {
            case "Quản lý chi nhánh":
                if (type == AppEventType.STORE_INFO
                        || type == AppEventType.SYSTEM_CONFIG
                        || type == AppEventType.DASHBOARD) {
                    panelCache.remove("Quản lý chi nhánh");
                    showMenuPanel("Quản lý chi nhánh", () -> new view.AdminSystemPanel(1));
                }
                break;

            case "Quản lý khuyến mãi":
                if (type == AppEventType.SYSTEM_CONFIG
                        || type == AppEventType.PRODUCTS
                        || type == AppEventType.DASHBOARD) {
                    panelCache.remove("Quản lý khuyến mãi");
                    showMenuPanel("Quản lý khuyến mãi", view.PromotionManagementPanel::new);
                }
                break;

            case "Quản lý cửa hàng trưởng":
                if (type == AppEventType.EMPLOYEES
                        || type == AppEventType.ACCOUNT_SECURITY
                        || type == AppEventType.STORE_INFO) {
                    panelCache.remove("Quản lý cửa hàng trưởng");
                    showMenuPanel("Quản lý cửa hàng trưởng", view.ManagerManagementView::new);
                }
                break;

            case "Quản lý tài khoản":
                if (type == AppEventType.ACCOUNT_SECURITY
                        || type == AppEventType.EMPLOYEES) {
                    panelCache.remove("Quản lý tài khoản");
                    showMenuPanel("Quản lý tài khoản", view.AccountRoleAssignmentPanel::new);
                }
                break;

            case "Quản lý phân quyền":
                if (type == AppEventType.ACCOUNT_SECURITY
                        || type == AppEventType.SYSTEM_CONFIG) {
                    panelCache.remove("Quản lý phân quyền");
                    showMenuPanel("Quản lý phân quyền", view.RoleManagementPanel::new);
                }
                break;

            case "Lịch sử truy cập":
                if (type == AppEventType.ACCOUNT_SECURITY
                        || type == AppEventType.SYSTEM_CONFIG) {
                    panelCache.remove("Lịch sử truy cập");
                    showMenuPanel("Lịch sử truy cập", view.LoginManagementPanel::new);
                }
                break;

            case "Nhật ký hệ thống":
                if (type == AppEventType.SYSTEM_CONFIG
                        || type == AppEventType.ACCOUNT_SECURITY
                        || type == AppEventType.PRODUCTS
                        || type == AppEventType.INVENTORY
                        || type == AppEventType.ORDERS
                        || type == AppEventType.CUSTOMERS
                        || type == AppEventType.EMPLOYEES
                        || type == AppEventType.STORE_INFO) {
                    panelCache.remove("Nhật ký hệ thống");
                    showMenuPanel("Nhật ký hệ thống", AuditLogPanel::new);
                }
                break;

            case "Cài đặt":
                if (type == AppEventType.SYSTEM_CONFIG
                        || type == AppEventType.ACCOUNT_SECURITY) {
                    panelCache.remove("Cài đặt");
                    showMenuPanel("Cài đặt", view.components.UnifiedSettingsPanel::new);
                }
                break;

            default:
                break;
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
        loading.setBackground(bgAdmin);
        loading.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
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
        progressBar.setPreferredSize(new Dimension(320, 10));
        progressBar.setMaximumSize(new Dimension(320, 10));
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(label);
        card.add(Box.createVerticalStrut(10));
        card.add(progressBar);
        card.add(Box.createVerticalStrut(10));
        card.add(hint);

        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setOpaque(false);
        wrapper.add(card);

        loading.add(wrapper, BorderLayout.CENTER);
        showPanel(loading, false);
    }

    private void showErrorPanel(String title, Exception ex) {
        JPanel error = new JPanel(new BorderLayout());
        error.setBackground(bgAdmin);

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

    public void showPanel(JPanel panel) {
        showPanel(panel, true);
    }

    private void showPanel(JPanel panel, boolean applyGuard) {
        mainContentPanel.removeAll();

        JPanel panelToDisplay = panel;

        boolean isBypassed = (panel instanceof view.components.TongQuanPanel)
                || (panel instanceof view.components.UnifiedSettingsPanel);

        if (applyGuard && !isBypassed) {
            panelToDisplay = common.security.UIPermissionGuard.protect(panel);
        }

        panelToDisplay.setMinimumSize(new Dimension(900, 600));
        panelToDisplay.setBackground(bgAdmin);

        JScrollPane scrollPane = new JScrollPane(panelToDisplay);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

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

            business.service.SessionManager.clear();

        } catch (Exception ex) {
            System.err.println("[Logout] Lỗi logout: " + ex.getMessage());
        }

        dispose();

        view.LoginView login = new view.LoginView();
        login.setLocationRelativeTo(null);
        login.setVisible(true);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 300, Short.MAX_VALUE)
        );
        pack();
    }

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(() -> new AdminDashboardView().setVisible(true));
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
}
