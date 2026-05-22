package view;

import business.service.AccountService;
import business.service.AuthorizationService;
import business.service.HeartbeatService;
import business.service.LoginService;
import business.service.SessionManager;
import common.auth.UserSession;
import common.security.UIPermissionGuard;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import model.account.Account;
import view.components.NotificationBell;
import view.components.TongQuanPanel;
import view.components.UnifiedSettingsPanel;
import view.components.WarehouseSidebar;

public class WarehouseDashboardView extends JFrame {

    private JPanel mainContentPanel;
    private WarehouseSidebar warehouseSidebar;
    private NotificationBell notificationBell;

    private final Color BACKGROUND_COLOR = new Color(246, 247, 251);
    private final Color TOPBAR_BORDER = new Color(230, 230, 230);

    public WarehouseDashboardView() {
        if (!AuthorizationService.isAdmin() && !AuthorizationService.isProductStaff()) {
            JOptionPane.showMessageDialog(
                    null,
                    "Bạn không có quyền truy cập Warehouse Portal.",
                    "Không có quyền",
                    JOptionPane.WARNING_MESSAGE
            );
            dispose();
            return;
        }

        setupWarehouseUI();
    }

    private void setupWarehouseUI() {
        Account currentUser = LoginService.getCurrentUser();

        String username = (currentUser != null && currentUser.getUsername() != null)
                ? currentUser.getUsername().trim()
                : "Nhân viên Kho";

        String portalTitle = AuthorizationService.currentPortalTitle();
        if (portalTitle == null || portalTitle.trim().isEmpty()
                || "SMART SUPERMARKET".equalsIgnoreCase(portalTitle.trim())) {
            portalTitle = "SMART SUPERMARKET - WAREHOUSE PORTAL";
        }

        setTitle(portalTitle + " | " + username);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
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
        showInventoryPanel(null);
    }

    private void handleSidebarMenuClick(String title) {
        switch (title) {
            case WarehouseSidebar.MENU_INVENTORY -> {
                if (!AuthorizationService.canManageStock()) {
                    showAccessDenied();
                    return;
                }
                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_INVENTORY);
                showInventoryPanel(null);
            }

            case WarehouseSidebar.MENU_PRODUCTS -> {
                if (!AuthorizationService.canAccessProductsAndInventory()) {
                    showAccessDenied();
                    return;
                }
                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_PRODUCTS);
                showPanel(new ProductView());
            }

            case WarehouseSidebar.MENU_SUPPLIERS -> {
                if (!AuthorizationService.canAccessSupplierAndCategory()) {
                    showAccessDenied();
                    return;
                }
                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_SUPPLIERS);
                showPanel(new SupplierManagementView(SupplierManagementView.SupplierViewMode.WAREHOUSE));
            }

            case WarehouseSidebar.MENU_CATEGORY_TAX -> {
                if (!AuthorizationService.canAccessSupplierAndCategory()) {
                    showAccessDenied();
                    return;
                }
                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_CATEGORY_TAX);
                showPanel(new CategoryTaxView());
            }

            case WarehouseSidebar.MENU_SETTINGS -> {
                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_SETTINGS);
                showPanel(new UnifiedSettingsPanel());
            }

            case WarehouseSidebar.MENU_LOGOUT ->
                handleLogout();

            default -> {
            }
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

        notificationBell = new NotificationBell(NotificationBell.Audience.WAREHOUSE);

        /*
         * Khi nhân viên kho bấm vào thông báo tồn kho:
         * 1. NotificationBell tự đóng popup.
         * 2. Dashboard chuyển sang menu Quản lý tồn kho.
         * 3. Mở InventoryView mới.
         * 4. Focus đúng productId trong bảng tồn kho.
         *
         * Điều kiện:
         * - NotificationBell phải có setProductClickListener(...).
         * - InventoryView phải có public void focusProduct(String productId).
         */
        notificationBell.setProductClickListener(productId -> {
            if (productId == null || productId.trim().isEmpty()) {
                JOptionPane.showMessageDialog(
                        this,
                        "Không tìm thấy mã sản phẩm trong thông báo.",
                        "Không thể mở sản phẩm",
                        JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_INVENTORY);
            showInventoryPanel(productId.trim());
        });

        topBar.add(notificationBell);

        return topBar;
    }

    private void showInventoryPanel(String focusProductId) {
        InventoryView inventoryView = new InventoryView();
        showPanel(inventoryView);

        if (focusProductId != null && !focusProductId.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> inventoryView.focusProduct(focusProductId.trim()));
        }
    }

    public void showPanel(JPanel panel) {
        if (panel == null) {
            return;
        }

        mainContentPanel.removeAll();

        JPanel panelToDisplay = panel;

        /*
         * Warehouse Portal:
         * - Nhân viên kho được thao tác đầy đủ ở các màn nghiệp vụ kho.
         * - Không bọc UIPermissionGuard cho các màn này để tránh khóa nút Thêm/Sửa/Xóa/Nhập kho.
         */
        boolean isWarehouseAllowedPanel
                = (panel instanceof InventoryView)
                || (panel instanceof ProductView)
                || (panel instanceof SupplierManagementView)
                || (panel instanceof CategoryTaxView)
                || (panel instanceof UnifiedSettingsPanel)
                || (panel instanceof TongQuanPanel);

        if (!isWarehouseAllowedPanel) {
            panelToDisplay = UIPermissionGuard.protect(panel);
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
            Account currentUser = SessionManager.getCurrentUser();
            String sessionId = SessionManager.getCurrentSessionId();

            if (currentUser != null
                    && currentUser.getAccountId() != null
                    && sessionId != null) {

                if (HeartbeatService.markLogoutOnce()) {
                    HeartbeatService.stop();

                    AccountService.onLogoutOrCloseApp(
                            currentUser.getAccountId(),
                            sessionId
                    );
                }
            }

            SessionManager.clear();

            try {
                UserSession.getInstance().clear();
            } catch (Exception ignored) {
            }

        } catch (Exception ex) {
            System.err.println(logPrefix + " Không thể cập nhật session: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            com.formdev.flatlaf.FlatLightLaf.setup();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        java.awt.EventQueue.invokeLater(() -> new WarehouseDashboardView().setVisible(true));
    }
}
