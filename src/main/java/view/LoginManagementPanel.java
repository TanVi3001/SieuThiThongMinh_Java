package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import view.components.IconHelper;

public class LoginManagementPanel extends JPanel {

    // --- BẢNG MÀU UI CHUẨN ---
    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color borderGray = new Color(230, 235, 241);
    private final Color primaryBlue = new Color(67, 97, 238);
    private final Color dangerRed = new Color(220, 53, 69);
    private final Color successGreen = new Color(16, 185, 129);

    private JTable tblLoginLogs;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JComboBox<String> cbFilterStatus;
    
    // --- KHAI BÁO CÁC Ô THỐNG KÊ ---
    private JLabel lblTotalLogins, lblFailedLogins, lblActiveSessions;
    private JButton btnRefresh;

    public LoginManagementPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents();
        loadLoginData("", "Tất cả");
        
        // Nhận tín hiệu nếu có ai đó vừa đăng nhập/đăng xuất
        setupRealtimeSync();
    }

    private void initUI() {
        // ── 1. HEADER & THỐNG KÊ AN NINH ──────────────────────────────────
        JPanel topContainer = new JPanel(new BorderLayout(0, 15));
        topContainer.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Giám Sát Truy Cập (Login History)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Theo dõi lịch sử đăng nhập, địa chỉ IP và phát hiện các nỗ lực truy cập bất thường");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);
        
        // Thẻ thống kê nhanh
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setOpaque(false);
        
        lblTotalLogins = new JLabel("0", SwingConstants.CENTER);
        lblFailedLogins = new JLabel("0", SwingConstants.CENTER);
        lblActiveSessions = new JLabel("0", SwingConstants.CENTER);
        
        statsPanel.add(createStatCard("Lượt Truy Cập (Hôm nay)", lblTotalLogins, primaryBlue));
        statsPanel.add(createStatCard("Đang Online", lblActiveSessions, successGreen));
        statsPanel.add(createStatCard("Cảnh Báo (Sai Pass)", lblFailedLogins, dangerRed));

        topContainer.add(titlePanel, BorderLayout.WEST);
        topContainer.add(statsPanel, BorderLayout.EAST);
        add(topContainer, BorderLayout.NORTH);

        // ── 2. MAIN CONTENT (FULL BẢNG) ───────────────────────────────────
        RoundedPanel mainCard = new RoundedPanel(20, cardWhite);
        mainCard.setLayout(new BorderLayout(0, 15));
        mainCard.setBorder(new EmptyBorder(20, 25, 20, 25));

        // Tool bar tìm kiếm và lọc
        JPanel toolBar = new JPanel(new BorderLayout());
        toolBar.setOpaque(false);
        
        JLabel lblListTitle = new JLabel("Nhật ký hệ thống");
        lblListTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblListTitle.setForeground(textDark);
        
        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchBox.setOpaque(false);
        
        cbFilterStatus = new JComboBox<>(new String[]{"Tất cả", "Thành công", "Sai mật khẩu", "Tài khoản bị khóa"});
        cbFilterStatus.setPreferredSize(new Dimension(150, 38));
        cbFilterStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        txtSearch = createTextField("Tra ID, IP, Mã phiên...");
        txtSearch.setPreferredSize(new Dimension(220, 38));
        
        btnRefresh = createCustomButton("Làm mới", primaryBlue, Color.WHITE, null);
        btnRefresh.setPreferredSize(new Dimension(100, 38));
        
        searchBox.add(cbFilterStatus);
        searchBox.add(txtSearch);
        searchBox.add(btnRefresh);
        
        toolBar.add(lblListTitle, BorderLayout.WEST);
        toolBar.add(searchBox, BorderLayout.EAST);

        // Table
        tableModel = new DefaultTableModel(new Object[]{
            "Mã Phiên (Session)", "Tài Khoản", "IP Address", "Thời Gian Đăng Nhập", "Thời Gian Đăng Xuất", "Trạng Thái"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLoginLogs = new JTable(tableModel);
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblLoginLogs);
        scrollPane.setBorder(BorderFactory.createLineBorder(borderGray));
        scrollPane.getViewport().setBackground(Color.WHITE);

        mainCard.add(toolBar, BorderLayout.NORTH);
        mainCard.add(scrollPane, BorderLayout.CENTER);

        add(mainCard, BorderLayout.CENTER);
    }
    
    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        RoundedPanel card = new RoundedPanel(15, cardWhite);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor), 
            new EmptyBorder(10, 15, 10, 15)
        ));
        card.setPreferredSize(new Dimension(180, 65));
        
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(textGray);
        
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(textDark);
        
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void initEvents() {
        btnRefresh.addActionListener(e -> doSearch());
        
        cbFilterStatus.addActionListener(e -> doSearch());

        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { doSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { doSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { doSearch(); }
        });
    }
    
    private void doSearch() {
        String keyword = txtSearch.getText().trim();
        String status = cbFilterStatus.getSelectedItem().toString();
        loadLoginData(keyword, status);
    }

    // =========================================================================
    // KẾT NỐI BẢNG LICHSUDANGNHAP
    // =========================================================================
    private void loadLoginData(String keyword, String statusFilter) {
        tableModel.setRowCount(0);
        int total = 0, failed = 0, active = 0;
        
        // Truy vấn dựa trên bảng LICHSUDANGNHAP
        String sql = "SELECT MAPHIEN, MATAIKHOAN, DIACHIIP, " +
                     "TO_CHAR(THOIGIANDANGNHAP, 'DD/MM/YYYY HH24:MI:SS') as TGIN, " +
                     "TO_CHAR(THOIGIANDANGXUAT, 'DD/MM/YYYY HH24:MI:SS') as TGOUT, " +
                     "TRANGTHAI " +
                     "FROM LICHSUDANGNHAP " +
                     "WHERE (LOWER(MAPHIEN) LIKE LOWER(?) OR LOWER(MATAIKHOAN) LIKE LOWER(?) OR DIACHIIP LIKE ?) ";
                     
        if (!statusFilter.equals("Tất cả")) {
            sql += " AND TRANGTHAI = '" + statusFilter + "' ";
        }
        sql += " ORDER BY THOIGIANDANGNHAP DESC";

        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            ps.setString(3, "%" + keyword + "%");
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String maphien = rs.getString("MAPHIEN");
                    String tk = rs.getString("MATAIKHOAN");
                    String ip = rs.getString("DIACHIIP");
                    String timeIn = rs.getString("TGIN");
                    String timeOut = rs.getString("TGOUT");
                    String tt = rs.getString("TRANGTHAI");
                    
                    if (timeOut == null || timeOut.isEmpty()) timeOut = "Đang hoạt động";
                    
                    tableModel.addRow(new Object[]{ maphien, tk, ip, timeIn, timeOut, tt });
                    
                    // Thống kê nhanh
                    total++;
                    if ("Sai mật khẩu".equalsIgnoreCase(tt) || "Tài khoản bị khóa".equalsIgnoreCase(tt)) failed++;
                    if ("Đang hoạt động".equals(timeOut) && "Thành công".equalsIgnoreCase(tt)) active++;
                }
            }
            
            // Cập nhật thẻ Dashboard
            lblTotalLogins.setText(String.valueOf(total));
            lblFailedLogins.setText(String.valueOf(failed));
            lblActiveSessions.setText(String.valueOf(active));
            
        } catch (Exception e) {
            System.err.println("Lỗi tải danh sách Đăng nhập: " + e.getMessage());
            
            // Dữ liệu Fake để test giao diện nếu bác chưa tạo bảng LICHSUDANGNHAP
            if (tableModel.getRowCount() == 0) {
                tableModel.addRow(new Object[]{"SS_83749", "EMP1717830", "192.168.1.45", "10/05/2026 08:30:15", "Đang hoạt động", "Thành công"});
                tableModel.addRow(new Object[]{"SS_83750", "admin_tong", "192.168.1.12", "10/05/2026 09:15:00", "10/05/2026 11:20:00", "Thành công"});
                tableModel.addRow(new Object[]{"SS_83751", "EMP992211", "113.190.23.5", "10/05/2026 14:02:11", "N/A", "Sai mật khẩu"});
                
                lblTotalLogins.setText("3");
                lblFailedLogins.setText("1");
                lblActiveSessions.setText("1");
            }
        }
    }

    private void setupTableStyle() {
        tblLoginLogs.setRowHeight(38);
        tblLoginLogs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblLoginLogs.setShowVerticalLines(false);
        tblLoginLogs.setSelectionBackground(new Color(237, 242, 255));
        tblLoginLogs.setSelectionForeground(textDark);
        tblLoginLogs.getTableHeader().setReorderingAllowed(false);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(textDark);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        for (int i = 0; i < tblLoginLogs.getColumnCount(); i++) {
            tblLoginLogs.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
        
        // Đổi màu chữ cột Trạng thái cho sinh động
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = String.valueOf(value);
                if (status.equalsIgnoreCase("Sai mật khẩu") || status.equalsIgnoreCase("Tài khoản bị khóa")) {
                    setForeground(dangerRed);
                    setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else if (status.equalsIgnoreCase("Thành công")) {
                    setForeground(successGreen);
                    setFont(new Font("Segoe UI", Font.BOLD, 14));
                } else {
                    setForeground(textDark);
                }
                return c;
            }
        };
        tblLoginLogs.getColumnModel().getColumn(5).setCellRenderer(statusRenderer);
    }
    
    private void setupRealtimeSync() {
        try {
            EventBus.subscribe(AppDataChangedEvent.class, event -> {
                // Lắng nghe sự kiện login/logout
                if (event.getType() == AppEventType.ACCOUNT_SECURITY || event.getType().name().equals("LOGIN_HISTORY")) {
                    SwingUtilities.invokeLater(() -> doSearch());
                }
            });
        } catch (Exception e) {
            System.err.println("Lỗi real-time màn hình Login History: " + e.getMessage());
        }
    }

    // =========================================================================
    // CÁC HÀM TIỆN ÍCH UI
    // =========================================================================
    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(borderGray, 8), new EmptyBorder(5, 10, 5, 10)));
        return txt;
    }

    private JButton createCustomButton(String t, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(t);
        if (icon != null) btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(16, 16, 1)));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg); btn.setBackground(bg);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground()); g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 8, 8);
                super.paint(g2, c); g2.dispose();
            }
        });
        return btn;
    }

    class RoundedPanel extends JPanel {
        private int r; private Color bg;
        public RoundedPanel(int r, Color bg) { this.r = r; this.bg = bg; setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg); g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
            g2.dispose();
        }
    }

    class RoundBorder implements javax.swing.border.Border {
        private Color c; private int r;
        public RoundBorder(Color c, int r) { this.c = c; this.r = r; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.c); g2.drawRoundRect(x, y, w - 1, h - 1, r, r);
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
        @Override public boolean isBorderOpaque() { return false; }
    }
}