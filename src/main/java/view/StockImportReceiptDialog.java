package view;

import business.sql.prod_inventory.InventoryTransactionSql;
import business.sql.prod_inventory.ProductsSql;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.product.Product;

public class StockImportReceiptDialog extends JDialog {

    private final String productId;
    private final Runnable onSuccess;

    private Product product;

    private JLabel lblProductInfo;
    private JLabel lblSalePrice;
    private JLabel lblBeforeTax;
    private JLabel lblTaxAmount;
    private JLabel lblAfterTax;
    private JLabel lblProfitHint;

    private JTextField txtQuantity;
    private JTextField txtImportPrice;
    private JTextField txtSupplier;
    private JTextField txtNote;
    private JComboBox<String> cbVat;

    private final Color NAVY = new Color(23, 52, 99);
    private final Color MUTED = new Color(111, 124, 149);
    private final Color BLUE = new Color(67, 97, 238);
    private final Color GREEN = new Color(0, 163, 108);
    private final Color RED = new Color(220, 53, 69);
    private final Color BORDER = new Color(232, 237, 245);

    public StockImportReceiptDialog(Frame owner, String productId, Runnable onSuccess) {
        super(owner, "Phiếu nhập kho", true);
        this.productId = productId;
        this.onSuccess = onSuccess;

        loadProduct();
        initUI();
        bindEvents();
        recalc();
    }

    private void loadProduct() {
        product = ProductsSql.getInstance().findById(productId);

        if (product == null) {
            throw new IllegalArgumentException("Không tìm thấy sản phẩm: " + productId);
        }
    }

    private void initUI() {
        setSize(560, 620);
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(246, 247, 251));

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(new Color(246, 247, 251));
        root.setBorder(new EmptyBorder(22, 24, 22, 24));

        JLabel title = new JLabel("Tạo phiếu nhập hàng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(NAVY);

        JLabel sub = new JLabel("Nhập giá vốn, VAT và số lượng để cập nhật tồn kho.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(MUTED);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(sub);

        root.add(titleBox, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(Color.WHITE);
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(20, 22, 20, 22)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        lblProductInfo = new JLabel(product.getProductName() + " (" + product.getProductId() + ")");
        lblProductInfo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblProductInfo.setForeground(NAVY);

        lblSalePrice = new JLabel("Giá bán hiện tại: " + money(product.getBasePrice()) + " VNĐ");
        lblSalePrice.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSalePrice.setForeground(GREEN);

        addRow(form, gbc, 0, "Sản phẩm", lblProductInfo);
        addRow(form, gbc, 2, "Giá bán", lblSalePrice);

        txtQuantity = createTextField("VD: 20");
        txtImportPrice = createTextField("Giá nhập / 1 sản phẩm");
        txtSupplier = createTextField("SUP001");
        txtNote = createTextField("VD: Nhập bổ sung theo cảnh báo tồn kho");

        cbVat = new JComboBox<>(new String[]{"0%", "5%", "8%", "10%"});
        cbVat.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbVat.setBackground(Color.WHITE);
        cbVat.setPreferredSize(new Dimension(0, 38));

        addRow(form, gbc, 4, "Số lượng nhập (*)", txtQuantity);
        addRow(form, gbc, 6, "Giá nhập / đơn vị (*)", txtImportPrice);
        addRow(form, gbc, 8, "Thuế VAT", cbVat);
        addRow(form, gbc, 10, "Nhà cung cấp", txtSupplier);
        addRow(form, gbc, 12, "Ghi chú", txtNote);

        JPanel summary = new JPanel(new GridLayout(4, 1, 0, 6));
        summary.setOpaque(false);
        summary.setBorder(new EmptyBorder(12, 0, 0, 0));

        lblBeforeTax = createSummaryLabel("Tiền hàng trước thuế: 0");
        lblTaxAmount = createSummaryLabel("Tiền thuế VAT: 0");
        lblAfterTax = createSummaryLabel("Tổng tiền nhập: 0");
        lblProfitHint = createSummaryLabel("Logic giá: Chưa nhập giá");

        summary.add(lblBeforeTax);
        summary.add(lblTaxAmount);
        summary.add(lblAfterTax);
        summary.add(lblProfitHint);

        gbc.gridy = 14;
        gbc.insets = new Insets(12, 0, 0, 0);
        form.add(summary, gbc);

        root.add(form, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);

        JButton btnCancel = createButton("Hủy", new Color(142, 153, 176));
        JButton btnSave = createButton("Lưu phiếu nhập", BLUE);

        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> saveReceipt());

        bottom.add(btnCancel);
        bottom.add(btnSave);

        root.add(bottom, BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);
    }

    private void addRow(JPanel form, GridBagConstraints gbc, int y, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(NAVY);

        gbc.gridy = y;
        gbc.insets = new Insets(0, 0, 5, 0);
        form.add(label, gbc);

        gbc.gridy = y + 1;
        gbc.insets = new Insets(0, 0, 13, 0);
        form.add(field, gbc);
    }

    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setPreferredSize(new Dimension(0, 38));
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(6, 10, 6, 10)
        ));
        return txt;
    }

    private JLabel createSummaryLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(NAVY);
        return label;
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(145, 40));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void bindEvents() {
        javax.swing.event.DocumentListener listener = new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                recalc();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                recalc();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                recalc();
            }
        };

        txtQuantity.getDocument().addDocumentListener(listener);
        txtImportPrice.getDocument().addDocumentListener(listener);
        cbVat.addActionListener(e -> recalc());
    }

    private void recalc() {
        try {
            int qty = Integer.parseInt(txtQuantity.getText().trim());
            BigDecimal importPrice = new BigDecimal(txtImportPrice.getText().trim());
            BigDecimal salePrice = product.getBasePrice();

            BigDecimal vatRate = getVatRate();
            BigDecimal beforeTax = importPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tax = beforeTax.multiply(vatRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal afterTax = beforeTax.add(tax).setScale(2, RoundingMode.HALF_UP);

            lblBeforeTax.setText("Tiền hàng trước thuế: " + money(beforeTax) + " VNĐ");
            lblTaxAmount.setText("Tiền thuế VAT: " + money(tax) + " VNĐ");
            lblAfterTax.setText("Tổng tiền nhập: " + money(afterTax) + " VNĐ");

            if (importPrice.compareTo(salePrice) < 0) {
                BigDecimal profit = salePrice.subtract(importPrice);
                lblProfitHint.setForeground(GREEN);
                lblProfitHint.setText("Logic giá: Hợp lệ, lãi gộp dự kiến " + money(profit) + " VNĐ / sản phẩm");
            } else {
                lblProfitHint.setForeground(RED);
                lblProfitHint.setText("Logic giá: Sai, giá nhập phải nhỏ hơn giá bán");
            }

        } catch (Exception e) {
            lblBeforeTax.setText("Tiền hàng trước thuế: 0");
            lblTaxAmount.setText("Tiền thuế VAT: 0");
            lblAfterTax.setText("Tổng tiền nhập: 0");
            lblProfitHint.setForeground(MUTED);
            lblProfitHint.setText("Logic giá: Chưa nhập đủ dữ liệu");
        }
    }

    private void saveReceipt() {
        try {
            int qty = Integer.parseInt(txtQuantity.getText().trim());
            BigDecimal importPrice = new BigDecimal(txtImportPrice.getText().trim());

            if (qty <= 0) {
                JOptionPane.showMessageDialog(this, "Số lượng nhập phải lớn hơn 0.");
                return;
            }

            if (importPrice.compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(this, "Giá nhập phải lớn hơn 0.");
                return;
            }

            if (product.getBasePrice() == null || product.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Sản phẩm chưa có giá bán hợp lệ.",
                        "Lỗi giá bán",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            // BẮT BUỘC: Giá nhập phải nhỏ hơn giá bán
            if (importPrice.compareTo(product.getBasePrice()) >= 0) {
                JOptionPane.showMessageDialog(
                        this,
                        "Giá nhập phải nhỏ hơn giá bán.\n\n"
                        + "Giá nhập: " + money(importPrice) + " VNĐ\n"
                        + "Giá bán: " + money(product.getBasePrice()) + " VNĐ\n\n"
                        + "Nếu giá nhập >= giá bán thì nghiệp vụ nhập hàng không hợp lý vì không có lợi nhuận.",
                        "Sai logic giá",
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }

            String receiptId = InventoryTransactionSql.getInstance().createPurchaseReceiptAndIncreaseStock(
                    productId,
                    qty,
                    importPrice,
                    getVatRate(),
                    txtSupplier.getText().trim(),
                    txtNote.getText().trim()
            );

            SyncVersionDao.bumpVersion("INVENTORY");
            SyncVersionDao.bumpVersion("PRODUCTS");

            RealtimeClient.send("INVENTORY_CHANGED");
            RealtimeClient.send("PRODUCTS_CHANGED");

            if (onSuccess != null) {
                onSuccess.run();
            }

            JOptionPane.showMessageDialog(
                    this,
                    "Đã lưu phiếu nhập và cập nhật tồn kho thành công.\n"
                    + "Mã phiếu: " + receiptId,
                    "Thành công",
                    JOptionPane.INFORMATION_MESSAGE
            );

            dispose();

            // Hiển thị phiếu hóa đơn nhập hàng ngay sau khi lưu
            Frame owner = (Frame) getOwner();
            new PurchaseReceiptInvoiceDialog(owner, receiptId).setVisible(true);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Số lượng hoặc giá nhập không hợp lệ.",
                    "Lỗi nhập liệu",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Không thể lưu phiếu nhập:\n" + e.getMessage(),
                    "Lỗi",
                    JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }

    private BigDecimal getVatRate() {
        String raw = String.valueOf(cbVat.getSelectedItem()).replace("%", "").trim();

        try {
            return new BigDecimal(raw);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "0";
        }

        return String.format("%,.0f", value);
    }
}
