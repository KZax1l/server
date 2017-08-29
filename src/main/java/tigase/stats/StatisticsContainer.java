package tigase.stats;

import java.util.Map;

/**
 * Objects which inherits this type can return runtime statistics.
 * Any object can collect job statistics and implementing this interface guarantees that statistics will be presented in configured way to user who wants to see them.
 */
public interface StatisticsContainer {
    Map<String, String> getStatistics();
}