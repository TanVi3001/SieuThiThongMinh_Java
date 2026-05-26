package common.security;

import business.service.LoginService;
import common.db.DatabaseConnection;
import model.account.Account;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UIPermissionGuard {

    /**
     * Hàm lấy quyền hạn của user đang đăng nhập từ Database Trả về mảng:
     * [canView, canAdd, canEdit, canDelete, canExport]
     */
    public static boolean[] getCurrentUserPermissions() {
        Account currentUser = LoginService.getCurrentUser();

        if (currentUser == null || currentUser.getRoleValue() == null) {
            return new boolean[]{false, false, false, false, false};
        }

        String roleId = currentUser.getRoleValue();

        boolean[] perms = new boolean[]{false, false, false, false, false};
        String sql = """
            SELECT can_view,
                   can_add,
                   can_edit,
                   can_delete,
                   can_export
            FROM ROLES
            WHERE role_id = ?
              AND NVL(is_deleted, 0) = 0
        """;

        try (
                Connection con = DatabaseConnection.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, roleId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    perms[0] = rs.getInt("can_view") == 1;
                    perms[1] = rs.getInt("can_add") == 1;
                    perms[2] = rs.getInt("can_edit") == 1;
                    perms[3] = rs.getInt("can_delete") == 1;
                    perms[4] = rs.getInt("can_export") == 1;
                    return perms;
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi quét quyền bảo mật UI: " + e.getMessage());
        }

        /*
         * Fallback an toàn cho Admin:
         * Nếu bảng ROLES bị thiếu dòng R_ADMIN_ALL thì vẫn cho Admin vào app,
         * nhưng bình thường quyền Admin phải đọc từ DB để ma trận phân quyền có hiệu lực.
         */
        if ("R_ADMIN_ALL".equals(roleId)) {
            return new boolean[]{true, true, true, true, true};
        }

        return perms;
    }

    /**
     * BẢO VỆ GIAO DIỆN: Làm mờ nút hoặc khóa trang.
     *
     * Lưu ý: - SellPanel / Bán hàng POS là nghiệp vụ riêng, không áp dụng CRUD
     * guard. - Các component có client property "permission.guard.skip" = true
     * cũng được bỏ qua.
     */
    public static JPanel protect(JPanel originalPanel) {
        if (originalPanel == null) {
            return null;
        }

        if (shouldSkipPermissionGuard(originalPanel)) {
            return originalPanel;
        }

        boolean[] perms = getCurrentUserPermissions();

        if (!perms[0]) {
            return createAccessDeniedPanel();
        }

        disableUnauthorizedButtons(originalPanel, perms);

        return originalPanel;
    }

    private static void disableUnauthorizedButtons(Container container, boolean[] perms) {
        if (container == null || shouldSkipPermissionGuard(container)) {
            return;
        }

        for (Component c : container.getComponents()) {
            if (shouldSkipPermissionGuard(c)) {
                continue;
            }

            if (c instanceof JButton btn) {
                String text = btn.getText() == null ? "" : btn.getText().toLowerCase().trim();
                boolean shouldDisable = false;

                if (!perms[1] && isAddAction(text)) {
                    shouldDisable = true;
                }

                if (!perms[2] && isEditAction(text)) {
                    shouldDisable = true;
                }

                if (!perms[3] && isDeleteAction(text)) {
                    shouldDisable = true;
                }

                if (!perms[4] && isExportAction(text)) {
                    shouldDisable = true;
                }

                if (shouldDisable) {
                    btn.setEnabled(false);
                    btn.setToolTipText("🔒 Tài khoản của bạn không có quyền thực hiện thao tác này");
                } else {
                    /*
                     * Nếu trước đó bị disable bởi guard rồi sau đó quyền được bật lại,
                     * cần mở lại để realtime/refresh quyền có tác dụng.
                     */
                    if ("🔒 Tài khoản của bạn không có quyền thực hiện thao tác này".equals(btn.getToolTipText())) {
                        btn.setToolTipText(null);
                    }
                    btn.setEnabled(true);
                }

            } else if (c instanceof Container child) {
                disableUnauthorizedButtons(child, perms);
            }
        }
    }

    private static boolean shouldSkipPermissionGuard(Component c) {
        if (c == null) {
            return false;
        }

        /*
         * Skip cứng cho màn Bán hàng POS.
         * Đây là nghiệp vụ thao tác giỏ hàng tạm, không phải CRUD database.
         */
        Class<?> clazz = c.getClass();

        while (clazz != null) {
            if ("view.SellPanel".equals(clazz.getName())) {
                return true;
            }

            clazz = clazz.getSuperclass();
        }

        if (c instanceof JComponent jc) {
            Object skip = jc.getClientProperty("permission.guard.skip");

            if (Boolean.TRUE.equals(skip)) {
                return true;
            }
        }

        return false;
    }

    private static boolean isAddAction(String text) {
        return text.contains("thêm")
                || text.contains("tạo")
                || text.contains("nhập hàng");
    }

    private static boolean isEditAction(String text) {
        return text.contains("sửa")
                || text.contains("cập nhật")
                || text.contains("lưu");
    }

    private static boolean isDeleteAction(String text) {
        return text.contains("xóa")
                || text.contains("xoá")
                || text.contains("hủy")
                || text.contains("huỷ")
                || text.contains("thu hồi");
    }

    private static boolean isExportAction(String text) {
        return text.contains("xuất")
                || text.contains("in ")
                || text.contains("pdf")
                || text.contains("excel");
    }

    private static JPanel createAccessDeniedPanel() {
        JPanel lockPanel = new JPanel(new GridBagLayout());
        lockPanel.setBackground(new Color(244, 246, 250));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(new Color(244, 246, 250));

        JLabel iconLabel;

        try {
            ImageIcon bigIcon = view.components.IconHelper.accessDenied(150);

            if (bigIcon != null) {
                iconLabel = new JLabel(bigIcon, SwingConstants.CENTER);
            } else {
                throw new Exception("IconHelper trả về null (Không tìm thấy ảnh)");
            }

        } catch (Exception e) {
            System.err.println("🛡️ [UIPermissionGuard] ❌ Lỗi gọi IconHelper: " + e.getMessage());
            iconLabel = new JLabel("🚫", SwingConstants.CENTER);
            iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 100));
            iconLabel.setForeground(new Color(220, 53, 69));
        }

        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("TRUY CẬP BỊ TỪ CHỐI", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(220, 53, 69));
        title.setBorder(new EmptyBorder(20, 0, 10, 0));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel(
                "<html><center>"
                + "Tài khoản của bạn không được cấp quyền xem dữ liệu ở phân hệ này."
                + "<br>Vui lòng liên hệ Quản trị viên nếu đây là một sai sót."
                + "</center></html>"
        );

        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitle.setForeground(new Color(100, 116, 139));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(iconLabel);
        content.add(title);
        content.add(subtitle);

        lockPanel.add(content);

        return lockPanel;
    }
}
