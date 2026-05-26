package common.realtime;

import java.awt.Component;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Debounce reload cho panel realtime.
 *
 * Nguyên tắc: - Event đến liên tục thì chỉ reload 1 lần sau delay. - Panel
 * không hiển thị thì không query DB, chỉ đánh dấu dirty. - Khi panel show lại,
 * gọi onPanelShown() để reload nếu dirty.
 */
public final class DebouncedReloader implements AutoCloseable {

    private final JComponent owner;
    private final Runnable reloadTask;
    private final int delayMs;
    private final Timer timer;

    private boolean dirty = false;
    private boolean closed = false;

    public DebouncedReloader(JComponent owner, int delayMs, Runnable reloadTask) {
        this.owner = owner;
        this.delayMs = Math.max(250, delayMs);
        this.reloadTask = reloadTask;

        final Timer localTimer = new Timer(this.delayMs, null);

        localTimer.addActionListener(e -> {
            localTimer.stop();
            runIfVisible();
        });

        localTimer.setRepeats(false);

        this.timer = localTimer;
    }

    public void requestReload() {
        if (closed) {
            return;
        }

        if (!isOwnerVisible()) {
            dirty = true;
            return;
        }

        dirty = false;

        if (SwingUtilities.isEventDispatchThread()) {
            timer.restart();
        } else {
            SwingUtilities.invokeLater(() -> timer.restart());
        }
    }

    public void markDirty() {
        if (!closed) {
            dirty = true;
        }
    }

    public void onPanelShown() {
        if (closed) {
            return;
        }

        if (dirty) {
            requestReload();
        }
    }

    public void onPanelHidden() {
        if (!closed) {
            dirty = true;
            timer.stop();
        }
    }

    private void runIfVisible() {
        if (closed) {
            return;
        }

        if (!isOwnerVisible()) {
            dirty = true;
            return;
        }

        dirty = false;

        if (reloadTask != null) {
            reloadTask.run();
        }
    }

    private boolean isOwnerVisible() {
        if (owner == null || !owner.isShowing()) {
            return false;
        }

        Component current = owner;

        while (current != null) {
            if (!current.isVisible()) {
                return false;
            }

            current = current.getParent();
        }

        return true;
    }

    @Override
    public void close() {
        closed = true;
        timer.stop();
    }
}
