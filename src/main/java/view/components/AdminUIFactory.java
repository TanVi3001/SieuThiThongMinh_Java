
package view.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;

public final class AdminUIFactory {
    public static final Color BG = new Color(244,246,250);
    public static final Color NAVY = new Color(18,38,63);
    public static final Color MUTED = new Color(100,116,139);
    public static final Color PRIMARY = new Color(67,97,238);
    public static final Color SUCCESS = new Color(16,185,129);
    public static final Color WARNING = new Color(245,158,11);
    public static final Color DANGER = new Color(239,68,68);
    public static final Color SECONDARY = new Color(226,232,240);
    public static final Color BORDER = new Color(226,232,240);

    private AdminUIFactory() {}

    public static JPanel page(String title, String subtitle) {
        JPanel page = new JPanel(new BorderLayout(0,18));
        page.setBackground(BG);
        page.setBorder(new EmptyBorder(24,28,28,28));
        page.add(header(title, subtitle), BorderLayout.NORTH);
        return page;
    }

    public static JPanel header(String title, String subtitle) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 28));
        t.setForeground(NAVY);
        JLabel s = new JLabel(subtitle == null ? "" : subtitle);
        s.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        s.setForeground(MUTED);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(t);
        p.add(Box.createVerticalStrut(4));
        p.add(s);
        return p;
    }

    public static RoundedPanel card() {
        RoundedPanel p = new RoundedPanel(22, Color.WHITE);
        p.setLayout(new BorderLayout());
        p.setBorder(new EmptyBorder(18,18,18,18));
        return p;
    }

    public static JPanel statCard(String title, String value, Color accent) {
        RoundedPanel p = new RoundedPanel(20, Color.WHITE);
        p.setLayout(new BorderLayout(8,4));
        p.setBorder(new EmptyBorder(16,16,16,16));
        JLabel a = new JLabel(title);
        a.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        a.setForeground(MUTED);
        JLabel b = new JLabel(value == null ? "0" : value);
        b.setFont(new Font("Segoe UI", Font.BOLD, 26));
        b.setForeground(NAVY);
        JLabel c = new JLabel("o");
        c.setFont(new Font("Segoe UI", Font.BOLD, 20));
        c.setForeground(accent == null ? PRIMARY : accent);
        p.add(a, BorderLayout.NORTH);
        p.add(b, BorderLayout.CENTER);
        p.add(c, BorderLayout.EAST);
        return p;
    }

    public static JButton button(String text, Color bg) {
        JButton b = new JButton(text);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setBackground(bg);
        b.setForeground(bg == SECONDARY ? NAVY : Color.WHITE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        b.setBorder(new EmptyBorder(10,16,10,16));
        return b;
    }

    public static JTextField searchField(String placeholder) {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setToolTipText(placeholder);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(9,12,9,12)));
        return f;
    }

    public static JTextField field() {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), new EmptyBorder(9,10,9,10)));
        return f;
    }

    public static JComboBox<String> combo(String... values) {
        JComboBox<String> c = new JComboBox<String>(values);
        c.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        c.setBackground(Color.WHITE);
        return c;
    }

    public static JLabel cardTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 18));
        l.setForeground(NAVY);
        return l;
    }

    public static JPanel formRow(String label, JComponent input) {
        JPanel p = new JPanel(new BorderLayout(0,6));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        l.setForeground(MUTED);
        p.add(l, BorderLayout.NORTH);
        p.add(input, BorderLayout.CENTER);
        return p;
    }

    public static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(null);
        sp.getViewport().setBackground(Color.WHITE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public static void setupTable(JTable table) {
        table.setRowHeight(44);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(NAVY);
        table.setSelectionBackground(new Color(219,234,254));
        table.setSelectionForeground(NAVY);
        table.setGridColor(new Color(241,245,249));
        table.setShowVerticalLines(false);
        table.setIntercellSpacing(new Dimension(0,1));
        JTableHeader h = table.getTableHeader();
        h.setPreferredSize(new Dimension(0,44));
        h.setFont(new Font("Segoe UI", Font.BOLD, 13));
        h.setForeground(Color.WHITE);
        h.setBackground(NAVY);
        h.setReorderingAllowed(false);
        table.setDefaultRenderer(Object.class, new ZebraRenderer());
    }

    public static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fill;
        public RoundedPanel(int radius, Color fill) {
            this.radius = radius;
            this.fill = fill;
            setOpaque(false);
        }
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(15,23,42,8));
            g2.fillRoundRect(3,4,getWidth()-6,getHeight()-7,radius,radius);
            g2.setColor(fill);
            g2.fillRoundRect(0,0,getWidth()-7,getHeight()-7,radius,radius);
            g2.setColor(BORDER);
            g2.drawRoundRect(0,0,getWidth()-8,getHeight()-8,radius,radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class ZebraRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
            JLabel c = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, col);
            c.setBorder(new EmptyBorder(0,10,0,10));
            if (!selected) c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248,250,252));
            c.setForeground(NAVY);
            return c;
        }
    }

    public static class BadgeRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, col);
            String text = value == null ? "" : value.toString();
            String u = text.toUpperCase();
            Color bg = new Color(226,232,240);
            Color fg = NAVY;
            if (u.contains("SUCCESS") || u.contains("HOAT") || u.contains("ACTIVE") || u.contains("ONLINE") || u.contains("CREATE")) {
                bg = new Color(220,252,231); fg = new Color(22,101,52);
            } else if (u.contains("FAIL") || u.contains("KHOA") || u.contains("DELETE") || u.contains("XOA") || u.contains("NGUNG")) {
                bg = new Color(254,226,226); fg = new Color(153,27,27);
            } else if (u.contains("UPDATE") || u.contains("CAP")) {
                bg = new Color(219,234,254); fg = new Color(30,64,175);
            } else if (u.contains("WARNING") || u.contains("PENDING") || u.contains("TAM")) {
                bg = new Color(254,243,199); fg = new Color(146,64,14);
            }
            l.setText("  " + text + "  ");
            l.setHorizontalAlignment(CENTER);
            l.setFont(new Font("Segoe UI", Font.BOLD, 12));
            l.setOpaque(true);
            l.setBackground(selected ? table.getSelectionBackground() : bg);
            l.setForeground(fg);
            return l;
        }
    }
}
