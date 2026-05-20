package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import view.components.IconHelper;

public class PromotionManagementPanel extends JPanel {

    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color borderGray = new Color(230, 235, 241);
    private final Color primaryBlue = new Color(67, 97, 238);
    private final Color successGreen = new Color(16, 185, 129);
    private final Color warningOrange = new Color(245, 158, 11);

    private JTable tblPromos;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JComboBox<String> cbFilterStatus;
    private JLabel lblTotalPromos, lblActivePromos, lblEndedPromos;

    private JTextField txtMaKM;
    private JTextField txtTenKM;
    private JSpinner spinGiamGia;
    private JTextField txtTuNgay;
    private JTextField txtDenNgay;
    private JComboBox<String> cbSieuThi;
    private JComboBox<String> cbSanPham;
    private JComboBox<String> cbHangThanhVien;
    private JComboBox<String> cbTrangThai;
    private JButton btnSave, btnClear;

    private boolean isEditMode = false;

    public PromotionManagementPanel() {
        setLayout(new BorderLayout(20, 20));
        setBackground(bgLight);
        setBorder(new EmptyBorder(20, 30, 20, 30));
        initUI();
        initEvents();
        loadPromoData("", "Tất cả");
    }

    private void initUI() {
        JPanel topContainer = new JPanel(new BorderLayout(0, 15));
        topContainer.setOpaque(false);
        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        JLabel lblTitle = new JLabel("Quản Lý Chiến Dịch Khuyến Mãi");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblTitle.setForeground(textDark);
        JLabel lblSub = new JLabel("Thiết lập mã giảm giá, cấu hình phạm vi áp dụng và đối tượng khách hàng");
        lblSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblSub.setForeground(textGray);
        titlePanel.add(lblTitle);
        titlePanel.add(lblSub);

        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setOpaque(false);
        lblTotalPromos = new JLabel("0", SwingConstants.CENTER);
        lblActivePromos = new JLabel("0", SwingConstants.CENTER);
        lblEndedPromos = new JLabel("0", SwingConstants.CENTER);
        statsPanel.add(createStatCard("Tổng Chương Trình", lblTotalPromos, primaryBlue));
        statsPanel.add(createStatCard("Đang Diễn Ra", lblActivePromos, successGreen));
        statsPanel.add(createStatCard("Đã Kết Thúc", lblEndedPromos, warningOrange));

        topContainer.add(titlePanel, BorderLayout.WEST);
        topContainer.add(statsPanel, BorderLayout.EAST);
        add(topContainer, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(20, 0));
        contentPanel.setOpaque(false);
        contentPanel.add(createLeftPanel(), BorderLayout.CENTER);
        contentPanel.add(createRightPanel(), BorderLayout.EAST);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        RoundedPanel card = new RoundedPanel(15, cardWhite);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor), new EmptyBorder(10, 15, 10, 15)));
        card.setPreferredSize(new Dimension(160, 60));
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(textGray);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(textDark);
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createLeftPanel() {
        RoundedPanel leftCard = new RoundedPanel(20, cardWhite);
        leftCard.setLayout(new BorderLayout(0, 15));
        leftCard.setBorder(new EmptyBorder(20, 20, 20, 20));
        JPanel toolBar = new JPanel(new BorderLayout());
        toolBar.setOpaque(false);
        JLabel lblListTitle = new JLabel("Danh sách Khuyến mãi");
        lblListTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblListTitle.setForeground(textDark);

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchBox.setOpaque(false);
        cbFilterStatus = new JComboBox<>(new String[]{"Tất cả", "Đang diễn ra", "Sắp diễn ra", "Đã kết thúc", "Tạm ngưng / Kết thúc"});
        cbFilterStatus.setPreferredSize(new Dimension(150, 38));
        cbFilterStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txtSearch = createTextField("Tra mã KM, Tên CT...");
        txtSearch.setPreferredSize(new Dimension(200, 38));
        JButton btnSearch = createCustomButton("Tìm", primaryBlue, Color.WHITE, null);
        btnSearch.setPreferredSize(new Dimension(80, 38));
        searchBox.add(cbFilterStatus);
        searchBox.add(txtSearch);
        searchBox.add(btnSearch);
        toolBar.add(lblListTitle, BorderLayout.WEST);
        toolBar.add(searchBox, BorderLayout.EAST);

        tableModel = new DefaultTableModel(new Object[]{"Mã KM", "Tên Chương Trình", "Mức giảm", "Từ ngày", "Đến ngày", "Trạng thái"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tblPromos = new JTable(tableModel);
        setupTableStyle();
        JScrollPane scrollPane = new JScrollPane(tblPromos);
        scrollPane.setBorder(BorderFactory.createLineBorder(borderGray));
        scrollPane.getViewport().setBackground(Color.WHITE);
        leftCard.add(toolBar, BorderLayout.NORTH);
        leftCard.add(scrollPane, BorderLayout.CENTER);
        return leftCard;
    }

    private JPanel createRightPanel() {
        RoundedPanel rightCard = new RoundedPanel(20, cardWhite);
        rightCard.setPreferredSize(new Dimension(420, 0));
        rightCard.setLayout(new BorderLayout());
        rightCard.setBorder(new EmptyBorder(25, 25, 25, 25));
        JLabel lblFormTitle = new JLabel("Cấu Hình Khuyến Mãi");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblFormTitle.setForeground(textDark);
        lblFormTitle.setBorder(new EmptyBorder(0, 0, 15, 0));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 12, 0);

        gbc.gridy = 0;
        JPanel row1 = new JPanel(new GridLayout(1, 2, 10, 0)); row1.setOpaque(false);
        JPanel pnlMa = new JPanel(new BorderLayout()); pnlMa.setOpaque(false);
        pnlMa.add(createFormLabel("Mã KM:"), BorderLayout.NORTH);
        txtMaKM = createTextField("VD: TET2026"); pnlMa.add(txtMaKM, BorderLayout.CENTER);
        JPanel pnlGiam = new JPanel(new BorderLayout()); pnlGiam.setOpaque(false);
        pnlGiam.add(createFormLabel("Mức giảm (%):"), BorderLayout.NORTH);
        spinGiamGia = new JSpinner(new SpinnerNumberModel(5, 0, 100, 1));
        spinGiamGia.setFont(new Font("Segoe UI", Font.BOLD, 14)); pnlGiam.add(spinGiamGia, BorderLayout.CENTER);
        row1.add(pnlMa); row1.add(pnlGiam); formPanel.add(row1, gbc);

        gbc.gridy = 1; formPanel.add(createFormLabel("Tên chương trình:"), gbc);
        gbc.gridy = 2; txtTenKM = createTextField("Nhập tên sự kiện..."); formPanel.add(txtTenKM, gbc);
        gbc.gridy = 3;
        JPanel rowTime = new JPanel(new GridLayout(1, 2, 10, 0)); rowTime.setOpaque(false);
        JPanel pnlTu = new JPanel(new BorderLayout()); pnlTu.setOpaque(false);
        pnlTu.add(createFormLabel("Từ ngày (YYYY-MM-DD):"), BorderLayout.NORTH);
        txtTuNgay = createTextField("2026-01-01"); pnlTu.add(txtTuNgay, BorderLayout.CENTER);
        JPanel pnlDen = new JPanel(new BorderLayout()); pnlDen.setOpaque(false);
        pnlDen.add(createFormLabel("Đến ngày (YYYY-MM-DD):"), BorderLayout.NORTH);
        txtDenNgay = createTextField("2026-12-31"); pnlDen.add(txtDenNgay, BorderLayout.CENTER);
        rowTime.add(pnlTu); rowTime.add(pnlDen); formPanel.add(rowTime, gbc);

        gbc.gridy = 4; formPanel.add(createFormLabel("Áp dụng tại Chi nhánh:"), gbc);
        gbc.gridy = 5; cbSieuThi = new JComboBox<>(new String[]{"Tất cả siêu thị trên toàn quốc"}); cbSieuThi.setPreferredSize(new Dimension(0, 35)); formPanel.add(cbSieuThi, gbc);
        gbc.gridy = 6; formPanel.add(createFormLabel("Áp dụng cho Sản phẩm:"), gbc);
        gbc.gridy = 7; cbSanPham = new JComboBox<>(new String[]{"Tất cả mặt hàng", "Chỉ Hàng tiêu dùng", "Chỉ Thực phẩm tươi sống"}); cbSanPham.setPreferredSize(new Dimension(0, 35)); formPanel.add(cbSanPham, gbc);
        gbc.gridy = 8; formPanel.add(createFormLabel("Áp dụng cho Hạng Thành Viên:"), gbc);
        gbc.gridy = 9; cbHangThanhVien = new JComboBox<>(new String[]{"Tất cả hạng (Kể cả khách vãng lai)", "Chỉ thành viên Đồng trở lên", "Chỉ thành viên Bạc trở lên", "Chỉ thành viên Vàng trở lên"}); cbHangThanhVien.setPreferredSize(new Dimension(0, 35)); formPanel.add(cbHangThanhVien, gbc);
        gbc.gridy = 10; formPanel.add(createFormLabel("Trạng thái hiện tại:"), gbc);
        gbc.gridy = 11; cbTrangThai = new JComboBox<>(new String[]{"Đang diễn ra", "Sắp diễn ra", "Đã kết thúc", "Tạm ngưng / Kết thúc"}); cbTrangThai.setPreferredSize(new Dimension(0, 35)); formPanel.add(cbTrangThai, gbc);

        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        btnClear = createCustomButton("Làm Mới", new Color(235, 238, 244), textDark, null);
        btnSave = createCustomButton("Lưu Khuyến Mãi", primaryBlue, Color.WHITE, null);
        actionPanel.add(btnClear); actionPanel.add(btnSave);
        rightCard.add(lblFormTitle, BorderLayout.NORTH);
        rightCard.add(formPanel, BorderLayout.CENTER);
        rightCard.add(actionPanel, BorderLayout.SOUTH);
        return rightCard;
    }

    private void initEvents() {
        btnClear.addActionListener(e -> clearForm());
        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { doSearch(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { doSearch(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { doSearch(); }
        });
        cbFilterStatus.addActionListener(e -> doSearch());
        tblPromos.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                int row = tblPromos.getSelectedRow();
                if (row >= 0) {
                    isEditMode = true;
                    txtMaKM.setText(tableModel.getValueAt(row, 0).toString());
                    txtMaKM.setEnabled(false);
                    loadPromoDetailsToForm(tableModel.getValueAt(row, 0).toString());
                }
            }
        });
        btnSave.addActionListener(e -> savePromo());
    }

    private void doSearch() { loadPromoData(txtSearch.getText().trim(), cbFilterStatus.getSelectedItem().toString()); }

    private void clearForm() {
        isEditMode = false;
        txtMaKM.setText(""); txtMaKM.setEnabled(true); txtTenKM.setText(""); spinGiamGia.setValue(5);
        txtTuNgay.setText(""); txtDenNgay.setText(""); cbSieuThi.setSelectedIndex(0); cbSanPham.setSelectedIndex(0);
        cbHangThanhVien.setSelectedIndex(0); cbTrangThai.setSelectedIndex(0); tblPromos.clearSelection();
    }

    private void loadPromoData(String keyword, String statusFilter) {
        tableModel.setRowCount(0);
        int total = 0, active = 0, ended = 0;
        String sql = "SELECT p.promotion_id AS MAKM, p.promotion_name AS TENKM, NVL(p.discount_amount,0) AS PHANTRAMGIAM, "
                + "TO_CHAR(c.start_date, 'YYYY-MM-DD') AS TUNGAY, TO_CHAR(c.end_date, 'YYYY-MM-DD') AS DENNGAY, p.status AS TRANGTHAI "
                + "FROM PROMOTIONS p LEFT JOIN PROMOTION_CAMPAIGNS c ON p.campaign_id = c.campaign_id "
                + "WHERE NVL(p.is_deleted,0)=0 AND (LOWER(p.promotion_id) LIKE LOWER(?) OR LOWER(p.promotion_name) LIKE LOWER(?)) ";
        if (!"Tất cả".equals(statusFilter)) sql += " AND p.status = ? ";
        sql += " ORDER BY c.start_date DESC NULLS LAST, p.promotion_id DESC";
        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%"); ps.setString(2, "%" + keyword + "%");
            if (!"Tất cả".equals(statusFilter)) ps.setString(3, statusFilter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String trangThai = rs.getString("TRANGTHAI");
                    tableModel.addRow(new Object[]{rs.getString("MAKM"), rs.getString("TENKM"), rs.getInt("PHANTRAMGIAM") + "%", rs.getString("TUNGAY"), rs.getString("DENNGAY"), trangThai});
                    total++;
                    if ("Đang diễn ra".equals(trangThai)) active++;
                    if ("Đã kết thúc".equals(trangThai) || "Tạm ngưng / Kết thúc".equals(trangThai)) ended++;
                }
            }
            lblTotalPromos.setText(String.valueOf(total)); lblActivePromos.setText(String.valueOf(active)); lblEndedPromos.setText(String.valueOf(ended));
        } catch (Exception e) {
            System.err.println("Lỗi tải danh sách Khuyến mãi: " + e.getMessage());
        }
    }

    private void loadPromoDetailsToForm(String maKM) {
        String sql = "SELECT p.promotion_name, NVL(p.discount_amount,0) discount_amount, TO_CHAR(c.start_date,'YYYY-MM-DD') start_date, TO_CHAR(c.end_date,'YYYY-MM-DD') end_date, p.status FROM PROMOTIONS p LEFT JOIN PROMOTION_CAMPAIGNS c ON p.campaign_id=c.campaign_id WHERE p.promotion_id=? AND NVL(p.is_deleted,0)=0";
        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maKM);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtTenKM.setText(rs.getString("promotion_name")); spinGiamGia.setValue(rs.getInt("discount_amount"));
                    txtTuNgay.setText(rs.getString("start_date")); txtDenNgay.setText(rs.getString("end_date")); cbTrangThai.setSelectedItem(rs.getString("status"));
                }
            }
        } catch (Exception e) { System.err.println("Lỗi tải chi tiết Khuyến mãi: " + e.getMessage()); }
    }

    private void savePromo() {
        String ma = txtMaKM.getText().trim(); String ten = txtTenKM.getText().trim(); int giam = (int) spinGiamGia.getValue();
        String tuNgay = txtTuNgay.getText().trim(); String denNgay = txtDenNgay.getText().trim(); String trangThai = cbTrangThai.getSelectedItem().toString();
        if (ma.isEmpty() || ten.isEmpty()) { JOptionPane.showMessageDialog(this, "Vui lòng nhập mã và tên Khuyến mãi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE); return; }
        String campaignId = ("CAMP_" + ma).length() > 50 ? ma : "CAMP_" + ma;
        try (Connection con = common.db.DatabaseConnection.getConnection()) {
            if (isEditMode) {
                try (PreparedStatement ps = con.prepareStatement("UPDATE PROMOTION_CAMPAIGNS SET campaign_name=?, start_date=TO_DATE(NULLIF(?,''),'YYYY-MM-DD'), end_date=TO_DATE(NULLIF(?,''),'YYYY-MM-DD') WHERE campaign_id=?")) {
                    ps.setString(1, ten); ps.setString(2, tuNgay); ps.setString(3, denNgay); ps.setString(4, campaignId); ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("UPDATE PROMOTIONS SET promotion_name=?, campaign_id=?, discount_amount=?, status=? WHERE promotion_id=? AND NVL(is_deleted,0)=0")) {
                    ps.setString(1, ten); ps.setString(2, campaignId); ps.setInt(3, giam); ps.setString(4, trangThai); ps.setString(5, ma); ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Cập nhật Khuyến mãi thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO PROMOTION_CAMPAIGNS (campaign_id, campaign_name, start_date, end_date, is_deleted) VALUES (?, ?, TO_DATE(NULLIF(?,''),'YYYY-MM-DD'), TO_DATE(NULLIF(?,''),'YYYY-MM-DD'), 0)")) {
                    ps.setString(1, campaignId); ps.setString(2, ten); ps.setString(3, tuNgay); ps.setString(4, denNgay); ps.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement("INSERT INTO PROMOTIONS (promotion_id, promotion_name, campaign_id, status, discount_amount, is_deleted) VALUES (?, ?, ?, ?, ?, 0)")) {
                    ps.setString(1, ma); ps.setString(2, ten); ps.setString(3, campaignId); ps.setString(4, trangThai); ps.setInt(5, giam); ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Đã tạo Khuyến mãi mới thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }
            business.service.AuditLogService.logAction(isEditMode ? "CẬP NHẬT" : "THÊM MỚI", "PROMOTIONS", ma, "", "Tên CT: " + ten + " (-" + giam + "%)", "Cập nhật chiến dịch KM");
            clearForm(); doSearch(); EventBus.publish(new AppDataChangedEvent(AppEventType.STORE_INFO, "PROMO_UPDATED"));
        } catch (Exception e) { JOptionPane.showMessageDialog(this, "Lỗi lưu Khuyến mãi: " + e.getMessage(), "Lỗi DB", JOptionPane.ERROR_MESSAGE); }
    }

    private JLabel createFormLabel(String text) { JLabel lbl = new JLabel(text); lbl.setFont(new Font("Segoe UI", Font.BOLD, 12)); lbl.setForeground(textGray); return lbl; }
    private JTextField createTextField(String placeholder) { JTextField txt = new JTextField(); txt.setFont(new Font("Segoe UI", Font.PLAIN, 14)); txt.putClientProperty("JTextField.placeholderText", placeholder); txt.setPreferredSize(new Dimension(0, 35)); txt.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(borderGray, 8), new EmptyBorder(5, 10, 5, 10))); return txt; }

    private void setupTableStyle() {
        tblPromos.setRowHeight(38); tblPromos.setFont(new Font("Segoe UI", Font.PLAIN, 13)); tblPromos.setShowVerticalLines(false);
        tblPromos.setSelectionBackground(new Color(237, 242, 255)); tblPromos.setSelectionForeground(textDark); tblPromos.getTableHeader().setReorderingAllowed(false);
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer(); headerRenderer.setBackground(textDark); headerRenderer.setForeground(Color.WHITE); headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 13)); headerRenderer.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));
        for (int i = 0; i < tblPromos.getColumnCount(); i++) tblPromos.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
    }

    private JButton createCustomButton(String t, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(t); if (icon != null) btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(16, 16, 1)));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13)); btn.setForeground(fg); btn.setBackground(bg); btn.setPreferredSize(new Dimension(130, 38));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() { @Override public void paint(Graphics g, JComponent c) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(c.getBackground()); g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 8, 8); super.paint(g2, c); g2.dispose(); } });
        return btn;
    }

    class RoundedPanel extends JPanel { private int r; private Color bg; public RoundedPanel(int r, Color bg) { this.r = r; this.bg = bg; setOpaque(false); } @Override protected void paintComponent(Graphics g) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(bg); g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r); g2.dispose(); } }
    class RoundBorder implements javax.swing.border.Border { private Color c; private int r; public RoundBorder(Color c, int r) { this.c = c; this.r = r; } @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) { Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON); g2.setColor(this.c); g2.drawRoundRect(x, y, w - 1, h - 1, r, r); g2.dispose(); } @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); } @Override public boolean isBorderOpaque() { return false; } }
}
