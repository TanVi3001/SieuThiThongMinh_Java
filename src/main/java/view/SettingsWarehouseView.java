package view;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import model.account.Account;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;

public class SettingsWarehouseView extends JPanel {

    // ── Màu sắc & Font chuẩn Modern UI (Tông Xanh Lá cho Kho Hàng) ─────────────
    private static final Color BG_PAGE = new Color(248, 249, 252);
    private static final Color BG_WHITE = Color.WHITE;
    private static final Color BG_SIDEBAR = new Color(30, 41, 59);   // slate-800
    private static final Color BG_SIDEBAR_HV = new Color(51, 65, 85);   // slate-700
    
    // Tông màu Xanh Lá đặc trưng cho Warehouse
    private static final Color BG_TAB_ACTIVE = new Color(34, 197, 94);   // green-500
    private static final Color COLOR_PRIMARY = new Color(34, 197, 94);   // green-500
    private static final Color COLOR_PRIMARY_DK = new Color(21, 128, 61);  // green-700
    private static final Color SB_INDICATOR = new Color(187, 247, 208);  // green-200
    
    private static final Color COLOR_TEXT = new Color(15, 23, 42);   // slate-900
    private static final Color COLOR_MUTED = new Color(100, 116, 139);  // slate-500
    private static final Color COLOR_BORDER = new Color(226, 232, 240);  // slate-200
    private static final Color COLOR_HINT = new Color(148, 163, 184);  // slate-400
    private static final Color SB_TEXT = new Color(226, 232, 240);
    private static final Color SB_MUTED = new Color(148, 163, 184);

    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_MICRO = new Font("Segoe UI", Font.BOLD, 11);

    private static final int TAB_SECURITY = 0;
    private static final int TAB_APPEARANCE = 1;

    // ── Khởi tạo sẵn Components chống lỗi NullPointer ──────────────────────────
    private final JPasswordField txtOldPass = new JPasswordField();
    private final JPasswordField txtNewPass = new JPasswordField();
    private final JPasswordField txtConfirmPass = new JPasswordField();
    
    private final JComboBox<String> themeComboBox = new JComboBox<>(new String[]{"Sáng (Light Mode)", "Tối (Dark Mode)"});

    // ── Layout Control ─────────────────────────────────────────────────────────
    private int selectedTab = TAB_SECURITY;
    private CardLayout cardLayout;
    private JPanel contentArea;
    private JPanel[] tabItems;

    private AutoCloseable dataChangedSub;

    // ══════════════════════════════════════════════════════════════════════════
    public SettingsWarehouseView() {
        initUI();
        setupEventBus(); 
    }

    private void setupEventBus() {
        dataChangedSub = EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e == null) return;
            if (e.getType() == AppEventType.ACCOUNT_SECURITY) {
                txtOldPass.setText("");
                txtNewPass.setText("");
                txtConfirmPass.setText("");
            }
        });
    }

    public void disposeView() {
        if (dataChangedSub != null) {
            try { dataChangedSub.close(); } catch (Exception ignored) {}
            dataChangedSub = null;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(BG_PAGE);

        add(createTopBar(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(BG_PAGE);
        body.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel inner = new JPanel(new BorderLayout(16, 0));
        inner.setBackground(BG_PAGE);
        inner.add(createSidebar(), BorderLayout.WEST);

        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(BG_WHITE);
        contentArea.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));

        contentArea.add(wrapScrollResponsive(buildSecurityTab()), "TAB_0");
        contentArea.add(wrapScrollResponsive(buildAppearanceTab()), "TAB_1");

        inner.add(contentArea, BorderLayout.CENTER);
        body.add(inner, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        switchTab(TAB_SECURITY);
    }

    private JScrollPane wrapScrollResponsive(JPanel content) {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(BG_WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 1.0; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(30, 40, 30, 40);

        wrapper.add(content, gbc);

        JScrollPane sp = new JScrollPane(wrapper);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getVerticalScrollBar().setUnitIncrement(16);
        sp.getViewport().setBackground(BG_WHITE);
        return sp;
    }

    // ── Thanh công cụ phía trên ────────────────────────────────────────────────
    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_WHITE);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, COLOR_BORDER));
        bar.setPreferredSize(new Dimension(0, 58));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);

        JPanel accent = new JPanel();
        accent.setBackground(COLOR_PRIMARY);
        accent.setPreferredSize(new Dimension(4, 58));
        left.add(accent);

        JPanel titles = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        titles.setOpaque(false);
        JLabel lTitle = new JLabel("Cài đặt cá nhân");
        lTitle.setFont(FONT_TITLE);
        lTitle.setForeground(COLOR_TEXT);
        JLabel lSep = new JLabel("/");
        lSep.setFont(FONT_LABEL);
        lSep.setForeground(COLOR_HINT);
        JLabel lSub = new JLabel("Bảo mật & Giao diện Cổng kho hàng");
        lSub.setFont(FONT_LABEL);
        lSub.setForeground(COLOR_MUTED);
        titles.add(lTitle); titles.add(lSep); titles.add(lSub);
        left.add(titles);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        right.setOpaque(false);
        JButton btnSave = createPrimaryButton("Lưu cài đặt");
        btnSave.addActionListener(e -> onSave());
        right.add(btnSave);

        bar.add(left, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    // ── Thanh Sidebar bóng đêm bên trái ────────────────────────────────────────
    private JPanel createSidebar() {
        JPanel panel = new JPanel();
        panel.setPreferredSize(new Dimension(270, 0));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_SIDEBAR);
        panel.setBorder(BorderFactory.createLineBorder(new Color(51, 65, 85)));

        panel.add(createUserCard());
        panel.add(createSbDivider());

        JLabel navLbl = new JLabel("TÙY CHỌN");
        navLbl.setFont(FONT_MICRO);
        navLbl.setForeground(SB_MUTED);
        navLbl.setBorder(new EmptyBorder(14, 16, 6, 0));
        navLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(navLbl);

        String[] names = {"Bảo mật & Tài khoản", "Giao diện hệ thống"};
        String[] iconTypes = {"lock", "theme"}; 
        tabItems = new JPanel[names.length];

        for (int i = 0; i < names.length; i++) {
            final int idx = i;
            JPanel item = buildTabItem(iconTypes[i], names[i], i == selectedTab);
            tabItems[i] = item;
            item.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { switchTab(idx); }
                @Override public void mouseEntered(MouseEvent e) { if (idx != selectedTab) item.setBackground(BG_SIDEBAR_HV); }
                @Override public void mouseExited(MouseEvent e) { if (idx != selectedTab) item.setBackground(BG_SIDEBAR); }
            });
            panel.add(item);
        }

        panel.add(Box.createVerticalGlue());

        JLabel ver = new JLabel("v2.1.0  Smart Supermarket");
        ver.setFont(FONT_MICRO); ver.setForeground(SB_MUTED);
        ver.setBorder(new EmptyBorder(0, 16, 14, 0));
        ver.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(ver);
        return panel;
    }

    private JPanel buildTabItem(String iconType, String label, boolean active) {
        JPanel item = new JPanel(new BorderLayout());
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        item.setAlignmentX(Component.LEFT_ALIGNMENT);
        item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        item.setBackground(active ? BG_TAB_ACTIVE : BG_SIDEBAR);
        item.setOpaque(true);

        JPanel ind = new JPanel();
        ind.setPreferredSize(new Dimension(3, 44));
        ind.setBackground(active ? SB_INDICATOR : new Color(0, 0, 0, 0));
        ind.setOpaque(true);
        item.add(ind, BorderLayout.WEST);

        JPanel content = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        content.setOpaque(false);
        content.add(new TabIcon(iconType, active));
        JLabel lbl = new JLabel(label);
        lbl.setFont(active ? FONT_BOLD : FONT_LABEL);
        lbl.setForeground(active ? Color.WHITE : SB_TEXT);
        content.add(lbl);
        item.add(content, BorderLayout.CENTER);
        return item;
    }

    private static class TabIcon extends JComponent {
        private final String type;
        private final boolean active;

        TabIcon(String type, boolean active) {
            this.type = type; this.active = active;
            setPreferredSize(new Dimension(16, 16)); setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            // Sáng màu icon lên thành màu xanh nhẹ nếu active
            Color ic = active ? SB_INDICATOR : new Color(148, 163, 184);
            g2.setColor(ic);
            int w = getWidth(), h = getHeight();
            switch (type) {
                case "lock":
                    g2.drawArc(4, 1, w - 8, 8, 0, 180);
                    g2.drawRect(2, 8, w - 4, h - 9);
                    g2.fillOval(w / 2 - 2, h / 2 - 1, 4, 4);
                    break;
                case "theme":
                    g2.drawOval(4, 4, w - 8, h - 8);
                    g2.drawLine(w / 2, 0, w / 2, 2);
                    g2.drawLine(w / 2, h - 2, w / 2, h);
                    g2.drawLine(0, h / 2, 2, h / 2);
                    g2.drawLine(w - 2, h / 2, w, h / 2);
                    break;
            }
            g2.dispose();
        }
    }

    private JPanel createUserCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_SIDEBAR);
        card.setBorder(new EmptyBorder(20, 16, 16, 16));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        Account user = business.service.LoginService.getCurrentUser();
        String username = (user != null && user.getUsername() != null) ? user.getUsername() : "Thủ kho";
        String role = "Nhân viên Kho";
        String initials = username.length() >= 2 ? username.substring(0, 2).toUpperCase() : username.toUpperCase();

        JPanel avatar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_PRIMARY_DK); // Xanh lá đậm
                g2.fillOval(0, 0, getWidth(), getHeight());
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 15));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(initials, (getWidth() - fm.stringWidth(initials)) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(54, 54));
        avatar.setMaximumSize(new Dimension(54, 54));
        avatar.setOpaque(false);
        avatar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblName = new JLabel(username);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblName.setForeground(SB_TEXT);
        lblName.setBorder(new EmptyBorder(12, 0, 6, 0));
        lblName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel rolePill = new JPanel() {
            @Override public Dimension getPreferredSize() {
                FontMetrics fm = getFontMetrics(FONT_MICRO);
                return new Dimension(fm.stringWidth(role) + 24, 22);
            }
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(34, 197, 94, 70)); // Xanh lá nhạt trong suốt
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 10, 10));
                g2.setColor(SB_INDICATOR); 
                g2.setFont(FONT_MICRO);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(role, 12, (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                g2.dispose();
            }
        };
        rolePill.setOpaque(false);
        rolePill.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(avatar); card.add(lblName); card.add(rolePill);
        return card;
    }

    private JPanel createSbDivider() {
        JPanel line = new JPanel();
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setPreferredSize(new Dimension(0, 1));
        line.setBackground(new Color(51, 65, 85));
        return line;
    }

    private void switchTab(int idx) {
        selectedTab = idx;
        for (int i = 0; i < tabItems.length; i++) {
            JPanel item = tabItems[i];
            boolean active = (i == idx);
            item.setBackground(active ? BG_TAB_ACTIVE : BG_SIDEBAR);

            JPanel ind = (JPanel) item.getComponent(0);
            ind.setBackground(active ? SB_INDICATOR : new Color(0, 0, 0, 0));

            JPanel content = (JPanel) item.getComponent(1);
            JLabel lbl = (JLabel) content.getComponent(1);
            lbl.setFont(active ? FONT_BOLD : FONT_LABEL);
            lbl.setForeground(active ? Color.WHITE : SB_TEXT);
            item.repaint();
        }
        cardLayout.show(contentArea, "TAB_" + idx);
    }

    // ═════════════════════════════════════════════════════════════════════════=
    // TAB 1: BẢO MẬT & TÀI KHOẢN 
    // ═════════════════════════════════════════════════════════════════════════=
    private JPanel buildSecurityTab() {
        JPanel root = tabRoot();
        root.add(tabHeader("Bảo mật & Tài khoản", "Đổi mật khẩu bảo vệ tài khoản (Để trống nếu không thay đổi)"));
        root.add(Box.createVerticalStrut(24));

        JPanel row1 = new JPanel(new GridLayout(1, 2, 24, 0));
        row1.setBackground(BG_WHITE);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.add(fieldBlock("Mật khẩu hiện tại", txtOldPass));
        row1.add(new JLabel());

        JPanel row2 = new JPanel(new GridLayout(1, 2, 24, 0));
        row2.setBackground(BG_WHITE);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.add(fieldBlock("Mật khẩu mới", txtNewPass));
        row2.add(fieldBlock("Xác nhận mật khẩu mới", txtConfirmPass));

        root.add(row1); root.add(Box.createVerticalStrut(20)); root.add(row2);
        return root;
    }

    // ═════════════════════════════════════════════════════════════════════════=
    // TAB 2: GIAO DIỆN HỆ THỐNG
    // ═════════════════════════════════════════════════════════════════════════=
    private JPanel buildAppearanceTab() {
        JPanel root = tabRoot();
        root.add(tabHeader("Giao diện hệ thống", "Tùy chỉnh chế độ hiển thị Sáng/Tối cho ứng dụng"));
        root.add(Box.createVerticalStrut(24));

        // Phản hồi lập tức khi chọn Theme
        themeComboBox.addActionListener(e -> {
            try {
                UIManager.setLookAndFeel(themeComboBox.getSelectedIndex() == 1 ? new FlatDarkLaf() : new FlatLightLaf());
                SwingUtilities.updateComponentTreeUI(SwingUtilities.getWindowAncestor(this));
            } catch (Exception ex) {}
        });

        JPanel row1 = new JPanel(new GridLayout(1, 2, 24, 0));
        row1.setBackground(BG_WHITE);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);

        row1.add(fieldBlock("Chế độ giao diện", themeComboBox));
        row1.add(new JLabel());

        root.add(row1); 
        return root;
    }

    // ═════════════════════════════════════════════════════════════════════════=
    // UI HELPERS & DATABASE
    // ═════════════════════════════════════════════════════════════════════════=
    private JPanel tabRoot() {
        JPanel p = new JPanel(); p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS)); p.setBackground(BG_WHITE); return p;
    }

    private JPanel tabHeader(String title, String subtitle) {
        JPanel p = new JPanel(new BorderLayout()); p.setBackground(BG_WHITE); p.setAlignmentX(Component.LEFT_ALIGNMENT); p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        JPanel bar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setColor(COLOR_PRIMARY); g2.fillRoundRect(0, 6, getWidth(), getHeight() - 12, 4, 4); g2.dispose();
            }
        };
        bar.setPreferredSize(new Dimension(4, 0)); bar.setOpaque(false); p.add(bar, BorderLayout.WEST);

        JPanel texts = new JPanel(); texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS)); texts.setBackground(BG_WHITE); texts.setBorder(new EmptyBorder(0, 14, 0, 0));
        JLabel lTitle = new JLabel(title); lTitle.setFont(new Font("Segoe UI", Font.BOLD, 16)); lTitle.setForeground(COLOR_TEXT); lTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lSub = new JLabel(subtitle); lSub.setFont(FONT_SMALL); lSub.setForeground(COLOR_MUTED); lSub.setBorder(new EmptyBorder(4, 0, 0, 0)); lSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        texts.add(lTitle); texts.add(lSub); p.add(texts, BorderLayout.CENTER); return p;
    }

    private JPanel fieldBlock(String labelText, JComponent comp) {
        JPanel block = new JPanel(); block.setLayout(new BoxLayout(block, BoxLayout.Y_AXIS)); block.setBackground(BG_WHITE); block.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel(labelText); lbl.setFont(new Font("Segoe UI", Font.BOLD, 13)); lbl.setForeground(new Color(51, 65, 85)); lbl.setAlignmentX(Component.LEFT_ALIGNMENT); lbl.setBorder(new EmptyBorder(0, 0, 8, 0)); block.add(lbl);
        styleInput(comp); comp.setPreferredSize(new Dimension(200, 40)); comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40)); comp.setAlignmentX(Component.LEFT_ALIGNMENT); block.add(comp); return block;
    }

    private void styleInput(JComponent c) {
        c.setFont(FONT_LABEL);
        if (c instanceof JTextField || c instanceof JPasswordField || c instanceof JComboBox) {
            c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1), new EmptyBorder(5, 12, 5, 12))); c.setBackground(BG_WHITE);
        }
    }

    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isPressed() ? COLOR_PRIMARY_DK : getModel().isRollover() ? new Color(34, 211, 153) : COLOR_PRIMARY);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8); g2.setColor(Color.WHITE); g2.setFont(FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics(); g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2, (getHeight() + fm.getAscent() - fm.getDescent()) / 2); g2.dispose();
            }
        };
        btn.setContentAreaFilled(false); btn.setBorderPainted(false); btn.setFocusPainted(false); btn.setPreferredSize(new Dimension(120, 34)); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)); return btn;
    }

    private void onSave() {
        String oldPass = new String(txtOldPass.getPassword());
        String newPass = new String(txtNewPass.getPassword());
        String confirmPass = new String(txtConfirmPass.getPassword());

        if (newPass.isEmpty() && oldPass.isEmpty() && confirmPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Đã lưu thiết lập hệ thống thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        if (newPass.isEmpty() || confirmPass.isEmpty() || oldPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ mật khẩu hiện tại, mật khẩu mới và xác nhận mật khẩu!", "Thiếu thông tin", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp!", "Lỗi xác thực", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Account user = business.service.LoginService.getCurrentUser();
        if (user == null || user.getUsername() == null || user.getUsername().isBlank()) {
            JOptionPane.showMessageDialog(this, "Không xác định được tài khoản đăng nhập hiện tại!", "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String hashFromDb = null;
        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT password FROM ACCOUNTS WHERE username = ? AND is_deleted = 0")) {
            ps.setString(1, user.getUsername());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) hashFromDb = rs.getString("password");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi đọc mật khẩu hiện tại: " + e.getMessage(), "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!common.utils.PasswordUtils.checkPassword(oldPass, hashFromDb)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu hiện tại không đúng!", "Sai mật khẩu", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(
                "UPDATE ACCOUNTS SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE username = ?")) {
            String newHash = common.utils.PasswordUtils.hashPassword(newPass);
            ps.setString(1, newHash);
            ps.setString(2, user.getUsername());

            int updated = ps.executeUpdate();
            if (updated <= 0) {
                JOptionPane.showMessageDialog(this, "Không cập nhật được mật khẩu (không tìm thấy tài khoản)!", "Lỗi cập nhật", JOptionPane.ERROR_MESSAGE);
                return;
            }
            EventBus.publish(new AppDataChangedEvent(AppEventType.ACCOUNT_SECURITY, "Password updated"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi cập nhật mật khẩu: " + e.getMessage(), "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            txtOldPass.setText(""); txtNewPass.setText(""); txtConfirmPass.setText("");
        }

        JOptionPane.showMessageDialog(this, "Đã cập nhật mật khẩu và lưu thiết lập thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
    }
}