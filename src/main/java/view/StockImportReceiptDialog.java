package view;

import business.service.InventoryPricePolicyService;
import business.service.SessionManager;
import business.sql.prod_inventory.InventoryTransactionSql;
import business.sql.prod_inventory.ProductsSql;
import common.db.DatabaseConnection;
import common.realtime.RealtimeClient;
import common.sync.SyncVersionDao;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import model.product.Product;

public class StockImportReceiptDialog extends JDialog {

    private final String initialProductId;
    private final Runnable onSuccess;

    private final List<Product> products = new ArrayList<>();
    private JComboBox<ProductItem> cbProduct;
    private JComboBox<SupplierItem> cbSupplier;
    private JTextField txtQuantity;
    private JTextField txtNote;
    private JTable tblLines;
    private DefaultTableModel lineModel;
    private JLabel lblProductPrice;
    private JLabel lblProductVat;
    private JLabel lblLineHint;
    private JLabel lblBeforeTax;
    private JLabel lblTaxAmount;
    private JLabel lblAfterTax;
    private JLabel lblLineCount;

    private final Color NAVY = new Color(23, 52, 99);
    private final Color MUTED = new Color(111, 124, 149);
    private final Color BLUE = new Color(67, 97, 238);
    private final Color GREEN = new Color(0, 163, 108);
    private final Color RED = new Color(220, 53, 69);
    private final Color BORDER = new Color(232, 237, 245);

    public StockImportReceiptDialog(Frame owner, String productId, Runnable onSuccess) {
        super(owner, "Phieu nhap kho", true);
        this.initialProductId = productId;
        this.onSuccess = onSuccess;

        loadProducts();
        initUI();
        loadSuppliers();
        selectInitialProduct();
        bindEvents();
        recalcCurrentLine();
        recalcTotals();
    }

    private void loadProducts() {
        String storeId = SessionManager.getCurrentStoreId();
        List<Product> loaded = storeId == null || storeId.trim().isEmpty()
                ? ProductsSql.getInstance().selectAll()
                : ProductsSql.getInstance().selectAllByStore(storeId);

        if (loaded != null) {
            products.addAll(loaded);
        }

        if (products.isEmpty() && initialProductId != null && !initialProductId.trim().isEmpty()) {
            Product product = ProductsSql.getInstance().findById(initialProductId);
            if (product != null) {
                products.add(product);
            }
        }

        if (products.isEmpty()) {
            throw new IllegalArgumentException("Khong co san pham nao de nhap kho.");
        }
    }

    private void initUI() {
        setSize(980, 720);
        setMinimumSize(new Dimension(900, 650));
        setLocationRelativeTo(getOwner());
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(246, 247, 251));

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(new Color(246, 247, 251));
        root.setBorder(new EmptyBorder(22, 24, 22, 24));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Tao phieu nhap hang");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(NAVY);

        JLabel sub = new JLabel("Them nhieu san pham vao cung mot phieu, he thong tu tinh gia nhap va VAT.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(MUTED);

        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(4));
        titleBox.add(sub);
        root.add(titleBox, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 14));
        body.setOpaque(false);
        body.add(createInputCard(), BorderLayout.NORTH);
        body.add(createTableCard(), BorderLayout.CENTER);
        body.add(createSummaryCard(), BorderLayout.SOUTH);
        root.add(body, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setOpaque(false);

        JButton btnCancel = createButton("Huy", new Color(142, 153, 176));
        JButton btnSave = createButton("Luu phieu nhap", BLUE);
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e -> saveReceipt());

        bottom.add(btnCancel);
        bottom.add(btnSave);
        root.add(bottom, BorderLayout.SOUTH);

        add(root, BorderLayout.CENTER);
    }

    private JPanel createInputCard() {
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(16, 18, 16, 18)
        ));

        cbProduct = new JComboBox<>();
        cbProduct.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbProduct.setBackground(Color.WHITE);
        cbProduct.setPreferredSize(new Dimension(0, 38));
        for (Product product : products) {
            cbProduct.addItem(new ProductItem(product));
        }

        txtQuantity = createTextField("VD: 20");
        txtNote = createTextField("Ghi chu chung cho phieu nhap");
        cbSupplier = new JComboBox<>();
        cbSupplier.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cbSupplier.setBackground(Color.WHITE);
        cbSupplier.setPreferredSize(new Dimension(0, 38));

        JButton btnAddLine = createButton("Them dong", GREEN);
        JButton btnRemoveLine = createButton("Xoa dong", RED);
        btnAddLine.addActionListener(e -> addOrUpdateLine());
        btnRemoveLine.addActionListener(e -> removeSelectedLine());

        lblProductPrice = createInfoLabel("Gia ban: 0 VND");
        lblProductVat = createInfoLabel("VAT: 0%");
        lblLineHint = createInfoLabel("Nhap so luong de xem tong dong.");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        addInput(card, gbc, 0, 0, 3, "San pham", cbProduct);
        addInput(card, gbc, 3, 0, 1, "So luong", txtQuantity);

        gbc.gridx = 4;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        card.add(btnAddLine, gbc);

        gbc.gridx = 5;
        card.add(btnRemoveLine, gbc);

        addInput(card, gbc, 0, 2, 2, "Nha cung cap", cbSupplier);
        addInput(card, gbc, 2, 2, 4, "Ghi chu", txtNote);

        JPanel info = new JPanel(new GridLayout(1, 3, 12, 0));
        info.setOpaque(false);
        info.add(lblProductPrice);
        info.add(lblProductVat);
        info.add(lblLineHint);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 6;
        gbc.weightx = 1;
        gbc.insets = new Insets(8, 0, 0, 0);
        card.add(info, gbc);

        return card;
    }

    private JPanel createTableCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(12, 14, 12, 14)
        ));

        lineModel = new DefaultTableModel(
                new Object[]{"Ma SP", "Ten san pham", "SL", "Don vi", "Gia nhap", "VAT", "Truoc thue", "Tien VAT", "Sau thue"},
                0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return columnIndex == 2 ? Integer.class : String.class;
            }
        };

        tblLines = new JTable(lineModel);
        tblLines.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblLines.setRowHeight(34);
        tblLines.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        tblLines.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        card.add(new JScrollPane(tblLines), BorderLayout.CENTER);
        return card;
    }

    private JPanel createSummaryCard() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);

        JPanel summary = new JPanel(new GridLayout(4, 1, 0, 8));
        summary.setBackground(Color.WHITE);
        summary.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(14, 18, 14, 18)
        ));

        lblLineCount = createSummaryLabel("So dong: 0");
        lblBeforeTax = createSummaryLabel("Tien hang truoc thue: 0 VND");
        lblTaxAmount = createSummaryLabel("Tien thue VAT: 0 VND");
        lblAfterTax = createSummaryLabel("Tong tien nhap: 0 VND");

        summary.add(lblLineCount);
        summary.add(lblBeforeTax);
        summary.add(lblTaxAmount);
        summary.add(lblAfterTax);

        wrap.add(summary, BorderLayout.EAST);
        return wrap;
    }

    private void addInput(JPanel panel, GridBagConstraints gbc, int x, int y, int width, String labelText, java.awt.Component field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(NAVY);

        gbc.gridx = x;
        gbc.gridy = y;
        gbc.gridwidth = width;
        gbc.weightx = width;
        gbc.insets = new Insets(0, 0, 4, 10);
        panel.add(label, gbc);

        gbc.gridy = y + 1;
        gbc.insets = new Insets(0, 0, 10, 10);
        panel.add(field, gbc);
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
                Connection con = DatabaseConnection.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
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
            cbSupplier.addItem(new SupplierItem("SUP_01", "Nha cung cap mac dinh"));
        }
    }

    private void selectInitialProduct() {
        if (initialProductId == null || initialProductId.trim().isEmpty()) {
            return;
        }

        for (int i = 0; i < cbProduct.getItemCount(); i++) {
            ProductItem item = cbProduct.getItemAt(i);
            if (initialProductId.equalsIgnoreCase(item.product.getProductId())) {
                cbProduct.setSelectedIndex(i);
                break;
            }
        }
    }

    private void bindEvents() {
        cbProduct.addActionListener(e -> recalcCurrentLine());

        txtQuantity.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                recalcCurrentLine();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                recalcCurrentLine();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                recalcCurrentLine();
            }
        });
    }

    private void recalcCurrentLine() {
        Product product = getSelectedProduct();
        if (product == null) {
            lblProductPrice.setText("Gia ban: 0 VND");
            lblProductVat.setText("VAT: 0%");
            lblLineHint.setText("Chua chon san pham.");
            return;
        }

        BigDecimal importBeforeVat = getFixedImportPriceBeforeVat(product);
        BigDecimal vatRate = getVatRate(product);
        lblProductPrice.setText("Gia ban: " + money(product.getBasePrice()) + " VND");
        lblProductVat.setText("Gia nhap: " + money(importBeforeVat) + " VND | VAT: "
                + vatRate.stripTrailingZeros().toPlainString() + "%");

        try {
            int qty = parseQuantity();
            BigDecimal afterTax = calculateAfterTax(importBeforeVat, vatRate, qty);
            lblLineHint.setForeground(NAVY);
            lblLineHint.setText("Tam tinh dong: " + money(afterTax) + " VND");
        } catch (Exception e) {
            lblLineHint.setForeground(MUTED);
            lblLineHint.setText("Nhap so luong de xem tong dong.");
        }
    }

    private void addOrUpdateLine() {
        Product product = getSelectedProduct();
        if (product == null) {
            JOptionPane.showMessageDialog(this, "Vui long chon san pham.");
            return;
        }

        int qty;
        try {
            qty = parseQuantity();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "So luong nhap khong hop le.");
            return;
        }

        BigDecimal importBeforeVat = getFixedImportPriceBeforeVat(product);
        BigDecimal vatRate = getVatRate(product);

        try {
            InventoryPricePolicyService.validateImportPriceLessThanSalePrice(
                    importBeforeVat,
                    vatRate,
                    product.getBasePrice()
            );
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Sai logic gia nhap", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int existingRow = findLineRow(product.getProductId());
        int finalQty = qty;
        if (existingRow >= 0) {
            finalQty += Integer.parseInt(String.valueOf(lineModel.getValueAt(existingRow, 2)));
            lineModel.removeRow(existingRow);
        }

        BigDecimal beforeTax = importBeforeVat.multiply(BigDecimal.valueOf(finalQty)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = beforeTax.multiply(vatRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal afterTax = beforeTax.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        lineModel.addRow(new Object[]{
            product.getProductId(),
            product.getProductName(),
            finalQty,
            safeUnit(product),
            money(importBeforeVat),
            vatRate.stripTrailingZeros().toPlainString() + "%",
            money(beforeTax),
            money(taxAmount),
            money(afterTax)
        });

        txtQuantity.setText("");
        recalcTotals();
    }

    private void removeSelectedLine() {
        int row = tblLines.getSelectedRow();
        if (row < 0) {
            return;
        }

        lineModel.removeRow(tblLines.convertRowIndexToModel(row));
        recalcTotals();
    }

    private void recalcTotals() {
        BigDecimal beforeTax = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal afterTax = BigDecimal.ZERO;

        for (int i = 0; i < lineModel.getRowCount(); i++) {
            Product product = findProductById(String.valueOf(lineModel.getValueAt(i, 0)));
            int qty = Integer.parseInt(String.valueOf(lineModel.getValueAt(i, 2)));
            BigDecimal importBeforeVat = getFixedImportPriceBeforeVat(product);
            BigDecimal vatRate = getVatRate(product);
            BigDecimal lineBeforeTax = importBeforeVat.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineTax = lineBeforeTax.multiply(vatRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            BigDecimal lineAfterTax = lineBeforeTax.add(lineTax).setScale(2, RoundingMode.HALF_UP);

            beforeTax = beforeTax.add(lineBeforeTax);
            taxAmount = taxAmount.add(lineTax);
            afterTax = afterTax.add(lineAfterTax);
        }

        lblLineCount.setText("So dong: " + lineModel.getRowCount());
        lblBeforeTax.setText("Tien hang truoc thue: " + money(beforeTax) + " VND");
        lblTaxAmount.setText("Tien thue VAT: " + money(taxAmount) + " VND");
        lblAfterTax.setText("Tong tien nhap: " + money(afterTax) + " VND");
    }

    private void saveReceipt() {
        if (lineModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Vui long them it nhat 1 san pham vao phieu nhap.");
            return;
        }

        try {
            List<InventoryTransactionSql.PurchaseReceiptInputLine> lines = new ArrayList<>();
            for (int i = 0; i < lineModel.getRowCount(); i++) {
                Product product = findProductById(String.valueOf(lineModel.getValueAt(i, 0)));
                int qty = Integer.parseInt(String.valueOf(lineModel.getValueAt(i, 2)));
                lines.add(new InventoryTransactionSql.PurchaseReceiptInputLine(
                        product.getProductId(),
                        qty,
                        getFixedImportPriceBeforeVat(product),
                        getVatRate(product)
                ));
            }

            String receiptId = InventoryTransactionSql.getInstance().createPurchaseReceiptAndIncreaseStock(
                    lines,
                    getSelectedSupplierId(),
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
                    "Da luu phieu nhap va cap nhat ton kho thanh cong.\nMa phieu: " + receiptId,
                    "Thanh cong",
                    JOptionPane.INFORMATION_MESSAGE
            );

            dispose();

            Frame owner = (Frame) getOwner();
            new PurchaseReceiptInvoiceDialog(owner, receiptId).setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    "Khong the luu phieu nhap:\n" + e.getMessage(),
                    "Loi",
                    JOptionPane.ERROR_MESSAGE
            );
            e.printStackTrace();
        }
    }

    private JTextField createTextField(String placeholder) {
        JTextField txt = new JTextField();
        txt.setPreferredSize(new Dimension(0, 38));
        txt.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        txt.putClientProperty("JTextField.placeholderText", placeholder);
        txt.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(6, 10, 6, 10)
        ));
        return txt;
    }

    private JLabel createInfoLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(NAVY);
        return label;
    }

    private JLabel createSummaryLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 13));
        label.setForeground(NAVY);
        return label;
    }

    private JButton createButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(132, 38));
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private int parseQuantity() {
        int qty = Integer.parseInt(txtQuantity.getText().trim());
        if (qty <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        return qty;
    }

    private Product getSelectedProduct() {
        Object selected = cbProduct.getSelectedItem();
        return selected instanceof ProductItem item ? item.product : null;
    }

    private Product findProductById(String productId) {
        for (Product product : products) {
            if (product.getProductId().equalsIgnoreCase(productId)) {
                return product;
            }
        }

        return ProductsSql.getInstance().findById(productId);
    }

    private int findLineRow(String productId) {
        for (int i = 0; i < lineModel.getRowCount(); i++) {
            if (productId.equalsIgnoreCase(String.valueOf(lineModel.getValueAt(i, 0)))) {
                return i;
            }
        }
        return -1;
    }

    private BigDecimal getFixedImportPriceBeforeVat(Product product) {
        BigDecimal salePrice = product == null ? null : product.getBasePrice();
        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return salePrice.multiply(new BigDecimal("0.70")).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal getVatRate(Product product) {
        return InventoryPricePolicyService.resolveVatRateByCategory(product.getCategoryId());
    }

    private BigDecimal calculateAfterTax(BigDecimal importBeforeVat, BigDecimal vatRate, int qty) {
        BigDecimal beforeTax = importBeforeVat.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = beforeTax.multiply(vatRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return beforeTax.add(tax).setScale(2, RoundingMode.HALF_UP);
    }

    private String getSelectedSupplierId() {
        Object selected = cbSupplier.getSelectedItem();
        return selected instanceof SupplierItem item ? item.supplierId : "SUP_01";
    }

    private String safeUnit(Product product) {
        String unit = product == null ? null : product.getUnit();
        return unit == null || unit.trim().isEmpty() ? "Cai" : unit.trim();
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "0";
        }
        return String.format("%,.0f", value);
    }

    private static class ProductItem {

        private final Product product;

        ProductItem(Product product) {
            this.product = product;
        }

        @Override
        public String toString() {
            return product.getProductId() + " - " + product.getProductName();
        }
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
