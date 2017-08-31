package tigase.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class MessageRouter extends AbstractMessageReceiver implements XMPPService {
    private List<ServerComponent> components = new ArrayList<ServerComponent>();
    private List<ComponentRegistrator> registrators = new ArrayList<ComponentRegistrator>();
    private List<MessageReceiver> receivers = new ArrayList<MessageReceiver>();

    public MessageRouter() {
        super(null);
        components.add(this);
    }

    @Override
    public Queue<Packet> processPacket(Packet packet) {
        return null;
    }

    public void addRegistrator(ComponentRegistrator registr) {
        registrators.add(registr);
        addComponent(registr);
        for (ServerComponent comp : components) {
            if (comp != registr) {
                registr.addComponent(comp);
            } // end of if (comp != registr)
        } // end of for (ServerComponent comp : components)
    }

    public void addRouter(MessageReceiver receiver) {
        addComponent(receiver);
        receivers.add(receiver);
    }

    public void addComponent(ServerComponent component) {
        for (ComponentRegistrator registr : registrators) {
            if (registr != component) {
                registr.addComponent(component);
            } // end of if (reg != component)
        } // end of for ()
        components.add(component);
    }
}
