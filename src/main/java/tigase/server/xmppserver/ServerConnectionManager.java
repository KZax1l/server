package tigase.server.xmppserver;

import tigase.conf.Configurable;
import tigase.server.AbstractMessageReceiver;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.server.XMPPService;
import tigase.stats.StatisticsContainer;

import java.util.Map;
import java.util.Queue;

public class ServerConnectionManager extends AbstractMessageReceiver implements Configurable, XMPPService, StatisticsContainer {
    public ServerConnectionManager(String[] addresses, int maxQueueSize, MessageReceiver parent) {
        super(addresses, maxQueueSize, parent);
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
    public Queue<Packet> processPacket(Packet packet) {
        return null;
    }
}
