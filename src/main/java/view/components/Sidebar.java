package view.components;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Sidebar extends JPanel {

    private final List<MenuItem> menuItems;
    private final JPanel menuPanel;
    private final String userRole;

    // Chỉ dùng constructor này, bắt buộc truyền role
    public Sidebar(String userRole) {
        this.userRole = normalizeRole(userRole);
        this.menuItems = new ArrayList<>();

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(250, Integer.MAX_VALUE));
        setBackground(Color.WHITE);
        setBorder(new MatteBorder(0, 0, 0, 1, new Color(230, 230, 230)));

        // 1) Branding (Top)
        JPanel brandingPanel = new JPanel();
        brandingPanel.setLayout(new BoxLayout(brandingPanel, BoxLayout.Y_AXIS));
        brandingPanel.setBackground(Color.WHITE);
        brandingPanel.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));

        JLabel appName = new JLabel("Smart Supermarket");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Management System");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(120, 120, 120));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandingPanel.add(appName);
        brandingPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        brandingPanel.add(subtitle);

        // 2) Menu (Center)
        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(Color.WHITE);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        addMenuItem("Tổng quan");
        addMenuItem("Quản lý sản phẩm");

        // staff / nhân viên -> KHÔNG hiện menu quản lý nhân viên và thống kê
        if (!isStaff()) {
            addMenuItem("Quản lý nhân viên");
        }

        addMenuItem("Khách hàng");
        addMenuItem("Hóa đơn");
        
        // Chỉ hiển thị menu Thống kê cho Admin/Manager
        if (!isStaff()) {
            addMenuItem("Thống kê");
        }
        
        addMenuItem("Cài đặt");

        JScrollPane scrollPane = new JScrollPane(menuPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // 3) Bottom (Logout)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 20, 10));

        MenuItem logoutItem = new MenuItem("Đăng xuất", () -> {
            if (listener != null) {
                listener.onMenuClick("Đăng xuất");
            }
        });
        logoutItem.setFramed(true);
        bottomPanel.add(logoutItem, BorderLayout.CENTER);

        add(brandingPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        if (!menuItems.isEmpty()) {
            menuItems.get(0).setActive(true);
        }
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "";
        }
        String s = role.trim().toLowerCase(Locale.ROOT);
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", ""); // bỏ dấu
        s = s.replaceAll("\\s+", ""); // bỏ khoảng trắng
        return s;
    }

    private boolean isStaff() {
        return userRole.equals("staff")
                || userRole.equals("nhanvien")
                || userRole.equals("employee");
    }

    public interface MenuClickListener {

        void onMenuClick(String title);
    }

    private MenuClickListener listener;

    public void setMenuClickListener(MenuClickListener listener) {
        this.listener = listener;
    }

    private void addMenuItem(final String title) {
        final MenuItem[] itemHolder = new MenuItem[1];
        MenuItem item = new MenuItem(title, () -> {
            for (MenuItem m : menuItems) {
                m.setActive(false);
            }
            itemHolder[0].setActive(true);
            if (listener != null) {
                listener.onMenuClick(title);
            }
        });
        itemHolder[0] = item;
        menuItems.add(item);
        menuPanel.add(item);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 8)));
    }

    // test
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("Failed to initialize LaF");
        }

        JFrame frame = new JFrame("Sidebar UI Test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        frame.add(new Sidebar("staff"), BorderLayout.WEST);

        JPanel mainContent = new JPanel(new BorderLayout());
        mainContent.setBackground(new Color(245, 245, 247));
        JLabel lblContent = new JLabel("Main Content Area", SwingConstants.CENTER);
        lblContent.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        lblContent.setForeground(Color.GRAY);
        mainContent.add(lblContent, BorderLayout.CENTER);

        frame.add(mainContent, BorderLayout.CENTER);
        frame.setSize(1000, 700);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
