/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package view;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

/**
 * TongQuanPanel - Panel Tổng Quan hiển thị mặc định khi mở Dashboard.
 * Hiển thị các thống kê cơ bản và thông tin tổng quan hệ thống.
 *
 * @author Le Tan Vi
 */
public class TongQuanPanel extends JPanel {

    public TongQuanPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // === Tiêu đề ===
        JLabel lblTitle = new JLabel("Tổng Quan");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblTitle.setBorder(new EmptyBorder(0, 0, 15, 0));
        add(lblTitle, BorderLayout.NORTH);

        // === Nội dung chính ===
        JPanel contentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.weighty = 0;

        // --- Hàng 1: 4 Card thống kê ---
        gbc.gridy = 0;
        gbc.weightx = 0.25;
        gbc.gridx = 0;
        contentPanel.add(createStatCard("Sản phẩm", "1,250", "\uD83D\uDCE6"), gbc);

        gbc.gridx = 1;
        contentPanel.add(createStatCard("Đơn hàng hôm nay", "48", "\uD83D\uDCC4"), gbc);

        gbc.gridx = 2;
        contentPanel.add(createStatCard("Khách hàng", "3,820", "\uD83D\uDC65"), gbc);

        gbc.gridx = 3;
        contentPanel.add(createStatCard("Doanh thu tháng", "125,000,000 ₫", "\uD83D\uDCB0"), gbc);

        // --- Hàng 2: 2 panel phụ ---
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;
        contentPanel.add(createRecentOrdersPanel(), gbc);

        gbc.gridx = 2;
        gbc.gridwidth = 2;
        contentPanel.add(createQuickActionsPanel(), gbc);

        add(contentPanel, BorderLayout.CENTER);
    }

    /**
     * Tạo Card thống kê với icon, tiêu đề và giá trị.
     */
    private JPanel createStatCard(String title, String value, String emoji) {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"), 1),
                new EmptyBorder(15, 15, 15, 15)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Emoji icon
        gbc.gridy = 0;
        JLabel lblEmoji = new JLabel(emoji);
        lblEmoji.setFont(new Font("Segoe UI", Font.PLAIN, 28));
        card.add(lblEmoji, gbc);

        // Tiêu đề
        gbc.gridy = 1;
        gbc.insets = new Insets(8, 0, 2, 0);
        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblTitle.setForeground(UIManager.getColor("Label.disabledForeground"));
        card.add(lblTitle, gbc);

        // Giá trị
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 18));
        card.add(lblValue, gbc);

        return card;
    }

    /**
     * Tạo panel "Đơn hàng gần đây" placeholder.
     */
    private JPanel createRecentOrdersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        UIManager.getBorder("TitledBorder.border"),
                        "Đơn hàng gần đây",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 14)
                ),
                new EmptyBorder(10, 10, 10, 10)
        ));

        // Bảng placeholder
        String[] columns = {"Mã ĐH", "Khách hàng", "Tổng tiền", "Trạng thái"};
        Object[][] data = {
            {"DH001", "Nguyễn Văn A", "250,000 ₫", "Hoàn thành"},
            {"DH002", "Trần Thị B", "180,000 ₫", "Đang xử lý"},
            {"DH003", "Lê Văn C", "520,000 ₫", "Hoàn thành"},
            {"DH004", "Phạm Thị D", "95,000 ₫", "Chờ thanh toán"},
            {"DH005", "Hoàng Văn E", "310,000 ₫", "Hoàn thành"},
        };

        JTable table = new JTable(data, columns);
        table.setEnabled(false);
        table.setRowHeight(30);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(null);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Tạo panel "Thao tác nhanh" placeholder.
     */
    private JPanel createQuickActionsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        UIManager.getBorder("TitledBorder.border"),
                        "Thao tác nhanh",
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font("Segoe UI", Font.BOLD, 14)
                ),
                new EmptyBorder(10, 10, 10, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 5, 5, 5);

        String[] actions = {
            "🛒  Tạo đơn hàng mới",
            "📦  Thêm sản phẩm",
            "👤  Thêm khách hàng"
        };
        
        // Chỉ hiển thị nút xem thống kê cho Admin/Manager (không phải staff)
        model.account.Account currentUser = business.service.LoginService.getCurrentUser();
        if (currentUser != null && !currentUser.getUsername().equalsIgnoreCase("staff")) {
            actions = new String[]{
                "🛒  Tạo đơn hàng mới",
                "📦  Thêm sản phẩm",
                "👤  Thêm khách hàng",
                "📊  Xem thống kê"
            };
        }

        for (int i = 0; i < actions.length; i++) {
            gbc.gridy = i;
            JButton btn = new JButton(actions[i]);
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btn.putClientProperty("JButton.buttonType", "roundRect");
            btn.setHorizontalAlignment(SwingConstants.LEFT);
            panel.add(btn, gbc);
        }

        // Glue để đẩy buttons lên trên
        gbc.gridy = actions.length;
        gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }
}
