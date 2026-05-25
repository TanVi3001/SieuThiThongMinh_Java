package view.components;

import business.service.LoginService;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import common.db.DatabaseConnection;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.utils.PasswordUtils;
import model.account.Account;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.sql.*;

/**
 * Unified Settings Panel - mỗi mục là một phần riêng: - Thông tin cửa hàng: chỉ
 * Admin được chỉnh. - Giao diện: đổi theme riêng. - Bảo mật: đổi mật khẩu
 * riêng. - Email: cấu hình email riêng.
 */
public class UnifiedSettingsPanel extends JPanel {

    private static final Color BG_APP = new Color(241, 245, 249);
    private static final Color BG_PANEL = Color.WHITE;
    private static final Color COLOR_PRIMARY = new Color(79, 70, 229);
    private static final Color COLOR_PRIMARY_SOFT = new Color(224, 231, 255);
    private static final Color COLOR_TEXT = new Color(15, 23, 42);
    private static final Color COLOR_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_NAV = new Color(15, 23, 42);
    private static final Color COLOR_NAV_TEXT = new Color(248, 250, 252);
    private static final Color COLOR_NAV_MUTED = new Color(148, 163, 184);
    private static final Color COLOR_NAV_ACTIVE = new Color(30, 41, 59);
    private static final Color COLOR_NAV_BORDER = new Color(51, 65, 85);
    private static final Color COLOR_NAV_BADGE = new Color(59, 130, 246);
    private static final Color COLOR_NAV_BADGE_THEME = new Color(34, 197, 94);
    private static final Color COLOR_NAV_BADGE_SECURITY = new Color(245, 158, 11);
    private static final Color COLOR_NAV_BADGE_EMAIL = new Color(168, 85, 247);
    private static final Color COLOR_NAV_HOVER = new Color(23, 37, 84);
    private static final Color COLOR_DANGER_SOFT = new Color(254, 242, 242);
    private static final Color COLOR_DANGER = new Color(220, 38, 38);
    private static final Color COLOR_SUCCESS_SOFT = new Color(240, 253, 244);
    private static final Color COLOR_SUCCESS = new Color(22, 163, 74);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 15);
    private static final Font FONT_NAV = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_TEXT = new Font("Segoe UI", Font.PLAIN, 14);

    private static final String STORE_KEY = "store";
    private static final String THEME_KEY = "theme";
    private static final String SECURITY_KEY = "security";
    private static final String EMAIL_KEY = "email";

    private JTextField txtStoreName;
    private JTextField txtStoreAddress;
    private JTextField txtStorePhone;
    private JComboBox<String> cbTheme;
    private JPasswordField txtOldPass;
    private JPasswordField txtNewPass;
    private JPasswordField txtConfirmPass;
    private JTextField txtEmailSender;
    private JPasswordField txtAppPassword;

    private JPanel cardPanel;
    private CardLayout cardLayout;
    private JButton btnStore;
    private JButton btnTheme;
    private JButton btnSecurity;
    private JButton btnEmail;
    private JButton btnSave;
    private JButton btnRefresh;
    private JLabel lblCurrentSectionHint;

    private String activeSection = STORE_KEY;

    private AutoCloseable eventSub;

    public UnifiedSettingsPanel() {
        initUI();
        loadSettings();
        applyStoreEditPermission();
        setupEventBus();
        showSection(STORE_KEY);
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(BG_APP);

        add(createTopBar(), BorderLayout.NORTH);
        add(createBody(), BorderLayout.CENTER);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_PANEL);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, COLOR_BORDER));
        bar.setPreferredSize(new Dimension(0, 64));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        left.setOpaque(false);

        JLabel title = new JLabel("Cài đặt hệ thống");
        title.setFont(FONT_TITLE);
        title.setForeground(COLOR_TEXT);

        JLabel subtitle = new JLabel("Mỗi mục là một phần riêng, lưu đúng phần đang chọn");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(COLOR_MUTED);

        JPanel titleWrap = new JPanel();
        titleWrap.setLayout(new BoxLayout(titleWrap, BoxLayout.Y_AXIS));
        titleWrap.setOpaque(false);
        titleWrap.add(title);
        titleWrap.add(subtitle);

        left.add(titleWrap);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        right.setOpaque(false);

        lblCurrentSectionHint = new JLabel("");
        lblCurrentSectionHint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCurrentSectionHint.setForeground(COLOR_MUTED);
        lblCurrentSectionHint.setBorder(new EmptyBorder(0, 0, 0, 10));

        btnRefresh = createActionButton("Làm mới", false);
        btnRefresh.addActionListener(e -> {
            loadSettings();
            applyStoreEditPermission();
        });

        btnSave = createActionButton("Lưu mục này", true);
        btnSave.addActionListener(e -> saveCurrentSection());

        right.add(lblCurrentSectionHint);
        right.add(btnRefresh);
        right.add(btnSave);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createBody() {
        JPanel body = new JPanel(new BorderLayout(18, 0));
        body.setBackground(BG_APP);
        body.setBorder(new EmptyBorder(14, 14, 14, 14));

        body.add(createNavPanel(), BorderLayout.WEST);
        body.add(createContentShell(), BorderLayout.CENTER);
        return body;
    }

    private JPanel createNavPanel() {
        JPanel nav = new JPanel();
        nav.setPreferredSize(new Dimension(270, 0));
        nav.setBackground(COLOR_NAV);
        nav.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, COLOR_NAV_BORDER),
                new EmptyBorder(18, 16, 18, 16)
        ));
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));

        JLabel group = new JLabel("Chức năng");
        group.setFont(FONT_SECTION);
        group.setForeground(COLOR_NAV_TEXT);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("Chọn từng mục để cấu hình riêng");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(COLOR_NAV_MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        nav.add(group);
        nav.add(Box.createVerticalStrut(4));
        nav.add(hint);
        nav.add(Box.createVerticalStrut(18));

        btnStore = createNavButton("Thông tin cửa hàng", STORE_KEY, "T", COLOR_NAV_BADGE);
        btnTheme = createNavButton("Giao diện", THEME_KEY, "▭", COLOR_NAV_BADGE_THEME);
        btnSecurity = createNavButton("Bảo mật", SECURITY_KEY, "B", COLOR_NAV_BADGE_SECURITY);

        nav.add(btnStore);
        nav.add(Box.createVerticalStrut(12));
        nav.add(btnTheme);
        nav.add(Box.createVerticalStrut(12));
        nav.add(btnSecurity);

        if (isAdminOrWarehouse()) {
            nav.add(Box.createVerticalStrut(12));
            btnEmail = createNavButton("Email", EMAIL_KEY, "E", COLOR_NAV_BADGE_EMAIL);
            nav.add(btnEmail);
        }

        nav.add(Box.createVerticalGlue());

        JLabel note = new JLabel("Mỗi mục lưu độc lập");
        note.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        note.setForeground(COLOR_NAV_MUTED);
        note.setAlignmentX(Component.LEFT_ALIGNMENT);
        nav.add(note);

        return nav;
    }

    private JPanel createContentShell() {
        JPanel shell = new JPanel(new BorderLayout());
        shell.setBackground(BG_APP);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BG_PANEL);
        content.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, COLOR_BORDER),
                new EmptyBorder(24, 28, 28, 28)
        ));

        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(BG_PANEL);

        cardPanel.add(buildStoreSection(), STORE_KEY);
        cardPanel.add(buildThemeSection(), THEME_KEY);
        cardPanel.add(buildSecuritySection(), SECURITY_KEY);
        if (isAdminOrWarehouse()) {
            cardPanel.add(buildEmailSection(), EMAIL_KEY);
        }

        content.add(cardPanel, BorderLayout.CENTER);
        shell.add(content, BorderLayout.CENTER);
        return shell;
    }

    private JButton createActionButton(String text, boolean primary) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_TEXT);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, primary ? COLOR_PRIMARY : COLOR_BORDER),
                new EmptyBorder(8, 16, 8, 16)
        ));
        btn.setBackground(primary ? COLOR_PRIMARY : BG_PANEL);
        btn.setForeground(primary ? Color.WHITE : COLOR_TEXT);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JButton createNavButton(String text, String sectionKey, String badgeText, Color badgeColor) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_NAV);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, COLOR_NAV_BORDER),
                new EmptyBorder(12, 14, 12, 14)
        ));
        btn.setBackground(COLOR_NAV);
        btn.setForeground(COLOR_NAV_TEXT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setIcon(createBadgeIcon(badgeText, badgeColor));
        btn.setIconTextGap(12);
        btn.setRolloverEnabled(true);
        btn.setContentAreaFilled(true);
        btn.setOpaque(true);
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (!sectionKey.equals(activeSection)) {
                    btn.setBackground(COLOR_NAV_HOVER);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (!sectionKey.equals(activeSection)) {
                    btn.setBackground(COLOR_NAV);
                }
            }
        });
        btn.addActionListener(e -> showSection(sectionKey));
        return btn;
    }

    private JPanel buildStoreSection() {
        JPanel card = createContentCard(
                "Thông tin cửa hàng",
                isAdminOnly()
                        ? "Admin có thể chỉnh thông tin hiển thị trên hóa đơn và báo cáo"
                        : "Chỉ Admin được chỉnh thông tin cửa hàng. Tài khoản hiện tại chỉ được xem."
        );

        txtStoreName = new JTextField();
        txtStoreAddress = new JTextField();
        txtStorePhone = new JTextField();

        card.add(permissionNotice(
                isAdminOnly()
                        ? "Bạn đang đăng nhập bằng quyền Admin, có thể chỉnh thông tin cửa hàng."
                        : "Bạn không có quyền chỉnh thông tin cửa hàng. Phần này chỉ cho phép xem.",
                isAdminOnly()
        ));
        card.add(Box.createVerticalStrut(14));

        card.add(fieldRow("Tên siêu thị/Cửa hàng", txtStoreName));
        card.add(Box.createVerticalStrut(14));
        card.add(fieldRow("Địa chỉ", txtStoreAddress));
        card.add(Box.createVerticalStrut(14));
        card.add(fieldRow("Số điện thoại", txtStorePhone));

        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel buildThemeSection() {
        JPanel card = createContentCard("Giao diện hệ thống", "Chọn chế độ hiển thị Sáng hoặc Tối");

        JLabel preview = new JLabel("Chế độ hiển thị giao diện");
        preview.setFont(new Font("Segoe UI", Font.BOLD, 14));
        preview.setForeground(COLOR_SUCCESS);
        preview.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(preview);
        card.add(Box.createVerticalStrut(12));

        cbTheme = new JComboBox<>(new String[]{"Sáng (Light Mode)", "Tối (Dark Mode)"});
        cbTheme.setFont(FONT_TEXT);
        cbTheme.addActionListener(e -> applyTheme());

        card.add(fieldRow("Chế độ giao diện", cbTheme));
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel buildSecuritySection() {
        JPanel card = createContentCard("Bảo mật tài khoản", "Đổi mật khẩu để bảo vệ tài khoản");

        JLabel lblUsername = new JLabel("Tài khoản: " + getCurrentUsername());
        lblUsername.setFont(FONT_TEXT);
        lblUsername.setForeground(COLOR_MUTED);
        card.add(lblUsername);
        card.add(Box.createVerticalStrut(16));

        txtOldPass = new JPasswordField();
        txtNewPass = new JPasswordField();
        txtConfirmPass = new JPasswordField();

        card.add(fieldRow("Mật khẩu hiện tại", txtOldPass));
        card.add(Box.createVerticalStrut(14));
        card.add(fieldRow("Mật khẩu mới", txtNewPass));
        card.add(Box.createVerticalStrut(14));
        card.add(fieldRow("Xác nhận mật khẩu mới", txtConfirmPass));

        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel buildEmailSection() {
        JPanel card = createContentCard("Cấu hình Email", "Thiết lập gửi email từ hệ thống");

        txtEmailSender = new JTextField();
        txtAppPassword = new JPasswordField();

        card.add(fieldRow("Email gửi", txtEmailSender));
        card.add(Box.createVerticalStrut(14));
        card.add(fieldRow("App Password / API Key", txtAppPassword));

        JLabel hint = new JLabel("Để trống App Password nếu không muốn đổi mật khẩu ứng dụng.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(COLOR_MUTED);
        hint.setBorder(new EmptyBorder(10, 2, 0, 0));
        card.add(hint);

        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel createContentCard(String title, String subtitle) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_PANEL);
        card.setBorder(new EmptyBorder(0, 0, 0, 0));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setBackground(new Color(248, 250, 252));
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COLOR_BORDER),
                new EmptyBorder(0, 0, 14, 0)
        ));

        JPanel accent = new JPanel();
        accent.setBackground(COLOR_PRIMARY);
        accent.setPreferredSize(new Dimension(0, 4));
        accent.setMaximumSize(new Dimension(Integer.MAX_VALUE, 4));
        accent.setMinimumSize(new Dimension(0, 4));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(FONT_SECTION);
        titleLabel.setForeground(COLOR_TEXT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(COLOR_MUTED);

        header.add(titleLabel);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitleLabel);

        card.add(accent);
        card.add(header);
        card.add(Box.createVerticalStrut(16));
        return card;
    }

    private JPanel permissionNotice(String text, boolean allowed) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBackground(allowed ? COLOR_SUCCESS_SOFT : COLOR_DANGER_SOFT);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, allowed ? new Color(187, 247, 208) : new Color(254, 202, 202)),
                new EmptyBorder(10, 12, 10, 12)
        ));

        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(allowed ? COLOR_SUCCESS : COLOR_DANGER);

        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    private JPanel fieldRow(String label, JComponent component) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(COLOR_TEXT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        styleComponent(component);
        component.setPreferredSize(new Dimension(400, 36));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        component.setAlignmentX(Component.LEFT_ALIGNMENT);

        panel.add(lbl);
        panel.add(Box.createVerticalStrut(6));
        panel.add(component);

        return panel;
    }

    private void styleComponent(JComponent c) {
        c.setFont(FONT_TEXT);
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER),
                new EmptyBorder(8, 12, 8, 12)
        ));
        c.setBackground(BG_PANEL);
        if (c instanceof JComboBox) {
            ((JComboBox<?>) c).setBackground(BG_PANEL);
        }
    }

    private void loadSettings() {
        try (Connection con = DatabaseConnection.getConnection(); Statement stmt = con.createStatement(); ResultSet rs = stmt.executeQuery("SELECT config_key, config_value FROM SYSTEM_CONFIG")) {

            while (rs.next()) {
                String key = rs.getString("config_key");
                String val = rs.getString("config_value");

                switch (key) {
                    case "store_name":
                        txtStoreName.setText(val != null ? val : "");
                        break;
                    case "store_address":
                        txtStoreAddress.setText(val != null ? val : "");
                        break;
                    case "store_phone":
                        txtStorePhone.setText(val != null ? val : "");
                        break;
                    case "theme_mode":
                        cbTheme.setSelectedIndex("Dark".equals(val) ? 1 : 0);
                        break;
                    case "email_sender":
                        if (txtEmailSender != null) {
                            txtEmailSender.setText(val != null ? val : "");
                        }
                        break;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải cấu hình: " + e.getMessage());
        }

        if (txtOldPass != null) {
            txtOldPass.setText("");
            txtNewPass.setText("");
            txtConfirmPass.setText("");
        }
    }

    private void saveCurrentSection() {
        try {
            switch (activeSection) {
                case STORE_KEY:
                    saveStoreSection();
                    break;
                case THEME_KEY:
                    saveThemeSection();
                    break;
                case SECURITY_KEY:
                    saveSecuritySection();
                    break;
                case EMAIL_KEY:
                    saveEmailSection();
                    break;
                default:
                    showMessage("Không xác định mục cài đặt cần lưu.", JOptionPane.WARNING_MESSAGE);
                    break;
            }
        } catch (Exception e) {
            showMessage("❌ Lỗi: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveStoreSection() throws SQLException {
        if (!isAdminOnly()) {
            showMessage("❌ Chỉ Admin mới được chỉnh thông tin cửa hàng.", JOptionPane.ERROR_MESSAGE);
            loadSettings();
            applyStoreEditPermission();
            return;
        }

        saveConfig("store_name", txtStoreName.getText().trim());
        saveConfig("store_address", txtStoreAddress.getText().trim());
        saveConfig("store_phone", txtStorePhone.getText().trim());

        EventBus.publish(new AppDataChangedEvent(AppEventType.SYSTEM_CONFIG, "Store config updated"));
        showMessage("✅ Đã lưu thông tin cửa hàng!", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveThemeSection() throws SQLException {
        saveConfig("theme_mode", cbTheme.getSelectedIndex() == 1 ? "Dark" : "Light");
        EventBus.publish(new AppDataChangedEvent(AppEventType.SYSTEM_CONFIG, "Theme updated"));
        showMessage("✅ Đã lưu giao diện!", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveSecuritySection() {
        String oldPass = new String(txtOldPass.getPassword());
        String newPass = new String(txtNewPass.getPassword());
        String confirmPass = new String(txtConfirmPass.getPassword());

        if (oldPass.isEmpty() && newPass.isEmpty() && confirmPass.isEmpty()) {
            showMessage("Vui lòng nhập thông tin mật khẩu cần đổi.", JOptionPane.WARNING_MESSAGE);
            return;
        }

        changePassword(oldPass, newPass, confirmPass);
    }

    private void saveEmailSection() throws SQLException {
        if (txtEmailSender == null) {
            return;
        }

        saveConfig("email_sender", txtEmailSender.getText().trim());

        String appPassword = txtAppPassword == null ? "" : new String(txtAppPassword.getPassword()).trim();
        if (!appPassword.isEmpty()) {
            saveConfig("email_app_password", appPassword);
            txtAppPassword.setText("");
        }

        EventBus.publish(new AppDataChangedEvent(AppEventType.SYSTEM_CONFIG, "Email config updated"));
        showMessage("✅ Đã lưu cấu hình email!", JOptionPane.INFORMATION_MESSAGE);
    }

    private void saveSettings() {
        saveCurrentSection();
    }

    private void saveConfig(String key, String value) throws SQLException {
        String sql = "MERGE INTO SYSTEM_CONFIG t USING (SELECT ? as k, ? as v FROM dual) s "
                + "ON (t.config_key = s.k) "
                + "WHEN MATCHED THEN UPDATE SET t.config_value = s.v "
                + "WHEN NOT MATCHED THEN INSERT (config_key, config_value) VALUES (s.k, s.v)";

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    private void changePassword(String oldPass, String newPass, String confirmPass) {
        if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
            showMessage("❌ Vui lòng nhập đầy đủ tất cả mật khẩu", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showMessage("❌ Mật khẩu xác nhận không khớp!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (newPass.length() < 6) {
            showMessage("❌ Mật khẩu mới phải có ít nhất 6 ký tự!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Account user = LoginService.getCurrentUser();
        if (user == null || user.getUsername() == null) {
            showMessage("❌ Không tìm thấy tài khoản!", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement("SELECT password FROM ACCOUNTS WHERE username = ? AND is_deleted = 0")) {

            ps.setString(1, user.getUsername());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    showMessage("❌ Tài khoản không tồn tại!", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String storedHash = rs.getString("password");
                if (!PasswordUtils.checkPassword(oldPass, storedHash)) {
                    showMessage("❌ Mật khẩu hiện tại không đúng!", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            try (PreparedStatement updatePs = con.prepareStatement(
                    "UPDATE ACCOUNTS SET password = ?, updated_at = CURRENT_TIMESTAMP WHERE username = ?")) {

                String newHash = PasswordUtils.hashPassword(newPass);
                updatePs.setString(1, newHash);
                updatePs.setString(2, user.getUsername());
                updatePs.executeUpdate();

                txtOldPass.setText("");
                txtNewPass.setText("");
                txtConfirmPass.setText("");

                EventBus.publish(new AppDataChangedEvent(AppEventType.ACCOUNT_SECURITY, "Password changed"));
                showMessage("✅ Đổi mật khẩu thành công!", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (Exception e) {
            showMessage("❌ Lỗi đổi mật khẩu: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void applyTheme() {
        try {
            boolean isDark = cbTheme.getSelectedIndex() == 1;
            UIManager.setLookAndFeel(isDark ? new FlatDarkLaf() : new FlatLightLaf());

            for (Window w : Window.getWindows()) {
                SwingUtilities.updateComponentTreeUI(w);
            }

            saveConfig("theme_mode", isDark ? "Dark" : "Light");
        } catch (Exception e) {
            showMessage("❌ Lỗi thay đổi giao diện: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showSection(String sectionKey) {
        activeSection = sectionKey;
        cardLayout.show(cardPanel, sectionKey);
        updateNavState();
        updateTopBarState();

        if (cardPanel != null) {
            cardPanel.revalidate();
            cardPanel.repaint();
        }
    }

    private void updateTopBarState() {
        if (btnSave == null || lblCurrentSectionHint == null) {
            return;
        }

        switch (activeSection) {
            case STORE_KEY:
                lblCurrentSectionHint.setText(isAdminOnly() ? "Đang chỉnh: Thông tin cửa hàng" : "Chỉ Admin được chỉnh cửa hàng");
                btnSave.setText(isAdminOnly() ? "Lưu cửa hàng" : "Chỉ xem");
                btnSave.setEnabled(isAdminOnly());
                btnSave.setBackground(isAdminOnly() ? COLOR_PRIMARY : new Color(203, 213, 225));
                break;
            case THEME_KEY:
                lblCurrentSectionHint.setText("Đang chỉnh: Giao diện");
                btnSave.setText("Lưu giao diện");
                btnSave.setEnabled(true);
                btnSave.setBackground(COLOR_PRIMARY);
                break;
            case SECURITY_KEY:
                lblCurrentSectionHint.setText("Đang chỉnh: Bảo mật");
                btnSave.setText("Đổi mật khẩu");
                btnSave.setEnabled(true);
                btnSave.setBackground(COLOR_PRIMARY);
                break;
            case EMAIL_KEY:
                lblCurrentSectionHint.setText("Đang chỉnh: Email");
                btnSave.setText("Lưu email");
                btnSave.setEnabled(true);
                btnSave.setBackground(COLOR_PRIMARY);
                break;
            default:
                lblCurrentSectionHint.setText("");
                btnSave.setText("Lưu mục này");
                btnSave.setEnabled(true);
                btnSave.setBackground(COLOR_PRIMARY);
                break;
        }
    }

    private void updateNavState() {
        styleNavButton(btnStore, STORE_KEY);
        styleNavButton(btnTheme, THEME_KEY);
        styleNavButton(btnSecurity, SECURITY_KEY);
        if (btnEmail != null) {
            styleNavButton(btnEmail, EMAIL_KEY);
        }
    }

    private void styleNavButton(JButton button, String sectionKey) {
        if (button == null) {
            return;
        }

        boolean active = sectionKey.equals(activeSection);
        button.setBackground(active ? COLOR_NAV_ACTIVE : COLOR_NAV);
        button.setForeground(active ? Color.WHITE : COLOR_NAV_TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, active ? COLOR_PRIMARY : COLOR_NAV_BORDER),
                new EmptyBorder(12, 14, 12, 14)
        ));
        button.setFont(active ? FONT_NAV.deriveFont(Font.BOLD) : FONT_NAV);
    }

    private void applyStoreEditPermission() {
        boolean admin = isAdminOnly();

        setStoreFieldEditable(txtStoreName, admin);
        setStoreFieldEditable(txtStoreAddress, admin);
        setStoreFieldEditable(txtStorePhone, admin);

        updateTopBarState();
    }

    private void setStoreFieldEditable(JTextField field, boolean editable) {
        if (field == null) {
            return;
        }

        field.setEditable(editable);
        field.setFocusable(editable);
        field.setBackground(editable ? BG_PANEL : new Color(248, 250, 252));
        field.setForeground(editable ? COLOR_TEXT : COLOR_MUTED);
        field.setToolTipText(editable ? null : "Chỉ Admin mới được chỉnh thông tin cửa hàng");
    }

    private Icon createBadgeIcon(String text, Color background) {
        int size = 24;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(background.getRed(), background.getGreen(), background.getBlue(), 220));

            if ("▭".equals(text)) {
                g.fillRoundRect(3, 6, size - 7, size - 12, 5, 5);
                g.setColor(new Color(255, 255, 255, 80));
                g.drawRoundRect(4, 7, size - 9, size - 14, 4, 4);
            } else {
                g.fillRoundRect(0, 0, size - 1, size - 1, 10, 10);
                g.setColor(new Color(255, 255, 255, 45));
                g.drawRoundRect(1, 1, size - 3, size - 3, 8, 8);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Segoe UI", Font.BOLD, 11));
                FontMetrics fm = g.getFontMetrics();
                int x = (size - fm.stringWidth(text)) / 2;
                int y = ((size - fm.getHeight()) / 2) + fm.getAscent() - 1;
                g.drawString(text, x, y);
            }
        } finally {
            g.dispose();
        }
        return new ImageIcon(image);
    }

    private String getCurrentUsername() {
        Account user = LoginService.getCurrentUser();
        return user != null && user.getUsername() != null ? user.getUsername() : "N/A";
    }

    private boolean isAdminOnly() {
        Account user = LoginService.getCurrentUser();
        if (user == null) {
            return false;
        }

        String role = String.valueOf(user.getRole()).toUpperCase();
        return role.contains("ADMIN") || role.contains("R_ADMIN_ALL");
    }

    private boolean isAdminOrWarehouse() {
        Account user = LoginService.getCurrentUser();
        if (user == null) {
            return false;
        }

        String role = String.valueOf(user.getRole()).toUpperCase();
        return role.contains("ADMIN")
                || role.contains("WAREHOUSE")
                || role.contains("VIEW_PROD")
                || role.contains("R_STAFF_VIEW_PROD");
    }

    private void showMessage(String msg, int type) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", type);
    }

    private void setupEventBus() {
        try {
            eventSub = EventBus.subscribe(AppDataChangedEvent.class, e -> {
                if (e != null && e.getType() == AppEventType.SYSTEM_CONFIG) {
                    loadSettings();
                    applyStoreEditPermission();
                }
            });
        } catch (Exception e) {
            System.err.println("Lỗi setup EventBus: " + e.getMessage());
        }
    }

    public void dispose() {
        if (eventSub != null) {
            try {
                eventSub.close();
            } catch (Exception ignored) {
            }
        }
    }
}
