package view;

import business.api.AccountActivationAPI;
import model.account.ActivationEmployeeInfo;
import common.realtime.RealtimeClient;
import common.events.EventBus;
import common.events.AppEventType;
import common.events.AppDataChangedEvent;
import business.service.EmailService; // 🌟 Import dịch vụ mail của bạn

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class RegisterView extends javax.swing.JFrame {

    private JTextField txtCode, txtFullName, txtEmail, txtPhone, txtUsername;
    private JPasswordField txtPass;
    private JButton btnCheck, btnReg;
    private JLabel lblResendEmail;
    private JLabel[] labels = new JLabel[6];

    private static final java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger(RegisterView.class.getName());

    private final AccountActivationAPI activationAPI = new AccountActivationAPI();
    private ActivationEmployeeInfo currentEmp;

    public RegisterView() {
        initComponents();
        setupModernUI();
    }

    private void setupModernUI() {
        this.getContentPane().removeAll();
        this.getContentPane().setLayout(new GridBagLayout());
        this.getContentPane().setBackground(Color.WHITE);

        JPanel cardPanel = new JPanel(null);
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setPreferredSize(new Dimension(450, 700));

        // --- HEADER ---
        JLabel lblTitle = new JLabel("Kích hoạt tài khoản");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(new Color(20, 30, 50));
        lblTitle.setBounds(95, 40, 300, 40);
        cardPanel.add(lblTitle);

        JLabel lblSub = new JLabel("Nhập mã kích hoạt được gửi qua Email");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(new Color(163, 174, 208));
        lblSub.setBounds(110, 80, 250, 20);
        cardPanel.add(lblSub);

        // --- INPUT ---
        txtCode = new JTextField();
        txtFullName = new JTextField();
        txtEmail = new JTextField();
        txtPhone = new JTextField();
        txtUsername = new JTextField();
        txtPass = new JPasswordField();

        JTextField[] fields = {txtCode, txtFullName, txtEmail, txtPhone, txtUsername, txtPass};
        String[] titleLabels = {"MÃ KÍCH HOẠT (*)", "HỌ VÀ TÊN", "EMAIL", "SỐ ĐIỆN THOẠI", "TÊN ĐĂNG NHẬP", "MẬT KHẨU MỚI"};
        String[] placeholders = {"VD: E01...", "Tự động điền...", "Tự động điền...", "Tự động điền...", "Nhập username của bạn", "********"};

        int startY = 120;
        int gap = 70;

        for (int i = 0; i < titleLabels.length; i++) {
            labels[i] = new JLabel(titleLabels[i]);
            labels[i].setFont(new Font("Segoe UI", Font.BOLD, 11));
            labels[i].setForeground(new Color(44, 62, 80));
            labels[i].setBounds(100, startY + (i * gap), 200, 20);
            cardPanel.add(labels[i]);

            fields[i].setBounds(100, startY + (i * gap) + 22, 250, 38);
            fields[i].setFont(new Font("Segoe UI", Font.PLAIN, 14));
            fields[i].putClientProperty("JComponent.roundRect", true);
            fields[i].putClientProperty("JTextField.placeholderText", placeholders[i]);
            fields[i].putClientProperty("JTextField.padding", new Insets(0, 12, 0, 12));

            if (fields[i] instanceof JPasswordField jPasswordField) {
                jPasswordField.putClientProperty("JPasswordField.showRevealButton", true);
            }
            cardPanel.add(fields[i]);
        }

        // --- BUTTONS ---
        btnCheck = createButton("KIỂM TRA MÃ");
        btnCheck.setBounds(100, 220, 250, 45);
        cardPanel.add(btnCheck);

        // 🌟 NÚT: CẬP NHẬT LẠI EMAIL VÀ GỬI MÃ MỚI
        lblResendEmail = new JLabel("<html><u>Sai Email? Cập nhật & Gửi lại mã</u></html>", SwingConstants.CENTER);
        lblResendEmail.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        lblResendEmail.setForeground(new Color(67, 97, 238));
        lblResendEmail.setBounds(100, 275, 250, 20);
        lblResendEmail.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblResendEmail.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                handleChangeEmailAndResend();
            }
        });
        cardPanel.add(lblResendEmail);

        btnReg = createButton("HOÀN TẤT KÍCH HOẠT");
        btnReg.setBackground(new Color(0, 168, 140));
        btnReg.setBounds(100, startY + (6 * gap), 250, 48);
        cardPanel.add(btnReg);

        // --- BACK LINK ---
        JLabel lblBack = new JLabel("Trở về Đăng nhập", SwingConstants.CENTER);
        lblBack.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblBack.setForeground(new Color(44, 62, 80));
        lblBack.setBounds(125, startY + (6 * gap) + 60, 200, 20);
        lblBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblBack.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                new LoginView().setVisible(true);
                dispose();
            }
        });
        cardPanel.add(lblBack);

        this.getContentPane().add(cardPanel, new GridBagConstraints());
        this.setSize(500, 750);
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        resetToStage1();
        initEvents();
    }

    private void resetToStage1() {
        currentEmp = null;
        txtCode.setText("");
        txtFullName.setText("");
        txtEmail.setText("");
        txtPhone.setText("");
        txtUsername.setText("");
        txtPass.setText("");

        for (int i = 1; i < 6; i++) {
            labels[i].setVisible(false);
            if (i == 1) {
                txtFullName.setVisible(false);
            }
            if (i == 2) {
                txtEmail.setVisible(false);
            }
            if (i == 3) {
                txtPhone.setVisible(false);
            }
            if (i == 4) {
                txtUsername.setVisible(false);
            }
            if (i == 5) {
                txtPass.setVisible(false);
            }
        }

        btnReg.setVisible(false);
        btnCheck.setVisible(true);
        lblResendEmail.setVisible(true);
        txtCode.setEditable(true);
        txtCode.setBackground(Color.WHITE);
    }

    private void advanceToStage2(ActivationEmployeeInfo emp) {
        currentEmp = emp;
        txtCode.setEditable(false);
        txtCode.setBackground(new Color(245, 245, 245));

        txtFullName.setText(emp.getFullName());
        txtFullName.setEditable(false);
        txtFullName.setBackground(new Color(245, 245, 245));

        txtEmail.setText(emp.getEmail());
        txtEmail.setEditable(false);
        txtEmail.setBackground(new Color(245, 245, 245));

        txtPhone.setText(emp.getPhone());
        txtPhone.setEditable(false);
        txtPhone.setBackground(new Color(245, 245, 245));

        for (int i = 1; i < 6; i++) {
            labels[i].setVisible(true);
            if (i == 1) {
                txtFullName.setVisible(true);
            }
            if (i == 2) {
                txtEmail.setVisible(true);
            }
            if (i == 3) {
                txtPhone.setVisible(true);
            }
            if (i == 4) {
                txtUsername.setVisible(true);
            }
            if (i == 5) {
                txtPass.setVisible(true);
            }
        }

        btnCheck.setVisible(false);
        lblResendEmail.setVisible(false);
        btnReg.setVisible(true);
        txtUsername.requestFocus();
    }

    // =========================================================================
    // 🌟 TEST CASE 1 & 2: CẬP NHẬT MAIL, GỬI LẠI MÃ VÀ KHÓA TÀI KHOẢN ĐÃ CẤP
    // =========================================================================
    private void handleChangeEmailAndResend() {
        JTextField txtU = new JTextField();
        JTextField txtE = new JTextField();
        Object[] msg = {
            "Nhập Tên Đăng Nhập (Do quản lý cấp):", txtU,
            "Nhập Email mới muốn nhận mã:", txtE
        };

        int option = JOptionPane.showConfirmDialog(this, msg, "Cập nhật Email & Nhận mã mới", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            String username = txtU.getText().trim();
            String newEmail = txtE.getText().trim();

            if (username.isEmpty() || newEmail.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (isEmailDuplicated(newEmail)) {
                JOptionPane.showMessageDialog(this, "Email này đã tồn tại trên hệ thống (Kể cả tài khoản đang bị khóa)!\nChỉ khi Admin xóa cứng tài khoản cũ thì mới được sử dụng lại Email này.", "Lỗi trùng Email", JOptionPane.ERROR_MESSAGE);
                return;
            }

            String newOtpCode = String.format("%06d", new java.util.Random().nextInt(999999));
            boolean success = updateEmailAndIssueNewOTP(username, newEmail, newOtpCode);

            if (success) {
                // 🌟 ĐÃ FIX: Chạy luồng phụ (Thread) để gọi EmailService, tránh làm đơ giao diện khi đợi gửi mail
                new Thread(() -> {
                    boolean mailSent = business.service.EmailService.sendActivationEmail(newEmail, username, newOtpCode);
                    if (!mailSent) {
                        System.err.println("❌ Không thể gửi mail tới: " + newEmail);
                    }
                }).start();

                JOptionPane.showMessageDialog(this, "Đã cập nhật Email và gửi mã OTP mới thành công!\nVui lòng kiểm tra hộp thư của bạn.", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Tên đăng nhập không tồn tại, đang bị khóa hoặc tài khoản này ĐÃ ĐƯỢC KÍCH HOẠT trước đó!", "Từ chối", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private boolean isEmailDuplicated(String email) {
        // Query toàn bộ bảng ACCOUNTS, không lọc is_deleted
        String sql = "SELECT COUNT(*) FROM ACCOUNTS WHERE email = ?";
        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean updateEmailAndIssueNewOTP(String username, String newEmail, String newOtpCode) {
        // 🔒 LOCK LOGIC: Chỉ Update nếu is_active = 0 và is_deleted = 0
        String sql = "UPDATE ACCOUNTS SET email = ?, otp_code = ?, updated_at = CURRENT_TIMESTAMP "
                + "WHERE username = ? AND NVL(is_active, 0) = 0 AND NVL(is_deleted, 0) = 0";
        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection(); java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, newEmail);
            ps.setString(2, newOtpCode);
            ps.setString(3, username);

            int rows = ps.executeUpdate();

            // Nếu update được bảng ACCOUNTS, phải đồng bộ sang bảng EMPLOYEES luôn để dữ liệu khớp
            if (rows > 0) {
                String sqlSyncEmp = "UPDATE EMPLOYEES SET email = ? WHERE employee_id = (SELECT user_id FROM ACCOUNTS WHERE username = ?)";
                try (java.sql.PreparedStatement ps2 = con.prepareStatement(sqlSyncEmp)) {
                    ps2.setString(1, newEmail);
                    ps2.setString(2, username);
                    ps2.executeUpdate();
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    // =========================================================================

    private void initEvents() {
        btnCheck.addActionListener(e -> {
            String code = txtCode.getText().trim();
            if (code.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập Mã Kích Hoạt từ email!");
                return;
            }
            btnCheck.setEnabled(false);
            try {
                ActivationEmployeeInfo emp = activationAPI.check(code);
                if (emp != null) {
                    advanceToStage2(emp);
                } else {
                    JOptionPane.showMessageDialog(this, "Mã không hợp lệ hoặc đã dùng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                logger.severe(ex.toString());
            } finally {
                btnCheck.setEnabled(true);
            }
        });

        btnReg.addActionListener(e -> {
            if (currentEmp == null) {
                return;
            }

            String user = txtUsername.getText().trim();
            String pass = new String(txtPass.getPassword());
            String code = txtCode.getText().trim();

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đủ Username và Password!");
                return;
            }

            btnReg.setEnabled(false);
            try {
                activationAPI.activate(code, user, pass);
                RealtimeClient.send("ACCOUNT_SECURITY_CHANGED");
                RealtimeClient.send("EMPLOYEES_CHANGED");
                EventBus.publish(new AppDataChangedEvent(AppEventType.ACCOUNT_SECURITY, "REGISTER_SUCCESS"));

                JOptionPane.showMessageDialog(this, "Kích hoạt thành công! Đăng nhập ngay.");
                new LoginView().setVisible(true);
                this.dispose();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            } finally {
                btnReg.setEnabled(true);
            }
        });
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 35, 35);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(44, 62, 80));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        pack();
    }

}
