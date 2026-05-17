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

        if (key.contains("tổng quan") || key.contains("chi nhánh")) {
            paintHomeIcon(g2, x, y);
        } else if (key.contains("sản phẩm") || key.contains("khuyến mãi")) {
            paintBoxIcon(g2, x, y);
        } else if (key.contains("nhân viên") || key.contains("khách") || key.contains("trưởng") || key.contains("tài khoản") || key.contains("phân quyền")) {
            paintUsersIcon(g2, x, y);
        } else if (key.contains("hóa đơn") || key.contains("lịch sử") || key.contains("nhật ký")) {
            paintReceiptIcon(g2, x, y);
        } else if (key.contains("báo cáo") || key.contains("thống kê")) {
            paintChartIcon(g2, x, y);
        } else if (key.contains("cài")) {
            paintSettingsIcon(g2, x, y);
        } else if (key.contains("bán hàng")) {
            paintCartIcon(g2, x, y);
        } else {
            paintBoxIcon(g2, x, y);
        }
    }

    private void paintHomeIcon(Graphics2D g2, int x, int y) {
        Path2D roof = new Path2D.Double();
        roof.moveTo(x + 3, y + 11);
        roof.lineTo(x + 11, y + 4);
        roof.lineTo(x + 19, y + 11);
        g2.draw(roof);
        g2.drawRoundRect(x + 6, y + 10, 10, 9, 2, 2);
        g2.drawLine(x + 10, y + 19, x + 10, y + 14);
    }

    private void paintBoxIcon(Graphics2D g2, int x, int y) {
        Path2D box = new Path2D.Double();
        box.moveTo(x + 4, y + 8);
        box.lineTo(x + 11, y + 4);
        box.lineTo(x + 18, y + 8);
        box.lineTo(x + 18, y + 16);
        box.lineTo(x + 11, y + 20);
        box.lineTo(x + 4, y + 16);
        box.closePath();
        g2.draw(box);
        g2.drawLine(x + 4, y + 8, x + 11, y + 12);
        g2.drawLine(x + 18, y + 8, x + 11, y + 12);
        g2.drawLine(x + 11, y + 12, x + 11, y + 20);
    }

    private void paintUsersIcon(Graphics2D g2, int x, int y) {
        g2.drawOval(x + 8, y + 4, 6, 6);
        g2.drawArc(x + 5, y + 11, 12, 9, 0, 180);
        g2.drawOval(x + 2, y + 7, 5, 5);
        g2.drawArc(x, y + 13, 9, 7, 10, 150);
        g2.drawOval(x + 16, y + 7, 5, 5);
        g2.drawArc(x + 13, y + 13, 9, 7, 20, 150);
    }

    private void paintReceiptIcon(Graphics2D g2, int x, int y) {
        g2.drawRoundRect(x + 5, y + 3, 12, 16, 2, 2);
        g2.drawLine(x + 8, y + 8, x + 14, y + 8);
        g2.drawLine(x + 8, y + 12, x + 14, y + 12);
        g2.drawLine(x + 8, y + 16, x + 12, y + 16);
    }

    private void paintChartIcon(Graphics2D g2, int x, int y) {
        g2.drawLine(x + 3, y + 19, x + 20, y + 19);
        g2.drawLine(x + 5, y + 19, x + 5, y + 13);
        g2.drawLine(x + 11, y + 19, x + 11, y + 9);
        g2.drawLine(x + 17, y + 19, x + 17, y + 5);
        Path2D trend = new Path2D.Double();
        trend.moveTo(x + 4, y + 11);
        trend.lineTo(x + 9, y + 7);
        trend.lineTo(x + 13, y + 9);
        trend.lineTo(x + 18, y + 4);
        g2.draw(trend);
    }

    private void paintSettingsIcon(Graphics2D g2, int x, int y) {
        g2.drawOval(x + 7, y + 7, 8, 8);
        g2.drawOval(x + 3, y + 3, 16, 16);
        g2.drawLine(x + 11, y + 1, x + 11, y + 4);
        g2.drawLine(x + 11, y + 18, x + 11, y + 21);
        g2.drawLine(x + 1, y + 11, x + 4, y + 11);
        g2.drawLine(x + 18, y + 11, x + 21, y + 11);
    }

    private void paintCartIcon(Graphics2D g2, int x, int y) {
        g2.drawLine(x + 3, y + 5, x + 6, y + 5);
        g2.drawLine(x + 6, y + 5, x + 9, y + 16);
        g2.drawLine(x + 9, y + 16, x + 18, y + 16);
        g2.drawLine(x + 8, y + 8, x + 19, y + 8);
        g2.drawLine(x + 19, y + 8, x + 17, y + 14);
        g2.fillOval(x + 9, y + 18, 3, 3);
        g2.fillOval(x + 17, y + 18, 3, 3);
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
