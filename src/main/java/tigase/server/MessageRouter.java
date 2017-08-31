package tigase.server;

import tigase.conf.Configurable;
import tigase.stats.StatisticsContainer;

import java.util.Map;
import java.util.Queue;

public class MessageRouter extends AbstractMessageReceiver implements Configurable, XMPPService, StatisticsContainer {
    public MessageRouter(String[] addresses, int maxQueueSize, MessageReceiver parent) {
        super(addresses, maxQueueSize, parent);
    }

    @Override
    public void setProperty(String name, String value) {

    }

    @Override
    public void setProperties(Map<String, String> properties) {

    }

    @Override
    public Map<String, String> getDefaults() {
        return null;
    }

    @Override
    public Queue<Packet> processPacket(Packet packet) {
        return null;
    }
}
