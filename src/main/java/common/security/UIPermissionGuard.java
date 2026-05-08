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
     * Hàm lấy quyền hạn của user đang đăng nhập từ Database
     * Trả về mảng: [canView, canAdd, canEdit, canDelete, canExport]
     */
    public static boolean[] getCurrentUserPermissions() {
        Account currentUser = LoginService.getCurrentUser();
        
        // Tùy theo code model Account của bạn, có thể là getRoleValue() hoặc getRoleId()
        if (currentUser == null || currentUser.getRoleValue() == null) {
            return new boolean[]{false, false, false, false, false}; // Mặc định chặn hết
        }

        String roleId = currentUser.getRoleValue();

        // Quản trị viên (Admin) mặc định có toàn quyền
        if ("R_ADMIN_ALL".equals(roleId)) {
            return new boolean[]{true, true, true, true, true};
        }

        boolean[] perms = new boolean[]{false, false, false, false, false};
        String sql = "SELECT can_view, can_add, can_edit, can_delete, can_export FROM ROLES WHERE role_id = ?";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, roleId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    perms[0] = rs.getInt("can_view") == 1;
                    perms[1] = rs.getInt("can_add") == 1;
                    perms[2] = rs.getInt("can_edit") == 1;
                    perms[3] = rs.getInt("can_delete") == 1;
                    perms[4] = rs.getInt("can_export") == 1;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi quét quyền bảo mật UI: " + e.getMessage());
        }
        return perms;
    }

    /**
     * BẢO VỆ GIAO DIỆN: Làm mờ nút hoặc khóa trang
     */
    public static JPanel protect(JPanel originalPanel) {
        boolean[] perms = getCurrentUserPermissions();

        // 1. NẾU KHÔNG CÓ QUYỀN XEM -> Trả về màn hình Khóa (Mờ)
        if (!perms[0]) {
            return createAccessDeniedPanel();
        }

        // 2. NẾU CÓ QUYỀN XEM -> Quét và làm mờ (Disable) các nút chức năng bị cấm
        disableUnauthorizedButtons(originalPanel, perms);

        return originalPanel;
    }

    private static void disableUnauthorizedButtons(Container container, boolean[] perms) {
        for (Component c : container.getComponents()) {
            if (c instanceof JButton) {
                JButton btn = (JButton) c;
                String text = btn.getText().toLowerCase();
                boolean shouldDisable = false;

                // Cấm Thêm
                if (!perms[1] && (text.contains("thêm") || text.contains("tạo") || text.contains("nhập hàng"))) {
                    shouldDisable = true;
                }
                // Cấm Sửa
                if (!perms[2] && (text.contains("sửa") || text.contains("cập nhật") || text.contains("lưu"))) {
                    shouldDisable = true;
                }
                // Cấm Xóa
                if (!perms[3] && (text.contains("xóa") || text.contains("hủy") || text.contains("thu hồi"))) {
                    shouldDisable = true;
                }
                // Cấm Xuất
                if (!perms[4] && (text.contains("xuất") || text.contains("in ") || text.contains("pdf") || text.contains("excel"))) {
                    shouldDisable = true;
                }

                if (shouldDisable) {
                    btn.setEnabled(false); // LÀM MỜ NÚT BẤM VÀ KHÔNG CHO CLICK
                    btn.setToolTipText("🔒 Tài khoản của bạn không có quyền thực hiện thao tác này");
                }

            } else if (c instanceof Container) {
                // Đệ quy đào sâu vào trong
                disableUnauthorizedButtons((Container) c, perms);
            }
        }
    }

    private static JPanel createAccessDeniedPanel() {
        JPanel lockPanel = new JPanel(new GridBagLayout());
        lockPanel.setBackground(new Color(244, 246, 250));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(new Color(244, 246, 250));

        // =====================================================================
        // SỬ DỤNG ICONHELPER ĐỂ LOAD ẢNH CHUẨN TỪ THƯ MỤC RESOURCES
        // =====================================================================
        JLabel iconLabel;
        try {
            // Nhờ IconHelper lấy hình cấm và resize ra 150x150
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
        // =====================================================================

        JLabel title = new JLabel("TRUY CẬP BỊ TỪ CHỐI", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(220, 53, 69)); // Màu đỏ cảnh báo
        title.setBorder(new EmptyBorder(20, 0, 10, 0));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("<html><center>Tài khoản của bạn không được cấp quyền xem dữ liệu ở phân hệ này.<br>Vui lòng liên hệ Quản trị viên nếu đây là một sai sót.</center></html>");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        subtitle.setForeground(new Color(100, 116, 139)); // Màu xám
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        content.add(iconLabel);
        content.add(title);
        content.add(subtitle);

        lockPanel.add(content);
        return lockPanel;
    }
}