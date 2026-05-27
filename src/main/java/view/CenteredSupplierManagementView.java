package view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

/**
 * Wrapper riêng cho Warehouse Portal: căn giữa + phóng to nhẹ màn nhà cung cấp.
 * Không đổi logic thêm/sửa/xóa/lọc của SupplierManagementView.
 */
public class CenteredSupplierManagementView extends SupplierManagementView {

    public CenteredSupplierManagementView() {
        super(SupplierViewMode.WAREHOUSE);
        SwingUtilities.invokeLater(this::applyWarehouseSupplierStyling);
    }

    private void applyWarehouseSupplierStyling() {
        enlargeFormAndButtons(this);

        JTable table = findSupplierTable(this);
        if (table == null || table.getColumnCount() < 6) {
            revalidate();
            repaint();
            return;
        }

        table.setRowHeight(Math.max(table.getRowHeight(), 42));
        table.setFont(new Font("Segoe UI", Font.BOLD, 14));
        centerHeader(table);

        // Bảng nhà cung cấp: căn giữa toàn bộ cột vì không có cột ảnh/icon.
        for (int i = 0; i < table.getColumnCount(); i++) {
            centerColumn(table, i);
        }

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
        headerRenderer.setFont(new Font("Segoe UI", Font.BOLD, 14));
        headerRenderer.setBackground(table.getTableHeader().getBackground());
        headerRenderer.setForeground(table.getTableHeader().getForeground());
        headerRenderer.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 10, 8, 10));
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

        table.getColumnModel().getColumn(columnIndex).setCellRenderer(new StrongCenteredRenderer());
    }

    private static class StrongCenteredRenderer extends DefaultTableCellRenderer {

        StrongCenteredRenderer() {
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
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));

            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(new java.awt.Color(23, 52, 99));
            }
            return this;
        }
    }
}
