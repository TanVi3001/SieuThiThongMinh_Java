package view.components;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class ModernSidebarMenuItem extends JPanel {

    public static final Color PRIMARY_ORANGE = new Color(255, 106, 0);
    public static final Color PRIMARY_ORANGE_DARK = new Color(238, 86, 0);
    public static final Color PRIMARY_ORANGE_LIGHT = new Color(255, 243, 234);
    public static final Color NAVY = new Color(23, 52, 99);
    public static final Color TEXT_NAVY = new Color(43, 63, 107);
    public static final Color BORDER = new Color(230, 236, 245);
    public static final Color WHITE = Color.WHITE;

    private static final int ARC = 14;
    private static final int ICON_SIZE = 22;

    private final String title;
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
        setPreferredSize(new Dimension(260, 58));
        setMinimumSize(new Dimension(220, 58));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));

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
        setPreferredSize(new Dimension(260, 58));
        setMinimumSize(new Dimension(220, 58));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int y = 3;
        int rectH = h - 6;

        if (logoutStyle) {
            paintLogout(g2, w, y, rectH);
        } else {
            paintMenuItem(g2, w, y, rectH);
        }
        g2.dispose();
    }

    private void paintMenuItem(Graphics2D g2, int w, int y, int rectH) {
        if (active) {
            paintSoftShadow(g2, 4, y + 4, w - 8, rectH - 2, ARC, new Color(255, 106, 0, 42));
            g2.setPaint(new java.awt.GradientPaint(0, y, PRIMARY_ORANGE, w, y + rectH, new Color(255, 133, 36)));
            g2.fillRoundRect(0, y, w, rectH, ARC, ARC);
        } else if (hovered) {
            g2.setColor(PRIMARY_ORANGE_LIGHT);
            g2.fillRoundRect(0, y, w, rectH, ARC, ARC);
        }

        Color contentColor = active ? WHITE : TEXT_NAVY;
        int iconX = 24;
        int iconY = (getHeight() - ICON_SIZE) / 2;
        paintIcon(g2, iconX, iconY, contentColor);

        g2.setColor(contentColor);
        g2.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 16));
        FontMetrics fm = g2.getFontMetrics();
        int textY = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(title, 64, textY);
    }

    private void paintLogout(Graphics2D g2, int w, int y, int rectH) {
        Color textColor = hovered ? PRIMARY_ORANGE_DARK : PRIMARY_ORANGE;
        g2.setColor(hovered ? PRIMARY_ORANGE_LIGHT : WHITE);
        g2.fillRoundRect(0, y, w, rectH, ARC, ARC);
        g2.setColor(BORDER);
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(0, y, w - 1, rectH - 1, ARC, ARC);

        g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
        FontMetrics fm = g2.getFontMetrics();
        int totalWidth = ICON_SIZE + 12 + fm.stringWidth(title);
        int iconX = Math.max(20, (w - totalWidth) / 2);
        int iconY = (getHeight() - ICON_SIZE) / 2;
        paintIcon(g2, iconX, iconY, new Color(255, 77, 61));

        g2.setColor(textColor);
        int textY = ((getHeight() - fm.getHeight()) / 2) + fm.getAscent();
        g2.drawString(title, iconX + ICON_SIZE + 12, textY);
    }

    private void paintSoftShadow(Graphics2D g2, int x, int y, int w, int h, int arc, Color color) {
        for (int i = 4; i >= 1; i--) {
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha() / (i + 1)));
            g2.fillRoundRect(x - i, y - i, w + i * 2, h + i * 2, arc + i, arc + i);
        }
    }

    private void paintIcon(Graphics2D g2, int x, int y, Color color) {
        ImageIcon source = icon != null ? icon : IconHelper.product(ICON_SIZE);
        if (source == null || source.getImage() == null) {
            paintFallbackIcon(g2, x, y, color);
            return;
        }

        BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D ig = image.createGraphics();
        ig.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        ig.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        ig.drawImage(source.getImage(), 0, 0, ICON_SIZE, ICON_SIZE, null);
        ig.setComposite(AlphaComposite.SrcAtop);
        ig.setColor(color);
        ig.fillRect(0, 0, ICON_SIZE, ICON_SIZE);
        ig.dispose();

        g2.drawImage(image, x, y, null);
    }

    private void paintFallbackIcon(Graphics2D g2, int x, int y, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawRoundRect(x + 3, y + 5, 16, 13, 4, 4);
        g2.drawLine(x + 7, y + 5, x + 7, y + 2);
        g2.drawLine(x + 15, y + 5, x + 15, y + 2);
    }
}
