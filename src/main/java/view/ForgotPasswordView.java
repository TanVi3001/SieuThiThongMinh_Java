package view;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;
import java.util.Arrays;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class ForgotPasswordView extends JFrame {

    private static final java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger(ForgotPasswordView.class.getName());

    /*
     * Giữ cùng kích thước cửa sổ với LoginView bản lớn.
     * Card của quên mật khẩu cao hơn một chút để không bị cắt nút cuối.
     */
    private static final int FRAME_WIDTH = 1050;
    private static final int FRAME_HEIGHT = 788;

    private static final int CARD_X = 605;
    private static final int CARD_Y = 45;
    private static final int CARD_WIDTH = 410;
    private static final int CARD_HEIGHT = 700;

    private static final int BG_CROP_TOP = 45;

    private static final int FORM_PADDING_X = 54;
    private static final int INPUT_WIDTH = 302;
    private static final int INPUT_HEIGHT = 49;

    private static final Color NAVY = new Color(7, 27, 77);
    private static final Color ORANGE = new Color(255, 90, 0);
    private static final Color ORANGE_LIGHT = new Color(255, 117, 20);
    private static final Color GREEN = new Color(77, 190, 84);
    private static final Color GREEN_LIGHT = new Color(85, 231, 94);
    private static final Color TEXT_MUTED = new Color(107, 120, 149);
    private static final Color BORDER = new Color(221, 227, 238);

    private final String usernameFromLogin;

    private BackgroundPanel rootPanel;
    private ShadowCardPanel cardPanel;

    private InputField txtUserEmail;
    private InputField txtOTP;
    private InputField txtNewPass;
    private InputField txtConfirmPass;

    private PrimaryButton btnSendOTP;
    private PrimaryButton btnConfirm;

    public ForgotPasswordView(String username) {
        this.usernameFromLogin = username == null ? "" : username;
        initFrame();
        buildUi();
    }

    public ForgotPasswordView() {
        this("");
    }

    private void initFrame() {
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setTitle("Smart Supermarket - Quên mật khẩu");
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

        JPanel forgotPanel = buildForgotPanel();
        cardPanel.add(forgotPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
        SwingUtilities.invokeLater(() -> txtUserEmail.requestFocusInWindow());
    }

    private JPanel buildForgotPanel() {
        JPanel panel = createTransparentPanel();
        panel.setLayout(null);

        int x = FORM_PADDING_X;
        int y = 55;

        JLabel title = label("Quên mật khẩu", 30, Font.BOLD, NAVY);
        title.setBounds(x, y, INPUT_WIDTH, 40);
        panel.add(title);

        y += 46;
        JLabel subtitle = label("Nhập Email để nhận mã OTP và Username", 12, Font.PLAIN, TEXT_MUTED);
        subtitle.setBounds(x, y, INPUT_WIDTH, 18);
        panel.add(subtitle);

        y += 22;
        JLabel subtitle2 = label("Mã xác minh có hiệu lực trong 5 phút", 12, Font.PLAIN, TEXT_MUTED);
        subtitle2.setBounds(x, y, INPUT_WIDTH, 18);
        panel.add(subtitle2);

        y += 36;
        panel.add(fieldLabel("Email đã đăng ký", x, y, INPUT_WIDTH));

        y += 18;
        txtUserEmail = new InputField("mail", false, "Nhập email đã đăng ký...", INPUT_HEIGHT);
        txtUserEmail.setBounds(x, y, INPUT_WIDTH, INPUT_HEIGHT);
        panel.add(txtUserEmail);

        y += INPUT_HEIGHT + 16;
        btnSendOTP = new PrimaryButton("Gửi mã xác minh", true);
        btnSendOTP.setBounds(x, y, INPUT_WIDTH, 46);
        btnSendOTP.addActionListener(e -> handleSendOTP());
        panel.add(btnSendOTP);

        y += 64;
        panel.add(fieldLabel("Mã OTP", x, y, INPUT_WIDTH));

        y += 18;
        txtOTP = new InputField("key", false, "Nhập mã OTP 6 số...", INPUT_HEIGHT);
        txtOTP.setBounds(x, y, INPUT_WIDTH, INPUT_HEIGHT);
        panel.add(txtOTP);

        y += INPUT_HEIGHT + 18;
        panel.add(fieldLabel("Mật khẩu mới", x, y, INPUT_WIDTH));

        y += 18;
        txtNewPass = new InputField("lock", true, "Nhập mật khẩu mới...", INPUT_HEIGHT);
        txtNewPass.setBounds(x, y, INPUT_WIDTH, INPUT_HEIGHT);
        panel.add(txtNewPass);

        y += INPUT_HEIGHT + 18;
        panel.add(fieldLabel("Xác nhận mật khẩu", x, y, INPUT_WIDTH));

        y += 18;
        txtConfirmPass = new InputField("lock", true, "Nhập lại mật khẩu mới...", INPUT_HEIGHT);
        txtConfirmPass.setBounds(x, y, INPUT_WIDTH, INPUT_HEIGHT);
        panel.add(txtConfirmPass);

        y += INPUT_HEIGHT + 25;
        btnConfirm = new PrimaryButton("Xác nhận thay đổi  →", false);
        btnConfirm.setBounds(x, y, INPUT_WIDTH, 48);
        btnConfirm.addActionListener(e -> handleResetPassword());
        panel.add(btnConfirm);

        y += 59;
        JLabel back = centeredHtml(
                "<span style='color:#6B7895'>Đã nhớ mật khẩu? </span>"
                + "<span style='color:#FF5A00'><u>Quay lại đăng nhập</u></span>");
        back.setBounds(x, y, INPUT_WIDTH, 24);
        back.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                new LoginView().setVisible(true);
                dispose();
            }
        });
        panel.add(back);

        return panel;
    }

    private void handleSendOTP() {
        String userEmail = txtUserEmail.getText().trim();

        if (userEmail.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Email!");
            return;
        }

        String foundUsername = business.sql.rbac.AccountSql.getInstance().findUsernameByEmail(userEmail);

        if (foundUsername == null) {
            JOptionPane.showMessageDialog(this, "Email này không tồn tại trong hệ thống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String otp = String.valueOf(new java.util.Random().nextInt(900000) + 100000);
        business.sql.rbac.AccountSql.getInstance().saveOTP(userEmail, otp);

        btnSendOTP.setEnabled(false);
        btnSendOTP.setText("Đang gửi mail...");

        new Thread(() -> {
            boolean mailSent = business.service.EmailService.sendPasswordRecoveryOTP(userEmail, foundUsername, otp);

            SwingUtilities.invokeLater(() -> {
                if (mailSent) {
                    JOptionPane.showMessageDialog(
                            this,
                            "Mã OTP và Tên đăng nhập đã được gửi vào Email của bạn!",
                            "Thành công",
                            JOptionPane.INFORMATION_MESSAGE);
                    btnSendOTP.setText("Gửi lại mã OTP");
                } else {
                    JOptionPane.showMessageDialog(
                            this,
                            "Lỗi gửi mail! Vui lòng kiểm tra lại kết nối mạng.",
                            "Lỗi hệ thống",
                            JOptionPane.ERROR_MESSAGE);
                    btnSendOTP.setText("Gửi mã xác minh");
                }
                btnSendOTP.setEnabled(true);
            });
        }, "forgot-password-send-otp-thread").start();
    }

    private void handleResetPassword() {
        String email = txtUserEmail.getText().trim();
        String otpInput = txtOTP.getText().trim();
        char[] newPassChars = txtNewPass.getPassword();
        char[] confirmPassChars = txtConfirmPass.getPassword();

        String newPass = new String(newPassChars);
        String confirmPass = new String(confirmPassChars);

        try {
            if (email.isEmpty() || otpInput.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ Email, OTP và mật khẩu mới!");
                return;
            }

            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp!");
                return;
            }

            boolean isValid = business.sql.rbac.AccountSql.getInstance().validateOTP(email, otpInput);

            if (isValid) {
                business.sql.rbac.AccountSql.getInstance().updatePasswordByEmail(email, newPass);
                JOptionPane.showMessageDialog(
                        this,
                        "Đổi mật khẩu thành công! Bạn hãy dùng Username trong mail để đăng nhập.");
                new LoginView().setVisible(true);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Mã OTP không đúng hoặc đã hết hạn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        } finally {
            Arrays.fill(newPassChars, '\0');
            Arrays.fill(confirmPassChars, '\0');
        }
    }

    private JLabel fieldLabel(String text, int x, int y, int width) {
        JLabel label = label(text, 11, Font.BOLD, NAVY);
        label.setBounds(x, y, width, 15);
        return label;
    }

    private JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        return label;
    }

    private JLabel centeredHtml(String body) {
        JLabel label = new JLabel("<html><div style='text-align:center'>" + body + "</div></html>");
        label.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return label;
    }

    private JPanel createTransparentPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        return panel;
    }

    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new ForgotPasswordView().setVisible(true));
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
            URL url = ForgotPasswordView.class.getResource("/image/bg.png");
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

            int arc = 20;
            int inset = 7;

            for (int i = 0; i < 10; i++) {
                float alpha = 0.08f - (i * 0.005f);
                if (alpha <= 0) {
                    continue;
                }
                g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
                g2.setColor(new Color(18, 34, 70));
                int grow = i;
                g2.fillRoundRect(
                        inset - grow / 2,
                        inset + 5 - grow / 2,
                        getWidth() - inset * 2 + grow,
                        getHeight() - inset * 2 + grow,
                        arc + grow,
                        arc + grow);
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
        private Color fieldBackground = Color.WHITE;

        private InputField(String iconType, boolean password, String placeholder, int height) {
            setLayout(null);
            setOpaque(false);
            setPreferredSize(new Dimension(INPUT_WIDTH, height));

            IconView icon = new IconView(iconType);
            icon.setBounds(9, 0, 28, height);
            add(icon);

            if (password) {
                JPasswordField pass = new JPasswordField();
                pass.setEchoChar('•');
                field = pass;

                JButton reveal = new EyeButton(pass);
                reveal.setBounds(INPUT_WIDTH - 38, 0, 31, height);
                add(reveal);
            } else {
                field = new JTextField();
            }

            field.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, password ? 28 : 8));
            field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            field.setForeground(NAVY);
            field.setOpaque(false);
            field.putClientProperty("JTextField.placeholderText", placeholder);
            field.setBounds(43, 1, INPUT_WIDTH - 52, height - 2);
            add(field);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(fieldBackground);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

            g2.dispose();
            super.paintComponent(g);
        }

        private String getText() {
            return field.getText();
        }

        private char[] getPassword() {
            if (field instanceof JPasswordField pass) {
                return pass.getPassword();
            }
            return field.getText().toCharArray();
        }

        @Override
        public boolean requestFocusInWindow() {
            return field.requestFocusInWindow();
        }
    }

    private static final class PrimaryButton extends JButton {

        private final boolean greenStyle;
        private boolean hovered;

        private PrimaryButton(String text, boolean greenStyle) {
            super(text);
            this.greenStyle = greenStyle;

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

            Color start;
            Color end;

            if (!isEnabled()) {
                start = new Color(180, 188, 205);
                end = new Color(180, 188, 205);
            } else if (greenStyle) {
                start = hovered ? GREEN_LIGHT : GREEN;
                end = hovered ? new Color(80, 225, 90) : new Color(88, 229, 96);
            } else {
                start = hovered ? ORANGE_LIGHT : ORANGE;
                end = new Color(255, 106, 0);
            }

            g2.setPaint(new java.awt.GradientPaint(0, 0, start, getWidth(), 0, end));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            g2.dispose();
            super.paintComponent(g);
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
                case "lock", "mail", "key" ->
                    24;
                default ->
                    22;
            };

            int y = (getHeight() - iconH) / 2;
            drawSmallIcon(g2, type, 1, y, TEXT_MUTED);

            g2.dispose();
        }
    }

    private static void drawSmallIcon(Graphics2D g2, String type, int x, int y, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        switch (type) {
            case "lock" -> {
                g2.drawRoundRect(x + 4, y + 10, 17, 13, 3, 3);
                g2.drawArc(x + 7, y + 1, 11, 15, 0, 180);
                g2.fillOval(x + 11, y + 15, 3, 3);
            }
            case "key" -> {
                g2.drawOval(x + 2, y + 7, 10, 10);
                g2.drawLine(x + 12, y + 12, x + 25, y + 12);
                g2.drawLine(x + 21, y + 12, x + 21, y + 17);
            }
            case "mail" -> {
                g2.drawRoundRect(x + 2, y + 5, 23, 16, 3, 3);
                g2.drawLine(x + 3, y + 6, x + 13, y + 14);
                g2.drawLine(x + 24, y + 6, x + 13, y + 14);
            }
            default -> {
                g2.drawOval(x + 7, y + 2, 10, 10);
                g2.drawArc(x + 2, y + 14, 23, 13, 20, 140);
            }
        }
    }

    private static void drawEye(Graphics2D g2, int cx, int cy, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        RoundRectangle2D eye = new RoundRectangle2D.Double(cx - 10, cy - 5, 20, 10, 12, 12);
        g2.draw(eye);
        g2.fillOval(cx - 3, cy - 3, 6, 6);
    }
}
