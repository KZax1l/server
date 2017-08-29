package tigase.server.xmppclient;

import tigase.conf.Configurable;
import tigase.server.AbstractComponentRegistrator;
import tigase.server.XMPPService;
import tigase.stats.StatisticsContainer;

import java.util.Map;

public class ClientConnectionManager extends AbstractComponentRegistrator implements Configurable, XMPPService, StatisticsContainer {
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
