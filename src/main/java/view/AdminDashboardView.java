package view;

import javax.swing.*;
import java.awt.*;
import view.components.AdminSidebar;

public class AdminDashboardView extends javax.swing.JFrame {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AdminDashboardView.class.getName());
    
    private JPanel mainContentPanel;
    private AdminSidebar adminSidebar;
    private Color bgAdmin = new Color(240, 242, 245); 

    public AdminDashboardView() {
        initComponents();
        setupAdminUI();
        
        // Mặc định khi vào Admin là quản lý cửa hàng trưởng
        showPanel(new view.ManagerManagementView()); 
    }

    private void setupAdminUI() {
        this.setTitle("SMART SUPERMARKET - CENTRAL ADMIN PORTAL");
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        // BẢO VỆ LỚP 1: Luôn mở Full màn hình và khóa mốc thu nhỏ tối thiểu
        this.setExtendedState(MAXIMIZED_BOTH);
        this.setMinimumSize(new Dimension(1100, 700));
        this.setLocationRelativeTo(null);
        
        // Khởi tạo Sidebar Admin
        adminSidebar = new AdminSidebar();
        
        // NỐI CÁC MỤC MENU VỚI PANEL TƯƠNG ỨNG
        adminSidebar.setMenuClickListener(title -> {
            switch (title) {
                case "Quản lý chi nhánh":
                    showPanel(new view.StoreManagementPanel()); 
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
                case "Nhật ký hệ thống":
                    showPanel(new AuditLogPanel()); 
                    break;
                case "Cài đặt":
                    showPanel(new SettingsAdminPanel()); 
                    break;
                case "Đăng xuất":
                    handleLogout();
                    break;
            }
        });

        // Thiết lập Layout chính
        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(adminSidebar, BorderLayout.WEST);
        
        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(bgAdmin);
        this.getContentPane().add(mainContentPanel, BorderLayout.CENTER);
    }

    public void showPanel(JPanel panel) {
        mainContentPanel.removeAll();
        
        JPanel panelToDisplay = panel;

        // ========================================================
        // LOGIC MIỄN TRỪ (BYPASS): 
        // Bỏ qua Lính gác đối với các trang Tổng quan và Cài đặt cá nhân
        // ========================================================
        boolean isBypassed = (panel instanceof view.components.TongQuanPanel) || 
                             (panel instanceof view.SettingsView) ||
                             (panel instanceof view.SettingsWarehouseView) ||
                             (panel instanceof view.SettingsAdminPanel);

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
        int confirm = JOptionPane.showConfirmDialog(this, "Đăng xuất khỏi hệ thống quản trị?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            business.service.LoginService.logout();
            new LoginView().setVisible(true);
            this.dispose();
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
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
}