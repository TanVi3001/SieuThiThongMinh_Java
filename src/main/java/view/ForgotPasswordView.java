/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package view;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities; // THÊM IMPORT

/**
 *
 * @author nguye
 */
public class ForgotPasswordView extends javax.swing.JFrame {

    private javax.swing.JTextField txtOTP;
    private javax.swing.JPasswordField txtNewPass;
    private javax.swing.JPasswordField txtConfirmPass;
    private javax.swing.JButton btnSendOTP; // Lưu biến nút bấm

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ForgotPasswordView.class.getName());
    /**
     * Creates new form ForgotPasswordView
     */

    private String usernameFromLogin; // Biến lưu tên đăng nhập từ LoginView truyền sang

    public ForgotPasswordView(String username) {
        this.usernameFromLogin = username;
        initComponents();
        setupModernUI();
    }

    public ForgotPasswordView() {
        this(""); // Gọi lại constructor ở trên với chuỗi rỗng
    }

    private void setupModernUI() {
        this.getContentPane().removeAll();
        this.getContentPane().setLayout(new java.awt.GridBagLayout());
        this.getContentPane().setBackground(java.awt.Color.WHITE);

        javax.swing.JPanel cardPanel = new javax.swing.JPanel(null);
        cardPanel.setBackground(java.awt.Color.WHITE);
        cardPanel.setPreferredSize(new java.awt.Dimension(450, 680)); // Tăng chiều cao để chứa form dài

        java.awt.Color navyBlue = new java.awt.Color(44, 62, 80);

        // Header & Subtitle
        javax.swing.JLabel lblTitle = new javax.swing.JLabel("QUÊN MẬT KHẨU", javax.swing.SwingConstants.CENTER);
        lblTitle.setFont(new java.awt.Font("Segoe UI", Font.BOLD, 28));
        lblTitle.setForeground(navyBlue);
        lblTitle.setBounds(50, 40, 350, 40);
        cardPanel.add(lblTitle);

        javax.swing.JLabel lblSubtitle = new javax.swing.JLabel("<html><center>Nhập Email để nhận mã OTP và Username<br>(Mã có hiệu lực trong 5 phút)</center></html>", javax.swing.SwingConstants.CENTER);
        lblSubtitle.setFont(new java.awt.Font("Segoe UI", Font.PLAIN, 12));
        lblSubtitle.setBounds(50, 85, 350, 40);
        cardPanel.add(lblSubtitle);

        // --- Ô NHẬP EMAIL ---
        txtUserEmail.setBounds(80, 150, 290, 45);
        txtUserEmail.putClientProperty("JComponent.roundRect", true);
        txtUserEmail.putClientProperty("JTextField.placeholderText", "Email đã đăng ký...");
        cardPanel.add(txtUserEmail);

        btnSendOTP = new javax.swing.JButton("GỬI MÃ XÁC MINH") {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(76, 175, 80)); // Màu xanh lá "Gửi mã"
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btnSendOTP.setBounds(80, 205, 290, 40);
        btnSendOTP.setForeground(Color.WHITE);
        btnSendOTP.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSendOTP.setContentAreaFilled(false);
        btnSendOTP.setBorderPainted(false);
        btnSendOTP.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSendOTP.addActionListener(e -> handleSendOTP());
        cardPanel.add(btnSendOTP);

        // --- Ô NHẬP OTP ---
        txtOTP = new javax.swing.JTextField();
        txtOTP.setBounds(80, 275, 290, 45);
        txtOTP.putClientProperty("JComponent.roundRect", true);
        txtOTP.putClientProperty("JTextField.placeholderText", "Nhập mã OTP 6 số...");
        cardPanel.add(txtOTP);

        // --- Ô NHẬP MẬT KHẨU MỚI ---
        txtNewPass = new javax.swing.JPasswordField();
        txtNewPass.setBounds(80, 345, 290, 45);
        txtNewPass.putClientProperty("JComponent.roundRect", true);
        txtNewPass.putClientProperty("JPasswordField.showRevealButton", true);
        txtNewPass.putClientProperty("JTextField.placeholderText", "Mật khẩu mới...");
        cardPanel.add(txtNewPass);

        txtConfirmPass = new javax.swing.JPasswordField();
        txtConfirmPass.setBounds(80, 405, 290, 45);
        txtConfirmPass.putClientProperty("JComponent.roundRect", true);
        txtConfirmPass.putClientProperty("JTextField.placeholderText", "Xác nhận mật khẩu...");
        cardPanel.add(txtConfirmPass);

        // --- NÚT XÁC NHẬN ĐỔI PASS ---
        javax.swing.JButton btnConfirm = new javax.swing.JButton("XÁC NHẬN THAY ĐỔI") {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(navyBlue);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 40, 40);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btnConfirm.setBounds(80, 480, 290, 50);
        btnConfirm.setForeground(Color.WHITE);
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnConfirm.setContentAreaFilled(false);
        btnConfirm.setBorderPainted(false);
        btnConfirm.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConfirm.addActionListener(e -> handleResetPassword());
        cardPanel.add(btnConfirm);

        // Quay lại
        javax.swing.JLabel lblBack = new javax.swing.JLabel("Quay lại đăng nhập", javax.swing.SwingConstants.CENTER);
        lblBack.setBounds(150, 550, 150, 20);
        lblBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblBack.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                new LoginView().setVisible(true);
                dispose();
            }
        });
        cardPanel.add(lblBack);

        this.getContentPane().add(cardPanel, new java.awt.GridBagConstraints());
        this.setSize(500, 750);
        this.setLocationRelativeTo(null);
        this.setResizable(false);
    }

    /**
     * Logic xử lý gửi mail
     */
    /**
     * Bước 1: Tìm Username và gửi OTP
     */
    private void handleSendOTP() {
        String userEmail = txtUserEmail.getText().trim();
        if (userEmail.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập Email!");
            return;
        }

        // 1. Dò tìm Username từ Database dựa trên Email
        String foundUsername = business.sql.rbac.AccountSql.getInstance().findUsernameByEmail(userEmail);

        if (foundUsername != null) {
            // 2. Tạo OTP ngẫu nhiên
            String otp = String.valueOf(new java.util.Random().nextInt(900000) + 100000);

            // 3. Lưu OTP vào CSDL (Hết hạn sau 5 phút)
            business.sql.rbac.AccountSql.getInstance().saveOTP(userEmail, otp);

            // Vô hiệu hóa nút và đổi text để người dùng biết máy đang xử lý
            btnSendOTP.setEnabled(false);
            btnSendOTP.setText("Đang gửi mail...");

            // Chạy tiến trình gửi mail ở luồng riêng (Tránh đơ máy)
            new Thread(() -> {
                boolean mailSent = business.service.EmailService.sendPasswordRecoveryOTP(userEmail, foundUsername, otp);

                SwingUtilities.invokeLater(() -> {
                    if (mailSent) {
                        JOptionPane.showMessageDialog(this, "Mã OTP và Tên đăng nhập đã được gửi vào Email của bạn!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                        btnSendOTP.setText("Gửi lại mã OTP");
                    } else {
                        JOptionPane.showMessageDialog(this, "Lỗi gửi mail! Vui lòng kiểm tra lại kết nối mạng.", "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
                        btnSendOTP.setText("GỬI MÃ XÁC MINH");
                    }
                    btnSendOTP.setEnabled(true);
                });
            }).start();
        } else {
            JOptionPane.showMessageDialog(this, "Email này không tồn tại trong hệ thống!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Bước 2: Kiểm tra OTP và Cập nhật mật khẩu mới
     */
    private void handleResetPassword() {
        String email = txtUserEmail.getText().trim();
        String otpInput = txtOTP.getText().trim();
        String newPass = new String(txtNewPass.getPassword());
        String confirmPass = new String(txtConfirmPass.getPassword());

        if (otpInput.isEmpty() || newPass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ OTP và mật khẩu mới!");
            return;
        }
        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu xác nhận không khớp!");
            return;
        }

        // Kiểm tra OTP trong DB (Khớp và chưa hết hạn)
        boolean isValid = business.sql.rbac.AccountSql.getInstance().validateOTP(email, otpInput);

        if (isValid) {
            // Cập nhật pass mới
            business.sql.rbac.AccountSql.getInstance().updatePasswordByEmail(email, newPass);
            JOptionPane.showMessageDialog(this, "Đổi mật khẩu thành công! Bạn hãy dùng Username trong mail để đăng nhập.");
            new LoginView().setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Mã OTP không đúng hoặc đã hết hạn!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        LoginView = new javax.swing.JPanel();
        Login = new javax.swing.JLabel();
        Username = new javax.swing.JLabel();
        txtUserEmail = new javax.swing.JTextField();
        btnLogin = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setBackground(new java.awt.Color(255, 255, 255));

        LoginView.setBackground(new java.awt.Color(44, 62, 80));
        LoginView.setForeground(new java.awt.Color(255, 255, 255));

        Login.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        Login.setForeground(new java.awt.Color(255, 255, 255));
        Login.setText("NHẬP EMAIL");

        Username.setFont(new java.awt.Font("Segoe UI", 1, 15)); // NOI18N
        Username.setForeground(new java.awt.Color(255, 255, 255));
        Username.setText("EMAIL");

        txtUserEmail.addActionListener(this::txtUserEmailActionPerformed);

        btnLogin.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnLogin.setText("XÁC NHẬN");
        btnLogin.addActionListener(this::btnLoginActionPerformed);

        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Vui lòng nhập email để chúng tôi có thể gửi thông tin cho bạn");

        javax.swing.GroupLayout LoginViewLayout = new javax.swing.GroupLayout(LoginView);
        LoginView.setLayout(LoginViewLayout);
        LoginViewLayout.setHorizontalGroup(
                LoginViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(LoginViewLayout.createSequentialGroup()
                                .addGroup(LoginViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(LoginViewLayout.createSequentialGroup()
                                                .addGap(43, 43, 43)
                                                .addComponent(jLabel1))
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, LoginViewLayout.createSequentialGroup()
                                                .addGap(65, 65, 65)
                                                .addComponent(Username)
                                                .addGap(18, 18, 18)
                                                .addComponent(txtUserEmail)))
                                .addContainerGap(49, Short.MAX_VALUE))
                        .addGroup(LoginViewLayout.createSequentialGroup()
                                .addGroup(LoginViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(LoginViewLayout.createSequentialGroup()
                                                .addGap(157, 157, 157)
                                                .addComponent(btnLogin))
                                        .addGroup(LoginViewLayout.createSequentialGroup()
                                                .addGap(150, 150, 150)
                                                .addComponent(Login)))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        LoginViewLayout.setVerticalGroup(
                LoginViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(LoginViewLayout.createSequentialGroup()
                                .addGap(46, 46, 46)
                                .addComponent(Login)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel1)
                                .addGap(18, 18, 18)
                                .addGroup(LoginViewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(txtUserEmail, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(Username, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(29, 29, 29)
                                .addComponent(btnLogin)
                                .addContainerGap(35, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(LoginView, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(LoginView, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>                        

    private void txtUserEmailActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException ex) {
            logger.log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> new ForgotPasswordView().setVisible(true));
    }

    // Variables declaration - do not modify                     
    private javax.swing.JLabel Login;
    private javax.swing.JPanel LoginView;
    private javax.swing.JLabel Username;
    private javax.swing.JButton btnLogin;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JTextField txtUserEmail;
    // End of variables declaration                   
}
