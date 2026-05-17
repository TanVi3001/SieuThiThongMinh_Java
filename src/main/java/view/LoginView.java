package view;

import business.api.AccountActivationAPI;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import common.realtime.RealtimeClient;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import model.account.ActivationEmployeeInfo;

public class LoginView extends JFrame {

    private static final Logger logger = Logger.getLogger(LoginView.class.getName());

    // Thay block hằng số kích thước trong LoginView.java bằng block này
// để LoginView và ForgotPasswordView có cùng kích thước.
    private static final int FRAME_WIDTH = 1050;
    private static final int FRAME_HEIGHT = 788;

    private static final int CARD_X = 605;
    private static final int CARD_Y = 74;
    private static final int CARD_WIDTH = 410;
    private static final int CARD_HEIGHT = 635;

    private static final int BG_CROP_TOP = 45;

    private static final int FORM_PADDING_X = 54;
    private static final int INPUT_WIDTH = 302;
    private static final int LOGIN_INPUT_HEIGHT = 49;
    private static final int REGISTER_INPUT_HEIGHT = 39;

    private static final Color NAVY = new Color(7, 27, 77);
    private static final Color ORANGE = new Color(255, 90, 0);
    private static final Color ORANGE_LIGHT = new Color(255, 117, 20);
    private static final Color TEXT_MUTED = new Color(107, 120, 149);
    private static final Color BORDER = new Color(221, 227, 238);

    private final AccountActivationAPI activationAPI = new AccountActivationAPI();

    private BackgroundPanel rootPanel;
    private ShadowCardPanel cardPanel;
    private JPanel loginPanel;
    private JPanel registerPanel;

    private InputField usernameField;
    private InputField passwordField;
    private PrimaryButton btnLogin;

    private InputField txtCode;
    private InputField txtFullName;
    private InputField txtEmail;
    private InputField txtPhone;
    private InputField txtRegUsername;
    private InputField txtRegPassword;
    private JLabel lblResendEmail;
    private JLabel[] registerLabels;
    private PrimaryButton btnCheckCode;
    private PrimaryButton btnRegister;
    private ActivationEmployeeInfo currentEmp;

    public LoginView() {
        initFrame();
        buildUi();
        bindLoginEnterKeys();
    }

    private void initFrame() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Smart Supermarket");
        setPreferredSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setMinimumSize(new Dimension(FRAME_WIDTH, FRAME_HEIGHT));
        setResizable(false);
    }

    private void buildUi() {
        rootPanel = new BackgroundPanel();
        rootPanel.setLayout(null);
        setContentPane(rootPanel);

        cardPanel = new ShadowCardPanel();
        cardPanel.setLayout(new BorderLayout());
        rootPanel.add(cardPanel);

        loginPanel = buildLoginPanel();
        registerPanel = buildRegisterPanel();
        showLoginPanel();

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildLoginPanel() {
        JPanel panel = createTransparentPanel();
        panel.setLayout(null);

        int x = FORM_PADDING_X;
        int y = 82;

        JLabel title = label("Đăng nhập", 30, Font.BOLD, NAVY);
        title.setBounds(x, y, INPUT_WIDTH, 38);
        panel.add(title);

        y += 48;
        JLabel subtitle = label("Vui lòng nhập thông tin tài khoản", 13, Font.PLAIN, TEXT_MUTED);
        subtitle.setBounds(x, y, INPUT_WIDTH, 18);
        panel.add(subtitle);

        y += 42;
        panel.add(fieldLabel("Tên đăng nhập", x, y, INPUT_WIDTH));

        y += 22;
        usernameField = new InputField("user", false, "Nhập username", LOGIN_INPUT_HEIGHT);
        usernameField.setBounds(x, y, INPUT_WIDTH, LOGIN_INPUT_HEIGHT);
        panel.add(usernameField);

        y += LOGIN_INPUT_HEIGHT + 24;
        panel.add(fieldLabel("Mật khẩu", x, y, INPUT_WIDTH));

        y += 22;
        passwordField = new InputField("lock", true, "Nhập mật khẩu", LOGIN_INPUT_HEIGHT);
        passwordField.setBounds(x, y, INPUT_WIDTH, LOGIN_INPUT_HEIGHT);
        panel.add(passwordField);

        y += LOGIN_INPUT_HEIGHT + 18;
        JPanel rememberRow = createTransparentPanel();
        rememberRow.setLayout(null);
        rememberRow.setBounds(x, y, INPUT_WIDTH, 18);

        JLabel forgot = linkLabel("Quên mật khẩu?");
        forgot.setBounds(INPUT_WIDTH - 130, 0, 130, 18);
        forgot.setHorizontalAlignment(SwingConstants.RIGHT);
        forgot.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                ForgotPasswordView fp = new ForgotPasswordView(usernameField.getText().trim());
                fp.setVisible(true);
                dispose();
            }
        });
        rememberRow.add(forgot);
        panel.add(rememberRow);

        y += 42;
        btnLogin = new PrimaryButton("Đăng nhập  →");
        btnLogin.setBounds(x, y, INPUT_WIDTH, 50);
        btnLogin.addActionListener(evt -> btnLoginActionPerformed());
        panel.add(btnLogin);

        y += 68;
        panel.add(createDivider(x, y, INPUT_WIDTH));

        y += 30;
        OutlineButton registerButton = new OutlineButton("Đăng ký tài khoản", "user-plus");
        registerButton.setBounds(x, y, INPUT_WIDTH, 48);
        registerButton.addActionListener(evt -> showRegisterPanel());
        panel.add(registerButton);

        return panel;
    }

    private JPanel buildRegisterPanel() {
        JPanel wrapper = createTransparentPanel();
        wrapper.setLayout(new BorderLayout());

        JPanel panel = createTransparentPanel();
        panel.setLayout(null);
        panel.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));

        int x = FORM_PADDING_X;
        int y = 40;

        JLabel title = label("Đăng ký", 30, Font.BOLD, NAVY);
        title.setBounds(x, y, INPUT_WIDTH, 38);
        panel.add(title);

        y += 44;
        JLabel subtitle = label("Tạo tài khoản để sử dụng hệ thống", 13, Font.PLAIN, TEXT_MUTED);
        subtitle.setBounds(x, y, INPUT_WIDTH, 18);
        panel.add(subtitle);

        txtCode = new InputField("key", false, "Nhập mã kích hoạt", REGISTER_INPUT_HEIGHT);
        txtFullName = new InputField("user", false, "Tự động điền...", REGISTER_INPUT_HEIGHT);
        txtEmail = new InputField("mail", false, "Tự động điền...", REGISTER_INPUT_HEIGHT);
        txtPhone = new InputField("phone", false, "Tự động điền...", REGISTER_INPUT_HEIGHT);
        txtRegUsername = new InputField("user", false, "Nhập username của bạn", REGISTER_INPUT_HEIGHT);
        txtRegPassword = new InputField("lock", true, "Nhập mật khẩu mới", REGISTER_INPUT_HEIGHT);

        InputField[] fields = {txtCode, txtFullName, txtEmail, txtPhone, txtRegUsername, txtRegPassword};
        String[] labels = {"Mã kích hoạt (*)", "Họ và tên", "Email", "Số điện thoại", "Tên đăng nhập", "Mật khẩu"};
        registerLabels = new JLabel[labels.length];

        y += 32;
        for (int i = 0; i < labels.length; i++) {
            registerLabels[i] = fieldLabel(labels[i], x, y, INPUT_WIDTH);
            panel.add(registerLabels[i]);

            y += 18;
            fields[i].setBounds(x, y, INPUT_WIDTH, REGISTER_INPUT_HEIGHT);
            panel.add(fields[i]);
            y += REGISTER_INPUT_HEIGHT + 13;
        }

        btnCheckCode = new PrimaryButton("Kiểm tra mã  →");
        btnCheckCode.setBounds(x, 188, INPUT_WIDTH, 45);
        btnCheckCode.addActionListener(evt -> checkActivationCode());
        panel.add(btnCheckCode);

        lblResendEmail = centeredHtml("");
        lblResendEmail.setBounds(x, 191, INPUT_WIDTH, 14);
        lblResendEmail.setVisible(false);
        panel.add(lblResendEmail);

        btnRegister = new PrimaryButton("Đăng ký  →");
        btnRegister.setBounds(x, 430, INPUT_WIDTH, 42);
        btnRegister.addActionListener(evt -> activateAccount());
        panel.add(btnRegister);

        JLabel back = centeredHtml(
                "<span style='color:#6B7895'>Đã có tài khoản? </span>"
                + "<span style='color:#FF5A00'><u>Quay lại đăng nhập</u></span>");
        back.setBounds(x, CARD_HEIGHT - 95, INPUT_WIDTH, 24);
        back.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showLoginPanel();
            }
        });
        panel.add(back);

        JScrollPane scrollPane = new JScrollPane(panel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        wrapper.add(scrollPane, BorderLayout.CENTER);

        resetRegisterStage1();
        return wrapper;
    }

    private void showLoginPanel() {
        cardPanel.removeAll();
        cardPanel.add(loginPanel, BorderLayout.CENTER);
        cardPanel.revalidate();
        cardPanel.repaint();
        SwingUtilities.invokeLater(() -> usernameField.requestFocusInWindow());
    }

    private void showRegisterPanel() {
        resetRegisterStage1();
        cardPanel.removeAll();
        cardPanel.add(registerPanel, BorderLayout.CENTER);
        cardPanel.revalidate();
        cardPanel.repaint();
        SwingUtilities.invokeLater(() -> txtCode.requestFocusInWindow());
    }

    private void bindLoginEnterKeys() {
        usernameField.addActionListener(evt -> passwordField.requestFocusInWindow());
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent evt) {
                if (evt.getKeyCode() == KeyEvent.VK_ENTER && btnLogin.isEnabled()) {
                    btnLogin.doClick();
                }
            }
        });
    }

    private void btnLoginActionPerformed() {
        String user = usernameField.getText().trim();
        char[] passwordChars = passwordField.getPassword();
        String pass = new String(passwordChars);

        common.security.SecurityGuard.setProcessingLogout(false);

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nhập đủ tài khoản/mật khẩu!");
            return;
        }

        btnLogin.setEnabled(false);

        new Thread(() -> {
            try {
                model.account.Account acc = business.service.LoginService.authenticate(user, pass);

                SwingUtilities.invokeLater(() -> {
                    if (acc != null) {
                        common.auth.UserSession.getInstance().createUserSession(
                                acc.getAccountId(), acc.getUsername(), acc.getRoleValue());

                        if (business.service.AuthorizationService.isAdmin(acc)) {
                            new AdminDashboardView().setVisible(true);
                        } else if (business.service.AuthorizationService.isWarehouseStaff(acc)) {
                            new WarehouseDashboardView().setVisible(true);
                        } else {
                            new DashboardView().setVisible(true);
                        }
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Sai tài khoản hoặc mật khẩu!");
                        btnLogin.setEnabled(true);
                    }
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Lỗi kết nối DB: " + e.getMessage());
                    btnLogin.setEnabled(true);
                });
            } finally {
                Arrays.fill(passwordChars, '\0');
            }
        }, "login-auth-thread").start();
    }

    private void resetRegisterStage1() {
        currentEmp = null;
        if (txtCode == null) {
            return;
        }

        txtCode.setText("");
        txtFullName.setText("");
        txtEmail.setText("");
        txtPhone.setText("");
        txtRegUsername.setText("");
        txtRegPassword.setText("");

        txtCode.setEditable(true);
        txtCode.setFieldBackground(Color.WHITE);

        InputField[] fields = {txtFullName, txtEmail, txtPhone, txtRegUsername, txtRegPassword};
        for (InputField field : fields) {
            field.setVisible(false);
            field.setEditable(true);
            field.setFieldBackground(Color.WHITE);
        }

        for (int i = 1; i < registerLabels.length; i++) {
            registerLabels[i].setVisible(false);
        }

        btnCheckCode.setVisible(true);
        lblResendEmail.setVisible(false);
        btnRegister.setVisible(false);
    }

    private void advanceToRegisterStage2(ActivationEmployeeInfo emp) {
        currentEmp = emp;

        txtCode.setEditable(false);
        txtCode.setFieldBackground(new Color(246, 248, 252));
        txtFullName.setText(emp.getFullName());
        txtEmail.setText(emp.getEmail());
        txtPhone.setText(emp.getPhone());

        InputField[] readonlyFields = {txtFullName, txtEmail, txtPhone};
        for (InputField field : readonlyFields) {
            field.setEditable(false);
            field.setFieldBackground(new Color(246, 248, 252));
        }

        InputField[] fields = {txtFullName, txtEmail, txtPhone, txtRegUsername, txtRegPassword};
        for (InputField field : fields) {
            field.setVisible(true);
        }

        for (int i = 1; i < registerLabels.length; i++) {
            registerLabels[i].setVisible(true);
        }

        btnCheckCode.setVisible(false);
        lblResendEmail.setVisible(false);
        btnRegister.setVisible(true);
        txtRegUsername.requestFocusInWindow();
    }

    private void checkActivationCode() {
        String code = txtCode.getText().trim();
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mã kích hoạt từ email!");
            return;
        }

        btnCheckCode.setEnabled(false);
        try {
            ActivationEmployeeInfo emp = activationAPI.check(code);
            advanceToRegisterStage2(emp);
        } catch (Exception ex) {
            logger.log(Level.WARNING, "Activation code check failed", ex);
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } finally {
            btnCheckCode.setEnabled(true);
        }
    }

    private void activateAccount() {
        if (currentEmp == null) {
            return;
        }

        String user = txtRegUsername.getText().trim();
        String pass = new String(txtRegPassword.getPassword());
        String code = txtCode.getText().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ Username và Password!");
            return;
        }

        btnRegister.setEnabled(false);
        try {
            activationAPI.activate(code, user, pass);
            RealtimeClient.send("ACCOUNT_SECURITY_CHANGED");
            RealtimeClient.send("EMPLOYEES_CHANGED");
            EventBus.publish(new AppDataChangedEvent(AppEventType.ACCOUNT_SECURITY, "REGISTER_SUCCESS"));

            JOptionPane.showMessageDialog(this, "Kích hoạt thành công! Đăng nhập ngay.");
            showLoginPanel();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        } finally {
            btnRegister.setEnabled(true);
        }
    }

    private void handleChangeEmailAndResend() {
        JTextField txtU = new JTextField();
        JTextField txtE = new JTextField();
        Object[] msg = {
            "Nhập Tên Đăng Nhập (Do quản lý cấp):", txtU,
            "Nhập Email mới muốn nhận mã:", txtE
        };

        int option = JOptionPane.showConfirmDialog(
                this,
                msg,
                "Cập nhật Email & Nhận mã mới",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (option != JOptionPane.OK_OPTION) {
            return;
        }

        String username = txtU.getText().trim();
        String newEmail = txtE.getText().trim();

        if (username.isEmpty() || newEmail.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (isEmailDuplicated(newEmail)) {
            JOptionPane.showMessageDialog(
                    this,
                    "Email này đã tồn tại trên hệ thống (kể cả tài khoản đang bị khóa)!\n"
                    + "Chỉ khi Admin xóa cứng tài khoản cũ thì mới được sử dụng lại Email này.",
                    "Lỗi trùng Email",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String newOtpCode = String.format("%06d", new Random().nextInt(999999));
        boolean success = updateEmailAndIssueNewOTP(username, newEmail, newOtpCode);

        if (success) {
            new Thread(() -> {
                boolean mailSent = business.service.EmailService.sendActivationEmail(newEmail, username, newOtpCode);
                if (!mailSent) {
                    System.err.println("Không thể gửi mail tới: " + newEmail);
                }
            }, "activation-email-thread").start();

            JOptionPane.showMessageDialog(
                    this,
                    "Đã cập nhật Email và gửi mã OTP mới thành công!\nVui lòng kiểm tra hộp thư của bạn.",
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(
                    this,
                    "Tên đăng nhập không tồn tại, đang bị khóa hoặc tài khoản này đã được kích hoạt trước đó!",
                    "Từ chối",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isEmailDuplicated(String email) {
        String sql = "SELECT COUNT(*) FROM ACCOUNTS WHERE email = ?";
        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot check duplicated email", e);
        }
        return false;
    }

    private boolean updateEmailAndIssueNewOTP(String username, String newEmail, String newOtpCode) {
        String sql = "UPDATE ACCOUNTS SET email = ?, otp_code = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE username = ? AND NVL(is_active, 0) = 0 AND NVL(is_deleted, 0) = 0";
        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newEmail);
            ps.setString(2, newOtpCode);
            ps.setString(3, username);

            int rows = ps.executeUpdate();
            if (rows > 0) {
                String sqlSyncEmp = "UPDATE EMPLOYEES SET email = ? "
                        + "WHERE employee_id = (SELECT user_id FROM ACCOUNTS WHERE username = ?)";
                try (PreparedStatement ps2 = con.prepareStatement(sqlSyncEmp)) {
                    ps2.setString(1, newEmail);
                    ps2.setString(2, username);
                    ps2.executeUpdate();
                }
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot update activation email", e);
        }
        return false;
    }

    private JLabel fieldLabel(String text, int x, int y, int width) {
        JLabel label = label(text, 12, Font.BOLD, NAVY);
        label.setBounds(x, y, width, 16);
        return label;
    }

    private JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private JLabel linkLabel(String text) {
        JLabel label = label(text, 11, Font.BOLD, ORANGE);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return label;
    }

    private JLabel centeredHtml(String body) {
        JLabel label = new JLabel("<html><div style='text-align:center'>" + body + "</div></html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return label;
    }

    private JPanel createDivider(int x, int y, int width) {
        JPanel divider = createTransparentPanel();
        divider.setLayout(null);
        divider.setBounds(x, y, width, 26);

        JLabel left = new JLabel();
        left.setOpaque(true);
        left.setBackground(BORDER);
        left.setBounds(0, 9, 120, 1);
        divider.add(left);

        JLabel text = label("hoặc", 11, Font.PLAIN, TEXT_MUTED);
        text.setHorizontalAlignment(SwingConstants.CENTER);
        text.setBounds(132, 0, 46, 20);
        divider.add(text);

        JLabel right = new JLabel();
        right.setOpaque(true);
        right.setBackground(BORDER);
        right.setBounds(width - 120, 9, 120, 1);
        divider.add(right);

        return divider;
    }

    private JPanel createTransparentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    public static void main(String args[]) {
        int deletedNow = business.sql.rbac.TokenSql.getInstance().deleteExpiredTokens();
        System.out.println("STARTUP CLEANUP deleted = " + deletedNow);

        business.service.TokenCleanupService.start();
        common.sync.SyncWatcher.start(2);
        common.realtime.RealtimeServer.tryStart(8887);
        common.realtime.RealtimeClient.connect("ws://127.0.0.1:8887");

        java.awt.EventQueue.invokeLater(() -> {
            LoginView login = new LoginView();
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });
    }

    private final class BackgroundPanel extends JPanel {

        private final Image background;

        private BackgroundPanel() {
            setOpaque(true);
            background = loadBackground();
        }

        @Override
        public void doLayout() {
            double sx = getWidth() / (double) FRAME_WIDTH;
            double sy = getHeight() / (double) FRAME_HEIGHT;
            cardPanel.setBounds(
                    (int) Math.round(CARD_X * sx),
                    (int) Math.round(CARD_Y * sy),
                    (int) Math.round(CARD_WIDTH * sx),
                    (int) Math.round(CARD_HEIGHT * sy));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            if (background != null) {
                int imageW = background.getWidth(this);
                int imageH = background.getHeight(this);
                if (imageW > 0 && imageH > 0) {
                    int sourceH = imageH - BG_CROP_TOP;
                    double scale = Math.max(getWidth() / (double) imageW, getHeight() / (double) sourceH);
                    int drawW = (int) Math.ceil(imageW * scale);
                    int drawH = (int) Math.ceil(sourceH * scale);
                    int drawX = (getWidth() - drawW) / 2;
                    int drawY = (getHeight() - drawH) / 2;
                    g2.drawImage(background,
                            drawX, drawY, drawX + drawW, drawY + drawH,
                            0, BG_CROP_TOP, imageW, imageH,
                            this);
                }
            } else {
                g2.setColor(new Color(245, 248, 252));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
        }

        private Image loadBackground() {
            URL url = LoginView.class.getResource("/image/bg.png");
            if (url == null) {
                logger.warning("Không tìm thấy background resource: /image/bg.png");
                return null;
            }
            return new javax.swing.ImageIcon(url).getImage();
        }
    }

    private static final class ShadowCardPanel extends JPanel {

        private ShadowCardPanel() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int arc = 22;
            int inset = 7;
            for (int i = 0; i < 9; i++) {
                float alpha = 0.075f - (i * 0.0045f);
                if (alpha <= 0) {
                    continue;
                }
                g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
                g2.setColor(new Color(18, 34, 70));
                int grow = i;
                g2.fillRoundRect(inset - grow / 2, inset + 5 - grow / 2,
                        getWidth() - inset * 2 + grow, getHeight() - inset * 2 + grow,
                        arc + grow, arc + grow);
            }

            g2.setComposite(AlphaComposite.SrcOver);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(inset, inset, getWidth() - inset * 2, getHeight() - inset * 2, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        public Insets getInsets() {
            return new Insets(7, 7, 7, 7);
        }
    }

    private static final class InputField extends JPanel {

        private final JTextField field;
        private final boolean password;
        private Color fieldBackground = Color.WHITE;

        private InputField(String iconType, boolean password, String placeholder, int height) {
            this.password = password;
            setLayout(null);
            setOpaque(false);
            setPreferredSize(new Dimension(INPUT_WIDTH, height));

            IconView icon = new IconView(iconType);
            icon.setBounds(10, 0, 28, height);
            add(icon);

            if (password) {
                JPasswordField pass = new JPasswordField();
                pass.setEchoChar('•');
                field = pass;
                JButton reveal = new EyeButton(pass);
                reveal.setBounds(INPUT_WIDTH - 42, 0, 34, height);
                add(reveal);
            } else {
                field = new JTextField();
            }

            field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, password ? 34 : 8));
            field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            field.setForeground(NAVY);
            field.setOpaque(false);
            field.putClientProperty("JTextField.placeholderText", placeholder);
            field.setBounds(44, 1, INPUT_WIDTH - 54, height - 2);
            add(field);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fieldBackground);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1.4f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            g2.dispose();
            super.paintComponent(g);
        }

        private String getText() {
            return field.getText();
        }

        private void setText(String text) {
            field.setText(text);
        }

        private char[] getPassword() {
            if (field instanceof JPasswordField pass) {
                return pass.getPassword();
            }
            return field.getText().toCharArray();
        }

        private void addActionListener(java.awt.event.ActionListener listener) {
            field.addActionListener(listener);
        }

        private void addKeyListener(KeyAdapter adapter) {
            field.addKeyListener(adapter);
        }

        private void requestFocusInWindowLater() {
            field.requestFocusInWindow();
        }

        @Override
        public boolean requestFocusInWindow() {
            return field.requestFocusInWindow();
        }

        private void setEditable(boolean editable) {
            field.setEditable(editable);
        }

        private void setFieldBackground(Color color) {
            fieldBackground = color;
            repaint();
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            field.setEnabled(enabled);
        }
    }

    private static final class PrimaryButton extends JButton {

        private boolean hovered;

        private PrimaryButton(String text) {
            super(text);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(Color.WHITE);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
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
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color start = isEnabled() ? (hovered ? ORANGE_LIGHT : ORANGE) : new Color(180, 188, 205);
            Color end = isEnabled() ? new Color(255, 106, 0) : new Color(180, 188, 205);
            g2.setPaint(new java.awt.GradientPaint(0, 0, start, getWidth(), 0, end));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class OutlineButton extends JButton {

        private final String iconType;
        private boolean hovered;

        private OutlineButton(String text, String iconType) {
            super(text);
            this.iconType = iconType;
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setForeground(NAVY);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setIcon(null);
            setIconTextGap(0);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setVerticalTextPosition(SwingConstants.CENTER);
            setHorizontalTextPosition(SwingConstants.CENTER);
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
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(hovered ? new Color(255, 249, 245) : Color.WHITE);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1.3f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 6, 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private static final class VectorIcon implements javax.swing.Icon {

        private final String type;
        private final int width;
        private final int height;
        private final Color color;

        private VectorIcon(String type, int width, int height, Color color) {
            this.type = type;
            this.width = width;
            this.height = height;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawSmallIcon(g2, type, x, y + 1, color);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return width;
        }

        @Override
        public int getIconHeight() {
            return height;
        }
    }

    private static final class EyeButton extends JButton {

        private final JPasswordField passwordField;
        private boolean visible;

        private EyeButton(JPasswordField passwordField) {
            this.passwordField = passwordField;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addActionListener(evt -> {
                visible = !visible;
                passwordField.setEchoChar(visible ? (char) 0 : '•');
                repaint();
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            drawEye(g2, getWidth() / 2, getHeight() / 2, visible ? ORANGE : TEXT_MUTED);
            g2.dispose();
        }
    }

    private static final class IconView extends JComponent {

        private final String type;

        private IconView(String type) {
            this.type = type;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int iconH = switch (type) {
                case "lock" ->
                    21;
                case "user", "user-plus" ->
                    21;
                default ->
                    19;
            };
            int y = (getHeight() - iconH) / 2;
            drawSmallIcon(g2, type, 1, y, TEXT_MUTED);
            g2.dispose();
        }
    }

    private static void drawSmallIcon(Graphics2D g2, String type, int x, int y, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (type) {
            case "lock" -> {
                g2.drawRoundRect(x + 4, y + 9, 15, 12, 3, 3);
                g2.drawArc(x + 7, y + 1, 9, 14, 0, 180);
                g2.fillOval(x + 10, y + 14, 3, 3);
            }
            case "user-plus" -> {
                g2.drawOval(x + 4, y + 2, 9, 9);
                g2.drawArc(x, y + 12, 18, 12, 15, 150);
                g2.drawLine(x + 20, y + 6, x + 20, y + 16);
                g2.drawLine(x + 15, y + 11, x + 25, y + 11);
            }
            case "key" -> {
                g2.drawOval(x + 2, y + 6, 9, 9);
                g2.drawLine(x + 11, y + 11, x + 23, y + 11);
                g2.drawLine(x + 19, y + 11, x + 19, y + 16);
            }
            case "mail" -> {
                g2.drawRoundRect(x + 2, y + 5, 20, 14, 3, 3);
                g2.drawLine(x + 3, y + 6, x + 12, y + 13);
                g2.drawLine(x + 21, y + 6, x + 12, y + 13);
            }
            case "phone" -> {
                g2.drawRoundRect(x + 6, y + 1, 12, 21, 4, 4);
                g2.drawLine(x + 10, y + 18, x + 14, y + 18);
            }
            default -> {
                g2.drawOval(x + 7, y + 2, 9, 9);
                g2.drawArc(x + 2, y + 13, 20, 12, 20, 140);
            }
        }
    }

    private static void drawEye(Graphics2D g2, int cx, int cy, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        RoundRectangle2D eye = new RoundRectangle2D.Double(cx - 10, cy - 5, 20, 10, 12, 12);
        g2.draw(eye);
        g2.fillOval(cx - 3, cy - 3, 6, 6);
    }
}
