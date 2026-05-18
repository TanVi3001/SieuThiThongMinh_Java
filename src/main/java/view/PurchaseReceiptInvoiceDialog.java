package view;

import business.service.InventoryPricePolicyService;
import business.sql.prod_inventory.InventoryTransactionSql;
import common.report.PurchaseReceiptReportService;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class PurchaseReceiptInvoiceDialog extends JDialog {

    private final String receiptId;

    private JTable table;
    private DefaultTableModel tableModel;

    private final Color NAVY = new Color(23, 52, 99);
    private final Color MUTED = new Color(111, 124, 149);
    private final Color BG = new Color(246, 247, 251);
    private final Color BORDER = new Color(232, 237, 245);
    private final Color BLUE = new Color(67, 97, 238);
    private final Color GREEN = new Color(0, 163, 108);
    private final Color RED = new Color(220, 53, 69);

    public PurchaseReceiptInvoiceDialog(Frame owner, String receiptId) {
        super(owner, "Phiếu nhập hàng", true);
        this.receiptId = receiptId;

        initUI();
    }

    private void initUI() {
        List<InventoryTransactionSql.PurchaseReceiptLineDTO> lines
                = InventoryTransactionSql.getInstance().getReceiptLines(receiptId);

        if (lines == null || lines.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không tìm thấy chi tiết phiếu nhập: " + receiptId,
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            dispose();
            return;
        }

        setSize(1320, 760);
        setMinimumSize(new Dimension(1180, 680));
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(22, 26, 22, 26));

        root.add(createHeader(lines.size()), BorderLayout.NORTH);
        root.add(createTableCard(lines), BorderLayout.CENTER);
        root.add(createBottomPanel(lines), BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);
    }

    private JPanel createHeader(int lineCount) {
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("PHIẾU NHẬP HÀNG");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(NAVY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "Mã phiếu: " + receiptId
                + "  •  Số dòng sản phẩm: " + lineCount
                + "  •  Giá nhập sau VAT phải nhỏ hơn giá bán"
        );
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(5));
        titleBox.add(subtitle);

        JLabel badge = new JLabel("PHIẾU NHẬP");
        badge.setOpaque(true);
        badge.setBackground(new Color(230, 245, 239));
        badge.setForeground(GREEN);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 13));
        badge.setHorizontalAlignment(SwingConstants.CENTER);
        badge.setPreferredSize(new Dimension(130, 34));
        badge.setBorder(BorderFactory.createLineBorder(new Color(194, 235, 220)));

        header.add(titleBox, BorderLayout.WEST);
        header.add(badge, BorderLayout.EAST);

        return header;
    }

    private JPanel createTableCard(List<InventoryTransactionSql.PurchaseReceiptLineDTO> lines) {
        JPanel card = new JPanel(new BorderLayout(0, 12));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(16, 16, 16, 16)
        ));

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setOpaque(false);

        JLabel title = new JLabel("Chi tiết hàng nhập");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(NAVY);

        JLabel note = new JLabel("Giá nhập là giá chưa VAT; hệ thống tự tính giá nhập sau VAT và thành tiền.");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        note.setForeground(MUTED);

        JPanel textBox = new JPanel();
        textBox.setOpaque(false);
        textBox.setLayout(new BoxLayout(textBox, BoxLayout.Y_AXIS));
        textBox.add(title);
        textBox.add(Box.createVerticalStrut(3));
        textBox.add(note);

        infoPanel.add(textBox, BorderLayout.WEST);

        tableModel = new DefaultTableModel(
                new Object[]{
                    "STT",
                    "Mã SP",
                    "Tên sản phẩm",
                    "SL",
                    "ĐV",
                    "Giá nhập",
                    "VAT",
                    "Nhập + VAT",
                    "Giá bán",
                    "Thành tiền"
                },
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int stt = 1;

        for (InventoryTransactionSql.PurchaseReceiptLineDTO line : lines) {
            BigDecimal importAfterVat = InventoryPricePolicyService.calculateImportPriceAfterVat(
                    line.unitImportPrice,
                    line.vatRate
            );

            tableModel.addRow(new Object[]{
                stt++,
                safe(line.productId, ""),
                safe(line.productName, ""),
                line.quantity,
                safe(line.unit, "Cái"),
                money(line.unitImportPrice),
                percent(line.vatRate),
                money(importAfterVat),
                money(line.salePrice),
                money(line.afterTax)
            });
        }

        table = new JTable(tableModel);
        styleTable();

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(245, 246, 250)));
        scrollPane.getViewport().setBackground(Color.WHITE);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(18);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        card.add(infoPanel, BorderLayout.NORTH);
        card.add(scrollPane, BorderLayout.CENTER);

        return card;
    }

    private JPanel createBottomPanel(List<InventoryTransactionSql.PurchaseReceiptLineDTO> lines) {
        JPanel bottom = new JPanel(new BorderLayout(16, 0));
        bottom.setOpaque(false);

        JPanel summaryCard = createSummaryCard(lines);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setOpaque(false);

        JButton btnPreview = createButton("Xuất phiếu nhập hàng", GREEN);
        JButton btnPrint = createButton("In phiếu", BLUE);
        JButton btnClose = createButton("Đóng", new Color(142, 153, 176));

        btnPreview.setPreferredSize(new Dimension(170, 40));
        btnPreview.addActionListener(e -> PurchaseReceiptReportService.showPurchaseReceipt(receiptId));
        btnPrint.addActionListener(e -> PurchaseReceiptReportService.printPurchaseReceipt(receiptId));
        btnClose.addActionListener(e -> dispose());

        buttons.add(btnPreview);
        buttons.add(btnPrint);
        buttons.add(btnClose);

        bottom.add(summaryCard, BorderLayout.CENTER);
        bottom.add(buttons, BorderLayout.SOUTH);

        return bottom;
    }

    private JPanel createSummaryCard(List<InventoryTransactionSql.PurchaseReceiptLineDTO> lines) {
        BigDecimal totalBeforeTax = BigDecimal.ZERO;
        BigDecimal totalTax = BigDecimal.ZERO;
        BigDecimal totalAfterTax = BigDecimal.ZERO;

        boolean hasInvalidProfit = false;

        for (InventoryTransactionSql.PurchaseReceiptLineDTO line : lines) {
            totalBeforeTax = totalBeforeTax.add(nullToZero(line.beforeTax));
            totalTax = totalTax.add(nullToZero(line.taxAmount));
            totalAfterTax = totalAfterTax.add(nullToZero(line.afterTax));

            BigDecimal importAfterVat = InventoryPricePolicyService.calculateImportPriceAfterVat(
                    line.unitImportPrice,
                    line.vatRate
            );

            if (line.salePrice != null && importAfterVat.compareTo(line.salePrice) >= 0) {
                hasInvalidProfit = true;
            }
        }

        JPanel summary = new JPanel(new BorderLayout(16, 0));
        summary.setBackground(Color.WHITE);
        summary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(14, 18, 14, 18)
        ));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Tổng kết phiếu nhập");
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setForeground(NAVY);

        JLabel logic = new JLabel(
                hasInvalidProfit
                        ? "Cảnh báo: Có sản phẩm có giá nhập sau VAT >= giá bán."
                        : "Logic giá hợp lệ: Giá nhập sau VAT nhỏ hơn giá bán."
        );
        logic.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logic.setForeground(hasInvalidProfit ? RED : GREEN);

        left.add(title);
        left.add(Box.createVerticalStrut(5));
        left.add(logic);

        JPanel right = new JPanel(new GridLayout(3, 1, 0, 6));
        right.setOpaque(false);
        right.add(totalLabel("Tổng trước thuế: " + money(totalBeforeTax) + " VNĐ"));
        right.add(totalLabel("Tổng VAT: " + money(totalTax) + " VNĐ"));
        right.add(totalLabel("Tổng sau thuế: " + money(totalAfterTax) + " VNĐ"));

        summary.add(left, BorderLayout.WEST);
        summary.add(right, BorderLayout.EAST);

        return summary;
    }

    private void styleTable() {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(38);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(NAVY);
        table.setGridColor(new Color(245, 246, 250));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(new Color(237, 242, 255));
        table.setSelectionForeground(NAVY);

        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(245, 246, 250));
        table.getTableHeader().setForeground(Color.BLACK);
        table.getTableHeader().setReorderingAllowed(false);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table,
                    Object value,
                    boolean isSelected,
                    boolean hasFocus,
                    int row,
                    int column
            ) {
                Component c = super.getTableCellRendererComponent(
                        table,
                        value,
                        isSelected,
                        hasFocus,
                        row,
                        column
                );

                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(NAVY);
                } else {
                    c.setBackground(Color.WHITE);
                    c.setForeground(NAVY);
                }

                if (c instanceof JLabel lbl) {
                    lbl.setBorder(new EmptyBorder(0, 8, 0, 8));

                    if (column == 0 || column == 3 || column == 4 || column == 6) {
                        lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    } else if (column >= 5) {
                        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                    } else {
                        lbl.setHorizontalAlignment(SwingConstants.LEFT);
                    }

                    if (column == 9) {
                        lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    } else {
                        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    }
                }

                return c;
            }
        });

        table.getColumnModel().getColumn(0).setPreferredWidth(45);
        table.getColumnModel().getColumn(1).setPreferredWidth(105);
        table.getColumnModel().getColumn(2).setPreferredWidth(320);
        table.getColumnModel().getColumn(3).setPreferredWidth(55);
        table.getColumnModel().getColumn(4).setPreferredWidth(60);
        table.getColumnModel().getColumn(5).setPreferredWidth(130);
        table.getColumnModel().getColumn(6).setPreferredWidth(65);
        table.getColumnModel().getColumn(7).setPreferredWidth(135);
        table.getColumnModel().getColumn(8).setPreferredWidth(120);
        table.getColumnModel().getColumn(9).setPreferredWidth(135);
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(120, 40));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private JLabel totalLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 15));
        label.setForeground(NAVY);
        label.setHorizontalAlignment(SwingConstants.RIGHT);
        return label;
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "0";
        }

        return String.format("%,.0f", value);
    }

    private String percent(BigDecimal value) {
        if (value == null) {
            return "0%";
        }

        return value.stripTrailingZeros().toPlainString() + "%";
    }

    private String safe(String s, String fallback) {
        return s == null || s.trim().isEmpty() ? fallback : s.trim();
    }
}
