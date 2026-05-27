package view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * Wrapper riêng cho Warehouse Portal: căn giữa một số cột text và in đậm bảng tồn kho.
 * Không dùng căn giữa global nên không đè renderer cột ảnh.
 */
public class CenteredInventoryView extends InventoryView {

    public CenteredInventoryView() {
        super();
        SwingUtilities.invokeLater(this::applySafeColumnAlignment);
    }

    private void applySafeColumnAlignment() {
        JTable table = findFirstTable(this);
        if (table == null || table.getColumnCount() <= 8) {
            return;
        }

        table.setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.setRowHeight(Math.max(table.getRowHeight(), 42));

        centerHeader(table);

        // Bảng tồn kho: 2 = Tên sản phẩm, 7 = Trạng thái.
        // Các cột số/đơn vị/chi nhánh đã được InventoryView căn giữa sẵn.
        centerColumn(table, 2);
        centerColumn(table, 7);

        revalidate();
        repaint();
    }

    private JTable findFirstTable(Component component) {
        if (component instanceof JTable table) {
            return table;
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTable table = findFirstTable(child);
                if (table != null) {
                    return table;
                }
            }
        }

        return null;
    }

    private void centerHeader(JTable table) {
        if (table.getTableHeader() == null) {
            return;
        }

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        headerRenderer.setVerticalAlignment(SwingConstants.CENTER);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 13));
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
                label.setFont(new Font("Segoe UI", Font.BOLD, 13));
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
