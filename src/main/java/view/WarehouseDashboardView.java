package view;

import javax.swing.*;
import java.awt.*;
import view.components.WarehouseSidebar;

public class WarehouseDashboardView extends JFrame {
    
    private JPanel mainContentPanel;
    private WarehouseSidebar warehouseSidebar;
    private final Color bgWarehouse = new Color(244, 246, 250); 
    
    public WarehouseDashboardView() {
        setupWarehouseUI();
    }

    private void setupWarehouseUI() {
        model.account.Account currentUser = business.service.LoginService.getCurrentUser();
        String username = (currentUser != null && currentUser.getUsername() != null) 
                          ? currentUser.getUsername() : "Nhân viên Kho";

        this.setTitle("SMART SUPERMARKET - WAREHOUSE PORTAL | " + username);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // BẢO VỆ LỚP 1: Luôn mở Full màn hình và khóa mốc thu nhỏ tối thiểu
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setMinimumSize(new Dimension(1100, 700));
        this.setLocationRelativeTo(null);
        
        this.getContentPane().setLayout(new BorderLayout());
        
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
                    // showPanel(new view.SupplierView());
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
            }
        });

        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(bgWarehouse);

        this.getContentPane().add(warehouseSidebar, BorderLayout.WEST); 
        this.getContentPane().add(mainContentPanel, BorderLayout.CENTER); 
            
        showPanel(new view.InventoryView());
    }

    public void showPanel(JPanel panel) {
        mainContentPanel.removeAll();
        
        JPanel panelToDisplay = panel;

        // ========================================================
        // LOGIC MIỄN TRỪ (BYPASS): 
        // Bỏ qua Lính gác đối với các trang Tổng quan và Cài đặt cá nhân
        // ========================================================
        boolean isBypassed = (panel instanceof view.components.TongQuanPanel) || 
                     (panel instanceof view.components.UnifiedSettingsPanel);

        if (!isBypassed) {
            // Nếu không thuộc diện miễn trừ -> Đưa cho Lính gác kiểm tra và khóa nút
            panelToDisplay = common.security.UIPermissionGuard.protect(panel);
        }

        // BẢO VỆ LỚP 2: Bọc thẻ con vào Thanh cuộn để chống ép bẹp
        panelToDisplay.setMinimumSize(new Dimension(900, 600)); 
        
        JScrollPane scrollPane = new JScrollPane(panelToDisplay);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); 
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        mainContentPanel.add(scrollPane, BorderLayout.CENTER);
        mainContentPanel.revalidate();
        mainContentPanel.repaint();
    }

    private void handleLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có muốn đăng xuất khỏi Cổng Kho Hàng?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            business.service.LoginService.logout();
            new LoginView().setVisible(true);
            this.dispose();
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