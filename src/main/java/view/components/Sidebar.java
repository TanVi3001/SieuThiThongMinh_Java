package view.components;

import business.service.AuthorizationService;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

public class Sidebar extends JPanel {

    private final List<MenuItem> menuItems;
    private final JPanel menuPanel;
    private final String userRole;

    public Sidebar(String userRole) {
        this.userRole = userRole;
        this.menuItems = new ArrayList<>();

        setLayout(new BorderLayout());

        // --- CHÌA KHÓA FIX LỖI Ở ĐÂY ---
        setPreferredSize(new Dimension(260, 0));
        setMinimumSize(new Dimension(260, 0));
        setMaximumSize(new Dimension(260, Integer.MAX_VALUE));
        // -------------------------------

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
        menuPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        // --- ĐÃ GẮN NÃO PHÂN QUYỀN VÀ ICON VÀO ĐÂY ---
        addMenuItem("Tổng quan", IconHelper.dashboard(20)); // Ai cũng thấy

        if (AuthorizationService.canAccessProductsAndInventory()) {
            addMenuItem("Quản lý sản phẩm", IconHelper.product(20));
        }

        if (AuthorizationService.canAccessStatisticsAndEmployees()) {
            addMenuItem("Quản lý nhân viên", IconHelper.employee(20));
        }

        if (AuthorizationService.canAccessPOS()) {
            addMenuItem("Khách hàng", IconHelper.customer(20));
        }

        if (AuthorizationService.canAccessInvoices()) {
            addMenuItem("Hóa đơn", IconHelper.bill(20));
        }

        if (AuthorizationService.canAccessStatisticsAndEmployees()) {
            addMenuItem("Báo cáo & Thống kê", IconHelper.barChart(20));
        }

        addMenuItem("Cài đặt", IconHelper.settings(20)); // Ai cũng thấy

        JScrollPane scrollPane = new JScrollPane(menuPanel);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        // 3) Bottom (Logout)
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(Color.WHITE);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // GẮN ICON ĐĂNG XUẤT
        MenuItem logoutItem = new MenuItem("Đăng xuất", IconHelper.logout(20), () -> {
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

    public interface MenuClickListener {

        void onMenuClick(String title);
    }

    private MenuClickListener listener;

    public void setMenuClickListener(MenuClickListener listener) {
        this.listener = listener;
    }

    // Đã cập nhật hàm này để nhận thêm tham số ImageIcon
    private void addMenuItem(final String title, ImageIcon icon) {
        final MenuItem[] itemHolder = new MenuItem[1];
        MenuItem item = new MenuItem(title, icon, () -> {
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

    // =========================================================
    // INNER CLASS: ĐỊNH NGHĨA LẠI THẺ MENU CÓ HỖ TRỢ ICON
    // =========================================================
    class MenuItem extends JPanel {

        private String title;
        private ImageIcon icon;
        private boolean isActive = false;
        private boolean isHovered = false;
        private boolean isFramed = false;

        private final Color COLOR_ACTIVE_BG = new Color(237, 242, 255);
        private final Color COLOR_ACTIVE_TEXT = new Color(43, 54, 116);
        private final Color COLOR_ACTIVE_LINE = new Color(67, 97, 238);

        private final Color COLOR_INACTIVE_BG = Color.WHITE;
        private final Color COLOR_INACTIVE_TEXT = new Color(112, 126, 174);
        private final Color COLOR_HOVER_BG = new Color(248, 249, 252);

        private final Color COLOR_LOGOUT_BG = new Color(220, 53, 69);
        private final Color COLOR_LOGOUT_HOVER = new Color(200, 35, 51);

        public MenuItem(String title, ImageIcon icon, Runnable onClickAction) {
            this.title = title;
            this.icon = icon;
            setLayout(new BorderLayout());

            setPreferredSize(new Dimension(260, 45));
            setMinimumSize(new Dimension(260, 45));
            setMaximumSize(new Dimension(260, 45));

            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onClickAction != null) {
                        onClickAction.run();
                    }
                }
            });
        }

        public void setActive(boolean active) {
            this.isActive = active;
            repaint();
        }

        public void setFramed(boolean framed) {
            this.isFramed = framed;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            if (isFramed) {
                // --- VẼ NÚT LOGOUT ĐỎ ---
                g2.setColor(isHovered ? COLOR_LOGOUT_HOVER : COLOR_LOGOUT_BG);
                g2.fillRoundRect(0, 0, w, h, 15, 15);

                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();

                int iconWidth = (icon != null) ? icon.getIconWidth() + 10 : 0;
                int totalWidth = iconWidth + fm.stringWidth(title);
                int startX = (w - totalWidth) / 2;

                if (icon != null) {
                    icon.paintIcon(this, g2, startX, (h - icon.getIconHeight()) / 2);
                    startX += iconWidth;
                }

                int textY = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(title, startX, textY);

            } else {
                // --- VẼ THẺ MENU BÌNH THƯỜNG ---
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

                int iconX = 25;
                int textX = 60;

                if (icon != null) {
                    icon.paintIcon(this, g2, iconX, (h - icon.getIconHeight()) / 2);
                }

                FontMetrics fm = g2.getFontMetrics();
                int textY = ((h - fm.getHeight()) / 2) + fm.getAscent();
                g2.drawString(title, textX, textY);
            }
            g2.dispose();
        }
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
