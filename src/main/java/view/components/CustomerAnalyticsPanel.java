package view.components;

import view.components.IconHelper;
import business.service.AuthorizationService;
import business.sql.sales_order.CustomersSql;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.order.Customer;
import business.service.SessionManager;
import view.components.AnimatedRevealPanel;

public class CustomerAnalyticsPanel extends JPanel {

    private static final Color BG = new Color(243, 245, 250);
    private static final Color WHITE = Color.WHITE;
    private static final Color TEXT_DARK = new Color(43, 54, 116);
    private static final Color TEXT_GRAY = new Color(130, 140, 160);
    private static final Color BORDER = new Color(226, 232, 240);

    private static final Color PRIMARY = new Color(67, 97, 238);
    private static final Color SUCCESS = new Color(0, 163, 108);
    private static final Color WARNING = new Color(255, 153, 0);
    private static final Color DANGER = new Color(220, 53, 69);
    private static final Color PURPLE = new Color(103, 58, 183);
    private static final Color CYAN = new Color(14, 165, 233);

    private final List<Customer> customers = new ArrayList<>();
    private int newCustomersThisMonth = 0;
    private String analyticsScopeLabel = "Toàn hệ thống";

    private final List<AnimatedRevealPanel> customerAnimations = new ArrayList<>();

    public CustomerAnalyticsPanel() {
        setLayout(new BorderLayout());
        setOpaque(false);

        if (!(AuthorizationService.isStoreManager() || AuthorizationService.isAdmin())) {
            setVisible(false);
            return;
        }

        loadCustomerAnalyticsData();
        buildUI();
    }

    private void loadCustomerAnalyticsData() {
        customers.clear();
        newCustomersThisMonth = 0;
        analyticsScopeLabel = "Toàn hệ thống";

        try {
            CustomersSql dao = CustomersSql.getInstance();

            boolean scoped = !SessionManager.isAdmin();
            String storeId = SessionManager.getCurrentStoreId();

            if (scoped && storeId != null && !storeId.trim().isEmpty()) {
                String sid = storeId.trim();

                List<Customer> list = dao.selectAllWithRankForStoreAnalytics(sid);

                if (list != null) {
                    customers.addAll(list);
                }

                newCustomersThisMonth = dao.countNewCustomersByFirstPurchaseInStoreThisMonth(sid);

                String storeName = SessionManager.getCurrentStoreName();
                analyticsScopeLabel = storeName != null && !storeName.trim().isEmpty()
                        ? storeName.trim()
                        : sid;
            } else {
                List<Customer> list = dao.selectAllWithRank();

                if (list != null) {
                    customers.addAll(list);
                }

                newCustomersThisMonth = dao.countNewCustomersThisMonthGlobal();
                analyticsScopeLabel = "Toàn hệ thống";
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JPanel animateCustomerPanel(Component child, int delayMs, int distance) {
        AnimatedRevealPanel animated = new AnimatedRevealPanel(child, 900, delayMs, distance);
        customerAnimations.add(animated);
        return animated;
    }

    private void restartCustomerAnimations() {
        SwingUtilities.invokeLater(() -> {
            for (AnimatedRevealPanel p : customerAnimations) {
                if (p != null && p.isShowing()) {
                    p.restartAnimation();
                }
            }
        });
    }

    private void buildUI() {
        JPanel root = new JPanel();
        root.setOpaque(false);
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        root.add(buildHeader());
        root.add(Box.createRigidArea(new Dimension(0, 14)));

        root.add(animateCustomerPanel(buildCustomerKpiPanel(), 0, 90));
        root.add(Box.createRigidArea(new Dimension(0, 16)));

        JPanel chartsRow = new JPanel(new GridLayout(1, 3, 16, 0));
        chartsRow.setOpaque(false);
        chartsRow.setPreferredSize(new Dimension(1000, 330));
        chartsRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 330));

        chartsRow.add(animateCustomerPanel(buildCustomerRankChartPanel(), 0, 150));
        chartsRow.add(animateCustomerPanel(buildTopSpendingCustomersPanel(), 140, 150));
        chartsRow.add(animateCustomerPanel(buildCustomerTrendChartPanel(), 280, 150));

        root.add(chartsRow);
        root.add(Box.createRigidArea(new Dimension(0, 16)));

        root.add(animateCustomerPanel(buildCustomerInsightPanel(), 420, 100));

        add(root, BorderLayout.CENTER);

        restartCustomerAnimations();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
        header.setPreferredSize(new Dimension(1000, 70));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Phân tích khách hàng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_DARK);

        JLabel subtitle = new JLabel(
                "Theo dõi hành vi chi tiêu, hạng thành viên và nhóm khách hàng tiềm năng | Phạm vi: "
                + analyticsScopeLabel
        );
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_GRAY);

        titleBox.add(title);
        titleBox.add(Box.createRigidArea(new Dimension(0, 6)));
        titleBox.add(subtitle);

        JButton btnAnalytics = new JButton("STORE ANALYTICS");
        btnAnalytics.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnAnalytics.setForeground(PRIMARY);
        btnAnalytics.setBackground(WHITE);
        btnAnalytics.setFocusPainted(false);
        btnAnalytics.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAnalytics.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(new Color(219, 225, 255), 14),
                new EmptyBorder(10, 18, 10, 18)
        ));

        header.add(titleBox, BorderLayout.WEST);
        header.add(btnAnalytics, BorderLayout.EAST);

        return header;
    }

    private JPanel buildCustomerKpiPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 5, 14, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 105));
        panel.setPreferredSize(new Dimension(1000, 105));

        int totalCustomers = customers.size();

        double totalSpending = 0;
        int normal = 0;
        int silver = 0;
        int gold = 0;
        int diamond = 0;

        for (Customer c : customers) {
            double spending = safeSpending(c);
            totalSpending += spending;

            String rank = normalizeRank(c.getMemberRank());

            if (isDiamond(rank)) {
                diamond++;
            } else if (isGold(rank)) {
                gold++;
            } else if (isSilver(rank)) {
                silver++;
            } else {
                normal++;
            }
        }

        double avgSpending = totalCustomers == 0 ? 0 : totalSpending / totalCustomers;

        panel.add(createKpiCard(
                "Tổng khách hàng",
                String.valueOf(totalCustomers),
                IconHelper.customer(24),
                PRIMARY
        ));

        panel.add(createKpiCard(
                "Khách mới tháng này",
                String.valueOf(newCustomersThisMonth),
                IconHelper.add(24),
                CYAN
        ));

        panel.add(createKpiCard(
                "Tổng chi tiêu",
                formatMoneyShort(totalSpending),
                IconHelper.revenue(24),
                SUCCESS
        ));

        panel.add(createKpiCard(
                "TB / khách hàng",
                formatMoneyShort(avgSpending),
                IconHelper.barChart(24),
                PURPLE
        ));

        panel.add(createKpiCard(
                "VIP/Vàng/Bạc/Thường",
                diamond + "/" + gold + "/" + silver + "/" + normal,
                IconHelper.pieChart(24),
                WARNING
        ));

        return panel;
    }

    private JPanel createKpiCard(String title, String value, ImageIcon iconImage, Color accent) {
        RoundedPanel card = new RoundedPanel(18, WHITE);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER, 18),
                new EmptyBorder(15, 16, 15, 16)
        ));

        JPanel iconWrap = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int size = 46;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;

                g2.setColor(accent);
                g2.fillRoundRect(x, y, size, size, 16, 16);

                g2.dispose();
                super.paintComponent(g);
            }
        };

        iconWrap.setOpaque(false);
        iconWrap.setPreferredSize(new Dimension(58, 64));

        JLabel icon = new JLabel();
        icon.setHorizontalAlignment(SwingConstants.CENTER);
        icon.setVerticalAlignment(SwingConstants.CENTER);

        if (iconImage != null) {
            icon.setIcon(new ImageIcon(
                    iconImage.getImage().getScaledInstance(22, 22, Image.SCALE_SMOOTH)
            ));
        }

        iconWrap.add(icon);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblTitle.setForeground(TEXT_GRAY);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblValue.setForeground(accent);
        lblValue.setAlignmentX(Component.LEFT_ALIGNMENT);

        text.add(Box.createVerticalGlue());
        text.add(lblTitle);
        text.add(Box.createRigidArea(new Dimension(0, 8)));
        text.add(lblValue);
        text.add(Box.createVerticalGlue());

        card.add(iconWrap, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildCustomerRankChartPanel() {
        RoundedPanel card = createChartCard("Phân bố hạng khách hàng");

        Map<String, Integer> rankMap = getRankDistribution();

        RankDonutChart chart = new RankDonutChart(rankMap);
        card.add(chart, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildTopSpendingCustomersPanel() {
        RoundedPanel card = createChartCard("Top khách hàng chi tiêu cao");

        List<Customer> top = new ArrayList<>(customers);
        top.sort(Comparator.comparingDouble(this::safeSpending).reversed());

        if (top.size() > 5) {
            top = top.subList(0, 5);
        }

        TopCustomerBarChart chart = new TopCustomerBarChart(top);
        card.add(chart, BorderLayout.CENTER);

        return card;
    }

    private JPanel buildCustomerTrendChartPanel() {
        RoundedPanel card = createChartCard("Xu hướng khách hàng mới");

        CustomerTrendLineChart chart = new CustomerTrendLineChart(customers.size());
        card.add(chart, BorderLayout.CENTER);

        return card;
    }

    private RoundedPanel createChartCard(String title) {
        RoundedPanel card = new RoundedPanel(18, WHITE);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER, 18),
                new EmptyBorder(16, 18, 16, 18)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(TEXT_DARK);

        card.add(lblTitle, BorderLayout.NORTH);

        return card;
    }

    private JPanel buildCustomerInsightPanel() {
        RoundedPanel wrapper = new RoundedPanel(20, WHITE);
        wrapper.setLayout(new BorderLayout(16, 0));
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER, 20),
                new EmptyBorder(18, 20, 18, 20)
        ));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 165));
        wrapper.setPreferredSize(new Dimension(1000, 165));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Gợi ý hành động chăm sóc khách hàng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_DARK);

        JLabel sub = new JLabel(
                "Các đề xuất dựa trên chi tiêu trong phạm vi: " + analyticsScopeLabel
        );
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(TEXT_GRAY);

        left.add(title);
        left.add(Box.createRigidArea(new Dimension(0, 6)));
        left.add(sub);

        JPanel insights = new JPanel(new GridLayout(2, 2, 12, 12));
        insights.setOpaque(false);

        insights.add(createInsightBox("[VIP] Chăm sóc khách giá trị cao", "Ưu tiên khuyến mãi riêng cho nhóm chi tiêu cao.", new Color(237, 242, 255)));
        insights.add(createInsightBox("[PROMO] Kích hoạt khách chưa chi tiêu", "Gửi ưu đãi cho khách tổng chi bằng 0.", new Color(255, 247, 230)));
        insights.add(createInsightBox("[RANK] Ưu đãi theo hạng", "Khách Vàng/Bạc nên có coupon định kỳ.", new Color(232, 245, 233)));
        insights.add(createInsightBox("[RETENTION] Giữ chân khách hàng", "Theo dõi khách chi cao nhưng lâu chưa quay lại.", new Color(255, 235, 238)));

        wrapper.add(left, BorderLayout.WEST);
        wrapper.add(insights, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createInsightBox(String title, String desc, Color bg) {
        RoundedPanel box = new RoundedPanel(16, bg);
        box.setLayout(new BorderLayout(0, 6));
        box.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel lblTitle = new JLabel("<html><div style='width:230px;'>" + escapeHtml(title) + "</div></html>");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTitle.setForeground(TEXT_DARK);

        JLabel lblDesc = new JLabel("<html><div style='width:230px; color:#555555; line-height:1.35;'>"
                + escapeHtml(desc)
                + "</div></html>");
        lblDesc.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblDesc.setForeground(TEXT_GRAY);

        box.add(lblTitle, BorderLayout.NORTH);
        box.add(lblDesc, BorderLayout.CENTER);

        return box;
    }

    private Map<String, Integer> getRankDistribution() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("Thường", 0);
        map.put("Bạc", 0);
        map.put("Vàng", 0);
        map.put("VIP", 0);

        for (Customer c : customers) {
            String rank = normalizeRank(c.getMemberRank());

            if (isDiamond(rank)) {
                map.put("VIP", map.get("VIP") + 1);
            } else if (isGold(rank)) {
                map.put("Vàng", map.get("Vàng") + 1);
            } else if (isSilver(rank)) {
                map.put("Bạc", map.get("Bạc") + 1);
            } else {
                map.put("Thường", map.get("Thường") + 1);
            }
        }

        return map;
    }

    private String normalizeRank(String rank) {
        if (rank == null || rank.trim().isEmpty() || rank.equalsIgnoreCase("null") || rank.equals("—")) {
            return "Thường";
        }

        return rank.trim();
    }

    private boolean isDiamond(String rank) {
        String value = rank.toLowerCase();
        return value.contains("kim") || value.contains("diamond") || value.contains("vip");
    }

    private boolean isGold(String rank) {
        String value = rank.toLowerCase();
        return value.contains("vàng") || value.contains("vang") || value.contains("gold");
    }

    private boolean isSilver(String rank) {
        String value = rank.toLowerCase();
        return value.contains("bạc") || value.contains("bac") || value.contains("silver");
    }

    private double safeSpending(Customer c) {
        if (c == null) {
            return 0;
        }

        return Math.max(0, c.getTotalSpending());
    }

    private String safeName(Customer c) {
        if (c == null || c.getCustomerName() == null || c.getCustomerName().trim().isEmpty()) {
            return "Khách hàng";
        }

        return c.getCustomerName().trim();
    }

    private String formatMoney(double amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setGroupingSeparator('.');

        DecimalFormat df = new DecimalFormat("#,###", symbols);
        return df.format(amount) + " VNĐ";
    }

    private String formatMoneyShort(double amount) {
        if (amount >= 1_000_000_000) {
            return String.format(Locale.US, "%.1fB", amount / 1_000_000_000);
        }

        if (amount >= 1_000_000) {
            return String.format(Locale.US, "%.1fM", amount / 1_000_000);
        }

        if (amount >= 1_000) {
            return String.format(Locale.US, "%.0fK", amount / 1_000);
        }

        return String.valueOf((long) amount);
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

    private class RankDonutChart extends JPanel {

        private final Map<String, Integer> data;
        private final Color[] colors = {
            new Color(3, 105, 161),
            new Color(71, 85, 105),
            new Color(245, 158, 11),
            new Color(109, 40, 217)
        };

        public RankDonutChart(Map<String, Integer> data) {
            this.data = data;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int total = 0;
            for (Integer v : data.values()) {
                total += v;
            }

            int size = Math.min(getWidth() - 40, getHeight() - 80);
            size = Math.max(size, 120);

            int x = 25;
            int y = 20;

            if (total == 0) {
                g2.setColor(TEXT_GRAY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g2.drawString("Chưa có dữ liệu khách hàng", 30, getHeight() / 2);
                g2.dispose();
                return;
            }

            double start = 90;
            int i = 0;

            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                double angle = entry.getValue() * 360.0 / total;

                g2.setColor(colors[i % colors.length]);
                g2.fill(new Arc2D.Double(x, y, size, size, start, -angle, Arc2D.PIE));

                start -= angle;
                i++;
            }

            int hole = (int) (size * 0.55);
            int hx = x + (size - hole) / 2;
            int hy = y + (size - hole) / 2;

            g2.setColor(WHITE);
            g2.fill(new Ellipse2D.Double(hx, hy, hole, hole));

            g2.setColor(TEXT_DARK);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 22));
            String totalText = String.valueOf(total);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(totalText, x + size / 2 - fm.stringWidth(totalText) / 2, y + size / 2 + 5);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.setColor(TEXT_GRAY);
            String label = "khách hàng";
            fm = g2.getFontMetrics();
            g2.drawString(label, x + size / 2 - fm.stringWidth(label) / 2, y + size / 2 + 24);

            drawLegend(g2, x + size + 24, y + 8);

            g2.dispose();
        }

        private void drawLegend(Graphics2D g2, int x, int y) {
            int i = 0;

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));

            for (Map.Entry<String, Integer> entry : data.entrySet()) {
                g2.setColor(colors[i % colors.length]);
                g2.fillRoundRect(x, y + i * 28, 12, 12, 6, 6);

                g2.setColor(TEXT_DARK);
                g2.drawString(entry.getKey() + ": " + entry.getValue(), x + 20, y + 11 + i * 28);

                i++;
            }
        }
    }

    private class TopCustomerBarChart extends JPanel {

        private final List<Customer> topCustomers;

        public TopCustomerBarChart(List<Customer> topCustomers) {
            this.topCustomers = topCustomers;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (topCustomers == null || topCustomers.isEmpty()) {
                g2.setColor(TEXT_GRAY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
                g2.drawString("Chưa có dữ liệu chi tiêu.", 24, getHeight() / 2);
                g2.dispose();
                return;
            }

            double max = 1;

            for (Customer c : topCustomers) {
                max = Math.max(max, safeSpending(c));
            }

            int left = 24;
            int top = 26;
            int barX = 130;
            int barW = Math.max(120, getWidth() - barX - 98);
            int rowH = 42;

            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));

            for (int i = 0; i < topCustomers.size(); i++) {
                Customer c = topCustomers.get(i);
                double spending = safeSpending(c);

                int y = top + i * rowH;
                int width = (int) Math.round(spending / max * barW);

                g2.setColor(TEXT_DARK);
                g2.drawString(trimText(g2, safeName(c), 100), left, y + 18);

                g2.setColor(new Color(225, 232, 255));
                g2.fillRoundRect(barX, y + 4, barW, 16, 10, 10);

                g2.setColor(PRIMARY);
                g2.fillRoundRect(barX, y + 4, width, 16, 10, 10);

                g2.setColor(TEXT_GRAY);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.drawString(formatMoneyShort(spending), barX + barW + 10, y + 18);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            }

            g2.dispose();
        }

        private String trimText(Graphics2D g2, String text, int maxWidth) {
            if (text == null) {
                return "";
            }

            FontMetrics fm = g2.getFontMetrics();

            if (fm.stringWidth(text) <= maxWidth) {
                return text;
            }

            String ellipsis = "...";
            int n = text.length();

            while (n > 0 && fm.stringWidth(text.substring(0, n) + ellipsis) > maxWidth) {
                n--;
            }

            return n <= 0 ? ellipsis : text.substring(0, n) + ellipsis;
        }
    }

    private class CustomerTrendLineChart extends JPanel {

        private final int totalCustomers;

        public CustomerTrendLineChart(int totalCustomers) {
            this.totalCustomers = totalCustomers;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int x = 18;
            int y = 18;
            int w = getWidth() - 36;
            int h = getHeight() - 36;

            g2.setColor(new Color(20, 35, 52));
            g2.fillRoundRect(x, y, w, h, 20, 20);

            int chartX = x + 48;
            int chartY = y + 58;
            int chartW = w - 78;
            int chartH = h - 105;

            g2.setFont(new Font("Segoe UI", Font.BOLD, 13));
            g2.setColor(Color.WHITE);
            g2.drawString("New Customer Trend", x + 20, y + 28);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            g2.setColor(new Color(180, 195, 210));
            g2.drawString("Dữ liệu xu hướng sẽ chính xác hơn khi CUSTOMERS có created_at.", x + 20, y + 45);

            int[] values = buildDemoTrendValues(totalCustomers);
            String[] labels = {"T1", "T2", "T3", "T4", "T5", "T6"};

            int max = 1;
            for (int v : values) {
                max = Math.max(max, v);
            }

            for (int i = 0; i <= 4; i++) {
                int gy = chartY + chartH * i / 4;

                g2.setColor(new Color(55, 75, 95));
                g2.drawLine(chartX, gy, chartX + chartW, gy);

                g2.setColor(new Color(150, 165, 180));
                int label = max - max * i / 4;
                g2.drawString(String.valueOf(label), x + 18, gy + 4);
            }

            int n = values.length;
            int[] xs = new int[n];
            int[] ys = new int[n];

            for (int i = 0; i < n; i++) {
                xs[i] = chartX + i * chartW / (n - 1);
                ys[i] = chartY + chartH - values[i] * chartH / max;
            }

            g2.setStroke(new BasicStroke(8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(116, 95, 255, 60));
            drawSmoothLine(g2, xs, ys);

            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(116, 95, 255));
            drawSmoothLine(g2, xs, ys);

            for (int i = 0; i < n; i++) {
                g2.setColor(new Color(255, 92, 122));
                g2.fillOval(xs[i] - 5, ys[i] - 5, 10, 10);

                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f));
                g2.drawOval(xs[i] - 5, ys[i] - 5, 10, 10);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                g2.drawString(String.valueOf(values[i]), xs[i] - 5, ys[i] - 12);

                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(new Color(180, 195, 210));
                g2.drawString(labels[i], xs[i] - 8, chartY + chartH + 24);
            }

            g2.dispose();
        }

        private int[] buildDemoTrendValues(int total) {
            int base = Math.max(1, total / 6);

            return new int[]{
                Math.max(1, base - 1),
                Math.max(1, base),
                Math.max(1, base + 2),
                Math.max(1, base + 1),
                Math.max(1, base + 3),
                Math.max(1, base + 2)
            };
        }

        private void drawSmoothLine(Graphics2D g2, int[] xs, int[] ys) {
            if (xs.length == 1) {
                g2.drawLine(xs[0], ys[0], xs[0], ys[0]);
                return;
            }

            Path2D path = new Path2D.Double();
            path.moveTo(xs[0], ys[0]);

            for (int i = 0; i < xs.length - 1; i++) {
                int x1 = xs[i];
                int y1 = ys[i];
                int x2 = xs[i + 1];
                int y2 = ys[i + 1];

                int ctrlX1 = x1 + (x2 - x1) / 2;
                int ctrlY1 = y1;
                int ctrlX2 = x1 + (x2 - x1) / 2;
                int ctrlY2 = y2;

                path.curveTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, x2, y2);
            }

            g2.draw(path);
        }
    }

    class RoundedPanel extends JPanel {

        private final int radius;
        private final Color bgColor;

        public RoundedPanel(int radius, Color bgColor) {
            this.radius = radius;
            this.bgColor = bgColor;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    class RoundBorder implements javax.swing.border.Border {

        private final Color color;
        private final int radius;

        public RoundBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

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
