package common.realtime;

import common.events.AppDataChangedEvent;
import common.events.AppEventType;
import common.events.EventBus;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;

/**
 * Helper gắn realtime đúng event cho mỗi panel.
 *
 * Cách dùng trong panel:
 *
 * private PanelRealtimeSupport realtime;
 *
 * realtime = new PanelRealtimeSupport( this, 800, this::loadDataAsync,
 * AppEventType.PRODUCT_CHANGED, AppEventType.INVENTORY_CHANGED );
 *
 * Khi panel bị dispose/logout: realtime.close();
 */
public final class PanelRealtimeSupport implements AutoCloseable {

    private final Set<AppEventType> acceptedTypes;
    private final DebouncedReloader reloader;
    private AutoCloseable subscription;

    public PanelRealtimeSupport(
            JComponent owner,
            int debounceMs,
            Runnable reloadAsync,
            AppEventType... acceptedTypes
    ) {
        this.acceptedTypes = new HashSet<>(Arrays.asList(acceptedTypes));
        this.reloader = new DebouncedReloader(owner, debounceMs, reloadAsync);

        this.subscription = EventBus.subscribe(AppDataChangedEvent.class, event -> {
            if (event == null || event.getType() == null) {
                return;
            }

            if (this.acceptedTypes.contains(event.getType())) {
                reloader.requestReload();
            }
        });
    }

    public void onPanelShown() {
        reloader.onPanelShown();
    }

    public void onPanelHidden() {
        reloader.onPanelHidden();
    }

    public void markDirty() {
        reloader.markDirty();
    }

    @Override
    public void close() {
        try {
            if (subscription != null) {
                subscription.close();
            }
        } catch (Exception ignored) {
        }

        reloader.close();
    }
}
