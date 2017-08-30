package tigase.stats;

public enum StatisticType {
    QUEUE_SIZE("Queue size", "int"),
    MSG_RECEIVED_OK("Messages received", "long"),
    QUEUE_OVERFLOW("Queue overflow", "long"),
    Other(null, null);

    private String description = null;
    private String unit = null;

    private StatisticType(String description, String unit) {
        this.description = description;
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public String getUnit() {
        return unit;
    }
}