package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import javax.swing.*;
import java.awt.*;
import view.components.AdminSidebar;

public class AdminDashboardView extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AdminDashboardView.class.getName());

    private JPanel mainContentPanel;
    private AdminSidebar adminSidebar;
    private Color bgAdmin = new Color(240, 242, 245);
    private String currentMenu = "Quản lý chi nhánh";

    public AdminDashboardView() {
        initComponents();
        setupAdminUI();
        setupRealtimeSync();

        // Sidebar mặc định đang active mục đầu tiên là "Quản lý chi nhánh",
        // nên nội dung mặc định cũng phải là màn hình Quản lý chi nhánh.
        showPanel(new view.AdminSystemPanel());

        // Đảm bảo JFrame được phóng to sau khi toàn bộ component đã add xong.
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

        // Khởi tạo Sidebar Admin
        adminSidebar = new AdminSidebar();

        // NỐI CÁC MỤC MENU VỚI PANEL TƯƠNG ỨNG
        adminSidebar.setMenuClickListener(title -> {
            currentMenu = title;

            switch (title) {
                case "Quản lý chi nhánh":
                    showPanel(new view.AdminSystemPanel());
                    break;
                case "Quản lý khuyến mãi":
                    showPanel(new view.PromotionManagementPanel());
                    break;
                case "Quản lý cửa hàng trưởng":
                    showPanel(new view.ManagerManagementView());
                    break;
                case "Quản lý tài khoản":
                    showPanel(new view.AccountRoleAssignmentPanel());
                    break;
                case "Quản lý phân quyền":
                    showPanel(new view.RoleManagementPanel());
                    break;
                case "Lịch sử truy cập":
                    showPanel(new view.LoginManagementPanel());
                    break;
                case "Nhật ký hệ thống":
                    showPanel(new AuditLogPanel());
                    break;
                case "Cài đặt":
                    showPanel(new view.components.UnifiedSettingsPanel());
                    break;
                case "Đăng xuất":
                    handleLogout();
                    break;
            }
        });

        // Thiết lập Layout chính
        this.getContentPane().removeAll();
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(adminSidebar, BorderLayout.WEST);

        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(bgAdmin);
        this.getContentPane().add(mainContentPanel, BorderLayout.CENTER);

        // Sau khi add đủ sidebar + content container thì pack lại,
        // rồi mới set maximize để tránh bị giữ kích thước 400x300 từ initComponents().
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
                    showPanel(new view.StoreManagementPanel());
                }
                break;

            case "Quản lý khuyến mãi":
                if (type == AppEventType.SYSTEM_CONFIG
                        || type == AppEventType.PRODUCTS
                        || type == AppEventType.DASHBOARD) {
                    showPanel(new view.PromotionManagementPanel());
                }
                break;

            case "Quản lý cửa hàng trưởng":
                if (type == AppEventType.EMPLOYEES
                        || type == AppEventType.ACCOUNT_SECURITY
                        || type == AppEventType.STORE_INFO) {
                    showPanel(new view.ManagerManagementView());
                }
                break;

            case "Quản lý tài khoản":
                if (type == AppEventType.ACCOUNT_SECURITY
                        || type == AppEventType.EMPLOYEES) {
                    showPanel(new view.AccountRoleAssignmentPanel());
                }
                break;

            case "Quản lý phân quyền":
                if (type == AppEventType.ACCOUNT_SECURITY
                        || type == AppEventType.SYSTEM_CONFIG) {
                    showPanel(new view.RoleManagementPanel());
                }
                break;

            case "Lịch sử truy cập":
                if (type == AppEventType.ACCOUNT_SECURITY
                        || type == AppEventType.SYSTEM_CONFIG) {
                    showPanel(new view.LoginManagementPanel());
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
                    showPanel(new AuditLogPanel());
                }
                break;

            case "Cài đặt":
                if (type == AppEventType.SYSTEM_CONFIG
                        || type == AppEventType.ACCOUNT_SECURITY) {
                    showPanel(new view.components.UnifiedSettingsPanel());
                }
                break;

            default:
                break;
        }
    }

    public void showPanel(JPanel panel) {
        mainContentPanel.removeAll();

        JPanel panelToDisplay = panel;

        // ========================================================
        // LOGIC MIỄN TRỪ (BYPASS):
        // Bỏ qua Lính gác đối với các trang Tổng quan và Cài đặt cá nhân
        // ========================================================
        boolean isBypassed = (panel instanceof view.components.TongQuanPanel)
                || (panel instanceof view.components.UnifiedSettingsPanel);

        if (!isBypassed) {
            // Đưa cho Lính gác kiểm tra và khóa nút (Dù Admin full quyền thì vẫn qua cổng cho chuẩn luồng)
            panelToDisplay = common.security.UIPermissionGuard.protect(panel);
        }

        // BẢO VỆ LỚP 2: Bọc thẻ con vào Thanh cuộn để chống ép bẹp biểu đồ
        panelToDisplay.setMinimumSize(new Dimension(900, 600)); // Kích thước an toàn cho thẻ con

        JScrollPane scrollPane = new JScrollPane(panelToDisplay);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // Lăn chuột mượt hơn
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