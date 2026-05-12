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
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.sql.*;

/**
 * Unified Settings Panel - menu bên trái, nội dung bên phải
 */
public class UnifiedSettingsPanel extends JPanel {

    private static final Color BG_APP = new Color(241, 245, 249);
    private static final Color BG_PANEL = Color.WHITE;
    private static final Color COLOR_PRIMARY = new Color(79, 70, 229);
    private static final Color COLOR_PRIMARY_SOFT = new Color(224, 231, 255);
    private static final Color COLOR_TEXT = new Color(15, 23, 42);
    private static final Color COLOR_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_NAV = new Color(248, 250, 252);

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
    private String activeSection = STORE_KEY;

    private AutoCloseable eventSub;

    public UnifiedSettingsPanel() {
        initUI();
        loadSettings();
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

        JLabel title = new JLabel("⚙️ Cài đặt hệ thống");
        title.setFont(FONT_TITLE);
        title.setForeground(COLOR_TEXT);

        JLabel subtitle = new JLabel("Chọn chức năng bên trái, xem nội dung bên phải");
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

        JButton btnRefresh = createActionButton("Làm mới", false);
        btnRefresh.addActionListener(e -> loadSettings());

        JButton btnSave = createActionButton("Lưu cài đặt", true);
        btnSave.addActionListener(e -> saveSettings());

        right.add(btnRefresh);
        right.add(btnSave);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel createBody() {
        JPanel body = new JPanel(new BorderLayout(16, 0));
        body.setBackground(BG_APP);
        body.setBorder(new EmptyBorder(16, 16, 16, 16));

        body.add(createNavPanel(), BorderLayout.WEST);
        body.add(createContentShell(), BorderLayout.CENTER);
        return body;
    }

    private JPanel createNavPanel() {
        JPanel nav = new JPanel();
        nav.setPreferredSize(new Dimension(240, 0));
        nav.setBackground(COLOR_NAV);
        nav.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, COLOR_BORDER),
                new EmptyBorder(18, 14, 18, 14)
        ));
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));

        JLabel group = new JLabel("Chức năng");
        group.setFont(FONT_SECTION);
        group.setForeground(COLOR_TEXT);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel("Chọn mục để hiển thị thông tin");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(COLOR_MUTED);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);

        nav.add(group);
        nav.add(Box.createVerticalStrut(4));
        nav.add(hint);
        nav.add(Box.createVerticalStrut(18));

        btnStore = createNavButton("🏢 Thông tin cửa hàng", STORE_KEY);
        btnTheme = createNavButton("🎨 Giao diện", THEME_KEY);
        btnSecurity = createNavButton("🔐 Bảo mật", SECURITY_KEY);
        nav.add(btnStore);
        nav.add(Box.createVerticalStrut(10));
        nav.add(btnTheme);
        nav.add(Box.createVerticalStrut(10));
        nav.add(btnSecurity);

        if (isAdminOrWarehouse()) {
            nav.add(Box.createVerticalStrut(10));
            btnEmail = createNavButton("📧 Email", EMAIL_KEY);
            nav.add(btnEmail);
        }

        nav.add(Box.createVerticalGlue());

        JLabel note = new JLabel("Màu xanh là mục đang chọn");
        note.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        note.setForeground(COLOR_MUTED);
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
                new EmptyBorder(22, 24, 24, 24)
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

    private JButton createNavButton(String text, String sectionKey) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_NAV);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, COLOR_BORDER),
                new EmptyBorder(12, 14, 12, 14)
        ));
        btn.setBackground(BG_PANEL);
        btn.setForeground(COLOR_TEXT);
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> showSection(sectionKey));
        return btn;
    }

    private JPanel buildStoreSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        panel.add(sectionHeader("Thông tin cửa hàng", "Hiển thị trên hóa đơn và báo cáo"));

        txtStoreName = new JTextField();
        txtStoreAddress = new JTextField();
        txtStorePhone = new JTextField();

        panel.add(fieldRow("Tên siêu thị/Cửa hàng", txtStoreName));
        panel.add(Box.createVerticalStrut(16));
        panel.add(fieldRow("Địa chỉ", txtStoreAddress));
        panel.add(Box.createVerticalStrut(16));
        panel.add(fieldRow("Số điện thoại", txtStorePhone));

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildThemeSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        panel.add(sectionHeader("Giao diện hệ thống", "Chọn chế độ hiển thị Sáng hoặc Tối"));

        cbTheme = new JComboBox<>(new String[]{"☀️ Sáng (Light Mode)", "🌙 Tối (Dark Mode)"});
        cbTheme.setFont(FONT_TEXT);
        cbTheme.addActionListener(e -> applyTheme());

        panel.add(fieldRow("Chế độ giao diện", cbTheme));
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildSecuritySection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        panel.add(sectionHeader("Bảo mật tài khoản", "Đổi mật khẩu để bảo vệ tài khoản"));

        lblUsername = new JLabel("Tài khoản: " + getCurrentUsername());
        lblUsername.setFont(FONT_TEXT);
        lblUsername.setForeground(COLOR_MUTED);
        panel.add(lblUsername);
        panel.add(Box.createVerticalStrut(20));

        txtOldPass = new JPasswordField();
        txtNewPass = new JPasswordField();
        txtConfirmPass = new JPasswordField();

        panel.add(fieldRow("Mật khẩu hiện tại", txtOldPass));
        panel.add(Box.createVerticalStrut(16));
        panel.add(fieldRow("Mật khẩu mới", txtNewPass));
        panel.add(Box.createVerticalStrut(16));
        panel.add(fieldRow("Xác nhận mật khẩu mới", txtConfirmPass));

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildEmailSection() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(0, 0, 0, 0));

        panel.add(sectionHeader("Cấu hình Email", "Thiết lập gửi email từ hệ thống"));

        txtEmailSender = new JTextField();
        txtAppPassword = new JPasswordField();

        panel.add(fieldRow("Email gửi", txtEmailSender));
        panel.add(Box.createVerticalStrut(16));
        panel.add(fieldRow("App Password / API Key", txtAppPassword));

        JLabel hint = new JLabel("💡 Để trống để bỏ qua cấu hình email");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(COLOR_MUTED);
        panel.add(Box.createVerticalStrut(12));
        panel.add(hint);

        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel sectionHeader(String title, String subtitle) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setBorder(new EmptyBorder(0, 0, 18, 0));

        JLabel lTitle = new JLabel(title);
        lTitle.setFont(FONT_SECTION);
        lTitle.setForeground(COLOR_TEXT);

        JLabel lSub = new JLabel(subtitle);
        lSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lSub.setForeground(COLOR_MUTED);

        panel.add(lTitle);
        panel.add(Box.createVerticalStrut(4));
        panel.add(lSub);
        panel.add(Box.createVerticalStrut(12));

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

    private JButton createButton(String text, boolean isPrimary) {
        JButton btn = new JButton(text);
        btn.setFont(FONT_TEXT);
        btn.setPreferredSize(new Dimension(120, 36));
        btn.setFocusPainted(false);

        if (isPrimary) {
            btn.setBackground(COLOR_PRIMARY);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    new MatteBorder(1, 1, 1, 1, COLOR_PRIMARY),
                    new EmptyBorder(8, 14, 8, 14)
            ));
        } else {
            btn.setBackground(BG_PANEL);
            btn.setForeground(COLOR_TEXT);
            btn.setBorder(BorderFactory.createLineBorder(COLOR_BORDER));
        }

        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void loadSettings() {
        try (Connection con = DatabaseConnection.getConnection();
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT config_key, config_value FROM SYSTEM_CONFIG")) {

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

        // Clear password fields
        if (txtOldPass != null) {
            txtOldPass.setText("");
            txtNewPass.setText("");
            txtConfirmPass.setText("");
        }
    }

    private void saveSettings() {
        try {
            saveConfig("store_name", txtStoreName.getText().trim());
            saveConfig("store_address", txtStoreAddress.getText().trim());
            saveConfig("store_phone", txtStorePhone.getText().trim());
            saveConfig("theme_mode", cbTheme.getSelectedIndex() == 1 ? "Dark" : "Light");

            if (txtEmailSender != null && !txtEmailSender.getText().trim().isEmpty()) {
                saveConfig("email_sender", txtEmailSender.getText().trim());
            }

            EventBus.publish(new AppDataChangedEvent(AppEventType.SYSTEM_CONFIG, "Updated"));

            // Handle password change
            String oldPass = new String(txtOldPass.getPassword());
            String newPass = new String(txtNewPass.getPassword());
            String confirmPass = new String(txtConfirmPass.getPassword());

            if (!oldPass.isEmpty() || !newPass.isEmpty() || !confirmPass.isEmpty()) {
                changePassword(oldPass, newPass, confirmPass);
            }

            showMessage("✅ Đã lưu cài đặt thành công!", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            showMessage("❌ Lỗi: " + e.getMessage(), JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveConfig(String key, String value) throws SQLException {
        String sql = "MERGE INTO SYSTEM_CONFIG t USING (SELECT ? as k, ? as v FROM dual) s " +
                "ON (t.config_key = s.k) " +
                "WHEN MATCHED THEN UPDATE SET t.config_value = s.v " +
                "WHEN NOT MATCHED THEN INSERT (config_key, config_value) VALUES (s.k, s.v)";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
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

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT password FROM ACCOUNTS WHERE username = ? AND is_deleted = 0")) {

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
        button.setBackground(active ? COLOR_PRIMARY_SOFT : BG_PANEL);
        button.setForeground(active ? COLOR_PRIMARY : COLOR_TEXT);
        button.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 1, 1, 1, active ? COLOR_PRIMARY : COLOR_BORDER),
                new EmptyBorder(12, 14, 12, 14)
        ));
    }

    private String getCurrentUsername() {
        Account user = LoginService.getCurrentUser();
        return user != null && user.getUsername() != null ? user.getUsername() : "N/A";
    }

    private boolean isAdminOrWarehouse() {
        Account user = LoginService.getCurrentUser();
        if (user == null) return false;
        String role = String.valueOf(user.getRole());
        return role.contains("ADMIN") || role.contains("WAREHOUSE") || role.contains("Warehouse");
    }

    private void showMessage(String msg, int type) {
        JOptionPane.showMessageDialog(this, msg, "Thông báo", type);
    }

    private void setupEventBus() {
        try {
            eventSub = EventBus.subscribe(AppDataChangedEvent.class, e -> {
                if (e != null && e.getType() == AppEventType.SYSTEM_CONFIG) {
                    loadSettings();
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
