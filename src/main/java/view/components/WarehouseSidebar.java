package view.components;

import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class WarehouseSidebar extends JPanel {
    
    private final List<MenuItem> menuItems;
    private final JPanel menuPanel;
    private MenuClickListener listener;

    public WarehouseSidebar() {
        this.menuItems = new ArrayList<>();

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(260, 0));
        setMinimumSize(new Dimension(260, 0));
        setMaximumSize(new Dimension(260, Integer.MAX_VALUE));
        
        setBackground(Color.WHITE); 
        setBorder(new MatteBorder(0, 0, 0, 1, new Color(230, 230, 230))); 

        // 1. Phần tiêu đề (Branding)
        JPanel brandingPanel = new JPanel();
        brandingPanel.setLayout(new BoxLayout(brandingPanel, BoxLayout.Y_AXIS));
        brandingPanel.setBackground(Color.WHITE);
        brandingPanel.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));

        JLabel appName = new JLabel("Cổng Kho Hàng");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        appName.setForeground(new Color(43, 54, 116));
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Quản lý Sản phẩm & Tồn kho");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(163, 174, 208)); 
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandingPanel.add(appName);
        brandingPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        brandingPanel.add(subtitle);

        // 2. Panel chứa các mục Menu
        menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBackground(Color.WHITE);
        menuPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // THÊM CÁC MỤC MENU CHUẨN KHO HÀNG
        addMenuItem("Quản lý tồn kho"); 
        addMenuItem("Quản lý sản phẩm");
        addMenuItem("Nhà cung cấp");
        addMenuItem("Danh mục & Thuế VAT");
        addMenuItem("Cài đặt");

        JScrollPane scrollPane = new JScrollPane(menuPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // 3. Phần Logout
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

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
        menuPanel.add(Box.createRigidArea(new Dimension(0, 5))); 
    }

    public void setMenuClickListener(MenuClickListener listener) {
        this.listener = listener;
    }

    public interface MenuClickListener {
        void onMenuClick(String title);
    }

    // =========================================================
    // INNER CLASS: ĐỊNH NGHĨA LẠI THẺ MENU (GIỐNG ADMIN SIDEBAR)
    // =========================================================
    class MenuItem extends JPanel {
        private String title;
        private boolean isActive = false;
        private boolean isHovered = false;
        private boolean isFramed = false; 

        // Màu mảng xanh lá/teal đặc trưng cho kho hàng
        private final Color COLOR_ACTIVE_BG = new Color(230, 248, 241);   
        private final Color COLOR_ACTIVE_TEXT = new Color(0, 163, 108);   
        private final Color COLOR_ACTIVE_LINE = new Color(0, 201, 135);   
        
        private final Color COLOR_INACTIVE_BG = Color.WHITE;
        private final Color COLOR_INACTIVE_TEXT = new Color(112, 126, 174); 
        private final Color COLOR_HOVER_BG = new Color(248, 249, 252);
        
        private final Color COLOR_LOGOUT_BG = new Color(220, 53, 69);
        private final Color COLOR_LOGOUT_HOVER = new Color(200, 35, 51);

        public MenuItem(String title, Runnable onClickAction) {
            this.title = title;
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(260, 45));
            setMinimumSize(new Dimension(260, 45));
            setMaximumSize(new Dimension(260, 45));
            setOpaque(false); 
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
                @Override public void mouseClicked(MouseEvent e) { if (onClickAction != null) onClickAction.run(); }
            });
        }

        public void setActive(boolean active) { this.isActive = active; repaint(); }
        public void setFramed(boolean framed) { this.isFramed = framed; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (isFramed) {
                g2.setColor(isHovered ? COLOR_LOGOUT_HOVER : COLOR_LOGOUT_BG);
                g2.fillRoundRect(0, 0, w, h, 15, 15);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                int textX = (w - fm.stringWidth(title)) / 2; 
                int textY = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(title, textX, textY);
            } else {
                if (isActive) {
                    g2.setColor(COLOR_ACTIVE_BG);
                    g2.fillRect(0, 0, w, h);
                    g2.setColor(COLOR_ACTIVE_LINE);
                    g2.fillRoundRect(0, 5, 4, h - 10, 4, 4); 
                    g2.setColor(COLOR_ACTIVE_TEXT);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else {    
                    g2.setColor(isHovered ? COLOR_HOVER_BG : COLOR_INACTIVE_BG);
                    g2.fillRect(0, 0, w, h);
                    g2.setColor(COLOR_INACTIVE_TEXT);
                    g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                }
                FontMetrics fm = g2.getFontMetrics();
                int textY = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(title, 35, textY);
            }
            g2.dispose();
        }
    }
}