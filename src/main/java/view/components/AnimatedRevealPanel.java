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
 * Wrapper tạo hiệu ứng chart trượt mạnh khi mới vào màn hình. Không đổi dữ
 * liệu, không đổi realtime, chỉ can thiệp phần hiển thị.
 */
public class AnimatedRevealPanel extends JPanel {

    private static final int DEFAULT_DURATION_MS = 850;
    private static final int DEFAULT_DELAY_MS = 0;
    private static final int DEFAULT_SLIDE_DISTANCE = 95;

    private final int durationMs;
    private final int delayMs;
    private final int slideDistance;

    private long startTime;
    private float progress;
    private Timer timer;

    public AnimatedRevealPanel(Component child) {
        this(child, DEFAULT_DURATION_MS, DEFAULT_DELAY_MS, DEFAULT_SLIDE_DISTANCE);
    }

    public AnimatedRevealPanel(Component child, int durationMs, int delayMs) {
        this(child, durationMs, delayMs, DEFAULT_SLIDE_DISTANCE);
    }

    public AnimatedRevealPanel(Component child, int durationMs, int delayMs, int slideDistance) {
        super(new BorderLayout());

        this.durationMs = Math.max(250, durationMs);
        this.delayMs = Math.max(0, delayMs);
        this.slideDistance = Math.max(20, slideDistance);
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

            float eased = easeOutBack(progress);

            float alpha = Math.min(1f, Math.max(0.15f, progress * 1.25f));
            int slideY = Math.round((1f - eased) * slideDistance);

            g2.translate(0, slideY);
            g2.setComposite(AlphaComposite.SrcOver.derive(alpha));

            super.paintChildren(g2);

        } finally {
            g2.dispose();
        }
    }

    private float easeOutBack(float t) {
        t = Math.max(0f, Math.min(1f, t));

        float c1 = 1.70158f;
        float c3 = c1 + 1f;

        return 1f + c3 * (float) Math.pow(t - 1f, 3)
                + c1 * (float) Math.pow(t - 1f, 2);
    }
}
