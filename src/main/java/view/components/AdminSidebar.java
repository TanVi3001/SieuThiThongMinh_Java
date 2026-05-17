package view.components;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class AdminSidebar extends JPanel {

    private static final int SIDEBAR_WIDTH = 310;
    private static final Color SIDEBAR_BG = ModernSidebarMenuItem.WHITE;
    private static final Color APP_BG = new Color(247, 249, 253);
    private static final Color NAVY = ModernSidebarMenuItem.NAVY;
    private static final Color TEXT_MUTED = new Color(95, 111, 143);
    private static final Color BORDER = ModernSidebarMenuItem.BORDER;
    private static final Color ORANGE = ModernSidebarMenuItem.PRIMARY_ORANGE;

    private final List<ModernSidebarMenuItem> menuItems;
    private final JPanel menuPanel;
    private MenuClickListener listener;

    public AdminSidebar() {
        this.menuItems = new ArrayList<>();

        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        setMinimumSize(new Dimension(SIDEBAR_WIDTH, 0));
        setMaximumSize(new Dimension(SIDEBAR_WIDTH, Integer.MAX_VALUE));
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 8));

        add(createBrandingPanel(), BorderLayout.NORTH);

        menuPanel = new JPanel();
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(18, 24, 12, 24));

        addMenuItem("Quản lý chi nhánh", IconHelper.dashboard(24));
        addMenuItem("Quản lý khuyến mãi", IconHelper.coupon(24));
        addMenuItem("Quản lý cửa hàng trưởng", IconHelper.employee(24));
        addMenuItem("Quản lý tài khoản", IconHelper.employee(24));
        addMenuItem("Quản lý phân quyền", IconHelper.customer(24));
        addMenuItem("Lịch sử truy cập", IconHelper.history(24));
        addMenuItem("Nhật ký hệ thống", IconHelper.barChart(24));
        addMenuItem("Cài đặt", IconHelper.settings(24));

        JScrollPane scrollPane = new JScrollPane(menuPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);

        if (!menuItems.isEmpty()) {
            menuItems.get(0).setActive(true);
        }
    }

    private JPanel createBrandingPanel() {
        JPanel brandingPanel = new JPanel(new BorderLayout(16, 0));
        brandingPanel.setOpaque(false);
        brandingPanel.setBorder(BorderFactory.createEmptyBorder(28, 28, 24, 24));

        JLabel logo = new JLabel(new CartLogoIcon(48));
        logo.setPreferredSize(new Dimension(52, 52));
        brandingPanel.add(logo, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel appName = new JLabel("Smart Supermarket");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 18));
        appName.setForeground(NAVY);
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Management System");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        textPanel.add(Box.createVerticalGlue());
        textPanel.add(appName);
        textPanel.add(Box.createRigidArea(new Dimension(0, 3)));
        textPanel.add(subtitle);
        textPanel.add(Box.createVerticalGlue());
        brandingPanel.add(textPanel, BorderLayout.CENTER);

        return brandingPanel;
    }

    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 24, 28, 24));

        SidebarWatermarkPanel watermark = new SidebarWatermarkPanel();
        watermark.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomPanel.add(watermark);
        bottomPanel.add(Box.createRigidArea(new Dimension(0, 12)));

        ModernSidebarMenuItem logoutItem = new ModernSidebarMenuItem("Đăng xuất", IconHelper.logout(24), () -> {
            if (listener != null) {
                listener.onMenuClick("Đăng xuất");
            }
        });
        logoutItem.setFramed(true);
        logoutItem.setAlignmentX(Component.LEFT_ALIGNMENT);
        bottomPanel.add(logoutItem);

        return bottomPanel;
    }

    private void addMenuItem(final String title, ImageIcon icon) {
        final ModernSidebarMenuItem[] itemHolder = new ModernSidebarMenuItem[1];
        ModernSidebarMenuItem item = new ModernSidebarMenuItem(title, icon, () -> {
            for (ModernSidebarMenuItem menuItem : menuItems) {
                menuItem.setActive(false);
            }
            itemHolder[0].setActive(true);
            if (listener != null) {
                listener.onMenuClick(title);
            }
        });
        itemHolder[0] = item;
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        menuItems.add(item);
        menuPanel.add(item);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 12)));
    }

    public void setMenuClickListener(MenuClickListener listener) {
        this.listener = listener;
    }

    public interface MenuClickListener {
        void onMenuClick(String title);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(APP_BG);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int x = 6;
        int y = 4;
        int w = getWidth() - 14;
        int h = getHeight() - 8;
        for (int i = 5; i >= 1; i--) {
            g2.setColor(new Color(23, 52, 99, 4 + i));
            g2.fillRoundRect(x + i, y + i, w - i * 2, h - i * 2, 24, 24);
        }
        g2.setColor(SIDEBAR_BG);
        g2.fillRoundRect(x, y, w, h, 24, 24);
        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w - 1, h - 1, 24, 24);
        g2.dispose();
        super.paintComponent(g);
    }

    private static class CartLogoIcon implements javax.swing.Icon {
        private final int size;

        CartLogoIcon(int size) {
            this.size = size;
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 106, 0, 24));
            g2.fillRoundRect(x + 3, y + 3, size - 6, size - 6, 16, 16);
            g2.setColor(ORANGE);
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int bx = x + 11;
            int by = y + 16;
            g2.drawLine(bx, by, bx + 5, by + 20);
            g2.drawLine(bx + 5, by + 20, bx + 26, by + 20);
            g2.drawLine(bx + 9, by + 6, bx + 30, by + 6);
            g2.drawLine(bx + 30, by + 6, bx + 25, by + 19);
            g2.drawLine(bx + 11, by + 10, bx + 24, by + 10);
            g2.drawLine(bx + 13, by + 15, bx + 22, by + 15);
            g2.fillOval(bx + 7, by + 25, 5, 5);
            g2.fillOval(bx + 24, by + 25, 5, 5);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
            FontMetrics fm = g2.getFontMetrics();
            String mark = "K";
            g2.drawString(mark, bx + 18 - fm.stringWidth(mark) / 2, by + 17);
            g2.dispose();
        }
    }

    private static class SidebarWatermarkPanel extends JPanel {
        SidebarWatermarkPanel() {
            setOpaque(false);
            setPreferredSize(new Dimension(260, 124));
            setMinimumSize(new Dimension(220, 100));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 124));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setComposite(AlphaComposite.SrcOver.derive(0.13f));
            int baseY = getHeight() - 18;
            g2.setColor(new Color(95, 111, 143));
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(154, baseY, 188, baseY);
            g2.drawRoundRect(88, baseY - 34, 74, 34, 8, 8);
            g2.drawLine(80, baseY - 48, 92, baseY - 34);
            g2.drawLine(100, baseY - 26, 154, baseY - 26);
            g2.drawLine(104, baseY - 17, 150, baseY - 17);
            g2.fillOval(102, baseY + 4, 7, 7);
            g2.fillOval(145, baseY + 4, 7, 7);

            g2.setColor(ORANGE);
            g2.fillOval(110, baseY - 29, 12, 12);
            g2.fillOval(128, baseY - 31, 14, 14);
            g2.fillOval(144, baseY - 27, 11, 11);

            g2.setColor(new Color(95, 111, 143));
            g2.drawLine(200, baseY, 200, baseY - 58);
            g2.drawLine(200, baseY - 42, 184, baseY - 56);
            g2.drawLine(200, baseY - 34, 218, baseY - 48);
            g2.drawLine(200, baseY - 22, 184, baseY - 34);
            g2.drawOval(176, baseY - 64, 18, 10);
            g2.drawOval(212, baseY - 56, 18, 10);
            g2.dispose();
        }
    }
}
