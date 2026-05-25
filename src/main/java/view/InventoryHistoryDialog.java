package view;

import business.sql.prod_inventory.InventoryTransactionSql;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import view.components.IconHelper;

public class InventoryHistoryDialog extends JDialog {

    private JTable table;
    private DefaultTableModel model;
    private final List<ReceiptGroup> currentGroups = new ArrayList<>();

    private final String storeId;

    private final Color NAVY = new Color(23, 52, 99);
    private final Color BG = new Color(246, 247, 251);
    private final Color GREEN = new Color(10, 116, 103);
    private final Color BORDER = new Color(214, 222, 235);

    private static final int COL_TIME = 0;
    private static final int COL_RECEIPT = 1;
    private static final int COL_TYPE = 2;
    private static final int COL_ITEMS = 3;
    private static final int COL_TOTAL_QTY = 4;
    private static final int COL_TOTAL_IMPORT = 5;
    private static final int COL_TOTAL_VAT = 6;
    private static final int COL_TOTAL_AMOUNT = 7;
    private static final int COL_NOTE = 8;

    public InventoryHistoryDialog(Frame owner) {
        this(owner, null);
    }

    public InventoryHistoryDialog(Frame owner, String storeId) {
        super(owner, "Lịch sử biến động kho", true);
        this.storeId = normalizeStoreId(storeId);
        initUI();
        loadData();
    }

    private void initUI() {
        setResizable(true);
        setMinimumSize(new Dimension(1180, 720));

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int width = Math.min(1400, Math.max(1180, screen.width - 140));
        int height = Math.min(840, Math.max(720, screen.height - 120));
        setSize(width, height);
        setLocationRelativeTo(getOwner());

        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(24, 28, 24, 28));

        root.add(createHeaderPanel(), BorderLayout.NORTH);
        root.add(createCenterTablePanel(), BorderLayout.CENTER);
        root.add(createBottomPanel(), BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Lịch sử biến động kho");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(NAVY);

        JLabel sub = new JLabel(buildSubtitle());
        sub.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sub.setForeground(new Color(93, 110, 140));

        JLabel hint = new JLabel("Mỗi phiếu nhập được gom thành 1 đơn. Nhấp đúp vào dòng để mở phiếu nhập.");
        hint.setFont(new Font("Segoe UI", Font.BOLD, 12));
        hint.setForeground(GREEN);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(5));
        titleBox.add(sub);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(hint);

        header.add(titleBox, BorderLayout.WEST);
        return header;
    }

    private JPanel createCenterTablePanel() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundBorder(BORDER, 18),
                new EmptyBorder(14, 14, 14, 14)
        ));

        model = new DefaultTableModel(
                new Object[]{
                    "Thời gian",
                    "Phiếu nhập",
                    "Loại",
                    "Mặt hàng",
                    "Tổng SL",
                    "Tổng giá nhập",
                    "VAT",
                    "Tổng tiền",
                    "Ghi chú"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        table = new JTable(model);
        setupTableStyle();

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() >= 2 && SwingUtilities.isLeftMouseButton(e)) {
                    openSelectedReceipt();
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.getHorizontalScrollBar().setUnitIncrement(22);

        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBottomPanel() {
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);

        JButton btnOpenReceipt = createFooterButton("Mở phiếu nhập", IconHelper.view(20));
        btnOpenReceipt.setPreferredSize(new Dimension(175, 42));
        btnOpenReceipt.addActionListener(e -> openSelectedReceipt());

        JButton btnClose = createFooterButton("Đóng", IconHelper.close(18));
        btnClose.setPreferredSize(new Dimension(115, 42));
        btnClose.addActionListener(e -> dispose());

        bottom.add(btnOpenReceipt);
        bottom.add(btnClose);

        return bottom;
    }

    private JButton createFooterButton(String text, Icon icon) {
        JButton btn = new JButton(text);
        btn.setIcon(icon);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(new Color(15, 23, 42));
        btn.setBackground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void setupTableStyle() {
        table.setRowHeight(40);
        table.setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(238, 242, 247));
        table.getTableHeader().setForeground(new Color(15, 23, 42));
        table.getTableHeader().setReorderingAllowed(false);

        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(226, 232, 240));
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(NAVY);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);

        CenterRenderer centerRenderer = new CenterRenderer();

        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        setColumnWidth(COL_TIME, 145);
        setColumnWidth(COL_RECEIPT, 170);
        setColumnWidth(COL_TYPE, 115);
        setColumnWidth(COL_ITEMS, 330);
        setColumnWidth(COL_TOTAL_QTY, 95);
        setColumnWidth(COL_TOTAL_IMPORT, 145);
        setColumnWidth(COL_TOTAL_VAT, 120);
        setColumnWidth(COL_TOTAL_AMOUNT, 145);
        setColumnWidth(COL_NOTE, 360);
    }

    private void setColumnWidth(int col, int width) {
        table.getColumnModel().getColumn(col).setPreferredWidth(width);
        table.getColumnModel().getColumn(col).setMinWidth(Math.min(width, 80));
    }

    private String buildSubtitle() {
        String base = "Theo dõi nhập kho, xuất/hủy kho, phiếu nhập, giá nhập, VAT và tổng tiền.";
        if (storeId == null || storeId.isBlank()) {
            return base + " Phạm vi: Tất cả chi nhánh.";
        }
        return base + " Phạm vi chi nhánh: " + storeId + ".";
    }

    private void loadData() {
        model.setRowCount(0);
        currentGroups.clear();

        List<InventoryTransactionSql.InventoryTransactionDTO> list
                = InventoryTransactionSql.getInstance().getRecentTransactionsByStore(storeId, 300);

        if (list == null || list.isEmpty()) {
            model.addRow(new Object[]{
                "",
                "",
                "Chưa có dữ liệu",
                storeId == null
                ? "Hãy nhập kho hoặc xuất/hủy kho để phát sinh lịch sử."
                : "Chưa có lịch sử biến động cho chi nhánh " + storeId + ".",
                "",
                "",
                "",
                "",
                ""
            });
            return;
        }

        currentGroups.addAll(groupTransactions(list));

        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (ReceiptGroup group : currentGroups) {
            model.addRow(new Object[]{
                group.createdAt == null ? "" : fmt.format(group.createdAt),
                group.receiptId == null ? "" : group.receiptId,
                normalizeType(group.transactionType),
                group.itemSummary,
                group.totalQuantity,
                money(group.totalImportAmount),
                money(group.totalVatAmount),
                money(group.totalAmount),
                group.note == null ? "" : group.note
            });
        }
    }

    private List<ReceiptGroup> groupTransactions(List<InventoryTransactionSql.InventoryTransactionDTO> list) {
        Map<String, ReceiptGroup> map = new LinkedHashMap<>();
        int fallbackIndex = 0;

        for (InventoryTransactionSql.InventoryTransactionDTO x : list) {
            String receiptId = normalizeEmpty(x.receiptId);

            String key;
            if (receiptId != null) {
                key = "RECEIPT_" + receiptId;
            } else {
                key = "TX_" + (++fallbackIndex) + "_" + normalizeEmpty(x.transactionId);
            }

            ReceiptGroup group = map.get(key);
            if (group == null) {
                group = new ReceiptGroup();
                group.receiptId = receiptId;
                group.transactionType = x.transactionType;
                group.createdAt = x.createdAt;
                group.note = x.note;
                map.put(key, group);
            }

            group.lines.add(x);
            group.totalQuantity += Math.max(0, x.quantity);

            BigDecimal quantity = BigDecimal.valueOf(Math.max(0, x.quantity));

            if (x.unitImportPrice != null) {
                group.totalImportAmount = group.totalImportAmount.add(x.unitImportPrice.multiply(quantity));
            }

            if (x.vatAmount != null) {
                group.totalVatAmount = group.totalVatAmount.add(x.vatAmount);
            }

            if (x.totalAmount != null) {
                group.totalAmount = group.totalAmount.add(x.totalAmount);
            }

            if (group.createdAt == null || (x.createdAt != null && x.createdAt.before(group.createdAt))) {
                group.createdAt = x.createdAt;
            }
        }

        for (ReceiptGroup group : map.values()) {
            group.itemSummary = buildItemSummary(group.lines);
        }

        return new ArrayList<>(map.values());
    }

    private String buildItemSummary(List<InventoryTransactionSql.InventoryTransactionDTO> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }

        String firstName = lines.get(0).productName == null ? "" : lines.get(0).productName;
        if (lines.size() == 1) {
            return firstName;
        }

        return firstName + " + " + (lines.size() - 1) + " mặt hàng khác";
    }

    private void openSelectedReceipt() {
        ReceiptGroup group = getSelectedGroup();

        if (group == null) {
            return;
        }

        if (group.receiptId == null || group.receiptId.isBlank()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Dòng này chưa có phiếu nhập.\nHãy chọn dòng nhập kho có mã phiếu.",
                    "Chưa có phiếu nhập",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        new PurchaseReceiptInvoiceDialog(owner, group.receiptId).setVisible(true);

        loadData();
    }

    private ReceiptGroup getSelectedGroup() {
        int row = table.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một đơn nhập trong bảng.");
            return null;
        }

        int modelRow = table.convertRowIndexToModel(row);

        if (modelRow < 0 || modelRow >= currentGroups.size()) {
            JOptionPane.showMessageDialog(this, "Dòng này chưa có dữ liệu chi tiết.");
            return null;
        }

        ReceiptGroup group = currentGroups.get(modelRow);

        if (group == null || group.lines.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Dòng này chưa có dữ liệu chi tiết.");
            return null;
        }

        return group;
    }

    private String normalizeType(String type) {
        if ("INBOUND".equalsIgnoreCase(type)) {
            return "Nhập kho";
        }

        if ("OUTBOUND".equalsIgnoreCase(type)) {
            return "Xuất / Hủy";
        }

        return type == null ? "" : type;
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "";
        }

        return String.format("%,.0f", value);
    }

    private String normalizeEmpty(String value) {
        if (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim())) {
            return null;
        }
        return value.trim();
    }

    private String normalizeStoreId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        String text = value.trim();

        if ("Tất cả chi nhánh".equalsIgnoreCase(text)
                || "Chưa xác định".equalsIgnoreCase(text)) {
            return null;
        }

        if (text.contains(" - ")) {
            return text.substring(0, text.indexOf(" - ")).trim();
        }

        return text;
    }

    private static class ReceiptGroup {

        String receiptId;
        String transactionType;
        java.sql.Timestamp createdAt;
        String itemSummary;
        int totalQuantity;
        BigDecimal totalImportAmount = BigDecimal.ZERO;
        BigDecimal totalVatAmount = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        String note;
        List<InventoryTransactionSql.InventoryTransactionDTO> lines = new ArrayList<>();
    }

    class CenterRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            setHorizontalAlignment(JLabel.CENTER);
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(new Color(15, 23, 42));
            setBorder(new EmptyBorder(0, 8, 0, 8));

            if (isSelected) {
                setBackground(new Color(219, 234, 254));
                setForeground(NAVY);
            } else {
                setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
            }

            String text = value == null ? "" : value.toString();
            setText(text);
            setToolTipText(text);

            return this;
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
            g2.setStroke(new BasicStroke(1.25f));
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
