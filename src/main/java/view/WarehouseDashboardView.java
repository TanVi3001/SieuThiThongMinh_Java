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
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setMinimumSize(new Dimension(1024, 768));
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // --- CHÌA KHÓA Ở ĐÂY: LAYOUT GIỐNG HỆT ADMIN ---
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
                    // MỞ FORM DANH MỤC VÀ THUẾ
                    showPanel(new CategoryTaxView()); 
                    break;
                case "Cài đặt":
                    // MỞ FORM CÀI ĐẶT KHO HÀNG
                    showPanel(new SettingsWarehouseView()); 
                    break;
                case "Đăng xuất":
                    handleLogout();
                    break;
            }
        });

        mainContentPanel = new JPanel(new BorderLayout());
        mainContentPanel.setBackground(bgWarehouse);

        // Sidebar vuốt thẳng từ trên xuống dưới ở bên TRÁI
        this.getContentPane().add(warehouseSidebar, BorderLayout.WEST); 
        // Content lấp đầy phần GIỮA
        this.getContentPane().add(mainContentPanel, BorderLayout.CENTER); 
            
        showPanel(new view.InventoryView());
    }

    public void showPanel(JPanel panel) {
        mainContentPanel.removeAll();
        mainContentPanel.add(panel, BorderLayout.CENTER);
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