package view.components;

import javax.swing.*;
import java.awt.*;

public class UnifiedSettingsPanel extends JPanel {
    public UnifiedSettingsPanel() {
        setLayout(new BorderLayout());
        JPanel page = AdminUIFactory.page("Cài đặt", "Cấu hình hệ thống, giao diện và bảo mật");
        JPanel grid = new JPanel(new GridLayout(1, 3, 16, 16));
        grid.setOpaque(false);
        grid.add(settingCard("Thông tin hệ thống", "Quản lý cấu hình cửa hàng, email và tham số vận hành."));
        grid.add(settingCard("Giao diện", "Đồng bộ nền #F4F6FA, card trắng, table navy và nút bo góc."));
        grid.add(settingCard("Bảo mật", "Giữ nguyên logic đổi mật khẩu, session và EventBus hiện có."));
        page.add(grid, BorderLayout.CENTER);
        add(page, BorderLayout.CENTER);
    }
    private JPanel settingCard(String title, String body) {
        AdminUIFactory.RoundedPanel card = AdminUIFactory.card();
        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));
        inner.add(AdminUIFactory.cardTitle(title));
        inner.add(Box.createVerticalStrut(12));
        JTextArea text = new JTextArea(body);
        text.setWrapStyleWord(true);
        text.setLineWrap(true);
        text.setEditable(false);
        text.setOpaque(false);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        text.setForeground(AdminUIFactory.MUTED);
        inner.add(text);
        inner.add(Box.createVerticalGlue());
        inner.add(AdminUIFactory.button("Lam moi", AdminUIFactory.SECONDARY));
        card.add(inner, BorderLayout.CENTER);
        return card;
    }
}
