package common.security;

import common.auth.UserSession;
import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import business.sql.rbac.AccountSql;
import view.LoginView;
import javax.swing.*;
import java.awt.Window;
import java.util.concurrent.atomic.AtomicBoolean; // IMPORT THÊM CÁI NÀY

public class SecurityGuard {

    // CHỐT CHẶN: Đánh dấu xem có đang trong quá trình bị "kick" ra không
    private static final AtomicBoolean isLoggingOut = new AtomicBoolean(false);

    public static void attach(JPanel view) {
        EventBus.subscribe(AppDataChangedEvent.class, event -> {
            if (event.getType() == AppEventType.ACCOUNT_SECURITY) {
                verifyCurrentSession(view);
            }
        });
    }

    private static void verifyCurrentSession(JPanel view) {
        // Nếu đã có 1 luồng đang thực hiện văng ra rồi thì chặn các luồng khác lại
        if (isLoggingOut.get()) {
            return;
        }

        UserSession session = UserSession.getInstance();
        if (!session.isLoggedIn()) {
            return;
        }

        String currentUserId = session.getUserId();
        String currentRole = session.getUserRole();

        new Thread(() -> {
            try {
                String[] latestData = AccountSql.getInstance().getAccountDetails(currentUserId);
                if (latestData == null) {
                    return;
                }

                String dbRoleId = latestData[4];
                boolean isActive = "0".equals(latestData[5]);

                if (!isActive || !dbRoleId.equals(currentRole)) {
                    // Dùng compareAndSet: Chỉ cái luồng chớp thời cơ nhanh nhất đầu tiên mới được phép chạy
                    if (isLoggingOut.compareAndSet(false, true)) {
                        SwingUtilities.invokeLater(() -> forceLogout(view));
                    }
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

        UserSession.getInstance().clear();

        // Reset lại chốt chặn để lần đăng nhập tiếp theo Vệ sĩ có thể làm việc tiếp
        isLoggingOut.set(false);

        Window window = SwingUtilities.getWindowAncestor(view);
        if (window != null) {
            window.dispose();
        }

        new LoginView().setVisible(true);
    }
}
