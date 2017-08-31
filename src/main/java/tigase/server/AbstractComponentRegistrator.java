package tigase.server;

import java.util.ArrayList;
import java.util.List;

public class AbstractComponentRegistrator implements ComponentRegistrator {
    private List<ServerComponent> components = new ArrayList<ServerComponent>();

    @Override
    public boolean addComponent(ServerComponent component) {
        return components.add(component);
    }

    @Override
    public boolean deleteComponent(ServerComponent component) {
        return components.remove(component);
    }
}
