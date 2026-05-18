package view;

import javax.swing.*;
import java.awt.*;
import view.components.WarehouseSidebar;
import view.components.NotificationBell;

public class WarehouseDashboardView extends JFrame {

    private JPanel mainContentPanel;
    private WarehouseSidebar warehouseSidebar;

    // Đồng bộ màu nền với Store Portal
    private final Color BACKGROUND_COLOR = new Color(246, 247, 251);
    private final Color TOPBAR_BORDER = new Color(230, 230, 230);

    public WarehouseDashboardView() {
        setupWarehouseUI();
    }

    private void setupWarehouseUI() {
        model.account.Account currentUser = business.service.LoginService.getCurrentUser();

        String username = (currentUser != null && currentUser.getUsername() != null)
                ? currentUser.getUsername().trim()
                : "Nhân viên Kho";

        this.setTitle("SMART SUPERMARKET - WAREHOUSE PORTAL | " + username);

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

        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().setBackground(BACKGROUND_COLOR);

        warehouseSidebar = new WarehouseSidebar();

        warehouseSidebar.setMenuClickListener(title -> {
            switch (title) {
                case "Quản lý tồn kho":
                    showPanel(new view.InventoryView());
                    break;

                case "Quản lý sản phẩm":
                    showPanel(new view.ProductView());
                    break;

                case "Nhà cung cấp":
                    JOptionPane.showMessageDialog(
                            this,
                            "Chức năng Nhà cung cấp đang được phát triển.",
                            "Thông báo",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    break;

                case "Danh mục & Thuế VAT":
                    showPanel(new CategoryTaxView());
                    break;

                case "Cài đặt":
                    showPanel(new view.components.UnifiedSettingsPanel());
                    break;

                case "Đăng xuất":
                    handleLogout();
                    break;

                default:
                    break;
            }
        });

        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(BACKGROUND_COLOR);
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JPanel topBar = createTopBar();

        this.getContentPane().add(topBar, BorderLayout.NORTH);
        this.getContentPane().add(warehouseSidebar, BorderLayout.WEST);
        this.getContentPane().add(mainContentPanel, BorderLayout.CENTER);

        showPanel(new view.InventoryView());
    }

    private JPanel createTopBar() {
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));

        topBar.setBackground(Color.WHITE);
        topBar.setPreferredSize(new Dimension(0, 38));
        topBar.setBorder(BorderFactory.createMatteBorder(
                0,
                0,
                1,
                0,
                TOPBAR_BORDER
        ));

        NotificationBell bell = new NotificationBell(NotificationBell.Audience.WAREHOUSE);
        topBar.add(bell);

        return topBar;
    }

    public void showPanel(JPanel panel) {
        mainContentPanel.removeAll();

        JPanel panelToDisplay = panel;

        boolean isBypassed
                = (panel instanceof view.components.TongQuanPanel)
                || (panel instanceof view.components.UnifiedSettingsPanel);

        if (!isBypassed) {
            panelToDisplay = common.security.UIPermissionGuard.protect(panel);
        }

        panelToDisplay.setMinimumSize(new Dimension(900, 600));
        panelToDisplay.setBackground(BACKGROUND_COLOR);

        JScrollPane scrollPane = new JScrollPane(panelToDisplay);

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

            business.service.SessionManager.clear();

            try {
                common.auth.UserSession.getInstance().clear();
            } catch (Exception ignored) {
            }

        } catch (Exception ex) {
            System.err.println("[Warehouse CloseApp] Không thể cập nhật session: " + ex.getMessage());
        }

        dispose();
        System.exit(0);
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

            try {
                common.auth.UserSession.getInstance().clear();
            } catch (Exception ignored) {
            }

        } catch (Exception ex) {
            System.err.println("[Warehouse Logout] Lỗi đăng xuất: " + ex.getMessage());
        }

        dispose();

        view.LoginView login = new view.LoginView();
        login.setLocationRelativeTo(null);
        login.setVisible(true);
    }

    public static void main(String args[]) {
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        java.awt.EventQueue.invokeLater(() -> new WarehouseDashboardView().setVisible(true));
    }
}
