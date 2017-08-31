package tigase.server.xmppsession;

import tigase.conf.Configurable;
import tigase.server.AbstractMessageReceiver;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.server.XMPPService;

import java.util.Queue;

public class SessionManager extends AbstractMessageReceiver implements Configurable, XMPPService {
    public SessionManager(MessageReceiver parent) {
        super(parent);
    }

    @Override
    public Queue<Packet> processPacket(Packet packet) {
        return null;
    }
}
