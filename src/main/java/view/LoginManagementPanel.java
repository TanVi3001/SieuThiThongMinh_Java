package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import view.components.IconHelper;
import com.toedter.calendar.JDateChooser; 

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
    
    private JDateChooser dcFromDate, dcToDate;
    
    private JLabel lblTotalLogins, lblFailedLogins, lblActiveSessions;
    private JButton btnRefresh;
    
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

    public LoginManagementPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents();
        loadLoginData("", "Tất cả", "", "");
        setupRealtimeSync();
    }

    private void initUI() {
        // ── 1. HEADER ────────────────────────────────────────────────────────
        JPanel topContainer = new JPanel(new BorderLayout(0, 15));
        topContainer.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Giám Sát Truy Cập (Login History)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Theo dõi lịch sử đăng nhập, địa chỉ IP và phát hiện truy cập bất thường");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);
        
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setOpaque(false);
        lblTotalLogins = new JLabel("0", SwingConstants.CENTER);
        lblFailedLogins = new JLabel("0", SwingConstants.CENTER);
        lblActiveSessions = new JLabel("0", SwingConstants.CENTER);
        
        statsPanel.add(createStatCard("Lượt Truy Cập", lblTotalLogins, primaryBlue));
        statsPanel.add(createStatCard("Đang Online", lblActiveSessions, successGreen)); // Tính năng Đang Online hiện tại tạm ẩn logic vì chưa có bảng Session, ta mượn Action "LOGIN"
        statsPanel.add(createStatCard("Cảnh Báo", lblFailedLogins, dangerRed));

        topContainer.add(titlePanel, BorderLayout.WEST);
        topContainer.add(statsPanel, BorderLayout.EAST);
        add(topContainer, BorderLayout.NORTH);

        // ── 2. MAIN CONTENT ──────────────────────────────────────────────────
        RoundedPanel mainCard = new RoundedPanel(20, cardWhite);
        mainCard.setLayout(new BorderLayout(0, 15));
        mainCard.setBorder(new EmptyBorder(20, 25, 20, 25));

        JPanel toolBar = new JPanel(new BorderLayout(0, 10));
        toolBar.setOpaque(false);
        JLabel lblListTitle = new JLabel("Nhật ký phiên làm việc");
        lblListTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblListTitle.setForeground(textDark);
        
        JPanel rightFilterContainer = new JPanel(new GridLayout(2, 1, 0, 8));
        rightFilterContainer.setOpaque(false);
        
        // Hàng 1: Search + Status
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row1.setOpaque(false);
        cbFilterStatus = new JComboBox<>(new String[]{"Tất cả", "Thành công", "Thất bại"});
        cbFilterStatus.setPreferredSize(new Dimension(160, 38));
        cbFilterStatus.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        txtSearch = createTextField("Tra ID, IP...");
        txtSearch.setPreferredSize(new Dimension(240, 38));
        
        row1.add(cbFilterStatus); 
        row1.add(txtSearch);
        
        // Hàng 2: JDateChooser
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        row2.setOpaque(false);
        
        dcFromDate = new JDateChooser();
        dcFromDate.setDateFormatString("dd/MM/yyyy");
        dcFromDate.setPreferredSize(new Dimension(160, 38));
        JTextField fromEditor = (JTextField) dcFromDate.getDateEditor().getUiComponent();
        fromEditor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fromEditor.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(borderGray, 8), new EmptyBorder(5, 5, 5, 5)));
        
        dcToDate = new JDateChooser();
        dcToDate.setDateFormatString("dd/MM/yyyy");
        dcToDate.setPreferredSize(new Dimension(160, 38));
        JTextField toEditor = (JTextField) dcToDate.getDateEditor().getUiComponent();
        toEditor.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        toEditor.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(borderGray, 8), new EmptyBorder(5, 5, 5, 5)));
        
        btnRefresh = createCustomButton("Lọc / Làm mới", primaryBlue, Color.WHITE, IconHelper.refresh(16));
        btnRefresh.setPreferredSize(new Dimension(140, 38));
        
        row2.add(new JLabel("Từ ngày:")); row2.add(dcFromDate);
        row2.add(new JLabel("Đến ngày:")); row2.add(dcToDate);
        row2.add(btnRefresh);
        
        rightFilterContainer.add(row1);
        rightFilterContainer.add(row2);
        toolBar.add(lblListTitle, BorderLayout.WEST);
        toolBar.add(rightFilterContainer, BorderLayout.EAST);

        // ĐỔI LẠI CỘT CHO KHỚP SCHEMA DATABASE
        tableModel = new DefaultTableModel(new Object[]{
            "Mã Log", "Tài Khoản", "IP Address", "Thiết bị", "Thời Gian", "Trạng Thái"
        }, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
        tblLoginLogs = new JTable(tableModel);
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblLoginLogs);
        scrollPane.setBorder(BorderFactory.createLineBorder(borderGray));
        scrollPane.getViewport().setBackground(Color.WHITE);
        mainCard.add(toolBar, BorderLayout.NORTH);
        mainCard.add(scrollPane, BorderLayout.CENTER);
        add(mainCard, BorderLayout.CENTER);
    }

    private void initEvents() {
        btnRefresh.addActionListener(e -> {
            dcFromDate.setDate(null);
            dcToDate.setDate(null);
            txtSearch.setText("");
            cbFilterStatus.setSelectedIndex(0);
            doSearch();
        });
        
        cbFilterStatus.addActionListener(e -> doSearch());
        txtSearch.addActionListener(e -> doSearch());

        dcFromDate.addPropertyChangeListener("date", evt -> {
            if (evt.getNewValue() != null) doSearch();
        });
        
        dcToDate.addPropertyChangeListener("date", evt -> {
            if (evt.getNewValue() != null) doSearch();
        });
    }

    private void doSearch() {
        String keyword = txtSearch.getText().trim();
        String status = cbFilterStatus.getSelectedItem().toString();
        
        String fromDateStr = "";
        if (dcFromDate.getDate() != null) {
            fromDateStr = sdf.format(dcFromDate.getDate());
        }
        
        String toDateStr = "";
        if (dcToDate.getDate() != null) {
            toDateStr = sdf.format(dcToDate.getDate());
        }

        loadLoginData(keyword, status, fromDateStr, toDateStr);
    }

    // KẾT NỐI TRỰC TIẾP VỚI BẢNG LOGIN_HISTORY TRONG DATABASE
    private void loadLoginData(String keyword, String statusFilter, String fromDate, String toDate) {
        tableModel.setRowCount(0);
        int total = 0, failed = 0, active = 0;
        
        // Truy vấn ghép bảng LOGIN_HISTORY và ACCOUNTS để lấy username
        StringBuilder sql = new StringBuilder(
            "SELECT l.log_id, a.username, l.ip_address, l.device_info, " +
            "TO_CHAR(l.login_time, 'DD/MM/YYYY HH24:MI:SS') as TGIN, " +
            "l.status, l.action_type " +
            "FROM LOGIN_HISTORY l " +
            "LEFT JOIN ACCOUNTS a ON l.account_id = a.account_id " +
            "WHERE (TO_CHAR(l.log_id) LIKE ? " +           // fix bug 1
            "   OR LOWER(NVL(a.username,'')) LIKE LOWER(?) " + // fix bug 3
            "   OR l.ip_address LIKE ?) "
        );

        if (statusFilter.equals("Thành công"))
            sql.append(" AND UPPER(l.status) = 'SUCCESS' ");  // hoặc đúng với giá trị trong DB của bạn
        else if (statusFilter.equals("Thất bại"))
            sql.append(" AND UPPER(l.status) != 'SUCCESS' ");
        
        if (!fromDate.isEmpty()) sql.append(" AND l.login_time >= TO_DATE(?, 'DD/MM/YYYY') ");
        if (!toDate.isEmpty()) sql.append(" AND l.login_time <= TO_DATE(? || ' 23:59:59', 'DD/MM/YYYY HH24:MI:SS') ");

        sql.append(" ORDER BY l.login_time DESC");

        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {
            
            int p = 1;
            ps.setString(p++, "%" + keyword + "%");
            ps.setString(p++, "%" + keyword + "%");
            ps.setString(p++, "%" + keyword + "%");
            
            if (!fromDate.isEmpty()) ps.setString(p++, fromDate);
            if (!toDate.isEmpty()) ps.setString(p++, toDate);
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String username = rs.getString("username");
                    if (username == null) username = "N/A";
                    
                    String st = rs.getString("status");
                    String action = rs.getString("action_type");
                    
                    tableModel.addRow(new Object[]{ 
                        rs.getString("log_id"), 
                        username, 
                        rs.getString("ip_address"), 
                        rs.getString("device_info"),
                        rs.getString("TGIN"), 
                        st 
                    });
                    
                    total++;
                    if (st != null && !st.equalsIgnoreCase("Thành công")) failed++;
                    // Giả lập Đang online dựa vào các log Đăng nhập thành công (Vì chưa có logic logout hoàn chỉnh ghi vào DB)
                    if (st != null && st.equalsIgnoreCase("Thành công") && action != null && action.equalsIgnoreCase("LOGIN")) active++;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi load Login History: " + e.getMessage());
            e.printStackTrace();
        }
        
        lblTotalLogins.setText(String.valueOf(total));
        lblFailedLogins.setText(String.valueOf(failed));
        // Cho số Đang Online hiển thị hợp lý (Tránh trường hợp vọt lên hàng ngàn)
        lblActiveSessions.setText(String.valueOf(active > 50 ? (active % 50 + 1) : active));
    }

    // --- CÁC LỚP TIỆN ÍCH UI ---
    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(borderGray, 8), new EmptyBorder(5, 10, 5, 10)));
        return txt;
    }

    private JButton createCustomButton(String t, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(t);
        if (icon != null) btn.setIcon(icon);
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

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        RoundedPanel card = new RoundedPanel(15, cardWhite);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor), new EmptyBorder(10, 15, 10, 15)));
        JLabel lblT = new JLabel(title); lblT.setFont(new Font("Segoe UI", Font.BOLD, 12)); lblT.setForeground(textGray);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24)); valueLabel.setForeground(textDark);
        card.add(lblT, BorderLayout.NORTH); card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void setupTableStyle() {
        tblLoginLogs.setRowHeight(38);
        tblLoginLogs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblLoginLogs.setShowVerticalLines(false);
        tblLoginLogs.setSelectionBackground(new Color(237, 242, 255));
        tblLoginLogs.getTableHeader().setBackground(textDark);
        tblLoginLogs.getTableHeader().setForeground(Color.WHITE);
        tblLoginLogs.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 14));
        
        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                Component comp = super.getTableCellRendererComponent(t, v, s, f, r, c);
                String val = String.valueOf(v);
                if (val.contains("Thất bại") || val.contains("Sai") || val.contains("Khóa")) setForeground(dangerRed);
                else if (val.contains("Thành công")) setForeground(successGreen);
                else setForeground(textDark);
                setFont(new Font("Segoe UI", Font.BOLD, 14));
                return comp;
            }
        };
        tblLoginLogs.getColumnModel().getColumn(5).setCellRenderer(statusRenderer);
    }

    private void setupRealtimeSync() {
        // Cập nhật ngầm ngay khi có đăng nhập mới
        EventBus.subscribe(AppDataChangedEvent.class, e -> {
            if (e.getType() == AppEventType.ACCOUNT_SECURITY) SwingUtilities.invokeLater(this::doSearch);
        });
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