package view;

import business.sql.prod_inventory.ProductsSql;
import business.sql.prod_inventory.SuppliersSql;
import common.db.DatabaseConnection;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import model.product.Product;
import model.product.Supplier;

/**
 * Wrapper riêng cho Warehouse Portal: bổ sung lại khu vực chọn ảnh, nhà cung cấp
 * và cột Nhà cung cấp trong bảng sản phẩm. Không căn giữa global, không đè renderer cột ảnh.
 */
public class CenteredProductView extends ProductView {

    private JComboBox<String> cbSupplier;
    private final Map<String, String> supplierNameById = new HashMap<>();
    private final Map<String, String> supplierIdByProductId = new HashMap<>();
    private boolean fillingSupplierColumn = false;
    private boolean actionsWrapped = false;

    public CenteredProductView() {
        super();
        SwingUtilities.invokeLater(this::applyWarehouseProductEnhancements);
    }

    private void applyWarehouseProductEnhancements() {
        loadSupplierCache();
        showImageControlsAgain();
        addSupplierControlToForm();
        wrapMutationButtonsToPersistSupplier();
        applySafeProductTableAlignment();
        bindTableSelectionToSupplierCombo();
        revalidate();
        repaint();
    }

    private void loadSupplierCache() {
        supplierNameById.clear();
        try {
            List<Supplier> suppliers = SuppliersSql.getInstance().selectAll();
            for (Supplier s : suppliers) {
                if (s == null || isBlank(s.getSupplierId())) {
                    continue;
                }
                String id = s.getSupplierId().trim();
                String name = isBlank(s.getSupplierName()) ? id : s.getSupplierName().trim();
                supplierNameById.put(id, name);
            }
        } catch (Exception ex) {
            System.err.println("[CenteredProductView] Cannot load suppliers: " + ex.getMessage());
        }

        if (supplierNameById.isEmpty()) {
            supplierNameById.put("SUP001", "Nhà cung cấp Tổng hợp");
        }

        reloadProductSupplierMap();
    }

    private void reloadProductSupplierMap() {
        supplierIdByProductId.clear();
        try {
            List<Product> products = ProductsSql.getInstance().selectAll();
            for (Product p : products) {
                if (p == null || isBlank(p.getProductId())) {
                    continue;
                }
                String supplierId = isBlank(p.getSupplierId()) ? "SUP001" : p.getSupplierId().trim();
                supplierIdByProductId.put(p.getProductId().trim(), supplierId);
            }
        } catch (Exception ex) {
            System.err.println("[CenteredProductView] Cannot load product suppliers: " + ex.getMessage());
        }
    }

    private void showImageControlsAgain() {
        setPrivateComponentVisible("lblImageSectionTitle", true);
        setPrivateComponentVisible("lblImagePreview", true);
        setPrivateComponentVisible("btnChooseImage", true);
    }

    private void setPrivateComponentVisible(String fieldName, boolean visible) {
        try {
            Object obj = getPrivateField(fieldName);
            if (obj instanceof Component component) {
                component.setVisible(visible);
                component.setEnabled(visible);
            }
        } catch (Exception ignored) {
        }
    }

    private void addSupplierControlToForm() {
        if (cbSupplier != null) {
            return;
        }

        JPanel formCard = getPrivateFieldAs("formCard", JPanel.class);
        if (formCard == null) {
            return;
        }

        cbSupplier = new JComboBox<>();
        cbSupplier.setEditable(true);
        cbSupplier.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cbSupplier.setPreferredSize(new Dimension(260, 40));
        cbSupplier.setBorder(BorderFactory.createEmptyBorder());

        for (Map.Entry<String, String> e : supplierNameById.entrySet()) {
            cbSupplier.addItem(e.getKey() + " - " + e.getValue());
        }
        cbSupplier.setSelectedItem(formatSupplierOption("SUP001"));

        JTextField editor = (JTextField) cbSupplier.getEditor().getEditorComponent();
        editor.putClientProperty("JTextField.placeholderText", "VD: SUP_04 - Masan Consumer...");
        editor.setHorizontalAlignment(JTextField.LEFT);
        editor.setFont(new Font("Segoe UI", Font.BOLD, 14));

        JLabel lblSupplier = new JLabel("Nhà cung cấp (*)");
        lblSupplier.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSupplier.setForeground(new java.awt.Color(43, 54, 116));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;

        // Đưa xuống cuối form để không phá layout cũ. Vẫn giữ nguyên phần chọn ảnh đã có sẵn.
        gbc.gridy = 900;
        gbc.insets = new Insets(10, 0, 5, 0);
        formCard.add(lblSupplier, gbc);

        gbc.gridy = 901;
        gbc.insets = new Insets(0, 0, 15, 0);
        formCard.add(cbSupplier, gbc);

        formCard.revalidate();
        formCard.repaint();
    }

    private void bindTableSelectionToSupplierCombo() {
        JTable table = findProductTable(this);
        if (table == null || cbSupplier == null) {
            return;
        }

        table.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            SwingUtilities.invokeLater(() -> syncSupplierComboFromSelectedRow(table));
        });
    }

    private void syncSupplierComboFromSelectedRow(JTable table) {
        if (table == null || cbSupplier == null || table.getSelectedRow() < 0) {
            return;
        }

        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        Object productValue = table.getModel().getValueAt(modelRow, 0);
        String productId = productValue == null ? "" : productValue.toString().trim();
        String supplierId = supplierIdByProductId.get(productId);

        if (!isBlank(supplierId)) {
            cbSupplier.setSelectedItem(formatSupplierOption(supplierId));
        }
    }

    private void wrapMutationButtonsToPersistSupplier() {
        if (actionsWrapped) {
            return;
        }

        JButton btnAdd = getPrivateFieldAs("btnAdd", JButton.class);
        JButton btnUpdate = getPrivateFieldAs("btnUpdate", JButton.class);

        wrapButton(btnAdd, false);
        wrapButton(btnUpdate, true);

        actionsWrapped = true;
    }

    private void wrapButton(JButton button, boolean updateMode) {
        if (button == null) {
            return;
        }

        ActionListener[] oldListeners = button.getActionListeners();
        for (ActionListener l : oldListeners) {
            button.removeActionListener(l);
        }

        button.addActionListener(e -> {
            String selectedSupplierId = getSelectedSupplierId();
            String selectedProductIdBefore = updateMode ? getSelectedTableProductId() : null;
            String nameBefore = getTextFieldValue("txtName");
            String categoryBefore = getCategoryIdFromCombo();

            for (ActionListener l : oldListeners) {
                l.actionPerformed(e);
            }

            SwingUtilities.invokeLater(() -> {
                String productId = selectedProductIdBefore;
                if (isBlank(productId)) {
                    productId = resolveProductIdAfterAdd(nameBefore, categoryBefore);
                }

                if (!isBlank(productId) && !isBlank(selectedSupplierId)) {
                    updateProductSupplier(productId, selectedSupplierId);
                    reloadProductSupplierMap();
                    fillSupplierColumn();
                }
            });
        });
    }

    private String resolveProductIdAfterAdd(String name, String categoryId) {
        try {
            Product product = ProductsSql.getInstance().findByExactNameAndCategory(name, categoryId);
            return product == null ? null : product.getProductId();
        } catch (Exception ex) {
            return null;
        }
    }

    private void updateProductSupplier(String productId, String supplierId) {
        String sql = "UPDATE PRODUCTS SET supplier_id = ? WHERE product_id = ? AND NVL(is_deleted, 0) = 0";
        try (Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, supplierId);
            ps.setString(2, productId);
            ps.executeUpdate();
        } catch (Exception ex) {
            System.err.println("[CenteredProductView] Cannot update supplier for " + productId + ": " + ex.getMessage());
        }
    }

    private String getSelectedSupplierId() {
        if (cbSupplier == null) {
            return "SUP001";
        }

        Object value = cbSupplier.getEditor().getItem();
        String text = value == null ? "" : value.toString().trim();
        if (text.contains(" - ")) {
            text = text.substring(0, text.indexOf(" - ")).trim();
        }
        return isBlank(text) ? "SUP001" : text;
    }

    private String formatSupplierOption(String supplierId) {
        if (isBlank(supplierId)) {
            supplierId = "SUP001";
        }
        String id = supplierId.trim();
        String name = supplierNameById.getOrDefault(id, id);
        return id + " - " + name;
    }

    private String getTextFieldValue(String fieldName) {
        JTextField field = getPrivateFieldAs(fieldName, JTextField.class);
        return field == null || field.getText() == null ? "" : field.getText().trim();
    }

    private String getCategoryIdFromCombo() {
        JComboBox<?> cbCategory = getPrivateFieldAs("cbCategory", JComboBox.class);
        if (cbCategory == null || cbCategory.getEditor() == null) {
            return "";
        }
        Object raw = cbCategory.getEditor().getItem();
        String text = raw == null ? "" : raw.toString().trim();
        if (text.contains(" - ")) {
            text = text.substring(0, text.indexOf(" - ")).trim();
        }
        return text;
    }

    private String getSelectedTableProductId() {
        JTable table = findProductTable(this);
        if (table == null || table.getSelectedRow() < 0) {
            return null;
        }
        int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
        Object value = table.getModel().getValueAt(modelRow, 0);
        return value == null ? null : value.toString().trim();
    }

    private void applySafeProductTableAlignment() {
        JTable table = findProductTable(this);
        if (table == null || table.getColumnCount() < 6) {
            return;
        }

        ensureSupplierColumn(table);
        centerHeader(table);

        // Bảng sản phẩm: 2 = Tên sản phẩm, 5 = Loại hàng, 6 = Nhà cung cấp.
        // Không động vào cột 1 = Ảnh SP để tránh mất ảnh/icon.
        centerColumn(table, 2);
        centerColumn(table, 5);
        centerColumn(table, getSupplierColumnIndex(table));

        if (table.getModel() instanceof DefaultTableModel model) {
            model.addTableModelListener(e -> {
                if (fillingSupplierColumn || e.getType() == TableModelEvent.DELETE) {
                    return;
                }
                SwingUtilities.invokeLater(this::fillSupplierColumn);
            });
        }

        fillSupplierColumn();
    }

    private void ensureSupplierColumn(JTable table) {
        if (!(table.getModel() instanceof DefaultTableModel model)) {
            return;
        }

        if (getSupplierColumnIndex(table) >= 0) {
            return;
        }

        model.addColumn("Nhà cung cấp");
        int col = table.getColumnCount() - 1;
        table.getColumnModel().getColumn(col).setPreferredWidth(190);
        table.getColumnModel().getColumn(col).setMinWidth(150);
    }

    private int getSupplierColumnIndex(JTable table) {
        if (table == null) {
            return -1;
        }
        for (int i = 0; i < table.getColumnCount(); i++) {
            String name = table.getColumnName(i);
            if (name != null && name.trim().equalsIgnoreCase("Nhà cung cấp")) {
                return i;
            }
        }
        return -1;
    }

    private void fillSupplierColumn() {
        JTable table = findProductTable(this);
        if (table == null || !(table.getModel() instanceof DefaultTableModel model)) {
            return;
        }

        int supplierCol = getSupplierColumnIndex(table);
        if (supplierCol < 0) {
            return;
        }

        fillingSupplierColumn = true;
        try {
            reloadProductSupplierMap();
            for (int row = 0; row < model.getRowCount(); row++) {
                Object productValue = model.getValueAt(row, 0);
                String productId = productValue == null ? "" : productValue.toString().trim();
                String supplierId = supplierIdByProductId.getOrDefault(productId, "SUP001");
                model.setValueAt(formatSupplierForTable(supplierId), row, supplierCol);
            }
        } finally {
            fillingSupplierColumn = false;
        }
    }

    private String formatSupplierForTable(String supplierId) {
        if (isBlank(supplierId)) {
            supplierId = "SUP001";
        }
        String id = supplierId.trim();
        String name = supplierNameById.getOrDefault(id, id);
        return id + " - " + name;
    }

    private JTable findProductTable(Component component) {
        if (component instanceof JTable table) {
            if (looksLikeProductTable(table)) {
                return table;
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTable found = findProductTable(child);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private boolean looksLikeProductTable(JTable table) {
        if (table == null || table.getColumnCount() < 6) {
            return false;
        }

        StringBuilder names = new StringBuilder();
        for (int i = 0; i < table.getColumnCount(); i++) {
            Object name = table.getColumnName(i);
            if (name != null) {
                names.append(name.toString().toLowerCase()).append(' ');
            }
        }

        String joined = names.toString();
        return joined.contains("tên sản phẩm")
                && joined.contains("loại hàng")
                && joined.contains("ảnh");
    }

    private void centerHeader(JTable table) {
        if (table.getTableHeader() == null) {
            return;
        }

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        headerRenderer.setVerticalAlignment(SwingConstants.CENTER);
        headerRenderer.setFont(table.getTableHeader().getFont());
        headerRenderer.setBackground(table.getTableHeader().getBackground());
        headerRenderer.setForeground(table.getTableHeader().getForeground());
        table.getTableHeader().setDefaultRenderer(headerRenderer);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
    }

    private void centerColumn(JTable table, int columnIndex) {
        if (columnIndex < 0 || columnIndex >= table.getColumnCount()) {
            return;
        }

        TableCellRenderer currentRenderer = table.getColumnModel().getColumn(columnIndex).getCellRenderer();
        if (currentRenderer == null) {
            currentRenderer = table.getDefaultRenderer(Object.class);
        }

        table.getColumnModel().getColumn(columnIndex).setCellRenderer(new CenteredRenderer(currentRenderer));
    }

    private Object getPrivateField(String fieldName) throws Exception {
        Field field = ProductView.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(this);
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateFieldAs(String fieldName, Class<T> type) {
        try {
            Object value = getPrivateField(fieldName);
            if (type.isInstance(value)) {
                return (T) value;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class CenteredRenderer implements TableCellRenderer {

        private final TableCellRenderer delegate;

        CenteredRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column
        ) {
            Component component = delegate.getTableCellRendererComponent(
                    table,
                    value,
                    isSelected,
                    hasFocus,
                    row,
                    column
            );

            centerComponent(component);
            return component;
        }

        private void centerComponent(Component component) {
            if (component instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setVerticalAlignment(SwingConstants.CENTER);
                return;
            }

            if (component instanceof Container container) {
                for (Component child : container.getComponents()) {
                    centerComponent(child);
                }
            }
        }
    }
}
