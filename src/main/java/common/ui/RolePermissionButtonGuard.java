package common.ui;

import business.service.RolePermissionService;
import java.awt.Component;
import java.awt.Container;
import java.text.Normalizer;
import javax.swing.AbstractButton;
import javax.swing.JButton;

public final class RolePermissionButtonGuard {

    private RolePermissionButtonGuard() {
    }

    public static void apply(Container root) {
        if (root == null) {
            return;
        }

        applyRecursive(root);
    }

    private static void applyRecursive(Component component) {
        if (component instanceof JButton button) {
            applyToButton(button);
        }

        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyRecursive(child);
            }
        }
    }

    private static void applyToButton(AbstractButton button) {
        String text = normalize(button.getText());

        if (text.isBlank()) {
            return;
        }

        if (isExportButton(text)) {
            lockIfNeeded(button, RolePermissionService.canExport(), "Bạn không có quyền Xuất file");
            return;
        }

        if (isDeleteButton(text)) {
            lockIfNeeded(button, RolePermissionService.canDelete(), "Bạn không có quyền Xóa");
            return;
        }

        if (isAddButton(text)) {
            lockIfNeeded(button, RolePermissionService.canAdd(), "Bạn không có quyền Thêm");
            return;
        }

        if (isEditButton(text)) {
            lockIfNeeded(button, RolePermissionService.canEdit(), "Bạn không có quyền Sửa/Cập nhật");
        }
    }

    private static boolean isAddButton(String text) {
        return text.startsWith("them")
                || text.contains(" them ")
                || text.contains("them ho so")
                || text.contains("them phan ca")
                || text.contains("them nha cung cap")
                || text.contains("tao moi")
                || text.contains("tao hoa don")
                || text.contains("lap phieu");
    }

    private static boolean isEditButton(String text) {
        return text.startsWith("cap nhat")
                || text.startsWith("sua")
                || text.contains(" cap nhat")
                || text.contains(" chinh sua")
                || text.contains("huy phan ca")
                || text.contains("cap nhat trang thai")
                || text.contains("luu thay doi")
                || text.contains("luu toan bo thay doi");
    }

    private static boolean isDeleteButton(String text) {
        return text.startsWith("xoa")
                || text.contains(" xoa")
                || text.contains("xoa ho so")
                || text.contains("xoa nha cung cap")
                || text.contains("xoa lich");
    }

    private static boolean isExportButton(String text) {
        return text.startsWith("xuat")
                || text.contains(" xuat")
                || text.contains("excel")
                || text.contains("bao cao")
                || text.contains("hoa don pdf")
                || text.contains("xuat hoa don")
                || text.contains("xuat excel")
                || text.contains("xuat bao cao")
                || text.contains("doanh thu");
    }

    private static void lockIfNeeded(AbstractButton button, boolean allowed, String tooltip) {
        if (allowed) {
            return;
        }

        button.setEnabled(false);
        button.setToolTipText(tooltip);
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }

        String text = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replace('Đ', 'D')
                .replace('đ', 'd')
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", " ")
                .trim();

        return " " + text + " ";
    }
}
