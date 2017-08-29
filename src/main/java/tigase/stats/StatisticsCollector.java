package tigase.stats;

import tigase.server.AbstractComponentRegistrator;
import tigase.server.XMPPService;

import java.util.Map;

public class StatisticsCollector extends AbstractComponentRegistrator implements XMPPService {
    public Map<String, String> getStatistics() {
        return null;
    }
}
