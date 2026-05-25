package view.util;

import business.service.RolePermissionService;
import java.awt.Component;
import java.awt.Container;
import javax.swing.AbstractButton;
import javax.swing.JPanel;

public final class RolePermissionButtonGuard {

    private RolePermissionButtonGuard() {
    }

    public static void applyTo(JPanel root) {
        if (root == null) {
            return;
        }
        applyToContainer(root);
    }

    private static void applyToContainer(Container container) {
        for (Component component : container.getComponents()) {
            if (component instanceof AbstractButton button) {
                applyToButton(button);
            }

            if (component instanceof Container child) {
                applyToContainer(child);
            }
        }
    }

    private static void applyToButton(AbstractButton button) {
        String text = button.getText();
        if (text == null) {
            return;
        }

        String normalized = normalize(text);

        if (isAddButton(normalized)) {
            lock(button, RolePermissionService.canAdd(), "Bạn không có quyền Thêm");
            return;
        }

        if (isEditButton(normalized)) {
            lock(button, RolePermissionService.canEdit(), "Bạn không có quyền Sửa/Cập nhật");
            return;
        }

        if (isDeleteButton(normalized)) {
            lock(button, RolePermissionService.canDelete(), "Bạn không có quyền Xóa");
            return;
        }

        if (isExportButton(normalized)) {
            lock(button, RolePermissionService.canExport(), "Bạn không có quyền Xuất file");
        }
    }

    private static void lock(AbstractButton button, boolean allowed, String tooltip) {
        button.setEnabled(allowed);
        button.setToolTipText(allowed ? null : tooltip);
    }

    private static boolean isAddButton(String text) {
        return text.contains("them")
                || text.contains("tao")
                || text.contains("themhoso")
                || text.contains("themncc")
                || text.contains("themnhacungcap");
    }

    private static boolean isEditButton(String text) {
        return text.contains("capnhat")
                || text.contains("sua")
                || text.contains("luu")
                || text.contains("huyphanca")
                || text.contains("capnhattrangthai");
    }

    private static boolean isDeleteButton(String text) {
        return text.contains("xoa")
                || text.contains("delete");
    }

    private static boolean isExportButton(String text) {
        return text.contains("xuat")
                || text.contains("export")
                || text.contains("excel")
                || text.contains("baocao")
                || text.contains("hoadon")
                || text.contains("doanhthu");
    }

    private static String normalize(String text) {
        String s = text.toLowerCase()
                .replace("đ", "d")
                .replace("Đ", "d");

        s = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");

        return s.replaceAll("[^a-z0-9]", "");
    }
}
