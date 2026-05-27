package view.components;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Panel wrapper tạo hiệu ứng xuất hiện nhẹ cho biểu đồ/dashboard component.
 * Không thay đổi dữ liệu hay logic realtime; chỉ can thiệp paint UI.
 */
public class AnimatedRevealPanel extends JPanel {

    private static final int DEFAULT_DURATION_MS = 520;
    private static final int DEFAULT_DELAY_MS = 12;

    private final int durationMs;
    private final int delayMs;
    private long startTime;
    private float progress;
    private Timer timer;

    public AnimatedRevealPanel(Component child) {
        this(child, DEFAULT_DURATION_MS, DEFAULT_DELAY_MS);
    }

    public AnimatedRevealPanel(Component child, int durationMs, int delayMs) {
        super(new BorderLayout());
        this.durationMs = Math.max(160, durationMs);
        this.delayMs = Math.max(0, delayMs);
        this.progress = 0f;
        setOpaque(false);
        if (child != null) {
            add(child, BorderLayout.CENTER);
        }
    }

    @Override
    public void addNotify() {
        super.addNotify();
        restartAnimation();
    }

    public void restartAnimation() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        progress = 0f;
        startTime = System.currentTimeMillis() + delayMs;

        timer = new Timer(16, e -> {
            long now = System.currentTimeMillis();
            if (now < startTime) {
                repaint();
                return;
            }

            float raw = (now - startTime) / (float) durationMs;
            progress = Math.min(1f, raw);

            if (progress >= 1f) {
                progress = 1f;
                timer.stop();
            }

            repaint();
        });
        timer.setCoalesce(true);
        timer.start();
        repaint();
    }

    @Override
    protected void paintChildren(Graphics g) {
        if (progress >= 1f) {
            super.paintChildren(g);
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float eased = easeOutCubic(progress);
            float alpha = Math.max(0.08f, eased);
            int slideY = Math.round((1f - eased) * 18f);

            g2.translate(0, slideY);
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
            super.paintChildren(g2);
        } finally {
            g2.dispose();
        }
    }

    private float easeOutCubic(float t) {
        float x = 1f - Math.max(0f, Math.min(1f, t));
        return 1f - x * x * x;
    }
}
