package view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * Wrapper riêng cho Warehouse Portal: căn giữa + phóng to nhẹ màn Danh mục & Thuế VAT.
 * Không đổi logic thêm/sửa/xóa/lọc, không đè mất icon danh mục.
 */
public class CenteredCategoryTaxView extends CategoryTaxView {

    public CenteredCategoryTaxView() {
        super();
        SwingUtilities.invokeLater(this::applyWarehouseCategoryStyling);
    }

    private void applyWarehouseCategoryStyling() {
        enlargeFormAndButtons(this);

        JTable table = findCategoryTable(this);
        if (table == null || table.getColumnCount() < 5) {
            revalidate();
            repaint();
            return;
        }

        table.setRowHeight(Math.max(table.getRowHeight(), 48));
        table.setFont(new Font("Segoe UI", Font.BOLD, 15));
        centerHeader(table);

        // 0 = Mã DM có icon, wrap renderer cũ để giữ icon nhưng vẫn căn giữa.
        centerColumn(table, 0, true);
        centerColumn(table, 1, false);
        centerColumn(table, 2, false);
        centerColumn(table, 3, false);
        centerColumn(table, 4, false);

        revalidate();
        repaint();
    }

    private void enlargeFormAndButtons(Component component) {
        if (component == null) {
            return;
        }

        if (component instanceof AbstractButton button) {
            Font old = button.getFont();
            button.setFont(new Font("Segoe UI", Font.BOLD, Math.max(14, old == null ? 14 : old.getSize() + 1)));
            button.setPreferredSize(new Dimension(Math.max(145, button.getPreferredSize().width), 42));
            button.setMinimumSize(new Dimension(130, 42));
            button.setHorizontalAlignment(SwingConstants.CENTER);
        } else if (component instanceof JTextField textField) {
            textField.setFont(new Font("Segoe UI", Font.BOLD, 14));
            textField.setHorizontalAlignment(JTextField.CENTER);
        } else if (component instanceof JComboBox<?> comboBox) {
            comboBox.setFont(new Font("Segoe UI", Font.BOLD, 14));
        } else if (component instanceof JLabel label) {
            Font old = label.getFont();
            if (old != null && old.getSize() <= 14) {
                label.setFont(new Font("Segoe UI", old.getStyle() | Font.BOLD, old.getSize() + 1));
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                enlargeFormAndButtons(child);
            }
        }
    }

    private JTable findCategoryTable(Component component) {
        if (component instanceof JTable table) {
            if (looksLikeCategoryTable(table)) {
                return table;
            }
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                JTable found = findCategoryTable(child);
                if (found != null) {
                    return found;
                }
            }
        }

        return null;
    }

    private boolean looksLikeCategoryTable(JTable table) {
        if (table == null || table.getColumnCount() < 5) {
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
        return joined.contains("mã dm")
                && joined.contains("tên danh mục")
                && joined.contains("thuế vat")
                && joined.contains("trạng thái");
    }

    private void centerHeader(JTable table) {
        if (table.getTableHeader() == null) {
            return;
        }

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
        headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        headerRenderer.setVerticalAlignment(SwingConstants.CENTER);
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 15));
        headerRenderer.setBackground(table.getTableHeader().getBackground());
        headerRenderer.setForeground(table.getTableHeader().getForeground());
        headerRenderer.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.getTableHeader().setDefaultRenderer(headerRenderer);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
        }
    }

    private void centerColumn(JTable table, int columnIndex, boolean keepIconRenderer) {
        if (columnIndex < 0 || columnIndex >= table.getColumnCount()) {
            return;
        }

        TableCellRenderer currentRenderer = table.getColumnModel().getColumn(columnIndex).getCellRenderer();
        if (currentRenderer == null) {
            currentRenderer = table.getDefaultRenderer(Object.class);
        }

        TableCellRenderer renderer = keepIconRenderer
                ? new CenteredWrapperRenderer(currentRenderer)
                : new StrongCenteredRenderer(columnIndex == 2);

        table.getColumnModel().getColumn(columnIndex).setCellRenderer(renderer);
    }

    private static class StrongCenteredRenderer extends DefaultTableCellRenderer {

        private final boolean vatColumn;

        StrongCenteredRenderer(boolean vatColumn) {
            this.vatColumn = vatColumn;
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
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
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            setVerticalAlignment(SwingConstants.CENTER);
            setFont(new Font("Segoe UI", Font.BOLD, 15));
            setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                if (vatColumn) {
                    String text = value == null ? "" : value.toString();
                    setForeground("Chưa set".equalsIgnoreCase(text) ? new java.awt.Color(180, 83, 9) : new java.awt.Color(185, 28, 28));
                } else {
                    setForeground(new java.awt.Color(43, 54, 116));
                }
            }
            return this;
        }
    }

    private static class CenteredWrapperRenderer implements TableCellRenderer {

        private final TableCellRenderer delegate;

        CenteredWrapperRenderer(TableCellRenderer delegate) {
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
            Component component = delegate.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            centerComponent(component);
            return component;
        }

        private void centerComponent(Component component) {
            if (component instanceof JLabel label) {
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setVerticalAlignment(SwingConstants.CENTER);
                label.setFont(new Font("Segoe UI", Font.BOLD, 15));
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
