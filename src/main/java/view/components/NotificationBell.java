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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import business.service.SessionManager;
import view.components.IconHelper;

/**
 * Hộp thư thông báo ở góc trên phải.
 *
 * Chức năng: - Hiển thị cảnh báo tồn kho tự động. - Hiển thị cảnh báo nhập hàng
 * do Manager/Staff gửi cho Kho. - Cảnh báo Manager/Staff được lưu DB nên không
 * mất khi tắt app. - Click vào thông báo có mã SP sẽ gọi callback để mở đúng
 * sản phẩm bên InventoryView.
 */
public class NotificationBell extends JPanel {

    public static final int THRESHOLD_DANGER = 5;
    public static final int THRESHOLD_WARNING = 20;

    private final List<NotifItem> notifications = new ArrayList<>();
    private final JLabel lblBell;
    private final JLabel lblBadge;
    private int unreadCount = 0;

    private JWindow popup;
    private final JPanel popupList;

    // Không để chuông thông báo query DB trên Event Dispatch Thread.
    // Các event tồn kho có thể bắn dồn dập, nên debounce + chạy nền để tránh trắng màn hình/đơ UI.
    private Timer refreshDebounceTimer;
    private SwingWorker<RefreshResult, Void> refreshWorker;
    private boolean pendingIncreaseUnread = false;
    private boolean refreshAgainAfterWorker = false;
    private AWTEventListener popupOutsideClickListener;

    private static final Color COLOR_DANGER = new Color(220, 53, 69);
    private static final Color COLOR_WARNING = new Color(255, 152, 0);
    private static final Color COLOR_INFO = new Color(67, 97, 238);
    private static final Color COLOR_BG = new Color(255, 255, 255);

    private static final int POPUP_WIDTH = 720;
    private static final int POPUP_HEIGHT = 560;
    private static final int NOTIF_TEXT_WIDTH = 545;

    public enum Audience {
        WAREHOUSE, MANAGER, SALE, ALL
    }

    private final Audience audience;

    /**
     * Callback dùng để màn cha xử lý khi bấm vào thông báo có productId. Ví dụ
     * WarehouseDashboardView sẽ mở InventoryView và focus đúng sản phẩm.
     */
    private java.util.function.Consumer<String> productClickListener;

    public NotificationBell(Audience audience) {
        this.audience = audience;

        setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        setOpaque(false);

        lblBell = new JLabel();
        ImageIcon bellIcon = IconHelper.notification(22);
        if (bellIcon != null) {
            lblBell.setIcon(bellIcon);
        } else {
            lblBell.setText("!");
            lblBell.setFont(new Font("Segoe UI", Font.BOLD, 18));
            lblBell.setForeground(COLOR_INFO);
        }
        lblBell.setHorizontalAlignment(SwingConstants.CENTER);
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
            if (event == null || event.getType() == null) {
                return;
            }

            if (event.getType() == AppEventType.INVENTORY_ALERT) {
                SwingUtilities.invokeLater(() -> {
                    handleInventoryAlert(event.getMessage());
                    requestRefresh(false);
                });
                return;
            }

            if (event.getType() == AppEventType.INVENTORY
                    || event.getType() == AppEventType.PRODUCTS
                    || event.getType() == AppEventType.ORDERS) {
                requestRefresh(false);
            }
        });

        requestRefresh(false);
    }

    private String currentStoreIdOrNull() {
        try {
            if (SessionManager.isAdmin()) {
                return null;
            }

            String storeId = SessionManager.getCurrentStoreId();

            if (storeId == null || storeId.trim().isEmpty()) {
                return null;
            }

            return storeId.trim();

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
            Product p = ProductsSql.getInstance().findByIdInStore(productId.trim(), storeId);
            return p != null;
        } catch (Exception e) {
            return false;
        }
    }

    public void setProductClickListener(java.util.function.Consumer<String> productClickListener) {
        this.productClickListener = productClickListener;
    }

    /**
     * API cũ còn được các màn khác gọi. Không query trực tiếp ở đây nữa,
     * chỉ gom request để chạy nền.
     */
    private void refreshPersistentWarehouseAlerts(boolean increaseUnread) {
        requestRefresh(increaseUnread);
    }

    /**
     * API cũ còn được các màn khác gọi. Không query trực tiếp ở đây nữa,
     * chỉ gom request để chạy nền.
     */
    public void checkLowStock() {
        requestRefresh(false);
    }

    private void requestRefresh(boolean increaseUnread) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> requestRefresh(increaseUnread));
            return;
        }

        pendingIncreaseUnread = pendingIncreaseUnread || increaseUnread;

        if (refreshDebounceTimer == null) {
            refreshDebounceTimer = new Timer(350, e -> startBackgroundRefresh());
            refreshDebounceTimer.setRepeats(false);
        }

        refreshDebounceTimer.restart();
    }

    private void startBackgroundRefresh() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::startBackgroundRefresh);
            return;
        }

        if (refreshWorker != null && !refreshWorker.isDone()) {
            refreshAgainAfterWorker = true;
            return;
        }

        boolean increaseUnread = pendingIncreaseUnread;
        pendingIncreaseUnread = false;

        refreshWorker = new SwingWorker<>() {
            @Override
            protected RefreshResult doInBackground() {
                return buildRefreshResult();
            }

            @Override
            protected void done() {
                try {
                    RefreshResult result = get();
                    applyRefreshResult(result, increaseUnread);
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (refreshAgainAfterWorker) {
                        refreshAgainAfterWorker = false;
                        requestRefresh(false);
                    }
                }
            }
        };

        refreshWorker.execute();
    }

    private RefreshResult buildRefreshResult() {
        RefreshResult result = new RefreshResult();

        String storeId = currentStoreIdOrNull();
        String storeKey = currentStoreKey();
        Map<String, Integer> qtyByProduct = new HashMap<>();

        try {
            List<Product> products = storeId == null
                    ? ProductsSql.getInstance().selectAll()
                    : ProductsSql.getInstance().selectAllByStore(storeId);

            for (Product p : products) {
                if (p == null) {
                    continue;
                }

                String productId = safe(p.getProductId()).trim();
                String productName = safe(p.getProductName()).trim();
                int qty = p.getQuantity();

                if (productId.isBlank()) {
                    continue;
                }

                qtyByProduct.put(productId.toUpperCase(), qty);

                NotifItem auto = createAutoStockItem(storeKey, productId, productName, qty);
                if (auto != null) {
                    result.items.add(auto);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        if (audience == Audience.WAREHOUSE || audience == Audience.ALL) {
            try {
                // Dọn PENDING đã hết cảnh báo ở DB bằng 1 câu SQL duy nhất, không resolve từng sản phẩm trên EDT.
                InventoryNotificationSql.getInstance()
                        .resolveRecoveredPendingNotificationsByStore(storeId, THRESHOLD_WARNING);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            try {
                List<?> rawList = storeId == null
                        ? InventoryNotificationSql.getInstance().getPendingWarehouseAlerts()
                        : InventoryNotificationSql.getInstance().getPendingWarehouseAlertsByStore(storeId);

                for (Object raw : rawList) {
                    String productId = readStringField(raw, "productId");
                    String productName = readStringField(raw, "productName");
                    String message = readStringField(raw, "message");
                    String rawStoreId = readStringField(raw, "storeId");
                    int remindCount = readIntField(raw, "remindCount", 1);
                    int currentQty = readIntField(raw, "currentQuantity", Integer.MIN_VALUE);

                    if (productId == null || productId.isBlank()) {
                        continue;
                    }

                    productId = productId.trim();
                    String productKey = productId.toUpperCase();

                    if (currentQty == Integer.MIN_VALUE) {
                        currentQty = qtyByProduct.getOrDefault(productKey, Integer.MIN_VALUE);
                    }

                    if (currentQty != Integer.MIN_VALUE && currentQty > THRESHOLD_WARNING) {
                        result.productIdsToResolve.add(productId);
                        continue;
                    }

                    if (productName == null || productName.isBlank()) {
                        productName = "Sản phẩm";
                    }

                    String keyStore = (rawStoreId == null || rawStoreId.isBlank()) ? storeKey : rawStoreId.trim();
                    String key = "MANAGER_STOCK_ALERT_STORE_" + keyStore + "_" + productId;
                    String title = "Nhắc nhập hàng: " + productName;
                    String body = "Mã sản phẩm [" + productId + "]. " + (message == null ? "" : message);

                    if (remindCount >= 2) {
                        body += "<br><br><b style='color:#DC3545'>"
                                + "Đã nhắc " + remindCount
                                + " lần. Thông báo này sẽ chỉ mất khi sản phẩm được nhập thêm."
                                + "</b>";
                    }

                    NotifItem item = new NotifItem(
                            key,
                            NotifItem.Type.DANGER,
                            title,
                            body,
                            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                            true
                    );
                    item.remindCount = remindCount;
                    result.items.add(item);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (!result.productIdsToResolve.isEmpty()) {
            for (String productId : result.productIdsToResolve) {
                try {
                    InventoryNotificationSql.getInstance().resolveByProductIdAndStore(productId, storeId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        return result;
    }

    private NotifItem createAutoStockItem(String storeKey, String productId, String productName, int qty) {
        NotifItem.Type type;
        String title;
        String body;
        Audience target;

        if (qty <= 0) {
            type = NotifItem.Type.DANGER;
            title = "Hết hàng: " + productName;
            body = "Sản phẩm [" + productId + "] đã hết hoàn toàn. Cần nhập khẩn!";
            target = Audience.ALL;
        } else if (qty <= THRESHOLD_DANGER) {
            type = NotifItem.Type.DANGER;
            title = "Sắp hết: " + productName;
            body = "Sản phẩm [" + productId + "] chỉ còn " + qty + " sản phẩm. Cần nhập ngay!";
            target = Audience.ALL;
        } else if (qty <= THRESHOLD_WARNING) {
            type = NotifItem.Type.WARNING;
            title = "Tồn kho thấp: " + productName;
            body = "Sản phẩm [" + productId + "] còn " + qty + " sản phẩm. Nên lên kế hoạch nhập thêm.";
            target = Audience.WAREHOUSE;
        } else {
            return null;
        }

        if (!isForCurrentAudience(target)) {
            return null;
        }

        String key = "AUTO_STORE_" + storeKey + "_" + productId;
        return new NotifItem(
                key,
                type,
                title,
                body,
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")),
                false
        );
    }

    private boolean isForCurrentAudience(Audience target) {
        return target == Audience.ALL || target == this.audience;
    }

    private void applyRefreshResult(RefreshResult result, boolean increaseUnread) {
        if (result == null) {
            return;
        }

        notifications.clear();
        notifications.addAll(result.items);

        if (increaseUnread) {
            unreadCount = Math.min(notifications.size(), unreadCount + 1);
        } else {
            clampUnreadCount();
        }

        updateBadge();

        if (popup != null && popup.isVisible()) {
            rebuildPopupList();
        }
    }

    private void resolveProductNotifications(String productId, String storeId) {
        if (productId == null || productId.trim().isEmpty()) {
            return;
        }

        String cleanProductId = productId.trim();
        removeProductNotificationsFromUI(cleanProductId, currentStoreKey());

        try {
            InventoryNotificationSql.getInstance()
                    .resolveByProductIdAndStore(cleanProductId, storeId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        clampUnreadCount();
        updateBadge();
        if (popup != null && popup.isVisible()) {
            rebuildPopupList();
        }
    }

    private void removeProductNotificationsFromUI(String productId, String storeKey) {
        if (productId == null || productId.trim().isEmpty()) {
            return;
        }

        String cleanProductId = productId.trim().toUpperCase();
        String cleanStoreKey = storeKey == null || storeKey.trim().isEmpty()
                ? currentStoreKey()
                : storeKey.trim();

        notifications.removeIf(n -> {
            if (n == null) {
                return false;
            }

            String key = n.key == null ? "" : n.key;
            String title = n.title == null ? "" : n.title;
            String body = n.body == null ? "" : n.body;
            String pid = n.productId == null ? "" : n.productId;

            boolean sameProduct
                    = cleanProductId.equalsIgnoreCase(pid)
                    || key.toUpperCase().contains(cleanProductId)
                    || title.toUpperCase().contains(cleanProductId)
                    || body.toUpperCase().contains(cleanProductId);

            boolean sameStore
                    = cleanStoreKey.equals("ALL")
                    || key.contains("_STORE_" + cleanStoreKey + "_")
                    || key.startsWith("AUTO_STORE_" + cleanStoreKey + "_");

            return sameProduct && sameStore;
        });
    }

    private void clampUnreadCount() {
        if (unreadCount < 0) {
            unreadCount = 0;
        }

        if (unreadCount > notifications.size()) {
            unreadCount = notifications.size();
        }
    }

    /**
     * Xử lý realtime message dạng: INVENTORY_ALERT:SP0000038:Cá hồi phi lê tươi
     * 200g:0
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

        if (!productBelongsToCurrentStore(productId)) {
            return;
        }

        String key = "MANAGER_STOCK_ALERT_STORE_" + currentStoreKey() + "_" + productId;
        String title = "Nhắc nhập hàng: " + productName;

        String body = "Có cảnh báo nhập hàng. "
                + "Sản phẩm [" + productId + "] hiện còn " + quantity
                + " sản phẩm. Cần kiểm tra và nhập hàng.";

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
        if (target != Audience.ALL && target != this.audience) {
            return;
        }

        String key = "AUTO_STORE_" + currentStoreKey() + "_" + title + "_"
                + extractProductIdFromText(title + " " + body);

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
        if (increaseUnread) {
            unreadCount++;
        }

        updateBadge();
        if (popup != null && popup.isVisible()) {
            rebuildPopupList();
        }
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
        if (target != Audience.ALL && target != this.audience) {
            return;
        }

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));

        for (int i = 0; i < notifications.size(); i++) {
            NotifItem n = notifications.get(i);

            if (n.key.equals(key)) {
                if (fixedRemindCount >= 0) {
                    n.remindCount = fixedRemindCount;
                } else if (increaseUnread) {
                    n.remindCount++;
                }

                n.type = type;
                n.title = title;
                n.time = time;
                n.urgentManagerAlert = urgentManagerAlert;
                n.body = body;
                n.productId = extractProductIdFromText(key + " " + title + " " + body);

                notifications.remove(i);
                notifications.add(0, n);

                if (increaseUnread) {
                    unreadCount++;
                }
                updateBadge();
                if (popup != null && popup.isVisible()) {
                    rebuildPopupList();
                }
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
        if (fixedRemindCount >= 0) {
            item.remindCount = fixedRemindCount;
        }

        notifications.add(0, item);
        if (increaseUnread) {
            unreadCount++;
        }

        updateBadge();
        if (popup != null && popup.isVisible()) {
            rebuildPopupList();
        }
    }

    private void updateBadge() {
        int displayCount = notifications.size();

        if (displayCount > 0) {
            lblBadge.setText(displayCount > 9 ? "9+" : String.valueOf(displayCount));
            lblBadge.setVisible(true);
        } else {
            lblBadge.setText("0");
            lblBadge.setVisible(false);
        }
    }

    private void togglePopup() {
        if (popup != null && popup.isVisible()) {
            hidePopup();
            return;
        }

        unreadCount = 0;
        updateBadge();
        showPopup();
        requestRefresh(false);
    }

    private void hidePopup() {
        if (popup != null) {
            popup.setVisible(false);
            popup.dispose();
            popup = null;
        }
        uninstallOutsideClickListener();
    }

    private void installOutsideClickListener() {
        uninstallOutsideClickListener();

        popupOutsideClickListener = event -> {
            if (!(event instanceof MouseEvent me)) {
                return;
            }

            if (me.getID() != MouseEvent.MOUSE_PRESSED) {
                return;
            }

            if (popup == null || !popup.isVisible()) {
                uninstallOutsideClickListener();
                return;
            }

            Point p = me.getLocationOnScreen();
            if (!popup.getBounds().contains(p)) {
                hidePopup();
            }
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(
                popupOutsideClickListener,
                AWTEvent.MOUSE_EVENT_MASK
        );
    }

    private void uninstallOutsideClickListener() {
        if (popupOutsideClickListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(popupOutsideClickListener);
            popupOutsideClickListener = null;
        }
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

        ImageIcon bellIcon = IconHelper.notification(18);

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

        // Không xóa cưỡng bức lời nhắc Manager/Staff: chỉ làm mới để dọn các thông báo đã nhập đủ hàng.
        btnClear.setText("  Làm mới  ");
        btnClear.addActionListener(e -> {
            unreadCount = 0;
            updateBadge();
            requestRefresh(false);
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

        installOutsideClickListener();
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
            JLabel lblIcon = checkIcon != null ? new JLabel(checkIcon) : new JLabel();

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

        String bodyHtml = toAllowedHtml(n.body);

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

        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        content.setCursor(new Cursor(Cursor.HAND_CURSOR));
        textPanel.setCursor(new Cursor(Cursor.HAND_CURSOR));

        MouseAdapter clickAdapter = new MouseAdapter() {
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

            @Override
            public void mouseClicked(MouseEvent e) {
                handleNotificationClick(n);
            }
        };

        attachClickListenerRecursive(card, clickAdapter);

        return card;
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
        if (n == null) {
            return;
        }

        String targetProductId = n.productId;

        if (targetProductId == null || targetProductId.isBlank()) {
            targetProductId = extractProductIdFromNotification(n);
        }

        if (targetProductId == null || targetProductId.isBlank()) {
            targetProductId = extractProductIdFromText(
                    (n.key == null ? "" : n.key) + " "
                    + (n.title == null ? "" : n.title) + " "
                    + (n.body == null ? "" : n.body)
            );
        }

        if (targetProductId == null || targetProductId.isBlank()) {
            JOptionPane.showMessageDialog(
                    NotificationBell.this,
                    "Không tìm thấy mã sản phẩm trong thông báo này.",
                    "Không thể mở sản phẩm",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        if (popup != null) {
            popup.setVisible(false);
        }

        if (productClickListener == null) {
            JOptionPane.showMessageDialog(
                    NotificationBell.this,
                    "Màn hiện tại chưa gắn xử lý mở sản phẩm từ thông báo.",
                    "Không thể mở sản phẩm",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        productClickListener.accept(targetProductId.trim().toUpperCase());
    }

    private String extractProductIdFromNotification(NotifItem n) {
        if (n == null) {
            return "";
        }

        String combined = n.key + " " + n.title + " " + n.body;

        return extractProductIdFromText(combined);
    }

    private static String extractProductIdFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        /*
         * Hỗ trợ các mã:
         * - SP000038
         * - SP0000001
         * - SP + 5..10 chữ số
         */
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("\\b(SP\\d{5,10})\\b", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(text);

        if (matcher.find()) {
            return matcher.group(1).toUpperCase();
        }

        return "";
    }

    private String toAllowedHtml(String input) {
        if (input == null) {
            return "";
        }

        String escaped = escapeHtml(input);

        escaped = escaped
                .replace("&lt;br&gt;", "<br>")
                .replace("&lt;br/&gt;", "<br>")
                .replace("&lt;br /&gt;", "<br>")
                .replace("&lt;b style=&#39;color:#DC3545&#39;&gt;", "<b style='color:#DC3545'>")
                .replace("&lt;b style=&quot;color:#DC3545&quot;&gt;", "<b style='color:#DC3545'>")
                .replace("&lt;/b&gt;", "</b>");

        return escaped;
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

    private static class RefreshResult {
        final List<NotifItem> items = new ArrayList<>();
        final List<String> productIdsToResolve = new ArrayList<>();
    }

    private static class NotifItem {

        enum Type {
            DANGER,
            WARNING,
            INFO
        }

        final String key;
        Type type;
        String title;
        String body;
        String time;
        int remindCount = 1;
        boolean urgentManagerAlert;
        String productId;

        NotifItem(
                String key,
                Type type,
                String title,
                String body,
                String time,
                boolean urgentManagerAlert
        ) {
            this.key = key == null ? title : key;
            this.type = type;
            this.title = title == null ? "" : title;
            this.body = body == null ? "" : body;
            this.time = time == null ? "" : time;
            this.urgentManagerAlert = urgentManagerAlert;
            this.productId = extractProductIdFromText(this.key + " " + this.title + " " + this.body);
        }
    }
}
