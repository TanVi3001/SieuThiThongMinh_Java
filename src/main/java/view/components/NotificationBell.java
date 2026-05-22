package view.components;

import business.service.SessionManager;
import business.sql.prod_inventory.InventoryNotificationSql;
import business.sql.prod_inventory.ProductsSql;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import model.product.Product;

/**
 * Hộp thư thông báo góc phải.
 *
 * Bản này cố định 2 lỗi chính:
 * - Badge/unread không tăng ảo khi refresh DB hoặc reload cảnh báo tồn kho.
 * - Thông báo kho được cô lập theo store_id hiện tại.
 */
public class NotificationBell extends JPanel {

    public static final int THRESHOLD_DANGER = 5;
    public static final int THRESHOLD_WARNING = 20;

    public enum Audience {
        WAREHOUSE, MANAGER, SALE, ALL
    }

    private static final Color COLOR_DANGER = new Color(220, 53, 69);
    private static final Color COLOR_WARNING = new Color(255, 152, 0);
    private static final Color COLOR_INFO = new Color(67, 97, 238);
    private static final Color COLOR_BG = Color.WHITE;
    private static final int POPUP_WIDTH = 720;
    private static final int POPUP_HEIGHT = 560;
    private static final int TEXT_WIDTH = 545;

    private final Audience audience;
    private final List<NotifItem> notifications = new ArrayList<>();
    private final JLabel lblBell;
    private final JLabel lblBadge;
    private final JPanel popupList;
    private int unreadCount = 0;
    private JWindow popup;
    private Consumer<String> productClickListener;

    public NotificationBell(Audience audience) {
        this.audience = audience == null ? Audience.ALL : audience;

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

        JLayeredPane layered = new JLayeredPane();
        layered.setPreferredSize(new Dimension(36, 36));
        layered.setOpaque(false);
        lblBell.setBounds(4, 4, 28, 28);
        lblBadge.setBounds(22, 0, 16, 16);
        layered.add(lblBell, JLayeredPane.DEFAULT_LAYER);
        layered.add(lblBadge, JLayeredPane.PALETTE_LAYER);
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
            if (event == null || event.getType() == null) {
                return;
            }

            if (event.getType() == AppEventType.INVENTORY_ALERT) {
                SwingUtilities.invokeLater(() -> {
                    handleInventoryAlert(event.getMessage());
                    refreshPersistentWarehouseAlerts(false);
                });
                return;
            }

            if (event.getType() == AppEventType.INVENTORY
                    || event.getType() == AppEventType.PRODUCTS
                    || event.getType() == AppEventType.ORDERS) {
                SwingUtilities.invokeLater(() -> {
                    checkLowStock();
                    refreshPersistentWarehouseAlerts(false);
                });
            }
        });

        SwingUtilities.invokeLater(() -> {
            checkLowStock();
            refreshPersistentWarehouseAlerts(false);
        });
    }

    public void setProductClickListener(Consumer<String> productClickListener) {
        this.productClickListener = productClickListener;
    }

    private String currentStoreIdOrNull() {
        try {
            if (SessionManager.isAdmin()) {
                return null;
            }
            String storeId = SessionManager.getCurrentStoreId();
            return storeId == null || storeId.trim().isEmpty() ? null : storeId.trim();
        } catch (Exception e) {
            return null;
        }
    }

    private String currentStoreKey() {
        String storeId = currentStoreIdOrNull();
        return storeId == null || storeId.trim().isEmpty() ? "ALL" : storeId.trim();
    }

    private boolean productBelongsToCurrentStore(String productId) {
        if (productId == null || productId.trim().isEmpty()) {
            return false;
        }
        String storeId = currentStoreIdOrNull();
        if (storeId == null) {
            return true;
        }
        try {
            return ProductsSql.getInstance().findByIdInStore(productId.trim(), storeId) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Load thông báo PENDING từ DB. Đây là refresh/sync nên không tăng unread.
     */
    private void refreshPersistentWarehouseAlerts(boolean increaseUnread) {
        if (audience != Audience.WAREHOUSE && audience != Audience.ALL) {
            return;
        }

        String storeKey = currentStoreKey();
        notifications.removeIf(n -> n.urgentManagerAlert
                && n.key.startsWith("MANAGER_STOCK_ALERT_")
                && n.key.contains("_STORE_" + storeKey + "_"));

        try {
            String storeId = currentStoreIdOrNull();
            List<?> rawList = storeId == null
                    ? InventoryNotificationSql.getInstance().getPendingWarehouseAlerts()
                    : InventoryNotificationSql.getInstance().getPendingWarehouseAlertsByStore(storeId);

            for (Object raw : rawList) {
                String productId = readStringField(raw, "productId");
                String productName = readStringField(raw, "productName");
                String message = readStringField(raw, "message");
                int remindCount = readIntField(raw, "remindCount", 1);

                if (productId == null || productId.isBlank()) {
                    continue;
                }
                if (productName == null || productName.isBlank()) {
                    productName = "Sản phẩm";
                }

                String key = "MANAGER_STOCK_ALERT_STORE_" + storeKey + "_" + productId;
                String title = "🚨 QUẢN LÝ/NHÂN VIÊN NHẮC NHẬP HÀNG: " + productName;
                String body = message == null ? "" : message;

                if (remindCount >= 2) {
                    body += "<br><br><b style='color:#DC3545'>Đã nhắc "
                            + remindCount
                            + " lần. Thông báo này sẽ chỉ mất khi sản phẩm được nhập thêm.</b>";
                }

                addOrUpdateNotification(
                        key,
                        NotifItem.Type.DANGER,
                        title,
                        body,
                        Audience.WAREHOUSE,
                        true,
                        increaseUnread,
                        remindCount
                );
            }

            updateBadge();
            rebuildPopupList();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Auto low-stock chỉ là trạng thái hiện tại, không tính vào unread khi reload.
     */
    public void checkLowStock() {
        try {
            String storeId = currentStoreIdOrNull();
            List<Product> products = storeId == null
                    ? ProductsSql.getInstance().selectAll()
                    : ProductsSql.getInstance().selectAllByStore(storeId);

            String storeKey = currentStoreKey();
            notifications.removeIf(n -> !n.urgentManagerAlert
                    && n.key.startsWith("AUTO_STORE_" + storeKey + "_"));

            for (Product p : products) {
                if (p == null) {
                    continue;
                }
                int qty = p.getQuantity();
                String productId = safe(p.getProductId());
                String name = safe(p.getProductName());

                if (qty <= 0) {
                    addNotification(
                            NotifItem.Type.DANGER,
                            "❌ HẾT HÀNG: " + name,
                            "Sản phẩm [" + productId + "] đã hết hoàn toàn. Cần nhập khẩn!",
                            Audience.ALL,
                            false
                    );
                } else if (qty <= THRESHOLD_DANGER) {
                    addNotification(
                            NotifItem.Type.DANGER,
                            "⚠️ SẮP HẾT: " + name,
                            "Sản phẩm [" + productId + "] chỉ còn " + qty + " sản phẩm. Cần nhập ngay!",
                            Audience.ALL,
                            false
                    );
                } else if (qty <= THRESHOLD_WARNING) {
                    addNotification(
                            NotifItem.Type.WARNING,
                            "📦 Tồn kho thấp: " + name,
                            "Sản phẩm [" + productId + "] còn " + qty + " sản phẩm. Nên lên kế hoạch nhập thêm.",
                            Audience.WAREHOUSE,
                            false
                    );
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

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
        if (!productBelongsToCurrentStore(productId)) {
            return;
        }

        String key = "MANAGER_STOCK_ALERT_STORE_" + currentStoreKey() + "_" + productId;
        String title = "🚨 QUẢN LÝ/NHÂN VIÊN NHẮC NHẬP HÀNG: " + productName;
        String body = "Có cảnh báo nhập hàng. Sản phẩm [" + productId
                + "] hiện còn " + quantity + " sản phẩm. Cần kiểm tra và nhập hàng.";

        addOrUpdateNotification(
                key,
                NotifItem.Type.DANGER,
                title,
                body,
                Audience.WAREHOUSE,
                true,
                true,
                -1
        );
    }

    public void addNotification(NotifItem.Type type, String title, String body, Audience target) {
        addNotification(type, title, body, target, true);
    }

    private void addNotification(NotifItem.Type type, String title, String body, Audience target, boolean increaseUnread) {
        if (target != Audience.ALL && target != audience) {
            return;
        }

        String key = "AUTO_STORE_" + currentStoreKey() + "_" + title + "_"
                + extractProductIdFromText(title + " " + body);

        boolean exists = notifications.stream().anyMatch(n -> n.key.equals(key));
        if (exists) {
            return;
        }

        NotifItem item = new NotifItem(key, type, title, body, now(), false);
        notifications.add(0, item);

        if (increaseUnread) {
            unreadCount++;
        }
        updateBadge();
        rebuildPopupList();
    }

    private void addOrUpdateNotification(
            String key,
            NotifItem.Type type,
            String title,
            String body,
            Audience target,
            boolean urgentManagerAlert,
            boolean increaseUnread,
            int fixedRemindCount
    ) {
        if (target != Audience.ALL && target != audience) {
            return;
        }

        for (int i = 0; i < notifications.size(); i++) {
            NotifItem n = notifications.get(i);
            if (n.key.equals(key)) {
                n.type = type;
                n.title = title;
                n.body = body;
                n.time = now();
                n.urgentManagerAlert = urgentManagerAlert;
                n.productId = extractProductIdFromText(key + " " + title + " " + body);

                if (fixedRemindCount >= 0) {
                    n.remindCount = fixedRemindCount;
                } else if (increaseUnread) {
                    n.remindCount++;
                }

                notifications.remove(i);
                notifications.add(0, n);

                if (increaseUnread) {
                    unreadCount++;
                }
                updateBadge();
                rebuildPopupList();
                return;
            }
        }

        NotifItem item = new NotifItem(key, type, title, body, now(), urgentManagerAlert);
        if (fixedRemindCount >= 0) {
            item.remindCount = fixedRemindCount;
        }
        notifications.add(0, item);

        if (increaseUnread) {
            unreadCount++;
        }
        updateBadge();
        rebuildPopupList();
    }

    private String now() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
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
        header.setBackground(COLOR_INFO);
        header.setBorder(new EmptyBorder(11, 16, 11, 12));

        JLabel lblTitle = new JLabel("Hộp thư thông báo");
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel lblCount = new JLabel("  " + notifications.size() + " thông báo  ");
        lblCount.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblCount.setForeground(Color.WHITE);
        lblCount.setBorder(new EmptyBorder(4, 10, 4, 10));

        JButton btnClear = new JButton("Xóa tất cả");
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClear.setForeground(Color.WHITE);
        btnClear.setBackground(COLOR_DANGER);
        btnClear.setFocusPainted(false);
        btnClear.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnClear.addActionListener(e -> {
            notifications.removeIf(n -> !n.urgentManagerAlert);
            unreadCount = 0;
            updateBadge();
            rebuildPopupList();
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.setOpaque(false);
        right.add(lblCount);
        right.add(btnClear);

        header.add(lblTitle, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        rebuildPopupList();

        JScrollPane scroll = new JScrollPane(popupList);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(POPUP_WIDTH, POPUP_HEIGHT));
        scroll.getVerticalScrollBar().setUnitIncrement(14);

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
            if (event instanceof MouseEvent me && me.getID() == MouseEvent.MOUSE_PRESSED) {
                if (popup != null && popup.isVisible()
                        && !popup.getBounds().contains(me.getLocationOnScreen())) {
                    popup.setVisible(false);
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK);
    }

    private void rebuildPopupList() {
        popupList.removeAll();
        popupList.setBackground(new Color(248, 249, 252));

        if (notifications.isEmpty()) {
            JPanel empty = new JPanel();
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            empty.setBackground(new Color(248, 249, 252));
            empty.setBorder(new EmptyBorder(30, 20, 30, 20));

            JLabel lblText = new JLabel("Không có thông báo mới");
            lblText.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblText.setForeground(new Color(163, 174, 208));
            lblText.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblSub = new JLabel("Hệ thống sẽ tự báo khi có sự kiện");
            lblSub.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            lblSub.setForeground(new Color(200, 210, 225));
            lblSub.setAlignmentX(Component.CENTER_ALIGNMENT);

            empty.add(Box.createRigidArea(new Dimension(0, 20)));
            empty.add(lblText);
            empty.add(Box.createRigidArea(new Dimension(0, 5)));
            empty.add(lblSub);
            popupList.add(empty);
        } else {
            for (int i = 0; i < notifications.size(); i++) {
                popupList.add(createNotifCard(notifications.get(i)));
                if (i < notifications.size() - 1) {
                    JSeparator sep = new JSeparator();
                    sep.setForeground(new Color(230, 235, 245));
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
        Color accent = switch (n.type) {
            case DANGER -> COLOR_DANGER;
            case WARNING -> COLOR_WARNING;
            default -> COLOR_INFO;
        };
        Color bg = switch (n.type) {
            case DANGER -> new Color(255, 247, 247);
            case WARNING -> new Color(255, 251, 242);
            default -> new Color(246, 248, 255);
        };
        String tag = switch (n.type) {
            case DANGER -> "KHẨN";
            case WARNING -> "CẢNH BÁO";
            default -> "THÔNG TIN";
        };

        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(bg);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel stripe = new JPanel();
        stripe.setBackground(accent);
        stripe.setPreferredSize(new Dimension(5, 0));
        card.add(stripe, BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout(14, 0));
        content.setBackground(bg);
        content.setBorder(new EmptyBorder(16, 18, 16, 22));

        JLabel icon = new JLabel(n.type == NotifItem.Type.DANGER ? "🚨" : n.type == NotifItem.Type.WARNING ? "⚠️" : "ℹ️");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        icon.setPreferredSize(new Dimension(46, 46));
        content.add(icon, BorderLayout.WEST);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("<html><b>" + tag + "</b> &nbsp; " + escapeHtml(cleanTitle(n.title)) + "</html>");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(new Color(25, 35, 75));

        JLabel lblBody = new JLabel("<html><div style='width:" + TEXT_WIDTH + "px; line-height:1.65;'>"
                + toAllowedHtml(n.body) + "</div></html>");
        lblBody.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblBody.setForeground(new Color(68, 68, 68));

        JLabel lblTime = new JLabel("◷ " + safe(n.time));
        lblTime.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblTime.setForeground(new Color(163, 174, 208));

        text.add(lblTitle);
        text.add(Box.createVerticalStrut(6));
        text.add(lblBody);
        text.add(Box.createVerticalStrut(6));
        text.add(lblTime);

        content.add(text, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);

        MouseAdapter adapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleNotificationClick(n);
            }
        };
        attachClickListenerRecursive(card, adapter);

        return card;
    }

    private String cleanTitle(String title) {
        if (title == null) {
            return "";
        }
        String t = title.replaceAll("^[\\p{So}\\p{Sm}\\s]*[^:]+:\\s*", "").trim();
        return t.isEmpty() ? title : t;
    }

    private void attachClickListenerRecursive(Component component, MouseAdapter adapter) {
        if (component == null || adapter == null) {
            return;
        }
        component.setCursor(new Cursor(Cursor.HAND_CURSOR));
        component.addMouseListener(adapter);
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                attachClickListenerRecursive(child, adapter);
            }
        }
    }

    private void handleNotificationClick(NotifItem n) {
        if (productClickListener == null) {
            return;
        }
        String productId = n.productId;
        if (productId == null || productId.isBlank()) {
            productId = extractProductIdFromNotification(n);
        }
        if (productId == null || productId.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không tìm thấy mã sản phẩm trong thông báo này.",
                    "Không thể mở sản phẩm",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        if (popup != null) {
            popup.setVisible(false);
        }
        productClickListener.accept(productId);
    }

    private String extractProductIdFromNotification(NotifItem n) {
        if (n == null) {
            return "";
        }
        return extractProductIdFromText(n.key + " " + n.title + " " + n.body);
    }

    private static String extractProductIdFromText(String text) {
        if (text == null) {
            return "";
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(SP\\d{7})").matcher(text);
        return m.find() ? m.group(1) : "";
    }

    private String toAllowedHtml(String input) {
        if (input == null) {
            return "";
        }
        String escaped = escapeHtml(input);
        return escaped
                .replace("&lt;br&gt;", "<br>")
                .replace("&lt;br/&gt;", "<br>")
                .replace("&lt;br /&gt;", "<br>")
                .replace("&lt;b style=&#39;color:#DC3545&#39;&gt;", "<b style='color:#DC3545'>")
                .replace("&lt;b style=&quot;color:#DC3545&quot;&gt;", "<b style='color:#DC3545'>")
                .replace("&lt;/b&gt;", "</b>");
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

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String readStringField(Object obj, String fieldName) {
        if (obj == null) {
            return "";
        }
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object v = f.get(obj);
            return v == null ? "" : String.valueOf(v);
        } catch (Exception e) {
            return "";
        }
    }

    private int readIntField(Object obj, String fieldName, int defaultValue) {
        if (obj == null) {
            return defaultValue;
        }
        try {
            java.lang.reflect.Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object v = f.get(obj);
            if (v == null) {
                return defaultValue;
            }
            if (v instanceof Number number) {
                return number.intValue();
            }
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception e) {
            return defaultValue;
        }
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
        public String productId;

        public NotifItem(String key, Type type, String title, String body, String time, boolean urgentManagerAlert) {
            this.key = key == null ? title : key;
            this.type = type;
            this.title = title == null ? "" : title;
            this.body = body == null ? "" : body;
            this.time = time == null ? "" : time;
            this.remindCount = 1;
            this.urgentManagerAlert = urgentManagerAlert;
            this.productId = extractProductIdFromText(this.key + " " + this.title + " " + this.body);
        }
    }
}
