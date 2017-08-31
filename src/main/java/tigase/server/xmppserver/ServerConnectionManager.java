package tigase.server.xmppserver;

import tigase.server.AbstractMessageReceiver;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.server.XMPPService;

import java.util.Queue;

public class ServerConnectionManager extends AbstractMessageReceiver implements XMPPService {
    public ServerConnectionManager(MessageReceiver parent) {
        super(parent);
    }

    @Override
    public Queue<Packet> processPacket(Packet packet) {
        return null;
    }
}
