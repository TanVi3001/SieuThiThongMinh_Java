package view.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Utility dùng chung để căn giữa các component dữ liệu trong dashboard.
 * Chỉ can thiệp UI renderer/alignment, không đổi logic nghiệp vụ hoặc realtime.
 */
public final class CenterAlignmentUtil {

    private CenterAlignmentUtil() {
    }

    public static void apply(Component root) {
        if (root == null) {
            return;
        }

        SwingUtilities.invokeLater(() -> applyRecursive(root));
    }

    private static void applyRecursive(Component component) {
        if (component == null) {
            return;
        }

        if (component instanceof JTable table) {
            centerTable(table);
        } else if (component instanceof JTextField textField) {
            textField.setHorizontalAlignment(JTextField.CENTER);
        } else if (component instanceof JComboBox<?> comboBox) {
            centerComboBox(comboBox);
        } else if (component instanceof JSpinner spinner) {
            centerSpinner(spinner);
        } else if (component instanceof JLabel label) {
            centerLabel(label);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyRecursive(child);
            }
        }
    }

    private static void centerTable(JTable table) {
        if (table == null) {
            return;
        }

        table.setRowHeight(Math.max(table.getRowHeight(), 36));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        centerRenderer.setVerticalAlignment(SwingConstants.CENTER);

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
            headerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
            headerRenderer.setVerticalAlignment(SwingConstants.CENTER);
            headerRenderer.setFont(header.getFont().deriveFont(Font.BOLD));
            headerRenderer.setBackground(header.getBackground());
            headerRenderer.setForeground(header.getForeground());
            header.setDefaultRenderer(headerRenderer);
            header.setReorderingAllowed(false);
        }
    }

    private static void centerComboBox(JComboBox<?> comboBox) {
        if (comboBox == null) {
            return;
        }

        comboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus
                );
                label.setHorizontalAlignment(SwingConstants.CENTER);
                return label;
            }
        });

        if (comboBox.isEditable()) {
            Component editor = comboBox.getEditor().getEditorComponent();
            if (editor instanceof JTextField textField) {
                textField.setHorizontalAlignment(JTextField.CENTER);
            }
        }
    }

    private static void centerSpinner(JSpinner spinner) {
        if (spinner == null) {
            return;
        }

        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor defaultEditor) {
            defaultEditor.getTextField().setHorizontalAlignment(JTextField.CENTER);
        }
    }

    private static void centerLabel(JLabel label) {
        if (label == null) {
            return;
        }

        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
    }
}
