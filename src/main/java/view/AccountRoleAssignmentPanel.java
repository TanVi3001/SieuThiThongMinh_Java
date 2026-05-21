package view;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;
import common.realtime.*;

public class AccountRoleAssignmentPanel extends javax.swing.JPanel {

    private final Color bgLight = new Color(244, 246, 250);
    private final Color cardWhite = Color.WHITE;
    private final Color textDark = new Color(43, 54, 116);
    private final Color textGray = new Color(163, 174, 208);
    private final Color primaryBlue = new Color(67, 97, 238);
    private final Color borderGray = new Color(230, 235, 241);

    private JLabel lblSelectedUser;
    private JLabel lblSelectedEmail;
    private JPanel pnlCurrRole;
    private String selectedAccountId = "";
    private String selectedOldRole = "";

    private JPanel listItems;
    private JTextField txtSearch;
    private JComboBox<String> cbRole;

    private Map<String, JRadioButton> radioMap = new HashMap<>();
    private ButtonGroup roleGroup;

    private JPanel roleCardsContainer;
    private Map<String, JPanel> roleCardMap = new HashMap<>();
    private JButton btnSaveRole;

    public AccountRoleAssignmentPanel() {
        initComponents();
        setupModernLayout();
        setupRealtimeSync();
    }

    @SuppressWarnings("unchecked")
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
        this.setLayout(new BorderLayout(20, 20));
        this.setBackground(bgLight);
        this.setBorder(new EmptyBorder(30, 30, 30, 30));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(bgLight);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 5));
        titlePanel.setBackground(bgLight);
        JLabel title = new JLabel("Phân Quyền Tài Khoản");
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        title.setForeground(textDark);
        JLabel subtitle = new JLabel("Gán quyền hạn và Khóa/Mở khóa tài khoản hệ thống");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(textGray);
        titlePanel.add(title);
        titlePanel.add(subtitle);

        JLabel signedInBadge = new JLabel("<html><span style='color:#A3AED0'>Đăng nhập:</span> <b>Quản trị viên</b></html>");
        signedInBadge.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        signedInBadge.setHorizontalAlignment(SwingConstants.CENTER);
        signedInBadge.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 20),
                new EmptyBorder(5, 15, 5, 15)
        ));
        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightHeader.setBackground(bgLight);
        rightHeader.add(signedInBadge);

        header.add(titlePanel, BorderLayout.WEST);
        header.add(rightHeader, BorderLayout.EAST);
        this.add(header, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(20, 0));
        content.setBackground(bgLight);

        JPanel rightCol = createAssignmentColumn();
        JPanel leftCol = createAccountListColumn();

        rightCol.setPreferredSize(new Dimension(430, rightCol.getPreferredSize().height));

        content.add(leftCol, BorderLayout.CENTER);
        content.add(rightCol, BorderLayout.EAST);

        this.add(content, BorderLayout.CENTER);
    }

    private JPanel createAccountListColumn() {
        RoundedPanel container = new RoundedPanel(20, cardWhite);
        container.setLayout(new BorderLayout(0, 15));
        container.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel lblList = new JLabel("Danh sách tài khoản");
        lblList.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblList.setForeground(textDark);

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        filterPanel.setBackground(cardWhite);

        txtSearch = new JTextField(15);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm theo tên hoặc email...");
        txtSearch.setPreferredSize(new Dimension(200, 35));
        txtSearch.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 10), new EmptyBorder(5, 15, 5, 15)
        ));

        cbRole = new JComboBox<>(new String[]{"Tất cả vai trò", "Quản trị viên", "Quản lý cửa hàng", "Nhân viên bán hàng", "Nhân viên kho"});
        cbRole.setPreferredSize(new Dimension(150, 35));
        cbRole.setBackground(Color.WHITE);
        cbRole.setBorder(new RoundBorder(borderGray, 10));

        filterPanel.add(txtSearch);
        filterPanel.add(cbRole);

        JPanel topSection = new JPanel(new BorderLayout(0, 15));
        topSection.setBackground(cardWhite);
        topSection.add(lblList, BorderLayout.NORTH);
        topSection.add(filterPanel, BorderLayout.CENTER);

        JPanel tableHeader = new JPanel(new GridLayout(1, 4, 10, 0));
        tableHeader.setBackground(new Color(248, 249, 252));
        tableHeader.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 10), new EmptyBorder(10, 15, 10, 15)
        ));
        String[] headers = {"Tài khoản", "Vai trò hiện tại", "Trạng thái", "Khóa / Mở"};
        for (String h : headers) {
            JLabel l = new JLabel(h);
            l.setFont(new Font("Segoe UI", Font.BOLD, 12));
            l.setForeground(textGray);
            if (h.equals("Khóa / Mở")) l.setHorizontalAlignment(SwingConstants.CENTER);
            tableHeader.add(l);
        }
        topSection.add(tableHeader, BorderLayout.SOUTH);
        container.add(topSection, BorderLayout.NORTH);

        listItems = new JPanel();
        listItems.setLayout(new BoxLayout(listItems, BoxLayout.Y_AXIS));
        listItems.setBackground(cardWhite);

        initTableData();

        cbRole.addActionListener(e -> initTableData());

        txtSearch.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { initTableData(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { initTableData(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { initTableData(); }
        });

        JScrollPane scroll = new JScrollPane(listItems);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(cardWhite);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        container.add(scroll, BorderLayout.CENTER);

        return container;
    }

    private JPanel createAccountRow(String accountId, String name, String email, String role, boolean isActive) {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 65));
        row.setBackground(cardWhite);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, borderGray),
                new EmptyBorder(10, 15, 10, 15)
        ));

        JPanel pnlName = new JPanel(new GridLayout(2, 1));
        pnlName.setBackground(cardWhite);
        JLabel lblName = new JLabel(name);
        lblName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblName.setForeground(textDark);
        JLabel lblEmail = new JLabel(email);
        lblEmail.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblEmail.setForeground(textGray);
        pnlName.add(lblName);
        pnlName.add(lblEmail);
        row.add(pnlName);

        JPanel pnlRoleBadge = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 10));
        pnlRoleBadge.setBackground(cardWhite);
        pnlRoleBadge.add(createBadge(role));
        row.add(pnlRoleBadge);

        String colorHex = isActive ? "#10B981" : "#EF4444";
        String statusText = isActive ? "Hoạt động" : "Bị khóa";
        JLabel lblStatus = new JLabel("<html><span style='color:" + colorHex + "; font-size:14px;'>●</span> <span style='color:#2B3674;'>" + statusText + "</span></html>");
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        row.add(lblStatus);

        JPanel pnlToggle = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 15));
        pnlToggle.setBackground(cardWhite);
        ToggleSwitch toggleBtn = new ToggleSwitch(isActive);
        
        if ("Quản trị viên".equals(role)) {
            toggleBtn.setEnabled(false);
            toggleBtn.setToolTipText("Không thể khóa tài khoản Quản trị viên.");
        }
        
        toggleBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!toggleBtn.isEnabled()) return;

                boolean nextState = !toggleBtn.isOn(); 
                String actionName = nextState ? "Mở khóa" : "Khóa";
                String confirmMsg = nextState ? "Bạn có chắc chắn muốn MỞ KHÓA tài khoản [" + name + "]?" 
                                              : "KHÓA tài khoản [" + name + "]?\nNgười dùng sẽ bị đăng xuất khỏi hệ thống ngay lập tức.";
                
                int confirm = JOptionPane.showConfirmDialog(null, confirmMsg, "Xác nhận " + actionName, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                
                if (confirm == JOptionPane.YES_OPTION) {
                    try (Connection con = common.db.DatabaseConnection.getConnection();
                         PreparedStatement ps = con.prepareStatement("UPDATE ACCOUNTS SET is_deleted = ? WHERE account_id = ?")) {
                        
                        ps.setInt(1, nextState ? 0 : 1);
                        ps.setString(2, accountId);
                        int updated = ps.executeUpdate();
                        
                        if (updated > 0) {
                            toggleBtn.setOn(nextState);
                            
                            business.service.AuditLogService.logAction(
                                "CẬP NHẬT", "ACCOUNTS", accountId, 
                                nextState ? "Bị khóa" : "Hoạt động", 
                                nextState ? "Hoạt động" : "Bị khóa", 
                                "Admin " + actionName.toLowerCase() + " tài khoản"
                            );
                            
                            common.sync.SyncVersionDao.bumpVersion("EMPLOYEES");
                            RealtimeClient.send("ACCOUNT_SECURITY_CHANGED");
                            EventBus.publish(new AppDataChangedEvent(AppEventType.ACCOUNT_SECURITY, "TOGGLE_LOCK"));
                            
                            SwingUtilities.invokeLater(() -> initTableData());
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(null, "Lỗi khi cập nhật trạng thái: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        pnlToggle.add(toggleBtn);
        row.add(pnlToggle);

        row.setCursor(new Cursor(Cursor.HAND_CURSOR));
        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getX() >= pnlToggle.getX()) return;

                selectedAccountId = accountId;
                selectedOldRole = role;
                lblSelectedUser.setText(name);
                lblSelectedEmail.setText(email);

                pnlCurrRole.removeAll();
                pnlCurrRole.add(createBadge(role));
                pnlCurrRole.revalidate();
                pnlCurrRole.repaint();

                roleCardsContainer.removeAll(); 

                if ("Quản trị viên".equals(role)) {
                    roleCardsContainer.add(roleCardMap.get("Quản trị viên"));
                    radioMap.get("Quản trị viên").setEnabled(false);
                    btnSaveRole.setEnabled(false); 
                } else {
                    roleCardsContainer.add(roleCardMap.get("Quản lý cửa hàng"));
                    roleCardsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
                    roleCardsContainer.add(roleCardMap.get("Nhân viên bán hàng"));
                    roleCardsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
                    roleCardsContainer.add(roleCardMap.get("Nhân viên kho"));

                    radioMap.get("Quản lý cửa hàng").setEnabled(true);
                    radioMap.get("Nhân viên bán hàng").setEnabled(true);
                    radioMap.get("Nhân viên kho").setEnabled(true);
                    btnSaveRole.setEnabled(true); 
                }

                roleCardsContainer.revalidate();
                roleCardsContainer.repaint();

                if (radioMap.containsKey(role)) {
                    radioMap.get(role).setSelected(true);
                } else {
                    roleGroup.clearSelection();
                }
            }
        });

        return row;
    }

    private JPanel createAssignmentColumn() {
        RoundedPanel container = new RoundedPanel(20, cardWhite);
        container.setLayout(new BorderLayout(0, 20));
        container.setBorder(new EmptyBorder(25, 25, 25, 25));

        JLabel lblAssign = new JLabel("Gán Vai Trò Mới");
        lblAssign.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblAssign.setForeground(textDark);
        container.add(lblAssign, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(cardWhite);

        lblSelectedUser = createLabel("-", textDark, true);
        lblSelectedEmail = createLabel("-", textDark, false);
        pnlCurrRole = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlCurrRole.setBackground(cardWhite);

        JPanel infoGrid = new JPanel(new GridBagLayout());
        infoGrid.setBackground(cardWhite);
        infoGrid.setBorder(new EmptyBorder(0, 0, 20, 0));
        infoGrid.setAlignmentX(Component.LEFT_ALIGNMENT);

        GridBagConstraints gbcInfo = new GridBagConstraints();
        gbcInfo.fill = GridBagConstraints.HORIZONTAL;
        gbcInfo.anchor = GridBagConstraints.WEST;

        gbcInfo.gridy = 0; gbcInfo.insets = new Insets(0, 0, 15, 20); gbcInfo.gridx = 0; gbcInfo.weightx = 0.0;
        infoGrid.add(createLabel("Người dùng đã chọn", textGray), gbcInfo);
        gbcInfo.insets = new Insets(0, 0, 15, 0); gbcInfo.gridx = 1; gbcInfo.weightx = 1.0;
        infoGrid.add(lblSelectedUser, gbcInfo);

        gbcInfo.gridy = 1; gbcInfo.insets = new Insets(0, 0, 15, 20); gbcInfo.gridx = 0; gbcInfo.weightx = 0.0;
        infoGrid.add(createLabel("Email", textGray), gbcInfo);
        gbcInfo.insets = new Insets(0, 0, 15, 0); gbcInfo.gridx = 1; gbcInfo.weightx = 1.0;
        infoGrid.add(lblSelectedEmail, gbcInfo);

        gbcInfo.gridy = 2; gbcInfo.insets = new Insets(0, 0, 0, 20); gbcInfo.gridx = 0; gbcInfo.weightx = 0.0;
        infoGrid.add(createLabel("Vai trò hiện tại", textGray), gbcInfo);
        gbcInfo.insets = new Insets(0, 0, 0, 0); gbcInfo.gridx = 1; gbcInfo.weightx = 1.0;
        infoGrid.add(pnlCurrRole, gbcInfo);

        centerPanel.add(infoGrid);

        roleGroup = new ButtonGroup();
        radioMap.clear();
        roleCardMap.clear();

        roleCardsContainer = new JPanel();
        roleCardsContainer.setLayout(new BoxLayout(roleCardsContainer, BoxLayout.Y_AXIS));
        roleCardsContainer.setBackground(cardWhite);
        roleCardsContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[][] activeRoles = {
            {"R_ADMIN_ALL", "Quản trị viên", "Toàn quyền quản lý hệ thống, nhân sự và thiết lập."},
            {"R_STORE_MNG", "Quản lý cửa hàng", "Quản lý hoạt động cửa hàng, xem báo cáo."},
            {"R_STAFF_SALE", "Nhân viên bán hàng", "Truy cập màn hình POS, tạo hóa đơn và thanh toán."},
            {"R_STAFF_VIEW_PROD", "Nhân viên kho", "Quản lý sản phẩm, lập phiếu nhập và tồn kho."}
        };

        for (String[] roleInfo : activeRoles) {
            JRadioButton rb = new JRadioButton();
            rb.setActionCommand(roleInfo[0]); 
            radioMap.put(roleInfo[1], rb); 

            JPanel card = createRoleCard(roleInfo[1], roleInfo[2], roleGroup, rb);
            card.setAlignmentX(Component.LEFT_ALIGNMENT);
            roleCardMap.put(roleInfo[1], card);
        }

        roleCardsContainer.add(roleCardMap.get("Quản lý cửa hàng"));
        roleCardsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        roleCardsContainer.add(roleCardMap.get("Nhân viên bán hàng"));
        roleCardsContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        roleCardsContainer.add(roleCardMap.get("Nhân viên kho"));

        centerPanel.add(roleCardsContainer);

        JPanel summaryBox = new RoundedPanel(10, new Color(248, 249, 252));
        summaryBox.setLayout(new BorderLayout());
        summaryBox.setBorder(BorderFactory.createCompoundBorder(
                new DashedBorder(textGray, 1, 5), new EmptyBorder(15, 15, 15, 15)
        ));
        summaryBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        summaryBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblSum = new JLabel("<html><b>Tóm tắt thay đổi:</b><br><span style='font-size:10px; color:#666;'>Tài khoản được chọn sẽ giữ nguyên vai trò hiện tại trừ khi bạn chọn vai trò khác trước khi lưu.</span></html>");
        lblSum.setForeground(textDark);
        summaryBox.add(lblSum, BorderLayout.CENTER);

        centerPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        centerPanel.add(summaryBox);
        container.add(centerPanel, BorderLayout.CENTER);

        JPanel bottomBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottomBtns.setBackground(cardWhite);
        JButton btnCancel = createCustomButton("Hủy bỏ", new Color(235, 238, 244), textDark);
        btnSaveRole = createCustomButton("Lưu thay đổi", primaryBlue, Color.WHITE);
        bottomBtns.add(btnCancel);

        btnSaveRole.addActionListener(e -> {
            if (selectedAccountId == null || selectedAccountId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một tài khoản từ danh sách bên trái!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            ButtonModel selectedModel = roleGroup.getSelection();
            if (selectedModel == null) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn một vai trò mới để gán!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String newRoleId = selectedModel.getActionCommand();
            boolean success = business.sql.rbac.AccountSql.getInstance().updateAccountRole(selectedAccountId, newRoleId);

            if (success) {
                business.service.AuditLogService.logAction(
                        "CẬP NHẬT", "ACCOUNTS", selectedAccountId, selectedOldRole, newRoleId, "Admin thay đổi quyền nhân viên"
                );

                JOptionPane.showMessageDialog(this, "Cập nhật phân quyền thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);

                AppDataChangedEvent securityEvent = new AppDataChangedEvent(AppEventType.ACCOUNT_SECURITY, "ROLE_CHANGED");
                try {
                    common.realtime.RealtimeClient.send("ACCOUNT_SECURITY_CHANGED");
                    common.realtime.RealtimeClient.send("EMPLOYEES_CHANGED");
                    EventBus.publish(securityEvent);
                } catch (Exception ex) {
                    System.err.println("Lỗi đồng bộ: " + ex.getMessage());
                }

                selectedAccountId = "";
                lblSelectedUser.setText("-");
                lblSelectedEmail.setText("-");
                pnlCurrRole.removeAll();
                pnlCurrRole.repaint();
                roleGroup.clearSelection();
                
                initTableData(); 
            } else {
                JOptionPane.showMessageDialog(this, "Cập nhật thất bại. Vui lòng kiểm tra lại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        bottomBtns.add(btnSaveRole);
        container.add(bottomBtns, BorderLayout.SOUTH);

        return container;
    }

    private JLabel createLabel(String text, Color color) {
        return createLabel(text, color, false);
    }

    private JLabel createLabel(String text, Color color, boolean isBold) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", isBold ? Font.BOLD : Font.PLAIN, 13));
        l.setForeground(color);
        return l;
    }

    private JLabel createBadge(String role) {
        Color bg, fg;

        if ("Quản trị viên".equals(role)) {
            bg = new Color(255, 235, 238);
            fg = new Color(220, 53, 69);
        } else if ("Quản lý cửa hàng".equals(role)) {
            bg = new Color(255, 248, 225);
            fg = new Color(245, 158, 11);
        } else if ("Nhân viên kho".equals(role)) {
            bg = new Color(243, 232, 255);
            fg = new Color(147, 51, 234);
        } else {
            bg = new Color(237, 242, 255);
            fg = primaryBlue;
        }

        JLabel badge = new JLabel(role, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(fg);
        badge.setOpaque(false);
        badge.setBorder(new EmptyBorder(5, 15, 5, 15));

        return badge;
    }

    private JPanel createRoleCard(String title, String desc, ButtonGroup group, JRadioButton rb) {
        JPanel card = new RoundedPanel(10, cardWhite);
        card.setLayout(new BorderLayout(10, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(borderGray, 10), new EmptyBorder(12, 15, 12, 15)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 75));

        rb.setBackground(cardWhite);
        group.add(rb);

        JLabel lblText = new JLabel("<html><b style='color:#2B3674; font-size:12px;'>" + title + "</b><br><span style='color:#A3AED0; font-size:10px;'>" + desc + "</span></html>");

        card.add(rb, BorderLayout.WEST);
        card.add(lblText, BorderLayout.CENTER);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (card.isEnabled() && rb.isEnabled()) {
                    rb.setSelected(true);
                }
            }
        });

        rb.addItemListener(e -> {
            if (rb.isSelected()) {
                card.setBorder(BorderFactory.createCompoundBorder(
                        new RoundBorder(primaryBlue, 10), new EmptyBorder(12, 15, 12, 15)
                ));
            } else {
                card.setBorder(BorderFactory.createCompoundBorder(
                        new RoundBorder(borderGray, 10), new EmptyBorder(12, 15, 12, 15)
                ));
            }
            card.revalidate();
            card.repaint();
        });

        return card;
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
        btn.setPreferredSize(new Dimension(130, 38));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void initTableData() {
        if (listItems == null) return;
        listItems.removeAll();

        java.util.List<String[]> listAcc = business.sql.rbac.AccountSql.getInstance().getAccountWithUserDetails();
        
        String selectedRoleFilter = (cbRole != null && cbRole.getSelectedItem() != null) ? cbRole.getSelectedItem().toString() : "Tất cả vai trò";
        String searchText = (txtSearch != null) ? txtSearch.getText().toLowerCase().trim() : "";

        for (String[] acc : listAcc) {
            String roleId = acc[4];
            String displayRole = "Nhân viên bán hàng";
            if ("R_ADMIN_ALL".equals(roleId)) displayRole = "Quản trị viên";
            else if ("R_STORE_MNG".equals(roleId)) displayRole = "Quản lý cửa hàng";
            else if ("R_STAFF_STOCK".equals(roleId) || "R_STAFF_VIEW_PROD".equals(roleId)) displayRole = "Nhân viên kho";

            String displayName = (acc[2] == null || acc[2].isEmpty()) ? acc[1] : acc[2];
            String displayEmail = (acc[3] == null || acc[3].isEmpty()) ? "Chưa có email" : acc[3];

            boolean isActive = "0".equals(acc[5]); 

            boolean matchRole = "Tất cả vai trò".equals(selectedRoleFilter) || displayRole.equals(selectedRoleFilter);
            boolean matchSearch = searchText.isEmpty() || displayName.toLowerCase().contains(searchText) || displayEmail.toLowerCase().contains(searchText);

            if (matchRole && matchSearch) {
                listItems.add(createAccountRow(acc[0], displayName, displayEmail, displayRole, isActive));
            }
        }
        
        listItems.revalidate();
        listItems.repaint();
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
        private boolean fillBg = false;

        public RoundBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        public RoundBorder(Color color, int radius, boolean fillBg) {
            this.color = color;
            this.radius = radius;
            this.fillBg = fillBg;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            if (fillBg) {
                g2.fillRoundRect(x, y, width - 1, height - 1, radius, radius);
            } else {
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            }
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) { return new Insets(1, 1, 1, 1); }

        @Override
        public boolean isBorderOpaque() { return false; }
    }

    class DashedBorder implements javax.swing.border.Border {
        private Color color;
        private int thickness;
        private int dashLength;

        public DashedBorder(Color color, int thickness, int dashLength) {
            this.color = color;
            this.thickness = thickness;
            this.dashLength = dashLength;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            Stroke dashed = new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{dashLength}, 0);
            g2.setStroke(dashed);
            g2.drawRoundRect(x, y, width - 1, height - 1, 10, 10);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) { return new Insets(thickness, thickness, thickness, thickness); }

        @Override
        public boolean isBorderOpaque() { return false; }
    }

    private void setupRealtimeSync() {
        EventBus.subscribe(AppDataChangedEvent.class, event -> {
            if (event.getType() == AppEventType.ACCOUNT_SECURITY || event.getType() == AppEventType.EMPLOYEES) {                        
                // Không popup, cập nhật ngầm
            }
        });
    }

    class ToggleSwitch extends JComponent {
        private boolean on;
        
        public ToggleSwitch(boolean on) {
            this.on = on;
            setPreferredSize(new Dimension(46, 24));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        
        public boolean isOn() { return on; }
        
        public void setOn(boolean on) { 
            this.on = on; 
            repaint(); 
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            if (on) {
                g2.setColor(new Color(16, 185, 129)); // Màu Xanh lá (Hoạt động)
                g2.fillRoundRect(0, 0, 46, 24, 24, 24);
                g2.setColor(Color.WHITE);
                g2.fillOval(24, 2, 20, 20);
            } else {
                g2.setColor(new Color(203, 213, 225)); // Màu Xám (Bị Khóa)
                if (!isEnabled()) g2.setColor(new Color(241, 245, 249)); 
                g2.fillRoundRect(0, 0, 46, 24, 24, 24);
                g2.setColor(Color.WHITE);
                g2.fillOval(2, 2, 20, 20);
            }
            g2.dispose();
        }
    }
}