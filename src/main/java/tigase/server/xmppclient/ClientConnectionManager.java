package tigase.server.xmppclient;

import tigase.server.AbstractMessageReceiver;
import tigase.server.MessageReceiver;
import tigase.server.Packet;
import tigase.server.XMPPService;

import java.util.Queue;

public class ClientConnectionManager extends AbstractMessageReceiver implements XMPPService {
    public ClientConnectionManager(MessageReceiver parent) {
        super(parent);
    }

    @Override
    public Queue<Packet> processPacket(Packet packet) {
        return null;
    }
}
