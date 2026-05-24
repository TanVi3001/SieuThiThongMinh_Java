package view.components;

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

public class WarehouseSidebar extends JPanel {

    public static final String MENU_INVENTORY = "Quản lý tồn kho";
    public static final String MENU_PRODUCTS = "Quản lý sản phẩm";
    public static final String MENU_SUPPLIERS = "Quản lý nhà cung cấp";
    public static final String MENU_CATEGORY_TAX = "Danh mục & Thuế VAT";
    public static final String MENU_SETTINGS = "Cài đặt";
    public static final String MENU_LOGOUT = "Đăng xuất";

    private static final int SIDEBAR_WIDTH = 280;

    private static final Color SIDEBAR_BG = ModernSidebarMenuItem.WHITE;
    private static final Color APP_BG = ModernSidebarMenuItem.APP_BG;
    private static final Color NAVY = ModernSidebarMenuItem.NAVY;
    private static final Color TEXT_MUTED = ModernSidebarMenuItem.MUTED;
    private static final Color BORDER = ModernSidebarMenuItem.BORDER;
    private static final Color ORANGE = ModernSidebarMenuItem.ORANGE;

    private final List<MenuEntry> menuEntries = new ArrayList<>();

    private JPanel menuPanel;
    private MenuClickListener listener;
    private String activeMenuTitle = MENU_INVENTORY;

    public WarehouseSidebar() {
        initLayout();
        initMenu();
        setActiveMenu(MENU_INVENTORY);
    }

    private void initLayout() {
        setLayout(new BorderLayout());
        setOpaque(false);
        setPreferredSize(new Dimension(SIDEBAR_WIDTH, 0));
        setMinimumSize(new Dimension(SIDEBAR_WIDTH, 0));
        setMaximumSize(new Dimension(SIDEBAR_WIDTH, Integer.MAX_VALUE));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 8));

        add(createBrandingPanel(), BorderLayout.NORTH);

        menuPanel = new JPanel();
        menuPanel.setOpaque(false);
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(8, 24, 12, 24));

        JScrollPane scrollPane = new JScrollPane(menuPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
        add(createBottomPanel(), BorderLayout.SOUTH);
    }

    private void initMenu() {
        addMenuItem(MENU_INVENTORY, getIconByTitle(MENU_INVENTORY));
        addMenuItem(MENU_PRODUCTS, getIconByTitle(MENU_PRODUCTS));
        addMenuItem(MENU_SUPPLIERS, getIconByTitle(MENU_SUPPLIERS));
        addMenuItem(MENU_CATEGORY_TAX, getIconByTitle(MENU_CATEGORY_TAX));
        addMenuItem(MENU_SETTINGS, getIconByTitle(MENU_SETTINGS));
    }

    private JPanel createBrandingPanel() {
        JPanel brandingPanel = new JPanel(new BorderLayout(16, 0));
        brandingPanel.setOpaque(false);
        brandingPanel.setBorder(BorderFactory.createEmptyBorder(32, 24, 18, 24));

        JLabel logo = new JLabel(new CartLogoIcon(42));
        logo.setPreferredSize(new Dimension(44, 44));
        brandingPanel.add(logo, BorderLayout.WEST);

        JPanel textPanel = new JPanel();
        textPanel.setOpaque(false);
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));

        JLabel appName = new JLabel("Smart Supermarket");
        appName.setFont(new Font("Segoe UI", Font.BOLD, 17));
        appName.setForeground(NAVY);
        appName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Warehouse Portal");
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
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(8, 24, 28, 24));
        bottomPanel.add(Box.createVerticalGlue());

        ModernSidebarMenuItem logoutItem = new ModernSidebarMenuItem(
                MENU_LOGOUT,
                IconHelper.logout(24),
                () -> {
                    if (listener != null) {
                        listener.onMenuClick(MENU_LOGOUT);
                    }
                }
        );

        logoutItem.setFramed(true);
        logoutItem.setAlignmentX(Component.LEFT_ALIGNMENT);

        bottomPanel.add(logoutItem);

        return bottomPanel;
    }

    private void addMenuItem(final String title, ImageIcon icon) {
        ModernSidebarMenuItem item = new ModernSidebarMenuItem(
                title,
                icon,
                () -> handleMenuClick(title)
        );

        item.setAlignmentX(Component.LEFT_ALIGNMENT);

        menuEntries.add(new MenuEntry(title, item));
        menuPanel.add(item);
        menuPanel.add(Box.createRigidArea(new Dimension(0, 9)));
    }

    private void handleMenuClick(String title) {
        setActiveMenu(title);

        if (listener != null) {
            listener.onMenuClick(title);
        }
    }

    public void setActiveMenu(String title) {
        if (title == null || title.trim().isEmpty()) {
            return;
        }

        activeMenuTitle = title;

        for (MenuEntry entry : menuEntries) {
            entry.item.setActive(title.equals(entry.title));
        }

        revalidate();
        repaint();
    }

    public String getActiveMenuTitle() {
        return activeMenuTitle;
    }

    private ImageIcon getIconByTitle(String title) {
        String key = title == null ? "" : title.toLowerCase();

        if (key.contains("tồn kho")) {
            return IconHelper.stock(24);
        }

        if (key.contains("sản phẩm")) {
            return IconHelper.product(24);
        }

        if (key.contains("nhà cung cấp")) {
            return IconHelper.delivery(24);
        }

        if (key.contains("danh mục") || key.contains("thuế")) {
            return IconHelper.coupon(24);
        }

        if (key.contains("cài")) {
            return IconHelper.settings(24);
        }

        return IconHelper.product(24);
    }

    public void setMenuClickListener(MenuClickListener listener) {
        this.listener = listener;
    }

    public interface MenuClickListener {

        void onMenuClick(String title);
    }

    private static class MenuEntry {

        private final String title;
        private final ModernSidebarMenuItem item;

        private MenuEntry(String title, ModernSidebarMenuItem item) {
            this.title = title;
            this.item = item;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(APP_BG);
        g2.fillRect(0, 0, getWidth(), getHeight());

        int x = 5;
        int y = 6;
        int w = getWidth() - 13;
        int h = getHeight() - 12;

        for (int i = 6; i >= 1; i--) {
            g2.setColor(new Color(23, 52, 99, 3 + i));
            g2.fillRoundRect(
                    x + i,
                    y + i,
                    w - i * 2,
                    h - i * 2,
                    24,
                    24
            );
        }

        g2.setColor(SIDEBAR_BG);
        g2.fillRoundRect(x, y, w, h, 24, 24);

        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(x, y, w - 1, h - 1, 20, 20);

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

            g2.setColor(new Color(255, 106, 0, 18));
            g2.fillRoundRect(x + 2, y + 2, size - 4, size - 4, 14, 14);

            g2.setColor(ORANGE);
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int bx = x + 9;
            int by = y + 14;

            g2.drawLine(bx, by, bx + 5, by + 20);
            g2.drawLine(bx + 5, by + 20, bx + 26, by + 20);
            g2.drawLine(bx + 9, by + 6, bx + 30, by + 6);
            g2.drawLine(bx + 30, by + 6, bx + 25, by + 19);
            g2.drawLine(bx + 11, by + 10, bx + 24, by + 10);
            g2.drawLine(bx + 13, by + 15, bx + 22, by + 15);

            g2.fillOval(bx + 7, by + 25, 5, 5);
            g2.fillOval(bx + 24, by + 25, 5, 5);

            g2.setFont(new Font("Segoe UI", Font.BOLD, 9));

            FontMetrics fm = g2.getFontMetrics();
            String mark = "S";

            g2.drawString(
                    mark,
                    bx + 18 - fm.stringWidth(mark) / 2,
                    by + 17
            );

            g2.dispose();
        }
    }
}
