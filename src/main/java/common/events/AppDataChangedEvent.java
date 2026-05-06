package common.events;

public final class AppDataChangedEvent implements AppEvent {
    private final AppEventType type;
    private final String message;

    public AppDataChangedEvent(AppEventType type, String message) {
        this.type = (type == null) ? AppEventType.UNKNOWN : type;
        this.message = message;
    }

    public AppEventType getType() { return type; }
    public String getMessage() { return message; }
}