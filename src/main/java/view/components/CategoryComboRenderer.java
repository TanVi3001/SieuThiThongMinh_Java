package view.components;

import java.awt.*;
import javax.swing.*;

public class CategoryComboRenderer extends DefaultListCellRenderer {

    private final int iconSize;

    public CategoryComboRenderer() {
        this(20);
    }

    public CategoryComboRenderer(int iconSize) {
        this.iconSize = iconSize;
    }

    @Override
    public Component getListCellRendererComponent(
            JList<?> list, Object value, int index,
            boolean isSelected, boolean cellHasFocus) {

        super.getListCellRendererComponent(
                list, value, index, isSelected, cellHasFocus);

        if (value != null) {
            String text = value.toString();
            String categoryId = extractCategoryId(text);

            setIcon(IconHelper.getCategoryIcon(categoryId, iconSize));
            setText(text);
            setIconTextGap(8);
        } else {
            setIcon(null);
            setText("");
        }

        return this;
    }

    // Tách "CAT001" từ "CAT001 - Nước giải khát"
    private String extractCategoryId(String text) {
        if (text == null) return "default";
        String trimmed = text.trim();
        if (trimmed.contains(" - ")) {
            return trimmed.split(" - ")[0].trim();
        }
        return trimmed;
    }
}