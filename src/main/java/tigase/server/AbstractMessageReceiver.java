package tigase.server;

public class AbstractMessageReceiver implements MessageReceiver {
    @Override
    public String routingAddresses() {
        return null;
    }

    @Override
    public void addMessage(Packet packet) {

    }
}
