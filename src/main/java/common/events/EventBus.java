package common.events;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * EventBus don gian, thread-safe. Mac dinh dispatch listener tren EDT.
 */
public final class EventBus {

    private static final Map<Class<?>, List<Consumer<?>>> LISTENERS = new ConcurrentHashMap<>();

    private EventBus() {
    }

    public static <T> AutoCloseable subscribe(Class<T> eventClass, Consumer<T> handler) {
        LISTENERS.computeIfAbsent(eventClass, k -> new CopyOnWriteArrayList<>()).add(handler);

        return () -> {
            List<Consumer<?>> list = LISTENERS.get(eventClass);
            if (list != null) {
                list.remove(handler);
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> void publish(T event) {
        if (event == null) {
            return;
        }

        List<Consumer<?>> list = LISTENERS.get(event.getClass());
        if (list == null || list.isEmpty()) {
            return;
        }

        for (Consumer<?> raw : list) {
            Consumer<T> handler = (Consumer<T>) raw;
            SwingUtilities.invokeLater(() -> handler.accept(event));
        }
    }

    /**
     * Don listener khi logout/chuyen man de tranh listener cu giu reference gay lag.
     */
    public static void clearAll() {
        LISTENERS.clear();
    }
}
