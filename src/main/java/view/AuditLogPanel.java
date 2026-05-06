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

    public AuditLogPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));

        initUI();
        loadDummyData(); // Load data ảo để test giao diện
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

        // ── 2. FILTER BAR (Thanh tìm kiếm & Lọc) ─────────────────────────────
        RoundedPanel filterCard = new RoundedPanel(15, cardWhite);
        filterCard.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 15));
        filterCard.setBorder(new EmptyBorder(5, 10, 5, 10));

        txtSearch = createTextField("Tìm kiếm theo tài khoản hoặc IP...");
        txtSearch.setPreferredSize(new Dimension(280, 40));

        cbActionType = new JComboBox<>(new String[]{"Tất cả Hành động", "THÊM MỚI (INSERT)", "CẬP NHẬT (UPDATE)", "XÓA (DELETE)", "ĐĂNG NHẬP (LOGIN)"});
        cbActionType.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbActionType.setPreferredSize(new Dimension(180, 40));

        cbEntity = new JComboBox<>(new String[]{"Tất cả Đối tượng", "TÀI KHOẢN", "SẢN PHẨM", "HÓA ĐƠN", "KHÁCH HÀNG"});
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

        tableModel = new DefaultTableModel(new Object[]{"Thời gian", "Tài khoản (ID)", "IP Address", "Hành động", "Đối tượng", "ID Đối tượng", "Nội dung thay đổi"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblLogs = new JTable(tableModel);
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblLogs);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        tableCard.add(scrollPane, BorderLayout.CENTER);

        // Ghép Filter và Table vào Center Panel
        JPanel centerPanel = new JPanel(new BorderLayout(0, 15));
        centerPanel.setOpaque(false);
        centerPanel.add(filterCard, BorderLayout.NORTH);
        centerPanel.add(tableCard, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private void loadDummyData() {
        // Data giả lập để bạn xem UI trước khi code Data thật
        tableModel.addRow(new Object[]{"2026-05-06 08:30:15", "MNG_TungND", "192.168.1.12", "CẬP NHẬT", "SẢN PHẨM", "SP001", "Giá: 50k -> 55k"});
        tableModel.addRow(new Object[]{"2026-05-06 09:12:05", "EMP_ViLT", "192.168.1.15", "THÊM MỚI", "HÓA ĐƠN", "HD1029", "Tạo hóa đơn mới"});
        tableModel.addRow(new Object[]{"2026-05-06 10:45:22", "ADMIN_Tong", "14.161.45.2", "XÓA", "TÀI KHOẢN", "ACC_099", "Thu hồi quyền QL"});
        tableModel.addRow(new Object[]{"2026-05-06 11:05:00", "EMP_NguyenK", "192.168.1.44", "ĐĂNG NHẬP", "HỆ THỐNG", "-", "Đăng nhập thành công"});
    }

    // --- CÁC HÀM TIỆN ÍCH (Giống hệt các Form trước) ---
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

    private void setupTableStyle() {
        tblLogs.setRowHeight(35);
        tblLogs.setFont(new Font("Consolas", Font.PLAIN, 13)); // Dùng font monospace cho Log dễ đọc
        tblLogs.setShowVerticalLines(false);
        tblLogs.setSelectionBackground(new Color(237, 242, 255));
        tblLogs.setSelectionForeground(textDark);
        tblLogs.getTableHeader().setReorderingAllowed(false);

        // Chỉnh kích thước các cột
        tblLogs.getColumnModel().getColumn(0).setPreferredWidth(140); // Thời gian
        tblLogs.getColumnModel().getColumn(6).setPreferredWidth(250); // Nội dung

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(textDark);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        for (int i = 0; i < tblLogs.getColumnCount(); i++) {
            tblLogs.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
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