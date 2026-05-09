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

    // 🌟 GLOBAL GUARD FLAG: Cờ khóa chống gọi Logout nhiều lần (Chống 2 app, 2 popup)
    private static volatile boolean isProcessingLogout = false;

    // 👉 THÊM VÀO: Hàm để các file khác có thể ĐỌC được trạng thái cờ
    public static boolean isProcessingLogout() {
        return isProcessingLogout;
    }

    // 👉 ĐÃ SỬA: Hàm để các file khác có thể ĐỔI trạng thái cờ (Xóa cái ném lỗi của IDE đi)
    public static void setProcessingLogout(boolean value) {
        isProcessingLogout = value;
    }

    public static void attach(JPanel view) {
        // Reset cờ mỗi khi gắn guard mới (lúc login vào)
        isProcessingLogout = false;

        EventBus.subscribe(AppDataChangedEvent.class, event -> {
            // Nếu đang trong quá trình văng acc rồi thì bỏ qua mọi event khác
            if (isProcessingLogout) {
                return;
            }

            if (event.getType() == AppEventType.ACCOUNT_SECURITY) {
                System.out.println("🛡️ [SecurityGuard] Bắt được tín hiệu WebSocket đổi quyền!");
                verifyCurrentSession(view);
            }
        });
    }

    private static void verifyCurrentSession(JPanel view) {
        if (isProcessingLogout) {
            return;
        }

        Account currentUser = LoginService.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        String accId = currentUser.getAccountId();
        String currentRole = currentUser.getRoleId();

        new Thread(() -> {
            try {
                String[] latestData = AccountSql.getInstance().getAccountDetails(accId);

                if (latestData == null) {
                    return;
                }

                String dbRoleId = latestData[4];
                boolean isActive = "0".equals(latestData[5]);

                if (!isActive || !dbRoleId.equals(currentRole)) {
                    // 🌟 LOCK NGAY LẬP TỨC: Thằng nào chạy đến đây trước thì set cờ true
                    // Thằng Timer hay Listener thứ 2 chạy tới sẽ bị chặn đứng
                    if (!isProcessingLogout) {
                        isProcessingLogout = true;
                        System.out.println("🛡️ [SecurityGuard] 🚨 PHÁT HIỆN ĐỔI QUYỀN -> TIẾN HÀNH KICK!");
                        SwingUtilities.invokeLater(() -> forceLogout(view));
                    }
                }
            } catch (Exception e) {
                System.err.println("SecurityGuard Error: " + e.getMessage());
            }
        }).start();
    }

    private static void forceLogout(JPanel view) {
        // Chỉ hiện ĐÚNG 1 POPUP
        JOptionPane.showMessageDialog(view,
                "Quyền truy cập của bạn đã thay đổi hoặc tài khoản đã bị khóa.\nVui lòng đăng nhập lại để cập nhật!",
                "Cảnh báo bảo mật", JOptionPane.WARNING_MESSAGE);

        // Clear dữ liệu
        LoginService.logout();
        try {
            common.auth.UserSession.getInstance().clear();
            // CHUẨN BÀI: Dọn dẹp luôn EventBus (Nếu hàm clearAll của bạn có tồn tại, nếu không thì cứ comment lại)
            // common.events.EventBus.clearAll(); 
        } catch (Exception ignored) {
        }

        // Tắt cửa sổ hiện tại (Dashboard)
        Window window = SwingUtilities.getWindowAncestor(view);
        if (window != null) {
            window.dispose();
        }

        // Mở ĐÚNG 1 FRAME LOGIN
        LoginView login = new LoginView();
        login.setLocationRelativeTo(null);
        login.setVisible(true);
    }
}
