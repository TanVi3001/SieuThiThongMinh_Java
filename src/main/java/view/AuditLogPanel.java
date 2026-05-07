package view;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import view.components.IconHelper;

public class AuditLogPanel extends JPanel {

    // --- BẢNG MÀU UI CHUẨN ---
    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color borderGray = new Color(230, 235, 241);

    private JTable tblLogs;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JComboBox<String> cbActionType, cbEntity;
    private JButton btnFilter, btnExport;
    private Timer autoRefreshTimer; // Khai báo bộ đếm nhịp

    public AuditLogPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents(); 
        loadRealData("Tất cả Hành động", "Tất cả Đối tượng", ""); 
    }

    private void initUI() {
        // ── 1. HEADER ────────────────────────────────────────────────────────
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Nhật Ký Hoạt Động Hệ Thống");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Truy vết và giám sát mọi thao tác thay đổi dữ liệu (Audit Trail)");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        // ── 2. FILTER BAR ────────────────────────────────────────────────────
        RoundedPanel filterCard = new RoundedPanel(15, cardWhite);
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));
        filterCard.setBorder(new EmptyBorder(5, 10, 5, 10));

        txtSearch = createTextField("Tìm kiếm theo tài khoản hoặc IP...");
        txtSearch.setPreferredSize(new Dimension(280, 40));

        cbActionType = new JComboBox<>(new String[]{"Tất cả Hành động", "THÊM MỚI", "CẬP NHẬT", "XÓA", "ĐĂNG NHẬP"});
        cbActionType.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbActionType.setPreferredSize(new Dimension(180, 40));

        cbEntity = new JComboBox<>(new String[]{"Tất cả Đối tượng", "TÀI KHOẢN", "NHÂN VIÊN", "SẢN PHẨM", "HÓA ĐƠN", "KHÁCH HÀNG", "HỆ THỐNG"});
        cbEntity.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbEntity.setPreferredSize(new Dimension(180, 40));

        btnFilter = createCustomButton("Lọc dữ liệu", new Color(54, 92, 245), Color.WHITE, IconHelper.search(18));
        btnExport = createCustomButton("Xuất Excel", new Color(165, 177, 194), Color.WHITE, IconHelper.barChart(18));

        filterCard.add(new JLabel("Tìm kiếm: ")); filterCard.add(txtSearch);
        filterCard.add(new JLabel("Hành động: ")); filterCard.add(cbActionType);
        filterCard.add(new JLabel("Đối tượng: ")); filterCard.add(cbEntity);
        filterCard.add(btnFilter);
        filterCard.add(btnExport);

        // ── 3. BẢNG DỮ LIỆU (AUDIT TABLE) ────────────────────────────────────
        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        tableModel = new DefaultTableModel(new Object[]{"Thời gian", "Tài khoản", "IP Address", "Hành động", "Đối tượng", "ID Đối tượng", "Chi tiết thay đổi"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLogs = new JTable(tableModel);
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblLogs);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        tableCard.add(scrollPane, BorderLayout.CENTER);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setOpaque(false);
        centerPanel.add(filterCard, BorderLayout.NORTH);
        centerPanel.add(tableCard, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private void initEvents() {
        btnFilter.addActionListener(e -> applyFilter());
        txtSearch.addActionListener(e -> applyFilter());
        cbActionType.addActionListener(e -> applyFilter());
        cbEntity.addActionListener(e -> applyFilter());

        // SỰ KIỆN NHẤP CHUỘT VÀO HÀNG ĐỂ XEM CHI TIẾT
        tblLogs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) { 
                    showLogDetailDialog();
                }
            }
        });

        // -------------------------------------------------------------
        // TÍNH NĂNG TỰ ĐỘNG LÀM MỚI (AUTO-REFRESH) MỖI 5 GIÂY (5000ms)
        // -------------------------------------------------------------
        autoRefreshTimer = new Timer(5000, e -> {
            // Vẫn giữ nguyên các điều kiện lọc (Action, Entity, Search)
            applyFilter(); 
        });
        autoRefreshTimer.start();
    }

    private void showLogDetailDialog() {
        int row = tblLogs.getSelectedRow();
        if (row < 0) return;

        // Lấy dữ liệu từ hàng được chọn
        String time = tblLogs.getValueAt(row, 0).toString();
        String user = tblLogs.getValueAt(row, 1).toString();
        String ip = tblLogs.getValueAt(row, 2).toString();
        String action = tblLogs.getValueAt(row, 3).toString();
        String entity = tblLogs.getValueAt(row, 4).toString();
        String entityId = tblLogs.getValueAt(row, 5).toString();
        String details = tblLogs.getValueAt(row, 6).toString();

        // Tạo giao diện Popup đẹp
        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setPreferredSize(new Dimension(500, 400));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        StringBuilder html = new StringBuilder("<html><body style='font-family:Segoe UI; font-size:11pt;'>");
        html.append("<h2 style='color:#365CF5;'>Thông Tin Chi Tiết Log</h2>");
        html.append("<hr color='#E2E8F0'>");
        html.append("<b>⌚ Thời gian:</b> ").append(time).append("<br><br>");
        html.append("<b>👤 Tài khoản:</b> ").append(user).append("<br><br>");
        html.append("<b>🌐 Địa chỉ IP:</b> ").append(ip).append("<br><br>");
        html.append("<b>⚡ Hành động:</b> ").append(action).append("<br><br>");
        html.append("<b>📦 Đối tượng:</b> ").append(entity).append(" (ID: ").append(entityId).append(")<br><br>");
        html.append("<b>📝 Nội dung thay đổi:</b>");
        html.append("</body></html>");

        JLabel lblInfo = new JLabel(html.toString());
        
        JTextArea txtDetails = new JTextArea(details);
        txtDetails.setFont(new Font("Consolas", Font.PLAIN, 14));
        txtDetails.setLineWrap(true);
        txtDetails.setWrapStyleWord(true);
        txtDetails.setEditable(false);
        txtDetails.setBackground(new Color(248, 249, 252));
        txtDetails.setBorder(new EmptyBorder(10, 10, 10, 10));

        JScrollPane sp = new JScrollPane(txtDetails);
        sp.setBorder(BorderFactory.createLineBorder(borderGray));

        panel.add(lblInfo, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, panel, "Chi tiết hoạt động", JOptionPane.PLAIN_MESSAGE);
    }

    private void applyFilter() {
        String action = cbActionType.getSelectedItem().toString();
        String entity = cbEntity.getSelectedItem().toString();
        String keyword = txtSearch.getText();
        loadRealData(action, entity, keyword);
    }

    // -------------------------------------------------------------
    // HÀM PHIÊN DỊCH CÁC MÃ CODE CỨNG SANG TIẾNG VIỆT
    // -------------------------------------------------------------
    private String translateReadable(String text) {
        if (text == null) return null;
        return text.replace("R_ADMIN_ALL", "Toàn quyền hệ thống")
                   .replace("R_STORE_MNG", "Quản lý chi nhánh")
                   .replace("R_STAFF_SALE", "Nhân viên bán hàng")
                   .replace("R_STAFF_VIEW_PROD", "Nhân viên kho")
                   .replace("Quyền:", "Chức vụ:");
    }

    private void loadRealData(String actionFilter, String entityFilter, String searchKeyword) {
        int selectedRow = tblLogs.getSelectedRow(); // Ghi nhớ dòng đang chọn (nếu có)
        tableModel.setRowCount(0);

        StringBuilder sql = new StringBuilder(
            "SELECT TO_CHAR(a.CREATED_AT, 'YYYY-MM-DD HH24:MI:SS') AS log_time, " +
            "acc.username, a.IP_ADDRESS, a.ACTION_TYPE, a.ENTITY_TYPE, a.ENTITY_ID, " +
            "a.NEW_VALUE, a.OLD_VALUE " +
            "FROM AUDIT_LOG a " +
            "LEFT JOIN ACCOUNTS acc ON a.ACCOUNT_ID = acc.ACCOUNT_ID " +
            "WHERE a.IS_DELETED = 0 "
        );

        if (actionFilter != null && !actionFilter.equals("Tất cả Hành động")) {
            sql.append(" AND a.ACTION_TYPE = '").append(actionFilter).append("' ");
        }
        if (entityFilter != null && !entityFilter.equals("Tất cả Đối tượng")) {
            sql.append(" AND a.ENTITY_TYPE = '").append(entityFilter).append("' ");
        }
        if (searchKeyword != null && !searchKeyword.trim().isEmpty()) {
            sql.append(" AND (LOWER(acc.username) LIKE LOWER('%").append(searchKeyword).append("%') ")
               .append(" OR a.IP_ADDRESS LIKE '%").append(searchKeyword).append("%') ");
        }
        sql.append(" ORDER BY a.CREATED_AT DESC");

        try (java.sql.Connection con = common.db.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql.toString());
             java.sql.ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String time = rs.getString("log_time");
                String user = rs.getString("username") != null ? rs.getString("username") : "SYSTEM";
                String ip = rs.getString("IP_ADDRESS");
                String action = rs.getString("ACTION_TYPE");
                String entity = rs.getString("ENTITY_TYPE");
                String entityId = rs.getString("ENTITY_ID");
                
                // Sử dụng bộ phiên dịch để chuyển sang tiếng Việt trước khi nối chuỗi
                String newValue = translateReadable(rs.getString("NEW_VALUE"));
                String oldValue = translateReadable(rs.getString("OLD_VALUE"));
                
                String details = "";
                if ("CẬP NHẬT".equals(action) && oldValue != null && newValue != null) {
                    details = oldValue + "  ➡️  " + newValue; 
                } else if (newValue != null && !newValue.isEmpty()) {
                    details = newValue;
                } else if (oldValue != null && !oldValue.isEmpty()) {
                    details = "Đã xóa: " + oldValue;
                }

                tableModel.addRow(new Object[]{time, user, ip, action, entity, entityId, details});
            }
            
            // Khôi phục lại dòng đang chọn (để bảng không bị nháy mất màu xanh khi Auto-refresh)
            if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
                tblLogs.setRowSelectionInterval(selectedRow, selectedRow);
            }

        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupTableStyle() {
        tblLogs.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); 
        
        tblLogs.setRowHeight(38);
        tblLogs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblLogs.setShowVerticalLines(false);
        tblLogs.setSelectionBackground(new Color(237, 242, 255));
        tblLogs.setSelectionForeground(textDark);
        tblLogs.getTableHeader().setReorderingAllowed(false);

        tblLogs.getColumnModel().getColumn(0).setPreferredWidth(160); 
        tblLogs.getColumnModel().getColumn(1).setPreferredWidth(120); 
        tblLogs.getColumnModel().getColumn(2).setPreferredWidth(120); 
        tblLogs.getColumnModel().getColumn(3).setPreferredWidth(100); 
        tblLogs.getColumnModel().getColumn(4).setPreferredWidth(120); 
        tblLogs.getColumnModel().getColumn(5).setPreferredWidth(150); 
        tblLogs.getColumnModel().getColumn(6).setPreferredWidth(600); // Chi tiết hiển thị rất rộng

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(textDark);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        for (int i = 0; i < tblLogs.getColumnCount(); i++) {
            tblLogs.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
    }

    // --- CÁC HÀM TIỆN ÍCH UI KHÁC ---
    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(borderGray, 8), new EmptyBorder(5, 10, 5, 10)));
        return txt;
    }

    private JButton createCustomButton(String t, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(t);
        if (icon != null) btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(18, 18, 1)));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg); btn.setBackground(bg);
        btn.setPreferredSize(new Dimension(130, 40));
        btn.setCursor(new Cursor(12)); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground()); g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 20, 20);
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