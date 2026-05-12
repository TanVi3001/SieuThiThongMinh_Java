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
 * Unified Settings Panel - Đơn giản, dễ dàng, tự động chuyển đổi theo role
 */
public class UnifiedSettingsPanel extends JPanel {

    private static final Color BG_LIGHT = new Color(248, 249, 252);
    private static final Color BG_WHITE = Color.WHITE;
    private static final Color COLOR_PRIMARY = new Color(99, 102, 241);
    private static final Color COLOR_TEXT = new Color(15, 23, 42);
    private static final Color COLOR_MUTED = new Color(100, 116, 139);
    private static final Color COLOR_BORDER = new Color(226, 232, 240);
    private static final Color COLOR_SUCCESS = new Color(34, 197, 94);
    private static final Color COLOR_ERROR = new Color(239, 68, 68);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 18);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_TEXT = new Font("Segoe UI", Font.PLAIN, 14);

    // UI Components
    private JTabbedPane tabbedPane;
    private JTextField txtStoreName, txtStoreAddress, txtStorePhone;
    private JComboBox<String> cbTheme;
    private JPasswordField txtOldPass, txtNewPass, txtConfirmPass;
    private JTextField txtEmailSender;
    private JPasswordField txtAppPassword;
    private JLabel lblUsername, lblRole;

    private AutoCloseable eventSub;

    public UnifiedSettingsPanel() {
        initUI();
        loadSettings();
        setupEventBus();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(BG_LIGHT);

        // ========== Top Bar ==========
        add(createTopBar(), BorderLayout.NORTH);

        // ========== Main Content ==========
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG_LIGHT);
        main.setBorder(new EmptyBorder(20, 20, 20, 20));

        tabbedPane = new JTabbedPane();
        tabbedPane.setTabPlacement(JTabbedPane.TOP);
        tabbedPane.setFont(FONT_TEXT);
        tabbedPane.setBackground(BG_WHITE);

        // Tab 1: Thông tin cửa hàng
        tabbedPane.addTab("🏢 Thông tin", buildStoreTab());

        // Tab 2: Giao diện
        tabbedPane.addTab("🎨 Giao diện", buildThemeTab());

        // Tab 3: Bảo mật
        tabbedPane.addTab("🔐 Bảo mật", buildSecurityTab());

        // Tab 4: Email (nếu là admin)
        if (isAdminOrWarehouse()) {
            tabbedPane.addTab("📧 Email", buildEmailTab());
        }

        main.add(tabbedPane, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
    }

    private JPanel createTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_WHITE);
        bar.setBorder(new MatteBorder(0, 0, 1, 0, COLOR_BORDER));
        bar.setPreferredSize(new Dimension(0, 60));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        left.setOpaque(false);

        JLabel title = new JLabel("⚙️ Cài đặt hệ thống");
        title.setFont(FONT_TITLE);
        title.setForeground(COLOR_TEXT);
        left.add(title);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        right.setOpaque(false);

        JButton btnSave = createButton("Lưu cài đặt", true);
        btnSave.addActionListener(e -> saveSettings());

        JButton btnRefresh = createButton("Làm mới", false);
        btnRefresh.addActionListener(e -> loadSettings());

        right.add(btnRefresh);
        right.add(btnSave);

        bar.add(left, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildStoreTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_WHITE);
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

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

    private JPanel buildThemeTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_WHITE);
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

        panel.add(sectionHeader("Giao diện hệ thống", "Chọn chế độ hiển thị Sáng hoặc Tối"));

        cbTheme = new JComboBox<>(new String[]{"☀️ Sáng (Light Mode)", "🌙 Tối (Dark Mode)"});
        cbTheme.setFont(FONT_TEXT);
        cbTheme.addActionListener(e -> applyTheme());

        panel.add(fieldRow("Chế độ giao diện", cbTheme));
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel buildSecurityTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_WHITE);
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

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

    private JPanel buildEmailTab() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_WHITE);
        panel.setBorder(new EmptyBorder(30, 40, 30, 40));

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
        panel.setBackground(BG_WHITE);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lTitle = new JLabel(title);
        lTitle.setFont(FONT_TITLE);
        lTitle.setForeground(COLOR_TEXT);

        JLabel lSub = new JLabel(subtitle);
        lSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lSub.setForeground(COLOR_MUTED);

        panel.add(lTitle);
        panel.add(Box.createVerticalStrut(4));
        panel.add(lSub);
        panel.add(Box.createVerticalStrut(20));

        return panel;
    }

    private JPanel fieldRow(String label, JComponent component) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_WHITE);
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
        c.setBackground(BG_WHITE);
        if (c instanceof JComboBox) {
            ((JComboBox<?>) c).setBackground(BG_WHITE);
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
            btn.setBorder(BorderFactory.createRaisedBevelBorder());
        } else {
            btn.setBackground(BG_WHITE);
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
                        txtEmailSender.setText(val != null ? val : "");
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
