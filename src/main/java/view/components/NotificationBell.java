package view.components;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import business.sql.prod_inventory.ProductsSql;
import business.sql.prod_inventory.InventoryNotificationSql;
import model.product.Product;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.FlowLayout;
import java.awt.RenderingHints;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Hộp thư thông báo – hiển thị chuông ở góc trên phải, khi click sẽ mở popup
 * danh sách thông báo. Hỗ trợ real-time khi nhận event INVENTORY hoặc PRODUCTS.
 *
 * Nghiệp vụ mới: - Cảnh báo do Manager gửi cho Kho sẽ được lưu DB. - Cảnh báo
 * đó không mất khi tắt app / mở lại app. - Nút "Xóa tất cả" không xóa cảnh báo
 * Manager. - Cảnh báo Manager chỉ mất khi sản phẩm đó được nhập thêm và DB
 * chuyển sang RESOLVED.
 */
public class NotificationBell extends JPanel {

    // Mốc cảnh báo
    public static final int THRESHOLD_DANGER = 5;
    public static final int THRESHOLD_WARNING = 20;

    private final List<NotifItem> notifications = new ArrayList<>();
    private final JLabel lblBell;
    private final JLabel lblBadge;
    private int unreadCount = 0;

    // Popup dropdown
    private JWindow popup;
    private final JPanel popupList;

    // Màu sắc
    private static final Color COLOR_DANGER = new Color(220, 53, 69);
    private static final Color COLOR_WARNING = new Color(255, 152, 0);
    private static final Color COLOR_INFO = new Color(67, 97, 238);
    private static final Color COLOR_BG = new Color(255, 255, 255);
    private static final int POPUP_WIDTH = 720;
    private static final int POPUP_HEIGHT = 560;
    private static final int NOTIF_TEXT_WIDTH = 545;

    // Phân loại: ai nhận gì
    public enum Audience {
        WAREHOUSE, MANAGER, SALE, ALL
    }

    private final Audience audience;

    public NotificationBell(Audience audience) {
        this.audience = audience;

        setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        setOpaque(false);

        lblBell = new JLabel("🔔");
        lblBell.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 22));
        lblBell.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblBell.setToolTipText("Thông báo hệ thống");

        lblBadge = new JLabel("0");
        lblBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblBadge.setForeground(Color.WHITE);
        lblBadge.setHorizontalAlignment(SwingConstants.CENTER);
        lblBadge.setVisible(false);

        JLayeredPane layered = new JLayeredPane();
        layered.setPreferredSize(new Dimension(36, 36));
        layered.setOpaque(false);

        lblBell.setBounds(4, 4, 28, 28);
        lblBadge.setBounds(22, 0, 16, 16);

        layered.add(lblBell, JLayeredPane.DEFAULT_LAYER);
        layered.add(lblBadge, JLayeredPane.PALETTE_LAYER);

        lblBadge.setUI(new javax.swing.plaf.basic.BasicLabelUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_DANGER);
                g2.fillOval(0, 0, c.getWidth(), c.getHeight());
                g2.dispose();
                super.paint(g, c);
            }
        });

        add(layered);

        popupList = new JPanel();
        popupList.setLayout(new BoxLayout(popupList, BoxLayout.Y_AXIS));
        popupList.setBackground(COLOR_BG);

        lblBell.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                togglePopup();
            }
        });

        EventBus.subscribe(AppDataChangedEvent.class, event -> {
            if (event.getType() == AppEventType.INVENTORY_ALERT) {
                SwingUtilities.invokeLater(() -> handleInventoryAlert(event.getMessage()));
                return;
            }

            if (event.getType() == AppEventType.INVENTORY || event.getType() == AppEventType.PRODUCTS) {
                SwingUtilities.invokeLater(() -> {
                    checkLowStock();
                    refreshPersistentWarehouseAlerts();
                });
            }
        });

        SwingUtilities.invokeLater(() -> {
            checkLowStock();
            refreshPersistentWarehouseAlerts();
        });
    }

    /**
     * Load lại cảnh báo Manager còn PENDING trong DB. Trước khi load, xóa các
     * cảnh báo Manager trong RAM để những cái đã RESOLVED biến mất.
     */
    private void refreshPersistentWarehouseAlerts() {
        if (this.audience != Audience.WAREHOUSE && this.audience != Audience.ALL) {
            return;
        }

        notifications.removeIf(n -> n.urgentManagerAlert && n.key.startsWith("MANAGER_STOCK_ALERT_"));

        try {
            List<InventoryNotificationSql.InventoryNotifDTO> list
                    = InventoryNotificationSql.getInstance().getPendingWarehouseAlerts();

            for (InventoryNotificationSql.InventoryNotifDTO dto : list) {
                String key = "MANAGER_STOCK_ALERT_" + dto.productId;

                String title = "🚨 QUẢN LÝ NHẮC NHẬP HÀNG: " + dto.productName;

                String body = dto.message;

                if (dto.remindCount >= 2) {
                    body += "<br><br><b style='color:#DC3545'>"
                            + "Manager đã nhắc " + dto.remindCount
                            + " lần. Thông báo này sẽ chỉ mất khi sản phẩm được nhập thêm."
                            + "</b>";
                }

                addOrUpdateNotification(
                        key,
                        NotifItem.Type.DANGER,
                        title,
                        body,
                        Audience.WAREHOUSE,
                        true
                );
            }

            updateBadge();
            rebuildPopupList();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Kiểm tra tồn kho tự động. Các cảnh báo auto này chỉ là cảnh báo UI, có
     * thể xóa bằng "Xóa tất cả".
     */
    public void checkLowStock() {
        try {
            List<Product> products = ProductsSql.getInstance().selectAll();

            for (Product p : products) {
                int qty = p.getQuantity();

                if (qty <= 0) {
                    addNotification(
                            NotifItem.Type.DANGER,
                            "❌ HẾT HÀNG: " + p.getProductName(),
                            "Sản phẩm [" + p.getProductId() + "] đã hết hoàn toàn. Cần nhập khẩn!",
                            Audience.ALL
                    );
                } else if (qty <= THRESHOLD_DANGER) {
                    addNotification(
                            NotifItem.Type.DANGER,
                            "⚠️ SẮP HẾT: " + p.getProductName(),
                            "Chỉ còn " + qty + " sản phẩm. Cần nhập ngay!",
                            Audience.ALL
                    );
                } else if (qty <= THRESHOLD_WARNING) {
                    addNotification(
                            NotifItem.Type.WARNING,
                            "📦 Tồn kho thấp: " + p.getProductName(),
                            "Còn " + qty + " sản phẩm. Nên lên kế hoạch nhập thêm.",
                            Audience.WAREHOUSE
                    );
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Xử lý cảnh báo do Manager gửi. Format: INVENTORY_ALERT:SP0000038:Cá hồi
     * phi lê tươi 200g:0
     */
    private void handleInventoryAlert(String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return;
        }

        String productId = "";
        String productName = "Sản phẩm";
        String quantity = "0";

        try {
            String[] parts = rawMessage.split(":", 4);

            if (parts.length >= 2) {
                productId = parts[1].trim();
            }

            if (parts.length >= 3) {
                productName = parts[2].trim();
            }

            if (parts.length >= 4) {
                quantity = parts[3].trim();
            }
        } catch (Exception ignored) {
        }

        if (productId == null || productId.trim().isEmpty()) {
            return;
        }

        String key = "MANAGER_STOCK_ALERT_" + productId;

        String title = "🚨 QUẢN LÝ NHẮC NHẬP HÀNG: " + productName;

        String body = "Manager đã gửi cảnh báo khẩn. "
                + "Sản phẩm [" + productId + "] hiện còn " + quantity
                + " sản phẩm. CẦN KIỂM TRA VÀ NHẬP HÀNG NGAY.";

        // Lưu xuống DB để tắt app / mở lại vẫn còn thông báo.
        try {
            String createdBy = null;

            if (business.service.SessionManager.getCurrentUser() != null) {
                createdBy = business.service.SessionManager.getCurrentUser().getAccountId();
            }

            InventoryNotificationSql.getInstance().createOrRemindAlert(
                    productId,
                    productName,
                    body,
                    createdBy
            );

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        // Hiển thị realtime ngay trên chuông.
        addOrUpdateNotification(
                key,
                NotifItem.Type.DANGER,
                title,
                body,
                Audience.WAREHOUSE,
                true
        );
    }

    public void addNotification(NotifItem.Type type, String title, String body, Audience target) {
        if (target != Audience.ALL && target != this.audience) {
            return;
        }

        String key = "AUTO_" + title;

        boolean exists = notifications.stream().anyMatch(n -> n.key.equals(key));

        if (exists) {
            return;
        }

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        NotifItem item = new NotifItem(
                key,
                type,
                title,
                body,
                time,
                false
        );

        notifications.add(0, item);
        unreadCount++;

        updateBadge();
        rebuildPopupList();
    }

    private void addOrUpdateNotification(
            String key,
            NotifItem.Type type,
            String title,
            String body,
            Audience target,
            boolean urgentManagerAlert
    ) {
        if (target != Audience.ALL && target != this.audience) {
            return;
        }

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        for (int i = 0; i < notifications.size(); i++) {
            NotifItem n = notifications.get(i);

            if (n.key.equals(key)) {
                n.remindCount++;
                n.type = type;
                n.title = title;
                n.time = time;
                n.urgentManagerAlert = urgentManagerAlert;

                if (n.remindCount >= 2) {
                    n.body = body
                            + "<br><br><b style='color:#DC3545'>"
                            + "Manager đã nhắc " + n.remindCount
                            + " lần. Việc này RẤT CẦN THIẾT, vui lòng xử lý ngay."
                            + "</b>";
                } else {
                    n.body = body;
                }

                notifications.remove(i);
                notifications.add(0, n);

                unreadCount++;
                updateBadge();
                rebuildPopupList();
                return;
            }
        }

        NotifItem item = new NotifItem(
                key,
                type,
                title,
                body,
                time,
                urgentManagerAlert
        );

        notifications.add(0, item);
        unreadCount++;

        updateBadge();
        rebuildPopupList();
    }

    private void updateBadge() {
        if (unreadCount > 0) {
            lblBadge.setText(unreadCount > 9 ? "9+" : String.valueOf(unreadCount));
            lblBadge.setVisible(true);
        } else {
            lblBadge.setVisible(false);
        }
    }

    private void togglePopup() {
        if (popup != null && popup.isVisible()) {
            popup.setVisible(false);
            return;
        }

        unreadCount = 0;
        updateBadge();
        showPopup();
    }

    private void showPopup() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);

        if (parentWindow == null) {
            return;
        }

        popup = new JWindow(parentWindow);
        popup.setLayout(new BorderLayout());

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setBackground(new Color(67, 97, 238));
        header.setBorder(new EmptyBorder(11, 16, 11, 12));

        JPanel titleGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleGroup.setOpaque(false);

        ImageIcon bellIcon = IconHelper.stock(18);

        if (bellIcon != null) {
            titleGroup.add(new JLabel(bellIcon));
        }

        JLabel lblTitle = new JLabel("Hộp thư thông báo");
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        titleGroup.add(lblTitle);

        JLabel lblCount = new JLabel("  " + notifications.size() + " thông báo  ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(255, 255, 255, 50));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                g2.dispose();
                super.paintComponent(g);
            }
        };

        lblCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCount.setForeground(Color.WHITE);
        lblCount.setOpaque(false);
        lblCount.setBorder(new EmptyBorder(4, 10, 4, 10));

        JButton btnClear = new JButton("  Xóa tất cả  ") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(220, 53, 69));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());

                g2.dispose();
                super.paintComponent(g);
            }
        };

        ImageIcon delIcon = IconHelper.delete(13);

        if (delIcon != null) {
            btnClear.setIcon(delIcon);
            btnClear.setIconTextGap(5);
        }

        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClear.setForeground(Color.WHITE);
        btnClear.setContentAreaFilled(false);
        btnClear.setBorderPainted(false);
        btnClear.setFocusPainted(false);
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClear.setBorder(new EmptyBorder(4, 10, 4, 10));

        // Chỉ xóa thông báo thường.
        // Không xóa thông báo Manager nhập hàng.
        btnClear.addActionListener(e -> {
            notifications.removeIf(n -> !n.urgentManagerAlert);

            unreadCount = 0;
            updateBadge();
            rebuildPopupList();
        });

        JPanel rightGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightGroup.setOpaque(false);
        rightGroup.add(lblCount);
        rightGroup.add(btnClear);

        header.add(titleGroup, BorderLayout.WEST);
        header.add(rightGroup, BorderLayout.EAST);

        rebuildPopupList();

        JScrollPane scroll = new JScrollPane(popupList);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(POPUP_WIDTH, POPUP_HEIGHT));
        scroll.getVerticalScrollBar().setUnitIncrement(10);

        popup.add(header, BorderLayout.NORTH);
        popup.add(scroll, BorderLayout.CENTER);
        popup.pack();

        Point loc = lblBell.getLocationOnScreen();

        int x = loc.x - POPUP_WIDTH + 55;
        int y = loc.y + 38;

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        if (x + POPUP_WIDTH > screen.width) {
            x = screen.width - POPUP_WIDTH - 12;
        }

        if (x < 0) {
            x = 12;
        }

        if (y + POPUP_HEIGHT > screen.height) {
            y = screen.height - POPUP_HEIGHT - 12;
        }

        popup.setLocation(x, y);
        popup.setVisible(true);

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;

                if (me.getID() == MouseEvent.MOUSE_PRESSED) {
                    if (popup != null && popup.isVisible()) {
                        if (!popup.getBounds().contains(me.getLocationOnScreen())) {
                            popup.setVisible(false);
                        }
                    }
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void rebuildPopupList() {
        popupList.removeAll();
        popupList.setBackground(new Color(248, 249, 252));

        if (notifications.isEmpty()) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setLayout(new BoxLayout(emptyPanel, BoxLayout.Y_AXIS));
            emptyPanel.setBackground(new Color(248, 249, 252));
            emptyPanel.setBorder(new EmptyBorder(30, 20, 30, 20));

            ImageIcon checkIcon = IconHelper.refresh(36);
            JLabel lblIcon = checkIcon != null ? new JLabel(checkIcon) : new JLabel("✅");

            lblIcon.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblText = new JLabel("Không có thông báo mới");
            lblText.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblText.setForeground(new Color(163, 174, 208));
            lblText.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblSub = new JLabel("Hệ thống sẽ tự báo khi có sự kiện");
            lblSub.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblSub.setForeground(new Color(200, 210, 225));
            lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

            emptyPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            emptyPanel.add(lblIcon);
            emptyPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            emptyPanel.add(lblText);
            emptyPanel.add(Box.createRigidArea(new Dimension(0, 5)));
            emptyPanel.add(lblSub);

            popupList.add(emptyPanel);
        } else {
            for (int i = 0; i < notifications.size(); i++) {
                popupList.add(createNotifCard(notifications.get(i)));

                if (i < notifications.size() - 1) {
                    JSeparator sep = new JSeparator();
                    sep.setForeground(new Color(230, 235, 245));
                    sep.setBackground(new Color(230, 235, 245));
                    popupList.add(sep);
                }
            }
        }

        popupList.revalidate();
        popupList.repaint();

        if (popup != null) {
            popup.pack();
        }
    }

    private JPanel createNotifCard(NotifItem n) {
        Color accent;
        Color bgCard;
        Color bgIcon;
        ImageIcon typeIcon;
        String typeTag;

        if (n.type == NotifItem.Type.DANGER) {
            accent = new Color(220, 53, 69);
            bgCard = new Color(255, 247, 247);
            bgIcon = new Color(255, 228, 230);
            typeIcon = IconHelper.delivery(15);
            typeTag = "KHẨN";
        } else if (n.type == NotifItem.Type.WARNING) {
            accent = new Color(230, 120, 0);
            bgCard = new Color(255, 251, 242);
            bgIcon = new Color(255, 237, 210);
            typeIcon = IconHelper.stock(15);
            typeTag = "CẢNH BÁO";
        } else {
            accent = COLOR_INFO;
            bgCard = new Color(246, 248, 255);
            bgIcon = new Color(225, 230, 255);
            typeIcon = IconHelper.barChart(15);
            typeTag = "THÔNG TIN";
        }

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(bgCard);

        JPanel stripe = new JPanel();
        stripe.setBackground(accent);
        stripe.setPreferredSize(new Dimension(5, 0));

        card.add(stripe, BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout(14, 0));
        content.setBackground(bgCard);
        content.setBorder(new EmptyBorder(18, 18, 18, 22));

        int circleSize = 46;

        JPanel iconCircle = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bgIcon);
                g2.fillOval(0, 0, getWidth(), getHeight());

                g2.dispose();
                super.paintComponent(g);
            }
        };

        iconCircle.setOpaque(false);
        iconCircle.setPreferredSize(new Dimension(circleSize, circleSize));
        iconCircle.setMinimumSize(new Dimension(circleSize, circleSize));
        iconCircle.setMaximumSize(new Dimension(circleSize, circleSize));

        if (typeIcon != null) {
            iconCircle.add(new JLabel(typeIcon));
        }

        JPanel iconWrapper = new JPanel(new GridBagLayout());
        iconWrapper.setOpaque(false);
        iconWrapper.add(iconCircle);

        content.add(iconWrapper, BorderLayout.WEST);

        JPanel textPanel = new JPanel(new GridBagLayout());
        textPanel.setOpaque(false);
        textPanel.setBorder(new EmptyBorder(0, 10, 0, 0));

        GridBagConstraints gc = new GridBagConstraints();

        JLabel lblTag = new JLabel(typeTag) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();

                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        lblTag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblTag.setForeground(Color.WHITE);
        lblTag.setOpaque(false);
        lblTag.setBorder(new EmptyBorder(3, 8, 3, 8));

        gc.gridx = 0;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(0, 0, 4, 8);
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;

        textPanel.add(lblTag, gc);

        String cleanTitle = n.title.replaceAll("^[\\p{So}\\p{Sm}\\s]*[^:]+:\\s*", "").trim();

        if (cleanTitle.isEmpty()) {
            cleanTitle = n.title;
        }

        JLabel lblTitle = new JLabel(cleanTitle);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(new Color(25, 35, 75));

        gc.gridx = 1;
        gc.gridy = 0;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 4, 0);

        textPanel.add(lblTitle, gc);

        String bodyHtml = escapeHtml(n.body)
                .replace("&lt;br&gt;", "<br>")
                .replace("&lt;br/&gt;", "<br>")
                .replace("&lt;br /&gt;", "<br>")
                .replace("&lt;b style=&#39;color:#DC3545&#39;&gt;", "<b style='color:#DC3545'>")
                .replace("&lt;/b&gt;", "</b>");

        JLabel lblBody = new JLabel(
                "<html><div style='width:" + NOTIF_TEXT_WIDTH
                + "px; color:#444444; font-size:13px; line-height:1.65;'>"
                + bodyHtml
                + "</div></html>"
        );

        lblBody.setFont(new Font("Segoe UI", Font.PLAIN, 12));

        gc.gridx = 0;
        gc.gridy = 1;
        gc.gridwidth = 2;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 5, 0);

        textPanel.add(lblBody, gc);

        JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        timeRow.setOpaque(false);

        ImageIcon histIcon = IconHelper.history(12);

        if (histIcon != null) {
            timeRow.add(new JLabel(histIcon));
        }

        JLabel lblTime = new JLabel(n.time);
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTime.setForeground(new Color(163, 174, 208));

        timeRow.add(lblTime);

        gc.gridx = 0;
        gc.gridy = 2;
        gc.gridwidth = 2;
        gc.weightx = 1.0;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 0);

        textPanel.add(timeRow, gc);

        content.add(textPanel, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);

        Color hoverBg = new Color(
                Math.max(bgCard.getRed() - 8, 0),
                Math.max(bgCard.getGreen() - 8, 0),
                Math.max(bgCard.getBlue() - 8, 0)
        );

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(hoverBg);
                content.setBackground(hoverBg);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(bgCard);
                content.setBackground(bgCard);
            }
        });

        return card;
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }

        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public static class NotifItem {

        public enum Type {
            DANGER, WARNING, INFO
        }

        public final String key;
        public Type type;
        public String title;
        public String body;
        public String time;
        public int remindCount;
        public boolean urgentManagerAlert;

        public NotifItem(
                String key,
                Type type,
                String title,
                String body,
                String time,
                boolean urgentManagerAlert
        ) {
            this.key = key == null ? title : key;
            this.type = type;
            this.title = title;
            this.body = body;
            this.time = time;
            this.remindCount = 1;
            this.urgentManagerAlert = urgentManagerAlert;
        }
    }
}
