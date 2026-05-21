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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import view.components.IconHelper;

public class PromotionManagementPanel extends JPanel {

    private static final String STATUS_ALL = "Tất cả";
    private static final String STATUS_ACTIVE = "Đang diễn ra";
    private static final String STATUS_UPCOMING = "Sắp diễn ra";
    private static final String STATUS_ENDED = "Đã kết thúc";
    private static final String STATUS_PAUSED = "Tạm ngưng / Kết thúc";

    private final Color bg = new Color(244, 246, 250);
    private final Color white = Color.WHITE;
    private final Color text = new Color(36, 47, 74);
    private final Color muted = new Color(143, 154, 179);
    private final Color border = new Color(226, 232, 240);
    private final Color blue = new Color(37, 99, 235);
    private final Color green = new Color(16, 185, 129);
    private final Color red = new Color(239, 68, 68);
    private final Color orange = new Color(245, 158, 11);
    private final Color grayBtn = new Color(148, 163, 184);
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
    private JComboBox<String> cbChiNhanh;
    private JComboBox<String> cbSanPham;
    private JComboBox<String> cbHangThanhVien;
    private JComboBox<String> cbTrangThai;
    private JButton btnSave, btnClear, btnDeactivate, btnPreview;
    private JLabel lblFormTitle;
    private JLabel lblFormHint;

    private boolean isEditMode = false;

    public PromotionManagementPanel() {
        setLayout(new BorderLayout(0, 22));
        setBackground(bg);
        setBorder(new EmptyBorder(22, 30, 22, 30));
        initUI();
        initEvents();
        loadPromoData("", STATUS_ALL);
    }

    private void initUI() {
        add(createHeaderPanel(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(22, 0));
        body.setOpaque(false);
        body.add(createMainPanel(), BorderLayout.CENTER);
        body.add(createRightPanel(), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Quản Lý Chiến Dịch Khuyến Mãi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(text);

        JLabel sub = new JLabel("Thiết lập mã giảm giá, cấu hình phạm vi áp dụng và đối tượng khách hàng");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        sub.setForeground(muted);

        panel.add(title);
        panel.add(Box.createVerticalStrut(5));
        panel.add(sub);
        return panel;
    }

    private JPanel createMainPanel() {
        RoundedPanel card = new RoundedPanel(20, white);
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(new EmptyBorder(22, 22, 18, 22));
        card.add(createStatsPanel(), BorderLayout.NORTH);
        card.add(createTableArea(), BorderLayout.CENTER);
        return card;
    }

    private JPanel createStatsPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 14, 0));
        panel.setOpaque(false);

        lblTotalPromos = new JLabel("0");
        lblActivePromos = new JLabel("0");
        lblEndedPromos = new JLabel("0");
        lblPausedPromos = new JLabel("0");

        panel.add(createStatCard("Tổng chương trình", lblTotalPromos, blue));
        panel.add(createStatCard("Đang diễn ra", lblActivePromos, green));
        panel.add(createStatCard("Đã kết thúc", lblEndedPromos, orange));
        panel.add(createStatCard("Tạm ngưng", lblPausedPromos, red));
        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        RoundedPanel card = new RoundedPanel(16, white);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border),
                new EmptyBorder(14, 16, 14, 16)
        ));

        JPanel iconBox = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 24));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        iconBox.setOpaque(false);
        iconBox.setPreferredSize(new Dimension(46, 46));

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.BOLD, 22));
        dot.setForeground(accentColor);
        iconBox.add(dot);

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLabel.setForeground(muted);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        valueLabel.setForeground(text);

        textBox.add(titleLabel);
        textBox.add(Box.createVerticalStrut(5));
        textBox.add(valueLabel);

        card.add(iconBox, BorderLayout.WEST);
        card.add(textBox, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTableArea() {
        JPanel area = new JPanel(new BorderLayout(0, 14));
        area.setOpaque(false);

        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setOpaque(false);

        JLabel title = new JLabel("Danh sách khuyến mãi");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(text);

        JPanel search = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        search.setOpaque(false);

        cbFilterStatus = new JComboBox<>(new String[]{STATUS_ALL, STATUS_ACTIVE, STATUS_UPCOMING, STATUS_ENDED, STATUS_PAUSED});
        cbFilterStatus.setPreferredSize(new Dimension(160, 40));
        cbFilterStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        txtSearch = createTextField("Tra mã KM, tên CT...");
        txtSearch.setPreferredSize(new Dimension(245, 40));

        JButton btnSearch = createButton("Tìm", blue, Color.WHITE, IconHelper.search(16));
        btnSearch.setPreferredSize(new Dimension(92, 40));
        btnSearch.addActionListener(e -> doSearch());

        search.add(cbFilterStatus);
        search.add(txtSearch);
        search.add(btnSearch);
        bar.add(title, BorderLayout.WEST);
        bar.add(search, BorderLayout.EAST);

        tableModel = new DefaultTableModel(new Object[]{"Mã KM", "Tên Chương Trình", "Mức Giảm", "Từ Ngày", "Đến Ngày", "Trạng Thái"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tblPromos = new JTable(tableModel);
        setupTableStyle();

        JScrollPane scrollPane = new JScrollPane(tblPromos);
        scrollPane.setBorder(BorderFactory.createLineBorder(border));
        scrollPane.getViewport().setBackground(Color.WHITE);

        JLabel hint = new JLabel("Gợi ý: Bấm Thêm để tạo khuyến mãi mới, hoặc click một dòng để chỉnh sửa / tạm ngưng.");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        hint.setForeground(muted);

        area.add(bar, BorderLayout.NORTH);
        area.add(scrollPane, BorderLayout.CENTER);
        area.add(hint, BorderLayout.SOUTH);
        return area;
    }

    private JPanel createRightPanel() {
        RoundedPanel card = new RoundedPanel(20, white);
        card.setPreferredSize(new Dimension(430, 0));
        card.setLayout(new BorderLayout(0, 18));
        card.setBorder(new EmptyBorder(24, 24, 22, 24));

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        lblFormTitle = new JLabel("Cấu Hình Khuyến Mãi");
        lblFormTitle.setFont(new Font("Segoe UI", Font.BOLD, 21));
        lblFormTitle.setForeground(text);

        lblFormHint = new JLabel("Bấm Thêm để nhập chương trình mới");
        lblFormHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblFormHint.setForeground(muted);

        header.add(lblFormTitle);
        header.add(Box.createVerticalStrut(6));
        header.add(lblFormHint);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;

        int y = 0;
        JPanel basicRow = new JPanel(new GridLayout(1, 2, 10, 0));
        basicRow.setOpaque(false);
        JPanel maPanel = fieldPanel("Mã KM", txtMaKM = createTextField("VD: TET2026"));
        JPanel discountPanel = new JPanel(new BorderLayout(0, 7));
        discountPanel.setOpaque(false);
        discountPanel.add(createFormLabel("Mức giảm (%)"), BorderLayout.NORTH);
        spinGiamGia = new JSpinner(new SpinnerNumberModel(5, 0, 100, 1));
        spinGiamGia.setFont(new Font("Segoe UI", Font.BOLD, 14));
        spinGiamGia.setPreferredSize(new Dimension(0, 40));
        discountPanel.add(spinGiamGia, BorderLayout.CENTER);
        basicRow.add(maPanel);
        basicRow.add(discountPanel);

        addSection(form, g, y++, "1. Thông tin cơ bản");
        addComponent(form, g, y++, basicRow, 14);
        addField(form, g, y, "Tên chương trình", txtTenKM = createTextField("Nhập tên sự kiện..."));
        y += 2;

        addSection(form, g, y++, "2. Thời gian áp dụng");
        JPanel timeRow = new JPanel(new GridLayout(1, 2, 10, 0));
        timeRow.setOpaque(false);
        timeRow.add(fieldPanel("Từ ngày", txtTuNgay = createTextField("2026-01-01")));
        timeRow.add(fieldPanel("Đến ngày", txtDenNgay = createTextField("2026-12-31")));
        addComponent(form, g, y++, timeRow, 14);

        addSection(form, g, y++, "3. Phạm vi áp dụng");
        cbChiNhanh = combo(new String[]{"Tất cả chi nhánh", "Chi nhánh đang hoạt động"});
        addField(form, g, y, "Áp dụng tại chi nhánh", cbChiNhanh);
        y += 2;
        cbSanPham = combo(new String[]{"Tất cả mặt hàng", "Chỉ Hàng tiêu dùng", "Chỉ Thực phẩm tươi sống"});
        addField(form, g, y, "Áp dụng cho sản phẩm", cbSanPham);
        y += 2;
        cbHangThanhVien = combo(new String[]{"Tất cả hạng", "Đồng trở lên", "Bạc trở lên", "Vàng trở lên"});
        addField(form, g, y, "Áp dụng cho hạng thành viên", cbHangThanhVien);
        y += 2;

        addSection(form, g, y++, "4. Trạng thái chương trình");
        cbTrangThai = combo(new String[]{STATUS_ACTIVE, STATUS_UPCOMING, STATUS_ENDED, STATUS_PAUSED});
        addComponent(form, g, y++, cbTrangThai, 12);

        JPanel actions = new JPanel(new GridLayout(2, 2, 10, 10));
        actions.setOpaque(false);
        btnClear = createButton("Làm mới", new Color(235, 238, 244), text, IconHelper.refresh(18));
        btnPreview = createButton("Xem trước", softBlue, blue, null);
        btnDeactivate = createButton("Tạm ngưng", red, Color.WHITE, IconHelper.delete(18));
        btnSave = createButton("Lưu", blue, Color.WHITE, IconHelper.edit(18));
        actions.add(btnClear);
        actions.add(btnPreview);
        actions.add(btnDeactivate);
        actions.add(btnSave);

        card.add(header, BorderLayout.NORTH);
        card.add(form, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private JPanel fieldPanel(String label, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout(0, 7));
        panel.setOpaque(false);
        panel.add(createFormLabel(label), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private JComboBox<String> combo(String[] values) {
        JComboBox<String> cb = new JComboBox<>(values);
        cb.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cb.setPreferredSize(new Dimension(0, 40));
        return cb;
    }

    private void addSection(JPanel form, GridBagConstraints g, int y, String textValue) {
        g.gridy = y;
        g.insets = new Insets(2, 0, 8, 0);
        form.add(createSectionTitle(textValue), g);
    }

    private void addField(JPanel form, GridBagConstraints g, int y, String label, JComponent field) {
        g.gridy = y;
        g.insets = new Insets(0, 0, 7, 0);
        form.add(createFormLabel(label), g);
        g.gridy = y + 1;
        g.insets = new Insets(0, 0, 14, 0);
        form.add(field, g);
    }

    private void addComponent(JPanel form, GridBagConstraints g, int y, JComponent component, int bottom) {
        g.gridy = y;
        g.insets = new Insets(0, 0, bottom, 0);
        form.add(component, g);
    }

    private void initEvents() {
        btnClear.addActionListener(e -> clearForm());
        btnPreview.addActionListener(e -> previewPromo());
        btnDeactivate.addActionListener(e -> deactivatePromo());
        btnSave.addActionListener(e -> savePromo());

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                doSearch();
            }

            public void removeUpdate(DocumentEvent e) {
                doSearch();
            }

            public void changedUpdate(DocumentEvent e) {
                doSearch();
            }
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
                    lblFormHint.setText("Đang cập nhật khuyến mãi " + ma);
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
        cbChiNhanh.setSelectedIndex(0);
        cbSanPham.setSelectedIndex(0);
        cbHangThanhVien.setSelectedIndex(0);
        cbTrangThai.setSelectedIndex(0);
        tblPromos.clearSelection();
        lblFormTitle.setText("Cấu Hình Khuyến Mãi");
        lblFormHint.setText("Bấm Thêm để nhập chương trình mới");
    }

    private void loadPromoData(String keyword, String statusFilter) {
        tableModel.setRowCount(0);
        int total = 0, active = 0, ended = 0, paused = 0;

        String sql = "SELECT p.promotion_id AS makm, p.promotion_name AS tenkm, "
                + "NVL(p.discount_amount, 0) AS phantramgiam, "
                + "TO_CHAR(c.start_date, 'YYYY-MM-DD') AS tungay, "
                + "TO_CHAR(c.end_date, 'YYYY-MM-DD') AS denngay, "
                + "NVL(p.status, '" + STATUS_ACTIVE + "') AS trangthai "
                + "FROM PROMOTIONS p "
                + "LEFT JOIN PROMOTION_CAMPAIGNS c ON p.campaign_id = c.campaign_id "
                + "WHERE NVL(p.is_deleted, 0) = 0 "
                + "AND (LOWER(p.promotion_id) LIKE LOWER(?) OR LOWER(p.promotion_name) LIKE LOWER(?)) ";

        if (!STATUS_ALL.equals(statusFilter)) {
            sql += "AND p.status = ? ";
        }
        sql += "ORDER BY c.start_date DESC NULLS LAST, p.promotion_id DESC";

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            if (!STATUS_ALL.equals(statusFilter)) {
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
                    if (STATUS_ACTIVE.equals(trangThai)) {
                        active++;
                    } else if (STATUS_ENDED.equals(trangThai)) {
                        ended++;
                    } else if (STATUS_PAUSED.equals(trangThai)) {
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
                + "NVL(p.status, '" + STATUS_ACTIVE + "') AS status "
                + "FROM PROMOTIONS p "
                + "LEFT JOIN PROMOTION_CAMPAIGNS c ON p.campaign_id = c.campaign_id "
                + "WHERE p.promotion_id = ? AND NVL(p.is_deleted, 0) = 0";

        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
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

            business.service.AuditLogService.logAction(isEditMode ? "CẬP NHẬT" : "THÊM MỚI", "PROMOTIONS", ma, "", "Tên CT: " + ten + " (-" + giam + "%)", "Cập nhật chiến dịch KM");
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
        try (Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, STATUS_PAUSED);
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
        String message = "Mã khuyến mãi: " + valueOrDash(txtMaKM.getText()) + "\n"
                + "Tên chương trình: " + valueOrDash(txtTenKM.getText()) + "\n"
                + "Mức giảm: " + spinGiamGia.getValue() + "%\n"
                + "Thời gian: " + valueOrDash(txtTuNgay.getText()) + " đến " + valueOrDash(txtDenNgay.getText()) + "\n"
                + "Trạng thái: " + cbTrangThai.getSelectedItem();
        JOptionPane.showMessageDialog(this, message, "Xem trước khuyến mãi", JOptionPane.INFORMATION_MESSAGE);
    }

    private String valueOrDash(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value.trim();
    }

    private JLabel createSectionTitle(String value) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(blue);
        return label;
    }

    private JLabel createFormLabel(String value) {
        JLabel label = new JLabel(value);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(muted);
        return label;
    }

    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setPreferredSize(new Dimension(0, 40));
        txt.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(border), new EmptyBorder(6, 12, 6, 12)));
        return txt;
    }

    private void setupTableStyle() {
        tblPromos.setRowHeight(44);
        tblPromos.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblPromos.setShowVerticalLines(false);
        tblPromos.setShowHorizontalLines(false);
        tblPromos.setIntercellSpacing(new Dimension(0, 0));
        tblPromos.setGridColor(new Color(245, 247, 251));
        tblPromos.setSelectionBackground(new Color(219, 234, 254));
        tblPromos.setSelectionForeground(text);
        tblPromos.setFillsViewportHeight(true);
        tblPromos.setAutoCreateRowSorter(true);

        JTableHeader header = tblPromos.getTableHeader();
        header.setPreferredSize(new Dimension(0, 42));
        header.setReorderingAllowed(false);
        header.setBackground(new Color(243, 246, 250));
        header.setForeground(Color.BLACK);
        header.setFont(new Font("Segoe UI", Font.BOLD, 13));

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                label.setOpaque(true);
                label.setBackground(new Color(243, 246, 250));
                label.setForeground(Color.BLACK);
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return label;
            }
        };

        DefaultTableCellRenderer bodyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                label.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                if (selected) {
                    label.setBackground(new Color(219, 234, 254));
                    label.setForeground(text);
                } else {
                    label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                    if (column == 0) {
                        label.setForeground(blue);
                        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    } else {
                        label.setForeground(text);
                    }
                }
                return label;
            }
        };

        DefaultTableCellRenderer statusRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                String status = value == null ? "" : value.toString();
                label.setOpaque(true);
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));
                if (selected) {
                    label.setBackground(new Color(219, 234, 254));
                    label.setForeground(text);
                } else if (STATUS_ACTIVE.equals(status)) {
                    label.setBackground(softGreen);
                    label.setForeground(green);
                } else if (STATUS_UPCOMING.equals(status)) {
                    label.setBackground(softBlue);
                    label.setForeground(blue);
                } else if (STATUS_ENDED.equals(status)) {
                    label.setBackground(softOrange);
                    label.setForeground(orange);
                } else {
                    label.setBackground(softRed);
                    label.setForeground(red);
                }
                return label;
            }
        };

        for (int i = 0; i < tblPromos.getColumnCount(); i++) {
            tblPromos.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
            tblPromos.getColumnModel().getColumn(i).setCellRenderer(bodyRenderer);
        }
        tblPromos.getColumnModel().getColumn(0).setPreferredWidth(90);
        tblPromos.getColumnModel().getColumn(1).setPreferredWidth(240);
        tblPromos.getColumnModel().getColumn(2).setPreferredWidth(90);
        tblPromos.getColumnModel().getColumn(3).setPreferredWidth(105);
        tblPromos.getColumnModel().getColumn(4).setPreferredWidth(105);
        tblPromos.getColumnModel().getColumn(5).setPreferredWidth(150);
        tblPromos.getColumnModel().getColumn(5).setCellRenderer(statusRenderer);
    }

    private JButton createButton(String value, Color bgColor, Color fgColor, Icon icon) {
        JButton button = new JButton(value);
        button.setIcon(icon);
        button.setIconTextGap(8);
        button.setHorizontalAlignment(SwingConstants.CENTER);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(fgColor);
        button.setBackground(bgColor);
        button.setPreferredSize(new Dimension(130, 40));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10);
                super.paint(g2, c);
                g2.dispose();
            }
        });
        return button;
    }

    class RoundedPanel extends JPanel {

        private final int radius;
        private final Color backgroundColor;

        RoundedPanel(int radius, Color backgroundColor) {
            this.radius = radius;
            this.backgroundColor = backgroundColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(backgroundColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
