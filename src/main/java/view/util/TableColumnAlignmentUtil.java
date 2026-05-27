package view.util;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * Utility căn giữa từng cột cụ thể, không quét toàn bộ UI nên không làm mất ảnh/icon.
 */
public final class TableColumnAlignmentUtil {

    private TableColumnAlignmentUtil() {
    }

    public static void centerColumns(JTable table, int... viewColumns) {
        if (table == null || viewColumns == null) {
            return;
        }

        for (int col : viewColumns) {
            if (col < 0 || col >= table.getColumnCount()) {
                continue;
            }

            TableCellRenderer oldRenderer = table.getColumnModel().getColumn(col).getCellRenderer();
            table.getColumnModel().getColumn(col).setCellRenderer(new CenterWrapperRenderer(oldRenderer));
        }
    }

    public static void centerHeaders(JTable table) {
        if (table == null || table.getTableHeader() == null) {
            return;
        }

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
        renderer.setHorizontalAlignment(SwingConstants.CENTER);
        renderer.setVerticalAlignment(SwingConstants.CENTER);
        renderer.setFont(table.getTableHeader().getFont());
        renderer.setBackground(table.getTableHeader().getBackground());
        renderer.setForeground(table.getTableHeader().getForeground());
        table.getTableHeader().setDefaultRenderer(renderer);
    }

    private static class CenterWrapperRenderer implements TableCellRenderer {

        private final TableCellRenderer delegate;
        private final DefaultTableCellRenderer fallback = new DefaultTableCellRenderer();

        CenterWrapperRenderer(TableCellRenderer delegate) {
            this.delegate = delegate;
            fallback.setHorizontalAlignment(SwingConstants.CENTER);
            fallback.setVerticalAlignment(SwingConstants.CENTER);
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
            Component c = delegate != null
                    ? delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                    : fallback.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            centerComponent(c);
            return c;
        }

        private void centerComponent(Component c) {
            if (c instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setVerticalAlignment(SwingConstants.CENTER);
                return;
            }

            if (c instanceof java.awt.Container container) {
                for (Component child : container.getComponents()) {
                    centerComponent(child);
                }
            }
        }
    }
}
