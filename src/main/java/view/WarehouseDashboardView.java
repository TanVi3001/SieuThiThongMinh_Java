package view;

import business.service.AccountService;
import business.service.AuthorizationService;
import business.service.HeartbeatService;
import business.service.LoginService;
import business.service.SessionManager;
import common.auth.UserSession;
import common.security.UIPermissionGuard;
import common.security.SecurityGuard;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

    private volatile SwingWorker<JPanel, Void> currentPanelWorker;
    private final Map<String, JPanel> panelCache = new HashMap<>();
    private String currentMenu = WarehouseSidebar.MENU_INVENTORY;

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
        SecurityGuard.attach(mainContentPanel);
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
        if (title != null && title.equals(currentMenu) && mainContentPanel.getComponentCount() > 0) {
            return;
        }

        currentMenu = title;

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
                showMenuPanel(WarehouseSidebar.MENU_PRODUCTS, ProductView::new);
            }

            case WarehouseSidebar.MENU_SUPPLIERS -> {
                if (!AuthorizationService.canAccessSupplierAndCategory()) {
                    showAccessDenied();
                    return;
                }
                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_SUPPLIERS);
                showMenuPanel(WarehouseSidebar.MENU_SUPPLIERS,
                        () -> new SupplierManagementView(SupplierManagementView.SupplierViewMode.WAREHOUSE));
            }

            case WarehouseSidebar.MENU_CATEGORY_TAX -> {
                if (!AuthorizationService.canAccessSupplierAndCategory()) {
                    showAccessDenied();
                    return;
                }

                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_CATEGORY_TAX);
                showMenuPanel(WarehouseSidebar.MENU_CATEGORY_TAX, CategoryTaxView::new);
            }

            case WarehouseSidebar.MENU_SETTINGS -> {
                warehouseSidebar.setActiveMenu(WarehouseSidebar.MENU_SETTINGS);
                showMenuPanel(WarehouseSidebar.MENU_SETTINGS, UnifiedSettingsPanel::new);
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
            currentMenu = WarehouseSidebar.MENU_INVENTORY;
            showInventoryPanel(productId.trim());
        });

        topBar.add(notificationBell);

        return topBar;
    }

    private void showInventoryPanel(String focusProductId) {
        String cacheKey = focusProductId == null || focusProductId.trim().isEmpty()
                ? WarehouseSidebar.MENU_INVENTORY
                : WarehouseSidebar.MENU_INVENTORY + ":" + focusProductId.trim();

        showMenuPanel(cacheKey, () -> {
            InventoryView inventoryView = new InventoryView();

            if (focusProductId != null && !focusProductId.trim().isEmpty()) {
                SwingUtilities.invokeLater(() -> inventoryView.focusProduct(focusProductId.trim()));
            }

            return inventoryView;
        });
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
                    showPanel(panel);
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

    public void showPanel(JPanel panel) {
        showPanel(panel, true);
    }

    private void showPanel(JPanel panel, boolean applyGuard) {
        if (panel == null) {
            return;
        }

        mainContentPanel.removeAll();

        JPanel panelToDisplay = panel;

        boolean isWarehouseAllowedPanel
                = (panel instanceof InventoryView)
                || (panel instanceof ProductView)
                || (panel instanceof SupplierManagementView)
                || (panel instanceof CategoryTaxView)
                || (panel instanceof UnifiedSettingsPanel)
                || (panel instanceof TongQuanPanel);

        if (applyGuard && !isWarehouseAllowedPanel) {
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
