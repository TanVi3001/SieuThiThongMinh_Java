package view;

import java.awt.Component;
import java.awt.Container;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * Wrapper riêng cho Warehouse Portal: căn giữa các cột bảng nhà cung cấp.
 * Không đổi logic thêm/sửa/xóa/lọc của SupplierManagementView.
 */
public class CenteredSupplierManagementView extends SupplierManagementView {

    public CenteredSupplierManagementView() {
        super(SupplierViewMode.WAREHOUSE);
        SwingUtilities.invokeLater(this::applySafeSupplierTableAlignment);
    }

    private void applySafeSupplierTableAlignment() {
        JTable table = findSupplierTable(this);
        if (table == null || table.getColumnCount() < 6) {
            return;
        }

        centerHeader(table);

        // Bảng nhà cung cấp: căn giữa toàn bộ cột vì không có cột ảnh/icon.
        for (int i = 0; i < table.getColumnCount(); i++) {
            centerColumn(table, i);
        }
    }

    private JTable findSupplierTable(Component component) {
        if (component instanceof JTable table) {
            if (looksLikeSupplierTable(table)) {
                return table;
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTable found = findSupplierTable(child);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private boolean looksLikeSupplierTable(JTable table) {
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
        return joined.contains("mã ncc")
                && joined.contains("tên nhà cung cấp")
                && joined.contains("số điện thoại")
                && joined.contains("email")
                && joined.contains("địa chỉ");
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
