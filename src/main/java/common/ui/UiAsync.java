package common.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Helper chạy tác vụ nặng ngoài EDT để UI Swing không bị đứng.
 *
 * Dùng cho: - SQL insert/update/delete - load bảng JTable - export
 * PDF/JasperReport - import CSV
 *
 * Lưu ý: - backgroundTask chạy trong doInBackground(). - onSuccess/onError chạy
 * lại trên EDT trong done().
 */
public final class UiAsync {

    private UiAsync() {
    }

    public static void run(
            JComponent owner,
            String loadingText,
            Runnable backgroundTask,
            Runnable onSuccess
    ) {
        run(owner, loadingText, () -> {
            backgroundTask.run();
            return null;
        }, ignored -> {
            if (onSuccess != null) {
                onSuccess.run();
            }
        }, null);
    }

    public static <T> void run(
            JComponent owner,
            String loadingText,
            Supplier<T> backgroundTask,
            Consumer<T> onSuccess,
            Consumer<Exception> onError
    ) {
        if (owner == null || backgroundTask == null) {
            return;
        }

        List<JButton> buttons = collectButtons(owner);

        SwingUtilities.invokeLater(() -> {
            owner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            buttons.forEach(btn -> btn.setEnabled(false));
            owner.putClientProperty("loadingText", loadingText == null ? "Đang xử lý..." : loadingText);
        });

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            private Exception error;

            @Override
            protected T doInBackground() {
                try {
                    return backgroundTask.get();
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void done() {
                owner.setCursor(Cursor.getDefaultCursor());
                buttons.forEach(btn -> btn.setEnabled(true));
                owner.putClientProperty("loadingText", null);

                if (error != null) {
                    error.printStackTrace();

                    if (onError != null) {
                        onError.accept(error);
                    } else {
                        JOptionPane.showMessageDialog(
                                owner,
                                "Có lỗi xảy ra:\n" + rootMessage(error),
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }

                    return;
                }

                try {
                    T result = get();
                    if (onSuccess != null) {
                        onSuccess.accept(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    if (onError != null) {
                        onError.accept(e);
                    } else {
                        JOptionPane.showMessageDialog(
                                owner,
                                "Có lỗi xảy ra:\n" + rootMessage(e),
                                "Lỗi",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                }
            }
        };

        worker.execute();
    }

    private static List<JButton> collectButtons(Container root) {
        List<JButton> result = new ArrayList<>();
        collectButtonsRecursive(root, result);
        return result;
    }

    private static void collectButtonsRecursive(Container root, List<JButton> result) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton btn) {
                result.add(btn);
            }

            if (c instanceof Container child) {
                collectButtonsRecursive(child, result);
            }
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        return t.getMessage() == null ? t.toString() : t.getMessage();
    }
}
