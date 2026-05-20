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
    private final Color dangerRed = new Color(239, 68, 68);
    private final Color purple = new Color(124, 58, 237);
    private final Color softBlue = new Color(237, 242, 255);
    private final Color softGreen = new Color(236, 253, 245);
    private final Color softOrange = new Color(255, 247, 237);
    private final Color softRed = new Color(254, 242, 242);

    private JTable tblPromos;
    private DefaultTableModel tableModel;
    private JTextField txtSearch;
    private JComboBox<String> cbFilterStatus;
    private JLabel lblTotalPromos, lblActivePromos, lblEndedPromos, lblPausedPromos;

    private JTextField txtMaKM;
    private JTextField txtTenKM;
    private JSpinner spinGiamGia;
    private JTextField txtTuNgay;
    private JTextField txtDenNgay;
    private JComboBox<String> cbSieuThi;
    private JComboBox<String> cbSanPham;
    private JComboBox<String> cbHangThanhVien;
    private JComboBox<String> cbTrangThai;
    private JButton btnSave, btnClear, btnDeactivate, btnPreview;

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
        add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel contentPanel = new JPanel(new BorderLayout(20, 0));
        contentPanel.setOpaque(false);
        contentPanel.add(createLeftPanel(), BorderLayout.CENTER);
        contentPanel.add(createRightPanel(), BorderLayout.EAST);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel topContainer = new JPanel(new BorderLayout(20, 0));
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

        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 14, 0));
        statsPanel.setOpaque(false);
        lblTotalPromos = new JLabel("0", SwingConstants.CENTER);
        lblActivePromos = new JLabel("0", SwingConstants.CENTER);
        lblEndedPromos = new JLabel("0", SwingConstants.CENTER);
        lblPausedPromos = new JLabel("0", SwingConstants.CENTER);
        statsPanel.add(createStatCard("Tổng Chương Trình", lblTotalPromos, primaryBlue));
        statsPanel.add(createStatCard("Đang Diễn Ra", lblActivePromos, successGreen));
        statsPanel.add(createStatCard("Đã Kết Thúc", lblEndedPromos, warningOrange));
        statsPanel.add(createStatCard("Tạm Ngưng", lblPausedPromos, dangerRed));

        topContainer.add(titlePanel, BorderLayout.CENTER);
        topContainer.add(statsPanel, BorderLayout.EAST);
        return topContainer;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        RoundedPanel card = new RoundedPanel(18, cardWhite);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(new EmptyBorder(12, 16, 12, 16));
        card.setPreferredSize(new Dimension(185, 74));

        JPanel iconBox = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 25));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(44, 44));
        iconBox.setLayout(new GridBagLayout());

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 24));
        dot.setForeground(accentColor);
        iconBox.add(dot);

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(textGray);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(textDark);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textBox.add(lblTitle);
        textBox.add(Box.createRigidArea(new Dimension(0, 4)));
        textBox.add(valueLabel);

        card.add(iconBox, BorderLayout.WEST);
        card.add(textBox, BorderLayout.CENTER);
        return card;
    }

    private JPanel createLeftPanel() {
        RoundedPanel leftCard = new RoundedPanel(20, cardWhite);
        leftCard.setLayout(new BorderLayout(0, 15));
        leftCard.setBorder(new EmptyBorder(20, 20, 14, 20));

        JPanel toolBar = new JPanel(new BorderLayout(16, 0));
        toolBar.setOpaque(false);

        JLabel lblListTitle = new JLabel("Danh sách Khuyến mãi");
        lblListTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblListTitle.setForeground(textDark);

        JPanel searchBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchBox.setOpaque(false);

        cbFilterStatus = new JComboBox<>(new String[]{"Tất cả", "Đang diễn ra", "Sắp diễn ra", "Đã kết thúc", "Tạm ngưng / Kết thúc"});
        cbFilterStatus.setPreferredSize(new Dimension(165, 38));
        cbFilterStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        txtSearch = createTextField("Tra mã KM, Tên CT...");
        txtSearch.setPreferredSize(new Dimension(230, 38));

        JButton btnSearch = createCustomButton("Tìm", primaryBlue, Color.WHITE, IconHelper.search(16));
        btnSearch.setPreferredSize(new Dimension(90, 38));
        btnSearch.addActionListener(e -> doSearch());

        searchBox.add(cbFilterStatus);
        searchBox.add(txtSearch);
        searchBox.add(btnSearch);

        toolBar.add(lblListTitle, BorderLayout.WEST);
        toolBar.add(searchBox, BorderLayout.EAST);

        tableModel = new DefaultTableModel(new Object[]{"Mã KM", "Tên Chương Trình", "Mức giảm", "Từ ngày", "Đến ngày", "Trạng thái"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tblPromos = new JTable(tableModel);
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblPromos);
        scrollPane.setBorder(BorderFactory.createLineBorder(borderGray));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JLabel hint = new JLabel("Gợi ý: Click vào một dòng trong bảng để chỉnh sửa hoặc tạm ngưng khuyến mãi.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(textGray);
        hint.setBorder(new EmptyBorder(2, 2, 0, 2));

        leftCard.add(toolBar, BorderLayout.NORTH);
        leftCard.add(scrollPane, BorderLayout.CENTER);
        leftCard.add(hint, BorderLayout.SOUTH);
        return leftCard;
    }

    private JPanel createRightPanel() {
        RoundedPanel rightCard = new RoundedPanel(20, cardWhite);
        rightCard.setPreferredSize(new Dimension(430, 0));
        rightCard.setLayout(new BorderLayout());
        rightCard.setBorder(new EmptyBorder(24, 24, 22, 24));

        JLabel lblFormTitle = new JLabel("Cấu Hình Khuyến Mãi");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblFormTitle.setForeground(textDark);
        lblFormTitle.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.gridx = 0;

        int y = 0;
        gbc.gridy = y++;
        formPanel.add(createSectionTitle("1. Thông tin cơ bản"), gbc);

        JPanel rowBasic = new JPanel(new GridLayout(1, 2, 10, 0));
        rowBasic.setOpaque(false);

        JPanel pnlMa = new JPanel(new BorderLayout(0, 4));
        pnlMa.setOpaque(false);
        pnlMa.add(createFormLabel("Mã KM:"), BorderLayout.NORTH);
        txtMaKM = createTextField("VD: TET2026");
        pnlMa.add(txtMaKM, BorderLayout.CENTER);

        JPanel pnlGiam = new JPanel(new BorderLayout(0, 4));
        pnlGiam.setOpaque(false);
        pnlGiam.add(createFormLabel("Mức giảm (%):"), BorderLayout.NORTH);
        spinGiamGia = new JSpinner(new SpinnerNumberModel(5, 0, 100, 1));
        spinGiamGia.setFont(new Font("Segoe UI", Font.BOLD, 14));
        spinGiamGia.setPreferredSize(new Dimension(0, 36));
        pnlGiam.add(spinGiamGia, BorderLayout.CENTER);

        rowBasic.add(pnlMa);
        rowBasic.add(pnlGiam);
        gbc.gridy = y++;
        formPanel.add(rowBasic, gbc);

        gbc.gridy = y++;
        formPanel.add(createFormLabel("Tên chương trình:"), gbc);
        txtTenKM = createTextField("Nhập tên sự kiện...");
        gbc.gridy = y++;
        formPanel.add(txtTenKM, gbc);

        gbc.gridy = y++;
        formPanel.add(createSectionTitle("2. Thời gian áp dụng"), gbc);

        JPanel rowTime = new JPanel(new GridLayout(1, 2, 10, 0));
        rowTime.setOpaque(false);

        JPanel pnlTu = new JPanel(new BorderLayout(0, 4));
        pnlTu.setOpaque(false);
        pnlTu.add(createFormLabel("Từ ngày:"), BorderLayout.NORTH);
        txtTuNgay = createTextField("2026-01-01");
        pnlTu.add(txtTuNgay, BorderLayout.CENTER);

        JPanel pnlDen = new JPanel(new BorderLayout(0, 4));
        pnlDen.setOpaque(false);
        pnlDen.add(createFormLabel("Đến ngày:"), BorderLayout.NORTH);
        txtDenNgay = createTextField("2026-12-31");
        pnlDen.add(txtDenNgay, BorderLayout.CENTER);

        rowTime.add(pnlTu);
        rowTime.add(pnlDen);
        gbc.gridy = y++;
        formPanel.add(rowTime, gbc);

        gbc.gridy = y++;
        formPanel.add(createSectionTitle("3. Phạm vi áp dụng"), gbc);

        gbc.gridy = y++;
        formPanel.add(createFormLabel("Áp dụng tại Chi nhánh:"), gbc);
        cbSieuThi = new JComboBox<>(new String[]{"Tất cả siêu thị trên toàn quốc"});
        cbSieuThi.setPreferredSize(new Dimension(0, 35));
        cbSieuThi.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridy = y++;
        formPanel.add(cbSieuThi, gbc);

        gbc.gridy = y++;
        formPanel.add(createFormLabel("Áp dụng cho Sản phẩm:"), gbc);
        cbSanPham = new JComboBox<>(new String[]{"Tất cả mặt hàng", "Chỉ Hàng tiêu dùng", "Chỉ Thực phẩm tươi sống"});
        cbSanPham.setPreferredSize(new Dimension(0, 35));
        cbSanPham.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridy = y++;
        formPanel.add(cbSanPham, gbc);

        gbc.gridy = y++;
        formPanel.add(createFormLabel("Áp dụng cho Hạng Thành Viên:"), gbc);
        cbHangThanhVien = new JComboBox<>(new String[]{"Tất cả hạng (Kể cả khách vãng lai)", "Chỉ thành viên Đồng trở lên", "Chỉ thành viên Bạc trở lên", "Chỉ thành viên Vàng trở lên"});
        cbHangThanhVien.setPreferredSize(new Dimension(0, 35));
        cbHangThanhVien.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridy = y++;
        formPanel.add(cbHangThanhVien, gbc);

        gbc.gridy = y++;
        formPanel.add(createSectionTitle("4. Trạng thái chương trình"), gbc);

        cbTrangThai = new JComboBox<>(new String[]{"Đang diễn ra", "Sắp diễn ra", "Đã kết thúc", "Tạm ngưng / Kết thúc"});
        cbTrangThai.setPreferredSize(new Dimension(0, 35));
        cbTrangThai.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        gbc.gridy = y++;
        formPanel.add(cbTrangThai, gbc);

        JPanel actionPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        actionPanel.setOpaque(false);
        actionPanel.setBorder(new EmptyBorder(12, 0, 0, 0));
        btnClear = createCustomButton("Làm Mới", new Color(235, 238, 244), textDark, null);
        btnPreview = createCustomButton("Xem Trước", softBlue, primaryBlue, null);
        btnDeactivate = createCustomButton("Tạm Ngưng", dangerRed, Color.WHITE, null);
        btnSave = createCustomButton("Lưu Khuyến Mãi", primaryBlue, Color.WHITE, null);
        actionPanel.add(btnClear);
        actionPanel.add(btnPreview);
        actionPanel.add(btnDeactivate);
        actionPanel.add(btnSave);

        rightCard.add(lblFormTitle, BorderLayout.NORTH);
        rightCard.add(formPanel, BorderLayout.CENTER);
        rightCard.add(actionPanel, BorderLayout.SOUTH);
        return rightCard;
    }

    private void initEvents() {
        btnClear.addActionListener(e -> clearForm());
        btnPreview.addActionListener(e -> previewPromo());
        btnDeactivate.addActionListener(e -> deactivatePromo());
        btnSave.addActionListener(e -> savePromo());

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
                    int modelRow = tblPromos.convertRowIndexToModel(row);
                    isEditMode = true;
                    String ma = tableModel.getValueAt(modelRow, 0).toString();
                    txtMaKM.setText(ma);
                    txtMaKM.setEnabled(false);
                    loadPromoDetailsToForm(ma);
                }
            }
        });
    }

    private void doSearch() {
        loadPromoData(txtSearch.getText().trim(), cbFilterStatus.getSelectedItem().toString());
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
        int total = 0, active = 0, ended = 0, paused = 0;

        String sql = "SELECT p.promotion_id AS makm, p.promotion_name AS tenkm, "
                + "NVL(p.discount_amount, 0) AS phantramgiam, "
                + "TO_CHAR(c.start_date, 'YYYY-MM-DD') AS tungay, "
                + "TO_CHAR(c.end_date, 'YYYY-MM-DD') AS denngay, "
                + "NVL(p.status, 'Đang diễn ra') AS trangthai "
                + "FROM PROMOTIONS p "
                + "LEFT JOIN PROMOTION_CAMPAIGNS c ON p.campaign_id = c.campaign_id "
                + "WHERE NVL(p.is_deleted, 0) = 0 "
                + "AND (LOWER(p.promotion_id) LIKE LOWER(?) OR LOWER(p.promotion_name) LIKE LOWER(?)) ";

        if (!"Tất cả".equals(statusFilter)) {
            sql += "AND p.status = ? ";
        }
        sql += "ORDER BY c.start_date DESC NULLS LAST, p.promotion_id DESC";

        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            if (!"Tất cả".equals(statusFilter)) {
                ps.setString(3, statusFilter);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String trangThai = rs.getString("trangthai");
                    tableModel.addRow(new Object[]{
                            rs.getString("makm"),
                            rs.getString("tenkm"),
                            rs.getInt("phantramgiam") + "%",
                            rs.getString("tungay"),
                            rs.getString("denngay"),
                            trangThai
                    });

                    total++;
                    if ("Đang diễn ra".equals(trangThai)) {
                        active++;
                    } else if ("Đã kết thúc".equals(trangThai)) {
                        ended++;
                    } else if ("Tạm ngưng / Kết thúc".equals(trangThai)) {
                        paused++;
                    }
                }
            }

            lblTotalPromos.setText(String.valueOf(total));
            lblActivePromos.setText(String.valueOf(active));
            lblEndedPromos.setText(String.valueOf(ended));
            lblPausedPromos.setText(String.valueOf(paused));

        } catch (Exception e) {
            System.err.println("Lỗi tải danh sách Khuyến mãi: " + e.getMessage());
        }
    }

    private void loadPromoDetailsToForm(String maKM) {
        String sql = "SELECT p.promotion_name, NVL(p.discount_amount, 0) AS discount_amount, "
                + "TO_CHAR(c.start_date, 'YYYY-MM-DD') AS start_date, "
                + "TO_CHAR(c.end_date, 'YYYY-MM-DD') AS end_date, "
                + "NVL(p.status, 'Đang diễn ra') AS status "
                + "FROM PROMOTIONS p "
                + "LEFT JOIN PROMOTION_CAMPAIGNS c ON p.campaign_id = c.campaign_id "
                + "WHERE p.promotion_id = ? AND NVL(p.is_deleted, 0) = 0";

        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, maKM);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    txtTenKM.setText(rs.getString("promotion_name"));
                    spinGiamGia.setValue(rs.getInt("discount_amount"));
                    txtTuNgay.setText(rs.getString("start_date"));
                    txtDenNgay.setText(rs.getString("end_date"));
                    cbTrangThai.setSelectedItem(rs.getString("status"));
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi tải chi tiết Khuyến mãi: " + e.getMessage());
        }
    }

    private void savePromo() {
        String ma = txtMaKM.getText().trim();
        String ten = txtTenKM.getText().trim();
        int giam = (int) spinGiamGia.getValue();
        String tuNgay = txtTuNgay.getText().trim();
        String denNgay = txtDenNgay.getText().trim();
        String trangThai = cbTrangThai.getSelectedItem().toString();

        if (ma.isEmpty() || ten.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập mã và tên Khuyến mãi!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String campaignId = buildCampaignId(ma);

        try (Connection con = common.db.DatabaseConnection.getConnection()) {
            upsertCampaign(con, campaignId, ten, tuNgay, denNgay);

            if (isEditMode) {
                String sql = "UPDATE PROMOTIONS SET promotion_name = ?, campaign_id = ?, discount_amount = ?, status = ? "
                        + "WHERE promotion_id = ? AND NVL(is_deleted, 0) = 0";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, ten);
                    ps.setString(2, campaignId);
                    ps.setInt(3, giam);
                    ps.setString(4, trangThai);
                    ps.setString(5, ma);
                    ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Cập nhật Khuyến mãi thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            } else {
                String sql = "INSERT INTO PROMOTIONS (promotion_id, promotion_name, campaign_id, status, discount_amount, is_deleted) "
                        + "VALUES (?, ?, ?, ?, ?, 0)";
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, ma);
                    ps.setString(2, ten);
                    ps.setString(3, campaignId);
                    ps.setString(4, trangThai);
                    ps.setInt(5, giam);
                    ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Đã tạo Khuyến mãi mới thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            }

            business.service.AuditLogService.logAction(
                    isEditMode ? "CẬP NHẬT" : "THÊM MỚI",
                    "PROMOTIONS",
                    ma,
                    "",
                    "Tên CT: " + ten + " (-" + giam + "%)",
                    "Cập nhật chiến dịch KM"
            );

            clearForm();
            doSearch();
            EventBus.publish(new AppDataChangedEvent(AppEventType.STORE_INFO, "PROMO_UPDATED"));

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi lưu Khuyến mãi: " + e.getMessage(), "Lỗi DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void upsertCampaign(Connection con, String campaignId, String ten, String tuNgay, String denNgay) throws Exception {
        String sql = "MERGE INTO PROMOTION_CAMPAIGNS c "
                + "USING (SELECT ? AS campaign_id FROM dual) src "
                + "ON (c.campaign_id = src.campaign_id) "
                + "WHEN MATCHED THEN UPDATE SET c.campaign_name = ?, c.start_date = TO_DATE(NULLIF(?, ''), 'YYYY-MM-DD'), c.end_date = TO_DATE(NULLIF(?, ''), 'YYYY-MM-DD') "
                + "WHEN NOT MATCHED THEN INSERT (campaign_id, campaign_name, start_date, end_date, is_deleted) "
                + "VALUES (?, ?, TO_DATE(NULLIF(?, ''), 'YYYY-MM-DD'), TO_DATE(NULLIF(?, ''), 'YYYY-MM-DD'), 0)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, campaignId);
            ps.setString(2, ten);
            ps.setString(3, tuNgay);
            ps.setString(4, denNgay);
            ps.setString(5, campaignId);
            ps.setString(6, ten);
            ps.setString(7, tuNgay);
            ps.setString(8, denNgay);
            ps.executeUpdate();
        }
    }

    private String buildCampaignId(String promotionId) {
        String id = "CAMP_" + promotionId;
        return id.length() <= 50 ? id : promotionId.substring(0, Math.min(50, promotionId.length()));
    }

    private void deactivatePromo() {
        String ma = txtMaKM.getText().trim();
        if (ma.isEmpty() || !isEditMode) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một khuyến mãi trong bảng trước khi tạm ngưng.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn tạm ngưng khuyến mãi này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        String sql = "UPDATE PROMOTIONS SET status = ? WHERE promotion_id = ? AND NVL(is_deleted, 0) = 0";
        try (Connection con = common.db.DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "Tạm ngưng / Kết thúc");
            ps.setString(2, ma);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Đã tạm ngưng khuyến mãi!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            clearForm();
            doSearch();
            EventBus.publish(new AppDataChangedEvent(AppEventType.STORE_INFO, "PROMO_UPDATED"));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi tạm ngưng khuyến mãi: " + e.getMessage(), "Lỗi DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void previewPromo() {
        String ma = txtMaKM.getText().trim();
        String ten = txtTenKM.getText().trim();
        int giam = (int) spinGiamGia.getValue();
        String tuNgay = txtTuNgay.getText().trim();
        String denNgay = txtDenNgay.getText().trim();
        String trangThai = cbTrangThai.getSelectedItem().toString();

        String message = "Mã khuyến mãi: " + valueOrDash(ma) + "\n"
                + "Tên chương trình: " + valueOrDash(ten) + "\n"
                + "Mức giảm: " + giam + "%\n"
                + "Thời gian: " + valueOrDash(tuNgay) + " đến " + valueOrDash(denNgay) + "\n"
                + "Trạng thái: " + trangThai;

        JOptionPane.showMessageDialog(this, message, "Xem trước khuyến mãi", JOptionPane.INFORMATION_MESSAGE);
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private JLabel createSectionTitle(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lbl.setForeground(primaryBlue);
        lbl.setBorder(new EmptyBorder(8, 0, 2, 0));
        return lbl;
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
        tblPromos.setRowHeight(44);
        tblPromos.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblPromos.setShowVerticalLines(false);
        tblPromos.setShowHorizontalLines(true);
        tblPromos.setIntercellSpacing(new Dimension(0, 0));
        tblPromos.setGridColor(new Color(245, 247, 251));
        tblPromos.setSelectionBackground(new Color(237, 242, 255));
        tblPromos.setSelectionForeground(textDark);
        tblPromos.setFillsViewportHeight(true);
        tblPromos.setAutoCreateRowSorter(true);
        tblPromos.getTableHeader().setReorderingAllowed(false);
        tblPromos.getTableHeader().setPreferredSize(new Dimension(0, 36));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setBackground(textDark);
        headerRenderer.setForeground(Color.WHITE);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 13));
        headerRenderer.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        DefaultTableCellRenderer bodyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                if (!isSelected) {
                    lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(250, 252, 255));
                    lbl.setForeground(textDark);
                }
                return lbl;
            }
        };

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String status = value == null ? "" : value.toString();
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                if (!isSelected) {
                    if ("Đang diễn ra".equals(status)) {
                        lbl.setForeground(successGreen);
                        lbl.setBackground(softGreen);
                    } else if ("Sắp diễn ra".equals(status)) {
                        lbl.setForeground(primaryBlue);
                        lbl.setBackground(softBlue);
                    } else if ("Đã kết thúc".equals(status)) {
                        lbl.setForeground(warningOrange);
                        lbl.setBackground(softOrange);
                    } else {
                        lbl.setForeground(dangerRed);
                        lbl.setBackground(softRed);
                    }
                }
                return lbl;
            }
        };

        for (int i = 0; i < tblPromos.getColumnCount(); i++) {
            tblPromos.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
            tblPromos.getColumnModel().getColumn(i).setCellRenderer(bodyRenderer);
        }
        tblPromos.getColumnModel().getColumn(0).setPreferredWidth(85);
        tblPromos.getColumnModel().getColumn(1).setPreferredWidth(240);
        tblPromos.getColumnModel().getColumn(2).setPreferredWidth(80);
        tblPromos.getColumnModel().getColumn(3).setPreferredWidth(95);
        tblPromos.getColumnModel().getColumn(4).setPreferredWidth(95);
        tblPromos.getColumnModel().getColumn(5).setPreferredWidth(140);
        tblPromos.getColumnModel().getColumn(5).setCellRenderer(statusRenderer);
    }

    private JButton createCustomButton(String text, Color bg, Color fg, ImageIcon icon) {
        JButton btn = new JButton(text);
        if (icon != null) {
            btn.setIcon(new ImageIcon(icon.getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH)));
        }
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setBackground(bg);
        btn.setPreferredSize(new Dimension(130, 38));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 9, 9);
                super.paint(g2, c);
                g2.dispose();
            }
        });
        return btn;
    }

    class RoundedPanel extends JPanel {
        private final int r;
        private final Color bg;

        public RoundedPanel(int r, Color bg) {
            this.r = r;
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), r, r);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class RoundBorder implements javax.swing.border.Border {
        private final Color c;
        private final int r;

        public RoundBorder(Color c, int r) {
            this.c = c;
            this.r = r;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(this.c);
            g2.drawRoundRect(x, y, w - 1, h - 1, r, r);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }
}
