package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
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
    private JButton btnFilter, btnRefresh, btnExport;

    public AuditLogPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        initEvents(); 
        
        loadRealData("Tất cả Hành động", "Tất cả Đối tượng", ""); 
        setupRealtimeSync();
    }

    private void initUI() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Nhật Ký Hoạt Động Hệ Thống (Audit Trail)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Giám sát mọi thao tác Thêm/Sửa/Xóa của tất cả các tài khoản theo thời gian thực");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        add(headerPanel, BorderLayout.NORTH);

        RoundedPanel filterCard = new RoundedPanel(15, cardWhite);
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));
        filterCard.setBorder(new EmptyBorder(5, 10, 5, 10));

        txtSearch = createTextField("Tìm kiếm theo tài khoản hoặc IP...");
        txtSearch.setPreferredSize(new Dimension(280, 40));

        // 🔥 ĐÃ GỠ BỎ "ĐĂNG NHẬP", "ĐĂNG XUẤT" KHỎI BỘ LỌC
        cbActionType = new JComboBox<>(new String[]{
            "Tất cả Hành động", "THÊM MỚI", "CẬP NHẬT", "XÓA", "XUẤT FILE"
        });
        cbActionType.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbActionType.setPreferredSize(new Dimension(180, 40));

        cbEntity = new JComboBox<>(new String[]{
            "Tất cả Đối tượng", "ROLES", "ACCOUNTS", "EMPLOYEES", "PRODUCTS", "ORDERS", "CUSTOMERS", "INVENTORY", "HỆ THỐNG"
        });
        cbEntity.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbEntity.setPreferredSize(new Dimension(180, 40));

        btnFilter = createCustomButton("Lọc dữ liệu", new Color(54, 92, 245), Color.WHITE, IconHelper.search(18));
        btnRefresh = createCustomButton("Cập nhật", new Color(23, 162, 184), Color.WHITE, IconHelper.refresh(18)); 
        btnExport = createCustomButton("Xuất Excel", new Color(165, 177, 194), Color.WHITE, IconHelper.barChart(18));

        filterCard.add(new JLabel("Tìm kiếm: ")); filterCard.add(txtSearch);
        filterCard.add(new JLabel("Hành động: ")); filterCard.add(cbActionType);
        filterCard.add(new JLabel("Đối tượng: ")); filterCard.add(cbEntity);
        filterCard.add(btnFilter);
        filterCard.add(btnRefresh); 
        filterCard.add(btnExport);

        RoundedPanel tableCard = new RoundedPanel(20, cardWhite);
        tableCard.setLayout(new BorderLayout());
        tableCard.setBorder(new EmptyBorder(10, 10, 10, 10));

        // CỘT SỐ 7: LÀ CỘT ẨN DÙNG ĐỂ CHỨA DỮ LIỆU ĐẦY ĐỦ CHO POPUP
        tableModel = new DefaultTableModel(new Object[]{
            "Thời gian", "Tài khoản", "IP Address", "Hành động", "Đối tượng", "Mã Đối tượng (ID)", "Chi tiết thay đổi", "FullDetails"
        }, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLogs = new JTable(tableModel);
        
        // Giấu cột FullDetails đi, không cho hiện trên bảng chính
        tblLogs.removeColumn(tblLogs.getColumnModel().getColumn(7));
        
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
        btnRefresh.addActionListener(e -> resetAndRefresh());

        tblLogs.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { 
                    showLogDetailDialog();
                }
            }
        });
        
        btnExport.addActionListener(e -> {
             JOptionPane.showMessageDialog(this, "Tính năng Xuất Excel đang được xây dựng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void resetAndRefresh() {
        txtSearch.setText("");
        cbActionType.setSelectedIndex(0);
        cbEntity.setSelectedIndex(0);
        applyFilter();
    }

    private void setupRealtimeSync() {
        try {
            EventBus.subscribe(AppDataChangedEvent.class, event -> {
                SwingUtilities.invokeLater(() -> applyFilter());
            });
        } catch (Exception e) {
            System.err.println("Lỗi thiết lập real-time Audit Log: " + e.getMessage());
        }
    }

    // =====================================================================
    // NÂNG CẤP GIAO DIỆN POPUP CHI TIẾT
    // =====================================================================
    private void showLogDetailDialog() {
        int row = tblLogs.getSelectedRow();
        if (row < 0) return;
        
        // Chuyển đổi index dòng từ View sang Model (Bắt buộc vì ta đã giấu cột)
        int modelRow = tblLogs.convertRowIndexToModel(row);

        String time = tableModel.getValueAt(modelRow, 0).toString();
        String user = tableModel.getValueAt(modelRow, 1).toString();
        String ip = tableModel.getValueAt(modelRow, 2).toString();
        String action = tableModel.getValueAt(modelRow, 3).toString();
        String entity = tableModel.getValueAt(modelRow, 4).toString();
        String entityId = tableModel.getValueAt(modelRow, 5).toString();
        
        // Lấy chuỗi FullDetails từ cột ẩn số 7 thay vì cột 6
        String fullDetails = tableModel.getValueAt(modelRow, 7).toString();

        JPanel panel = new JPanel(new BorderLayout(10, 15));
        panel.setPreferredSize(new Dimension(600, 450));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        StringBuilder html = new StringBuilder("<html><body style='font-family:Segoe UI; font-size:11pt;'>");
        html.append("<h2 style='color:#365CF5; margin-bottom:5px;'>Thông Tin Chi Tiết Giao Dịch</h2>");
        html.append("<hr color='#E2E8F0'>");
        html.append("<table width='100%' cellpadding='5'>");
        html.append("<tr><td width='120'><b>⌚ Thời gian:</b></td><td>").append(time).append("</td></tr>");
        html.append("<tr><td><b>👤 Tài khoản:</b></td><td><span style='color:#E63946; font-weight:bold;'>").append(user).append("</span></td></tr>");
        html.append("<tr><td><b>🌐 Địa chỉ IP:</b></td><td>").append(ip).append("</td></tr>");
        html.append("<tr><td><b>⚡ Hành động:</b></td><td>").append(action).append("</td></tr>");
        html.append("<tr><td><b>📦 Đối tượng:</b></td><td>").append(entity).append(" (Mã: ").append(entityId).append(")</td></tr>");
        html.append("</table><br><b>📝 Nội dung thay đổi chi tiết:</b>");
        html.append("</body></html>");

        JLabel lblInfo = new JLabel(html.toString());
        
        JTextArea txtDetails = new JTextArea(fullDetails); // Hiện chuỗi FullDetails lên
        txtDetails.setFont(new Font("Consolas", Font.PLAIN, 15));
        txtDetails.setLineWrap(true);
        txtDetails.setWrapStyleWord(true);
        txtDetails.setEditable(false);
        txtDetails.setBackground(new Color(248, 249, 252));
        txtDetails.setBorder(new EmptyBorder(15, 15, 15, 15));

        JScrollPane sp = new JScrollPane(txtDetails);
        sp.setBorder(BorderFactory.createLineBorder(borderGray));

        panel.add(lblInfo, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);

        JOptionPane.showMessageDialog(this, panel, "Truy vết hệ thống (Audit Trail)", JOptionPane.PLAIN_MESSAGE);
    }

    private void applyFilter() {
        String action = cbActionType.getSelectedItem().toString();
        String entity = cbEntity.getSelectedItem().toString();
        String keyword = txtSearch.getText();
        loadRealData(action, entity, keyword);
    }

    private String translateReadable(String text) {
        if (text == null) return null;
        return text.replace("R_ADMIN_ALL", "Quản trị viên")
                   .replace("R_STORE_MNG", "Quản lý cửa hàng")
                   .replace("R_STAFF_SALE", "Nhân viên bán hàng")
                   .replace("R_STAFF_STOCK", "Nhân viên kho")
                   .replace("R_STAFF_VIEW_PROD", "Nhân viên kho");
    }

    private void loadRealData(String actionFilter, String entityFilter, String searchKeyword) {
        int selectedRow = tblLogs.getSelectedRow();
        tableModel.setRowCount(0);

        // 🔥 CHỈ LẤY CÁC LOG KHÔNG PHẢI LÀ ĐĂNG NHẬP/ĐĂNG XUẤT
        StringBuilder sql = new StringBuilder(
            "SELECT TO_CHAR(a.CREATED_AT, 'DD/MM/YYYY HH24:MI:SS') AS log_time, " +
            "acc.username, a.IP_ADDRESS, a.ACTION_TYPE, a.ENTITY_TYPE, a.ENTITY_ID, " +
            "a.NEW_VALUE, a.OLD_VALUE, a.REASON, " + 
            "(CASE " +
            "  WHEN a.ENTITY_TYPE = 'ACCOUNTS' THEN (SELECT u.full_name FROM ACCOUNTS ac JOIN USERS u ON ac.user_id = u.user_id WHERE ac.account_id = a.ENTITY_ID) " +
            "  WHEN a.ENTITY_TYPE = 'EMPLOYEES' THEN (SELECT employee_name FROM EMPLOYEES WHERE employee_id = a.ENTITY_ID) " +
            "  WHEN a.ENTITY_TYPE = 'PRODUCTS' THEN (SELECT product_name FROM PRODUCTS WHERE product_id = a.ENTITY_ID) " +
            "  WHEN a.ENTITY_TYPE = 'CUSTOMERS' THEN (SELECT customer_name FROM CUSTOMERS WHERE customer_id = a.ENTITY_ID) " +
            "  WHEN a.ENTITY_TYPE = 'ROLES' THEN (SELECT role_name FROM ROLES WHERE role_id = a.ENTITY_ID) " +
            "  ELSE CAST(a.ENTITY_ID AS NVARCHAR2(50)) " +
            "END) AS target_name " +
            "FROM AUDIT_LOG a " +
            "LEFT JOIN ACCOUNTS acc ON a.ACCOUNT_ID = acc.ACCOUNT_ID " +
            "WHERE a.IS_DELETED = 0 AND UPPER(a.ACTION_TYPE) NOT IN ('ĐĂNG NHẬP', 'ĐĂNG XUẤT') "
        );

        if (actionFilter != null && !actionFilter.equals("Tất cả Hành động")) {
            sql.append(" AND UPPER(a.ACTION_TYPE) = '").append(actionFilter).append("' ");
        }
        if (entityFilter != null && !entityFilter.equals("Tất cả Đối tượng")) {
            sql.append(" AND UPPER(a.ENTITY_TYPE) = '").append(entityFilter).append("' ");
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
                String user = rs.getString("username");
                if(user == null || user.isEmpty()) user = "Hệ thống (SYSTEM)";
                
                String ip = rs.getString("IP_ADDRESS");
                if(ip == null || ip.isEmpty()) ip = "Localhost";
                
                String action = rs.getString("ACTION_TYPE");
                String entity = rs.getString("ENTITY_TYPE");
                String entityId = rs.getString("ENTITY_ID");
                
                String reason = rs.getString("REASON");
                if (reason == null || reason.isEmpty()) reason = "Hệ thống ghi nhận tự động";
                
                String targetName = rs.getString("target_name");
                if (targetName == null || targetName.isEmpty()) targetName = entityId;
                
                String newValue = translateReadable(rs.getString("NEW_VALUE"));
                String oldValue = translateReadable(rs.getString("OLD_VALUE"));
                
                String details = ""; // Nội dung rút gọn hiện ở Bảng
                String fullDetails = ""; // Nội dung đầy đủ có Enter xuống dòng hiện ở Popup
                
                if ("ROLES".equalsIgnoreCase(entity) && "CẬP NHẬT".equalsIgnoreCase(action)) {
                    details = "Chỉnh quyền [" + targetName + "]: " + newValue; 
                    fullDetails = "🔹 Chức vụ bị tác động:\n   " + targetName + " (" + entityId + ")\n\n" +
                                  "🔹 Chi tiết thay đổi:\n   " + newValue + "\n\n" + // Hiển thị Bật/Tắt
                                  "🔹 Quyền hạn trước đó:\n   " + oldValue + "\n\n" +
                                  "💡 Ghi chú:\n   " + reason;
                                  
                } else if ("CẬP NHẬT".equalsIgnoreCase(action) && oldValue != null && newValue != null) {
                    details = "Cập nhật [" + targetName + "]: " + oldValue + " sang " + newValue; 
                    fullDetails = "🔹 Đối tượng tác động:\n   " + targetName + " (" + entityId + ")\n\n" +
                                  "🔹 Dữ liệu ban đầu:\n   " + oldValue + "\n\n" +
                                  "🔹 Dữ liệu sau khi cập nhật:\n   " + newValue + "\n\n" +
                                  "💡 Lý do / Ghi chú:\n   " + reason;
                    
                    }else if (newValue != null && !newValue.isEmpty()) {
                    details = "Thêm mới [" + targetName + "]: " + newValue;
                    fullDetails = "🔹 Đối tượng tác động:\n   " + targetName + " (" + entityId + ")\n\n" +
                                  "🔹 Dữ liệu được thêm mới:\n   " + newValue + "\n\n" +
                                  "💡 Lý do / Ghi chú:\n   " + reason;
                                  
                } else if (oldValue != null && !oldValue.isEmpty()) {
                    details = "Đã xóa [" + targetName + "]: " + oldValue;
                    fullDetails = "🔹 Đối tượng tác động:\n   " + targetName + " (" + entityId + ")\n\n" +
                                  "🔹 Dữ liệu bị xóa:\n   " + oldValue + "\n\n" +
                                  "💡 Lý do / Ghi chú:\n   " + reason;
                } else {
                    details = "Thao tác trên [" + targetName + "]";
                    fullDetails = "🔹 Đối tượng tác động:\n   " + targetName + " (" + entityId + ")\n\n" +
                                  "💡 Lý do / Ghi chú:\n   " + reason;
                }

                tableModel.addRow(new Object[]{time, user, ip, action, entity, entityId, details, fullDetails});
            }
            
            if (selectedRow >= 0 && selectedRow < tableModel.getRowCount()) {
                tblLogs.setRowSelectionInterval(selectedRow, selectedRow);
            }

        } catch (Exception e) { 
            e.printStackTrace(); 
            JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu: " + e.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void setupTableStyle() {
        tblLogs.setAutoResizeMode(JTable.AUTO_RESIZE_OFF); 
        
        tblLogs.setRowHeight(40); 
        tblLogs.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        tblLogs.setShowVerticalLines(false);
        tblLogs.setSelectionBackground(new Color(237, 242, 255));
        tblLogs.setSelectionForeground(textDark);
        tblLogs.getTableHeader().setReorderingAllowed(false);

        tblLogs.getColumnModel().getColumn(0).setPreferredWidth(160); 
        tblLogs.getColumnModel().getColumn(1).setPreferredWidth(150); 
        tblLogs.getColumnModel().getColumn(2).setPreferredWidth(120); 
        tblLogs.getColumnModel().getColumn(3).setPreferredWidth(130); 
        tblLogs.getColumnModel().getColumn(4).setPreferredWidth(130); 
        tblLogs.getColumnModel().getColumn(5).setPreferredWidth(150); 
        tblLogs.getColumnModel().getColumn(6).setPreferredWidth(600); 

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(textDark);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(12, 10, 12, 10));

        for (int i = 0; i < tblLogs.getColumnCount(); i++) {
            tblLogs.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
    }

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
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false);
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