package tigase.server.xmppcomponent;

import tigase.server.AbstractMessageReceiver;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.server.XMPPService;

import java.util.Queue;

public class ComponentConnectionManager extends AbstractMessageReceiver implements XMPPService {
    public ComponentConnectionManager(MessageReceiver parent) {
        super(parent);
    }

    @Override
    public Queue<Packet> processPacket(Packet packet) {
        return null;
    }
}
