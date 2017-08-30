package tigase.stats;

import java.util.List;

/**
 * Objects which inherits this type can return runtime statistics.
 * Any object can collect job statistics and implementing this interface guarantees that statistics will be presented in configured way to user who wants to see them.
 */
public interface StatisticsContainer {
    List<StatRecord> getStatistics();
}