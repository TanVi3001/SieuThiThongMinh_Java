package view;

import common.events.AppDataChangedEvent;
import common.events.EventBus;
import view.components.TongQuanPanel;
import view.components.Sidebar;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class DashboardView extends JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DashboardView.class.getName());

    private Timer sessionTimer;
    private boolean isLoggingOut = false;
    private JPanel mainContentPanel;

    private String currentMenu = "Tổng quan";

    public DashboardView() {
        setupUI();
        startSessionCheck();

        common.security.SecurityGuard.attach(mainContentPanel);

        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            SwingUtilities.invokeLater(() -> {
                if ("Tổng quan".equals(currentMenu)) {
                    showPanel(new TongQuanPanel());
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

        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setMinimumSize(new Dimension(1024, 768));
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());

        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(new java.awt.Color(245, 245, 247));

        String roleForSidebar = common.auth.UserSession.getInstance().getUserRole();
        boolean isStaff = "R_STAFF_SALE".equals(roleForSidebar);

        Sidebar newSidebar = new Sidebar(roleForSidebar);
        newSidebar.setMenuClickListener(title -> {
            currentMenu = title;

            switch (title) {
                case "Tổng quan":
                    showPanel(new TongQuanPanel());
                    break;
                case "Quản lý sản phẩm":
                    if (isStaff) {
                        JOptionPane.showMessageDialog(this, "Bạn không có quyền truy cập!", "Từ chối", JOptionPane.WARNING_MESSAGE);
                    } else {
                        showPanel(new ProductView());
                    }
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

        this.getContentPane().add(newSidebar, BorderLayout.WEST);
        this.getContentPane().add(mainContentPanel, BorderLayout.CENTER);

        showPanel(new TongQuanPanel());
    }

    public void showPanel(JPanel childPanel) {
        mainContentPanel.removeAll();
        mainContentPanel.add(childPanel, BorderLayout.CENTER);
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

            // TUI ĐÃ THÊM DÒNG NÀY ĐỂ XÓA SẠCH GỐC RỄ PHÂN QUYỀN TRONG CACHE SESSION
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
            if (isLoggingOut) {
                ((Timer) e.getSource()).stop();
                return;
            }

            // Đưa việc quét DB vào Thread ngầm để không làm giật lag giao diện
            new Thread(() -> {
                // 1. Kiểm tra Token
                String currentToken = business.service.LoginService.getToken();
                boolean isValid = business.sql.rbac.TokenSql.getInstance().isTokenValid(currentToken);

                // 2. RADAR BẢO MẬT: Kiểm tra xem quyền có bị Admin đổi ngầm dưới DB không
                boolean roleChanged = false;
                model.account.Account currentUser = business.service.LoginService.getCurrentUser();

                if (currentUser != null) {
                    try {
                        String[] latestData = business.sql.rbac.AccountSql.getInstance().getAccountDetails(currentUser.getAccountId());
                        if (latestData != null) {
                            String dbRoleId = latestData[4]; // Quyền đang lưu dưới DB
                            boolean isActive = "0".equals(latestData[5]);

                            // So sánh quyền DB với quyền lúc vừa đăng nhập (nếu lệch là bị đá)
                            if (!isActive || !dbRoleId.equals(currentUser.getRoleValue())) {
                                roleChanged = true;
                            }
                        } else {
                            roleChanged = true; // Tài khoản bị xóa thẳng tay
                        }
                    } catch (Exception ex) {
                        // Bỏ qua lỗi DB tạm thời nếu mạng giật
                    }
                }

                // NẾU PHÁT HIỆN BẤT THƯỜNG -> KICK NGAY LẬP TỨC
                if (!isValid || roleChanged) {
                    SwingUtilities.invokeLater(() -> {
                        if (!isLoggingOut) {
                            isLoggingOut = true;
                            if (sessionTimer != null) {
                                sessionTimer.stop();
                            }

                            JOptionPane.showMessageDialog(this, "Phiên đăng nhập đã hết hạn hoặc Quyền truy cập đã bị thay đổi!\nVui lòng đăng nhập lại.", "Thông báo bảo mật", JOptionPane.ERROR_MESSAGE);

                            // Xóa sạch thông tin cũ
                            try {
                                common.auth.UserSession.getInstance().clear();
                            } catch (Exception ignored) {
                            }
                            business.service.LoginService.logout();

                            // Văng ra màn hình Login
                            view.LoginView login = new view.LoginView();
                            login.setVisible(true);
                            login.setLocationRelativeTo(null);

                            this.dispose(); // Tắt Dashboard hiện tại
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
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        java.awt.EventQueue.invokeLater(() -> {
            new DashboardView().setVisible(true);
        });
    }
}
