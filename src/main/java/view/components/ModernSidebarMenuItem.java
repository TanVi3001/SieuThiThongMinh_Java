package view.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class ModernSidebarMenuItem extends JPanel {

    public static final Color ORANGE = new Color(255, 106, 0);
    public static final Color ORANGE_2 = new Color(255, 122, 26);
    public static final Color ORANGE_LIGHT = new Color(255, 243, 234);
    public static final Color NAVY = new Color(23, 52, 99);
    public static final Color TEXT_NAVY = new Color(37, 59, 102);
    public static final Color MUTED_ICON = new Color(82, 107, 149);
    public static final Color MUTED = new Color(111, 124, 149);
    public static final Color BORDER = new Color(232, 237, 245);
    public static final Color APP_BG = new Color(246, 247, 251);
    public static final Color WHITE = Color.WHITE;
    private static final Color LOGOUT_RED = new Color(255, 77, 61);
    private static final Color LOGOUT_HOVER_BORDER = new Color(255, 216, 194);

    public static final Color PRIMARY_ORANGE = ORANGE;
    public static final Color PRIMARY_ORANGE_DARK = new Color(238, 86, 0);
    public static final Color PRIMARY_ORANGE_LIGHT = ORANGE_LIGHT;

    private static final int ARC = 14;
    private static final int ICON_SIZE = 22;
    private static final int ITEM_HEIGHT = 54;

    private final String title;
    @SuppressWarnings("unused")
    private final ImageIcon icon;
    private final Runnable onClickAction;
    private boolean active;
    private boolean hovered;
    private boolean logoutStyle;

    public ModernSidebarMenuItem(String title, ImageIcon icon, Runnable onClickAction) {
        this.title = title;
        this.icon = icon;
        this.onClickAction = onClickAction;

        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(232, ITEM_HEIGHT));
        setMinimumSize(new Dimension(200, ITEM_HEIGHT));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, ITEM_HEIGHT));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (ModernSidebarMenuItem.this.onClickAction != null) {
                    ModernSidebarMenuItem.this.onClickAction.run();
                }
            }
        });
    }

    public void setActive(boolean active) {
        this.active = active;
        repaint();
    }

    public void setFramed(boolean framed) {
        this.logoutStyle = framed;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int y = 2;
        int rectH = getHeight() - 4;
        if (logoutStyle) {
            paintLogout(g2, y, rectH);
        } else {
            paintMenuItem(g2, y, rectH);
        }
        g2.dispose();
    }

    private void paintMenuItem(Graphics2D g2, int y, int rectH) {
        int w = getWidth();
        if (active) {
            paintSoftShadow(g2, 5, y + 5, w - 10, rectH - 3, new Color(255, 106, 0, 32));
            g2.setPaint(new GradientPaint(0, y, ORANGE, w, y + rectH, ORANGE_2));
            g2.fillRoundRect(0, y, w, rectH, ARC, ARC);
        } else if (hovered) {
            g2.setColor(ORANGE_LIGHT);
            g2.fillRoundRect(0, y, w, rectH, ARC, ARC);
        }

        Color contentColor = active ? WHITE : (hovered ? ORANGE : TEXT_NAVY);
        Color iconColor = active ? WHITE : (hovered ? ORANGE : MUTED_ICON);
        int iconX = 19;
        int iconY = (getHeight() - ICON_SIZE) / 2;
        paintVectorIcon(g2, iconX, iconY, iconColor);

        g2.setColor(contentColor);
        g2.setFont(new Font("Segoe UI Semibold", active ? Font.BOLD : Font.PLAIN, 15));
        FontMetrics fm = g2.getFontMetrics();
        int textY = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(title, 56, textY);
    }

    private void paintLogout(Graphics2D g2, int y, int rectH) {
        int w = getWidth();
        g2.setColor(hovered ? ORANGE_LIGHT : WHITE);
        g2.fillRoundRect(0, y, w, rectH, ARC, ARC);
        g2.setColor(hovered ? LOGOUT_HOVER_BORDER : BORDER);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(0, y, w - 1, rectH - 1, ARC, ARC);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int logoutIconSize = 26;
        int totalWidth = logoutIconSize + 14 + fm.stringWidth(title);
        int iconX = Math.max(18, (w - totalWidth) / 2);
        int iconY = (getHeight() - logoutIconSize) / 2;
        paintLogoutIcon(g2, iconX, iconY, LOGOUT_RED);

        g2.setColor(LOGOUT_RED);
        int textY = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(title, iconX + logoutIconSize + 14, textY);
    }

    private void paintSoftShadow(Graphics2D g2, int x, int y, int w, int h, Color color) {
        for (int i = 5; i >= 1; i--) {
            int alpha = Math.max(3, color.getAlpha() / (i + 1));
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
            g2.fillRoundRect(x - i, y - i, w + i * 2, h + i * 2, ARC + i, ARC + i);
        }
    }

    private void paintVectorIcon(Graphics2D g2, int x, int y, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        String key = title.toLowerCase();

        // --- Admin sidebar ---
        if (key.contains("hệ thống")) {
            paintServerIcon(g2, x, y);
        } else if (key.contains("khuyến mãi")) {
            paintTagIcon(g2, x, y);
        } else if (key.contains("cửa hàng trưởng")) {
            paintBuildingIcon(g2, x, y);
        } else if (key.contains("tài khoản")) {
            paintIdCardIcon(g2, x, y);
        } else if (key.contains("phân quyền")) {
            paintKeyIcon(g2, x, y);
        } else if (key.contains("lịch sử")) {
            paintClockIcon(g2, x, y);
        } else if (key.contains("nhật ký")) {
            paintClipboardIcon(g2, x, y);
        // --- Shared sidebar ---
        } else if (key.contains("tổng quan") || key.contains("chi nhánh")) {
            paintDashboardIcon(g2, x, y);
        } else if (key.contains("bán hàng")) {
            paintCartIcon(g2, x, y);
        } else if (key.contains("sản phẩm")) {
            paintBoxIcon(g2, x, y);
        } else if (key.contains("tồn kho")) {
            paintWarehouseIcon(g2, x, y);
        } else if (key.contains("nhà cung cấp")) {
            paintTruckIcon(g2, x, y);
        } else if (key.contains("nhân viên")) {
            paintBadgeIcon(g2, x, y);
        } else if (key.contains("khách")) {
            paintPersonIcon(g2, x, y);
        } else if (key.contains("hóa đơn")) {
            paintReceiptIcon(g2, x, y);
        } else if (key.contains("báo cáo") || key.contains("thống kê")) {
            paintChartIcon(g2, x, y);
        } else if (key.contains("danh mục") || key.contains("thuế")) {
            paintFolderTagIcon(g2, x, y);
        } else if (key.contains("cài")) {
            paintSettingsIcon(g2, x, y);
        } else {
            paintBoxIcon(g2, x, y);
        }
    }

    // 🖥 Server stack — "Quản lý hệ thống"
    private void paintServerIcon(Graphics2D g2, int x, int y) {
        g2.drawRoundRect(x + 3, y + 3,  16, 5, 2, 2);
        g2.drawRoundRect(x + 3, y + 10, 16, 5, 2, 2);
        g2.fillOval(x + 15, y + 5,  2, 2);
        g2.fillOval(x + 15, y + 12, 2, 2);
        g2.drawLine(x + 6,  y + 20, x + 16, y + 20);
        g2.drawLine(x + 11, y + 15, x + 11, y + 20);
    }

    // 🏷 Price tag — "Quản lý khuyến mãi"
    private void paintTagIcon(Graphics2D g2, int x, int y) {
        Path2D tag = new Path2D.Double();
        tag.moveTo(x + 4,  y + 4);
        tag.lineTo(x + 13, y + 4);
        tag.lineTo(x + 20, y + 11);
        tag.lineTo(x + 13, y + 18);
        tag.lineTo(x + 4,  y + 18);
        tag.closePath();
        g2.draw(tag);
        g2.fillOval(x + 7, y + 7, 3, 3);
        g2.drawLine(x + 14, y + 8, x + 8, y + 14);
    }

    // 🏬 Store building — "Quản lý cửa hàng trưởng"
    private void paintBuildingIcon(Graphics2D g2, int x, int y) {
        g2.drawRect(x + 4, y + 8, 14, 12);
        g2.drawLine(x + 4,  y + 8,  x + 11, y + 3);
        g2.drawLine(x + 11, y + 3,  x + 18, y + 8);
        g2.drawRect(x + 9,  y + 14, 4, 6);
        g2.drawRect(x + 6,  y + 10, 3, 3);
        g2.drawRect(x + 13, y + 10, 3, 3);
    }

    // 🪪 ID Card — "Quản lý tài khoản"
    private void paintIdCardIcon(Graphics2D g2, int x, int y) {
        g2.drawRoundRect(x + 3, y + 6, 16, 10, 3, 3);
        g2.drawOval(x + 5,  y + 8, 4, 4);
        g2.drawLine(x + 11, y + 9,  x + 17, y + 9);
        g2.drawLine(x + 11, y + 13, x + 15, y + 13);
    }

    // 🔑 Key — "Quản lý phân quyền"
    private void paintKeyIcon(Graphics2D g2, int x, int y) {
        g2.drawOval(x + 3, y + 7, 8, 8);
        g2.drawLine(x + 11, y + 13, x + 19, y + 5);
        g2.drawLine(x + 17, y + 7,  x + 19, y + 5);
        g2.drawLine(x + 15, y + 15, x + 17, y + 13);
        g2.drawLine(x + 17, y + 15, x + 19, y + 13);
    }

    // 🕐 Clock — "Lịch sử truy cập"
    private void paintClockIcon(Graphics2D g2, int x, int y) {
        g2.drawOval(x + 3, y + 3, 16, 16);
        g2.drawLine(x + 11, y + 11, x + 11, y + 6);
        g2.drawLine(x + 11, y + 11, x + 15, y + 13);
        g2.fillOval(x + 10, y + 10, 2, 2);
    }

    // 📋 Clipboard — "Nhật ký hệ thống"
    private void paintClipboardIcon(Graphics2D g2, int x, int y) {
        g2.drawRoundRect(x + 5, y + 4, 12, 15, 2, 2);
        g2.drawRect(x + 8, y + 2, 6, 4);
        g2.drawLine(x + 8,  y + 10, x + 14, y + 10);
        g2.drawLine(x + 8,  y + 13, x + 14, y + 13);
        g2.drawLine(x + 8,  y + 16, x + 12, y + 16);
    }

    // ⊞ Dashboard grid — "Tổng quan"
    private void paintDashboardIcon(Graphics2D g2, int x, int y) {
        g2.drawRoundRect(x + 3,  y + 3,  7, 7, 2, 2);
        g2.drawRoundRect(x + 12, y + 3,  7, 7, 2, 2);
        g2.drawRoundRect(x + 3,  y + 12, 7, 7, 2, 2);
        g2.drawRoundRect(x + 12, y + 12, 7, 7, 2, 2);
    }

    // 🛒 Cart — "Bán hàng"
    private void paintCartIcon(Graphics2D g2, int x, int y) {
        g2.drawLine(x + 3,  y + 5,  x + 6,  y + 5);
        g2.drawLine(x + 6,  y + 5,  x + 9,  y + 16);
        g2.drawLine(x + 9,  y + 16, x + 18, y + 16);
        g2.drawLine(x + 8,  y + 8,  x + 19, y + 8);
        g2.drawLine(x + 19, y + 8,  x + 17, y + 14);
        g2.fillOval(x + 9,  y + 18, 3, 3);
        g2.fillOval(x + 17, y + 18, 3, 3);
    }

    // 📦 3D Box — "Quản lý sản phẩm"
    private void paintBoxIcon(Graphics2D g2, int x, int y) {
        Path2D box = new Path2D.Double();
        box.moveTo(x + 4,  y + 8);
        box.lineTo(x + 11, y + 4);
        box.lineTo(x + 18, y + 8);
        box.lineTo(x + 18, y + 16);
        box.lineTo(x + 11, y + 20);
        box.lineTo(x + 4,  y + 16);
        box.closePath();
        g2.draw(box);
        g2.drawLine(x + 4,  y + 8,  x + 11, y + 12);
        g2.drawLine(x + 18, y + 8,  x + 11, y + 12);
        g2.drawLine(x + 11, y + 12, x + 11, y + 20);
        g2.drawLine(x + 7,  y + 6,  x + 14, y + 10);
    }

    // 🏭 Warehouse shelves — "Quản lý tồn kho"
    private void paintWarehouseIcon(Graphics2D g2, int x, int y) {
        g2.drawLine(x + 3,  y + 4,  x + 19, y + 4);
        g2.drawLine(x + 3,  y + 11, x + 19, y + 11);
        g2.drawLine(x + 3,  y + 18, x + 19, y + 18);
        g2.drawLine(x + 5,  y + 4,  x + 5,  y + 18);
        g2.drawLine(x + 17, y + 4,  x + 17, y + 18);
        g2.drawRoundRect(x + 7,  y + 5,  4, 5, 1, 1);
        g2.drawRoundRect(x + 7,  y + 12, 4, 5, 1, 1);
        g2.drawRoundRect(x + 12, y + 5,  4, 5, 1, 1);
        g2.drawRoundRect(x + 12, y + 12, 4, 5, 1, 1);
    }

    // 🚚 Truck — "Quản lý nhà cung cấp"
    private void paintTruckIcon(Graphics2D g2, int x, int y) {
        g2.drawRect(x + 3,  y + 7, 11, 9);
        Path2D cabin = new Path2D.Double();
        cabin.moveTo(x + 14, y + 7);
        cabin.lineTo(x + 14, y + 10);
        cabin.lineTo(x + 19, y + 10);
        cabin.lineTo(x + 19, y + 16);
        cabin.lineTo(x + 14, y + 16);
        g2.draw(cabin);
        g2.fillOval(x + 5,  y + 16, 4, 4);
        g2.fillOval(x + 14, y + 16, 4, 4);
        g2.drawLine(x + 14, y + 12, x + 19, y + 12);
    }

    // 👤 Badge — "Quản lý nhân viên"
    private void paintBadgeIcon(Graphics2D g2, int x, int y) {
        g2.drawRoundRect(x + 5, y + 3, 12, 16, 3, 3);
        g2.drawOval(x + 9,  y + 6, 4, 4);
        g2.drawArc(x + 7,   y + 11, 8, 5, 0, 180);
        g2.drawLine(x + 8,  y + 3,  x + 8,  y + 6);
        g2.drawLine(x + 14, y + 3,  x + 14, y + 6);
        g2.drawLine(x + 8,  y + 3,  x + 14, y + 3);
    }

    // 🙍 Person — "Khách hàng"
    private void paintPersonIcon(Graphics2D g2, int x, int y) {
        g2.drawOval(x + 7, y + 3, 8, 8);
        g2.drawArc(x + 3,  y + 12, 16, 9, 0, 180);
    }

    // 🧾 Receipt — "Hóa đơn"
    private void paintReceiptIcon(Graphics2D g2, int x, int y) {
        Path2D receipt = new Path2D.Double();
        receipt.moveTo(x + 5,  y + 2);
        receipt.lineTo(x + 17, y + 2);
        receipt.lineTo(x + 17, y + 20);
        receipt.lineTo(x + 14, y + 18);
        receipt.lineTo(x + 11, y + 20);
        receipt.lineTo(x + 8,  y + 18);
        receipt.lineTo(x + 5,  y + 20);
        receipt.closePath();
        g2.draw(receipt);
        g2.drawLine(x + 8, y + 7,  x + 14, y + 7);
        g2.drawLine(x + 8, y + 11, x + 14, y + 11);
        g2.drawLine(x + 8, y + 15, x + 12, y + 15);
    }

    // 📊 Bar chart — "Báo cáo & Thống kê"
    private void paintChartIcon(Graphics2D g2, int x, int y) {
        g2.drawLine(x + 3, y + 19, x + 20, y + 19);
        g2.drawLine(x + 3, y + 19, x + 3,  y + 4);
        g2.fillRect(x + 5,  y + 12, 4, 7);
        g2.fillRect(x + 11, y + 8,  4, 11);
        g2.fillRect(x + 17, y + 5,  3, 14);
    }

    // 🗂 Folder — "Danh mục & Thuế VAT"
    private void paintFolderTagIcon(Graphics2D g2, int x, int y) {
        Path2D folder = new Path2D.Double();
        folder.moveTo(x + 3,  y + 7);
        folder.lineTo(x + 3,  y + 18);
        folder.lineTo(x + 19, y + 18);
        folder.lineTo(x + 19, y + 9);
        folder.lineTo(x + 10, y + 9);
        folder.lineTo(x + 8,  y + 7);
        folder.closePath();
        g2.draw(folder);
        g2.drawLine(x + 8,  y + 13, x + 14, y + 13);
        g2.drawLine(x + 11, y + 11, x + 11, y + 15);
    }

    // ⚙ Gear — "Cài đặt"
    private void paintSettingsIcon(Graphics2D g2, int x, int y) {
        g2.drawOval(x + 7, y + 7, 8, 8);
        int cx = x + 11, cy = y + 11;
        int[][] spokes = {{0,-1},{1,-1},{1,0},{1,1},{0,1},{-1,1},{-1,0},{-1,-1}};
        for (int[] s : spokes) {
            g2.drawLine(cx + s[0]*5, cy + s[1]*5, cx + s[0]*8, cy + s[1]*8);
        }
    }

    private void paintLogoutIcon(Graphics2D g2, int x, int y, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawRoundRect(x + 2, y + 4, 11, 18, 3, 3);
        g2.drawLine(x + 13, y + 13, x + 23, y + 13);
        g2.drawLine(x + 19, y + 8, x + 24, y + 13);
        g2.drawLine(x + 19, y + 18, x + 24, y + 13);
    }
}