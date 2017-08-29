package tigase.server;

import tigase.conf.Configurable;
import tigase.stats.StatisticsContainer;

import java.util.Map;

public class MessageRouter extends AbstractComponentRegistrator implements Configurable, XMPPService, StatisticsContainer {
    @Override
    public String getId() {
        return null;
    }

    @Override
    public void setProperty(String name, String value) {

    }

    @Override
    public void setProperties() {

    }

    @Override
    public Map<String, String> getDefaults() {
        return null;
    }

    @Override
    public Map<String, String> getStatistics() {
        return null;
    }
}
