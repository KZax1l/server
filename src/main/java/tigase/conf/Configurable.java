package tigase.conf;

import java.util.Map;

/**
 * Interface Configurable
 * Objects inheriting this interface can be configured. In Tigase system object can't request configuration properties.
 * Configuration of the object is passed to it at some time. Actually it can be passed at any time.
 * This allows dynamic system reconfiguration at runtime.
 */
public interface Configurable {
    /**
     * Get object name. This name corresponds to section in configuration.
     */
    String getName();

    /**
     * Sets all configuration properties for the object.
     */
    void setProperties(Map<String, ?> properties);

    /**
     * Returns defualt configuration settings for this object.
     */
    Map<String, ?> getDefaults();
}