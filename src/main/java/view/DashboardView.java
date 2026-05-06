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

    // Biến lưu trữ tab hiện tại đang mở để Real-time biết đường mà refresh
    private String currentMenu = "Tổng quan";

    public DashboardView() {
        setupUI();
        startSessionCheck();

        // ==========================================
        // GẮN "MÁY NGHE LÉN" REAL-TIME CHO TOÀN BỘ DASHBOARD
        // ==========================================
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            SwingUtilities.invokeLater(() -> {
                // Nếu có bất kỳ thay đổi data nào, và user đang xem Tổng quan hoặc Thống kê thì tự động load lại
                if ("Tổng quan".equals(currentMenu)) {
                    showPanel(new TongQuanPanel());
                } else if ("Báo cáo & Thống kê".equals(currentMenu)) {
                    // Nếu user có quyền vô thống kê thì mới refresh
                    if (business.service.AuthorizationService.canAccessStatisticsAndEmployees()) {
                        showPanel(new StatisticView());
                    }
                }
            });
        });
    }

    private void setupUI() {
        // 1. LẤY THÔNG TIN USER ĐỂ HIỂN THỊ TRÊN TIÊU ĐỀ CỬA SỔ
        model.account.Account u = business.service.LoginService.getCurrentUser();
        String tk = business.service.LoginService.getToken();
        String username = "";

        if (u != null) {
            username = u.getUsername().trim();
            System.out.println("SESSION USER = " + username);
            System.out.println("SESSION TOKEN = " + tk);
            this.setTitle("SMART SUPERMARKET - STORE PORTAL | Chào, " + username);
        } else {
            this.setTitle("SMART SUPERMARKET - STORE PORTAL");
        }

        // 2. CẤU HÌNH CỬA SỔ CHÍNH
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setMinimumSize(new Dimension(1024, 768));
        this.setLocationRelativeTo(null);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());

        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(new java.awt.Color(245, 245, 247));

        // 3. KIỂM TRA PHÂN QUYỀN TRƯỚC KHI GẮN VÀO SIDEBAR
        boolean canAccessEmployee = business.service.AuthorizationService.canAccessStatisticsAndEmployees();
        boolean canAccessStatistics = business.service.AuthorizationService.canAccessStatisticsAndEmployees();
        String roleForSidebar = business.service.AuthorizationService.currentRoleForUi();

        // 4. KHỞI TẠO SIDEBAR BÊN TRÁI
        Sidebar newSidebar = new Sidebar(roleForSidebar);
        newSidebar.setMenuClickListener(title -> {
            currentMenu = title; // Cập nhật tab đang mở

            switch (title) {
                case "Tổng quan":
                    showPanel(new TongQuanPanel());
                    break;
                case "Quản lý sản phẩm":
                    showPanel(new ProductView());
                    break;
                case "Quản lý nhân viên":
                    if (!canAccessEmployee) {
                        JOptionPane.showMessageDialog(this, "Bạn không có quyền truy cập chức năng này!", "Từ chối truy cập", JOptionPane.WARNING_MESSAGE);
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
                    if (!canAccessStatistics) {
                        JOptionPane.showMessageDialog(this, "Bạn không có quyền truy cập chức năng này!", "Từ chối truy cập", JOptionPane.WARNING_MESSAGE);
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

        // 5. RÁP 2 KHỐI VÀO NHAU (SIDEBAR TRÁI - CONTENT PHẢI)
        this.getContentPane().add(newSidebar, BorderLayout.WEST);
        this.getContentPane().add(mainContentPanel, BorderLayout.CENTER);

        // Mặc định khi vừa login sẽ hiển thị Tổng Quan
        showPanel(new TongQuanPanel());
    }

    // ========================================================
    // CÁC HÀM XỬ LÝ GIAO DIỆN & LOGIC
    // ========================================================
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

            java.awt.EventQueue.invokeLater(() -> {
                view.LoginView login = new view.LoginView();
                login.setVisible(true);
                login.setLocationRelativeTo(null);
            });

            this.dispose();
        }
    }

    private void startSessionCheck() {
        sessionTimer = new Timer(1000, e -> {
            if (isLoggingOut) {
                ((Timer) e.getSource()).stop();
                return;
            }

            String currentToken = business.service.LoginService.getToken();
            boolean isValid = business.sql.rbac.TokenSql.getInstance().isTokenValid(currentToken);

            if (!isValid) {
                if (!isLoggingOut) {
                    ((Timer) e.getSource()).stop();
                    JOptionPane.showMessageDialog(this, "Phiên đăng nhập của bạn đã hết hạn!", "Thông báo bảo mật", JOptionPane.ERROR_MESSAGE);

                    java.awt.EventQueue.invokeLater(() -> {
                        view.LoginView login = new view.LoginView();
                        login.setVisible(true);
                        login.setLocationRelativeTo(null);
                    });

                    this.dispose();
                }
            }
        });
        sessionTimer.start();
    }

    // ========================================================
    // HÀM MAIN KHỞI ĐỘNG
    // ========================================================
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
