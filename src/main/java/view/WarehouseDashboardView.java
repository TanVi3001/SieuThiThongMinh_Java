package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import view.components.NotificationBell;
import view.components.WarehouseSidebar;

public class WarehouseDashboardView extends JFrame {

    private JPanel mainContentPanel;
    private WarehouseSidebar warehouseSidebar;

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

        setTitle("SMART SUPERMARKET - WAREHOUSE PORTAL | " + username);
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
        getContentPane().setBackground(BACKGROUND_COLOR);

        warehouseSidebar = new WarehouseSidebar();
        warehouseSidebar.setMenuClickListener(this::handleSidebarMenuClick);

        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(BACKGROUND_COLOR);
        mainContentPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        JPanel topBar = createTopBar();

        getContentPane().add(topBar, BorderLayout.NORTH);
        getContentPane().add(warehouseSidebar, BorderLayout.WEST);
        getContentPane().add(mainContentPanel, BorderLayout.CENTER);

        warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_INVENTORY);
        showPanel(new InventoryView());
    }

    private void handleSidebarMenuClick(String title) {
        switch (title) {
            case WarehouseSidebar.MENU_INVENTORY -> {
                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_INVENTORY);
                showPanel(new InventoryView());
            }

            case WarehouseSidebar.MENU_PRODUCTS -> {
                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_PRODUCTS);
                showPanel(new ProductView());
            }

            case WarehouseSidebar.MENU_SUPPLIERS -> {
                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_SUPPLIERS);
                showPanel(new SupplierManagementView(SupplierManagementView.SupplierViewMode.WAREHOUSE));
            }

            case WarehouseSidebar.MENU_CATEGORY_TAX -> {
                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_CATEGORY_TAX);
                showPanel(new CategoryTaxView());
            }

            case WarehouseSidebar.MENU_SETTINGS -> {
                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_SETTINGS);
                showPanel(new view.components.UnifiedSettingsPanel());
            }

            case WarehouseSidebar.MENU_LOGOUT ->
                handleLogout();

            default -> {
            }
        }
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

        /*
     * Warehouse Portal:
     * - Nhân viên kho được thao tác đầy đủ ở các màn thuộc nghiệp vụ kho:
     *   + Quản lý tồn kho
     *   + Quản lý sản phẩm
     *   + Quản lý nhà cung cấp ở chế độ xem
     *   + Danh mục & Thuế VAT
     *
     * Không bọc UIPermissionGuard cho các màn này để tránh bị khóa nút Thêm/Sửa/Xóa.
         */
        boolean isWarehouseAllowedPanel
                = (panel instanceof InventoryView)
                || (panel instanceof ProductView)
                || (panel instanceof SupplierManagementView)
                || (panel instanceof CategoryTaxView)
                || (panel instanceof view.components.UnifiedSettingsPanel)
                || (panel instanceof view.components.TongQuanPanel);

        if (!isWarehouseAllowedPanel) {
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

        updateLogoutSession("[Warehouse CloseApp]");
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

        updateLogoutSession("[Warehouse Logout]");

        dispose();

        LoginView login = new LoginView();
        login.setLocationRelativeTo(null);
        login.setVisible(true);
    }

    private void updateLogoutSession(String logPrefix) {
        try {
            model.account.Account currentUser = business.service.SessionManager.getCurrentUser();
            String sessionId = business.service.SessionManager.getCurrentSessionId();

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
            System.err.println(logPrefix + " Không thể cập nhật session: " + ex.getMessage());
        }
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
