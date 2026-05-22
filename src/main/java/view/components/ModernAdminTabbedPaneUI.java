package view.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicTabbedPaneUI;

/**
 * Soft rounded tab style for Admin portal tabs.
 */
public class ModernAdminTabbedPaneUI extends BasicTabbedPaneUI {

    private static final Color ORANGE = new Color(255, 106, 0);
    private static final Color ORANGE_LIGHT = new Color(255, 243, 234);
    private static final Color NAVY = new Color(15, 23, 42);
    private static final Color MUTED = new Color(100, 116, 139);
    private static final Color BORDER = new Color(226, 232, 240);

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabAreaInsets = new Insets(0, 0, 8, 0);
        tabInsets = new Insets(9, 18, 9, 18);
        selectedTabPadInsets = new Insets(0, 0, 0, 0);
        contentBorderInsets = new Insets(0, 0, 0, 0);
        tabPane.setOpaque(false);
        tabPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (isSelected) {
            g2.setColor(ORANGE_LIGHT);
            g2.fillRoundRect(x + 2, y + 2, w - 4, h - 5, 14, 14);
            g2.setColor(new Color(255, 106, 0, 40));
            g2.drawRoundRect(x + 2, y + 2, w - 5, h - 6, 14, 14);
        } else if (getRolloverTab() == tabIndex) {
            g2.setColor(new Color(255, 106, 0, 15));
            g2.fillRoundRect(x + 2, y + 2, w - 4, h - 5, 14, 14);
        }

        g2.dispose();
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex, int x, int y, int w, int h, boolean isSelected) {
        if (!isSelected) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ORANGE);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(x + 12, y + h - 4, x + w - 12, y + h - 4);
        g2.dispose();
    }

    @Override
    protected void paintText(Graphics g, int tabPlacement, Font font, java.awt.FontMetrics metrics, int tabIndex, String title, Rectangle textRect, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(font.deriveFont(isSelected ? Font.BOLD : Font.PLAIN));
        g2.setColor(isSelected ? ORANGE : MUTED);
        g2.drawString(title, textRect.x, textRect.y + metrics.getAscent());
        g2.dispose();
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(BORDER);
        g2.drawLine(0, calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight) + 1, tabPane.getWidth(), calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight) + 1);
        g2.dispose();
    }

    @Override
    public void update(Graphics g, JComponent c) {
        c.setOpaque(false);
        paint(g, c);
    }
}
