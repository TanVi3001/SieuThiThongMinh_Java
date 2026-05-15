package common.window;

import business.service.AccountService;
import business.service.SessionManager;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import model.account.Account;

public class AppCloseHandler {

    private AppCloseHandler() {
    }

    public static void register(JFrame frame) {
        if (frame == null) {
            return;
        }

        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmAndExit(frame);
            }
        });
    }

    private static void confirmAndExit(JFrame frame) {
        int confirm = JOptionPane.showConfirmDialog(
                frame,
                "Bạn có chắc muốn thoát ứng dụng?",
                "Xác nhận thoát",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        setCurrentUserOffline();

        frame.dispose();
        System.exit(0);
    }

    public static void setCurrentUserOffline() {
        try {
            Account currentUser = SessionManager.getCurrentUser();

            if (currentUser != null && currentUser.getAccountId() != null) {
                AccountService.setOffline(currentUser.getAccountId());
            }

        } catch (Exception e) {
            System.err.println("[AppCloseHandler] Không thể set OFFLINE: " + e.getMessage());
        }
    }
}