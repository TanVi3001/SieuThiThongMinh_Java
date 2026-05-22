package view.components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Rounded button painter used by the Admin portal.
 * It keeps each button's existing background color, then paints a softer rounded
 * shape, hover overlay and subtle shadow.
 */
public class ModernAdminButtonUI extends BasicButtonUI {

    private static final int ARC = 16;

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        c.setOpaque(false);
        c.setBorder(javax.swing.BorderFactory.createEmptyBorder(9, 18, 9, 18));
        c.setFont(new Font("Segoe UI", Font.BOLD, 13));
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        d.width = Math.max(d.width + 10, 98);
        d.height = Math.max(d.height, 40);
        return d;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = c.getWidth();
        int h = c.getHeight();
        int x = 2;
        int y = 2;
        int bw = w - 4;
        int bh = h - 4;

        Color base = b.getBackground();
        if (base == null || base.equals(UIManager.getColor("Button.background"))) {
            base = new Color(37, 99, 235);
        }

        if (!b.isEnabled()) {
            base = new Color(148, 163, 184);
        }

        if (b.getModel().isRollover()) {
            base = brighten(base, 1.08f);
        }
        if (b.getModel().isPressed()) {
            base = darken(base, 0.88f);
            y += 1;
        }

        paintShadow(g2, x, y + 1, bw, bh, base);
        g2.setColor(base);
        g2.fillRoundRect(x, y, bw, bh, ARC, ARC);

        g2.setColor(new Color(255, 255, 255, b.getModel().isRollover() ? 42 : 26));
        g2.drawRoundRect(x, y, bw - 1, bh - 1, ARC, ARC);

        if (b.isFocusPainted() && b.hasFocus()) {
            g2.setColor(new Color(255, 255, 255, 90));
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRoundRect(x + 3, y + 3, bw - 7, bh - 7, ARC - 4, ARC - 4);
        }

        FontMetrics fm = g2.getFontMetrics();
        String text = b.getText();
        Icon icon = b.getIcon();
        Rectangle viewR = new Rectangle(0, 0, w, h);
        Rectangle iconR = new Rectangle();
        Rectangle textR = new Rectangle();

        SwingUtilities.layoutCompoundLabel(
                c,
                fm,
                text,
                icon,
                b.getVerticalAlignment(),
                b.getHorizontalAlignment(),
                b.getVerticalTextPosition(),
                b.getHorizontalTextPosition(),
                viewR,
                iconR,
                textR,
                text == null ? 0 : b.getIconTextGap()
        );

        if (icon != null) {
            icon.paintIcon(c, g2, iconR.x, iconR.y);
        }

        if (text != null && !text.isBlank()) {
            g2.setFont(b.getFont());
            g2.setColor(b.getForeground() != null ? b.getForeground() : Color.WHITE);
            g2.drawString(text, textR.x, textR.y + fm.getAscent());
        }

        g2.dispose();
    }

    private void paintShadow(Graphics2D g2, int x, int y, int w, int h, Color base) {
        for (int i = 4; i >= 1; i--) {
            g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), 10 / i));
            g2.fillRoundRect(x + i, y + i, w - i * 2, h - i, ARC + i, ARC + i);
        }
    }

    private Color brighten(Color c, float factor) {
        return new Color(
                Math.min(255, Math.round(c.getRed() * factor)),
                Math.min(255, Math.round(c.getGreen() * factor)),
                Math.min(255, Math.round(c.getBlue() * factor))
        );
    }

    private Color darken(Color c, float factor) {
        return new Color(
                Math.max(0, Math.round(c.getRed() * factor)),
                Math.max(0, Math.round(c.getGreen() * factor)),
                Math.max(0, Math.round(c.getBlue() * factor))
        );
    }
}
