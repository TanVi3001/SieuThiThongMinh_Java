package common.events;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * EventBus đơn giản, thread-safe.
 * Mặc định dispatch listener trên EDT (SwingUtilities.invokeLater).
 */
public final class EventBus {

    private static final Map<Class<?>, List<Consumer<?>>> LISTENERS = new ConcurrentHashMap<>();

    private EventBus() {}

    public static <T> AutoCloseable subscribe(Class<T> eventClass, Consumer<T> handler) {
        LISTENERS.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(handler);

        // trả về "subscription" để unsubscribe khi cần
        return () -> {
            List<Consumer<?>> list = LISTENERS.get(eventClass);
            if (list != null) list.remove(handler);
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> void publish(T event) {
        if (event == null) return;
        List<Consumer<?>> list = LISTENERS.get(event.getClass());
        if (list == null || list.isEmpty()) return;

        for (Consumer<?> raw : list) {
            Consumer<T> handler = (Consumer<T>) raw;
            // Đảm bảo UI update chạy đúng thread EDT
            SwingUtilities.invokeLater(() -> handler.accept(event));
        }
    }
}