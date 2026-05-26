package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RoleManagementPanel extends javax.swing.JPanel {

    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color primaryBlue = new Color(67, 97, 238);
    private final Color borderGray = new Color(230, 235, 241);

    private List<String> actionList = Arrays.asList("Xem", "Thêm", "Sửa", "Xóa", "Xuất file");
    private String searchRoleQuery = "";

    private List<RoleMatrixItem> roleDataList = new ArrayList<>();

    private JPanel matrixContainer;

    public RoleManagementPanel() {
        initComponents();
        setupModernLayout();
    }

    private void initComponents() {
        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGap(0, 300, Short.MAX_VALUE)
        );
    }

    private void setupModernLayout() {
        this.removeAll();
        this.setLayout(new BorderLayout(0, 20));
        this.setBackground(bgLight);
        this.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(bgLight);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        titlePanel.setBackground(bgLight);
        JLabel title = new JLabel("Ma Trận Phân Quyền");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(textDark);
        JLabel subtitle = new JLabel("Quản lý tập trung các thao tác: Xem, Thêm, Sửa, Xóa, Xuất file của từng vai trò");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(textGray);
        titlePanel.add(title);
        titlePanel.add(subtitle);

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightHeader.setBackground(bgLight);

        JTextField txtSearchRole = new JTextField(15);
        txtSearchRole.putClientProperty("JTextField.placeholderText", "Tìm vai trò...");
        txtSearchRole.setPreferredSize(new Dimension(200, 40));
        txtSearchRole.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 10), new EmptyBorder(5, 15, 5, 15)
        ));

        txtSearchRole.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                updateSearch();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                updateSearch();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                updateSearch();
            }

            private void updateSearch() {
                searchRoleQuery = txtSearchRole.getText().toLowerCase().trim();
                refreshMatrixUI();
            }
        });

        JButton btnReload = createCustomButton("Làm mới dữ liệu", new Color(163, 174, 208), Color.WHITE);
        btnReload.addActionListener(e -> loadDataFromDB());

        rightHeader.add(txtSearchRole);
        rightHeader.add(btnReload);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);
        this.add(header, BorderLayout.NORTH);

        matrixContainer = new JPanel(new BorderLayout());
        matrixContainer.setBackground(bgLight);

        loadDataFromDB();
        this.add(matrixContainer, BorderLayout.CENTER);
    }

    private void loadDataFromDB() {
        roleDataList.clear();
        String sql = """
            SELECT role_id, role_name, can_view, can_add, can_edit, can_delete, can_export
            FROM ROLES
            WHERE is_deleted = 0
              AND role_id IN ('R_ADMIN_ALL', 'R_STORE_MNG', 'R_STAFF_SALE', 'R_STAFF_VIEW_PROD')
            ORDER BY CASE role_id
                WHEN 'R_ADMIN_ALL' THEN 1
                WHEN 'R_STORE_MNG' THEN 2
                WHEN 'R_STAFF_SALE' THEN 3
                WHEN 'R_STAFF_VIEW_PROD' THEN 4
                ELSE 99
            END
        """;

        try (
                Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                RoleMatrixItem item = new RoleMatrixItem(
                        rs.getString("role_id"),
                        rs.getString("role_name"),
                        rs.getInt("can_view") == 1,
                        rs.getInt("can_add") == 1,
                        rs.getInt("can_edit") == 1,
                        rs.getInt("can_delete") == 1,
                        rs.getInt("can_export") == 1
                );
                roleDataList.add(item);
            }

            refreshMatrixUI();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải phân quyền: " + e.getMessage(), "Lỗi DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveMatrixToDB() {
        String sql = """
            UPDATE ROLES
            SET can_view = ?,
                can_add = ?,
                can_edit = ?,
                can_delete = ?,
                can_export = ?
            WHERE role_id = ?
        """;

        try (
                Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            boolean hasChanges = false;

            for (RoleMatrixItem item : roleDataList) {

                /*
                 * ADMIN:
                 * - Không cho tắt Xem/Thêm/Sửa/Xuất file vì Admin là vai trò lõi hệ thống.
                 * - CHO phép bật/tắt Xóa để demo nghiệp vụ chặn thao tác xóa.
                 * - Vì vậy không được continue bỏ qua R_ADMIN_ALL như bản cũ.
                 */
                if ("R_ADMIN_ALL".equals(item.roleId)) {
                    item.canView = true;
                    item.canAdd = true;
                    item.canEdit = true;
                    item.canExport = true;
                }

                if (item.isChanged()) {
                    ps.setInt(1, item.canView ? 1 : 0);
                    ps.setInt(2, item.canAdd ? 1 : 0);
                    ps.setInt(3, item.canEdit ? 1 : 0);
                    ps.setInt(4, item.canDelete ? 1 : 0);
                    ps.setInt(5, item.canExport ? 1 : 0);
                    ps.setString(6, item.roleId);
                    ps.addBatch();
                    hasChanges = true;

                    business.service.AuditLogService.logAction(
                            "CẬP NHẬT",
                            "ROLES",
                            item.roleId,
                            item.getOldPermissions(),
                            item.getActionDiff(),
                            "Admin tinh chỉnh quyền hạn trên Ma trận"
                    );

                    item.updateOriginals();
                }
            }

            if (hasChanges) {
                ps.executeBatch();

                try {
                    common.realtime.RealtimeNotifier.roleChanged("ROLE_MATRIX_UPDATED");
                } catch (Exception ignored) {
                    try {
                        common.realtime.RealtimeClient.send("ACCOUNT_SECURITY_CHANGED");
                        EventBus.publish(new AppDataChangedEvent(AppEventType.ACCOUNT_SECURITY, "MATRIX_UPDATED"));
                    } catch (Exception ignored2) {
                    }
                }

                JOptionPane.showMessageDialog(
                        this,
                        "Đã lưu cấu hình Phân quyền thành công!\nHệ thống đã đồng bộ bảo mật.",
                        "Thành công",
                        JOptionPane.INFORMATION_MESSAGE
                );

                loadDataFromDB();

            } else {
                JOptionPane.showMessageDialog(this, "Không có sự thay đổi nào để lưu.", "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi cập nhật phân quyền: " + e.getMessage(), "Lỗi DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshMatrixUI() {
        matrixContainer.removeAll();
        matrixContainer.add(createMatrixPanel(), BorderLayout.CENTER);
        matrixContainer.revalidate();
        matrixContainer.repaint();
    }

    private JPanel createMatrixPanel() {
        RoundedPanel container = new RoundedPanel(20, cardWhite);
        container.setLayout(new BorderLayout(0, 20));
        container.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel tablePanel = new JPanel(new GridBagLayout());
        tablePanel.setBackground(cardWhite);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;

        gbc.gridy = 0;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.gridx = 0;
        gbc.weightx = 0.3;

        JLabel lblHeaderRole = new JLabel("Vai trò / Chức vụ");
        lblHeaderRole.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblHeaderRole.setForeground(textGray);
        lblHeaderRole.setBorder(new EmptyBorder(0, 10, 0, 0));
        tablePanel.add(lblHeaderRole, gbc);

        double actionWeight = 0.7 / actionList.size();
        for (int i = 0; i < actionList.size(); i++) {
            gbc.gridx = i + 1;
            gbc.weightx = actionWeight;
            JLabel lblAction = new JLabel(actionList.get(i), SwingConstants.CENTER);
            lblAction.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblAction.setForeground(textDark);
            tablePanel.add(lblAction, gbc);
        }

        int rowIndex = 1;
        for (RoleMatrixItem item : roleDataList) {

            String displayRoleName = item.roleName;
            if ("R_ADMIN_ALL".equals(item.roleId)) {
                displayRoleName = "Quản trị viên";
            } else if ("R_STORE_MNG".equals(item.roleId)) {
                displayRoleName = "Quản lý cửa hàng";
            } else if ("R_STAFF_SALE".equals(item.roleId)) {
                displayRoleName = "Nhân viên bán hàng";
            } else if ("R_STAFF_VIEW_PROD".equals(item.roleId) || "R_STAFF_STOCK".equals(item.roleId)) {
                displayRoleName = "Nhân viên kho";
            }

            if (searchRoleQuery.isEmpty() || displayRoleName.toLowerCase().contains(searchRoleQuery)) {
                gbc.gridy = rowIndex++;
                gbc.weighty = 0.0;
                gbc.insets = new Insets(0, 0, 0, 0);

                gbc.gridx = 0;
                JPanel cellRole = new JPanel(new BorderLayout());
                cellRole.setBackground(cardWhite);
                cellRole.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderGray));

                JLabel lblRole = new JLabel(displayRoleName);
                lblRole.setFont(new Font("Segoe UI", Font.BOLD, 14));
                lblRole.setForeground(textDark);

                if ("R_ADMIN_ALL".equals(item.roleId)) {
                    lblRole.setForeground(new Color(220, 53, 69));
                }

                lblRole.setBorder(new EmptyBorder(12, 10, 12, 0));
                cellRole.add(lblRole, BorderLayout.CENTER);

                // Chỉ cho xóa mềm các vai trò phụ, tuyệt đối không cho xóa role Admin lõi.
                if (!"R_ADMIN_ALL".equals(item.roleId)) {
                    JButton btnDeleteRole = new JButton();

                    try {
                        btnDeleteRole.setIcon(view.components.IconHelper.delete(18));
                    } catch (Exception ex) {
                        btnDeleteRole.setText("❌");
                        btnDeleteRole.setForeground(Color.RED);
                    }

                    btnDeleteRole.setBorderPainted(false);
                    btnDeleteRole.setContentAreaFilled(false);
                    btnDeleteRole.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    btnDeleteRole.setToolTipText("Xóa vai trò này (Xóa mềm)");

                    final String finalRoleId = item.roleId;
                    final String finalRoleName = displayRoleName;
                    btnDeleteRole.addActionListener(e -> handleDeleteRole(finalRoleId, finalRoleName));

                    cellRole.add(btnDeleteRole, BorderLayout.EAST);
                }

                tablePanel.add(cellRole, gbc);

                boolean isAdmin = "R_ADMIN_ALL".equals(item.roleId);

                for (int j = 0; j < actionList.size(); j++) {
                    gbc.gridx = j + 1;
                    JPanel cellCb = new JPanel(new GridBagLayout());
                    cellCb.setBackground(cardWhite);
                    cellCb.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderGray));

                    JCheckBox cb = new JCheckBox();
                    cb.setBackground(cardWhite);
                    cb.setCursor(new Cursor(Cursor.HAND_CURSOR));

                    if (j == 0) {
                        cb.setSelected(item.canView);
                    } else if (j == 1) {
                        cb.setSelected(item.canAdd);
                    } else if (j == 2) {
                        cb.setSelected(item.canEdit);
                    } else if (j == 3) {
                        cb.setSelected(item.canDelete);
                    } else if (j == 4) {
                        cb.setSelected(item.canExport);
                    }

                    /*
                     * Admin chỉ khóa 4 quyền lõi, nhưng quyền Xóa phải chỉnh được.
                     * Mục đích: demo Admin có thể bị chặn thao tác xóa ở module nghiệp vụ
                     * như Khách hàng, nhưng vẫn dùng được màn phân quyền.
                     */
                    if (isAdmin && j != 3) {
                        cb.setSelected(true);
                        cb.setEnabled(false);
                    } else {
                        final int actionIndex = j;
                        cb.addActionListener(e -> {
                            if (actionIndex == 0) {
                                item.canView = cb.isSelected();
                            } else if (actionIndex == 1) {
                                item.canAdd = cb.isSelected();
                            } else if (actionIndex == 2) {
                                item.canEdit = cb.isSelected();
                            } else if (actionIndex == 3) {
                                item.canDelete = cb.isSelected();
                            } else if (actionIndex == 4) {
                                item.canExport = cb.isSelected();
                            }
                        });
                    }

                    cellCb.add(cb);
                    tablePanel.add(cellCb, gbc);
                }
            }
        }

        gbc.gridy = rowIndex;
        gbc.weighty = 1.0;
        gbc.gridx = 0;
        gbc.gridwidth = actionList.size() + 1;
        JPanel filler = new JPanel();
        filler.setBackground(cardWhite);
        tablePanel.add(filler, gbc);

        JScrollPane scroll = new JScrollPane(tablePanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(cardWhite);
        container.add(scroll, BorderLayout.CENTER);

        JPanel bottomBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomBtns.setBackground(cardWhite);
        bottomBtns.setBorder(new EmptyBorder(15, 0, 0, 0));

        JButton btnSave = createCustomButton("Lưu toàn bộ thay đổi", primaryBlue, Color.WHITE);
        btnSave.addActionListener(e -> saveMatrixToDB());

        bottomBtns.add(btnSave);
        container.add(bottomBtns, BorderLayout.SOUTH);

        return container;
    }

    private void handleDeleteRole(String roleId, String roleName) {
        JPasswordField pf = new JPasswordField();

        int okCxl = JOptionPane.showConfirmDialog(
                this,
                new Object[]{"Vui lòng nhập mật khẩu Quản trị viên để xác nhận xóa vai trò [" + roleName + "]:", pf},
                "Xác thực bảo mật",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (okCxl != JOptionPane.OK_OPTION) {
            return;
        }

        String password = new String(pf.getPassword());

        if (!verifyAdminPassword(password)) {
            JOptionPane.showMessageDialog(this, "Mật khẩu không chính xác! Từ chối thao tác.", "Lỗi bảo mật", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "UPDATE ROLES SET is_deleted = 1 WHERE role_id = ? AND role_id <> 'R_ADMIN_ALL'";

        try (
                Connection con = common.db.DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, roleId);
            int affected = ps.executeUpdate();

            if (affected <= 0) {
                JOptionPane.showMessageDialog(this, "Không thể xóa vai trò lõi hoặc vai trò không tồn tại.", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            business.service.AuditLogService.logAction(
                    "XÓA",
                    "ROLES",
                    roleId,
                    roleName,
                    "Đã đưa vào thùng rác (Xóa mềm)",
                    "Admin xác thực mật khẩu và xóa vai trò"
            );

            JOptionPane.showMessageDialog(this, "Đã xóa vai trò [" + roleName + "] thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);

            loadDataFromDB();

            try {
                common.realtime.RealtimeNotifier.roleChanged("ROLE_DELETED:" + roleId);
            } catch (Exception ignored) {
                try {
                    common.realtime.RealtimeClient.send("ACCOUNT_SECURITY_CHANGED");
                    EventBus.publish(new AppDataChangedEvent(AppEventType.ACCOUNT_SECURITY, "ROLE_DELETED"));
                } catch (Exception ignored2) {
                }
            }

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi khi xóa vai trò: " + ex.getMessage(), "Lỗi DB", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean verifyAdminPassword(String inputPassword) {
        model.account.Account currentUser = business.service.LoginService.getCurrentUser();

        if (currentUser != null && currentUser.getPassword() != null) {
            return currentUser.getPassword().equals(inputPassword);
        }

        return false;
    }

    class RoleMatrixItem {

        String roleId;
        String roleName;
        boolean canView, canAdd, canEdit, canDelete, canExport;
        boolean oldView, oldAdd, oldEdit, oldDelete, oldExport;

        public RoleMatrixItem(String roleId, String roleName, boolean view, boolean add, boolean edit, boolean del, boolean exp) {
            this.roleId = roleId;
            this.roleName = roleName;
            this.canView = view;
            this.canAdd = add;
            this.canEdit = edit;
            this.canDelete = del;
            this.canExport = exp;

            this.oldView = view;
            this.oldAdd = add;
            this.oldEdit = edit;
            this.oldDelete = del;
            this.oldExport = exp;
        }

        public boolean isChanged() {
            return canView != oldView || canAdd != oldAdd || canEdit != oldEdit
                    || canDelete != oldDelete || canExport != oldExport;
        }

        public String getPermissionsString(boolean v, boolean a, boolean e, boolean d, boolean x) {
            java.util.List<String> list = new java.util.ArrayList<>();

            if (v) {
                list.add("Xem");
            }
            if (a) {
                list.add("Thêm");
            }
            if (e) {
                list.add("Sửa");
            }
            if (d) {
                list.add("Xóa");
            }
            if (x) {
                list.add("Xuất file");
            }

            return list.isEmpty() ? "Không có quyền" : String.join(", ", list);
        }

        public String getOldPermissions() {
            return getPermissionsString(oldView, oldAdd, oldEdit, oldDelete, oldExport);
        }

        public String getNewPermissions() {
            return getPermissionsString(canView, canAdd, canEdit, canDelete, canExport);
        }

        public void updateOriginals() {
            oldView = canView;
            oldAdd = canAdd;
            oldEdit = canEdit;
            oldDelete = canDelete;
            oldExport = canExport;
        }

        public String getActionDiff() {
            java.util.List<String> bat = new java.util.ArrayList<>();
            java.util.List<String> tat = new java.util.ArrayList<>();

            if (!oldView && canView) {
                bat.add("Xem");
            }
            if (oldView && !canView) {
                tat.add("Xem");
            }

            if (!oldAdd && canAdd) {
                bat.add("Thêm");
            }
            if (oldAdd && !canAdd) {
                tat.add("Thêm");
            }

            if (!oldEdit && canEdit) {
                bat.add("Sửa");
            }
            if (oldEdit && !canEdit) {
                tat.add("Sửa");
            }

            if (!oldDelete && canDelete) {
                bat.add("Xóa");
            }
            if (oldDelete && !canDelete) {
                tat.add("Xóa");
            }

            if (!oldExport && canExport) {
                bat.add("Xuất file");
            }
            if (oldExport && !canExport) {
                tat.add("Xuất file");
            }

            java.util.List<String> result = new java.util.ArrayList<>();

            if (!bat.isEmpty()) {
                result.add("Bật: " + String.join(", ", bat));
            }

            if (!tat.isEmpty()) {
                result.add("Tắt: " + String.join(", ", tat));
            }

            return String.join(" | ", result);
        }
    }

    private JButton createCustomButton(String text, Color bg, Color fg) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                super.paintComponent(g);
                g2.dispose();
            }
        };

        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(fg);
        btn.setPreferredSize(new Dimension(180, 40));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        return btn;
    }

    class RoundedPanel extends JPanel {

        private int radius;
        private Color bgColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    class RoundBorder implements javax.swing.border.Border {

        private Color color;
        private int radius;

        public RoundBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
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
