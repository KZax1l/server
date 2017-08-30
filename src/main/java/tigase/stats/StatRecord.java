package tigase.stats;

public class StatRecord {
    private StatisticType type = null;
    private long longValue = -1;
    private int intValue = -1;

    private String description = null;
    private String unit = null;

    /**
     * Creates a new <code>StatRecord</code> instance.
     *
     * @param type  a <code>StatisticType</code> value
     * @param value a <code>long</code> value
     */
    public StatRecord(StatisticType type, long value) {
        this.type = type;
        longValue = value;
    }

    public StatRecord(StatisticType type, int value) {
        this.type = type;
        intValue = value;
    }

    public StatRecord(String description, String unit, int value) {
        this.description = description;
        this.unit = unit;
        intValue = value;
    }
}