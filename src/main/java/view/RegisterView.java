package view;

import business.api.AccountActivationAPI;
import model.account.ActivationEmployeeInfo;

import javax.swing.*;
import java.awt.*;

public class RegisterView extends javax.swing.JFrame {

    private JTextField txtCode, txtFullName, txtEmail, txtPhone, txtUsername;
    private JPasswordField txtPass;
    private JButton btnCheck, btnReg;
    private JLabel[] labels = new JLabel[6];

    private static final java.util.logging.Logger logger
            = java.util.logging.Logger.getLogger(RegisterView.class.getName());

    // API backend (Vĩ)
    private final AccountActivationAPI activationAPI = new AccountActivationAPI();

    // giữ nhân viên đã check OK để activate
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
        btnReg.setVisible(true);
        txtUsername.requestFocus();
    }

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
                    JOptionPane.showMessageDialog(
                            this,
                            "Mã kích hoạt không tồn tại / đã dùng / đã hết hạn hoặc tài khoản đã được kích hoạt!",
                            "Lỗi xác thực",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            } catch (Exception ex) {
                logger.severe(ex.toString());
                JOptionPane.showMessageDialog(
                        this,
                        "Lỗi hệ thống: " + ex.getMessage(),
                        "Lỗi",
                        JOptionPane.ERROR_MESSAGE
                );
            } finally {
                btnCheck.setEnabled(true);
            }
        });

        btnReg.addActionListener(e -> {
            if (currentEmp == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng kiểm tra mã kích hoạt trước!");
                return;
            }

            String user = txtUsername.getText().trim();
            String pass = new String(txtPass.getPassword());
            String code = txtCode.getText().trim(); // <-- FIX: dùng code

            if (user.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng tạo Tên đăng nhập và Mật khẩu!");
                return;
            }

            btnReg.setEnabled(false);

            try {
                // Backend sẽ tự BCrypt + insert + mark token used
                activationAPI.activate(code, user, pass);

                JOptionPane.showMessageDialog(
                        this,
                        "Kích hoạt tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ."
                );

                new LoginView().setVisible(true);
                this.dispose();
            } catch (Exception ex) {
                logger.severe(ex.toString());
                JOptionPane.showMessageDialog(
                        this,
                        ex.getMessage(),
                        "Lỗi kích hoạt",
                        JOptionPane.ERROR_MESSAGE
                );
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

    @SuppressWarnings("unchecked")
    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
    }
}
