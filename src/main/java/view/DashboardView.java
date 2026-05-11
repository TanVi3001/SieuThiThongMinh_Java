package view;

import common.events.AppDataChangedEvent;
import common.events.EventBus;
import view.components.TongQuanPanel;
import view.components.Sidebar;

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

        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
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
                    showPanel(new view.SettingsView());
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
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có thực sự muốn đăng xuất không?", "Xác nhận", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            this.isLoggingOut = true;
            if (sessionTimer != null && sessionTimer.isRunning()) {
                sessionTimer.stop();
            }

            String tk = business.service.LoginService.getToken();
            business.sql.rbac.TokenSql.getInstance().revokeToken(tk);
            business.service.LoginService.logout();

            common.auth.UserSession.getInstance().clear();

            java.awt.EventQueue.invokeLater(() -> {
                view.LoginView login = new view.LoginView();
                login.setVisible(true);
                login.setLocationRelativeTo(null);
            });

            this.dispose();
        }
    }

    private void startSessionCheck() {
        sessionTimer = new Timer(2000, e -> {
            if (common.security.SecurityGuard.isProcessingLogout() || isLoggingOut) {
                ((Timer) e.getSource()).stop();
                return;
            }

            new Thread(() -> {
                if (common.security.SecurityGuard.isProcessingLogout() || isLoggingOut) {
                    return;
                }

                String currentToken = business.service.LoginService.getToken();
                boolean isValid = business.sql.rbac.TokenSql.getInstance().isTokenValid(currentToken);

                boolean roleChanged = false;
                model.account.Account currentUser = business.service.LoginService.getCurrentUser();

                if (currentUser != null) {
                    try {
                        String[] latestData = business.sql.rbac.AccountSql.getInstance().getAccountDetails(currentUser.getAccountId());
                        if (latestData != null) {
                            String dbRoleId = latestData[4];
                            boolean isActive = "0".equals(latestData[5]);

                            if (!isActive || !dbRoleId.equals(currentUser.getRoleValue())) {
                                roleChanged = true;
                            }
                        } else {
                            roleChanged = true;
                        }
                    } catch (Exception ex) {
                        // Bỏ qua lỗi kết nối DB tạm thời
                    }
                }

                if (!isValid || roleChanged) {
                    SwingUtilities.invokeLater(() -> {
                        if (!isLoggingOut && !common.security.SecurityGuard.isProcessingLogout()) {
                            isLoggingOut = true;
                            common.security.SecurityGuard.setProcessingLogout(true);

                            if (sessionTimer != null) {
                                sessionTimer.stop();
                            }

                            common.events.EventBus.clearAll();

                            JOptionPane.showMessageDialog(this, "Phiên đăng nhập đã hết hạn hoặc Quyền truy cập đã bị thay đổi!\nVui lòng đăng nhập lại.", "Thông báo bảo mật", JOptionPane.ERROR_MESSAGE);

                            try {
                                common.auth.UserSession.getInstance().clear();
                            } catch (Exception ignored) {
                            }
                            business.service.LoginService.logout();

                            java.awt.Window[] windows = java.awt.Window.getWindows();
                            boolean hasLogin = false;
                            for (java.awt.Window w : windows) {
                                if (w instanceof view.LoginView && w.isVisible()) {
                                    hasLogin = true;
                                    break;
                                }
                            }

                            if (!hasLogin) {
                                view.LoginView login = new view.LoginView();
                                login.setVisible(true);
                                login.setLocationRelativeTo(null);
                            }

                            this.dispose();
                        }
                    });
                }
            }).start();
        });

        sessionTimer.start();
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
}
