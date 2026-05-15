package view;

import common.events.AppDataChangedEvent;
import common.events.EventBus;
import view.components.TongQuanPanel;
import view.components.Sidebar;
import business.service.AccountService;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import common.events.AppEventType;
import common.security.SecurityGuard;

public class DashboardView extends JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DashboardView.class.getName());

    private Timer sessionTimer;
    private boolean isLoggingOut = false;
    private final long dashboardCreatedAt = System.currentTimeMillis();
    private JPanel mainContentPanel;

    // Màu nền chuẩn cho UI hiện đại (Trắng xám nhẹ)
    private final Color BACKGROUND_COLOR = new Color(245, 245, 247);

    private String currentMenu = "Tổng quan";

    public DashboardView() {
        setupUI();
        startSessionCheck();

        common.security.SecurityGuard.attach(mainContentPanel);

        // =========================================================
        // 🌟 BẮT SÓNG REAL-TIME TỪ TỔNG ĐÀI (GIỮ NGUYÊN 100%)
        // =========================================================
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            SwingUtilities.invokeLater(() -> {
                // Chỉ nạp lại dữ liệu nếu người dùng ĐANG MỞ tab đó
                if (e.getType() == AppEventType.ORDERS && "Tổng quan".equals(currentMenu)) {
                    System.out.println("Cập nhật Real-time: Refresh Dashboard...");

                    // Lấy cái JScrollPane (lớp bảo vệ 2) đang chứa TongQuanPanel
                    if (mainContentPanel.getComponentCount() > 0) {
                        Component c = mainContentPanel.getComponent(0);
                        if (c instanceof JScrollPane) {
                            JScrollPane scroll = (JScrollPane) c;
                            Component innerPanel = scroll.getViewport().getView();

                            // Nếu cái bên trong là TongQuanPanel thì ép nó tải lại DB
                            if (innerPanel instanceof TongQuanPanel) {
                                // Gọi thẳng vào hàm đã public
                                ((TongQuanPanel) innerPanel).loadRealData();
                            }
                        }
                    }
                } else if ("Báo cáo & Thống kê".equals(currentMenu)) {
                    if (business.service.AuthorizationService.canAccessStatisticsAndEmployees()) {
                        showPanel(new StatisticView());
                    }
                }
            });
        });
    }

    private void setupUI() {
        model.account.Account u = business.service.LoginService.getCurrentUser();
        String tk = business.service.LoginService.getToken();
        String username = "";

        if (u != null) {
            username = u.getUsername().trim();
            this.setTitle("SMART SUPERMARKET - STORE PORTAL | Chào, " + username);
        } else {
            this.setTitle("SMART SUPERMARKET - STORE PORTAL");
        }

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleCloseApp();
            }
        });
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setMinimumSize(new Dimension(1100, 700));
        this.setLocationRelativeTo(null);

        // Sử dụng BorderLayout chuẩn chỉ
        this.getContentPane().setLayout(new BorderLayout());

        // Panel chứa nội dung chính
        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(BACKGROUND_COLOR);

        // Tạo khoảng trống (margin) giữa Sidebar và Content để UI bớt ngộp
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        String roleForSidebar = common.auth.UserSession.getInstance().getUserRole();
        boolean isStaff = "R_STAFF_SALE".equals(roleForSidebar);

        Sidebar newSidebar = new Sidebar(roleForSidebar);
        newSidebar.setMenuClickListener(title -> {
            currentMenu = title;

            switch (title) {
                case "Tổng quan":
                    showPanel(new TongQuanPanel());
                    break;
                case "Bán hàng":
                    showPanel(new SellPanel());
                    break;
                case "Quản lý sản phẩm":
                    showPanel(new ProductView());
                    break;
                case "Quản lý nhân viên":
                    if (isStaff) {
                        JOptionPane.showMessageDialog(this, "Bạn không có quyền truy cập!", "Từ chối", JOptionPane.WARNING_MESSAGE);
                    } else {
                        showPanel(new view.EmployeeView());
                    }
                    break;
                case "Khách hàng":
                    showPanel(new CustomerView());
                    break;
                case "Hóa đơn":
                    showPanel(new OrderView());
                    break;
                case "Báo cáo & Thống kê":
                    if (isStaff) {
                        JOptionPane.showMessageDialog(this, "Bạn không có quyền truy cập!", "Từ chối", JOptionPane.WARNING_MESSAGE);
                    } else {
                        showPanel(new StatisticView());
                    }
                    break;
                case "Cài đặt":
                    showPanel(new view.components.UnifiedSettingsPanel());
                    break;
                case "Đăng xuất":
                    handleLogout();
                    break;
            }
        });

        // Bố trí Sidebar bên trái và Content ở giữa
        this.getContentPane().add(newSidebar, BorderLayout.WEST);
        this.getContentPane().add(mainContentPanel, BorderLayout.CENTER);

        // Hiển thị mặc định
        showPanel(new TongQuanPanel());
    }

    public void showPanel(JPanel childPanel) {
        mainContentPanel.removeAll();

        childPanel.setMinimumSize(new Dimension(900, 600));
        // Đảm bảo các panel con có màu nền đồng nhất với khung chính
        childPanel.setBackground(BACKGROUND_COLOR);

        JScrollPane scrollPane = new JScrollPane(childPanel);

        // CÁC THIẾT LẬP FLATLAF CHO SCROLLPANE:
        scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Xóa viền thô cứng
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR); // Đồng bộ nền Viewport
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Bo góc cho thanh cuộn (Tùy chọn hiển thị mượt hơn trong FlatLaf)
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

            common.security.SecurityGuard.setProcessingLogout(false);

        } catch (Exception ex) {
            System.err.println("[Logout] Lỗi logout: " + ex.getMessage());
        }

        dispose();

        view.LoginView login = new view.LoginView();
        login.setLocationRelativeTo(null);
        login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        login.setVisible(true);
    }

    private void startSessionCheck() {
        sessionTimer = new Timer(5000, e -> {
            if (common.security.SecurityGuard.isProcessingLogout() || isLoggingOut) {
                ((Timer) e.getSource()).stop();
                return;
            }

            // Tránh vừa mở Dashboard đã check quá sớm rồi đá user
            if (System.currentTimeMillis() - dashboardCreatedAt < 10000) {
                return;
            }

            new Thread(() -> {
                if (common.security.SecurityGuard.isProcessingLogout() || isLoggingOut) {
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
                    // Admin / Manager được đăng nhập nhiều app nên KHÔNG kiểm tra CURRENT_SESSION_ID
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

                    // Check role thật sự có bị đổi không
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
                    // Lỗi DB tạm thời thì KHÔNG logout, tránh đá nhầm user
                    System.err.println("[SessionCheck] Bỏ qua lỗi kiểm tra session: " + ex.getMessage());
                    return;
                }

                if (forceLogout) {
                    SwingUtilities.invokeLater(() -> forceLogoutToLogin());
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
        if (isLoggingOut || common.security.SecurityGuard.isProcessingLogout()) {
            return;
        }

        isLoggingOut = true;
        common.security.SecurityGuard.setProcessingLogout(true);

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

            view.LoginView login = new view.LoginView();
            login.setLocationRelativeTo(null);
            login.setVisible(true);

        } catch (Exception ex) {
            System.err.println("[DashboardView] Force logout error: " + ex.getMessage());
        }

        // Không setProcessingLogout(false) ở đây.
        // Khi user login lại, LoginView hoặc HeartbeatService.start() sẽ reset.
    }

    public static void main(String args[]) {
        System.setProperty("sun.java2d.uiScale", "1.5");
        try {
            // Khuyến nghị dùng FlatMacLightLaf hoặc FlatIntelliJLaf để có độ mượt và bóng bẩy hơn FlatLightLaf cơ bản
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> {
            new DashboardView().setVisible(true);
        });
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
