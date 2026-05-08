package view.components;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;

public class CategoryTableRenderer extends DefaultTableCellRenderer {

    private final int iconSize;

    // Constructor mặc định size 24
    public CategoryTableRenderer() {
        this(24);
    }

    public CategoryTableRenderer(int iconSize) {
        this.iconSize = iconSize;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {

        // Gọi super để giữ màu nền selected/hover đúng
        super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        if (value != null) {
            String text = value.toString();

            // Tách categoryId từ text dạng "CAT001 - Nước giải khát"
            // hoặc dùng thẳng nếu chỉ là "CAT001"
            String categoryId = extractCategoryId(text);

            setIcon(IconHelper.getCategoryIcon(categoryId, iconSize));
            setText(text); // Hiển thị text đầy đủ
        } else {
            setIcon(null);
            setText("");
        }

        setHorizontalAlignment(SwingConstants.LEFT);
        setIconTextGap(8); // Khoảng cách icon với text
        return this;
    }

    // Tách "CAT001" từ "CAT001 - Nước giải khát"
    private String extractCategoryId(String text) {
        if (text == null) return "default";
        String trimmed = text.trim();
        if (trimmed.contains(" - ")) {
            return trimmed.split(" - ")[0].trim();
        }
        return trimmed; // Trường hợp chỉ có "CAT001"
    }
}