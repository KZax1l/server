package tigase.conf;

import tigase.server.AbstractComponentRegistrator;
import tigase.server.XMPPService;

import java.util.Map;
import java.util.TreeMap;

public class Configurator extends AbstractComponentRegistrator implements XMPPService {
    public Configurator(String fileName) {
    }

    /**
     * Returns defualt configuration settings in case if there is no
     * config file.
     */
    public Map<String, String> getDefaults() {
        Map<String, String> defaults = new TreeMap<String, String>();
        defaults.put("tigase.message-router.id", "router");
        defaults.put("tigase.message-router.class", "tigase.server.MessageRouter");
        return defaults;
    }

}
