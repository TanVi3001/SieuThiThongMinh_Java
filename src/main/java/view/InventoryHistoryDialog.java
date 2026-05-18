package view;

import business.sql.prod_inventory.InventoryTransactionSql;
import java.awt.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

public class InventoryHistoryDialog extends JDialog {

    private JTable table;
    private DefaultTableModel model;

    private final Color NAVY = new Color(23, 52, 99);
    private final Color BG = new Color(246, 247, 251);

    public InventoryHistoryDialog(Frame owner) {
        super(owner, "Lịch sử biến động kho", true);
        initUI();
        loadData();
    }

    private void initUI() {
        setSize(1050, 620);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        getContentPane().setBackground(BG);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Lịch sử biến động kho");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(NAVY);

        JLabel sub = new JLabel("Theo dõi nhập kho, xuất/hủy kho, phiếu nhập, giá nhập, VAT và tổng tiền.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(new Color(111, 124, 149));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(sub);

        root.add(titleBox, BorderLayout.NORTH);

        model = new DefaultTableModel(
                new Object[]{
                    "Thời gian",
                    "Loại",
                    "Mã SP",
                    "Tên sản phẩm",
                    "SL",
                    "Đơn vị",
                    "Giá nhập",
                    "Giá bán",
                    "VAT",
                    "Tổng tiền",
                    "Phiếu",
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
        table.setRowHeight(34);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(245, 246, 250));
        table.setShowVerticalLines(false);
        table.setSelectionBackground(new Color(237, 242, 255));
        table.setSelectionForeground(NAVY);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        root.add(scroll, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.setOpaque(false);

        JButton btnClose = new JButton("Đóng");
        btnClose.setPreferredSize(new Dimension(100, 38));
        btnClose.addActionListener(e -> dispose());

        JButton btnViewReceipt = new JButton("Xem phiếu nhập");
        btnViewReceipt.setPreferredSize(new Dimension(150, 38));
        btnViewReceipt.addActionListener(e -> viewReceipt());

        bottom.add(btnViewReceipt);
        bottom.add(btnClose);

        root.add(bottom, BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);
    }

    private void loadData() {
        model.setRowCount(0);

        List<InventoryTransactionSql.InventoryTransactionDTO> list
                = InventoryTransactionSql.getInstance().getRecentTransactions(100);

        if (list == null || list.isEmpty()) {
            model.addRow(new Object[]{
                "",
                "Chưa có dữ liệu",
                "",
                "Hãy nhập kho hoặc xuất/hủy kho để phát sinh lịch sử.",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                ""
            });
            return;
        }

        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        for (InventoryTransactionSql.InventoryTransactionDTO x : list) {
            model.addRow(new Object[]{
                x.createdAt == null ? "" : fmt.format(x.createdAt),
                normalizeType(x.transactionType),
                x.productId,
                x.productName,
                x.quantity,
                x.unit,
                money(x.unitImportPrice),
                money(x.salePrice),
                x.vatRate == null ? "" : x.vatRate + "%",
                money(x.totalAmount),
                x.receiptId == null ? "" : x.receiptId,
                x.note == null ? "" : x.note
            });
        }
    }

    private void viewReceipt() {
        int row = table.getSelectedRow();

        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một dòng có phiếu nhập.");
            return;
        }

        int modelRow = table.convertRowIndexToModel(row);
        String receiptId = String.valueOf(model.getValueAt(modelRow, 10));

        if (receiptId == null
                || receiptId.trim().isEmpty()
                || "null".equalsIgnoreCase(receiptId)
                || "Chưa có dữ liệu".equalsIgnoreCase(String.valueOf(model.getValueAt(modelRow, 1)))) {

            JOptionPane.showMessageDialog(
                    this,
                    "Dòng này chưa có phiếu nhập.\n"
                    + "Hãy bấm Nhập Kho, lưu phiếu nhập trước rồi quay lại xem.",
                    "Chưa có phiếu nhập",
                    JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        new PurchaseReceiptInvoiceDialog(owner, receiptId).setVisible(true);

        loadData();
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
}
