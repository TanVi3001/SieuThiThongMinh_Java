package view;

/**
 *
 * @author Admin
 */
public class LoginView extends javax.swing.JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger
            .getLogger(LoginView.class.getName());

    public LoginView() {
        initComponents();
        this.setLocationRelativeTo(null);

        // Bắt sự kiện bàn phím cho ô Mật khẩu (Nhấn Enter để đăng nhập)
        txtPassword.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent evt) {
                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    btnLoginActionPerformed(null);
                }
            }
        });

        setupModernUI();
    }

    private void setupModernUI() {
        // ── 1. Dọn dẹp & set layout tổng ────────────────────────────────────────
        this.getContentPane().removeAll();
        this.getContentPane().setLayout(null);
        this.getContentPane().invalidate();
        this.getContentPane().setLayout(new java.awt.BorderLayout());

        // ── 2. PANEL TRÁI – minh họa siêu thị tối ───────────────────────────────
        javax.swing.JPanel leftPanel = new javax.swing.JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                super.paintComponent(g);
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int w = getWidth(), h = getHeight();

                // Nền gradient navy
                java.awt.GradientPaint bg = new java.awt.GradientPaint(0, 0, new java.awt.Color(26, 43, 74), w, h, new java.awt.Color(13, 27, 51));
                g2.setPaint(bg);
                g2.fillRect(0, 0, w, h);

                // Lưới nền
                g2.setColor(new java.awt.Color(0, 201, 167, 15));
                for (int x = 0; x < w; x += 40) {
                    g2.drawLine(x, 0, x, h);
                }
                for (int y = 0; y < h; y += 40) {
                    g2.drawLine(0, y, w, y);
                }

                // Vòng tròn nền mờ
                paintGlowCircle(g2, -60, -60, 300, new java.awt.Color(0, 201, 167), 0.07f);
                paintGlowCircle(g2, w - 80, h - 80, 240, new java.awt.Color(255, 107, 53), 0.07f);
                paintGlowCircle(g2, w / 2, h / 2, 140, new java.awt.Color(255, 209, 102), 0.04f);

                // Minh họa siêu thị
                paintIllustration(g2, w, h);

                // Tên hệ thống
                int ty = h - 185;
                g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 24));
                g2.setColor(java.awt.Color.WHITE);
                drawCentered(g2, "HỆ THỐNG QUẢN LÝ", w, ty - 10);

                g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 32));
                g2.setColor(new java.awt.Color(255, 107, 53));
                drawCentered(g2, "SIÊU THỊ THÔNG MINH", w, ty + 40);

                g2.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
                g2.setColor(new java.awt.Color(255, 255, 255, 110));
                drawCentered(g2, "Mua sắm thông minh — Trải nghiệm vượt trội", w, ty + 72);

                // Feature pills
                paintPills(g2, w, h);

                g2.dispose();
            }

            private void paintGlowCircle(java.awt.Graphics2D g2, int cx, int cy, int r, java.awt.Color c, float alpha) {
                g2.setColor(new java.awt.Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (alpha * 255)));
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
            }

            private void drawCentered(java.awt.Graphics2D g2, String text, int w, int y) {
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, (w - fm.stringWidth(text)) / 2, y);
            }

            private void paintIllustration(java.awt.Graphics2D g2, int w, int h) {
                int areaH = (int) (h * 0.62);
                float s = Math.min(w / 380f, areaH / 230f);
                int ox = (int) (w / 2 - 155 * s);
                int oy = (int) (h * 0.15);

                paintShelf(g2, ox + (int) (0 * s), oy + (int) (15 * s), (int) (78 * s), (int) (155 * s), s);
                paintShelf(g2, ox + (int) (100 * s), oy + (int) (25 * s), (int) (78 * s), (int) (143 * s), s);
                paintShelf(g2, ox + (int) (232 * s), oy + (int) (15 * s), (int) (78 * s), (int) (155 * s), s);

                paintCart(g2, ox + (int) (148 * s), oy + (int) (143 * s), s);
                paintPhone(g2, ox + (int) (195 * s), oy + (int) (75 * s), s);
                paintWifi(g2, ox + (int) (170 * s), oy + (int) (25 * s), s);

                g2.setColor(new java.awt.Color(0, 201, 167, 35));
                g2.fillRect(ox + (int) (133 * s), oy + (int) (143 * s), (int) (70 * s), (int) (40 * s));
                g2.setColor(new java.awt.Color(0, 201, 167, 180));
                g2.fillRect(ox + (int) (133 * s), oy + (int) (142 * s), (int) (70 * s), (int) (2 * s + 1));

                paintBadge(g2, ox + (int) (-5 * s), oy + (int) (-25 * s), "Thanh toán thông minh", new java.awt.Color(255, 107, 53), s);
                paintBadge(g2, ox + (int) (210 * s), oy + (int) (-15 * s), "Tiết kiệm thời gian", new java.awt.Color(0, 201, 167), s);
            }

            private void paintShelf(java.awt.Graphics2D g2, int x, int y, int w, int h, float s) {
                g2.setColor(new java.awt.Color(36, 51, 82));
                g2.fillRoundRect(x, y, w, h, 8, 8);
                g2.setColor(new java.awt.Color(255, 255, 255, 18));
                for (int r = 1; r <= 3; r++) {
                    g2.fillRect(x, y + r * h / 4, w, 2);
                }

                java.awt.Color[][] cols = {
                    {new java.awt.Color(255, 107, 53), new java.awt.Color(0, 201, 167), new java.awt.Color(255, 209, 102)},
                    {new java.awt.Color(255, 209, 102), new java.awt.Color(255, 107, 53), new java.awt.Color(167, 139, 250)},
                    {new java.awt.Color(0, 201, 167), new java.awt.Color(167, 139, 250), new java.awt.Color(255, 107, 53)},
                    {new java.awt.Color(255, 209, 102), new java.awt.Color(255, 107, 53), new java.awt.Color(0, 201, 167)}
                };
                int cellW = w / 3, cellH = h / 5;
                int pw = (int) (cellW * 0.55), ph = (int) (cellH * 0.72);
                for (int r = 0; r < 4; r++) {
                    for (int c = 0; c < 3; c++) {
                        int px = x + c * cellW + (cellW - pw) / 2;
                        int py = y + 4 + r * cellH + (cellH - ph) / 2;
                        g2.setColor(cols[r][c]);
                        g2.fillRoundRect(px, py, pw, ph, 3, 3);
                    }
                }
            }

            private void paintCart(java.awt.Graphics2D g2, int x, int y, float s) {
                int cw = (int) (42 * s), ch = (int) (30 * s);
                g2.setColor(new java.awt.Color(13, 27, 51));
                g2.fillRoundRect(x, y, cw, ch, 8, 8);
                g2.setColor(new java.awt.Color(0, 201, 167, 170));
                g2.setStroke(new java.awt.BasicStroke(1.5f));
                g2.drawRoundRect(x, y, cw, ch, 8, 8);

                g2.setColor(new java.awt.Color(0, 201, 167, 55));
                g2.fillRoundRect(x + 4, y + 4, cw - 8, (int) (ch * 0.55), 3, 3);

                g2.setColor(new java.awt.Color(0, 201, 167));
                g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                int mx = x + cw / 2 - 6, my = y + ch / 4;
                g2.drawLine(mx, my + 5, mx + 4, my + 9);
                g2.drawLine(mx + 4, my + 9, mx + 10, my + 2);
                g2.setStroke(new java.awt.BasicStroke(1f));

                int wr = (int) (5 * s);
                for (int wx : new int[]{x + 5, x + cw - 5 - wr * 2}) {
                    g2.setColor(new java.awt.Color(36, 51, 82));
                    g2.fillOval(wx, y + ch, wr * 2, wr * 2);
                    g2.setColor(new java.awt.Color(0, 201, 167, 130));
                    g2.drawOval(wx, y + ch, wr * 2, wr * 2);
                }
                g2.setColor(new java.awt.Color(36, 51, 82));
                g2.fillRoundRect(x + cw, y + 3, (int) (8 * s), (int) (18 * s), 4, 4);
                g2.setColor(new java.awt.Color(0, 201, 167, 100));
                g2.drawRoundRect(x + cw, y + 3, (int) (8 * s), (int) (18 * s), 4, 4);
            }

            private void paintPhone(java.awt.Graphics2D g2, int x, int y, float s) {
                int pw = (int) (26 * s), ph = (int) (56 * s);
                g2.setColor(new java.awt.Color(255, 107, 53));
                g2.fillRoundRect(x, y, pw, ph, 7, 7);
                g2.setColor(new java.awt.Color(255, 255, 255, 28));
                g2.fillRoundRect(x + 3, y + 4, pw - 6, ph - 8, 4, 4);

                int b = (int) (4 * s);
                g2.setColor(new java.awt.Color(255, 255, 255, 200));
                g2.fillRoundRect(x + 4, y + 7, b, b, 1, 1);
                g2.fillRoundRect(x + 4 + b + 2, y + 7, b, b, 1, 1);
                g2.fillRoundRect(x + 4, y + 7 + b + 2, b, b, 1, 1);
                int s2 = (int) (2 * s);
                g2.fillRoundRect(x + 4 + b + 2, y + 7 + b + 2, s2, s2, 1, 1);
                g2.fillRoundRect(x + 4 + b + 2 + s2 + 2, y + 7 + b + 2, s2, s2, 1, 1);
                g2.fillRoundRect(x + 4 + b + 2, y + 7 + b + 2 + s2 + 2, b, s2, 1, 1);

                g2.setColor(new java.awt.Color(255, 255, 255, 150));
                int br = (int) (3 * s);
                g2.fillOval(x + pw / 2 - br, y + ph - br * 2 - 4, br * 2, br * 2);
            }

            private void paintWifi(java.awt.Graphics2D g2, int cx, int y, float s) {
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                int[] radii = {(int) (23 * s), (int) (15 * s), (int) (10 * s)};
                float[] alphas = {0.3f, 0.55f, 0.95f};
                for (int i = 0; i < 3; i++) {
                    int r = radii[i];
                    g2.setColor(new java.awt.Color(0, 201, 167, (int) (alphas[i] * 200)));
                    g2.setStroke(new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
                    g2.drawArc(cx - r, y - r, r * 2, r * 2, 25, 130);
                }
                g2.setStroke(new java.awt.BasicStroke(1f));
                int dr = (int) (4 * s);
                g2.setColor(new java.awt.Color(0, 201, 167));
                g2.fillOval(cx - dr, y - dr, dr * 2, dr * 2);
            }

            private void paintBadge(java.awt.Graphics2D g2, int x, int y, String text, java.awt.Color color, float s) {
                java.awt.Font f = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, Math.max(9, (int) (10 * s)));
                g2.setFont(f);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                int bw = fm.stringWidth(text) + 18, bh = 20;
                g2.setColor(color);
                g2.fillRoundRect(x, y, bw, bh, bh, bh);
                g2.setColor(java.awt.Color.WHITE);
                g2.drawString(text, x + 9, y + bh - 5);
            }

            private void paintPills(java.awt.Graphics2D g2, int w, int h) {
                String[] labels = {"Quét mã tự động", "Thanh toán nhanh", "Quản lý kho thực tế", "AI gợi ý sản phẩm"};
                java.awt.Color[] dots = {
                    new java.awt.Color(0, 201, 167), new java.awt.Color(255, 107, 53),
                    new java.awt.Color(255, 209, 102), new java.awt.Color(167, 139, 250)
                };

                java.awt.Font f = new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12);
                g2.setFont(f);
                java.awt.FontMetrics fm = g2.getFontMetrics();

                int totalW = 0;
                int[] widths = new int[labels.length];
                for (int i = 0; i < labels.length; i++) {
                    widths[i] = fm.stringWidth(labels[i]) + 36;
                    totalW += widths[i];
                }

                int gap = 10;
                totalW += gap * (labels.length - 1);
                int pillH = 28;
                int baseY = h - 14 - pillH;

                if (totalW <= w - 20) {
                    int startX = (w - totalW) / 2;
                    int cx = startX;
                    for (int i = 0; i < labels.length; i++) {
                        drawPill(g2, cx, baseY, widths[i], pillH, labels[i], dots[i]);
                        cx += widths[i] + gap;
                    }
                } else {
                    int rowGap = pillH + 8;
                    int row1W = widths[0] + gap + widths[1];
                    int row2W = widths[2] + gap + widths[3];
                    int r1x = (w - row1W) / 2, r2x = (w - row2W) / 2;

                    drawPill(g2, r1x, baseY - rowGap, widths[0], pillH, labels[0], dots[0]);
                    drawPill(g2, r1x + widths[0] + gap, baseY - rowGap, widths[1], pillH, labels[1], dots[1]);
                    drawPill(g2, r2x, baseY, widths[2], pillH, labels[2], dots[2]);
                    drawPill(g2, r2x + widths[2] + gap, baseY, widths[3], pillH, labels[3], dots[3]);
                }
            }

            private void drawPill(java.awt.Graphics2D g2, int x, int y, int w, int h, String text, java.awt.Color dotColor) {
                g2.setColor(new java.awt.Color(255, 255, 255, 20));
                g2.fillRoundRect(x, y, w, h, h, h);
                g2.setColor(new java.awt.Color(255, 255, 255, 40));
                g2.setStroke(new java.awt.BasicStroke(1.2f));
                g2.drawRoundRect(x, y, w, h, h, h);
                int dr = 6;
                g2.setColor(dotColor);
                g2.fillOval(x + 12, y + (h - dr) / 2, dr, dr);
                g2.setColor(java.awt.Color.WHITE);
                java.awt.FontMetrics fm = g2.getFontMetrics();
                g2.drawString(text, x + 24, y + (h + fm.getAscent() - fm.getDescent()) / 2 - 1);
            }
        };

        leftPanel.setPreferredSize(new java.awt.Dimension(420, 550));
        leftPanel.setOpaque(true);

        // ── 3. PANEL PHẢI – form đăng nhập ──────────────────────────────────────
        javax.swing.JPanel rightOuter = new javax.swing.JPanel(null) {
            @Override
            public void doLayout() {
                int w = getWidth(), h = getHeight();
                if (getComponentCount() > 0) {
                    int cardW = (int) (w * 0.75f);
                    int cardH = (int) (h * 0.85f);
                    getComponent(0).setBounds((w - cardW) / 2, (h - cardH) / 2, cardW, cardH);
                }
            }
        };
        rightOuter.setBackground(java.awt.Color.WHITE);

        javax.swing.JPanel card = new javax.swing.JPanel(null) {
            private java.util.Map<java.awt.Component, java.awt.Rectangle> originalBounds = null;

            @Override
            public void doLayout() {
                int w = getWidth(), h = getHeight();
                if (w == 0 || h == 0) {
                    return;
                }

                if (originalBounds == null) {
                    originalBounds = new java.util.HashMap<>();
                    for (java.awt.Component c : getComponents()) {
                        originalBounds.put(c, c.getBounds());
                    }
                }

                float sx = w / 400f, sy = h / 480f;
                for (java.awt.Component c : getComponents()) {
                    java.awt.Rectangle r = originalBounds.get(c);
                    if (r != null) {
                        c.setBounds((int) (r.x * sx), (int) (r.y * sy), (int) (r.width * sx), (int) (r.height * sy));
                    }
                }
            }
        };
        card.setBackground(java.awt.Color.WHITE);
        card.setPreferredSize(new java.awt.Dimension(400, 480));

        // ── Thanh gradient bên trái card ──────────────────────────────────────
        javax.swing.JPanel accentBar = new javax.swing.JPanel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setPaint(new java.awt.GradientPaint(
                        0, 0, new java.awt.Color(255, 107, 53),
                        0, getHeight(), new java.awt.Color(0, 201, 167)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        accentBar.setBounds(0, 0, 4, 480);
        card.add(accentBar);

        // ── Logo ──────────────────────────────────────────────────────────────
        javax.swing.JLabel lblLogoCircle = new javax.swing.JLabel() {
            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setStroke(new java.awt.BasicStroke(3));
                g2.setColor(new java.awt.Color(255, 69, 0));
                g2.drawOval(2, 2, 16, 16);
                g2.dispose();
            }
        };
        lblLogoCircle.setBounds(30, 20, 20, 20);
        card.add(lblLogoCircle);

        javax.swing.JLabel lblAppName = new javax.swing.JLabel("Smart Supermarket");
        lblAppName.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 15));
        lblAppName.setBounds(55, 17, 180, 26);
        card.add(lblAppName);

        // ── Tiêu đề ───────────────────────────────────────────────────────────
        javax.swing.JLabel lblTitle = new javax.swing.JLabel("Đăng nhập");
        lblTitle.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 28));
        lblTitle.setForeground(new java.awt.Color(26, 43, 74));
        lblTitle.setBounds(75, 78, 260, 38);
        card.add(lblTitle);

        javax.swing.JLabel lblSub = new javax.swing.JLabel("Chào mừng trở lại! Nhập thông tin của bạn.");
        lblSub.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        lblSub.setForeground(new java.awt.Color(122, 138, 154));
        lblSub.setBounds(75, 118, 300, 18);
        card.add(lblSub);

        // ── Label username ────────────────────────────────────────────────────
        javax.swing.JLabel lblUser = new javax.swing.JLabel("Tên đăng nhập");
        lblUser.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        lblUser.setForeground(new java.awt.Color(26, 43, 74));
        lblUser.setBounds(75, 152, 250, 16);
        card.add(lblUser);

        // ── txtUsername ────────────────────────────────────────────────────────
        txtUsername.setBounds(75, 172, 250, 44);
        txtUsername.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        txtUsername.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new RoundBorder(new java.awt.Color(221, 227, 234), 12),
                javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 12)));
        txtUsername.setBackground(new java.awt.Color(248, 250, 252));
        txtUsername.putClientProperty("JTextField.placeholderText", "Nhập username");
        card.add(txtUsername);

        // ── Label password ────────────────────────────────────────────────────
        javax.swing.JLabel lblPass = new javax.swing.JLabel("Mật khẩu");
        lblPass.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        lblPass.setForeground(new java.awt.Color(26, 43, 74));
        lblPass.setBounds(75, 228, 250, 16);
        card.add(lblPass);

        // ── txtPassword ───────────────────────────────────────────────────────
        txtPassword.setBounds(75, 248, 250, 44);
        txtPassword.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 13));
        txtPassword.setBorder(javax.swing.BorderFactory.createCompoundBorder(
                new RoundBorder(new java.awt.Color(221, 227, 234), 12),
                javax.swing.BorderFactory.createEmptyBorder(0, 12, 0, 12)));
        txtPassword.setBackground(new java.awt.Color(248, 250, 252));
        txtPassword.putClientProperty("JTextField.placeholderText", "Nhập mật khẩu");
        txtPassword.putClientProperty("JPasswordField.showRevealButton", true);
        card.add(txtPassword);

        // ── Quên mật khẩu ────────────────────────────────────────────────────
        javax.swing.JLabel lblForgot = new javax.swing.JLabel("Quên mật khẩu?");
        lblForgot.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12));
        lblForgot.setForeground(new java.awt.Color(255, 107, 53));
        lblForgot.setBounds(75, 303, 250, 18);
        lblForgot.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        lblForgot.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblForgot.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                String name = txtUsername.getText().trim();
                ForgotPasswordView fp = new ForgotPasswordView(name);
                fp.setVisible(true);
                dispose();
            }

            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                lblForgot.setForeground(new java.awt.Color(200, 70, 20));
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                lblForgot.setForeground(new java.awt.Color(255, 107, 53));
            }
        });
        card.add(lblForgot);

        // ── Nút Đăng nhập ─────────────────────────────────────────────────────
        javax.swing.JButton btnGradientLogin = new javax.swing.JButton("Đăng nhập  →") {
            private boolean hovered = false;

            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(java.awt.Graphics g) {
                java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                java.awt.Color c1 = hovered ? new java.awt.Color(255, 107, 53) : new java.awt.Color(26, 43, 74);
                java.awt.Color c2 = hovered ? new java.awt.Color(255, 140, 90) : new java.awt.Color(36, 51, 82);
                g2.setPaint(new java.awt.GradientPaint(0, 0, c1, getWidth(), 0, c2));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btnGradientLogin.setBounds(75, 335, 250, 46);
        btnGradientLogin.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 14));
        btnGradientLogin.setForeground(java.awt.Color.WHITE);
        btnGradientLogin.setContentAreaFilled(false);
        btnGradientLogin.setBorderPainted(false);
        btnGradientLogin.setFocusPainted(false);
        btnGradientLogin.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        btnGradientLogin.addActionListener(evt -> btnLoginActionPerformed(null));
        card.add(btnGradientLogin);

        // ── Link "Chưa có tài khoản?" ─────────────────────────────────────────
        javax.swing.JLabel lblRegisterLink = new javax.swing.JLabel("<html><span style='color:#7a8a9a'>Chưa có tài khoản? </span><span style='color:#FF6B35'><u>Đăng ký</u></span></html>");
        lblRegisterLink.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
        lblRegisterLink.setBounds(75, 392, 250, 18);
        lblRegisterLink.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblRegisterLink.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        lblRegisterLink.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                RegisterView regView = new RegisterView();
                regView.setVisible(true);
                regView.setLocationRelativeTo(null);
                dispose();
            }
        });
        card.add(lblRegisterLink);

        rightOuter.add(card);

        this.getContentPane().add(leftPanel, java.awt.BorderLayout.WEST);
        this.getContentPane().add(rightOuter, java.awt.BorderLayout.CENTER);

        this.pack();
        this.setSize(960, 620);
        this.setLocationRelativeTo(null);
        this.revalidate();
        this.repaint();
    }

    class RoundBorder implements javax.swing.border.Border {

        private final java.awt.Color color;
        private final int radius;

        public RoundBorder(java.awt.Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(java.awt.Component c, java.awt.Graphics g,
                int x, int y, int width, int height) {
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new java.awt.BasicStroke(1.5f));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public java.awt.Insets getBorderInsets(java.awt.Component c) {
            return new java.awt.Insets(1, 1, 1, 1);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        LoginView = new javax.swing.JPanel();
        Login = new javax.swing.JLabel();
        Username = new javax.swing.JLabel();
        Password = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        btnLogin = new javax.swing.JButton();
        txtPassword = new javax.swing.JPasswordField();
        HomePanel = new javax.swing.JPanel();
        SystemName = new javax.swing.JLabel();
        Separator = new javax.swing.JSeparator();
        Greeting = new javax.swing.JLabel();
        ClassName = new javax.swing.JLabel();
        IconMarket = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setBackground(new java.awt.Color(0, 0, 102));
        getContentPane().setLayout(new java.awt.GridBagLayout());

        LoginView.setBackground(new java.awt.Color(255, 255, 255));
        LoginView.setForeground(new java.awt.Color(255, 255, 255));
        LoginView.setPreferredSize(new java.awt.Dimension(450, 480));
        LoginView.setLayout(new java.awt.GridBagLayout());

        Login.setBackground(new java.awt.Color(0, 0, 0));
        Login.setFont(new java.awt.Font("Segoe UI", 1, 20));
        Login.setText("ĐĂNG NHẬP");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(46, 18, 0, 0);
        LoginView.add(Login, gridBagConstraints);

        Username.setBackground(new java.awt.Color(0, 0, 0));
        Username.setFont(new java.awt.Font("Segoe UI", 1, 15));
        Username.setText("TÀI KHOẢN");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.ipady = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(31, 36, 0, 0);
        LoginView.add(Username, gridBagConstraints);

        Password.setBackground(new java.awt.Color(0, 0, 0));
        Password.setFont(new java.awt.Font("Segoe UI", 1, 15));
        Password.setText("MẬT KHẨU");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.ipady = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(26, 36, 0, 0);
        LoginView.add(Password, gridBagConstraints);

        txtUsername.addActionListener(this::txtUsernameActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.ipadx = 115;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(34, 18, 0, 42);
        LoginView.add(txtUsername, gridBagConstraints);

        btnLogin.setBackground(new java.awt.Color(44, 62, 80));
        btnLogin.setFont(new java.awt.Font("Segoe UI", 1, 14));
        btnLogin.setForeground(new java.awt.Color(255, 255, 255));
        btnLogin.setText("Đăng nhập");
        btnLogin.addActionListener(this::btnLoginActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(32, 37, 28, 0);
        LoginView.add(btnLogin, gridBagConstraints);

        txtPassword.addActionListener(this::txtPasswordActionPerformed);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.ipadx = 115;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(30, 18, 0, 42);
        LoginView.add(txtPassword, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipadx = 129;
        gridBagConstraints.ipady = 64;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 0);
        getContentPane().add(LoginView, gridBagConstraints);

        HomePanel.setBackground(new java.awt.Color(236, 240, 241));
        HomePanel.setForeground(new java.awt.Color(255, 255, 255));
        HomePanel.setLayout(new java.awt.GridBagLayout());

        SystemName.setFont(new java.awt.Font("Segoe UI", 1, 17));
        SystemName.setForeground(new java.awt.Color(44, 62, 80));
        SystemName.setText("HỆ THỐNG QUẢN LÝ SIÊU THỊ ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(110, 6, 0, 39);
        HomePanel.add(SystemName, gridBagConstraints);

        Separator.setForeground(new java.awt.Color(0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.ipadx = 218;
        gridBagConstraints.ipady = 12;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(18, 88, 0, 0);
        HomePanel.add(Separator, gridBagConstraints);

        Greeting.setFont(new java.awt.Font("Segoe UI", 1, 12));
        Greeting.setForeground(new java.awt.Color(44, 62, 80));
        Greeting.setText("Chào mừng đến với hệ thống!");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 16, 0, 0);
        HomePanel.add(Greeting, gridBagConstraints);

        ClassName.setFont(new java.awt.Font("Segoe UI", 1, 11));
        ClassName.setForeground(new java.awt.Color(44, 62, 80));
        ClassName.setText("Nhóm 12 - IS216.Q22");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 44, 108, 0);
        HomePanel.add(ClassName, gridBagConstraints);

        // Đã xóa (comment) dòng IconMarket gọi file ảnh cũ bị lỗi
        // IconMarket.setIcon(new javax.swing.ImageIcon(getClass().getResource("/view/image/supermarket (2).png"))); 
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridheight = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(80, 32, 0, 0);
        HomePanel.add(IconMarket, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.ipady = -2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        getContentPane().add(HomePanel, gridBagConstraints);

        pack();
    }

    private void btnLoginActionPerformed(java.awt.event.ActionEvent evt) {
        String user = txtUsername.getText().trim();
        String pass = new String(txtPassword.getPassword());

        if (user.isEmpty() || pass.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Vui lòng nhập đầy đủ Tài khoản và Mật khẩu!",
                    "Nhắc nhở",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            model.account.Account acc = business.service.LoginService.authenticate(user, pass);

            if (acc != null) {
                java.awt.EventQueue.invokeLater(() -> {
                    if (business.service.AuthorizationService.isAdmin(acc)) {
                        AdminDashboardView adminScreen = new AdminDashboardView();
                        adminScreen.setVisible(true);
                        adminScreen.setLocationRelativeTo(null);
                    } else if (business.service.AuthorizationService.isWarehouseStaff(acc)) {
                        WarehouseDashboardView warehouseScreen = new WarehouseDashboardView();
                        warehouseScreen.setVisible(true);
                        warehouseScreen.setLocationRelativeTo(null);
                    } else {
                        DashboardView mainScreen = new DashboardView();
                        mainScreen.setVisible(true);
                        mainScreen.setLocationRelativeTo(null);
                    }
                });
                this.dispose();
            } else {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Tài khoản hoặc mật khẩu không chính xác!",
                        "Lỗi đăng nhập",
                        javax.swing.JOptionPane.ERROR_MESSAGE);

                txtPassword.setText("");
                txtPassword.requestFocusInWindow();
            }
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Hệ thống đang gặp sự cố kết nối: " + e.getMessage(),
                    "LỖI NGHIÊM TRỌNG",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            java.util.Arrays.fill(txtPassword.getPassword(), '\0');
        }
    }

    private void txtUsernameActionPerformed(java.awt.event.ActionEvent evt) {
        txtPassword.requestFocus();
        btnLoginActionPerformed(evt);
    }

    private void txtPasswordActionPerformed(java.awt.event.ActionEvent evt) {
    }

    public static void main(String args[]) {
        int deletedNow = business.sql.rbac.TokenSql.getInstance().deleteExpiredTokens();
        System.out.println("STARTUP CLEANUP deleted = " + deletedNow);

        business.service.TokenCleanupService.start();

        // (Optional) giữ SyncWatcher làm fallback
        // common.sync.SyncWatcher.start(2);
        // REALTIME: start server (ai mở app trước sẽ host server), rồi connect client
        common.realtime.RealtimeServer.tryStart(8887);

        // Nếu chạy LAN: thay localhost bằng IP máy host, ví dụ ws://192.168.1.10:8887
        common.realtime.RealtimeClient.connect("ws://localhost:8887");

        java.awt.EventQueue.invokeLater(() -> {
            LoginView login = new LoginView();
            login.setLocationRelativeTo(null);
            login.setVisible(true);
        });
    }

    private javax.swing.JLabel ClassName;
    private javax.swing.JLabel Greeting;
    private javax.swing.JPanel HomePanel;
    private javax.swing.JLabel IconMarket;
    private javax.swing.JLabel Login;
    private javax.swing.JPanel LoginView;
    private javax.swing.JLabel Password;
    private javax.swing.JSeparator Separator;
    private javax.swing.JLabel SystemName;
    private javax.swing.JLabel Username;
    private javax.swing.JButton btnLogin;
    private javax.swing.JPasswordField txtPassword;
    private javax.swing.JTextField txtUsername;
}
