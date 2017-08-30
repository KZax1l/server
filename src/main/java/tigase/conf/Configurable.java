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
     * Get object id. This id corresponds to entry in configuration.
     */
    String getName();

    /**
     * Sets configuration property to object.
     */
    void setProperty(String name, String value);

    /**
     * Sets all configuration properties for object.
     */
    void setProperties();

    /**
     * Returns defualt configuration settings for this object.
     */
    Map<String, String> getDefaults();
}