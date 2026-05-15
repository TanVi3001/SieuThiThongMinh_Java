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
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import view.components.IconHelper;

public class PromotionManagementPanel extends JPanel {

    // --- BẢNG MÀU UI CHUẨN ---
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
    
    // --- KHAI BÁO CÁC Ô THỐNG KÊ ---
    private JLabel lblTotalPromos, lblActivePromos, lblEndedPromos;

    // --- FORM COMPONENTS ---
    private JTextField txtMaKM;
    private JTextField txtTenKM;
    private JSpinner spinGiamGia; // Dùng Spinner cho % giảm giá
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
        // ── 1. HEADER & DASHBOARD THỐNG KÊ ──────────────────────────────────
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
        
        // Thẻ thống kê nhanh
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

        // ── 2. MAIN CONTENT (SPLIT LAYOUT) ───────────────────────────────────
        JPanel contentPanel = new JPanel(new BorderLayout(20, 0));
        contentPanel.setOpaque(false);

        contentPanel.add(createLeftPanel(), BorderLayout.CENTER);
        contentPanel.add(createRightPanel(), BorderLayout.EAST);

        add(contentPanel, BorderLayout.CENTER);
    }
    
    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        RoundedPanel card = new RoundedPanel(15, cardWhite);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 4, 0, 0, accentColor), 
            new EmptyBorder(10, 15, 10, 15)
        ));
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
        
        cbFilterStatus = new JComboBox<>(new String[]{"Tất cả", "Đang diễn ra", "Sắp diễn ra", "Đã kết thúc"});
        cbFilterStatus.setPreferredSize(new Dimension(130, 38));
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

        // 1. Mã & Mức giảm
        gbc.gridy = 0;
        JPanel row1 = new JPanel(new GridLayout(1, 2, 10, 0)); row1.setOpaque(false);
        
        JPanel pnlMa = new JPanel(new BorderLayout()); pnlMa.setOpaque(false);
        pnlMa.add(createFormLabel("Mã KM:"), BorderLayout.NORTH);
        txtMaKM = createTextField("VD: TET2026");
        pnlMa.add(txtMaKM, BorderLayout.CENTER);
        
        JPanel pnlGiam = new JPanel(new BorderLayout()); pnlGiam.setOpaque(false);
        pnlGiam.add(createFormLabel("Mức giảm (%):"), BorderLayout.NORTH);
        spinGiamGia = new JSpinner(new SpinnerNumberModel(5, 1, 100, 1));
        spinGiamGia.setFont(new Font("Segoe UI", Font.BOLD, 14));
        pnlGiam.add(spinGiamGia, BorderLayout.CENTER);
        
        row1.add(pnlMa); row1.add(pnlGiam);
        formPanel.add(row1, gbc);

        // 2. Tên CTKM
        gbc.gridy = 1; formPanel.add(createFormLabel("Tên chương trình:"), gbc);
        gbc.gridy = 2; txtTenKM = createTextField("Nhập tên sự kiện..."); formPanel.add(txtTenKM, gbc);

        // 3. Thời gian
        gbc.gridy = 3;
        JPanel rowTime = new JPanel(new GridLayout(1, 2, 10, 0)); rowTime.setOpaque(false);
        
        JPanel pnlTu = new JPanel(new BorderLayout()); pnlTu.setOpaque(false);
        pnlTu.add(createFormLabel("Từ ngày (YYYY-MM-DD):"), BorderLayout.NORTH);
        txtTuNgay = createTextField("2026-01-01"); pnlTu.add(txtTuNgay, BorderLayout.CENTER);
        
        JPanel pnlDen = new JPanel(new BorderLayout()); pnlDen.setOpaque(false);
        pnlDen.add(createFormLabel("Đến ngày (YYYY-MM-DD):"), BorderLayout.NORTH);
        txtDenNgay = createTextField("2026-12-31"); pnlDen.add(txtDenNgay, BorderLayout.CENTER);
        
        rowTime.add(pnlTu); rowTime.add(pnlDen);
        formPanel.add(rowTime, gbc);

        // 4. Phạm vi áp dụng
        gbc.gridy = 4; formPanel.add(createFormLabel("Áp dụng tại Chi nhánh:"), gbc);
        gbc.gridy = 5; 
        cbSieuThi = new JComboBox<>(new String[]{"Tất cả siêu thị trên toàn quốc", "Chỉ chọn 1 siêu thị (ST001)", "Chỉ chọn 1 siêu thị (ST002)"});
        cbSieuThi.setPreferredSize(new Dimension(0, 35)); formPanel.add(cbSieuThi, gbc);

        gbc.gridy = 6; formPanel.add(createFormLabel("Áp dụng cho Sản phẩm:"), gbc);
        gbc.gridy = 7; 
        cbSanPham = new JComboBox<>(new String[]{"Tất cả mặt hàng", "Chỉ Hàng tiêu dùng", "Chỉ Thực phẩm tươi sống"});
        cbSanPham.setPreferredSize(new Dimension(0, 35)); formPanel.add(cbSanPham, gbc);

        // 5. Đối tượng Hạng thành viên (Cộng dồn)
        gbc.gridy = 8; 
        JLabel lblHangTV = createFormLabel("Áp dụng cho Hạng Thành Viên:");
        formPanel.add(lblHangTV, gbc);
        
        gbc.gridy = 9; gbc.insets = new Insets(0, 0, 5, 0);
        cbHangThanhVien = new JComboBox<>(new String[]{"Tất cả hạng (Kể cả khách vãng lai)", "Chỉ thành viên Đồng trở lên", "Chỉ thành viên Bạc trở lên", "Chỉ thành viên Vàng trở lên"});
        cbHangThanhVien.setPreferredSize(new Dimension(0, 35)); formPanel.add(cbHangThanhVien, gbc);
        
        // Dòng chú thích về quy tắc cộng dồn giảm giá
        gbc.gridy = 10; gbc.insets = new Insets(0, 0, 15, 0);
        JLabel lblNote = new JLabel("<html><i style='color:#E63946; font-size:10px;'>*Hệ thống sẽ cộng dồn % giảm giá này vào % chiết khấu mặc định của Hạng thẻ (nếu có).</i></html>");
        formPanel.add(lblNote, gbc);

        // 6. Trạng thái
        gbc.gridy = 11; formPanel.add(createFormLabel("Trạng thái hiện tại:"), gbc);
        gbc.gridy = 12; 
        cbTrangThai = new JComboBox<>(new String[]{"Đang diễn ra", "Tạm ngưng / Kết thúc"});
        cbTrangThai.setPreferredSize(new Dimension(0, 35)); formPanel.add(cbTrangThai, gbc);

        // Nút bấm
        JPanel actionPanel = new JPanel(new GridLayout(1, 2, 10, 0));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        btnClear = createCustomButton("Làm Mới", new Color(235, 238, 244), textDark, null);
        btnSave = createCustomButton("Lưu Khuyến Mãi", primaryBlue, Color.WHITE, null);
        
        actionPanel.add(btnClear);
        actionPanel.add(btnSave);

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
            @Override
            public void mouseClicked(MouseEvent e) {
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
    
    private void doSearch() {
        String keyword = txtSearch.getText().trim();
        String status = cbFilterStatus.getSelectedItem().toString();
        loadPromoData(keyword, status);
    }

    private void clearForm() {
        isEditMode = false;
        txtMaKM.setText("");
        txtMaKM.setEnabled(true);
        txtTenKM.setText("");
        spinGiamGia.setValue(5);
        txtTuNgay.setText("");
        txtDenNgay.setText("");
        cbSieuThi.setSelectedIndex(0);
        cbSanPham.setSelectedIndex(0);
        cbHangThanhVien.setSelectedIndex(0);
        cbTrangThai.setSelectedIndex(0);
        tblPromos.clearSelection();
    }

    private void loadPromoData(String keyword, String statusFilter) {
        tableModel.setRowCount(0);
        int total = 0, active = 0, ended = 0;
        
        String sql = "SELECT MAKM, TENKM, PHANTRAMGIAM, TO_CHAR(NGAYBATDAU, 'YYYY-MM-DD') as TUNGAY, " +
                     "TO_CHAR(NGAYKETTHUC, 'YYYY-MM-DD') as DENNGAY, TRANGTHAI " +
                     "FROM KHUYENMAI " +
                     "WHERE (LOWER(MAKM) LIKE LOWER(?) OR LOWER(TENKM) LIKE LOWER(?)) ";
                     
        if (!statusFilter.equals("Tất cả")) {
            sql += " AND TRANGTHAI = '" + statusFilter + "' ";
        }
        sql += " ORDER BY NGAYBATDAU DESC";

        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String ma = rs.getString("MAKM");
                    String ten = rs.getString("TENKM");
                    int giam = rs.getInt("PHANTRAMGIAM");
                    String tuNgay = rs.getString("TUNGAY");
                    String denNgay = rs.getString("DENNGAY");
                    String trangThai = rs.getString("TRANGTHAI");
                    
                    tableModel.addRow(new Object[]{ ma, ten, giam + "%", tuNgay, denNgay, trangThai });
                    
                    total++;
                    if ("Đang diễn ra".equals(trangThai)) active++;
                    if ("Đã kết thúc".equals(trangThai) || "Tạm ngưng / Kết thúc".equals(trangThai)) ended++;
                }
            }
            
            lblTotalPromos.setText(String.valueOf(total));
            lblActivePromos.setText(String.valueOf(active));
            lblEndedPromos.setText(String.valueOf(ended));
            
        } catch (Exception e) {
            System.err.println("Lỗi tải danh sách Khuyến mãi: " + e.getMessage());
            // Đã xóa phần Fake Data. Nếu có lỗi, bảng sẽ để trống.
        }
    }

    private void loadPromoDetailsToForm(String maKM) {
        String sql = "SELECT TENKM, PHANTRAMGIAM, TO_CHAR(NGAYBATDAU, 'YYYY-MM-DD') as TUNGAY, TO_CHAR(NGAYKETTHUC, 'YYYY-MM-DD') as DENNGAY, TRANGTHAI FROM KHUYENMAI WHERE MAKM = ?";
        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maKM);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtTenKM.setText(rs.getString("TENKM"));
                    spinGiamGia.setValue(rs.getInt("PHANTRAMGIAM"));
                    txtTuNgay.setText(rs.getString("TUNGAY"));
                    txtDenNgay.setText(rs.getString("DENNGAY"));
                    cbTrangThai.setSelectedItem(rs.getString("TRANGTHAI"));
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void savePromo() {
        String ma = txtMaKM.getText().trim();
        String ten = txtTenKM.getText().trim();
        int giam = (int) spinGiamGia.getValue();
        String tuNgay = txtTuNgay.getText().trim();
        String denNgay = txtDenNgay.getText().trim();
        String trangThai = cbTrangThai.getSelectedItem().toString();

        if (ma.isEmpty() || ten.isEmpty() || tuNgay.isEmpty() || denNgay.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ thông tin Khuyến mãi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection con = common.db.DatabaseConnection.getConnection()) {
            if (isEditMode) {
                String sql = "UPDATE KHUYENMAI SET TENKM=?, PHANTRAMGIAM=?, NGAYBATDAU=TO_DATE(?, 'YYYY-MM-DD'), NGAYKETTHUC=TO_DATE(?, 'YYYY-MM-DD'), TRANGTHAI=? WHERE MAKM=?";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, ten); ps.setInt(2, giam); ps.setString(3, tuNgay); ps.setString(4, denNgay); ps.setString(5, trangThai); ps.setString(6, ma);
                    ps.executeUpdate();
                    
                    business.service.AuditLogService.logAction(
                        "CẬP NHẬT", "KHUYENMAI", ma, "Dữ liệu cũ", "Giảm: " + giam + "%, TT: " + trangThai, "Cập nhật chiến dịch KM"
                    );
                    JOptionPane.showMessageDialog(this, "Cập nhật Khuyến mãi thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            } else {
                String sql = "INSERT INTO KHUYENMAI (MAKM, TENKM, PHANTRAMGIAM, NGAYBATDAU, NGAYKETTHUC, TRANGTHAI) VALUES (?, ?, ?, TO_DATE(?, 'YYYY-MM-DD'), TO_DATE(?, 'YYYY-MM-DD'), ?)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, ma); ps.setString(2, ten); ps.setInt(3, giam); ps.setString(4, tuNgay); ps.setString(5, denNgay); ps.setString(6, trangThai);
                    ps.executeUpdate();
                    
                    business.service.AuditLogService.logAction(
                        "THÊM MỚI", "KHUYENMAI", ma, "", "Tên CT: " + ten + " (-" + giam + "%)", "Tạo chiến dịch KM mới"
                    );
                    JOptionPane.showMessageDialog(this, "Đã tạo Khuyến mãi mới thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                }
            }
            
            clearForm();
            doSearch();
            EventBus.publish(new AppDataChangedEvent(AppEventType.STORE_INFO, "PROMO_UPDATED"));
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi lưu Khuyến mãi: " + e.getMessage(), "Lỗi DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel createFormLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(textGray);
        return lbl;
    }

    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setPreferredSize(new Dimension(0, 35));
        txt.setBorder(BorderFactory.createCompoundBorder(new RoundBorder(borderGray, 8), new EmptyBorder(5, 10, 5, 10)));
        return txt;
    }

    private void setupTableStyle() {
        tblPromos.setRowHeight(38);
        tblPromos.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblPromos.setShowVerticalLines(false);
        tblPromos.setSelectionBackground(new Color(237, 242, 255));
        tblPromos.setSelectionForeground(textDark);
        tblPromos.getTableHeader().setReorderingAllowed(false);

        tblPromos.getColumnModel().getColumn(0).setPreferredWidth(80);
        tblPromos.getColumnModel().getColumn(1).setPreferredWidth(180);
        tblPromos.getColumnModel().getColumn(2).setPreferredWidth(70);
        tblPromos.getColumnModel().getColumn(3).setPreferredWidth(90);
        tblPromos.getColumnModel().getColumn(4).setPreferredWidth(90);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(textDark);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 5));

        for (int i = 0; i < tblPromos.getColumnCount(); i++) {
            tblPromos.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
    }

    private JButton createCustomButton(String t, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(t);
        if (icon != null) btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(16, 16, 1)));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg); btn.setBackground(bg);
        btn.setPreferredSize(new Dimension(130, 38));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR)); btn.setFocusPainted(false); btn.setBorderPainted(false); btn.setContentAreaFilled(false);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg); g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r); g2.dispose();
        }
    }

    class RoundBorder implements javax.swing.border.Border {
        private Color c; private int r;
        public RoundBorder(Color c, int r) { this.c = c; this.r = r; }
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.c); g2.drawRoundRect(x, y, w - 1, h - 1, r, r); g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }
        @Override public boolean isBorderOpaque() { return false; }
    }
}