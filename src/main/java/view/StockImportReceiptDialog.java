package view;

import business.service.InventoryPricePolicyService;
import business.sql.prod_inventory.InventoryTransactionSql;
import business.sql.prod_inventory.ProductsSql;
import common.db.DatabaseConnection;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.product.Product;

public class StockImportReceiptDialog extends JDialog {

    private final String productId;
    private final Runnable onSuccess;

    private Product product;

    private JLabel lblProductInfo;
    private JLabel lblSalePrice;
    private JLabel lblFixedImportPrice;
    private JLabel lblCategoryVat;
    private JLabel lblImportAfterVat;
    private JLabel lblBeforeTax;
    private JLabel lblTaxAmount;
    private JLabel lblAfterTax;
    private JLabel lblProfitHint;

    private JTextField txtQuantity;
    private JTextField txtNote;

    private JComboBox<SupplierItem> cbSupplier;

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
        loadSuppliers();
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
        setSize(640, 660);
        setMinimumSize(new Dimension(620, 620));
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(246, 247, 251));

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(new Color(246, 247, 251));
        root.setBorder(new EmptyBorder(22, 24, 22, 24));

        JLabel title = new JLabel("Tạo phiếu nhập hàng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(NAVY);

        JLabel sub = new JLabel("Nhập số lượng, chọn nhà cung cấp. Giá nhập và VAT được hệ thống tự tính.");
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

        BigDecimal fixedImportPrice = getFixedImportPriceBeforeVat();
        lblFixedImportPrice = new JLabel("Giá nhập cố định chưa VAT: " + money(fixedImportPrice) + " VNĐ");
        lblFixedImportPrice.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblFixedImportPrice.setForeground(new Color(255, 153, 0));

        BigDecimal fixedVat = getVatRate();
        lblCategoryVat = new JLabel(
                "VAT theo danh mục " + safe(product.getCategoryId(), "Không rõ") + ": "
                + fixedVat.stripTrailingZeros().toPlainString() + "%"
        );
        lblCategoryVat.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblCategoryVat.setForeground(BLUE);

        addRow(form, gbc, 0, "Sản phẩm", lblProductInfo);
        addRow(form, gbc, 2, "Giá bán", lblSalePrice);
        addRow(form, gbc, 4, "Giá nhập hệ thống", lblFixedImportPrice);
        addRow(form, gbc, 6, "Thuế VAT cố định", lblCategoryVat);

        txtQuantity = createTextField("VD: 20");
        txtNote = createTextField("VD: Nhập bổ sung theo cảnh báo tồn kho");

        cbSupplier = new JComboBox<>();
        cbSupplier.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cbSupplier.setBackground(Color.WHITE);
        cbSupplier.setPreferredSize(new Dimension(0, 38));

        addRow(form, gbc, 8, "Số lượng nhập (*)", txtQuantity);
        addRow(form, gbc, 10, "Nhà cung cấp", cbSupplier);
        addRow(form, gbc, 12, "Ghi chú", txtNote);

        JPanel summary = new JPanel(new GridLayout(5, 1, 0, 6));
        summary.setOpaque(false);
        summary.setBorder(new EmptyBorder(12, 0, 0, 0));

        lblImportAfterVat = createSummaryLabel("Giá nhập sau VAT / đơn vị: 0");
        lblBeforeTax = createSummaryLabel("Tiền hàng trước thuế: 0");
        lblTaxAmount = createSummaryLabel("Tiền thuế VAT: 0");
        lblAfterTax = createSummaryLabel("Tổng tiền nhập: 0");
        lblProfitHint = createSummaryLabel("Logic giá: Chưa nhập số lượng");

        summary.add(lblImportAfterVat);
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

    private void loadSuppliers() {
        cbSupplier.removeAllItems();

        String sql = """
            SELECT supplier_id, supplier_name
            FROM SUPPLIERS
            WHERE NVL(is_deleted, 0) = 0
            ORDER BY supplier_id
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cbSupplier.addItem(new SupplierItem(
                        rs.getString("supplier_id"),
                        rs.getString("supplier_name")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (cbSupplier.getItemCount() == 0) {
            cbSupplier.addItem(new SupplierItem("SUP_01", "Nhà cung cấp mặc định"));
        }

        String productSupplier = product.getSupplierId();

        if (productSupplier != null && !productSupplier.trim().isEmpty()) {
            for (int i = 0; i < cbSupplier.getItemCount(); i++) {
                SupplierItem item = cbSupplier.getItemAt(i);

                if (productSupplier.equalsIgnoreCase(item.supplierId)) {
                    cbSupplier.setSelectedIndex(i);
                    break;
                }
            }
        }
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
    }

    private void recalc() {
        try {
            int qty = Integer.parseInt(txtQuantity.getText().trim());

            if (qty <= 0) {
                throw new IllegalArgumentException("Số lượng phải lớn hơn 0");
            }

            BigDecimal salePrice = product.getBasePrice();
            BigDecimal importPriceBeforeVat = getFixedImportPriceBeforeVat();
            BigDecimal vatRate = getVatRate();

            BigDecimal importAfterVat = InventoryPricePolicyService.calculateImportPriceAfterVat(
                    importPriceBeforeVat,
                    vatRate
            );

            BigDecimal beforeTax = InventoryPricePolicyService.calculateLineBeforeTax(
                    importPriceBeforeVat,
                    qty
            );

            BigDecimal tax = InventoryPricePolicyService.calculateLineTax(
                    beforeTax,
                    vatRate
            );

            BigDecimal afterTax = InventoryPricePolicyService.calculateLineAfterTax(
                    beforeTax,
                    tax
            );

            lblImportAfterVat.setText("Giá nhập sau VAT / đơn vị: " + money(importAfterVat) + " VNĐ");
            lblBeforeTax.setText("Tiền hàng trước thuế: " + money(beforeTax) + " VNĐ");
            lblTaxAmount.setText("Tiền thuế VAT: " + money(tax) + " VNĐ");
            lblAfterTax.setText("Tổng tiền nhập: " + money(afterTax) + " VNĐ");

            if (salePrice != null && importAfterVat.compareTo(salePrice) < 0) {
                BigDecimal profit = salePrice.subtract(importAfterVat);
                lblProfitHint.setForeground(GREEN);
                lblProfitHint.setText("Logic giá: Hợp lệ, lãi gộp dự kiến "
                        + money(profit) + " VNĐ / sản phẩm");
            } else {
                lblProfitHint.setForeground(RED);
                lblProfitHint.setText("Logic giá: Sai, giá nhập sau VAT phải nhỏ hơn giá bán");
            }

        } catch (Exception e) {
            BigDecimal importPriceBeforeVat = getFixedImportPriceBeforeVat();
            BigDecimal importAfterVat = InventoryPricePolicyService.calculateImportPriceAfterVat(
                    importPriceBeforeVat,
                    getVatRate()
            );

            lblImportAfterVat.setText("Giá nhập sau VAT / đơn vị: " + money(importAfterVat) + " VNĐ");
            lblBeforeTax.setText("Tiền hàng trước thuế: 0");
            lblTaxAmount.setText("Tiền thuế VAT: 0");
            lblAfterTax.setText("Tổng tiền nhập: 0");
            lblProfitHint.setForeground(MUTED);
            lblProfitHint.setText("Logic giá: Chưa nhập số lượng");
        }
    }

    private void saveReceipt() {
        try {
            int qty = Integer.parseInt(txtQuantity.getText().trim());

            if (qty <= 0) {
                JOptionPane.showMessageDialog(this, "Số lượng nhập phải lớn hơn 0.");
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

            BigDecimal importPriceBeforeVat = getFixedImportPriceBeforeVat();
            BigDecimal vatRate = getVatRate();

            InventoryPricePolicyService.validateImportPriceLessThanSalePrice(
                    importPriceBeforeVat,
                    vatRate,
                    product.getBasePrice()
            );

            String supplierId = getSelectedSupplierId();

            String receiptId = InventoryTransactionSql.getInstance().createPurchaseReceiptAndIncreaseStock(
                    productId,
                    qty,
                    importPriceBeforeVat,
                    vatRate,
                    supplierId,
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

            Frame owner = (Frame) getOwner();
            new PurchaseReceiptInvoiceDialog(owner, receiptId).setVisible(true);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Số lượng nhập không hợp lệ.",
                    "Lỗi nhập liệu",
                    JOptionPane.ERROR_MESSAGE
            );
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Sai logic giá nhập",
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

    private BigDecimal getFixedImportPriceBeforeVat() {
        BigDecimal salePrice = product.getBasePrice();

        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return salePrice
                .multiply(new BigDecimal("0.70"))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getVatRate() {
        return InventoryPricePolicyService.resolveVatRateByCategory(product.getCategoryId());
    }

    private String getSelectedSupplierId() {
        Object selected = cbSupplier.getSelectedItem();

        if (selected instanceof SupplierItem item) {
            return item.supplierId;
        }

        return "SUP_01";
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "0";
        }

        return String.format("%,.0f", value);
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private static class SupplierItem {

        private final String supplierId;
        private final String supplierName;

        SupplierItem(String supplierId, String supplierName) {
            this.supplierId = supplierId;
            this.supplierName = supplierName;
        }

        @Override
        public String toString() {
            if (supplierName == null || supplierName.trim().isEmpty()) {
                return supplierId;
            }

            return supplierId + " - " + supplierName;
        }
    }
}
