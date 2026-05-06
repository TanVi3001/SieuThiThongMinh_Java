package common.security;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import business.sql.rbac.AccountSql;
import business.service.LoginService;
import model.account.Account;
import view.LoginView;
import javax.swing.*;
import java.awt.Window;

public class SecurityGuard {

    public static void attach(JPanel view) {
        EventBus.subscribe(AppDataChangedEvent.class, event -> {
            System.out.println("🛡️ [SecurityGuard] Bắt được tín hiệu WebSocket: " + event.getType());

            if (event.getType() == AppEventType.ACCOUNT_SECURITY) {
                verifyCurrentSession(view);
            }
        });
    }

    private static void verifyCurrentSession(JPanel view) {
        Account currentUser = LoginService.getCurrentUser();

        if (currentUser == null) {
            System.out.println("🛡️ [SecurityGuard] Chưa đăng nhập, không cần check.");
            return;
        }

        // Lấy Account ID thực sự của người dùng.
        // NẾU CHỖ NÀY BỊ GẠCH ĐỎ: Hãy đổi .getAccountId() thành .getId() hoặc .getUsername() tùy theo ông đang đặt tên hàm trong model Account là gì nhé.
        String accId = currentUser.getAccountId();
        String currentRole = currentUser.getRoleId();

        System.out.println("🛡️ [SecurityGuard] Đang check ID: " + accId + " | Role hiện tại trong App: " + currentRole);

        new Thread(() -> {
            try {
                String[] latestData = AccountSql.getInstance().getAccountDetails(accId);

                if (latestData == null) {
                    System.out.println("🛡️ [SecurityGuard] ❌ Lỗi: Không tìm thấy ID " + accId + " dưới DB! (Truyền sai mã)");
                    return;
                }

                String dbRoleId = latestData[4]; // Cột Role ID từ DB
                boolean isActive = "0".equals(latestData[5]); // Trạng thái hoạt động

                System.out.println("🛡️ [SecurityGuard] Role dưới DB: " + dbRoleId + " | Active: " + isActive);

                // NẾU ROLE DƯỚI DB KHÁC ROLE ĐANG LƯU TRONG APP -> KICK NGAY!
                if (!isActive || !dbRoleId.equals(currentRole)) {
                    System.out.println("🛡️ [SecurityGuard] 🚨 PHÁT HIỆN SAI LỆCH QUYỀN -> KICK VĂNG RA!");
                    SwingUtilities.invokeLater(() -> forceLogout(view));
                } else {
                    System.out.println("🛡️ [SecurityGuard] ✅ Mọi thứ bình thường.");
                }
            } catch (Exception e) {
                System.err.println("SecurityGuard Error: " + e.getMessage());
            }
        }).start();
    }

    private static void forceLogout(JPanel view) {
        JOptionPane.showMessageDialog(view,
                "Quyền truy cập của bạn đã thay đổi hoặc tài khoản đã bị khóa.\nVui lòng đăng nhập lại để cập nhật!",
                "Cảnh báo bảo mật", JOptionPane.WARNING_MESSAGE);

        LoginService.logout();

        // Dùng try-catch phòng hờ UserSession không tồn tại
        try {
            common.auth.UserSession.getInstance().clear();
        } catch (Exception ignored) {
        }

        Window window = SwingUtilities.getWindowAncestor(view);
        if (window != null) {
            window.dispose();
        }
        new LoginView().setVisible(true);
    }
}
